package org.matirelire.android

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import org.matirelire.android.domain.Money

class MainActivity : ComponentActivity() {
    private var paused by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
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
                DemoApp(paused) { paused = false }
            }
        }
    }

    override fun onStop() {
        if (!isChangingConfigurations) paused = true
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
)

/** Memory only: survives rotation, never writes financial values into saved state. */
class DemoViewModel : ViewModel() {
    internal val transactions = mutableStateListOf<DemoTransaction>()
    internal val destination = mutableIntStateOf(0)
    internal val account = mutableIntStateOf(0)
    internal val openings = listOf(BigDecimal("500.00"), BigDecimal("3000.00"))
    internal val categories = mutableStateListOf<String>()
    internal val payments = mutableStateListOf<String>()
    internal var draft by mutableStateOf<EntryDraft?>(null)
        private set
    private var initialized = false

    internal fun initialize(samples: List<DemoTransaction>) {
        if (!initialized) {
            transactions.addAll(samples)
            samples.forEach { addCategory(it.category); addPayment(it.payment) }
            initialized = true
        }
    }

    internal fun balance(account: Int, clearedOnly: Boolean = false): BigDecimal =
        transactions.filter { it.account == account && (!clearedOnly || it.cleared) }
            .fold(openings[account]) { total, tx -> total + tx.amount }

    internal fun toggleCleared(id: Long) {
        val index = transactions.indexOfFirst { it.id == id }
        if (index >= 0) transactions[index] = transactions[index].copy(cleared = !transactions[index].cleared)
    }

    private fun addReference(items: MutableList<String>, text: String): String? {
        val name = text.trim().take(60)
        if (name.isBlank()) return null
        val existing = items.firstOrNull { it.equals(name, ignoreCase = true) }
        if (existing != null) return existing
        items.add(name)
        return name
    }

    internal fun addCategory(text: String): String? = addReference(categories, text)
    internal fun addPayment(text: String): String? = addReference(payments, text)

    internal fun beginEntry() {
        if (draft == null) draft = EntryDraft(account = account.intValue,
            category = categories.firstOrNull().orEmpty(), payment = payments.firstOrNull().orEmpty())
    }

    internal fun updateDraft(next: EntryDraft) { if (draft != null) draft = next.copy(attempted = false) }
    internal fun cancelEntry() { draft = null }

    internal fun saveEntry(locale: Locale, date: String): Boolean {
        val current = draft ?: return false
        val amount = Money.parseAmount(current.amount, locale)
        if (amount == null || current.description.isBlank() || current.account !in openings.indices ||
            current.category !in categories || current.payment !in payments || current.addingCategory || current.addingPayment) {
            draft = current.copy(attempted = true)
            return false
        }
        transactions.add(DemoTransaction((transactions.maxOfOrNull { it.id } ?: 0) + 1,
            current.account, current.description.trim(), if (current.income) amount else amount.negate(),
            current.category, current.payment, date))
        account.intValue = current.account
        destination.intValue = 1
        draft = null
        return true
    }
}

@Composable
private fun money(amount: BigDecimal): String = NumberFormat.getCurrencyInstance(LocalConfiguration.current.locales[0]).apply {
    currency = Currency.getInstance("EUR")
}.format(amount)

@Composable
private fun DemoApp(paused: Boolean, onResume: () -> Unit) {
    val model: DemoViewModel = viewModel()
    val accountNames = listOf(stringResource(R.string.current_account), stringResource(R.string.savings_account))
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
    val transactions = model.transactions
    var destination by model.destination
    var account by model.account
    val balances = accountNames.indices.map { model.balance(it) }
    val clearedBalances = accountNames.indices.map { model.balance(it, clearedOnly = true) }
    val labels = listOf(stringResource(R.string.accounts), stringResource(R.string.transactions), stringResource(R.string.settings))
    val symbols = listOf("◫", "≡", "⚙")
    if (paused) {
        PauseScreen(onResume)
        return
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (wide) {
                NavigationRail {
                    Spacer(Modifier.height(24.dp))
                    labels.forEachIndexed { index, label ->
                        NavigationRailItem(
                            selected = destination == index,
                            onClick = { destination = index },
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
                                onClick = { destination = index },
                                icon = { Text(symbols[index]) },
                                label = { Text(label) },
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (destination != 2) ExtendedFloatingActionButton(onClick = model::beginEntry) {
                        Text(stringResource(R.string.enter_transaction))
                    }
                },
            ) { padding ->
                Column(Modifier.padding(padding).fillMaxSize()) {
                    BrandHeader()
                    DemoBanner()
                    when (destination) {
                        0 -> AccountsScreen(accountNames, balances, clearedBalances) { selected -> account = selected; destination = 1 }
                        1 -> TransactionsScreen(accountNames, account, balances[account], clearedBalances[account], transactions.filter { it.account == account },
                            onAccount = { account = it }, onToggle = model::toggleCleared)
                        else -> SettingsScreen()
                    }
                }
            }
        }
    }
    if (model.draft != null) EntryDialog(accountNames, model)
}

@Composable
private fun BrandHeader() {
    Row(Modifier.fillMaxWidth().background(Color(0xFF142D36)).padding(horizontal = 24.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.brand), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.tagline), color = Color(0xFFB9CEC8), style = MaterialTheme.typography.bodyMedium)
        }
        Surface(color = Color(0xFF29434A), shape = RoundedCornerShape(12.dp)) {
            Text(BuildConfig.VERSION_NAME, modifier = Modifier.padding(10.dp), color = Color(0xFFDAEBDF), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun DemoBanner() {
    Column(Modifier.fillMaxWidth().background(Color(0xFFF4E8C9)).padding(horizontal = 24.dp, vertical = 10.dp)) {
        Text(stringResource(R.string.demo_banner), style = MaterialTheme.typography.labelMedium, color = Color(0xFF654E20), fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.demo_detail), style = MaterialTheme.typography.bodySmall, color = Color(0xFF654E20))
    }
}

@Composable
private fun AccountsScreen(names: List<String>, balances: List<BigDecimal>, clearedBalances: List<BigDecimal>, onAccount: (Int) -> Unit) {
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
        item { Text(stringResource(R.string.your_accounts), style = MaterialTheme.typography.titleMedium) }
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
                }
            }
        }
        item { Text(stringResource(R.string.account_hint), color = MaterialTheme.colorScheme.secondary) }
    }
}

