package org.matirelire.android

/** One automatic request per locked session; cancellation leaves a manual retry. */
internal class AutomaticUnlock {
    private var attempted = false
    fun onLock(allowAutomatic: Boolean = true) { attempted = !allowAutomatic }
    fun shouldRequest(exists: Boolean, unlocked: Boolean, busy: Boolean, needsCode: Boolean): Boolean {
        if (!exists || unlocked || busy || needsCode || attempted) return false
        attempted = true
        return true
    }
}
