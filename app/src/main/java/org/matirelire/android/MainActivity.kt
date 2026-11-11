package org.matirelire.android

import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

import androidx.compose.foundation.selection.toggleable

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.isActive
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import org.matirelire.android.domain.Money

class MainActivity : FragmentActivity() {
    private lateinit var vault: VaultController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        vault = ViewModelProvider(this)[VaultController::class.java]
        vault.bindModel(ViewModelProvider(this)[DemoViewModel::class.java])
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF206859),
                    secondary = Color(0xFF526866),
                    background = Color(0xFFF7F7F0),
                    surface = Color(0xFFFEFEF9),
                    primaryContainer = Color(0xFFDAEBDF),
                    onPrimaryContainer = Color(0xFF163B32),
                ),
            ) {
                VaultGate(vault, this@MainActivity) { DemoApp(vault, this@MainActivity) }
            }
        }
    }

    override fun onStop() {
        if (!isChangingConfigurations && ::vault.isInitialized) vault.onBackground()
        super.onStop()
    }
}

internal data class DemoTransaction(
    val id: Long,
    val account: Int,
    val description: String,
    val amount: BigDecimal,
    val category: String,
    val payment: String,
    val date: String,
    val cleared: Boolean = false,
    val peerId: Long? = null,
    val seriesId: String? = null,
    val occurrence: Int? = null,
    val amountConfirmed: Boolean = true,
    val readyToConfirm: Boolean = false,
)

internal data class EntryDraft(
    val account: Int,
    val income: Boolean = false,
    val amount: String = "",
    val description: String = "",
    val category: String = "",
    val payment: String = "",
    val attempted: Boolean = false,
    val addingCategory: Boolean = false,
    val addingPayment: Boolean = false,
    val newCategory: String = "",
    val newPayment: String = "",
    val editingId: Long? = null,
    val transferTarget: Int? = null,
    val repeatFrequency: RepeatFrequency = RepeatFrequency.NONE,
    val repeatUntil: String = "",
    val repeatCount: String = "",
    val applyToSeries: Boolean = false,
    val date: String = "",
)

internal data class ReconciliationDraft(val account: Int, val target: String = "", val selected: Set<Long>,
    val original: List<DemoTransaction>, val opening: BigDecimal)

internal data class AccountDraft(val index: Int?, val name: String = "", val opening: String = "0", val attempted: Boolean = false)

/** Session state; snapshots are persisted only through the encrypted vault callback. */
class DemoViewModel : ViewModel() {
    internal var operationsMenu by mutableStateOf(OperationsMenu.NONE)
    internal var accountPeriod by mutableStateOf(AccountPeriod())
    internal var searchQuery by mutableStateOf("")
    internal val transactions = mutableStateListOf<DemoTransaction>()
    internal val destination = mutableIntStateOf(0)
    internal val account = mutableIntStateOf(0)
    internal var openings by mutableStateOf(listOf(BigDecimal("500.00"), BigDecimal("3000.00")))
        private set
    internal var onChange: (() -> Unit)? = null
    internal val accountNames = mutableStateListOf<String>()
    internal var accountDraft by mutableStateOf<AccountDraft?>(null)
        private set
    internal fun ensureAccountNames(defaults: List<String>) {
        if (accountNames.isEmpty()) { accountNames.addAll(defaults); onChange?.invoke() }
    }
    internal fun beginAccount(index: Int?, locale: Locale) {
        if (index != null && index !in openings.indices) return
        val separator = java.text.DecimalFormatSymbols.getInstance(locale).decimalSeparator
        accountDraft = AccountDraft(index, index?.let { accountNames[it] }.orEmpty(),
            index?.let { openings[it].toPlainString().replace('.', separator) } ?: "0")
    }
    internal fun updateAccountDraft(value: AccountDraft) { accountDraft = value.copy(attempted = false) }
    internal fun cancelAccount() { accountDraft = null }
    internal fun saveAccount(locale: Locale): Boolean {
        val current = accountDraft ?: return false
        val name = current.name.trim()
        val value = Money.parseOpening(current.opening, locale)
        if (name.isEmpty() || name.length > 60 || value == null ||
            accountNames.withIndex().any { it.index != current.index && it.value.equals(name, true) } ||
            (current.index == null && accountNames.size >= 50) ||
            (current.index != null && current.index !in openings.indices)) {
            accountDraft = current.copy(attempted = true); return false
        }
        if (current.index == null) {
            accountNames.add(name); openings = openings + value
        } else {
            accountNames[current.index] = name
            openings = openings.toMutableList().also { it[current.index] = value }
        }
        accountDraft = null
        onChange?.invoke()
        return true
    }
    internal var reconciliation by mutableStateOf<ReconciliationDraft?>(null)
        private set
    internal fun beginReconciliation(index: Int) {
        if (index !in openings.indices) return
        val rows = transactions.filter { it.account == index }
        reconciliation = ReconciliationDraft(index, selected = rows.filter { it.cleared }.map { it.id }.toSet(),
            original = rows, opening = openings[index])
    }
    internal fun reconciliationTarget(value: String) { reconciliation = reconciliation?.copy(target = value.take(30)) }
    internal fun reconciliationToggle(id: Long) {
        val current = reconciliation ?: return
        if (current.original.none { it.id == id }) return
        reconciliation = current.copy(selected = if (id in current.selected) current.selected - id else current.selected + id)
    }
    internal fun reconciliationBalance(): BigDecimal? = reconciliation?.let { d ->
        d.original.filter { it.id in d.selected }.fold(d.opening) { sum, row -> sum + row.amount }
    }
    internal fun reconciliationDifference(locale: Locale): BigDecimal? {
        val current = reconciliation ?: return null
        val target = Money.parseOpening(current.target, locale) ?: return null
        return target - requireNotNull(reconciliationBalance())
    }
    internal fun cancelReconciliation() { reconciliation = null }
    internal fun confirmReconciliation(locale: Locale): Boolean {
        val current = reconciliation ?: return false
        if (reconciliationDifference(locale)?.signum() != 0 ||
            transactions.filter { it.account == current.account } != current.original ||
            openings.getOrNull(current.account) != current.opening) return false
        // Apply all flags in one persisted snapshot, after checking the exact target.
        transactions.indices.forEach { i ->
            val row = transactions[i]
            if (row.account == current.account) transactions[i] = row.copy(cleared = row.id in current.selected)
        }
        reconciliation = null
        onChange?.invoke()
        return true
    }
    internal val referenceUiStates = mutableMapOf<String, Any>()
    internal val referenceOptions = androidx.compose.runtime.mutableStateMapOf<String, ReferenceOptions>()
    internal val categories = mutableStateListOf<String>()
    internal var transferPayment by mutableStateOf<String?>(null)
        private set
    internal fun renameTransferPayment(old: String, next: String) { if (transferPayment == old) transferPayment = next }
    internal fun isTransferPayment(value: String) = value == transferPayment
    internal fun ensureTransferPayment(defaultName: String) {
        if (transferPayment != null) return
        val designated = payments.firstOrNull { it.equals("Virement", true) || it.equals("Transfer", true) }
            ?: addPayment(defaultName) ?: return
        transferPayment = designated
        for (i in rules.indices) if (rules[i].target != null) rules[i] = rules[i].copy(payment = designated)
        val migrated = transactions.map { if (it.peerId != null) it.copy(payment = designated) else it }
        transactions.clear(); transactions.addAll(migrated)
        draft = draft?.let { if (it.transferTarget != null) it.copy(payment = designated, addingPayment = false, newPayment = "") else it }
        onChange?.invoke()
    }
    internal fun selectDraftPayment(value: String) {
        val current = draft ?: return
        val linked = transactions.firstOrNull { it.id == current.editingId }?.peerId != null
        if (linked && !isTransferPayment(value)) return
        updateDraft(current.copy(payment = value, transferTarget = if (isTransferPayment(value)) current.transferTarget else null))
    }
    /** All future income/expense and category reports must use this perimeter. */
    internal fun reportingTransactions() = transactions.filter {
        it.peerId == null && !RecurringState.isFutureUnconfirmed(it, calendarToday)
    }
    internal var dateMigrationVersion = 0
        private set
    internal var transactionOrder by mutableStateOf(TransactionOrder.ORIGINAL)
        private set
    internal fun setTransactionOrder(value: TransactionOrder) {
        if (transactionOrder == value) return
        transactionOrder = value; onChange?.invoke()
    }
    internal var anticipation by mutableStateOf(Anticipation.DUE)
        private set
    internal var calendarToday by mutableStateOf(java.time.LocalDate.now())
        private set
    internal fun setAnticipation(value: Anticipation) {
        if (anticipation == value) return
        anticipation = value; recurrenceEpoch++; onChange?.invoke()
    }
    internal val rules = mutableStateListOf<RecurringRule>()
    internal var recurrenceEpoch by mutableIntStateOf(0)
        private set
    internal var recurrenceBlocked by mutableStateOf(false)
        private set
    internal fun generateDue(today: java.time.LocalDate = java.time.LocalDate.now()): Boolean {
        calendarToday = today
        val result = Recurrences.generate(exportData(), today)
        recurrenceBlocked = result.blocked
        if (!result.changed) return false
        val checked = try { SnapshotCodec.encode(result.data) }
        catch (_: IllegalArgumentException) { recurrenceBlocked = true; return false }
        checked.fill(0)
        transactions.clear(); transactions.addAll(result.data.transactions)
        rules.clear(); rules.addAll(result.data.rules)
        recurrenceEpoch++
        onChange?.invoke()
        return true
    }
    internal fun setSeriesActive(id: String, active: Boolean) {
        val index = rules.indexOfFirst { it.id == id }
        if (index < 0) return
        val next = rules[index].copy(active = active)
        if (active && next.nextDate() == null) return
        rules[index] = next; recurrenceEpoch++; onChange?.invoke()
    }
    internal val payments = mutableStateListOf<String>()
    internal var draft by mutableStateOf<EntryDraft?>(null)
        private set
    internal var limitReached by mutableStateOf(false)
        private set
    private var initialized = false

