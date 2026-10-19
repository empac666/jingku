package com.broll.poc.ui

/** Native inference can outlive coroutine cancellation and Activity recreation. */
object SearchLease {
    @Volatile var busy = false
        private set
    @Synchronized fun acquire(): Boolean {
        if (busy) return false
        busy = true; return true
    }
    @Synchronized fun release() { busy = false }
}
