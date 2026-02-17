# **SharkFin Financial Intelligence Platform: Architectural Blueprint**

## **1. Vision & Architectural Principles**

SharkFin is a **Financial Intelligence Operating System**, not a simple expense tracker. The architecture is designed for security, scalability, and high-velocity data processing to deliver real-time financial intelligence.

**Core Architectural Principles:**

*   **Security First:** Every component is designed with a zero-trust mindset. All data is encrypted at rest and in transit. Access is strictly controlled by role-based permissions.
*   **Scalability & Performance:** The system is built on a serverless, microservices-oriented architecture using Firebase and Cloud Functions, designed to handle millions of users and billions of data points.
*   **Data-Driven Intelligence:** The entire system revolves around a normalized, immutable financial ledger. All insights are derived from this single source of truth.
*   **Modularity & Maintainability:** A clean, decoupled architecture with a feature-based frontend and a service-oriented backend ensures the platform is maintainable and extensible.
*   **Production-Ready:** This blueprint omits prototyping shortcuts. It includes provisions for logging, monitoring, error handling, and CI/CD from day one.

---

## **2. System Architecture Overview**

The platform is composed of two primary components: a React-based frontend and a Firebase-powered backend.

*   **Frontend (React):** A highly modular, single-page application responsible for all user interactions and data visualization. It is a "thin" client that offloads all business logic to the backend.
*   **Backend (Firebase):**
    *   **Firebase Authentication:** Manages user identity, secure sign-on, and token-based session management.
    *   **Firestore:** The primary datastore, housing the normalized financial ledger and user data. Its security rules are the first line of defense for data access.
    *   **Cloud Functions:** The "Intelligence Engine." A collection of serverless functions that perform all complex data processing, analysis, forecasting, and simulation.

