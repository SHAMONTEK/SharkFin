package com.example.sharkfin

import java.util.Date

// ── Output Types ──────────────────────────────────────────────────────────────

enum class TransactionIntent {
    EXPENSE, INCOME, RECURRING_EXPENSE, FUTURE_EXPENSE, FUTURE_INCOME,
    RENT_UPDATE, BILL_UPDATE, NOTE, RESET_DATA, SETUP_FINANCES,
    HUSTLE_INCOME, DEBT_PAYMENT, CORRECTION, UNCLEAR
}

data class ParsedTransaction(
    val intent          : TransactionIntent,
    val amount          : Double?,
    val amountIsApprox  : Boolean = false,
    val category        : String,
    val merchantHint    : String?,
    val tipAmount       : Double?,
    val isFuture        : Boolean = false,
    val recurringKey    : String?,
    val noteText        : String? = null,
    val rawInput        : String,
    val confidence      : Float,
    val needsConfirm    : Boolean = false,
    val calendarDate    : Long?  = null,
    val secondaryAmount : Double? = null,
    val taxWithholding  : Double? = null,
    val durationHours   : Double? = null
)

val defaultRecurringBills = listOf(
    RecurringBill("rent",        "Rent",              0.0,  "Bills & Utilities"),
    RecurringBill("car_note",    "Car Note",          0.0,  "Transportation"),
    RecurringBill("light",       "Light Bill",        0.0,  "Bills & Utilities"),
    RecurringBill("water",       "Water Bill",        0.0,  "Bills & Utilities"),
    RecurringBill("gas_bill",    "Gas Bill",          0.0,  "Bills & Utilities"),
    RecurringBill("internet",    "Internet",          0.0,  "Bills & Utilities"),
    RecurringBill("phone",       "Phone Bill",        0.0,  "Bills & Utilities"),
    RecurringBill("netflix",     "Netflix",           0.0,  "Subscriptions"),
    RecurringBill("hulu",        "Hulu",              0.0,  "Subscriptions"),
    RecurringBill("spotify",     "Spotify",           0.0,  "Subscriptions"),
    RecurringBill("apple",       "Apple Subscription",0.0,  "Subscriptions"),
    RecurringBill("car_ins",     "Car Insurance",     0.0,  "Insurance"),
    RecurringBill("court",       "Court Payment",     0.0,  "Other"),
    RecurringBill("probation",   "Probation Fee",     0.0,  "Other"),
    RecurringBill("gas_car",     "Gas",               0.0,  "Transportation")
)

private val INCOME_SIGNALS = listOf("made", "got", "received", "earned", "sent me", "gave me", "paid me", "deposited", "deposit", "payday")
private val HUSTLE_SIGNALS = listOf("dashed", "doordash", "ubered", "uber eats", "lyfted", "instacart", "hustle", "gig")
private val EXPENSE_SIGNALS = listOf("spent", "paid", "bought", "grabbed", "got", "picked up", "dropped", "charged", "swiped", "blew")
private val NUMBER_WORDS = mapOf("zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "ten" to 10, "twenty" to 20, "hundred" to 100, "thousand" to 1000)
private val APPROX_SIGNALS = listOf("about", "roughly", "around", "like", "maybe")

object AICoachNLP {

    private fun matchRecurringBill(input: String, known: List<RecurringBill>): String? {
        return known.find { bill -> input.contains(bill.label.lowercase()) || input.contains(bill.key.replace("_", " ")) }?.key
    }

    private fun extractDetailedAmounts(input: String): Triple<Double?, Double?, Double?> {
        val pattern = Regex("""\$?\s*(\d{1,9}(?:\.\d{1,2})?)""")
        val matches = pattern.findAll(input).mapNotNull { it.groupValues[1].toDoubleOrNull() }.toList()
        val primary = matches.getOrNull(0)
        val tip = if (input.contains("tip")) matches.getOrNull(1) else null
        val secondary = if (tip != null) matches.getOrNull(2) else matches.getOrNull(1)
        return Triple(primary, tip, secondary)
    }

    private fun extractDynamicMerchant(input: String): String? {
        val known = listOf("starbucks", "amazon", "target", "walmart", "doordash", "uber")
        known.forEach { if (input.contains(it)) return it.replaceFirstChar { c -> c.uppercase() } }
        val atPattern = Regex("""at\s+([A-Za-z\s]{2,15})""")
        return atPattern.find(input)?.groupValues?.get(1)?.trim()?.split(" ")?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    fun parse(rawInput: String, knownRecurring: List<RecurringBill> = defaultRecurringBills, currentSession: SharkAgentSession = SharkAgentSession.IDLE): ParsedTransaction {
        val input = rawInput.lowercase().trim()
        val (amt, tip, sec) = extractDetailedAmounts(input)
        val recurringKey = matchRecurringBill(input, knownRecurring)
        val merchant = extractDynamicMerchant(input)
        
        val intent = when {
            HUSTLE_SIGNALS.any { input.contains(it) } -> TransactionIntent.HUSTLE_INCOME
            INCOME_SIGNALS.any { input.contains(it) } -> TransactionIntent.INCOME
            recurringKey != null -> TransactionIntent.RECURRING_EXPENSE
            EXPENSE_SIGNALS.any { input.contains(it) } -> TransactionIntent.EXPENSE
            amt != null -> TransactionIntent.EXPENSE
            else -> TransactionIntent.UNCLEAR
        }

        val confidence = if (amt != null && intent != TransactionIntent.UNCLEAR) 0.8f else 0.4f

        return ParsedTransaction(
            intent = intent,
            amount = amt,
            category = if (intent == TransactionIntent.INCOME) "Income" else "Other",
            merchantHint = merchant,
            tipAmount = tip,
            rawInput = rawInput,
            confidence = confidence,
            secondaryAmount = sec,
            recurringKey = recurringKey,
            amountIsApprox = APPROX_SIGNALS.any { input.contains(it) },
            needsConfirm = confidence < 0.7f
        )
    }
}
