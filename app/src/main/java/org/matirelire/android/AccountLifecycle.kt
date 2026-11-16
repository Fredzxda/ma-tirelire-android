package org.matirelire.android

internal enum class AccountActionKind { ARCHIVE, DELETE }
internal data class AccountAction(val index: Int, val name: String, val kind: AccountActionKind)

internal fun DemoViewModel.isArchived(index: Int) = index in archivedAccounts
internal fun DemoViewModel.activeAccountIndices() = openings.indices.filterNot(::isArchived)
internal fun DemoViewModel.canEditTransaction(row: DemoTransaction): Boolean = !isArchived(row.account) &&
    row.peerId?.let { id -> transactions.firstOrNull { it.id == id }?.let { !isArchived(it.account) } } != false

internal fun DemoViewModel.canDeleteAccount(index: Int): Boolean = index in openings.indices && openings.size > 1

internal fun DemoViewModel.requestAccountAction(index: Int, kind: AccountActionKind) {
    if (index !in accountNames.indices || (kind == AccountActionKind.ARCHIVE && isArchived(index))) return
    pendingAccountAction = AccountAction(index, accountNames[index], kind)
}

/** Convert only the future template, using its current native amounts, never an old occurrence. */
internal fun externalTransferTemplate(rule: RecurringRule, removed: Int, archived: Set<Int>): RecurringRule? {
    val target = rule.target ?: return null
    if (rule.account != removed && target != removed) return null
    val surviving = if (rule.account == removed) target else rule.account
    val amount = if (rule.account == removed) rule.targetAmount ?: rule.amount.negate() else rule.amount
    return rule.copy(account = surviving, target = null, amount = amount, targetAmount = null,
        originalCurrency = null, originalAmount = null, fxRate = null,
        active = rule.active && surviving !in archived)
}

/** Old 0.7.11 archives had stopped their transfers. Convert them but preserve their stopped state. */
internal fun normalizeArchivedTransferTemplates(data: LedgerData): LedgerData = data.copy(rules = data.rules.map { rule ->
    if (rule.target != null && ((rule.account in data.archivedAccounts) != (rule.target in data.archivedAccounts))) {
        externalTransferTemplate(rule, if (rule.account in data.archivedAccounts) rule.account else rule.target, data.archivedAccounts) ?: rule
    } else rule
})

internal fun DemoViewModel.archiveAccount(index: Int): Boolean {
    if (index !in openings.indices || isArchived(index)) return false
    val data = AnticipationForecasts.materialize(exportData())
    val deferredIds = deferredForecasts.map { it.id }.toSet()
    val next = data.copy(archivedAccounts = data.archivedAccounts + index,
        rules = data.rules.map { rule ->
            externalTransferTemplate(rule, index, data.archivedAccounts + index)
                ?: if (rule.account == index) rule.copy(active = false) else rule
        },
        draft = data.draft?.takeUnless { it.account == index || it.transferTarget == index }).partitionForecasts(deferredIds)
    SnapshotCodec.encode(next).fill(0)
    val selected = account.intValue
    restoreData(next)
    account.intValue = selected.takeIf { it in openings.indices && !isArchived(it) } ?: activeAccountIndices().firstOrNull() ?: index
    pendingAccountAction = null
    onChange?.invoke()
    return true
}

internal fun DemoViewModel.unarchiveAccount(index: Int): Boolean {
    if (!isArchived(index) || index !in openings.indices) return false
    val next = normalizeArchivedTransferTemplates(exportData().copy(archivedAccounts = archivedAccounts.toSet() - index))
    SnapshotCodec.encode(next).fill(0)
    val selected = account.intValue
    val configurationDraft = accountDraft
    restoreData(next)
    account.intValue = selected
    configurationDraft?.let(::updateAccountDraft)
    onChange?.invoke() // Never re-link externalized transfers or resume a deliberately stopped rule.
    return true
}