    internal fun initialize(samples: List<DemoTransaction>) {
        if (!initialized) {
            transactions.addAll(samples)
            samples.forEach { addCategory(it.category); addPayment(it.payment) }
            initialized = true
            onChange?.invoke()
        }
    }

    internal fun balance(account: Int, clearedOnly: Boolean = false): BigDecimal =
        transactions.filter { it.account == account && (!clearedOnly || it.cleared) }
            .fold(openings[account]) { total, tx -> total + tx.amount }

    internal fun toggleCleared(id: Long) {
        val index = transactions.indexOfFirst { it.id == id }
        if (index >= 0) {
            transactions[index] = transactions[index].copy(cleared = !transactions[index].cleared)
            onChange?.invoke()
        }
    }

    private fun addReference(items: MutableList<String>, text: String): String? {
        val name = text.trim().take(60)
        if (name.isBlank()) return null
        val existing = items.firstOrNull { it.equals(name, ignoreCase = true) }
        if (existing != null) return existing
        if (items.size >= 500) { limitReached = true; return null }
        items.add(name)
        if (initialized) onChange?.invoke()
        return name
    }

    internal fun addCategory(text: String): String? = addReference(categories, text)
    internal fun addPayment(text: String): String? = addReference(payments, text)

    internal fun beginEntry(today: java.time.LocalDate = java.time.LocalDate.now()) {
        if (draft == null) {
            draft = EntryDraft(account = account.intValue, date = today.toString(),
                category = referenceChoices(true, account.intValue, false).firstOrNull().orEmpty(), payment = referenceChoices(false, account.intValue, false).firstOrNull().orEmpty())
            onChange?.invoke()
        }
    }

    internal fun editEntry(id: Long, locale: Locale) {
        if (draft != null) return
        val row = transactions.firstOrNull { it.id == id } ?: return
        val separator = java.text.DecimalFormatSymbols.getInstance(locale).decimalSeparator
        draft = EntryDraft(account = row.account, income = row.amount.signum() > 0,
            amount = row.amount.abs().toPlainString().replace('.', separator),
            description = row.description, category = row.category, payment = row.payment, editingId = id,
            repeatFrequency = rules.firstOrNull { it.id == row.seriesId }?.frequency ?: RepeatFrequency.NONE,
            repeatUntil = rules.firstOrNull { it.id == row.seriesId }?.until.orEmpty(),
            repeatCount = rules.firstOrNull { it.id == row.seriesId }?.let { r -> r.count?.let { (it - r.next).coerceAtLeast(0).toString() } }.orEmpty(),
            transferTarget = row.peerId?.let { peer -> transactions.firstOrNull { it.id == peer }?.account }, date = if (OperationDates.parse(row.date) != null) row.date else "")
        onChange?.invoke()
    }

    internal fun deleteEntry(id: Long) {
        val row = transactions.firstOrNull { it.id == id } ?: return
        val ids = setOfNotNull(row.id, row.peerId)
        if (transactions.removeAll { it.id in ids }) {
            if (draft?.editingId in ids) draft = null
            onChange?.invoke() // one encrypted snapshot deletes both sides
        }
    }

    internal fun changeDraftAccount(index: Int) {
        val current = draft ?: return
        if (current.editingId != null && current.transferTarget != null) return
        val target = if (current.transferTarget == index) openings.indices.firstOrNull { it != index } else current.transferTarget
        updateDraft(current.copy(account = index, transferTarget = target))
    }

    internal fun updateDraft(next: EntryDraft) {
        val previous = draft ?: return
        var value = next.copy(attempted = false)
        if (next.editingId == null && (previous.account != next.account || previous.income != next.income)) {
            val categoryChoices = referenceChoices(true, next.account, next.income)
            val paymentChoices = referenceChoices(false, next.account, next.income)
            value = value.copy(category = next.category.takeIf { it in categoryChoices } ?: categoryChoices.firstOrNull().orEmpty(),
                payment = next.payment.takeIf { it in paymentChoices } ?: paymentChoices.firstOrNull().orEmpty())
            if (!isTransferPayment(value.payment)) value = value.copy(transferTarget = null)
        }
        draft = value; onChange?.invoke()
    }
    internal fun selectRepeatEnd(value: String?): Boolean {
        val current = draft ?: return false
        if (value != null) {
            val date = OperationDates.parse(value) ?: return false
            val start = OperationDates.parse(current.date) ?: return false
            if (date < start) return false
        }
        updateDraft(current.copy(repeatUntil = value.orEmpty(), repeatCount = if (value == null) current.repeatCount else ""))
        return true
    }
    internal fun cancelEntry() { draft = null; onChange?.invoke() }

    internal fun exportData() = LedgerData(transactions.toList(), categories.toList(), payments.toList(), openings.toList(), draft, accountNames.toList(), transferPayment, rules.toList(), anticipation, transactionOrder, dateMigrationVersion, referenceOptions.toMap())

