<div align="center">

```
███████╗██╗  ██╗ █████╗ ██████╗ ██╗  ██╗███████╗██╗███╗   ██╗
██╔════╝██║  ██║██╔══██╗██╔══██╗██║ ██╔╝██╔════╝██║████╗  ██║
███████╗███████║███████║██████╔╝█████╔╝ █████╗  ██║██╔██╗ ██║
╚════██║██╔══██║██╔══██║██╔══██╗██╔═██╗ ██╔══╝  ██║██║╚██╗██║
███████║██║  ██║██║  ██║██║  ██║██║  ██╗██║     ██║██║ ╚████║
╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝     ╚═╝╚═╝  ╚═══╝
```

### **The AI-powered finance app that knows your money like a smart friend.**

<br/>

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/SHAMONTEK/SharkFin)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Firebase](https://img.shields.io/badge/Firebase-Firestore%20%2B%20Auth-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com/)
[![Status](https://img.shields.io/badge/Status-Live%20%F0%9F%A6%88-00C853?style=for-the-badge)](https://github.com/SHAMONTEK/SharkFin)

<br/>

> *"Most finance apps show you numbers. SharkFin shows you the truth."*

<br/>

</div>

---

## 🦈 What is SharkFin?

SharkFin is a **real-time personal finance dashboard** built for people who want clarity, not clutter. No bank login required. No subscription. Just you, your money, and a Money Score that tells it straight.

Built with **Kotlin + Jetpack Compose** on Android. Powered by **Firebase** for live sync. Designed with **Cash App energy** — pure black, surgical green, zero fluff.

---

## ✨ Feature Flow

> Tap any block below to see what each screen does.

<br/>

<div align="center">

```
┌─────────────────────┐
│                     │
│   🏠  HOME SCREEN   │  ← Your financial pulse. Always.
│                     │
│  • Money Score 0–100│
│  • Breathing arc    │
│  • Live balance     │
│  • Recent activity  │
│                     │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│                     │
│  📋  ACTIVITY TAB   │  ← Every dollar, in and out.
│                     │
│  • Total In / Out   │
│  • Net balance      │
│  • Full history     │
│  • Add manually     │
│                     │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│                     │
│   ⚡  FEATURES TAB  │  ← Your full financial toolkit.
│                     │
│  • Import Statement │
│  • Bill Tracker     │
│  • Goal Tracker     │
│  • Visual Models    │
│  • Tax Tracker      │
│  • Market Watch     │
│                     │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│                     │
│  👤  PROFILE TAB    │  ← Your account. Your control.
│                     │
│  • Display name     │
│  • Account type     │
│  • Sign out         │
│                     │
└─────────────────────┘
```

</div>

---

## 🔑 Core Features

### 🏠 Home — Money Score
Your financial health at a glance. No confusion, no clutter.

- **Money Score (0–100)** — A single number calculated from your income vs. spending ratio
- **Breathing arc animation** — The circle literally pulses. Alive. Real.
- **Score labels:** `Thriving` · `Steady` · `Watchful` · `Stretched` · `Critical`
- **Counting balance animation** — Numbers count up from $0 on every load
- **Quick actions** — Add, Bills, Goals, Visuals in one tap

```
Score ≥ 60  →  🟢 Thriving
Score 35–59 →  🟡 Steady / Watchful
Score < 35  →  🔴 Stretched / Critical
```

---

### 📥 Import Statement *(NEW)*
Stop typing every transaction. Import your whole month in seconds.

- **Upload your Cash App CSV or bank PDF**
- SharkFin parses, categorizes, and bulk-writes to Firestore
- 86 transactions uploaded in one tap (tested & confirmed ✅)
- Supported: `CSV (Date, Description, Amount)` · `PDF (Cash App + standard bank)`

---

### 🧾 Bill Tracker
Never miss a bill. Never wonder if you can afford the month.

- **Calendar view** — See every bill plotted on the month grid
- **Amber dot** = bill due · **Blue dot** = paid · **Blue ring** = today
- **Toggle paid** — One tap marks it done
- **End of Month projection** — Shows if you'll survive the billing cycle
- **Categories:** Housing · Utilities · Subscriptions · Transport · Insurance · Other

---

### 🎯 Goal Tracker
Set it. Track it. Hit it.

- **Savings arc** — Visual progress circle per goal
- **Smart status badges** — `Behind ⚠️` · `On Track ✅` · `Complete 🎉`
- **Add Savings** — Drop money into a goal instantly
- **Deadline tracking** — Calculates if you're behind pace
- **Mark as Complete** — The W deserves a checkmark

---

### 📊 Visual Models — Living Dashboard
*"Reactive intelligence powered by your data"*

- **Spending Distribution donut** — See where your money actually goes
- **Cash Flow Pillars bar chart** — Income vs Spend vs Bills vs Taxes
- **Financial Health score** — Same Money Score logic, visualized differently
- **Growth Trends** — Month-over-month view (below fold)
- Zero chart libraries — all drawn with **Jetpack Compose Canvas**

---

### 📈 Market Intelligence
Real-time stocks and crypto, inside your finance app.

- Powered by **Alpha Vantage API**
- Tracks: `AAPL` · `MSFT` · `GOOGL` · `TSLA` · `NVDA` · `BTC` · `ETH` · `EUR` · `GBP`
- Expand any ticker for: Open · Prev Close · Day High · Day Low · Volume · Change
- Force Refresh button per ticker

---

## 🛠 Tech Stack

| Layer | Tech |
|-------|------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Auth | Firebase Authentication |
| Database | Cloud Firestore (real-time listeners) |
| Background Jobs | WorkManager (bill reminders) |
| Market Data | Alpha Vantage REST API |
| Architecture | Single source of truth — all state in `SharkFinDashboard` |
| Charts | Custom Canvas composables (no third-party libs) |
| Navigation | HorizontalPager + overlay pattern |

---

## 🗂 Project Structure

```
com.example.sharkfin/
│
├── WelcomeActivity.kt        # Dashboard shell + all Firestore listeners
├── SharedComponents.kt       # Single source of truth: colors, data classes, composables
├── HomeScreen.kt             # Money Score, breathing arc, balance
├── ActivityScreen.kt         # Transaction history + Add/Edit sheets
├── BillTracker.kt            # Calendar grid + bill management
├── GoalTracker.kt            # Goal cards + savings input
├── VisualModels.kt           # Canvas charts + Living Dashboard
├── FeaturesAndOnboarding.kt  # Feature grid + first-run flow
├── StockForexTracker.kt      # Alpha Vantage market terminal
├── TaxTracker.kt             # 2024 tax bracket estimator
├── InflationCalc.kt          # Inflation impact calculator
├── SplashActivity.kt         # Auth routing (persistent login)
├── MainActivity.kt           # Login / Signup screen
│
└── ui/theme/
    ├── SharkFinColors.kt
    ├── SharkFinTheme.kt
    └── SharkFinType.kt       # System fonts, no external deps
```

---

## ⚡ Getting Started

```bash
# 1. Clone it
git clone https://github.com/SHAMONTEK/SharkFin.git

# 2. Open in Android Studio (Hedgehog or later)

# 3. Add your google-services.json to /app
# (Firebase project: sharkfin-eba6f)

# 4. Add your Alpha Vantage API key to MarketApiService.kt

# 5. Build & run on any Android device (API 26+)
```

> **No emulator?** The app runs perfectly on a physical device over USB debug.

---

## 🎨 Design System

SharkFin runs on one rule: **Cash App energy.**

```kotlin
val SharkGreen = Color(0xFF00C853)  // The only accent. Period.
val SharkBlack = Color(0xFF000000)  // Background. Always.
val SharkMuted = Color(0xFF666666)  // Secondary text. Quiet.
val SharkRed   = Color(0xFFFF3B30)  // Danger. Debt. Stretched.
val SharkAmber = Color(0xFFF59E0B)  // Warning. Behind. Watch it.
```

- No borders on main screens
- No glass cards on primary views
- No gradients competing with content
- Every number **counts up from zero** on load

---

## 🔐 Auth Flow

```
Launch App
    │
    ▼
SplashActivity
    │
    ├─ User logged in? ──► "Logging you in •••" ──► WelcomeActivity
    │
    └─ New user? ────────► "Welcome, [name]"    ──► WelcomeActivity
                                ▲
                                │
                         MainActivity
                       (Login / Signup)
```

Once logged in, **you stay logged in**. No re-entering passwords. Ever.

---

## 📱 Data Flow

```
Firebase Firestore
        │
        ▼  (real-time snapshot listeners)
SharkFinDashboard  ← single source of truth
        │
        ├──► HomeScreen(expenses, incomeSources, ...)
        ├──► ActivityScreen(expenses, bills)
        ├──► BillTrackerScreen(bills, ...)
        ├──► GoalTrackerScreen(goals, ...)
        └──► VisualModelsScreen(expenses, bills, goals)
```

Every screen is **read-only from state**. All writes go directly to Firestore and propagate back through listeners automatically.

---

## 🚧 Roadmap

- [x] Money Score with breathing animation
- [x] Real-time Firestore sync
- [x] Bill Tracker with calendar
- [x] Goal Tracker with savings arc
- [x] CSV/PDF bank statement import
- [x] Live market data (Alpha Vantage)
- [x] Tax estimator
- [x] Inflation calculator
- [x] Persistent auth (no re-login)
- [x] AI Coach — natural language → auto-updates
- [x] Biometric lock
- [x] Push notifications for bill due dates
- [x] CSV export
- [x] Shared accounts / invite system

---

## 👤 Built By

**Shamonte Knight** — CS Senior, Georgia State University  
GitHub: [@SHAMONTEK](https://github.com/SHAMONTEK)
Education: Senior, B.S. in Computer Science, Georgia State University.
Technical Skills: Technical Writing, Project Management, Requirements Engineering, System Analysis.
Experience: Proven track record in software documentation and leading student development teams.
Project Role: Project Implementation and Manager.
## 👤 Assisted By Deborah Maignan
Education: Senior, B.S. in Computer Science, Georgia State University.
Technical Skills: Java, System Modeling, Analytical Problem Solving, Logic Design.
Experience: Background in modeling complex system behaviors and interpreting technical requirements into actionable diagrams.
Project Role: Documentation Architect and Diagram Designer.


<div align="center">

**SharkFin** · Built different · 🦈

*If the ocean had a finance app, it would look like this.*

</div>