@Composable
private fun TransactionsScreen(names: List<String>, account: Int, balance: BigDecimal, clearedBalance: BigDecimal, transactions: List<DemoTransaction>, onAccount: (Int) -> Unit, onToggle: (Long) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                names.forEachIndexed { index, name -> FilterChip(selected = index == account, onClick = { onAccount(index) }, label = { Text(name) }) }
            }
        }
        item {
            Text(stringResource(R.string.balance), color = MaterialTheme.colorScheme.secondary)
            Text(money(balance), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.cleared_balance), color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 8.dp))
            Text(money(clearedBalance), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        item { Text(stringResource(R.string.pointing_hint), style = MaterialTheme.typography.bodySmall) }
        if (transactions.isEmpty()) item { Text(stringResource(R.string.empty_transactions)) }
        items(transactions, key = { it.id }) { tx ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(tx.description, style = MaterialTheme.typography.titleMedium)
                            Text(tx.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        Text(money(tx.amount), fontWeight = FontWeight.Bold, color = if (tx.amount.signum() < 0) Color(0xFF944C3C) else MaterialTheme.colorScheme.primary)
                    }
                    Text("${tx.category} · ${tx.payment}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    Row(Modifier.fillMaxWidth().clickable { onToggle(tx.id) }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = tx.cleared, onCheckedChange = { onToggle(tx.id) })
                        Text(stringResource(if (tx.cleared) R.string.marked else R.string.unmarked), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        InfoCard(R.string.security_title, R.string.security_detail)
        InfoCard(R.string.privacy_title, R.string.privacy_detail)
        InfoCard(R.string.roadmap_title, R.string.roadmap_detail)
        Text(stringResource(R.string.language_hint), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelMedium)
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
    AlertDialog(
        onDismissRequest = model::cancelEntry,
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
        title = { Text(stringResource(R.string.new_transaction)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                names.forEachIndexed { index, name -> FilterChip(selected = draft.account == index, onClick = { model.updateDraft(draft.copy(account = index)) }, label = { Text(name) }) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !draft.income, onClick = { model.updateDraft(draft.copy(income = false)) }, label = { Text(stringResource(R.string.expense)) })
                    FilterChip(selected = draft.income, onClick = { model.updateDraft(draft.copy(income = true)) }, label = { Text(stringResource(R.string.income)) })
                }
                OutlinedTextField(value = draft.amount, onValueChange = { model.updateDraft(draft.copy(amount = it.take(30))) }, label = { Text(stringResource(R.string.amount)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(value = draft.description, onValueChange = { model.updateDraft(draft.copy(description = it.take(100))) }, label = { Text(stringResource(R.string.description)) }, singleLine = true)
                ReferencePicker(
                    label = stringResource(R.string.category), selected = draft.category,
                    options = model.categories, onSelect = { model.updateDraft(draft.copy(category = it)) },
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
                ReferencePicker(
                    label = stringResource(R.string.payment), selected = draft.payment,
                    options = model.payments, onSelect = { model.updateDraft(draft.copy(payment = it)) },
                    adding = draft.addingPayment, newName = draft.newPayment,
                    onBeginAdd = { model.updateDraft(draft.copy(addingPayment = true, newPayment = "")) },
                    onNameChange = { model.updateDraft(draft.copy(newPayment = it.take(60))) },
                    onCancelAdd = { model.updateDraft(draft.copy(addingPayment = false, newPayment = "")) },
                    onAdd = {
                        model.addPayment(draft.newPayment)?.let {
                            model.updateDraft(draft.copy(payment = it, addingPayment = false, newPayment = ""))
                        }
                    },
                )
                if (draft.attempted) Text(stringResource(R.string.invalid_amount), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(locale))
                model.saveEntry(locale, date)
            }, enabled = !draft.addingCategory && !draft.addingPayment) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = model::cancelEntry) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ReferencePicker(
    label: String, selected: String, options: List<String>, onSelect: (String) -> Unit,
    adding: Boolean, newName: String, onBeginAdd: () -> Unit, onNameChange: (String) -> Unit,
    onCancelAdd: () -> Unit, onAdd: () -> Unit,
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
                HorizontalDivider()
                DropdownMenuItem(text = { Text(stringResource(R.string.add_reference, label)) }, onClick = { expanded = false; onBeginAdd() })
            }
        }
        if (adding) {
            OutlinedTextField(value = newName, onValueChange = onNameChange, label = { Text(stringResource(R.string.reference_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.reference_duplicate_hint), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onAdd, enabled = newName.isNotBlank()) { Text(stringResource(R.string.add_and_select)) }
                TextButton(onClick = onCancelAdd) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}