    internal fun restoreData(data: LedgerData) {
        referenceUiStates.clear()
        transactions.clear(); transactions.addAll(data.transactions)
        referenceOptions.clear(); referenceOptions.putAll(data.referenceOptions)
        categories.clear(); categories.addAll(data.categories)
        payments.clear(); payments.addAll(data.payments)
        transferPayment = data.transferPayment
        dateMigrationVersion = data.dateMigrationVersion
        transactionOrder = data.transactionOrder
        anticipation = data.anticipation; calendarToday = java.time.LocalDate.now()
        rules.clear(); rules.addAll(data.rules); recurrenceEpoch++; recurrenceBlocked = false
        openings = data.openings.toList()
        accountNames.clear(); accountNames.addAll(data.accountNames)
        reconciliation = null
        accountDraft = null
        draft = data.draft?.let {
            if (it.date.isEmpty() && it.editingId == null) it.copy(date = java.time.LocalDate.now().toString()) else it
        }
        initialized = true
        account.intValue = data.draft?.account ?: 0
    }

    internal fun clearSensitive() {
        searchQuery = ""
        accountPeriod = AccountPeriod()
        operationsMenu = OperationsMenu.NONE
        transactionOrder = TransactionOrder.ORIGINAL
        dateMigrationVersion = 0
        onChange = null
        rules.clear(); recurrenceEpoch++; recurrenceBlocked = false
        anticipation = Anticipation.DUE; calendarToday = java.time.LocalDate.now()
        accountNames.clear(); accountDraft = null; reconciliation = null
        referenceUiStates.clear()
        referenceOptions.clear()
        transactions.clear(); categories.clear(); payments.clear(); draft = null; transferPayment = null
        openings = listOf(BigDecimal("500.00"), BigDecimal("3000.00"))
        destination.intValue = 0; account.intValue = 0; initialized = false; limitReached = false
    }

    internal fun operationFieldsChanged(locale: Locale): Boolean {
        val current = draft ?: return false
        val row = transactions.firstOrNull { it.id == current.editingId } ?: return true
        val amount = Money.parseAmount(current.amount, locale) ?: return true
        val signed = if (current.income) amount else amount.negate()
        val target = row.peerId?.let { id -> transactions.firstOrNull { it.id == id }?.account }
        return current.account != row.account || signed.compareTo(row.amount) != 0 ||
            current.description.trim() != row.description || current.category != row.category || current.payment != row.payment ||
            current.date.ifEmpty { row.date } != row.date || current.transferTarget != target || current.addingCategory || current.addingPayment
    }

    internal fun needsSeriesScope(locale: Locale): Boolean {
        val row = transactions.firstOrNull { it.id == draft?.editingId } ?: return false
        return row.seriesId != null && operationFieldsChanged(locale)
    }

    internal fun seriesOnlyEdit(locale: Locale): Boolean {
        val current = draft ?: return false
        val row = transactions.firstOrNull { it.id == current.editingId } ?: return false
        val rule = rules.firstOrNull { it.id == row.seriesId } ?: return false
        val remaining = rule.count?.let { (it - rule.next).coerceAtLeast(0) }
        val countChanged = if (current.repeatCount.isBlank()) remaining != null else current.repeatCount.toIntOrNull() == null || current.repeatCount.toIntOrNull() != remaining
        return !operationFieldsChanged(locale) && (current.repeatFrequency != rule.frequency || current.repeatUntil != rule.until.orEmpty() || countChanged)
    }

    private fun saveSeriesOnly(today: java.time.LocalDate): Boolean {
        val current = draft ?: return false
        val row = transactions.firstOrNull { it.id == current.editingId } ?: return false
        val index = rules.indexOfFirst { it.id == row.seriesId }
        if (index < 0) return false
        val revised = SeriesRevision.updateSettings(rules[index], current, row, transactions.toList(), today) ?: run {
            draft = current.copy(attempted = true); return false
        }
        val nextRules = rules.toMutableList().also { it[index] = revised }
        val checked = SnapshotCodec.encode(exportData().copy(rules = nextRules, draft = null)); checked.fill(0)
        rules.clear(); rules.addAll(nextRules)
        draft = null; recurrenceEpoch++; onChange?.invoke()
        return true
    }

    internal fun saveEntry(locale: Locale, date: String, scope: SeriesEditScope = SeriesEditScope.CURRENT,
        today: java.time.LocalDate = java.time.LocalDate.now()): Boolean {
        val current = draft ?: return false
        if (seriesOnlyEdit(locale)) return saveSeriesOnly(today)
        val old = transactions.firstOrNull { it.id == current.editingId }
        val additional = if (current.editingId == null) (if (current.transferTarget == null) 1 else 2)
            else if (current.transferTarget != null && old?.peerId == null) 1 else 0
        if (transactions.size + additional > LedgerLimits.MAX_TRANSACTIONS) { limitReached = true; return false }
        val amount = Money.parseAmount(current.amount, locale)
        if (amount == null || current.description.isBlank() || current.account !in openings.indices ||
            current.category !in categories || current.payment !in payments ||
            (old == null && (current.category !in referenceChoices(true, current.account, current.income) ||
                current.payment !in referenceChoices(false, current.account, current.income))) || current.addingCategory || current.addingPayment ||
            (current.date.isNotEmpty() && OperationDates.parse(current.date) == null)) {
            draft = current.copy(attempted = true)
            return false
        }
        val index = current.editingId?.let { id -> transactions.indexOfFirst { it.id == id } }
        // A missing target must never turn an edit into a new operation.
        if (index != null && index < 0) return false
        val previous = index?.let { transactions[it] }
        val preparing = previous?.let { RecurringState.of(it, calendarToday) == RecurringState.FORECAST } == true
        val peer = previous?.peerId?.let { peerId -> transactions.firstOrNull { it.id == peerId } }
        val target = current.transferTarget
        if (previous?.peerId != null && target == null) return false
        if (target != null && transferPayment != null && !isTransferPayment(current.payment)) return false
        if (target != null && (target !in openings.indices || target == current.account ||
                (previous?.peerId != null && (peer == null || previous.account != current.account || peer.account != target || current.income != (previous.amount.signum() > 0))))) {
            draft = current.copy(attempted = true); return false
        }
        val nextId = (transactions.maxOfOrNull { it.id } ?: 0) + 1
        if (previous == null && (nextId <= 0 || nextId >= Long.MAX_VALUE - 1)) return false
        if (previous != null && target != null && peer == null && (nextId <= 0 || nextId == Long.MAX_VALUE)) return false
        val rowId = previous?.id ?: nextId
        val peerId = if (target == null) null else peer?.id ?: (if (previous == null) nextId + 1 else nextId)
        var row = DemoTransaction(rowId,
            current.account, current.description.trim(), if (current.income) amount else amount.negate(),
            current.category, current.payment,
            current.date.ifEmpty { previous?.date ?: OperationDates.parse(date)?.toString() ?: return false },
            previous?.cleared ?: false, peerId, previous?.seriesId, previous?.occurrence, !preparing, preparing || previous?.readyToConfirm == true)
        val nextRules = rules.toMutableList()
        val oldRule = previous?.seriesId?.let { id -> nextRules.firstOrNull { it.id == id } }
        if (oldRule == null && current.repeatFrequency != RepeatFrequency.NONE) {
            val anchor = OperationDates.parse(row.date)
            val until = current.repeatUntil.takeIf { it.isNotBlank() }?.let(OperationDates::parse)
            val count = current.repeatCount.takeIf { it.isNotBlank() }?.toIntOrNull()
            if (anchor == null || (current.repeatUntil.isNotBlank() && (until == null || until < anchor)) ||
                (current.repeatCount.isNotBlank() && (count == null || count !in 1..10_000)) ||
                (current.repeatUntil.isNotBlank() && current.repeatCount.isNotBlank()) || nextRules.size >= 500) {
                draft = current.copy(attempted = true); return false
            }
            val id = java.util.UUID.randomUUID().toString()
            nextRules.add(RecurringRule(id, row.date, current.repeatFrequency, row.account, target, row.amount,
                row.description, row.category, row.payment, until = until?.toString(), count = count))
            row = row.copy(seriesId = id, occurrence = 0)
        } else if (oldRule != null) {
            var revised = oldRule
            if (scope == SeriesEditScope.SERIES) {
                revised = SeriesRevision.update(oldRule, current, row, transactions.toList(), today) ?: run {
                    draft = current.copy(attempted = true); return false
                }
            }
            if (row.amountConfirmed) revised = revised.copy(confirmedThrough = maxOf(revised.confirmedThrough, requireNotNull(row.occurrence)))
            nextRules[nextRules.indexOf(oldRule)] = revised
        }
        val nextRows = transactions.toMutableList()
        if (index == null) nextRows.add(row) else nextRows[index] = row
        if (target != null) {
            val counterpart = row.copy(id = requireNotNull(peerId), account = target, amount = row.amount.negate(),
                cleared = peer?.cleared ?: false, peerId = row.id)
            val peerIndex = peer?.let { old -> nextRows.indexOfFirst { it.id == old.id } }
            if (peerIndex == null) nextRows.add(counterpart) else nextRows[peerIndex] = counterpart
        }
        // Validate the complete pair before exposing it or invoking persistence.
        val checked = SnapshotCodec.encode(exportData().copy(transactions = nextRows, draft = null, rules = nextRules))
        checked.fill(0)
        transactions.clear(); transactions.addAll(nextRows)
        rules.clear(); rules.addAll(nextRules); recurrenceEpoch++
        account.intValue = current.account
        destination.intValue = 1
        draft = null
        onChange?.invoke()
        return true
    }
}

