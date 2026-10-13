package com.muyeon.app.ui.membership

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams.SubscriptionProductReplacementParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.muyeon.app.utils.TokenManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.core.content.edit
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * 멤버십 구글 플레이 인앱결제(자동갱신 구독) — 앱 전역에 하나. iOS `MembershipStore.swift` 이식.
 *
 * ★ 앱 안 디지털 혜택이라 **구글 플레이 결제만** 쓴다(토스 결제·웹 결제 링크 금지 — 토스는 웹 전용).
 *
 * ★ 멤버십을 여는 판단은 **서버가** 한다. 앱은 구매 토큰(purchaseToken)을
 *   서버(POST /monetization/google/purchases)에 넘기고, 서버가 구글에 확인해 멤버십을 준다.
 *   ★ 확인(acknowledge)도 **서버가** 한다 — 앱은 acknowledgePurchase 를 부르지 않는다.
 *     서버가 끝내 못 받은 구매는 3일 뒤 구글이 자동 환불한다(돈은 냈는데 멤버십이 없는 상태 방지).
 *   통신이 끊겨도 다음 시작·로그인·앱 복귀 때 queryPurchasesAsync 로 다시 보낸다(iOS Transaction.unfinished 대응).
 *
 * 상품 구조: 등급 3종(productId) × 기간 3종(basePlanId).
 *  productId `kr.co.muyeon.app.membership.{basic|standard|pro}`, basePlanId `1m|3m|6m`.
 *
 * 갱신·만료·환불은 구글이 서버로 직접 알린다(RTDN). 앱이 꺼져 있어도 반영된다.
 */
object MembershipBilling : PurchasesUpdatedListener {
    private const val TAG = "MembershipBilling"
    private const val PRODUCT_PREFIX = "kr.co.muyeon.app.membership."
    private const val PREFS = "muyeon.billing"
    /** 서버가 4xx(401 제외)로 거절한 토큰 — 조용한 재전송에서 건너뛴다(다시 보내도 결과가 같다). */
    private const val KEY_REJECTED = "rejectedTokens"
    /** 서버가 받은 토큰 — 서버 확인(acknowledge)이 기기에 늦게 보여도 다시 보내지 않게. */
    private const val KEY_DONE = "doneTokens"
    /** productId → 마지막으로 결제를 시작한 basePlanId. Purchase 에는 basePlanId 가 없어 재전송 때 쓴다. */
    private const val KEY_BASE_PLAN_PREFIX = "basePlan."
    private const val MAX_TOKENS = 30

    const val DELAYED_MESSAGE = "결제는 완료됐지만 멤버십 반영이 늦어지고 있어요. 잠시 후 [구매 복원]을 눌러 주세요."

    sealed interface Outcome {
        /** 서버 반영 완료. 응답을 읽지 못했으면 null — 화면이 /membership/me 를 다시 불러온다. */
        data class Success(val mine: MyMembership?, val deferred: Boolean = false) : Outcome
        data object Cancelled : Outcome
        /** 결제 대기(편의점 결제 등). 결제가 끝나면 onPurchasesUpdated 로 들어온다. */
        data object Pending : Outcome
        data class Failed(val message: String) : Outcome
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var app: Application
    private var client: BillingClient? = null
    private val connectMutex = Mutex()
    /** 서버 전송은 한 번에 하나 — 구매 콜백과 재전송이 같은 토큰을 동시에 보내지 않게. */
    private val submitMutex = Mutex()

    /** 구글 productId → 구글이 내려준 상품(현지 가격 포함). */
    private val _products = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val products: StateFlow<Map<String, ProductDetails>> = _products.asStateFlow()

