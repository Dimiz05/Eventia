package com.example.eventia

import com.example.eventia.ui.navigation.ConnectionNotice
import com.example.eventia.ui.navigation.connectionNotices
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectionNoticeTest {
    @Test
    fun offlineStaysVisibleAndRestoredClearsUnlessConnectionDropsAgain() = runBlocking {
        withTimeout(10_000) {
            val network = Channel<Boolean>()
            val notices = Channel<ConnectionNotice?>()
            val collection = launch {
                network.receiveAsFlow().connectionNotices().collect { notices.send(it) }
            }
            network.send(true)
            assertNull(notices.receive())
            network.send(false)
            assertEquals(ConnectionNotice.Offline, notices.receive())
            network.send(true)
            assertEquals(ConnectionNotice.Restored, notices.receive())
            network.send(false)
            assertEquals(ConnectionNotice.Offline, notices.receive())
            network.send(true)
            assertEquals(ConnectionNotice.Restored, notices.receive())
            assertNull(notices.receive())
            collection.cancel()
        }
    }
}