@Composable
internal fun money(amount: BigDecimal): String = NumberFormat.getCurrencyInstance(LocalConfiguration.current.locales[0]).apply {
    currency = Currency.getInstance("EUR")
}.format(amount)

@Composable
private fun DemoApp(vault: VaultController, activity: FragmentActivity) {
    val model: DemoViewModel = viewModel()
    val accountNames = model.accountNames
    val salary = stringResource(R.string.sample_salary)
    val groceries = stringResource(R.string.sample_groceries)
    val cafe = stringResource(R.string.sample_cafe)
    val deposit = stringResource(R.string.sample_savings)
    val foodCategory = stringResource(R.string.category_food)
    val salaryCategory = stringResource(R.string.category_salary)
    val leisureCategory = stringResource(R.string.category_leisure)
    val savingsCategory = stringResource(R.string.category_savings)
    val card = stringResource(R.string.payment_card)
    val transfer = stringResource(R.string.payment_transfer)
    val date = stringResource(R.string.sample_date)
    model.initialize(
        listOf(
            DemoTransaction(1, 0, salary, BigDecimal("2400.00"), salaryCategory, transfer, date, true),
            DemoTransaction(2, 0, groceries, BigDecimal("-46.80"), foodCategory, card, date, true),
            DemoTransaction(3, 0, cafe, BigDecimal("-3.50"), leisureCategory, card, date),
            DemoTransaction(4, 1, deposit, BigDecimal("250.00"), savingsCategory, transfer, date, true),
        )
    )
    model.ensureAccountNames(listOf(stringResource(R.string.current_account), stringResource(R.string.savings_account)))
    model.ensureTransferPayment(transfer)
    LaunchedEffect(model.recurrenceEpoch) {
        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            if (!vault.busy && model.reconciliation == null && model.draft == null) model.generateDue()
            kotlinx.coroutines.delay(60_000)
        }
    }
    val transactions = model.transactions
    var destination by model.destination
    var account by model.account
    val balances = accountNames.indices.map { model.balance(it) }
    val clearedBalances = accountNames.indices.map { model.balance(it, clearedOnly = true) }
    val labels = listOf(stringResource(R.string.accounts), stringResource(R.string.transactions), stringResource(R.string.settings))
    val symbols = listOf("◫", "≡", "⚙")

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp || LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (wide) {
                NavigationRail(containerColor = MaterialTheme.colorScheme.surface, modifier = Modifier.background(MaterialTheme.colorScheme.surface).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    Column(Modifier.width(112.dp).background(Color(0xFF142D36)).padding(horizontal = 8.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.brand), style = MaterialTheme.typography.labelLarge, color = Color.White)
                        TextButton(onClick = vault::openReleaseNotes) {
                            Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.labelSmall, color = Color(0xFFB9CEC8))
                        }
                    }
                    labels.forEachIndexed { index, label ->
                        NavigationRailItem(
                            selected = destination == index,
                            onClick = { model.operationsMenu = OperationsMenu.NONE; destination = index },
                            icon = { Text(symbols[index]) },
                            label = { Text(label) },
                        )
                    }
                }
            }
            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (!wide) NavigationBar {
                        labels.forEachIndexed { index, label ->
                            NavigationBarItem(
                                selected = destination == index,
                                onClick = { model.operationsMenu = OperationsMenu.NONE; destination = index },
                                icon = { Text(symbols[index]) },
                                label = { Text(label) },
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (destination == 0) ExtendedFloatingActionButton(onClick = { model.beginEntry() }) {
                        Text(stringResource(R.string.enter_transaction))
                    }
                },
            ) { padding ->
                Column(Modifier.padding(padding).fillMaxSize()) {
                    if (!wide && destination != 1) BrandHeader(vault::openReleaseNotes)
                    when (destination) {
                        0 -> AccountsScreen(accountNames, balances, clearedBalances,
                            onConfigure = { model.beginAccount(it, activity.resources.configuration.locales[0]) },
                            onAdd = { model.beginAccount(null, activity.resources.configuration.locales[0]) },
                            onAccount = { selected -> account = selected; destination = 1 })
                        1 -> TransactionsScreen(accountNames, account, balances[account], clearedBalances[account], model.transactionOrder.arrange(transactions.filter { it.account == account }),
                            onAccount = { account = it }, onToggle = model::toggleCleared,
                            onReconcile = { model.beginReconciliation(account) },
                            onEdit = { model.editEntry(it, activity.resources.configuration.locales[0]) },
                            onNew = { model.beginEntry() })
                        else -> SettingsScreen(vault, activity)
                    }
                }
            }
        }
    }
    if (model.draft != null) EntryDialog(accountNames, model)
    if (model.accountDraft != null) AccountDialog(model)
    if (model.reconciliation != null) ReconciliationDialog(model, accountNames)
    if (vault.showReleaseNotes && vault.feedback == null) ReleaseNotesDialog(vault)
}