    /** 결제 진행 중인 "productId:basePlanId"(버튼 스피너·중복 탭 방지). */
    private val _purchasingId = MutableStateFlow<String?>(null)
    val purchasingId: StateFlow<String?> = _purchasingId.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    /**
     * 결제가 서버에 반영됐다 — iOS `membershipDidChange` + `ResultBus.send(.membership)` 대응.
     *  멤버십 화면은 다시 불러오고, 웹뷰(WebViewActivity)는 보이는 중이면 __muyeonResult('MEMBERSHIP') 를 부른다.
     *  replay 없음 — 웹뷰가 가려져 있으면(멤버십 화면 위) 그 화면이 닫힐 때 결과 키(MEMBERSHIP)로 알린다.
     */
    private val _changes = MutableSharedFlow<MyMembership?>(extraBufferCapacity = 4)
    val changes: SharedFlow<MyMembership?> = _changes.asSharedFlow()

    /**
     * 구매 결과 안내(제목 to 본문). 결제 창(Play)에서 돌아올 때 멤버십 화면이 다시 만들어질 수 있어
     *  (2026-10-13 실기기 — 액티비티 재생성으로 화면 코루틴과 함께 '구독 완료' 팝업이 사라졌다)
     *  화면이 아니라 여기에 둔다. 화면은 띄운 뒤 consumeNotice() 로 비운다.
     */
    private val _notice = MutableStateFlow<Pair<String, String>?>(null)
    val notice: StateFlow<Pair<String, String>?> = _notice.asStateFlow()

    fun consumeNotice() { _notice.value = null }

    /** 화면 수명과 무관하게(앱 범위) 구매를 진행하고, 결과 안내를 notice 로 남긴다. toNotice 가 null 이면 안내 없음. */
    fun launchPurchase(
        activity: Activity,
        plan: MembershipPlan,
        currentBasePlanId: String? = null,
        toNotice: (Outcome) -> Pair<String, String>?,
    ) {
        scope.launch { toNotice(purchase(activity, plan, currentBasePlanId))?.let { _notice.value = it } }
    }

    /** launchBillingFlow 결과를 기다리는 구매(사용자가 누른 것). 없으면 콜백은 조용한 전송이다. */
    private var pendingFlow: CompletableDeferred<Outcome>? = null
    /** 미전송 구매 재전송이 도는 중인지(로그인·포그라운드 복귀가 겹쳐도 한 번만 돈다). */
    private var isFlushing = false

    // ── 시작 ──

