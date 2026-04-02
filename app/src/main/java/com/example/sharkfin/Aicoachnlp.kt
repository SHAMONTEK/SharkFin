package com.example.sharkfin

// ─────────────────────────────────────────────────────────────────────────────
// AICoachNLP.kt
// The brain. Takes raw voice/text input, extracts meaning, returns structured
// financial data that Shark can act on and log to Firestore.
// No API. No network. Pure Kotlin pattern matching.
// ─────────────────────────────────────────────────────────────────────────────

import java.util.Calendar

// ── Output Types ──────────────────────────────────────────────────────────────

enum class TransactionIntent {
    EXPENSE,            // user spent money
    INCOME,             // user received money
    RECURRING_EXPENSE,  // known bill paid (rent, car, etc.)
    FUTURE_EXPENSE,     // user is about to spend (not logged yet)
    FUTURE_INCOME,      // user expects money (not logged yet)
    RENT_UPDATE,        // rent amount changed
    BILL_UPDATE,        // any recurring bill amount changed
    NOTE,               // user just wants to log a note/reminder
    RESET_DATA,         // user wants to start over (wipe expenses/streak)
    SETUP_FINANCES,     // user is telling Shark their current cash/goals
    UNCLEAR             // parser couldn't figure it out
}

data class ParsedTransaction(
    val intent          : TransactionIntent,
    val amount          : Double?,           // null = uncertain, needs confirmation
    val amountIsApprox  : Boolean = false,   // "about", "roughly", "like"
    val category        : String,            // "Food", "Rent", "Income", etc.
    val merchantHint    : String?,           // "Chick-fil-A", "Apple", "parking"
    val tipAmount       : Double?,           // separate tip if mentioned
    val isFuture        : Boolean = false,   // "I'm about to..." "I'm gonna..."
    val recurringKey    : String?,           // matches a known recurring bill key
    val noteText        : String?,           // raw text for calendar note
    val rawInput        : String,            // original user input
    val confidence      : Float,            // 0.0–1.0 how confident the parse is
    val needsConfirm    : Boolean = false,   // true = ask user to confirm amount
    val calendarDate    : Long?  = null,     // if user mentioned a specific date
    val secondaryAmount : Double? = null     // used for "holding onto X, want to save Y"
)

// Default recurring bills — user customizes in settings, stored in Firestore
// In the app, load these from Firestore users/{uid}/recurringBills
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

// ── Keyword Banks ─────────────────────────────────────────────────────────────

private val INCOME_SIGNALS = listOf(
    "just made", "just got", "just received", "just earned",
    "sent me", "gave me", "paid me", "hit my account", "deposited",
    "program sent", "job gave", "check came", "direct deposit",
    "got paid", "my mom sent", "my dad sent", "my girl sent", "my boy sent",
    "somebody sent", "someone sent", "cash app", "zelle", "venmo",
    "just came in", "just dropped", "just landed", "just hit",
    "i made", "i got", "i received", "i earned", "bonus",
    "refund", "tax return", "stimulus", "grant", "scholarship"
)

private val EXPENSE_SIGNALS = listOf(
    "just spent", "just paid", "just bought", "just grabbed",
    "just got", "just picked up", "just dropped", "just dropped",
    "spent", "paid", "bought", "grabbed", "picked up",
    "took from me", "charged me", "charged", "deducted",
    "i spent", "i paid", "i bought", "i grabbed",
    "just copped", "just copped", "just swiped", "swiped",
    "dropped", "blew", "accidentally spent", "accidentally paid",
    "i need cash for", "i'm a put", "putting", "put in",
    "filled up", "topped off", "tipped", "left a tip"
)

private val RESET_SIGNALS = listOf(
    "start over", "reset everything", "wipe everything", "clear my history",
    "restart", "start fresh", "reset my streak", "delete my expenses"
)

private val SETUP_SIGNALS = listOf(
    "holding onto", "i have", "saved up", "want to save", "target is", "my goal is",
    "planning to save", "need to save", "holding on to"
)

private val FUTURE_SIGNALS = listOf(
    "about to", "gonna", "going to", "finna", "fixing to",
    "i'm a", "ima", "i will", "planning to", "thinking about",
    "might spend", "probably spend", "expect to", "anticipating"
)

private val FUTURE_INCOME_SIGNALS = listOf(
    "getting paid", "gonna get", "about to get", "expect",
    "should receive", "waiting on", "coming next", "next week",
    "on the", "on tuesday", "on friday", "on monday",
    "payday is", "check coming", "deposit coming"
)

