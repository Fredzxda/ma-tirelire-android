package org.matirelire.android

/** Shared resource limits for local storage, imports and recurring generation. */
internal object LedgerLimits {
    const val MAX_TRANSACTIONS = 50_000
    const val MAX_SNAPSHOT_BYTES = 32 * 1024 * 1024
}