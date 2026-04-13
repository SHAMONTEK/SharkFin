package com.example.sharkfin

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class SharkAssistant(
    private val apiKey: String,
    private val uid: String,
    private val db: FirebaseFirestore
) {
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.7f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 1024
        },
        safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE),
        )
    )

    private var systemPrompt = ""

    fun updateSystemContext(state: SharkFinancialState) {
        val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.US)
        val now = java.util.Date()
        
        fun isSameDay(d1: java.util.Date, d2: java.util.Date): Boolean {
            val cal1 = java.util.Calendar.getInstance().apply { time = d1 }
            val cal2 = java.util.Calendar.getInstance().apply { time = d2 }
            return cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR) &&
                   cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR)
        }

        val recentExpenses = state.expenses.take(20).joinToString("\n") { 
            "- ${it.title}: $${it.amount} (${it.category}) on ${sdf.format(it.createdAtDate)}"
        }
        
        val foodSpentToday = state.expenses.filter { 
            isSameDay(it.createdAtDate, now) && (it.category.lowercase().contains("food") || it.category.lowercase().contains("dining"))
        }.sumOf { it.amount }

        val upcomingBills = state.upcomingBills.joinToString("\n") {
            "- ${it.name}: $${it.amount} due on day ${it.dayOfMonth}"
        }
        
        val daysToNextBill = state.upcomingBills.minByOrNull { it.dayOfMonth }?.let { 
            val cal = java.util.Calendar.getInstance()
            val today = cal.get(java.util.Calendar.DAY_OF_MONTH)
            if (it.dayOfMonth >= today) it.dayOfMonth - today else (30 - today + it.dayOfMonth)
        } ?: 99

        val activeGoals = state.activeGoals.joinToString("\n") {
            "- ${it.name}: $${it.savedAmount}/$${it.targetAmount}"
        }

        systemPrompt = """
            You are Sharkie, the aggressive, honest, and personality-driven financial CFO for the SharkFin app.
            
            USER FINANCIAL STATUS:
            - Current Balance: ${"$"}${state.balance}
            - Daily Spending Limit: ${"$"}${state.dailyBudget}
            - Spent Today (Total): ${"$"}${state.dailySpentSoFar}
            - Spent on Food Today: ${"$"}$foodSpentToday
            - Money Score: ${state.moneyScore}/100
            - Total Debt: ${"$"}${state.totalDebt}
            - Average Daily Burn: ${"$"}${state.averageDailyBurn}
            - Days until next bill: $daysToNextBill days
            
            RECENT TRANSACTIONS (Last 20):
            $recentExpenses
            
            UPCOMING BILLS:
            $upcomingBills
            
            ACTIVE GOALS:
            $activeGoals
            
            YOUR PERSONALITY (SMARTER SHARK):
            - You are sharp and protective. If they spend on "Food" when their "Money Score" is low, bite back.
            - If a bill is due in < 3 days, mention it with urgency.
            - Use trend-based insults: "You're burning through your runway faster than a Great White in a feeding frenzy."
            - Be concise but impactful.
            
            SPECIFIC DEMO PROMPTS HANDLING (EXPECT SLURRY SPEECH):
            - "Summarize my recent imports" (Misheard: "Summer my important", "Reporting", "Recent summary"): List the last 3-5 major expenses/incomes.
            - "When is my next bill due?" (Misheard: "Next pill due", "Bell due", "When's rent"): Check 'UPCOMING BILLS' and tell them the date and amount.
            - "Can I afford to spend $50?" (Misheard: "Effort to spend", "Buy this for 50", "A ford"): Compare $50 to their 'Daily Spending Limit'.
            - "How do I improve my score?" (Misheard: "Improve my store", "Better money health", "Door"): Suggest reducing 'Average Daily Burn' or paying down 'Total Debt'.
            - "How much spent on food today?" (Misheard: "Foot today", "Fluid spent", "What did I eat"): Tell them exactly ${"$"}$foodSpentToday.
            
            TRANSLATION LAYER:
            If the user says something that sounds like a financial question but is mangled, assume they are asking one of the 5 demo questions above.
        """.trimIndent()
    }

    private suspend fun logToFirestore(parsed: ParsedTransaction, confirmedAmount: Double?) {
        val amount = confirmedAmount ?: parsed.amount ?: return
        
        // Log to expenses collection (SharkFin treats both income and expense in one list usually, distinguished by category)
        val category = if (parsed.intent == TransactionIntent.INCOME || parsed.intent == TransactionIntent.HUSTLE_INCOME) "Income" else parsed.category
        
        val expense = Expense(
            title = parsed.merchantHint ?: parsed.rawInput,
            amount = amount,
            category = category,
            note = "Logged via Sharkie AI",
            createdAt = Timestamp.now()
        )
        
        try {
            db.collection("users").document(uid).collection("expenses").add(expense).await()
        } catch (e: Exception) {
            // Silently fail or log error
        }
    }

    fun processInput(input: String, state: SharkFinancialState): Flow<SharkResponse> = flow {
        // AUTO-CORRECT / SLURRY SPEECH FIXES (PRE-PROCESSING FOR GEMINI)
        var fixedInput = input.lowercase().trim()
        val speechFixes = mapOf(
            "sharky" to "sharkie", "shirley" to "sharkie", "sharpie" to "sharkie", "shirk" to "sharkie",
            "siri" to "sharkie", "series" to "sharkie", "sear" to "sharkie",
            "summarize" to "summarize", "summer" to "summarize", "summary" to "summarize",
            "imports" to "imports", "important" to "imports", "imported" to "imports"
        )
        speechFixes.forEach { (bad, good) -> fixedInput = fixedInput.replace(bad, good) }

        // 1. First, use local NLP for structured parsing
        val parsed = AICoachNLP.parse(fixedInput, state.knownRecurring)
        val baseResponse = AICoachResponse.generate(parsed, state)

        // Side effect: Actually log the transaction if detected and not already logged
        if (baseResponse.logTransaction && parsed.amount != null) {
            logToFirestore(parsed, baseResponse.updatedAmount)
        }

        // 2. Use Gemini for the final response to ensure 100% "AI" feel and accuracy for questions
        try {
            val structuredContext = if (parsed.intent != TransactionIntent.UNCLEAR) {
                "Structured Parse Result: Intent=${parsed.intent}, Amount=${parsed.amount}, Category=${parsed.category}, Merchant=${parsed.merchantHint}"
            } else "No clear structured intent found."

            val prompt = """
                $systemPrompt
                
                $structuredContext
                
                User Input: "$input"
                
                Generate a response that:
                1. Incorporates the user's specific financial data if they asked a question.
                2. Maintains your aggressive shark personality.
                3. If it was an expense/income log, confirm it (e.g., "Logged the $10 for coffee. Keep an eye on that spending limit.")
                4. Keep it under 3 sentences.
            """.trimIndent()

            val result = model.generateContent(prompt)
            val geminiText = result.text ?: baseResponse.message
            
            emit(baseResponse.copy(message = geminiText, mood = if (parsed.intent == TransactionIntent.EXPENSE) SharkMood.CONCERNED else SharkMood.CURIOUS))
        } catch (e: Exception) {
            // Fallback to local response if Gemini fails
            emit(baseResponse)
        }
    }
}