@Composable
private fun BrandHeader(onVersion: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFF142D36)).padding(horizontal = 24.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.brand), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.tagline), color = Color(0xFFB9CEC8), style = MaterialTheme.typography.bodyMedium)
        }
        Surface(onClick = onVersion, color = Color(0xFF29434A), shape = RoundedCornerShape(12.dp)) {
            Text(BuildConfig.VERSION_NAME, modifier = Modifier.padding(10.dp), color = Color(0xFFDAEBDF), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun AccountsScreen(names: List<String>, balances: List<BigDecimal>, clearedBalances: List<BigDecimal>, onConfigure: (Int) -> Unit, onAdd: () -> Unit, onAccount: (Int) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text(stringResource(R.string.overview), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF206859)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp)) {
                    Text(stringResource(R.string.total_balance), color = Color(0xFFDAEBDF))
                    Text(money(balances.fold(BigDecimal.ZERO, BigDecimal::add)), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(stringResource(R.string.consolidated_cleared, money(clearedBalances.fold(BigDecimal.ZERO, BigDecimal::add))), color = Color(0xFFDAEBDF), modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.your_accounts), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onAdd, enabled = names.size < 50) { Text(stringResource(R.string.account_add)) }
            }
        }
        items(names.indices.toList()) { index ->
            Card(Modifier.fillMaxWidth().clickable { onAccount(index) }) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(names[index], style = MaterialTheme.typography.titleMedium)
                        Text("EUR", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.total_short, money(balances[index])), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.cleared_short, money(clearedBalances[index])), style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onConfigure(index) }) {
                        Icon(painterResource(R.drawable.ic_edit_account), contentDescription = stringResource(R.string.account_configure, names[index]))
                    }
                }
            }
        }
        item { Text(stringResource(R.string.account_hint), color = MaterialTheme.colorScheme.secondary) }
    }
}

@Composable
private fun SettingsScreen(vault: VaultController, activity: FragmentActivity) {
    val model: DemoViewModel = viewModel()
    var showReferences by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    if (showReferences) ReferenceManagementDialog(model) { model.referenceUiStates.clear(); showReferences = false }
    var showRules by remember { mutableStateOf(false) }
    if (showRules) RecurringRulesDialog(model) { showRules = false }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = { showReferences = true }, enabled = !vault.busy) { Text(stringResource(R.string.refs_title)) }
        OutlinedButton(onClick = { showRules = true }) { Text(stringResource(R.string.repeat_manage)) }
        if (model.recurrenceBlocked) Text(stringResource(R.string.repeat_blocked), color = MaterialTheme.colorScheme.error)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.order_title), style = MaterialTheme.typography.titleMedium)
                TransactionOrder.entries.forEach { order ->
                    Row(Modifier.fillMaxWidth().toggleable(value = model.transactionOrder == order,
                        enabled = !vault.busy, role = androidx.compose.ui.semantics.Role.RadioButton,
                        onValueChange = { model.setTransactionOrder(order) }), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = model.transactionOrder == order, onClick = null, enabled = !vault.busy)
                        Text(stringResource(when (order) {
                            TransactionOrder.ORIGINAL -> R.string.order_original
                            TransactionOrder.NEWEST -> R.string.order_newest
                            TransactionOrder.OLDEST -> R.string.order_oldest
                        }))
                    }
                }
                Text(stringResource(R.string.order_hint), style = MaterialTheme.typography.bodySmall)
            }
        }
        AnticipationSettings(model, enabled = !vault.busy)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.date_migration_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.date_migration_hint), style = MaterialTheme.typography.bodySmall)
                val unresolved = model.transactions.count { OperationDates.parse(it.date) == null && OperationDates.forOrdering(it.date) == null }
                if (unresolved > 0) Text(stringResource(R.string.date_migration_unresolved, unresolved), color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = vault::migrateDates, enabled = !vault.busy) { Text(stringResource(R.string.date_migration_action)) }
            }
        }
        VaultSettings(vault, activity)
        InfoCard(R.string.security_title, R.string.security_detail)
        InfoCard(R.string.privacy_title, R.string.privacy_detail)
        InfoCard(R.string.roadmap_title, R.string.roadmap_detail)
        Text(stringResource(R.string.language_hint), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = vault::openReleaseNotes) { Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable
private fun InfoCard(title: Int, body: Int) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PauseScreen(onResume: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = Color(0xFFF7F7F0)) {
        Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.pause_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.pause_detail))
            Spacer(Modifier.height(24.dp))
            Button(onClick = onResume) { Text(stringResource(R.string.resume)) }
        }
    }
}

@Composable
private fun EntryDialog(names: List<String>, model: DemoViewModel) {
    val draft = model.draft ?: return
    val locale = LocalConfiguration.current.locales[0]
    var confirmDelete by remember { mutableStateOf(false) }
    val toDelete = model.transactions.firstOrNull { it.id == draft.editingId }
    Dialog(
        onDismissRequest = model::cancelEntry,
        properties = DialogProperties(
            securePolicy = SecureFlagPolicy.SecureOn,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        // Own the available window, including IME insets, instead of the narrow
        // platform alert width. Re-evaluated on rotation and window resizing.
        BoxWithConstraints(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            val twoColumns = maxWidth >= 600.dp
            Surface(
                modifier = Modifier.widthIn(max = 1200.dp).fillMaxSize(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(if (draft.editingId == null) R.string.new_transaction else R.string.edit_transaction),
                            modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        if (draft.editingId != null) IconButton(onClick = { confirmDelete = true }) {
                            Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.delete_action),
                                tint = MaterialTheme.colorScheme.error)
                        }
                        if (twoColumns) EntryActions(model, draft, locale)
                    }
                    if (!twoColumns) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically) { EntryActions(model, draft, locale) }
                    HorizontalDivider()
                    // Account navigation stays narrow; the form owns the remaining width.
                    if (twoColumns) {
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.width(144.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.accounts), style = MaterialTheme.typography.labelLarge)
                                EntryAccounts(names, model, draft)
                            }
                            VerticalDivider()
                            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    EntryDirection(model, draft)
                                    EntryDate(model, draft)
                                }
                                EntryAmounts(model, draft, compact = true)
                                EntryReferences(model, draft, compact = true)
                                EntryRepeat(model, draft)
                                if (model.limitReached) Text(stringResource(R.string.vault_data_limit), color = MaterialTheme.colorScheme.error)
                                if (draft.attempted) Text(stringResource(if (draft.repeatFrequency == RepeatFrequency.NONE) R.string.invalid_amount else R.string.repeat_invalid), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            EntryBasics(names, model, draft)
                            EntryReferences(model, draft)
                            EntryRepeat(model, draft)
                            if (model.limitReached) Text(stringResource(R.string.vault_data_limit), color = MaterialTheme.colorScheme.error)
                                if (draft.attempted) Text(stringResource(if (draft.repeatFrequency == RepeatFrequency.NONE) R.string.invalid_amount else R.string.repeat_invalid), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete && toDelete != null) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.delete_transaction)) },
        text = {
            Column {
                Text(stringResource(if (toDelete.peerId == null) R.string.delete_transaction_warning else R.string.transfer_delete_warning, toDelete.description, money(toDelete.amount)))
                if (toDelete.seriesId != null) Text(stringResource(R.string.repeat_delete_hint))
            }
        },
        confirmButton = { TextButton(onClick = { model.deleteEntry(toDelete.id) }) { Text(stringResource(R.string.delete_action)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
    )
}

@Composable
private fun EntryAccounts(names: List<String>, model: DemoViewModel, draft: EntryDraft, horizontal: Boolean = false) {
    if (horizontal) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            names.forEachIndexed { index, name ->
                FilterChip(selected = draft.account == index,
                    onClick = { model.changeDraftAccount(index) }, enabled = draft.editingId == null || draft.transferTarget == null,
                    label = { Text(name, maxLines = 1) })
            }
        }
    } else {
        names.forEachIndexed { index, name -> FilterChip(selected = draft.account == index, onClick = { model.changeDraftAccount(index) }, enabled = draft.editingId == null || draft.transferTarget == null, modifier = Modifier.fillMaxWidth(), label = { Text(name) }) }
    }
}