private val RECURRING_KEYWORDS = mapOf(
    "rent"                          to "rent",
    "car note"                      to "car_note",
    "car payment"                   to "car_note",
    "car loan"                      to "car_note",
    "light bill"                    to "light",
    "lights"                        to "light",
    "electric"                      to "light",
    "electricity"                   to "light",
    "power bill"                    to "light",
    "water bill"                    to "water",
    "water"                         to "water",
    "gas bill"                      to "gas_bill",
    "internet"                      to "internet",
    "wifi"                          to "internet",
    "wi-fi"                         to "internet",
    "phone bill"                    to "phone",
    "phone"                         to "phone",
    "netflix"                       to "netflix",
    "hulu"                          to "hulu",
    "spotify"                       to "spotify",
    "apple"                         to "apple",
    "car insurance"                 to "car_ins",
    "insurance"                     to "car_ins",
    "court"                         to "court",
    "probation"                     to "probation",
    "gas"                           to "gas_car",
    "filled up"                     to "gas_car",
    "topped off"                    to "gas_car"
)

private val CATEGORY_KEYWORDS = mapOf(
    "grocery"       to "Groceries",
    "groceries"     to "Groceries",
    "food"          to "Food & Dining",
    "restaurant"    to "Food & Dining",
    "chick"         to "Food & Dining",
    "mcdonald"      to "Food & Dining",
    "burger"        to "Food & Dining",
    "pizza"         to "Food & Dining",
    "taco"          to "Food & Dining",
    "coffee"        to "Food & Dining",
    "starbucks"     to "Food & Dining",
    "uber eats"     to "Food & Dining",
    "doordash"      to "Food & Dining",
    "postmates"     to "Food & Dining",
    "parking"       to "Transportation",
    "uber"          to "Transportation",
    "lyft"          to "Transportation",
    "gas"           to "Gas",
    "clothes"       to "Shopping",
    "shoes"         to "Shopping",
    "amazon"        to "Shopping",
    "walmart"       to "Shopping",
    "target"        to "Shopping",
    "doctor"        to "Healthcare",
    "pharmacy"      to "Healthcare",
    "medicine"      to "Healthcare",
    "school"        to "Education",
    "tuition"       to "Education",
    "books"         to "Education",
    "bar"           to "Entertainment",
    "club"          to "Entertainment",
    "movie"         to "Entertainment",
    "game"          to "Entertainment",
    "concert"       to "Entertainment",
    "tip"           to "Food & Dining",
    "friend"        to "People I Owe",
    "boy"           to "People I Owe",
    "girl"          to "People I Owe",
    "mom"           to "People I Owe",
    "dad"           to "People I Owe",
    "lady"          to "People I Owe"
)

private val APPROX_SIGNALS = listOf(
    "about", "roughly", "like", "around", "approximately",
    "almost", "close to", "maybe", "probably", "bout", "'bout"
)

private val AMOUNT_ZERO_SIGNALS = listOf(
    "zero dollars", "zero", "free", "nothing", "no cost",
    "no charge", "waived", "forgiven", "doesn't cost"
)

// ── Number Word Map ───────────────────────────────────────────────────────────

private val NUMBER_WORDS = mapOf(
    "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
    "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
    "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13,
    "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17,
    "eighteen" to 18, "nineteen" to 19, "twenty" to 20, "thirty" to 30,
    "forty" to 40, "fifty" to 50, "sixty" to 60, "seventy" to 70,
    "eighty" to 80, "ninety" to 90, "hundred" to 100, "thousand" to 1000,
    "a hundred" to 100, "a thousand" to 1000, "hunnid" to 100,
    "buck" to 1, "bucks" to 1, "a buck" to 1
)

// ── Main Parser ───────────────────────────────────────────────────────────────

object AICoachNLP {

