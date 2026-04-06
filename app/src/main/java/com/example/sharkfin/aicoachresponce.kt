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
    val expenses          : List<Expense> = emptyList(),
    val averageDailyBurn  : Double = 0.0, // average daily spending for Runway calculation
    val totalDebt         : Double = 0.0  // total debt for Trajectory Engine
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
    val performReset : Boolean = false,      // should the app wipe expenses/streak?
    val navigateTo   : String? = null       // Screen name to deep-link to
)

// ── Main Response Engine ──────────────────────────────────────────────────────

object AICoachResponse {

    fun generate(
        parsed : ParsedTransaction,
        state  : SharkFinancialState
    ): SharkResponse {

        // 11-LINE UPGRADE: Intent-based response logic refinement
        return when (parsed.intent) {
            TransactionIntent.HUSTLE_INCOME      -> handleHustleIncome(parsed, state)
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
            TransactionIntent.DEBT_PAYMENT       -> handleDebtPayment(parsed, state)
            TransactionIntent.CORRECTION         -> handleCorrection(parsed, state)
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

    // ── Hustle Income Handler (NLP Engine) ────────────────────────────────────

    private fun handleHustleIncome(parsed: ParsedTransaction, state: SharkFinancialState): SharkResponse {
        val amount = parsed.amount
        if (amount == null) {
            return SharkResponse(
                message = "Nice hustle. How much did you pull in?",
                mood = SharkMood.CURIOUS,
                logTransaction = false,
                askFollowUp = "How much did you make?"
            )
        }

        val tax = parsed.taxWithholding ?: (amount * 0.20)
        val runwayExtended = if (state.averageDailyBurn > 0) (amount / state.averageDailyBurn).toInt() else 0
        
        val message = buildString {
            append("\$${fmt(amount)} logged. ")
            append("I've set aside \$${fmt(tax)} (20%) for taxes. ")
            if (runwayExtended > 0) {
                append("This just extended your Freedom Runway by $runwayExtended day${if (runwayExtended == 1) "" else "s"}. ")
            }
            append("Keep that momentum.")
        }

        return SharkResponse(
            message = message,
            mood = SharkMood.HAPPY,
            logTransaction = true,
            updatedAmount = amount
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

        val isDividend = parsed.rawInput.contains("dividend")
        val source = when {
            isDividend -> "from dividends"
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
            " New daily spending limit: \$${String.format("%.2f", newDaily)}."
        } else ""

        val message = buildString {
            append("\$${fmt(amount)} in")
            if (source.isNotEmpty()) append(" $source")
            append(". ")
            if (isDividend) {
                append("Your Passive Cash is growing. ")
            }
            append("Total Cash: \$${fmt(newBal)}.")
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

        // UPGRADE: Aggressive confirmation for uncertain expense amounts
        if (amount == null || parsed.needsConfirm) {
            val q = if (parsed.merchantHint != null) "for ${parsed.merchantHint}?" else ""
            return SharkResponse(
                message = "I think you spent money $q — but I didn't catch the amount. How much was it?",
                mood = SharkMood.CURIOUS,
                logTransaction = false,
                askFollowUp = "Exact amount?"
            )
        }

        val totalSpentToday = state.dailySpentSoFar + amount
        val leftToday       = state.dailyBudget - totalSpentToday
        val wentOver        = leftToday < 0
        val tipMsg          = if (parsed.tipAmount != null) " Plus \$${fmt(parsed.tipAmount)} tip." else ""
        val merchant        = parsed.merchantHint?.let { " at $it" } ?: ""

        // Proactive Protection: Shark Guardrail
        val currentRunway = if (state.averageDailyBurn > 0) (state.balance / state.averageDailyBurn).toInt() else 999
        val newRunway = if (state.averageDailyBurn > 0) ((state.balance - amount) / state.averageDailyBurn).toInt() else 999
        val runwayLoss = currentRunway - newRunway

        val isHungry = newRunway < 7

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

        var mood = when {
            isHungry          -> SharkMood.HUNGRY
            wentOver          -> SharkMood.SAD
            leftToday < 5     -> SharkMood.CONCERNED
            leftToday > state.dailyBudget * 0.5 -> SharkMood.HAPPY
            else              -> SharkMood.NEUTRAL
        }

        val message = buildString {
            if (isHungry) {
                append("This purchase kills $runwayLoss days of freedom. ")
            }
            append("\$${fmt(amount)} logged$merchant.")
            append(tipMsg)
            if (wentOver) {
                append(" You're \$${fmt(Math.abs(leftToday))} over your spending limit for today.")
            } else {
                append(" \$${fmt(leftToday)} left to spend today.")
            }
            append(streakMsg)
            if (isHungry) {
                append(" Proceed?")
            }
        }

        return SharkResponse(
            message        = message,
            mood           = mood,
            logTransaction = true,
            updatedAmount  = amount,
            askFollowUp    = if (isHungry) "Confirm purchase?" else null
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
                "Total cash: \$${fmt(newBal)}."

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
                        "Your daily spending limit just got a lot healthier."
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

    // ── Debt Payment Handler (Trajectory Engine) ──────────────────────────────

    private fun handleDebtPayment(parsed: ParsedTransaction, state: SharkFinancialState): SharkResponse {
        val amount = parsed.amount
        if (amount == null) {
            return SharkResponse(
                message = "How much did you pay towards your debt?",
                mood = SharkMood.CURIOUS,
                logTransaction = false,
                askFollowUp = "Amount paid?"
            )
        }

        val remainingDebt = state.totalDebt - amount
        val message = buildString {
            append("\$${fmt(amount)} paid towards your debt. ")
            append("That just moved your freedom date up. ")
            if (remainingDebt > 0) {
                append("Total debt remaining: \$${fmt(remainingDebt)}.")
            } else {
                append("Debt free! That's what I'm talking about.")
            }
        }

        return SharkResponse(
            message = message,
            mood = SharkMood.HAPPY,
            logTransaction = true,
            updatedAmount = amount
        )
    }

    // ── Correction Handler ────────────────────────────────────────────────────

    private fun handleCorrection(parsed: ParsedTransaction, state: SharkFinancialState): SharkResponse {
        val amount = parsed.amount
        return SharkResponse(
            message = if (amount != null)
                "Correction received. Updating that to \$${fmt(amount)}. I've adjusted the records."
            else
                "You want to fix something? Tell me the correct amount or what I got wrong.",
            mood = SharkMood.NEUTRAL,
            logTransaction = true,
            updatedAmount = amount,
            askFollowUp = if (amount == null) "What's the correct amount?" else null
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
                "I caught the \$${fmt(parsed.amount!!)} — but what was it for? Did you spend it or get paid?"
            parsed.rawInput.contains("rent") || parsed.rawInput.contains("car") ||
                    parsed.rawInput.contains("light") || parsed.rawInput.contains("bill") ->
                "I heard you mention a bill — was that a payment you just made?"
            else ->
                "Run that back — I didn't quite catch the intent. What's happening with your money?"
        }

        return SharkResponse(
            message        = message,
            mood           = SharkMood.CURIOUS,
            logTransaction = false,
            askFollowUp    = "What was that for?"
        )
    }

    // ── Daily Opening Insight (rotates every app open) ───────────────────────

    fun generateDailyInsight(state: SharkFinancialState): String {
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val isFriday  = dayOfWeek == Calendar.FRIDAY
        val isMonday  = dayOfWeek == Calendar.MONDAY
        val isPayday  = state.paydayInDays == 0
        val streak    = state.currentStreak

        val balancePrefix = "Total Cash: \$${fmt(state.balance)}. "

        // Priority order — most urgent first
        return balancePrefix + when {

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
                "Already over your limit for today. Tomorrow starts fresh."

            // Almost at limit
            state.dailyBudget - state.dailySpentSoFar < 5 && state.dailyBudget > 0 ->
                "Almost reached your limit for today. \$${fmt(state.dailyBudget - state.dailySpentSoFar)} left."

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
                "New week. \$${fmt(state.dailyBudget)} to spend today. Let's go."

            // Low balance warning
            state.balance < 50 ->
                "Cash is low — \$${fmt(state.balance)}. Move careful today."

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
            SharkMood.HUNGRY   -> " Focus on the goal."
        }
    }
}
