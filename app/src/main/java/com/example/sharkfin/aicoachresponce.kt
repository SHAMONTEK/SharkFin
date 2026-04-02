package com.example.sharkfin

// ─────────────────────────────────────────────────────────────────────────────
// AICoachResponse.kt
// Shark's voice. Takes a ParsedTransaction + current financial state and
// generates a specific, honest, personality-driven response.
// No filler. No generic advice. Specific to YOUR numbers.
// ─────────────────────────────────────────────────────────────────────────────

import java.text.SimpleDateFormat
import java.util.*

// ── Financial State (passed in from SharkFinDashboard) ───────────────────────

data class SharkFinancialState(
    val dailyBudget       : Double,       // how much user can spend today
    val dailySpentSoFar   : Double,       // how much already spent today
    val currentStreak     : Int,          // total streak number (never resets)
    val goalPercent       : Int,          // 0–100 progress toward paycheck goal
    val balance           : Double,       // current total balance
    val moneyScore        : Int,          // 0–100 money score
    val paydayInDays      : Int?,         // days until next paycheck (null = unknown)
    val knownRecurring    : List<RecurringBill> = defaultRecurringBills,
    val upcomingBills     : List<Bill>    = emptyList(),
    val activeGoals       : List<Goal>    = emptyList(),
    val expenses          : List<Expense> = emptyList()
)

// ── Response Types ────────────────────────────────────────────────────────────

// Redeclaration of SharkMood removed. Using the central one in SharedComponents.kt.

data class SharkResponse(
    val message      : String,
    val mood         : SharkMood,
    val logTransaction: Boolean = true,     // should this be saved to Firestore?
    val updatedAmount: Double? = null,      // confirmed amount after clarification
    val askFollowUp  : String? = null,      // follow-up question to display
    val calendarNote : String? = null,      // text to save to calendar for this date
    val calendarDate : Long?  = null,       // which date to put the note on
    val performReset : Boolean = false      // should the app wipe expenses/streak?
)

// ── Main Response Engine ──────────────────────────────────────────────────────

object AICoachResponse {

    fun generate(
        parsed : ParsedTransaction,
        state  : SharkFinancialState
    ): SharkResponse {

        return when (parsed.intent) {
            TransactionIntent.INCOME             -> handleIncome(parsed, state)
            TransactionIntent.EXPENSE            -> handleExpense(parsed, state)
            TransactionIntent.RECURRING_EXPENSE  -> handleRecurring(parsed, state)
            TransactionIntent.FUTURE_EXPENSE     -> handleFutureExpense(parsed, state)
            TransactionIntent.FUTURE_INCOME      -> handleFutureIncome(parsed, state)
            TransactionIntent.RENT_UPDATE        -> handleRentUpdate(parsed, state)
            TransactionIntent.BILL_UPDATE        -> handleBillUpdate(parsed, state)
            TransactionIntent.NOTE               -> handleNote(parsed, state)
            TransactionIntent.RESET_DATA         -> handleReset(parsed, state)
            TransactionIntent.SETUP_FINANCES     -> handleSetup(parsed, state)
            TransactionIntent.UNCLEAR            -> handleUnclear(parsed, state)
        }
    }

    // ── Reset Handler ─────────────────────────────────────────────────────────

    private fun handleReset(parsed: ParsedTransaction, state: SharkFinancialState): SharkResponse {
        return SharkResponse(
            message = "You want a clean slate? Done. Wiping expenses and resetting your streak to zero. " +
                      "What's your fresh starting balance? Tell me how much cash you're holding onto.",
            mood = SharkMood.CURIOUS,
            logTransaction = false,
            performReset = true,
            askFollowUp = "What's your current starting balance?"
        )
    }

    // ── Setup Handler ─────────────────────────────────────────────────────────

