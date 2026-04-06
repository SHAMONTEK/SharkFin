# Blueprint: Sharkfin - AI-Driven Personal CFO

## 1. Overview
Sharkfin is a premium, AI-driven personal financial management application built for Android. It focuses on real-time tracking, predictive analytics (Freedom Runway), and automated wealth building (Passive Snowball). The app features an integrated AI Coach (NLP) that allows users to manage their finances through natural language.

## 2. Style and Design
- **Theme:** "Shark Navy" Premium Dark Theme.
- **Visuals:** Glassmorphism, Neon accents (SharkGreen, SharkRed, SharkAmber), and Lottie-driven mascot animations.
- **Mascot:** A reactive Shark mascot that changes moods based on the user's financial health (Money Score).
- **Core Components:** Glass cards, radial gradients, and animated progress arcs.

## 3. Feature Set (100% Complete)

### Core Financial Engine
- **Expense & Income Tracking:** Real-time logging with Firestore persistence.
- **Import Statement:** Bulk CSV processing for bank statements.
- **Bill Tracker:** Calendar-based commitment tracking with automated 2-week reminders.
- **Goal Tracker:** Progress-based goal setting with "Pace Check" logic.

### Advanced Intelligence
- **AI Coach (AICoachNLP):** Intent detection for natural language entries (e.g., "Spent $15 at Starbucks").
- **Freedom Runway:** Real-time calculation of financial safety in days based on average burn rate.
- **Money Score:** A proprietary health metric (0-100) based on savings rate and debt-to-income ratio.
- **Tax Tracker:** 2024 bracket estimation and effective rate calculation.

### Wealth & Markets
- **Dividend Tracker:** Specialized module for tracking stock payouts, yields, and ex-dividend dates.
- **Market Intelligence:** Live terminal for Stocks, Forex, and Crypto with "Benchmark Comparison" (Portfolio vs SPY/QQQ).
- **Passive Snowball:** Visualizing passive income milestones (e.g., "Dividends now pay for Netflix").
- **Debt Vanish:** Trajectory engine for visualizing debt elimination paths.

## 4. Technical Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Backend:** Firebase (Auth, Firestore)
- **Architecture:** MVVM / State-driven Navigation
- **Animations:** Lottie, Compose Animation API

## 5. Project Structure
```
.
├── app/src/main/java/com/example/sharkfin/
│   ├── AICoachNLP.kt         // AI Intent Logic
│   ├── WelcomeActivity.kt    // Main Navigation Router
│   ├── HomeScreen.kt         // Dashboard & AI Interface
│   ├── DividendTracker.kt    // Dividend Module
│   ├── StockForexTracker.kt  // Market Intelligence
│   ├── Goaltracker.kt        // Goal Management
│   ├── Billtracker.kt        // Bill & Reminder Logic
│   ├── SharedComponents.kt   // Data Models & Glass UI
│   └── ...                   // Specialized Feature Screens
└── blueprint.md              // Project Specification
```