![System Architecture Diagram](https://storage.googleapis.com/spec-validator.appspot.com/presubmit-img/Sharkfin_architecture.png)

---

## **3. Firestore Data Model & Schema**

This is the heart of the system. The structure is normalized to prevent data duplication and ensure data integrity.

**Primary Collections:**

*   **/users/{userId}**: Stores user-specific information, including profile data, settings, and role.
    *   `email`: string
    *   `displayName`: string
    *   `createdAt`: timestamp
    *   `role`: string (`personal`, `family_admin`, `business_admin`)
    *   `status`: string (`active`, `inactive`)

*   **/accounts/{accountId}**: A user's financial accounts (checking, savings, credit card).
    *   `userId`: string (FK to `users`)
    *   `name`: string (e.g., "Chase Sapphire Reserve")
    *   `type`: string (`depository`, `credit`, `investment`, `loan`)
    *   `balance`: number (current balance)
    *   `currency`: string (e.g., "USD")
    *   `institution`: string (e.g., "Chase")
    *   `plaidItemId`: string (if linked via Plaid)

*   **/transactions/{transactionId}**: The immutable financial ledger. Every financial event is a transaction.
    *   `userId`: string (FK to `users`)
    *   `accountId`: string (FK to `accounts`)
    *   `type`: string (`income`, `expense`, `transfer`)
    *   `amount`: number (positive for income, negative for expense)
    *   `date`: timestamp
    *   `description`: string (e.g., "Starbucks Coffee")
    *   `category`: string (e.g., "Food & Drink")
    *   `isRecurring`: boolean
    *   `linkedTransactionId`: string (for transfers between accounts)
    *   `metadata`: map (e.g., `{ "location": "San Francisco, CA" }`)

*   **/investments/{investmentId}**: Tracks investment holdings.
    *   `accountId`: string (FK to `accounts`)
    *   `tickerSymbol`: string (e.g., "VOO")
    *   `quantity`: number
    *   `averageCost`: number

*   **/bills/{billId}**: Tracks recurring bills.
    *   `userId`: string
    *   `name`: string (e.g., "Netflix Subscription")
    *   `amount`: number
    *   `dueDate`: date
    *   `frequency`: string (`monthly`, `yearly`)

*   **/incomeStreams/{streamId}**: Tracks sources of income.
    *   `userId`: string
    *   `name`: string (e.g., "Salary - Google")
    *   `amount`: number
    *   `frequency`: string (`bi-weekly`, `monthly`)

*   **/riskProfiles/{profileId}**: Stores the calculated risk scores for a user.
    *   `userId`: string
    *   `snapshotDate`: timestamp
    *   `liquidityScore`: number (0-100)
    *   `stabilityScore`: number (0-100)
    *   `diversificationScore`: number (0-100)
    *   `taxEfficiencyScore`: number (0-100)
    *   `overallScore`: number

*   **/forecastSnapshots/{snapshotId}**: Stores the output of predictive forecasts.
    *   `userId`: string
    *   `generatedAt`: timestamp
    *   `timeframeDays`: number (30, 60, 90)
    *   `projectedBalance`: number
    *   `confidenceRange`: { `lowerBound`: number, `upperBound`: number }
    *   `assumptions`: map (e.g., `{ "incomeGrowth": 0.02 }`)

*   **/simulationScenarios/{scenarioId}**: Stores the inputs and outputs of "what-if" simulations.
    *   `userId`: string
    *   `baseSnapshotId`: string (FK to `forecastSnapshots`)
    *   `scenarioType`: string (e.g., "INCOME_CHANGE")
    *   `parameters`: map (e.g., `{ "percentChange": -0.20 }`)
    *   `result`: { `newProjectedBalance`: number, `riskDelta`: number, `timelineImpactDays`: number }

---

## **4. Security Architecture & Auth Flow**

Security is paramount. The authentication and authorization flow is designed to be robust.

**Authentication Flow:**

1.  **Login:** User enters credentials in the React app.
2.  **Firebase Auth:** Credentials are sent *directly* to Firebase Auth, never touching our frontend state or backend servers.
3.  **JWT Token:** Upon success, Firebase Auth returns a short-lived JWT (JSON Web Token) to the client.
4.  **Token Storage:** The JWT is stored securely in the browser (e.g., in memory or a secure cookie).
5.  **API Requests:** Every subsequent request to our Cloud Functions includes the JWT in the `Authorization` header.
6.  **Token Validation:** A middleware in our Cloud Functions verifies the JWT's signature and expiration.
7.  **Role & Ownership Check:** The middleware decodes the JWT to get the `userId` and queries the `/users` collection to check the user's role and confirm data ownership before allowing the request to proceed.

**Security Rules:**

Firestore security rules will enforce data ownership at the database level, ensuring a user can only ever access their own data.

---

## **5. Intelligence Engine (Cloud Functions)**

This is the brain of SharkFin. All functions are designed to be idempotent and scalable.

**Function Groups:**

*   **`onTransactionCreate`**: Triggered when a new transaction is added.
    *   Updates the corresponding account balance.
    *   Kicks off the `cashFlowAnalyzer` function.
*   **`cashFlowAnalyzer`**:
    *   Analyzes recent transactions to detect recurring patterns (subscription creep).
    *   Calculates burn rate and income volatility.
    *   Generates a new `forecastSnapshot` for the next 90 days.
*   **`riskScorer`**:
    *   Calculates liquidity, stability, and diversification scores based on account balances, transaction history, and investment holdings.
    *   Updates the user's `riskProfile`.
*   **`simulationEngine`**:
    *   Takes a `scenario` as input (e.g., "income drop of 20%").
    *   Runs a Monte Carlo simulation against the user's latest `forecastSnapshot`.
    *   Returns a new, non-persistent `simulationResult`.
*   **`portfolioAnalyzer`**:
    *   Benchmarks investment portfolio performance against market indices.
    *   Calculates risk-adjusted returns and sector exposure.

---

## **6. Frontend Architecture (React)**

The frontend is a modular, feature-driven application designed for maintainability.

**Folder Structure:**

```
/frontend
|-- /public
|-- /src
|   |-- /assets
|   |-- /components (Dumb, reusable UI components)
|   |   |-- /Button
|   |   |-- /Input
|   |   +-- /Chart
|   |-- /features (Smart components, business logic)
|   |   |-- /authentication
|   |   |-- /dashboard
|   |   |-- /cashflow
|   |   |-- /risk
|   |   +-- /investments
|   |-- /hooks (Custom React hooks)
|   |-- /lib (External library configs, e.g., Axios)
|   |-- /pages (Top-level page components)
|   |-- /providers (Context providers)
|   |-- /routes (Routing configuration)
|   |-- /services (API interaction layer)
|   |-- /styles
|   |-- /utils
|   |-- App.tsx
|   +-- index.tsx
|-- package.json
+-- tsconfig.json
```

**Key Principles:**

*   **Feature-Based Modules:** Each major feature (e.g., `cashflow`, `risk`) is a self-contained module.
*   **Separation of Concerns:**
    *   **Components:** Dumb, presentational components.
    *   **Features:** Smart components that contain business logic and state management.
    *   **Services:** All API calls are abstracted into a dedicated service layer.
    *   **Hooks:** Reusable logic is extracted into custom hooks (e.g., `useAuth`, `useApi`).
*   **State Management:** React Query (TanStack Query) for server state management (caching, refetching) and Zustand or Context API for global UI state.
*   **Routing:** `react-router-dom` for declarative, URL-based routing.

---

This blueprint establishes a solid foundation for the SharkFin platform. The next steps will be to implement this structure, starting with the backend data model and security rules, followed by the frontend scaffolding.
