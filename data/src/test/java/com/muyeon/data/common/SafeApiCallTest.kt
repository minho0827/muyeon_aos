package com.muyeon.data.common

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.ErrorKind
import com.squareup.moshi.Moshi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class SafeApiCallTest {
    private val moshi = Moshi.Builder().build()
    private fun err(code: Int, body: String) =
        Response.error<String>(code, body.toResponseBody("application/json".toMediaType()))

    @Test fun `4xx 서버 문구와 코드를 그대로`() = runTest {
        val r = safeApiCall(moshi) { err(403, """{"statusCode":403,"message":"강사 유형으로 바꿔 주세요","code":"SWITCH_REQUIRED"}""") }
        r as ApiResult.Error
        assertEquals("강사 유형으로 바꿔 주세요", r.message)
        assertEquals("SWITCH_REQUIRED", r.code)
        assertEquals(ErrorKind.REJECTED, r.kind)
    }

    @Test fun `검증 오류 배열은 첫 줄`() = runTest {
        val r = safeApiCall(moshi) { err(400, """{"message":["제목을 입력해 주세요","b"]}""") } as ApiResult.Error
        assertEquals("제목을 입력해 주세요", r.message)
    }

    @Test fun `5xx 와 경로 없음은 서버 공통 문구`() = runTest {
        val a = safeApiCall(moshi) { err(502, "<html>") } as ApiResult.Error
        val b = safeApiCall(moshi) { err(404, """{"message":"Cannot GET /api/x"}""") } as ApiResult.Error
        assertEquals(ErrorKind.SERVER, a.kind)
        assertEquals(ErrorKind.SERVER, b.kind)
    }

    @Test fun `연결 실패는 NETWORK`() = runTest {
        val r = safeApiCall<String>(moshi) { throw java.net.UnknownHostException("x") } as ApiResult.Error
        assertEquals(ErrorKind.NETWORK, r.kind)
    }
}
