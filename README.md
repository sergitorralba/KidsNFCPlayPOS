# KidsNFCPlayPOS

A kid-friendly "Point of Sale" (POS) Android application designed for play-pretend shops, restaurants, and cafes. It supports real NFC card detection to simulate payments and features a robust "Cash Register" style calculator.

## 🚀 Features

*   **Dynamic Shop Menus:** Switch between different shops (Cafe, Restaurant, Fruit Shop) with categorized items.
*   **Shopping Cart:** Add/remove items to a cart and review them in a summary screen before payment.
*   **Cash Register Calculator:**
    *   Uses a "Paper Tape" style history to track every line item.
    *   Supports "Operator-as-Enter" logic (e.g., type `500` then `+` to add €5.00).
    *   Handles complex expressions like `3 x 2` then `+`.
    *   Percentage logic for taxes or discounts (e.g., `10%` then `-` subtracts 10% of the current total).
*   **Feature Visibility Settings:** Enable or disable the **Shop** or **Calculator** features from the settings menu to customize the experience. **Direct Input** is always available as the default starting screen.
*   **Seamless Reset Flow:** Upon a successful payment, the app automatically clears the cart or calculator tape and returns you to the main input screen, ready for the next "customer."
*   **NFC Payment Simulation:** Tap a real NFC card or tag to trigger a realistic payment processing animation with sound and haptic feedback.
*   **Multilingual Support:** Fully localized in **English**, **Spanish (Español)**, **Dutch (Nederlands)**, and **Catalan (Català)**.
*   **Kid-Friendly UI:** Large buttons, high-contrast text, colorful icons, and a custom splash screen.

## 🛠 Technical Details

*   **Min SDK:** 24 (Android 7.0 Nougat)
*   **Architecture:** MVVM with Kotlin Coroutines and Flow.
*   **Navigation:** Android Navigation Component with a centralized `nav_graph` and auto-redirection logic.
*   **Localization:** Custom `ResourceResolver` to ensure locale changes apply correctly even on older devices.
*   **NFC:** Uses `NfcAdapter` in Reader Mode.

## 🤫 Secret Features (For Testing)

### Force Payment Failure
Want to see how the "Payment Failed" screen looks? 
1.  Initiate a payment.
2.  While the **connection image is spinning** (during the 7-second processing phase), **tap the spinning image 3 times quickly**.
3.  The simulation will wait for the timer to finish and then transition to the **Failure** state.

## 📁 Project Structure

*   `assets/menus/`: JSON files defining the products and categories for each shop.
*   `ui/shop/`: Fragment and ViewModel logic for the shopping experience.
*   `ui/calculator/`: The custom cash-register logic and summary screen.
*   `ui/payment/`: NFC detection and processing simulation.
*   `util/`: Centralized helpers for language selection and resource resolution.

## 📦 Installation

1.  Clone the repository.
2.  Open in Android Studio (Ladybug or newer recommended).
3.  Ensure you have an NFC-enabled device for the best experience.
4.  Build and Run!

---
*Created for fun and learning!*
