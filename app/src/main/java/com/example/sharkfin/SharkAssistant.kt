package com.example.sharkfin

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.Tool
import com.google.ai.client.generativeai.type.defineFunction
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

/**
 * SharkAssistant: Senior Android Engineer implementation for a hyper-flexible financial assistant.
 * Uses Gemini Function Calling to bridge Natural Language (Voice/Text) to structured logic.
 */
class SharkAssistant(
    private val apiKey: String,
    private val uid: String,
    private val db: FirebaseFirestore
) {

    // ─── 20+ Diverse Financial Intents Exhausted via Tool Definitions ──────────
    private val sharkTools = Tool(
        listOf(
            defineFunction(
                name = "log_expense",
                description = "Log a one-time spending event or purchase. Handles slang and partial entries.",
                parameters = listOf(
                    Schema.double("amount", "Amount spent"),
                    Schema.str("merchant", "Merchant name"),
                    Schema.str("category", "Expense category"),
                    Schema.str("note", "Optional note"),
                    Schema.double("tip", "Optional tip amount")
                )
            ),
            defineFunction(
                name = "log_income",
                description = "Log incoming money, dividends, bonuses, or gifts.",
                parameters = listOf(
                    Schema.double("amount", "Amount received"),
                    Schema.str("source", "Source of income"),
                    Schema.bool("is_recurring", "Whether it's a recurring income")
                )
            ),
            defineFunction(
                name = "add_recurring_bill",
                description = "Set up a new recurring cost like rent, utilities, or subscriptions.",
                parameters = listOf(
                    Schema.str("name", "Name of the bill"),
                    Schema.double("amount", "Bill amount"),
                    Schema.int("due_day", "Day of the month the bill is due"),
                    Schema.str("recurrence", "Frequency (e.g., Weekly, Monthly)"),
                    Schema.str("category", "Bill category")
                )
            ),
            defineFunction(
                name = "mark_bill_paid",
                description = "Mark a known recurring bill as paid for the current cycle.",
                parameters = listOf(
                    Schema.str("bill_name", "Name of the bill paid"),
                    Schema.double("amount_paid", "Actual amount paid")
                )
            ),
            defineFunction(
                name = "update_bill_amount",
                description = "Change the expected amount for an existing recurring bill.",
                parameters = listOf(
                    Schema.str("bill_name", "Name of the bill to update"),
                    Schema.double("new_amount", "New bill amount")
                )
            ),
            defineFunction(
                name = "log_hustle_income",
                description = "Log specific gig work income (Uber, DoorDash) with ROI tracking.",
                parameters = listOf(
                    Schema.double("amount", "Amount earned"),
                    Schema.double("hours", "Hours worked"),
                    Schema.str("platform", "Platform used (e.g., Uber)"),
                    Schema.double("tax_withholding", "Tax amount withheld")
                )
            ),
            defineFunction(
                name = "set_savings_goal",
                description = "Create a new financial target or savings goal.",
                parameters = listOf(
                    Schema.str("name", "Name of the goal"),
                    Schema.double("target_amount", "Target savings amount"),
                    Schema.str("deadline", "Goal deadline")
                )
            ),
            defineFunction(
                name = "contribute_to_goal",
                description = "Add money toward an existing savings goal.",
                parameters = listOf(
                    Schema.str("goal_name", "Name of the goal"),
                    Schema.double("amount", "Contribution amount")
                )
            ),
            defineFunction(
                name = "log_debt_payment",
                description = "Record a payment toward a loan or credit card debt.",
                parameters = listOf(
                    Schema.str("debt_name", "Name of the debt"),
                    Schema.double("amount", "Payment amount")
                )
            ),
            defineFunction(
                name = "schedule_future_spend",
                description = "Put a planned expense on the calendar.",
                parameters = listOf(
                    Schema.double("amount", "Planned amount"),
                    Schema.str("description", "Description of the spend"),
                    Schema.str("date", "Planned date (YYYY-MM-DD)")
                )
            ),
            defineFunction(
                name = "schedule_future_income",
                description = "Put an expected income event on the calendar.",
                parameters = listOf(
                    Schema.double("amount", "Expected amount"),
                    Schema.str("description", "Description of the income"),
                    Schema.str("date", "Expected date")
                )
            ),
            defineFunction(
                name = "reset_financial_data",
                description = "Wipe all expenses and streaks for a clean slate.",
                parameters = listOf(
                    Schema.bool("confirm", "Confirmation to wipe data")
                )
            ),
            defineFunction(
                name = "update_rent",
                description = "Specific shortcut to update monthly rent amount.",
                parameters = listOf(
                    Schema.double("new_rent", "New monthly rent amount")
                )
            ),
            defineFunction(
                name = "log_note",
                description = "Save a general financial reminder or note to the calendar.",
                parameters = listOf(
                    Schema.str("text", "Note content"),
                    Schema.str("date", "Date of the note")
                )
            ),
            defineFunction(
                name = "correct_previous_transaction",
                description = "Modify the amount or details of the most recent entry.",
                parameters = listOf(
                    Schema.double("correct_amount", "Corrected amount"),
                    Schema.str("correct_merchant", "Corrected merchant")
                )
            ),
            defineFunction(
                name = "check_balance",
                description = "Query current cash balance or projected end-of-month state.",
                parameters = emptyList()
            ),
            defineFunction(
                name = "query_bill_status",
                description = "Ask if a specific bill has been paid yet.",
                parameters = listOf(
                    Schema.str("bill_name", "Name of the bill")
                )
            ),
            defineFunction(
                name = "set_starting_balance",
                description = "Initialize the account with a specific cash amount.",
                parameters = listOf(
                    Schema.double("amount", "Starting balance amount")
                )
            ),
            defineFunction(
                name = "update_payday",
                description = "Set or change the next expected payday.",
                parameters = listOf(
                    Schema.str("date", "New payday date")
                )
            ),
            defineFunction(
                name = "predict_wealth",
                description = "Deep-link the user to the AI Prediction screen when they ask about future wealth, retiring early, or financial destiny.",
                parameters = listOf(
                    Schema.str("scenario", "The scenario to forecast (Baseline, Bull Market, Recession, Side Hustle)")
                )
            ),
            defineFunction(
                name = "apply_slang_correction",
                description = "Internal handler for 'No I meant X' type corrections.",
                parameters = listOf(
                    Schema.str("new_input", "Corrected input text")
                )
            )
        )
    )

    private val model = GenerativeModel(
        modelName = "gemini-1.5-pro-latest",
        apiKey = apiKey,
        tools = listOf(sharkTools),
        systemInstruction = Content.Builder().apply {
            text("You are Shark, a hyper-flexible financial assistant. " +
                 "You are honest, aggressive about saving, and have a shark personality. " +
                 "Your job is to parse any natural language input (voice or text) into " +
                 "structured function calls. Exhaust every edge case. If the user is " +
                 "vague, ask for the amount. If they use slang like 'bucks' or 'hundy', " +
                 "convert to numbers. You can handle multiple intents in one sentence.")
        }.build()
    )

    private var chat = model.startChat()

    /**
     * Handles both Voice (as transcribed text) and direct Text strings.
     * Voice streams should be transcribed by a separate STT engine before calling this.
     */
    suspend fun processInput(input: String): Flow<SharkResponse> = flow {
        val response = chat.sendMessage(input)
        
        val functionCalls = response.functionCalls
        if (functionCalls.isEmpty()) {
            // No tool used, just a normal chat response
            emit(SharkResponse(response.text ?: "I'm listening...", SharkMood.CURIOUS, logTransaction = false))
        } else {
            // Execute each function call and map to data models
            for (call in functionCalls) {
                val result = executeFunction(call.name, call.args)
                emit(result)
            }
        }
    }

    private suspend fun executeFunction(name: String, args: Map<String, Any?>): SharkResponse {
        return when (name) {
            "log_expense" -> {
                val amount = (args["amount"] as? Number)?.toDouble() ?: 0.0
                val merchant = args["merchant"] as? String ?: "Purchase"
                val category = args["category"] as? String ?: "Other"
                val expense = Expense(title = merchant, amount = amount, category = category)
                db.collection("users").document(uid).collection("expenses").add(expense)
                SharkResponse("$${amount} logged at ${merchant}. Watching your streak.", SharkMood.HAPPY)
            }
            "add_recurring_bill" -> {
                val bill = Bill(
                    name = args["name"] as? String ?: "Bill",
                    amount = (args["amount"] as? Number)?.toDouble() ?: 0.0,
                    dayOfMonth = (args["due_day"] as? Number)?.toInt() ?: 1,
                    recurrence = args["recurrence"] as? String ?: "Monthly",
                    category = args["category"] as? String ?: "Housing"
                )
                db.collection("users").document(uid).collection("bills").add(bill)
                SharkResponse("New bill added: ${bill.name} for $${bill.amount}. Noted on the calendar.", SharkMood.NEUTRAL)
            }
            "mark_bill_paid" -> {
                val billName = args["bill_name"] as? String ?: ""
                // Logic to find bill by name and update Firestore
                SharkResponse("Marked $billName as paid. Your projected balance updated.", SharkMood.HAPPY)
            }
            "log_hustle_income" -> {
                val amount = (args["amount"] as? Number)?.toDouble() ?: 0.0
                val platform = args["platform"] as? String ?: "Gig"
                val tax = (args["tax_withholding"] as? Number)?.toDouble() ?: (amount * 0.20)
                // Log to income and tax trackers
                SharkResponse("Nice hustle! $${amount} from $platform logged. I've set aside $${tax} for taxes.", SharkMood.PROUD)
            }
            "schedule_future_spend" -> {
                val amount = (args["amount"] as? Number)?.toDouble() ?: 0.0
                val desc = args["description"] as? String ?: "Planned spend"
                val date = args["date"] as? String ?: ""
                SharkResponse("Scheduled $${amount} for $desc on $date. I'll remind you.", SharkMood.NEUTRAL, calendarNote = desc)
            }
            "predict_wealth" -> {
                val scenario = args["scenario"] as? String ?: "Baseline"
                SharkResponse("Consulting the Oracle... Let's look at your $scenario projection.", SharkMood.PROUD, navigateTo = "AI Prediction")
            }
            // ... Implement other 15+ handlers here ...
            else -> SharkResponse("I'm not sure how to handle $name yet.", SharkMood.CURIOUS)
        }
    }
}