    fun parse(
        rawInput        : String,
        knownRecurring  : List<RecurringBill> = defaultRecurringBills,
        currentSession  : SharkAgentSession = SharkAgentSession.IDLE
    ): ParsedTransaction {

        val input = rawInput.lowercase().trim()

        // ── 1. Extract amount ──
        val amounts        = extractAllAmounts(input)
        val amount         = amounts.firstOrNull()
        val amountIsApprox = APPROX_SIGNALS.any { input.contains(it) }
        val tipAmount      = extractTip(input)

        // ── 2. Detect intent ──
        val isReset           = RESET_SIGNALS.any { input.contains(it) }
        val isSetup           = SETUP_SIGNALS.any { input.contains(it) } || 
                                (currentSession == SharkAgentSession.AWAITING_SETUP_BALANCE && amount != null)
        val isFuture          = FUTURE_SIGNALS.any { input.contains(it) }
        val isFutureIncome    = FUTURE_INCOME_SIGNALS.any { input.contains(it) }
        val isIncome          = INCOME_SIGNALS.any { input.contains(it) } && !isFuture
        val isExpense         = EXPENSE_SIGNALS.any { input.contains(it) }
        val isAmountZero      = AMOUNT_ZERO_SIGNALS.any { input.contains(it) }

        // ── 3. Check for recurring bill match ──
        val recurringKey = knownRecurring.firstOrNull { bill ->
            RECURRING_KEYWORDS.entries.any { (keyword, key) ->
                key == bill.key && input.contains(keyword)
            }
        }?.key ?: RECURRING_KEYWORDS.entries.firstOrNull { (keyword, _) ->
            input.contains(keyword)
        }?.value

        // ── 4. Check for bill update ──
        val isRentUpdate = input.contains("rent") &&
                (input.contains("update") || input.contains("changed") ||
                        input.contains("going to be") || input.contains("gonna be") ||
                        input.contains("is now") || input.contains("is zero") ||
                        input.contains("zero") || input.contains("free") ||
                        input.contains("waived") || isAmountZero)

        val isBillUpdate = recurringKey != null &&
                (input.contains("update") || input.contains("changed") ||
                        input.contains("now") || input.contains("going to be") ||
                        input.contains("gonna be") || isAmountZero) && !isExpense

        // ── 5. Detect category ──
        val category = when {
            recurringKey != null -> knownRecurring.firstOrNull { it.key == recurringKey }?.category ?: "Bills & Utilities"
            isIncome -> "Income"
            else -> detectCategory(input)
        }

        // ── 6. Extract merchant hint ──
        val merchantHint = extractMerchant(input)

        // ── 7. Detect calendar date mention ──
        val calendarDate = extractFutureDate(input)

        // ── 8. Determine final intent ──
        val intent = when {
            isReset                    -> TransactionIntent.RESET_DATA
            isSetup                    -> TransactionIntent.SETUP_FINANCES
            isRentUpdate               -> TransactionIntent.RENT_UPDATE
            isBillUpdate               -> TransactionIntent.BILL_UPDATE
            isFutureIncome             -> TransactionIntent.FUTURE_INCOME
            isFuture && !isIncome      -> TransactionIntent.FUTURE_EXPENSE
            isIncome                   -> TransactionIntent.INCOME
            recurringKey != null && isExpense -> TransactionIntent.RECURRING_EXPENSE
            isExpense                  -> TransactionIntent.EXPENSE
            amount != null             -> TransactionIntent.EXPENSE  // has amount, assume expense
            else                       -> TransactionIntent.UNCLEAR
        }

        // ── 9. Calculate confidence ──
        val confidence = calculateConfidence(
            intent         = intent,
            amount         = amount,
            amountIsApprox = amountIsApprox,
            recurringKey   = recurringKey,
            isAmountZero   = isAmountZero
        )

        // ── 10. Needs confirmation? ──
        val needsConfirm = amount == null ||
                amountIsApprox ||
                intent == TransactionIntent.UNCLEAR ||
                intent == TransactionIntent.RESET_DATA ||
                (intent == TransactionIntent.FUTURE_EXPENSE && amount != null) ||
                confidence < 0.6f

        return ParsedTransaction(
            intent         = intent,
            amount         = if (isAmountZero) 0.0 else amount,
            amountIsApprox = amountIsApprox,
            category       = category,
            merchantHint   = merchantHint,
            tipAmount      = tipAmount,
            isFuture       = isFuture || isFutureIncome,
            recurringKey   = recurringKey,
            noteText       = if (intent == TransactionIntent.FUTURE_INCOME ||
                intent == TransactionIntent.FUTURE_EXPENSE) rawInput else null,
            rawInput       = rawInput,
            confidence     = confidence,
            needsConfirm   = needsConfirm,
            calendarDate   = calendarDate,
            secondaryAmount = if (amounts.size > 1) amounts[1] else null
        )
    }

    private fun extractAllAmounts(input: String): List<Double> {
        val found = mutableListOf<Double>()
        // Improved Regex: Supports up to 9 digits, commas, and decimals. No more "1000 -> 100" truncation.
        val pattern = Regex("""\$?\s*(\d{1,9}(?:,\d{3})*(?:\.\d{1,2})?)""")
        pattern.findAll(input).forEach { match ->
            val clean = match.groupValues[1].replace(",", "")
            clean.toDoubleOrNull()?.let { found.add(it) }
        }
        
        if (found.isEmpty()) {
            parseWordNumber(input)?.let { found.add(it) }
        }
        
        return found
    }

