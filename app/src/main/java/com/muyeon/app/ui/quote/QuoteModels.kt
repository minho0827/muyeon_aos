package com.muyeon.app.ui.quote

/**
 * 견적 문진(채팅형 위저드) 데이터 모델 — iOS `QuoteQuestion.swift` 1:1 이식.
 *  옵션 id·질문 id·순서를 iOS/웹과 동일하게 유지해야 서버 answers 매핑이 맞는다(절대 변경 금지).
 */

enum class QuoteAnswerType { SINGLE, MULTI, REGION, DATE, TEXT }

data class QuoteOption(val id: String, val label: String)

data class QuoteQuestion(
    val id: String,
    val type: QuoteAnswerType,
    val title: String,
    val options: List<QuoteOption> = emptyList(),
    val required: Boolean = true,
)

data class QuoteAnswer(
    val questionId: String,
    val selectedOptionIds: List<String> = emptyList(),
    val text: String? = null,
    val region: String? = null,
    val regionCode: String? = null,
    val dateText: String? = null, // yyyy.MM.dd(E) 표시용 — 서버는 date 문자열
) {
    val isEmpty: Boolean
        get() = selectedOptionIds.isEmpty() && text.isNullOrEmpty() &&
                region.isNullOrEmpty() && dateText.isNullOrEmpty()

    /** 채팅 로그에 보여줄 답변 텍스트 — iOS displayText(in:) 동일. */
    fun displayText(question: QuoteQuestion): String = when (question.type) {
        QuoteAnswerType.SINGLE, QuoteAnswerType.MULTI ->
            question.options.filter { selectedOptionIds.contains(it.id) }.joinToString(", ") { it.label }
        QuoteAnswerType.REGION -> region ?: ""
        QuoteAnswerType.TEXT -> text ?: ""
        QuoteAnswerType.DATE -> dateText ?: ""
    }
}

data class QuoteCategory(
    val id: String,
    val name: String,
    val title: String,
    val classOptions: List<QuoteOption>,
) {
    companion object {
        val all: List<QuoteCategory> = listOf(
            QuoteCategory("ballet", "발레", "발레 레슨", listOf(
                QuoteOption("adult", "성인 발레"), QuoteOption("teen", "초중고 발레"),
                QuoteOption("kid", "유아 발레"), QuoteOption("major", "전공반"), QuoteOption("etc", "기타"))),
            QuoteCategory("barre", "바레", "바레 레슨", listOf(
                QuoteOption("adult", "성인 바레"), QuoteOption("diet", "다이어트 바레"),
                QuoteOption("posture", "체형교정·재활"), QuoteOption("group", "그룹 바레"), QuoteOption("etc", "기타"))),
            QuoteCategory("korean", "한국무용", "한국무용 레슨", listOf(
                QuoteOption("adult", "성인 한국무용"), QuoteOption("teen", "초중고"),
                QuoteOption("kid", "유아"), QuoteOption("major", "전공반(입시)"), QuoteOption("etc", "기타"))),
            QuoteCategory("modern", "현대무용", "현대무용 레슨", listOf(
                QuoteOption("adult", "성인 현대무용"), QuoteOption("teen", "초중고"),
                QuoteOption("major", "전공반(입시)"), QuoteOption("contemp", "컨템포러리"), QuoteOption("etc", "기타"))),
            QuoteCategory("practical", "실용무용", "실용무용 레슨", listOf(
                QuoteOption("kpop", "방송댄스(K-pop)"), QuoteOption("hiphop", "힙합"),
                QuoteOption("cover", "커버댄스"), QuoteOption("exam", "입시 실용무용"), QuoteOption("etc", "기타"))),
            QuoteCategory("balletfit", "발레핏", "발레핏 레슨", listOf(
                QuoteOption("adult", "성인 발레핏"), QuoteOption("diet", "다이어트"),
                QuoteOption("posture", "체형교정"), QuoteOption("group", "그룹"), QuoteOption("etc", "기타"))),
            QuoteCategory("musical", "뮤지컬", "뮤지컬 레슨", listOf(
                QuoteOption("exam", "뮤지컬 입시(연기·노래·춤)"), QuoteOption("audition", "오디션 준비"),
                QuoteOption("hobby", "취미 뮤지컬"), QuoteOption("workshop", "무대 워크숍"), QuoteOption("etc", "기타"))),
        )

        fun find(idOrName: String?): QuoteCategory? =
            all.firstOrNull { it.id == idOrName || it.name == idOrName }
    }
}