    private fun handleSetup(parsed: ParsedTransaction, state: SharkFinancialState): SharkResponse {
        val holding = parsed.amount
        val target = parsed.secondaryAmount

        if (holding == null) {
            return SharkResponse(
                message = "I need to know your starting numbers. How much cash are you holding onto right now?",
                mood = SharkMood.CURIOUS,
                logTransaction = false,
                askFollowUp = "Current balance?"
            )
        }

        if (target == null) {
            return SharkResponse(
                message = "Got it, \$${fmt(holding)} in the tank. Now, how much of that do you want to save by your next payday? Give me a dollar amount or percentage.",
                mood = SharkMood.CURIOUS,
                logTransaction = false,
                updatedAmount = holding,
                askFollowUp = "Savings goal amount?"
            )
        }

        // We have both! Calculate the daily budget agent-style
        val payDays = state.paydayInDays ?: 14
        val spendable = holding - target
        val daily = if (payDays > 0) spendable / payDays else spendable / 30.0

        val message = buildString {
            append("Finances calibrated. Holding \$${fmt(holding)}, target savings \$${fmt(target)}. ")
            append("With $payDays days until payday, you can spend \$${fmt(daily)} daily to stay on track. ")
            append("I've updated your dashboard. I'm watching your streak now — don't blow it.")
        }

        return SharkResponse(
            message = message,
            mood = SharkMood.HAPPY,
            logTransaction = false,
            updatedAmount = holding
        )
    }

    // ── Income Handler ────────────────────────────────────────────────────────

    private fun handleIncome(parsed: ParsedTransaction, state: SharkFinancialState): SharkResponse {
        val amount   = parsed.amount
        val merchant = parsed.merchantHint
        val newBal   = state.balance + (amount ?: 0.0)
        val remaining = state.dailyBudget - state.dailySpentSoFar

        if (amount == null) {
            return SharkResponse(
                message     = "How much did you get? Drop the number.",
                mood        = SharkMood.CURIOUS,
                logTransaction = false,
                askFollowUp = "How much came in?"
            )
        }

        val source = when {
            merchant != null -> "from $merchant"
            parsed.rawInput.contains("mom")    -> "from your mom"
            parsed.rawInput.contains("dad")    -> "from your dad"
            parsed.rawInput.contains("job")    -> "from your job"
            parsed.rawInput.contains("program")-> "from the program"
            parsed.rawInput.contains("zelle")  -> "via Zelle"
            parsed.rawInput.contains("cash app")-> "via Cash App"
            parsed.rawInput.contains("venmo")  -> "via Venmo"
            else -> ""
        }

        val mood = when {
            amount >= 500  -> SharkMood.HAPPY
            amount >= 100  -> SharkMood.HAPPY
            else           -> SharkMood.NEUTRAL
        }

        val dailyUpdateMsg = if (state.paydayInDays != null && state.paydayInDays > 0) {
            val newDaily = (newBal * (state.goalPercent / 100.0)) / state.paydayInDays
            " New daily budget: \$${String.format("%.2f", newDaily)}."
        } else ""

        val message = buildString {
            append("\$${fmt(amount)} in")
            if (source.isNotEmpty()) append(" $source")
            append(". ")
            append("Balance: \$${fmt(newBal)}.")
            if (dailyUpdateMsg.isNotEmpty()) append(dailyUpdateMsg)
            append(motivate(mood, state.currentStreak))
        }

        return SharkResponse(
            message       = message,
            mood          = mood,
            logTransaction = true,
            updatedAmount  = amount
        )
    }

    // ── Expense Handler ───────────────────────────────────────────────────────

