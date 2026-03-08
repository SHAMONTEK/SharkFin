SharkFin Project Documentation (V1.1 - Authentication)
======================================================

Project Overview
----------------
SharkFin is a budgeting and finance tracking application for Android. This version establishes the core user management system, allowing users to sign up for different types of accounts (Individual, Joint, Family, Business) and securely log in. The system is built on Firebase for authentication and uses Cloud Firestore for the database.


Architecture
------------
*   **Language**: Kotlin
*   **Authentication**: Firebase Authentication (Email/Password)
*   **Database**: Cloud Firestore
*   **UI Architecture**: The app uses an Activity-based architecture with an authentication gate.
    *   `MainActivity.kt`: Acts as the app's entry point. It contains an `AuthStateListener` that checks if a user is logged in. If not, it displays the login/signup UI. If a user is logged in (or after a successful signup/login), it immediately navigates to the `WelcomeActivity`.
    *   `WelcomeActivity.kt`: The main screen shown to an authenticated user. It displays user-specific information (email, role, account type) and contains the logout functionality.


Data Model (Firestore)
----------------------
1.  **`users` collection**:
    *   Each document ID is the user's `uid` from Firebase Authentication.
    *   Fields:
        *   `uid` (String): The user's unique ID.
        *   `email` (String): The user's email address.
        *   `displayName` (String): The user's display name (optional).
        *   `accountType` (String): The type of account (INDIVIDUAL, JOINT, FAMILY, BUSINESS).
        *   `primaryAccountId` (String): A foreign key linking to the `accounts` collection.
        *   `createdAt` (Timestamp): Server timestamp of when the user was created.

2.  **`accounts` collection**:
    *   Each document has a unique, auto-generated ID.
    *   Fields:
        *   `accountId` (String): The unique ID for the account.
        *   `type` (String): The type of account (matches `users.accountType`).
        *   `name` (String): A descriptive name for the account.
        *   `createdByUid` (String): The `uid` of the user who created the account.
        *   `createdAt` (Timestamp): Server timestamp of when the account was created.

3.  **`members` subcollection**:
    *   This is a subcollection inside each document in the `accounts` collection (i.e., `accounts/{accountId}/members/{uid}`).
    *   Fields:
        *   `uid` (String): The user's `uid`.
        *   `role` (String): The user's role within that account (e.g., OWNER, ADMIN, ORGANIZER, MEMBER).
        *   `status` (String): The user's membership status (e.g., ACTIVE).
        *   `joinedAt` (Timestamp): Server timestamp of when the user joined the account.


How to Get the Project and Run it
---------------------------------
1.  **Clone from GitHub**: Use Android Studio's "Get from VCS" feature with the URL: `https://github.com/SHAMONTEK/SharkFin.git`
2.  **Switch Branch**: Once the project is open, use the Git menu in the bottom-right to check out the `V1.1-Authentication` branch.
3.  **Run**: Connect an emulator or device and click the green "Run" button.