@Composable
private fun EntryDirection(model: DemoViewModel, draft: EntryDraft) {
    if (draft.transferTarget != null) {
        Text(stringResource(if (draft.income) R.string.transfer_received else R.string.transfer_sent))
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = !draft.income, onClick = { model.updateDraft(draft.copy(income = false)) }, label = { Text(stringResource(R.string.expense)) })
        FilterChip(selected = draft.income, onClick = { model.updateDraft(draft.copy(income = true)) }, label = { Text(stringResource(R.string.income)) })
    }
}

@Composable
private fun EntryAmounts(model: DemoViewModel, draft: EntryDraft, compact: Boolean = false) {
    if (compact) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EntryAmount(model, draft, Modifier.weight(0.38f))
            EntryDescription(model, draft, Modifier.weight(0.62f))
        }
    } else {
        EntryAmount(model, draft, Modifier.fillMaxWidth())
        EntryDescription(model, draft, Modifier.fillMaxWidth())
    }
}

@Composable
private fun EntryAmount(model: DemoViewModel, draft: EntryDraft, modifier: Modifier) {
    OutlinedTextField(value = draft.amount, onValueChange = { model.updateDraft(draft.copy(amount = it.take(30))) }, label = { Text(stringResource(R.string.amount)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = modifier)
}

@Composable
private fun EntryDescription(model: DemoViewModel, draft: EntryDraft, modifier: Modifier) {
    OutlinedTextField(value = draft.description, onValueChange = { model.updateDraft(draft.copy(description = it.take(100))) }, label = { Text(stringResource(R.string.description)) }, singleLine = true, modifier = modifier)
}

@Composable
private fun EntryBasics(names: List<String>, model: DemoViewModel, draft: EntryDraft) {
    EntryAccounts(names, model, draft, horizontal = true)
    EntryDirection(model, draft)
    EntryDate(model, draft)
    EntryAmounts(model, draft)
}

@Composable
private fun EntryReferences(model: DemoViewModel, draft: EntryDraft, compact: Boolean = false) {
    if (compact && !model.isTransferPayment(draft.payment)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) { EntryCategory(model, draft) }
            Box(Modifier.weight(1f)) { EntryPayment(model, draft) }
        }
    } else {
        EntryCategory(model, draft)
        EntryPayment(model, draft)
    }
}

@Composable
private fun EntryCategory(model: DemoViewModel, draft: EntryDraft) {
    if (draft.transferTarget != null) {
        Text(stringResource(R.string.transfer_category_excluded), style = MaterialTheme.typography.bodySmall)
        return
    }
    ReferencePicker(
        label = stringResource(R.string.category), selected = draft.category,
        options = model.referenceChoices(true, draft.account, draft.income, if (draft.editingId != null) draft.category else ""), onSelect = { model.updateDraft(draft.copy(category = it)) },
        adding = draft.addingCategory, newName = draft.newCategory,
        onBeginAdd = { model.updateDraft(draft.copy(addingCategory = true, newCategory = "")) },
        onNameChange = { model.updateDraft(draft.copy(newCategory = it.take(60))) },
        onCancelAdd = { model.updateDraft(draft.copy(addingCategory = false, newCategory = "")) },
        onAdd = {
            model.addCategory(draft.newCategory)?.let {
                model.updateDraft(draft.copy(category = it, addingCategory = false, newCategory = ""))
            }
        },
    )
}

@Composable
private fun EntryPayment(model: DemoViewModel, draft: EntryDraft) {
    val linked = model.transactions.firstOrNull { it.id == draft.editingId }?.peerId != null
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f)) {
            ReferencePicker(
                label = stringResource(R.string.payment), selected = draft.payment,
                options = if (linked) model.payments.filter(model::isTransferPayment) else model.referenceChoices(false, draft.account, draft.income, if (draft.editingId != null) draft.payment else ""),
                onSelect = model::selectDraftPayment,
                adding = draft.addingPayment, newName = draft.newPayment,
                onBeginAdd = { model.updateDraft(draft.copy(addingPayment = true, newPayment = "")) },
                onNameChange = { model.updateDraft(draft.copy(newPayment = it.take(60))) },
                onCancelAdd = { model.updateDraft(draft.copy(addingPayment = false, newPayment = "")) },
                onAdd = {
                    model.addPayment(draft.newPayment)?.let {
                        model.updateDraft(draft.copy(payment = it, transferTarget = null, addingPayment = false, newPayment = ""))
                    }
                }, allowAdd = !linked,
            )
        }
        if (model.isTransferPayment(draft.payment)) Box(Modifier.weight(1f)) { TransferTargetPicker(model, draft, linked) }
    }
    if (draft.transferTarget != null) Text(stringResource(if (linked) R.string.transfer_edit_hint else R.string.transfer_conversion_hint),
        style = MaterialTheme.typography.bodySmall)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReferencePicker(
    label: String, selected: String, options: List<String>, onSelect: (String) -> Unit,
    adding: Boolean, newName: String, onBeginAdd: () -> Unit, onNameChange: (String) -> Unit,
    onCancelAdd: () -> Unit, onAdd: () -> Unit, allowAdd: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), enabled = !adding) {
                Text(selected.ifBlank { stringResource(R.string.choose_reference) }, modifier = Modifier.weight(1f))
                Text("▾")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { value -> DropdownMenuItem(text = { Text(value) }, onClick = { onSelect(value); expanded = false }) }
                if (allowAdd) {
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text(stringResource(R.string.add_reference, label)) }, onClick = { expanded = false; onBeginAdd() })
                }
            }
        }
        if (adding) {
            OutlinedTextField(value = newName, onValueChange = onNameChange, label = { Text(stringResource(R.string.reference_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.reference_duplicate_hint), style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onAdd, enabled = newName.isNotBlank()) { Text(stringResource(R.string.add_and_select)) }
                TextButton(onClick = onCancelAdd) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}

@Composable
private fun ReleaseNotesDialog(vault: VaultController) {
    AlertDialog(
        onDismissRequest = vault::acknowledgeReleaseNotes,
        title = { Text(stringResource(R.string.release_notes_title, BuildConfig.VERSION_NAME)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.release_notes_changes))
                Text(stringResource(R.string.release_notes_test_title), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.release_notes_tests))
            }
        },
        confirmButton = { TextButton(onClick = vault::acknowledgeReleaseNotes) { Text(stringResource(R.string.release_notes_understood)) } },
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
    )
}

