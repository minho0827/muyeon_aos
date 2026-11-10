package com.muyeon.app.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 취소는 실패로 잡지 않고 다시 던진다 · 그 밖의 오류는 Result 로. */
class RunCatchingCancellableTest {

    @Test fun `성공은 값`() {
        assertEquals(3, runCatchingCancellable { 1 + 2 }.getOrNull())
    }

    @Test fun `일반 오류는 실패 Result`() {
        val r = runCatchingCancellable { error("x") }
        assertTrue(r.isFailure)
        assertEquals("x", r.exceptionOrNull()?.message)
    }

    @Test(expected = CancellationException::class)
    fun `취소 예외는 다시 던진다`() {
        runCatchingCancellable { throw CancellationException("cancel") }
    }

    @Test fun `취소된 코루틴 - 실패 처리로 빠지지 않는다`() = runBlocking {
        var failed = false
        val job = async {
            runCatchingCancellable { awaitCancellation() }.onFailure { failed = true }
        }
        yield()
        job.cancel()
        runCatching { job.await() }
        assertTrue(job.isCancelled)
        assertEquals(false, failed)
    }
}