    private fun handleExpense(parsed: ParsedTransaction, state: SharkFinancialState): SharkResponse {
        val amount    = parsed.amount
        val remaining = state.dailyBudget - state.dailySpentSoFar

        if (amount == null && parsed.amountIsApprox) {
            return SharkResponse(
                message     = "You said about how much? Lock in the number.",
                mood        = SharkMood.CURIOUS,
                logTransaction = false,
                askFollowUp = "Exact amount?"
            )
        }

        if (amount == null) {
            return SharkResponse(
                message     = "How much was it?",
                mood        = SharkMood.CURIOUS,
                logTransaction = false,
                askFollowUp = "What was the amount?"
            )
        }

        val totalSpentToday = state.dailySpentSoFar + amount
        val leftToday       = state.dailyBudget - totalSpentToday
        val wentOver        = leftToday < 0
        val tipMsg          = if (parsed.tipAmount != null) " Plus \$${fmt(parsed.tipAmount)} tip." else ""
        val merchant        = parsed.merchantHint?.let { " at $it" } ?: ""

        // Streak impact
        val streakMsg = when {
            wentOver && state.currentStreak > 7  ->
                " You had a ${state.currentStreak}-day streak going. Protect it next time."
            wentOver ->
                " Over your limit for today."
            leftToday < 5 ->
                " Barely anything left today — hold tight."
            else -> ""
        }

        val mood = when {
            wentOver          -> SharkMood.SAD
            leftToday < 5     -> SharkMood.CONCERNED
            leftToday > state.dailyBudget * 0.5 -> SharkMood.HAPPY
            else              -> SharkMood.NEUTRAL
        }

        val message = buildString {
            append("\$${fmt(amount)} logged$merchant.")
            append(tipMsg)
            if (wentOver) {
                append(" You're \$${fmt(Math.abs(leftToday))} over your limit today.")
            } else {
                append(" \$${fmt(leftToday)} left today.")
            }
            append(streakMsg)
        }

        return SharkResponse(
            message        = message,
            mood           = mood,
            logTransaction = true,
            updatedAmount  = amount
        )
    }

    // ── Recurring Bill Handler ────────────────────────────────────────────────

    private fun handleRecurring(parsed: ParsedTransaction, state: SharkFinancialState): SharkResponse {
        val key      = parsed.recurringKey
        val bill     = state.knownRecurring.firstOrNull { it.key == key }
        val amount   = parsed.amount ?: bill?.amount
        val billName = bill?.label ?: key?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Bill"

        if (amount == null || amount == 0.0) {
            return SharkResponse(
                message     = "I know about $billName but I don't have the amount on file. How much was it?",
                mood        = SharkMood.CURIOUS,
                logTransaction = false,
                askFollowUp = "How much is $billName?"
            )
        }

        val newBal = state.balance - amount
        val mood   = if (newBal < 0) SharkMood.CONCERNED else SharkMood.NEUTRAL

        // Different from stored amount?
        val amountDiffMsg = if (parsed.amount != null && bill?.amount != null &&
            Math.abs(parsed.amount - bill.amount) > 0.50) {
            " That's different from your usual \$${fmt(bill.amount)} — want me to update it?"
        } else ""

        val message = "\$${fmt(amount)} for $billName. Done.$amountDiffMsg " +
                "Balance now: \$${fmt(newBal)}."

        return SharkResponse(
            message        = message,
            mood           = mood,
            logTransaction = true,
            updatedAmount  = amount,
            askFollowUp    = if (amountDiffMsg.isNotEmpty()) "Update $billName to \$${fmt(parsed.amount ?: 0.0)}?" else null
        )
    }

    // ── Future Expense Handler ────────────────────────────────────────────────

    private fun handleFutureExpense(parsed: ParsedTransaction, state: SharkFinancialState): SharkResponse {
        val amount = parsed.amount
        val dateStr = parsed.calendarDate?.let {
            SimpleDateFormat("EEEE, MMM d", Locale.US).format(Date(it))
        }

        if (amount == null) {
            return SharkResponse(
                message     = "How much you planning to spend?",
                mood        = SharkMood.CURIOUS,
                logTransaction = false,
                askFollowUp = "Estimated amount?"
            )
        }

        val canAfford = state.balance >= amount
        val affordMsg = if (canAfford) "You got it." else "Heads up — you're short \$${fmt(amount - state.balance)} right now."

        val noteMsg = if (dateStr != null) {
            "Noted on $dateStr. "
        } else "Noted. "

        val message = "${noteMsg}Planning to spend \$${fmt(amount)}. $affordMsg Let me know when it actually goes through."

        return SharkResponse(
            message        = message,
            mood           = if (canAfford) SharkMood.NEUTRAL else SharkMood.CONCERNED,
            logTransaction = false,
            calendarNote   = "Planned spend: \$${fmt(amount)} — ${parsed.merchantHint ?: parsed.rawInput}",
            calendarDate   = parsed.calendarDate ?: System.currentTimeMillis()
        )
    }