@Composable
private fun EntryActions(model: DemoViewModel, draft: EntryDraft, locale: Locale) {
    val keyboard = LocalSoftwareKeyboardController.current
    var chooseScope by remember { mutableStateOf(false) }
    val row = model.transactions.firstOrNull { it.id == draft.editingId }
    TextButton(onClick = model::cancelEntry) { Text(stringResource(R.string.cancel)) }
    Button(onClick = {
        keyboard?.hide()
        if (model.needsSeriesScope(locale)) chooseScope = true
        else model.saveEntry(locale, java.time.LocalDate.now().toString())
    }, enabled = !draft.addingCategory && !draft.addingPayment) {
        Text(stringResource(if (model.seriesOnlyEdit(locale)) R.string.repeat_save_series
            else if (row?.let { RecurringState.of(it, model.calendarToday) } == RecurringState.FORECAST) R.string.repeat_prepare_occurrence
            else if (row?.let { it.seriesId != null && !it.amountConfirmed } == true) R.string.repeat_confirm_occurrence else R.string.save))
    }
    if (chooseScope) Dialog(onDismissRequest = { chooseScope = false },
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn, usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.large) {
            Column(Modifier.heightIn(max = 600.dp).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.repeat_scope_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.repeat_scope_description))
                val rule = model.rules.firstOrNull { it.id == row?.seriesId }
                val amount = Money.parseAmount(draft.amount, locale)
                val proposed = if (rule != null && row != null && amount != null) SeriesRevision.update(rule, draft,
                    row.copy(date = draft.date.ifEmpty { row.date }, amount = if (draft.income) amount else amount.negate()),
                    model.transactions.toList(), java.time.LocalDate.now()) else null
                Text(stringResource(R.string.repeat_next, proposed?.nextDate()?.let { OperationDates.display(it.toString(), locale) }
                    ?: stringResource(if (proposed == null) R.string.repeat_check_parameters else R.string.repeat_stopped)))
                HorizontalDivider()
                TextButton(onClick = {
                    chooseScope = false
                    model.saveEntry(locale, java.time.LocalDate.now().toString(), SeriesEditScope.SERIES)
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.repeat_scope_series)) }
                TextButton(onClick = {
                    chooseScope = false
                    model.saveEntry(locale, java.time.LocalDate.now().toString(), SeriesEditScope.CURRENT)
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.repeat_scope_current)) }
                TextButton(onClick = { chooseScope = false }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}

@Composable
private fun AccountDialog(model: DemoViewModel) {
    val draft = model.accountDraft ?: return
    val locale = LocalConfiguration.current.locales[0]
    AlertDialog(onDismissRequest = model::cancelAccount,
        title = { Text(stringResource(if (draft.index == null) R.string.account_add else R.string.account_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = draft.name, onValueChange = { model.updateAccountDraft(draft.copy(name = it.take(60))) },
                    label = { Text(stringResource(R.string.account_name)) }, singleLine = true)
                OutlinedTextField(value = draft.opening, onValueChange = { model.updateAccountDraft(draft.copy(opening = it.take(30))) },
                    label = { Text(stringResource(R.string.account_opening)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Text(stringResource(R.string.account_opening_hint))
                if (draft.attempted) Text(stringResource(R.string.account_invalid), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = { model.saveAccount(locale) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = model::cancelAccount) { Text(stringResource(R.string.cancel)) } },
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
    )
}

@Composable
private fun ReconciliationDialog(model: DemoViewModel, names: List<String>) {
    val draft = model.reconciliation ?: return
    val locale = LocalConfiguration.current.locales[0]
    val difference = model.reconciliationDifference(locale)
    Dialog(onDismissRequest = model::cancelReconciliation,
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn,
            usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(8.dp),
            shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.reconcile_title, names[draft.account]), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.reconcile_hint), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = draft.target, onValueChange = model::reconciliationTarget,
                    label = { Text(stringResource(R.string.reconcile_target)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.reconcile_selected, money(requireNotNull(model.reconciliationBalance()))))
                Text(if (difference == null) stringResource(R.string.reconcile_invalid)
                    else stringResource(R.string.reconcile_difference, money(difference)),
                    color = if (difference?.signum() == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(draft.original, key = { it.id }) { row ->
                        Row(Modifier.fillMaxWidth().clickable { model.reconciliationToggle(row.id) }.padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = row.id in draft.selected, onCheckedChange = { model.reconciliationToggle(row.id) })
                            Column(Modifier.weight(1f)) {
                                Text(row.description)
                                Text(OperationDates.display(row.date, LocalConfiguration.current.locales[0]), style = MaterialTheme.typography.bodySmall)
                            }
                            Text(money(row.amount))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = model::cancelReconciliation) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { model.confirmReconciliation(locale) }, enabled = difference?.signum() == 0) {
                        Text(stringResource(R.string.reconcile_confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryDate(model: DemoViewModel, draft: EntryDraft) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    var picker by remember { mutableStateOf<DatePickerDialog?>(null) }
    DisposableEffect(Unit) { onDispose { picker?.dismiss() } }
    val legacy = model.transactions.firstOrNull { it.id == draft.editingId }?.date
    val shown = draft.date.ifEmpty { legacy.orEmpty() }
    Column {
        OutlinedButton(onClick = {
            val date = OperationDates.parse(draft.date) ?: java.time.LocalDate.now()
            picker?.dismiss()
            picker = DatePickerDialog(context, { _, year, month, day ->
                val current = model.draft
                if (current != null) model.updateDraft(current.copy(date = java.time.LocalDate.of(year, month + 1, day).toString()))
            }, date.year, date.monthValue - 1, date.dayOfMonth).apply {
                window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                show()
            }
        }) { Text(stringResource(R.string.operation_date, OperationDates.display(shown, locale))) }
        if (draft.date.isEmpty() && legacy != null) Text(stringResource(R.string.operation_date_legacy),
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TransferTargetPicker(model: DemoViewModel, draft: EntryDraft, linked: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(stringResource(R.string.transfer_target_label), style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = !linked, modifier = Modifier.fillMaxWidth()) {
                Text(draft.transferTarget?.let { model.accountNames[it] } ?: stringResource(R.string.transfer_external))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.transfer_external)) }, onClick = {
                    model.updateDraft(draft.copy(transferTarget = null)); expanded = false
                })
                model.accountNames.forEachIndexed { index, name ->
                    if (index != draft.account) DropdownMenuItem(text = { Text(name) }, onClick = {
                        model.updateDraft(draft.copy(transferTarget = index)); expanded = false
                    })
                }
            }
        }
    }
}

@Composable
private fun repeatLabel(frequency: RepeatFrequency): String = stringResource(when (frequency) {
    RepeatFrequency.NONE -> R.string.repeat_none
    RepeatFrequency.WEEK -> R.string.repeat_week
    RepeatFrequency.TWO_WEEKS -> R.string.repeat_two_weeks
    RepeatFrequency.MONTH -> R.string.repeat_month
    RepeatFrequency.MONTH_END -> R.string.repeat_month_end
    RepeatFrequency.TWO_MONTHS -> R.string.repeat_two_months
    RepeatFrequency.THREE_MONTHS -> R.string.repeat_three_months
    RepeatFrequency.FOUR_MONTHS -> R.string.repeat_four_months
    RepeatFrequency.SIX_MONTHS -> R.string.repeat_six_months
    RepeatFrequency.YEAR -> R.string.repeat_year
    RepeatFrequency.TWO_YEARS -> R.string.repeat_two_years
})

@Composable
internal fun EntryRepeat(model: DemoViewModel, draft: EntryDraft) {
    val row = model.transactions.firstOrNull { it.id == draft.editingId }
    val rule = model.rules.firstOrNull { it.id == row?.seriesId }
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.repeat_title), style = MaterialTheme.typography.labelLarge)
        if (rule != null) Text(stringResource(R.string.repeat_scope_hint), style = MaterialTheme.typography.bodySmall)
        Box {
            OutlinedButton(onClick = { expanded = true }) { Text(repeatLabel(draft.repeatFrequency)) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                RepeatFrequency.entries.forEach { frequency ->
                    DropdownMenuItem(text = { Text(repeatLabel(frequency)) }, onClick = {
                        model.updateDraft(draft.copy(repeatFrequency = frequency)); expanded = false
                    })
                }
            }
        }
        if (draft.repeatFrequency != RepeatFrequency.NONE) {
            Text(stringResource(if (rule == null) R.string.repeat_limits_hint else R.string.repeat_remaining_hint), style = MaterialTheme.typography.bodySmall)
            BoxWithConstraints {
                val stacked = maxWidth < 340.dp || LocalConfiguration.current.fontScale > 1.15f
                val countField: @Composable (Modifier) -> Unit = { fieldModifier ->
                    Box(fieldModifier.testTag("repeat-count-frame")) {
                    PinnedLabelField(value = draft.repeatCount,
                        onValueChange = { model.updateDraft(draft.copy(repeatCount = it, repeatUntil = if (it.isNotBlank()) "" else draft.repeatUntil)) },
                        label = stringResource(if (rule == null) R.string.repeat_count else R.string.repeat_remaining),
                        placeholder = stringResource(R.string.repeat_unlimited), maxLength = 5,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("repeat-count"))
                    }
                }
                if (stacked) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RepeatEndDate(model, draft)
                    countField(Modifier.fillMaxWidth())
                } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.weight(1f)) { RepeatEndDate(model, draft) }
                    countField(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RecurringRulesDialog(model: DemoViewModel, onDismiss: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.repeat_manage)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.repeat_resume_hint))
                if (model.recurrenceBlocked) Text(stringResource(R.string.repeat_blocked), color = MaterialTheme.colorScheme.error)
                if (model.rules.isEmpty()) Text(stringResource(R.string.repeat_empty))
                model.rules.forEach { rule ->
                    HorizontalDivider()
                    Text(rule.description, fontWeight = FontWeight.Bold)
                    Text(repeatLabel(rule.frequency))
                    Text(stringResource(R.string.repeat_next, rule.nextDate()?.let { OperationDates.display(it.toString(), locale) }
                        ?: stringResource(R.string.repeat_stopped)))
                    TextButton(onClick = { model.setSeriesActive(rule.id, !rule.active) },
                        enabled = rule.active || rule.copy(active = true).nextDate() != null) {
                        Text(stringResource(if (rule.active) R.string.repeat_pause else R.string.repeat_resume))
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } },
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn))
}

