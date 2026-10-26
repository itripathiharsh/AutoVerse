package com.autoverse.launcher.core.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueSelectionTest {

    @Test
    fun `sessions that advertise skip-to-queue-item support selection`() {
        assertTrue(queueSelectionSupported(ACTION_SKIP_TO_QUEUE_ITEM_FLAG))
    }

    @Test
    fun `sessions without the action are refused before any transport call`() {
        assertFalse(queueSelectionSupported(0L))
        // Play/pause bitmask only (ACTION_PLAY + ACTION_PAUSE): no queue skip.
        assertFalse(queueSelectionSupported((1L shl 4) or (1L shl 5)))
    }

    @Test
    fun `combined action masks keep the queue bit`() {
        assertTrue(
            queueSelectionSupported(
                ACTION_SKIP_TO_QUEUE_ITEM_FLAG or (1L shl 4) or (1L shl 5),
            ),
        )
    }

    @Test
    fun `unknown action masks are treated leniently`() {
        assertTrue(queueSelectionSupported(null))
    }
}