    // ── Future Income Handler ─────────────────────────────────────────────────

    private fun handleFutureIncome(parsed: ParsedTransaction, state: SharkFinancialState): SharkResponse {
        val amount = parsed.amount
        val dateStr = parsed.calendarDate?.let {
            SimpleDateFormat("EEEE, MMM d", Locale.US).format(Date(it))
        }

        if (amount == null) {
            return SharkResponse(
                message     = "How much you expecting?",
                mood        = SharkMood.CURIOUS,
                logTransaction = false,
                askFollowUp = "Expected amount?"
            )
        }

        val dateMsg = if (dateStr != null) "on $dateStr" else "soon"
        val message = "Got it — expecting \$${fmt(amount)} $dateMsg. I'll remind you. " +
                "Hit me when it actually lands."

        return SharkResponse(
            message        = message,
            mood           = SharkMood.HAPPY,
            logTransaction = false,
            calendarNote   = "Expected income: \$${fmt(amount)}",
            calendarDate   = parsed.calendarDate ?: System.currentTimeMillis()
        )
    }

    // ── Rent Update Handler ───────────────────────────────────────────────────

    private fun handleRentUpdate(parsed: ParsedTransaction, state: SharkFinancialState): SharkResponse {
        val newAmount = parsed.amount ?: 0.0
        val oldRent   = state.knownRecurring.firstOrNull { it.key == "rent" }?.amount ?: 0.0

        val message = when {
            newAmount == 0.0 ->
                "Rent is zero for now? That's a major W. Updating to \$0. " +
                        "Your daily budget just got a lot healthier."
            newAmount < oldRent ->
                "Rent dropped from \$${fmt(oldRent)} to \$${fmt(newAmount)}. " +
                        "That's \$${fmt(oldRent - newAmount)} back in your pocket every month. Updated."
            newAmount > oldRent ->
                "Rent went up to \$${fmt(newAmount)}. That's \$${fmt(newAmount - oldRent)} more per month. " +
                        "I'll factor that in. Updated."
            else ->
                "Rent stays at \$${fmt(newAmount)}. Got it."
        }

        return SharkResponse(
            message        = message,
            mood           = if (newAmount == 0.0 || newAmount < oldRent) SharkMood.HAPPY else SharkMood.CONCERNED,
            logTransaction = false,  // don't log — just update settings
            updatedAmount  = newAmount
        )
    }

    // ── Bill Update Handler ───────────────────────────────────────────────────

    private fun handleBillUpdate(parsed: ParsedTransaction, state: SharkFinancialState): SharkResponse {
        val key       = parsed.recurringKey
        val bill      = state.knownRecurring.firstOrNull { it.key == key }
        val billName  = bill?.label ?: "that bill"
        val newAmount = parsed.amount ?: 0.0

        val message = "\$${fmt(newAmount)} for $billName — got it. Updated in your recurring bills."

        return SharkResponse(
            message        = message,
            mood           = SharkMood.NEUTRAL,
            logTransaction = false,
            updatedAmount  = newAmount
        )
    }

    // ── Note Handler ─────────────────────────────────────────────────────────

    private fun handleNote(parsed: ParsedTransaction, state: SharkFinancialState): SharkResponse {
        return SharkResponse(
            message        = "Noted. I'll put that on the calendar for today.",
            mood           = SharkMood.NEUTRAL,
            logTransaction = false,
            calendarNote   = parsed.rawInput,
            calendarDate   = System.currentTimeMillis()
        )
    }

    // ── Unclear Handler ───────────────────────────────────────────────────────