@Composable
private fun RepeatEndDate(model: DemoViewModel, draft: EntryDraft) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    var picker by remember { mutableStateOf<DatePickerDialog?>(null) }
    DisposableEffect(Unit) { onDispose { picker?.dismiss() } }
    val label = stringResource(R.string.repeat_end_label)
    val prompt = stringResource(R.string.repeat_choose_date_short)
    val value = draft.repeatUntil.takeIf { it.isNotEmpty() }?.let { OperationDates.display(it, locale) }.orEmpty()
    Column {
        Box(Modifier.fillMaxWidth().testTag("repeat-end")) {
            PinnedLabelField(value = value, onValueChange = {}, label = label, readOnly = true,
                placeholder = prompt, modifier = Modifier.fillMaxWidth().testTag("repeat-end-field").clearAndSetSemantics {})
            Spacer(Modifier.matchParentSize().clickable(role = androidx.compose.ui.semantics.Role.Button,
                onClickLabel = prompt, onClick = {
                    val start = OperationDates.parse(draft.date) ?: java.time.LocalDate.now()
                    val selected = (OperationDates.parse(draft.repeatUntil) ?: start).coerceAtLeast(start)
                    picker?.dismiss()
                    picker = DatePickerDialog(context, { _, year, month, day ->
                        model.selectRepeatEnd(java.time.LocalDate.of(year, month + 1, day).toString())
                    }, selected.year, selected.monthValue - 1, selected.dayOfMonth).apply {
                        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        datePicker.minDate = start.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        show()
                    }
                }).semantics { contentDescription = "$label: ${value.ifEmpty { prompt }}" })
        }
        if (draft.repeatUntil.isNotEmpty()) TextButton(onClick = { model.selectRepeatEnd(null) }) {
            Text(stringResource(R.string.repeat_clear_end))
        }
    }
}

@Composable
internal fun UpcomingDialog(model: DemoViewModel, account: Int, names: List<String>, onDismiss: () -> Unit) {
    var days by rememberSaveable { mutableIntStateOf(90) }
    var today by remember { mutableStateOf(java.time.LocalDate.now()) }
    val locale = LocalConfiguration.current.locales[0]
    LaunchedEffect(Unit) {
        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            today = java.time.LocalDate.now()
            kotlinx.coroutines.delay(60_000)
        }
    }
    val rules = model.rules.toList()
    val upcoming = remember(rules, account, today, days) { UpcomingOccurrences.project(rules, account, today, days) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn,
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(8.dp), shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.upcoming_title, names[account]), style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.upcoming_close)) }
                }
                Text(stringResource(R.string.upcoming_hint), style = MaterialTheme.typography.bodySmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 90, 365).forEach { horizon ->
                        FilterChip(selected = days == horizon, onClick = { days = horizon },
                            label = { Text(stringResource(R.string.upcoming_days, horizon)) })
                    }
                }
                Text(stringResource(R.string.upcoming_until, OperationDates.display(today.plusDays(days.toLong()).toString(), locale)),
                    style = MaterialTheme.typography.bodySmall)
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (upcoming.items.isEmpty()) item { Text(stringResource(R.string.upcoming_empty)) }
                    items(upcoming.items, key = { "${it.seriesId}:${it.index}" }) { item ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.description, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                    Text(money(item.amount), fontWeight = FontWeight.Bold)
                                }
                                Text(OperationDates.display(item.date.toString(), locale))
                                item.otherAccount?.let { other -> Text(stringResource(R.string.upcoming_transfer, names[other]),
                                    style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                    if (upcoming.truncated) item { Text(stringResource(R.string.upcoming_truncated)) }
                }
            }
        }
    }
}

@Composable
internal fun RecurringBadge(row: DemoTransaction, today: java.time.LocalDate) {
    val state = RecurringState.of(row, today) ?: return
    val color = when (state) {
        RecurringState.FORECAST -> MaterialTheme.colorScheme.secondaryContainer
        RecurringState.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
        RecurringState.CONFIRMED -> MaterialTheme.colorScheme.primaryContainer
    }
    val label = when (state) {
        RecurringState.FORECAST -> R.string.recurring_forecast
        RecurringState.PENDING -> R.string.recurring_pending
        RecurringState.CONFIRMED -> R.string.recurring_confirmed
    }
    Surface(color = color, shape = MaterialTheme.shapes.small) {
        Text(stringResource(label), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun AnticipationSettings(model: DemoViewModel, enabled: Boolean) {
    HorizontalDivider()
    Text(stringResource(R.string.anticipation_title), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.anticipation_hint), style = MaterialTheme.typography.bodySmall)
    Anticipation.entries.forEach { value ->
        val label = when (value) {
            Anticipation.DUE -> R.string.anticipation_due
            Anticipation.MONTH_END -> R.string.anticipation_month_end
            Anticipation.DAYS_15 -> R.string.anticipation_15
            Anticipation.DAYS_30 -> R.string.anticipation_30
        }
        Row(Modifier.fillMaxWidth().clickable(enabled = enabled) { model.setAnticipation(value) },
            verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = model.anticipation == value, onClick = { model.setAnticipation(value) }, enabled = enabled)
            Text(stringResource(label))
        }
    }
}
