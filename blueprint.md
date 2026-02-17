# Blueprint: Fin-Pal - Your Personal Finance Companion

## 1. Overview

Fin-Pal is a beautiful and intuitive Flutter application designed to help users track their income and expenses, manage their budget, and gain insights into their spending habits. The app will have a modern, clean, and visually appealing interface, with a focus on user experience and accessibility.

## 2. Style and Design

- **Theme:** Material 3 with `ColorScheme.fromSeed`.
- **Color Palette:** A vibrant and energetic look and feel with a primary seed color of deep purple.
- **Typography:** Expressive and relevant typography using the `google_fonts` package. We'll use Oswald for headings and Open Sans for body text.
- **Iconography:** Modern and interactive icons from the Material Icons library.
- **Visual Effects:** Multi-layered drop shadows for a sense of depth, and a soft glow effect for interactive elements.
- **Texture:** A subtle noise texture will be applied to the main background to add a premium, tactile feel.
- **Layout:** A visually balanced layout with clean spacing and a responsive design that adapts to different screen sizes.

## 3. Features

### Implemented Features

- **Theme Management:**
  - `ThemeProvider` to manage light and dark modes.
  - A user-facing theme toggle in the app bar.
- **Home Screen:**
  - A welcoming hero section with a large, expressive title.
  - **Financial Summary Card:** A visually distinct card to show income, expenses, and balance.
  - **Recent Transactions List:** A scrollable list of recent transactions with icons and styled details, populated with mock data.
  - **Floating Action Button (FAB):** For adding new transactions.

## 4. Project Structure

```
.
├── lib
│   ├── main.dart
│   ├── theme_provider.dart
│   ├── home_screen.dart
│   └── models
│       └── transaction.dart
├── assets
│   └── images
│       └── noise.png
├── pubspec.yaml
└── blueprint.md
```