    private fun handleUnclear(parsed: ParsedTransaction, state: SharkFinancialState): SharkResponse {
        val hasAmount = parsed.amount != null

        val message = when {
            hasAmount ->
                "Got \$${fmt(parsed.amount!!)} — was that money in or money out?"
            parsed.rawInput.contains("rent") || parsed.rawInput.contains("car") ||
                    parsed.rawInput.contains("light") || parsed.rawInput.contains("bill") ->
                "Was that a bill payment or just a heads up?"
            else ->
                "Run that back — did you spend or receive money?"
        }

        return SharkResponse(
            message        = message,
            mood           = SharkMood.CURIOUS,
            logTransaction = false,
            askFollowUp    = message
        )
    }

    // ── Daily Opening Insight (rotates every app open) ───────────────────────

    fun generateDailyInsight(state: SharkFinancialState): String {
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val isFriday  = dayOfWeek == Calendar.FRIDAY
        val isMonday  = dayOfWeek == Calendar.MONDAY
        val isPayday  = state.paydayInDays == 0
        val streak    = state.currentStreak

        // Priority order — most urgent first
        return when {

            // Payday
            isPayday ->
                "Payday. Set your goal before you spend a dollar."

            // Upcoming bills
            state.upcomingBills.isNotEmpty() && (state.paydayInDays ?: 99) <= 3 -> {
                val bill = state.upcomingBills.first()
                "Heads up — ${bill.name} is due in ${state.paydayInDays} days. \$${fmt(bill.amount)}."
            }

            // Over budget
            state.dailySpentSoFar > state.dailyBudget ->
                "Already over for today. Tomorrow starts fresh."

            // Almost at limit
            state.dailyBudget - state.dailySpentSoFar < 5 && state.dailyBudget > 0 ->
                "Almost tapped out for today. \$${fmt(state.dailyBudget - state.dailySpentSoFar)} left."

            // Strong streak
            streak >= 30 ->
                "$streak days strong. Don't let today be the one that breaks it."

            streak >= 7 ->
                "Week ${streak / 7} of staying on it. Keep going."

            // Friday warning (historically high spend day)
            isFriday ->
                "Friday. Historically your most expensive day. You got \$${fmt(state.dailyBudget - state.dailySpentSoFar)} left today."

            // Monday motivation
            isMonday ->
                "New week. \$${fmt(state.dailyBudget)} to work with today. Let's go."

            // Low balance warning
            state.balance < 50 ->
                "Balance is low — \$${fmt(state.balance)}. Move careful today."

            // Goal progress
            state.goalPercent >= 75 ->
                "You're ${state.goalPercent}% toward your goal. Almost there."

            state.goalPercent >= 50 ->
                "Halfway to your goal. \$${fmt(state.dailyBudget - state.dailySpentSoFar)} to spend today."

            // Default — daily budget
            else ->
                "\$${fmt(state.dailyBudget - state.dailySpentSoFar)} to spend today. ${
                    if (streak > 0) "$streak-day streak on the line." else "Start your streak today."
                }"
        }
    }

    // ── Streak Milestone Responses ────────────────────────────────────────────

    fun streakMilestone(streak: Int): String? {
        return when (streak) {
            1    -> "Day one. Everybody starts somewhere."
            3    -> "3 days in. You're building something."
            7    -> "One week clean. That's real."
            14   -> "Two weeks. You're not the same person you were."
            30   -> "30 days. That's a habit now."
            60   -> "Two months of discipline. Shark sees you."
            100  -> "100 days. That's elite."
            365  -> "A full year. You won."
            else -> null
        }
    }

    // ── Helper: Format amount ─────────────────────────────────────────────────

    private fun fmt(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) {
            String.format("%,.0f", amount)
        } else {
            String.format("%,.2f", amount)
        }
    }

    // ── Helper: Motivational closer ──────────────────────────────────────────

    private fun motivate(mood: SharkMood, streak: Int): String {
        return when (mood) {
            SharkMood.HAPPY    -> if (streak > 0) " Streak intact." else ""
            SharkMood.PROUD    -> " That's the move."
            SharkMood.NEUTRAL  -> ""
            SharkMood.CONCERNED -> " Watch the rest of today."
            SharkMood.SAD      -> " Tomorrow we reset."
            SharkMood.CURIOUS  -> ""
            SharkMood.UPSET    -> " Move careful today."
        }
    }
}