    private fun parseWordNumber(input: String): Double? {
        var result = 0.0
        var current = 0.0
        var found = false

        val words = input.split(Regex("\\s+|[-]"))

        for (i in words.indices) {
            val word = words[i].replace(",", "").replace(".", "")
            val num  = NUMBER_WORDS[word]

            if (num != null) {
                found = true
                when {
                    num == 100  -> current = if (current == 0.0) 100.0 else current * 100
                    num == 1000 -> { result += current * 1000; current = 0.0 }
                    else        -> current += num
                }
            }
        }

        if (!found) return null
        result += current
        return if (result > 0) result else null
    }

    private fun extractTip(input: String): Double? {
        val tipPatterns = listOf(
            Regex("""tip(?:ped)?\s+(?:the\s+)?(?:lady|guy|server|waiter|waitress)?\s*\$?\s*(\d+(?:\.\d{1,2})?)"""),
            Regex("""left\s+(?:a\s+)?(?:the\s+)?tip\s+of\s+\$?\s*(\d+(?:\.\d{1,2})?)"""),
            Regex("""gave\s+(?:the\s+)?(?:lady|guy|server)?\s*\$?\s*(\d+(?:\.\d{1,2})?)\s+tip""")
        )
        for (pattern in tipPatterns) {
            pattern.find(input)?.let { return it.groupValues[1].toDoubleOrNull() }
        }
        return null
    }

    // ── Category Detection ────────────────────────────────────────────────────

    private fun detectCategory(input: String): String {
        for ((keyword, category) in CATEGORY_KEYWORDS) {
            if (input.contains(keyword)) return category
        }
        return "Other"
    }

    // ── Merchant Extraction ───────────────────────────────────────────────────

    private fun extractMerchant(input: String): String? {
        val knownMerchants = listOf(
            "chick-fil-a", "chick fil a", "mcdonald's", "mcdonalds",
            "burger king", "wendy's", "wendys", "taco bell",
            "starbucks", "dunkin", "chipotle", "subway",
            "amazon", "walmart", "target", "costco",
            "uber eats", "doordash", "postmates", "grubhub",
            "apple", "netflix", "spotify", "hulu",
            "uber", "lyft", "shell", "exxon", "bp", "chevron",
            "walgreens", "cvs", "publix", "kroger", "aldi"
        )
        for (merchant in knownMerchants) {
            if (input.contains(merchant)) return merchant.split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }

        // try to extract proper noun after "at" or "from"
        val atPattern = Regex("""(?:at|from|@)\s+([A-Za-z][A-Za-z\s']+?)(?:\s+for|\s+and|\.|$)""")
        atPattern.find(input)?.let { return it.groupValues[1].trim() }

        return null
    }

    // ── Future Date Extraction ────────────────────────────────────────────────

    private fun extractFutureDate(input: String): Long? {
        val cal = Calendar.getInstance()

        val dayNames = mapOf(
            "monday" to Calendar.MONDAY, "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY, "thursday" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY, "saturday" to Calendar.SATURDAY,
            "sunday" to Calendar.SUNDAY
        )

        for ((dayName, dayConst) in dayNames) {
            if (input.contains(dayName) || input.contains("next $dayName")) {
                while (cal.get(Calendar.DAY_OF_WEEK) != dayConst) {
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                }
                if (input.contains("next $dayName")) cal.add(Calendar.WEEK_OF_YEAR, 1)
                return cal.timeInMillis
            }
        }

        // "next week"
        if (input.contains("next week")) {
            cal.add(Calendar.WEEK_OF_YEAR, 1)
            return cal.timeInMillis
        }

        // "on the 3rd" "on the 15th"
        val datePattern = Regex("""on the (\d{1,2})(?:st|nd|rd|th)?""")
        datePattern.find(input)?.let {
            val day = it.groupValues[1].toIntOrNull()
            if (day != null) {
                cal.set(Calendar.DAY_OF_MONTH, day)
                if (cal.timeInMillis < System.currentTimeMillis()) {
                    cal.add(Calendar.MONTH, 1)
                }
                return cal.timeInMillis
            }
        }

        return null
    }

    // ── Confidence Calculation ────────────────────────────────────────────────

    private fun calculateConfidence(
        intent         : TransactionIntent,
        amount         : Double?,
        amountIsApprox : Boolean,
        recurringKey   : String?,
        isAmountZero   : Boolean
    ): Float {
        var score = 0.5f

        if (amount != null || isAmountZero) score += 0.25f
        if (!amountIsApprox) score += 0.1f
        if (recurringKey != null) score += 0.1f
        if (intent != TransactionIntent.UNCLEAR) score += 0.1f
        if (amountIsApprox) score -= 0.1f
        if (intent == TransactionIntent.UNCLEAR) score -= 0.2f
        if (amount == null && !isAmountZero) score -= 0.2f

        return score.coerceIn(0f, 1f)
    }
}