    /**
     * 앱 시작 시 1회(MuyeonApplication.onCreate). 앱 밖(다른 기기·Play 스토어·결제 대기)에서 생긴 구매와
     *  이전에 못 보낸 구매를 서버에 보낸다. 로그인 직후(TokenWebViewInterface.saveToken)·앱 복귀 때도 다시 보낸다.
     */
    fun start(application: Application) {
        if (client != null) return
        app = application
        client = BillingClient.newBuilder(application)
            .setListener(this)
            // 구독만 팔지만 라이브러리가 요구한다(일회성 상품 결제 대기 허용).
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            // 연결이 끊긴 뒤 호출하면 라이브러리가 알아서 다시 붙는다.
            .enableAutoServiceReconnection()
            .build()
        // 앱이 앞으로 나올 때마다(최초 시작 포함) 미전송 구매를 보낸다.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = flushUnfinished()
        })
    }

    /** 아직 서버가 모르는(확인 안 된) 구매를 다시 보낸다. 로그인 전이면 아무것도 하지 않는다. */
    fun flushUnfinished() {
        if (client == null || !isLoggedIn || isFlushing) return
        isFlushing = true
        scope.launch {
            try {
                val purchases = querySubscriptions() ?: return@launch
                val prefs = prefs()
                val rejected = prefs.getStringSet(KEY_REJECTED, emptySet()).orEmpty()
                val done = prefs.getStringSet(KEY_DONE, emptySet()).orEmpty()
                purchases.filter {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged &&
                        it.purchaseToken !in rejected && it.purchaseToken !in done
                }.forEach { submit(it, quiet = true) }
            } finally {
                isFlushing = false
            }
        }
    }

    // ── 상품 ──

    suspend fun loadProducts(ids: List<String>) {
        val wanted = ids.toSet() - _products.value.keys
        if (wanted.isEmpty() || !ready()) return
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            wanted.map {
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(it).setProductType(BillingClient.ProductType.SUBS).build()
            },
        ).build()
        val list = suspendCancellableCoroutine { cont ->
            client!!.queryProductDetailsAsync(params) { r, result ->
                if (r.responseCode != BillingResponseCode.OK) Log.w(TAG, "상품 조회 실패: ${r.responseCode} ${r.debugMessage}")
                if (cont.isActive) cont.resume(result.productDetailsList)
            }
        }
        if (list.isNotEmpty()) _products.value = _products.value + list.associateBy { it.productId }
    }

    /** basePlanId 가 같은 기본 요금제 오퍼(offerId 없음). 없으면 그 기본 요금제의 첫 오퍼. */
    fun offerFor(details: ProductDetails?, basePlanId: String?): ProductDetails.SubscriptionOfferDetails? {
        val offers = details?.subscriptionOfferDetails?.filter { it.basePlanId == basePlanId }.orEmpty()
        return offers.firstOrNull { it.offerId == null } ?: offers.firstOrNull()
    }

    /** 기본 요금제의 정기 결제 단계(마지막 단계) — 화면 가격. */
    fun recurringPhase(details: ProductDetails?, basePlanId: String?): ProductDetails.PricingPhase? =
        offerFor(details, basePlanId)?.pricingPhases?.pricingPhaseList?.lastOrNull()

    // ── 구매 ──

    /**
     * 구독 시작 또는 변경. 이 Google 계정에 이용 중인 멤버십 구독이 있으면 교체(SubscriptionUpdateParams)로 연다 —
     *  - 다른 상품(등급 변경): 결제 금액이 오르면 즉시(WITH_TIME_PRORATION), 같거나 내려가면 다음 갱신일부터(DEFERRED).
     *    지금 상품의 금액을 모르면 등급으로 대신한다.
     *  - 같은 상품의 기간만 바꿈(베이직 1→3개월 등): 즉시 + 새 기간 요금 전액(CHARGE_FULL_PRICE). 남은 기간은 구글이 이어 준다.
     *    ★ 2026-10-13 실기기 진단: 같은 상품끼리는 DEFERRED·WITH_TIME_PRORATION·CHARGE_PRORATED_PRICE 가 모두
     *     DEVELOPER_ERROR(5) 'Requested replacement mode is not supported for this request' 로 거절되고 이 방식만 된다.
     */
    suspend fun purchase(activity: Activity, plan: MembershipPlan, currentBasePlanId: String? = null): Outcome {
        if (_purchasingId.value != null) return Outcome.Cancelled
        if (!isLoggedIn) return Outcome.Failed("로그인한 뒤 구독할 수 있어요.")
        val productId = plan.googleProductId ?: return Outcome.Failed("상품 정보를 불러오지 못했어요.")
        val basePlanId = plan.googleBasePlanId ?: return Outcome.Failed("상품 정보를 불러오지 못했어요.")
        _purchasingId.value = "$productId:$basePlanId"
        try {
            // obfuscatedAccountId — 서버가 구글 알림에서 "누구의 구매인지" 찾는 열쇠. 못 받으면 결제를 시작하지 않는다.
            val accountId = api().googleAccountToken().getOrNull()
                ?: return Outcome.Failed("결제를 준비하지 못했어요. 잠시 후 다시 시도해 주세요.")
            if (!ready()) return Outcome.Failed("Google Play 에 연결하지 못했어요. 잠시 후 다시 시도해 주세요.")
            val details = _products.value[productId]
            val offer = offerFor(details, basePlanId)
            if (details == null || offer == null) {
                return Outcome.Failed("Google Play 상품 정보를 불러오지 못했어요. 잠시 후 다시 열어 주세요.")
            }

            val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details).setOfferToken(offer.offerToken)
            val flow = BillingFlowParams.newBuilder().setObfuscatedAccountId(accountId)

            // 이용 중인 구독이 있으면 새로 사지 않고 교체한다(이중 구독 방지).
            val old = querySubscriptions()?.firstOrNull {
                it.purchaseState == Purchase.PurchaseState.PURCHASED && membershipProductOf(it) != null
            }
            var deferred = false
            if (old != null) {
                val oldProductId = membershipProductOf(old)!!
                val oldBasePlanId = currentBasePlanId ?: lastBasePlan(oldProductId)
                val oldPrice = recurringPhase(_products.value[oldProductId], oldBasePlanId)?.priceAmountMicros
                val newPrice = offer.pricingPhases.pricingPhaseList.lastOrNull()?.priceAmountMicros
                deferred = if (oldPrice != null && newPrice != null) newPrice <= oldPrice
                else tierRank(productId) <= tierRank(oldProductId)
                val update = BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                    .setOldPurchaseToken(old.purchaseToken)
                if (oldProductId == productId) {
                    // 같은 상품의 기간만 바꿈 — 구독 단위 교체 + CHARGE_FULL_PRICE 만 구글이 받아 준다(위 설명).
                    deferred = false
                    update.setSubscriptionReplacementMode(
                        BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE,
                    )
                } else {
                    val mode = if (deferred) SubscriptionProductReplacementParams.ReplacementMode.DEFERRED
                    else SubscriptionProductReplacementParams.ReplacementMode.WITH_TIME_PRORATION
                    productParams.setSubscriptionProductReplacementParams(
                        SubscriptionProductReplacementParams.newBuilder()
                            .setOldProductId(oldProductId).setReplacementMode(mode).build(),
                    )
                }
                flow.setSubscriptionUpdateParams(update.build())
            }
            flow.setProductDetailsParamsList(listOf(productParams.build()))

            rememberBasePlan(productId, basePlanId)
            val waiter = CompletableDeferred<Outcome>()
            pendingFlow = waiter
            val launched = client!!.launchBillingFlow(activity, flow.build())
            if (launched.responseCode != BillingResponseCode.OK) {
                pendingFlow = null
                return launchFailure(launched)
            }
            val outcome = waiter.await()
            return if (deferred && outcome is Outcome.Success) outcome.copy(deferred = true) else outcome
        } finally {
            pendingFlow = null
            _purchasingId.value = null
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        val waiter = pendingFlow
        pendingFlow = null
        when (result.responseCode) {
            BillingResponseCode.OK -> scope.launch {
                var outcome: Outcome? = null
                for (p in purchases.orEmpty()) {
                    val o = when (p.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> submit(p, quiet = waiter == null)
                        Purchase.PurchaseState.PENDING -> Outcome.Pending
                        else -> continue
                    }
                    if (outcome !is Outcome.Failed) outcome = o
                }
                // 다음 갱신일 교체(DEFERRED)는 구매 목록 없이 성공으로만 올 수 있다 — 서버는 구글 알림으로 안다.
                waiter?.complete(outcome ?: Outcome.Success(null))
            }
            BillingResponseCode.USER_CANCELED -> waiter?.complete(Outcome.Cancelled)
            // 이미 이 Google 계정으로 구독 중 — 서버에 다시 보내 연결한다(구매 복원과 같다).
            BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch {
                waiter?.complete(submitOwned() ?: Outcome.Failed("이미 구독 중인 상품이에요."))
            }
            else -> {
                Log.w(TAG, "구매 실패: ${result.responseCode} ${result.debugMessage}")
                waiter?.complete(launchFailure(result))
            }
        }
    }

    // ── 구매 복원 ──

    /** 이 Google 계정으로 구독 중인 멤버십을 다시 연결한다(앱 재설치·기기 변경·반영 지연). */
    suspend fun restore(): Outcome {
        if (!isLoggedIn) return Outcome.Failed("로그인한 뒤 복원할 수 있어요.")
        _isRestoring.value = true
        try {
            if (!ready()) return Outcome.Failed("Google Play 에 연결하지 못했어요. 잠시 후 다시 시도해 주세요.")
            return submitOwned() ?: Outcome.Failed("이 Google 계정으로 이용 중인 멤버십 구독이 없어요.")
        } finally {
            _isRestoring.value = false
        }
    }

    /** 이 기기 Google 계정의 멤버십 구독을 모두 보낸다(조용하지 않게). 구독이 없으면 null. */
    private suspend fun submitOwned(): Outcome? {
        val purchases = querySubscriptions()
            ?: return Outcome.Failed("Google Play 구매 내역을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.")
        var last: Outcome? = null
        for (p in purchases) {
            if (membershipProductOf(p) == null) continue
            val outcome = if (p.purchaseState == Purchase.PurchaseState.PURCHASED) submit(p, quiet = false)
            else Outcome.Pending
            if (outcome is Outcome.Failed) return outcome
            last = outcome
        }
        return last
    }

    // ── 서버 전송 ──

    /**
     * 구매를 서버에 보낸다. 서버가 받으면(2xx) 끝, 4xx(401 제외)면 다시 보내도 같아 그만 보낸다,
     *  401·5xx·통신 오류면 남겨 두고 다음 기회(시작·로그인·복귀·[구매 복원])에 다시 보낸다.
     * @param quiet 사용자가 누르지 않은 백그라운드 전송(앱 시작·복귀·콜백). 이미 받은 토큰은 건너뛴다.
     */
    private suspend fun submit(purchase: Purchase, quiet: Boolean): Outcome = submitMutex.withLock {
        // 결제 대기·미확정 구매는 서버로 보내지 않는다 — 결제가 끝나면 콜백으로 다시 온다.
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return@withLock Outcome.Pending
        // 로그인 전이면 보내지 않고 남겨 둔다 — 로그인 직후 flushUnfinished 로 다시 온다.
        if (!isLoggedIn) return@withLock Outcome.Failed("로그인한 뒤 다시 시도해 주세요.")
        val token = purchase.purchaseToken
        if (quiet && token in prefs().getStringSet(KEY_DONE, emptySet()).orEmpty()) {
            return@withLock Outcome.Success(null)
        }
        val productId = membershipProductOf(purchase) ?: purchase.products.firstOrNull()
            ?: return@withLock Outcome.Failed("결제 정보를 확인하지 못했어요.")
        val basePlanId = prefs().getString(KEY_BASE_PLAN_PREFIX + productId, null)

        val res = api().submitGooglePurchase(productId, basePlanId, token).getOrElse {
            Log.w(TAG, "구매 전송 실패(통신): ${it.message}")
            return@withLock Outcome.Failed(DELAYED_MESSAGE)
        }
        when {
            // 서버가 본 구글 상태가 아직 결제 대기(PENDING) — 아무것도 주지 않았다. 받은 토큰으로 기록하지 않아야
            //  결제가 끝난 뒤 콜백·복귀 때 다시 보낸다.
            res.status == 202 -> Outcome.Pending
            res.status in 200..299 -> {
                addToken(KEY_DONE, token)
                removeToken(KEY_REJECTED, token)
                // 응답을 못 읽어도 결제는 반영됐다 — 실패로 알리지 않는다(화면이 다시 불러온다).
                val mine = runCatching { MyMembership.from(JSONObject(res.body)) }.getOrNull()
                // 반영됐다 — 네이티브 화면과 웹뷰를 함께 갱신한다(앱 복귀·콜백으로 반영된 것도).
                _changes.tryEmit(mine)
                Outcome.Success(mine)
            }
            else -> {
                val message = runCatching { JSONObject(res.body).optString("message") }.getOrNull()
                    ?.takeIf { it.isNotBlank() }
                // 4xx 는 다시 보내도 결과가 같다(다른 계정 구독·잘못된 토큰) → 그만 보낸다. 401·5xx 는 남겨 두고 재시도.
                if (res.status in 400..499 && res.status != 401) addToken(KEY_REJECTED, token)
                Log.w(TAG, "구매 전송 실패: ${res.status} $message")
                Outcome.Failed(message ?: DELAYED_MESSAGE)
            }
        }
    }

    // ── 구독 관리 ──

    /** Google Play 구독 관리 화면 주소(해지·결제 수단 변경은 Play 가 처리). */
    fun manageUrl(packageName: String, productId: String?): String =
        if (productId.isNullOrEmpty()) "https://play.google.com/store/account/subscriptions?package=$packageName"
        else "https://play.google.com/store/account/subscriptions?sku=$productId&package=$packageName"

    /** 서버가 basePlanId 를 안 내려줄 때 쓰는, 이 기기에서 마지막으로 결제한 기간. */
    fun lastBasePlan(productId: String?): String? =
        if (productId == null || !::app.isInitialized) null
        else prefs().getString(KEY_BASE_PLAN_PREFIX + productId, null)

    /** 등급 순서 PRO > STANDARD > BASIC. 모르는 상품은 0. */
    fun tierRank(productId: String?): Int = when (productId?.removePrefix(PRODUCT_PREFIX)?.lowercase()) {
        "basic" -> 1
        "standard" -> 2
        "pro" -> 3
        else -> 0
    }

    // ── 내부 ──

    private fun membershipProductOf(p: Purchase): String? = p.products.firstOrNull { it.startsWith(PRODUCT_PREFIX) }

    private fun launchFailure(r: BillingResult): Outcome = when (r.responseCode) {
        BillingResponseCode.USER_CANCELED -> Outcome.Cancelled
        BillingResponseCode.BILLING_UNAVAILABLE ->
            Outcome.Failed("이 기기에서는 Google Play 결제를 사용할 수 없어요. Play 스토어 로그인 상태를 확인해 주세요.")
        BillingResponseCode.ITEM_UNAVAILABLE -> Outcome.Failed("지금은 구매할 수 없는 상품이에요.")
        else -> Outcome.Failed("결제를 완료하지 못했어요. 잠시 후 다시 시도해 주세요.")
    }

    /** 연결돼 있지 않으면 연결을 기다린다. 실패하면 false. */
    private suspend fun ready(): Boolean {
        val c = client ?: return false
        if (c.isReady) return true
        return connectMutex.withLock {
            if (c.isReady) return@withLock true
            suspendCancellableCoroutine { cont ->
                c.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (result.responseCode != BillingResponseCode.OK) {
                            Log.w(TAG, "Play 연결 실패: ${result.responseCode} ${result.debugMessage}")
                        }
                        if (cont.isActive) cont.resume(result.responseCode == BillingResponseCode.OK)
                    }

                    override fun onBillingServiceDisconnected() {
                        if (cont.isActive) cont.resume(false)
                    }
                })
            }
        }
    }

    /** 이 기기 Google 계정의 구독 구매(활성). 조회 실패면 null. */
    private suspend fun querySubscriptions(): List<Purchase>? {
        if (!ready()) return null
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        return suspendCancellableCoroutine { cont ->
            client!!.queryPurchasesAsync(params) { r, list ->
                val ok = r.responseCode == BillingResponseCode.OK
                if (!ok) Log.w(TAG, "구매 조회 실패: ${r.responseCode} ${r.debugMessage}")
                if (cont.isActive) cont.resume(if (ok) list else null)
            }
        }
    }

    private val isLoggedIn: Boolean
        get() = ::app.isInitialized && !TokenManager.getAccessToken(app).isNullOrEmpty()

    private fun api() = MembershipApi(TokenManager.getAccessToken(app))

    private fun prefs() = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun rememberBasePlan(productId: String, basePlanId: String) {
        prefs().edit { putString(KEY_BASE_PLAN_PREFIX + productId, basePlanId) }
    }

    private fun addToken(key: String, token: String) {
        val cur = prefs().getStringSet(key, emptySet()).orEmpty().toMutableList()
        if (token in cur) return
        cur.add(token)
        // 넘치면 아무거나 버린다(Set 이라 순서가 없다) — 버려진 토큰은 구글 확인 여부로 다시 걸러진다.
        while (cur.size > MAX_TOKENS) cur.removeAt(0)
        prefs().edit { putStringSet(key, cur.toSet()) }
    }

    private fun removeToken(key: String, token: String) {
        val cur = prefs().getStringSet(key, emptySet()).orEmpty()
        if (token !in cur) return
        prefs().edit { putStringSet(key, cur - token) }
    }
}