/** Delete this account's history; retain other accounts' entries, amounts and clearing. */
internal fun DemoViewModel.deleteAccount(index: Int): Boolean {
    if (!canDeleteAccount(index)) return false
    fun shifted(value: Int) = if (value > index) value - 1 else value
    val data = AnticipationForecasts.materialize(exportData())
    val removedIds = data.transactions.filter { it.account == index }.map { it.id }.toSet()
    val survivors = data.transactions.filterNot { it.id in removedIds }.map { row ->
        if (row.peerId in removedIds) row.copy(peerId = null, exchangeAmount = null) else row
    }
    val nextRules = data.rules.mapNotNull { rule ->
        val affected = rule.account == index || rule.target == index
        val external = externalTransferTemplate(rule, index, data.archivedAccounts)
        val replacement = external ?: if (affected) {
            // Keep a stopped historical template when surviving entries need their occurrence
            // status. Never turn an unconfirmed forecast into a confirmed ordinary entry.
            val row = survivors.firstOrNull { it.seriesId == rule.id && it.amount.signum() != 0 } ?: return@mapNotNull null
            val peer = row.peerId?.let { id -> survivors.firstOrNull { it.id == id } }
            rule.copy(account = row.account, target = peer?.account, amount = row.amount,
                targetAmount = row.exchangeAmount, originalCurrency = row.originalCurrency,
                originalAmount = row.originalAmount, fxRate = row.fxRate,
                description = row.description, category = row.category, payment = row.payment, active = false)
        } else rule
        replacement.copy(account = shifted(replacement.account), target = replacement.target?.let(::shifted))
    }
    val retainedRuleIds = nextRules.map { it.id }.toSet()
    val rows = survivors.map { row ->
        val changed = row.copy(account = shifted(row.account))
        if (changed.seriesId != null && changed.seriesId !in retainedRuleIds) changed.copy(seriesId = null, occurrence = null) else changed
    }
    val nextOptions = data.referenceOptions.mapValues { (_, options) ->
        if (options.accounts.isEmpty()) options else {
            val accounts = options.accounts.filter { it != index }.map(::shifted).toSet()
            options.copy(accounts = accounts, hidden = options.hidden || accounts.isEmpty())
        }
    }
    val nextDraft = data.draft?.takeUnless { it.account == index || it.transferTarget == index || it.editingId in removedIds }
        ?.let { it.copy(account = shifted(it.account), transferTarget = it.transferTarget?.let(::shifted)) }
    val deferredIds = deferredForecasts.map { it.id }.toSet()
    val next = data.copy(transactions = rows,
        openings = data.openings.filterIndexed { i, _ -> i != index },
        accountNames = data.accountNames.filterIndexed { i, _ -> i != index },
        accountCurrencies = data.accountCurrencies.filterIndexed { i, _ -> i != index },
        archivedAccounts = data.archivedAccounts.filter { it != index }.map(::shifted).toSet(),
        rules = nextRules, draft = nextDraft, referenceOptions = nextOptions).partitionForecasts(deferredIds)
    SnapshotCodec.encode(next).fill(0)
    val selected = account.intValue
    restoreData(next)
    account.intValue = if (selected == index) activeAccountIndices().firstOrNull() ?: 0 else shifted(selected)
    pendingAccountAction = null
    onChange?.invoke()
    return true
}

internal fun DemoViewModel.confirmAccountAction(): Boolean {
    val action = pendingAccountAction ?: return false
    if (accountNames.getOrNull(action.index) != action.name) { pendingAccountAction = null; return false }
    return when (action.kind) {
        AccountActionKind.ARCHIVE -> archiveAccount(action.index)
        AccountActionKind.DELETE -> deleteAccount(action.index)
    }.also { pendingAccountAction = null }
}

internal fun DemoViewModel.archiveSafeRule(rule: RecurringRule): RecurringRule =
    if (isArchived(rule.account) || rule.target?.let(::isArchived) == true) rule.copy(active = false) else rule

/** Preserve visibility while remapping complete paired histories during account lifecycle changes. */
private fun LedgerData.partitionForecasts(ids: Set<Long>): LedgerData = copy(
    transactions = transactions.filterNot { it.id in ids },
    deferredForecasts = transactions.filter { it.id in ids })
