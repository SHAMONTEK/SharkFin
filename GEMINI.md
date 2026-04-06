# **AI Development Guidelines for Sharkfin (Kotlin/Compose)**

These guidelines define the operational principles and capabilities of an AI agent (e.g., Gemini) interacting with the Sharkfin project within the Android Studio environment. The goal is to enable an efficient, automated, and error-resilient application design and development workflow.

## **Environment & Context Awareness**

The AI operates within the Android Studio development environment, specializing in modern Android development using Kotlin and Jetpack Compose.

*   **Project Structure:** The AI assumes a standard Android Gradle project structure.
    *   **Root Folder:** `/Users/taek/StudioProjects/SharkFin`
    *   **App Module:** `:app`
    *   **Source Code:** `app/src/main/java/com/example/sharkfin`
    *   **Resources:** `app/src/main/res`
    *   **Manifest:** `app/src/main/AndroidManifest.xml`
*   **idx Configuration:**
    *   The `.idx/dev.nix` file defines the workspace environment.
    *   The AI understands its role in providing system tools (JDK, Android SDK, Node.js) and preview commands.
*   **Preview & Deployment:**
    *   The AI uses `deploy` to run the app on an Android emulator or device.
    *   `render_compose_preview` is used to visualize specific Compose components.
    *   `ui_state` and `take_screenshot` are used to inspect the running app's UI.
*   **Firebase Integration:** The project uses Firebase (Firestore, Auth, etc.). The AI recognizes standard Firebase initialization and usage patterns in Kotlin.

## **App Capabilities & Feature Set**

Sharkfin is an AI-driven personal CFO designed for proactive financial management.

### **1. "Shark" AI Assistant (NLP Engine)**
*   **Voice & Text Interaction:** Natural language processing via `AICoachNLP.kt` for logging transactions (e.g., "Spent $20 at Target").
*   **Intent Detection:** Automatically categorizes inputs as Expense, Income, Recurring Bill, or Future Plan.
*   **Smart Parsing:** Handles word-based numbers ("fifty bucks"), approximate amounts ("about $10"), and tip extraction.
*   **Merchant Recognition:** Automatically identifies merchants like Starbucks, Amazon, and Uber from speech.
*   **Predictive Planning:** Detects future financial commitments ("Payday is Friday") to create reminders.

### **2. Financial Management Tools**
*   **Expense & Income Tracking:** Real-time logging with Firestore sync.
*   **Bill Tracker:** Management of recurring bills (Rent, Utilities, Subscriptions) with "Paid" status tracking.
*   **Goal Tracker:** Visual progress tracking for specific savings targets.
*   **Tax Tracker:** Dedicated module for monitoring tax-deductible expenses and withholdings.
*   **Market Monitoring:** Real-time Stock and Forex tracking (`StockForexTracker.kt`).
*   **Statement Import:** Ability to import and parse financial data from external statements.

### **3. Insights & Analytics**
*   **Money Score:** A proprietary 0-100 score (`calcMoneyScore`) based on savings rate and spending habits.
*   **Snapshot Overview:** A high-level "financial vitals" dashboard.
*   **Visual Models:** Graphical data representations for trend analysis.
*   **Inflation Calculator:** Tool to visualize the impact of inflation on purchasing power.
*   **Streak System:** Gamified consecutive days of financial tracking to build habits.

### **4. Technical Features**
*   **Local NLP:** Core parsing logic happens on-device for speed and privacy.
*   **Background Tasks:** `WorkManager` handles automated bill reminders via `BillRemainderWorker.kt`.
*   **Advanced UI:** 
    *   **Lottie Animations:** A reactive Shark mascot with moods (Happy, Sad, Curious, Neutral) tied to financial state.
    *   **Glassmorphism:** Premium semi-transparent UI layers with blur effects.
    *   **Haptic Feedback:** Tactile responses for voice input and transaction success.

## **Code Modification & Dependency Management**

The AI is empowered to modify the Kotlin codebase and manage Gradle dependencies.

*   **Core Code Assumption:** The AI primarily focuses on Kotlin files using Jetpack Compose for UI.
*   **Dependency Management:**
    *   Add dependencies to `app/build.gradle.kts`.
    *   Always run `gradle_sync` after modifying build files.
*   **Code Quality:**
    *   Adhere to Kotlin and Jetpack Compose best practices.
    *   Use Material 3 components and theming (`com.example.sharkfin.ui.theme`).
    *   Implement effective state management (e.g., `remember`, `mutableStateOf`, ViewModels).
    *   Ensure proper asynchronous handling using Coroutines and Flow.

## **Automated Error Detection & Remediation**

The AI continuously monitors for and automatically resolves errors.

*   **Post-Modification Checks:**
    1.  Run `analyze_current_file` to check for syntax and lint issues.
    2.  Run `gradle_build` (e.g., `app:assembleDebug`) to verify compilation.
    3.  Monitor `read_logcat` for runtime exceptions during deployment.
*   **Automatic Error Correction:**
    *   Fix syntax errors and type mismatches.
    *   Add missing imports.
    *   Resolve missing permissions in `AndroidManifest.xml` (e.g., `RECORD_AUDIO`, `INTERNET`).

## **UI & Design (Sharkfin Aesthetics)**

**Theming:** Sharkfin uses a custom Material 3 theme with a specific aesthetic:
*   **Primary Palette:** Dark-themed with vibrant accents (SharkNavy, SharkGreen, SharkRed, SharkAmber).
*   **Visual Effects:** Use of gradients (`background_gradient.xml`), blur/glassmorphism effects, and custom animations (Lottie).
*   **Typography:** Bold headlines and clean, legible body text.

**Interactive Elements:**
*   Buttons and cards should have subtle elevations or glows.
*   Use `HapticFeedback` for tactile interaction (e.g., on voice recording or transaction logging).

## **Iterative Development & Blueprint**

*   **Blueprint Management:** Update `blueprint.md` in the project root to document the current state, implemented features, and future plans.
*   **Communication:** Provide clear explanations of changes and summarize progress.

---

# Firebase MCP

When requested for Firebase add the following the server configurations to .idx/mcp.json. Just add the following and don't add anything else.

{
    "mcpServers": {
        "firebase": {
            "command": "npx",
            "args": [
                "-y",
                "firebase-tools@latest",
                "experimental:mcp"
            ]
        }
    }
}
