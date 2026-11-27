package com.muyeon.app.auth

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceSyncRunnerTest {
    private class MemStore : DeviceSyncStore {
        var value: DeviceFingerprint? = null
        override fun load() = value
        override fun save(fp: DeviceFingerprint) { value = fp }
        override fun clear() { value = null }
    }

    private val fp = DeviceFingerprint("tok", allowed = false, appVersion = "1.0", userId = "42")

    @Test fun success_isRemembered_andIdenticalSkipped() = runBlocking {
        val store = MemStore(); val runner = DeviceSyncRunner(store); var sends = 0
        assertTrue(runner.run(fp, false) { sends++; true })
        assertEquals(fp, store.value)
        assertFalse(runner.run(fp, false) { sends++; true })
        assertEquals(1, sends)
    }

    @Test fun failure_notRemembered_soNextRunResends() = runBlocking {
        val store = MemStore(); val runner = DeviceSyncRunner(store); var sends = 0
        assertFalse(runner.run(fp, false) { sends++; false })
        assertNull(store.value)
        assertFalse(runner.run(fp, false) { sends++; throw RuntimeException("net") })
        assertNull(store.value)
        assertTrue(runner.run(fp, false) { sends++; true })
        assertEquals(3, sends)
    }

    @Test fun permissionTurnedOn_sendsAgain() = runBlocking {
        val store = MemStore(); val runner = DeviceSyncRunner(store)
        runner.run(fp, false) { true }
        val on = fp.copy(allowed = true)
        assertTrue(runner.run(on, false) { true })
        assertEquals(on, store.value)
    }

    @Test fun clear_makesNextRunSend() = runBlocking {
        val store = MemStore(); val runner = DeviceSyncRunner(store); var sends = 0
        runner.run(fp, false) { sends++; true }
        runner.clear()
        assertTrue(runner.run(fp, false) { sends++; true })
        assertEquals(2, sends)
    }

    @Test fun loggedOut_neverCallsSend() = runBlocking {
        val runner = DeviceSyncRunner(MemStore()); var sends = 0
        assertFalse(runner.run(null, true) { sends++; true })
        assertEquals(0, sends)
    }
}