object QuoteQuestions {
    /** 카테고리 무관 공통 문진 — iOS `QuoteQuestion.common` 과 동일 순서/옵션. */
    private val common: List<QuoteQuestion> = listOf(
        QuoteQuestion("age", QuoteAnswerType.SINGLE, "레슨생의 연령대는 어떻게 되나요?", listOf(
            QuoteOption("preschool", "미취학 아동"), QuoteOption("elem", "초등학생"), QuoteOption("middle", "중학생"),
            QuoteOption("high", "고등학생"), QuoteOption("20s", "20대"), QuoteOption("30s", "30대"),
            QuoteOption("40s+", "40대 이상"))),
        QuoteQuestion("format", QuoteAnswerType.MULTI, "어떤 레슨 형태를 원하시나요?", listOf(
            QuoteOption("private", "개인 레슨"), QuoteOption("group", "그룹 레슨"), QuoteOption("academy", "학원"),
            QuoteOption("any", "무관"), QuoteOption("etc", "기타"))),
        QuoteQuestion("days", QuoteAnswerType.MULTI, "선호하는 레슨 요일을 선택해주세요.", listOf(
            QuoteOption("mon", "월요일"), QuoteOption("tue", "화요일"), QuoteOption("wed", "수요일"),
            QuoteOption("thu", "목요일"), QuoteOption("fri", "금요일"), QuoteOption("sat", "토요일"),
            QuoteOption("sun", "일요일"))),
        QuoteQuestion("time", QuoteAnswerType.MULTI, "희망하는 레슨 시간대는 언제인가요?", listOf(
            QuoteOption("dawn", "이른 오전 (9시 이전)"), QuoteOption("morning", "오전 (9~12시)"),
            QuoteOption("noon", "오후 (12~3시)"), QuoteOption("afternoon", "늦은 오후 (3~6시)"),
            QuoteOption("evening", "저녁 (6~9시)"), QuoteOption("night", "늦은 저녁 (9시 이후)"))),
        QuoteQuestion("gender", QuoteAnswerType.SINGLE, "레슨생의 성별은 무엇인가요?", listOf(
            QuoteOption("male", "남자"), QuoteOption("female", "여자"))),
        QuoteQuestion("teacherGender", QuoteAnswerType.SINGLE, "선호하는 강사의 성별이 있나요?", listOf(
            QuoteOption("any", "무관"), QuoteOption("male", "남"), QuoteOption("female", "여")), required = false),
        QuoteQuestion("when", QuoteAnswerType.SINGLE, "레슨받고 싶은 날이 언제인가요?", listOf(
            QuoteOption("negotiable", "협의 가능해요."), QuoteOption("asap", "가능한 빨리 진행하고 싶어요."),
            QuoteOption("week", "일주일 이내로 진행하고 싶어요."), QuoteOption("date", "원하는 날짜가 있어요."),
            QuoteOption("etc", "기타"))),
        QuoteQuestion("region", QuoteAnswerType.REGION, "레슨받고 싶은 지역이 어디인가요?"),
        QuoteQuestion("method", QuoteAnswerType.SINGLE, "어떻게 진행하기 원하시나요?", listOf(
            QuoteOption("visitMe", "제가 있는 곳으로 와주세요."), QuoteOption("visitThem", "강사가 있는 곳으로 갈게요."),
            QuoteOption("any", "어떤 방식이든 상관없어요."))),
        QuoteQuestion("inquiry", QuoteAnswerType.SINGLE, "수업 관련 문의사항을 알려주세요!", listOf(
            QuoteOption("later", "강사와 상담 시 논의할게요"), QuoteOption("now", "지금 작성할게요"))),
        QuoteQuestion("detail", QuoteAnswerType.TEXT, "추가 요청사항이 있으면 알려주세요.", required = false),
    )

    /** 카테고리별 첫 질문("어떤 수업") + 공통 문진. */
    fun forCategory(category: QuoteCategory): List<QuoteQuestion> =
        listOf(
            QuoteQuestion("class", QuoteAnswerType.MULTI, "어떤 수업을 원하시나요?", category.classOptions)
        ) + common
}
