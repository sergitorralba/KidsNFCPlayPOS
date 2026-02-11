## Current Implementation Plan Summary & Discussion Log

**Project Goal:** To create an Android app that functions as an NFC Point of Sale (POS) system for kids to play, simulating payment transactions.

**Current Working Directory:** `/Users/sergitorralba/Workspaces/AI/KidsNFCPlayPOS/`

**Existing Files:**
*   `implementation_plan_summary.md` (initial project brief)
*   `app/src/main/res/drawable/funny_mastercard_placeholder.svg`
*   `app/src/main/res/drawable/funny_visa_placeholder.svg`

---

### **1. Core Functionalities:**

The app will feature three main interactive sections:
1.  **Shop (Restaurant Menu):** Allows selection of predefined items from categories with quantities to generate a bill.
2.  **Calculator:** Generates an amount to be "paid" through basic arithmetic operations.
3.  **Direct Input:** Allows direct entry of an amount to be "paid."
These functionalities will all lead to the NFC Payment Simulation.

---

### **2. Key Constraints & Principles:**

*   **Target Audience:** Kids (requires intuitive, engaging, safe UI/UX).
*   **Security:** Must be secure, with no real payment processing.
*   **Data Privacy:** No data leakage, no sensitive information.
*   **Privileges:** Requires the least amount of Android permissions.
*   **Multilingual:** All user-facing text must support translation.
*   **Extensible (Developer-side):** Content (like menu items/shop catalogs) should be easily modifiable by developers without code changes for new app versions.

---

### **3. Architectural Decisions:**

*   **Pattern:** MVVM (Model-View-ViewModel) for clear separation of concerns, testability, and maintainability.
*   **UI Toolkit:** Modern Android development practices (XML layouts confirmed for now, Jetpack Compose mentioned as viable for future).
*   **Concurrency:** Kotlin Coroutines for asynchronous operations.
*   **State Management:** `MutableStateFlow` (or `LiveData`) for ViewModel state exposure.
*   **One-Time Events:** `SharedFlow` with a custom `Event` wrapper for single-trigger UI effects.

---

### **4. NFC Payment Simulation Details:**

*   **Core Mechanism:** Simulates a payment transaction using NFC chip detection.
    *   **Presence-Only Detection:** Detects the *presence* of any NFC chip without attempting to read its content. This simplifies implementation and adheres to security/privacy.
    *   **Android NFC API:** Will use `NfcAdapter.enableReaderMode()` and `NfcAdapter.disableReaderMode()`.
        *   **`enableReaderMode` Flags:** Will use `NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B | NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_NFC_V | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK`. The `SKIP_NDEF_CHECK` flag is crucial for presence-only detection.
    *   **Activation:** `enableReaderMode()` is called when the "Tap your card to pay" screen is active and waiting for input.
    *   **Deactivation:** `disableReaderMode()` is called:
        1.  **Immediately** upon the *first* NFC tag detection (within the `onTagDiscovered()` callback) to prevent further detections during the ongoing simulation.
        2.  When the "Tap your card to pay" screen is paused or destroyed (e.g., in `onPause()`).
    *   **Feedback:** Upon *first* NFC chip detection, the app triggers a vibration and a beep.
        *   **Implementation:** Handled as a "one-time event" from `PaymentViewModel` to `PaymentSimulationFragment` using `SharedFlow<Event<PaymentEvent.TriggerNfcFeedback>>`. The `Event` wrapper ensures the trigger fires only once per observation.
        *   **APIs:** Android's `Vibrator` and `SoundPool` APIs will be used. Requires `android.permission.VIBRATE`.
*   **Payment Simulation Flow:**
    *   **Duration:** A 10-second "payment in process" simulation.
    *   **Visuals during Process:**
        *   A static, "kawaii" image of a "walking credit card".
            *   **Recommendation:** SVG format, integrated as a `VectorDrawable` in `res/drawable`.
        *   A default circular Android `ProgressBar` (spinning wheel) layered visually *above* the static credit card image.
            *   **Recommendation:** `ProgressBar` with `android:indeterminate="true"` and `style="?android:attr/progressBarStyle"`.
        *   **Layout:** `ConstraintLayout` for positioning and layering these elements, ensuring responsiveness across phone/tablet screens.
        *   **Visibility Management:** The visibility of the credit card image and spinning wheel will be managed dynamically within a single `PaymentSimulationFragment` by observing the `PaymentUiState` (e.g., `PaymentUiState.Processing`) from the `PaymentViewModel`.
    *   **Secret Failure Mechanism:**
        *   **Trigger:** Tapping a specific image (the "walking credit card" image) 3 times during the 10-second simulation forces a payment failure.
        *   **Implementation:** An `OnClickListener` on the `ImageView`. `CalculatorViewModel` (or a dedicated handler) will manage `tapCount` and `lastTapTimeMillis` to detect 3 consecutive taps within a `TAP_TIMEOUT_MS` (e.g., 500ms), using a `Handler` to reset `tapCount` if taps are not consecutive.
    *   **Simulation States (`PaymentUiState`):**
        *   `Idle`: Waiting for amount or NFC tap.
        *   `Processing`: 10-second simulation active (NFC detected, animations showing).
        *   `Success`: "Payment Successful" screen.
        *   `Failure`: 5-second "Error" screen.
        *   **Implementation:** All these states will be handled within the same `PaymentSimulationFragment` by making different sets of UI elements visible/gone based on the `PaymentUiState` observed from the `PaymentViewModel`.
    *   **Success Scenario:** After 10 seconds (without forced failure), displays "Payment Successful" (green tick, message). An "Accept" button returns to the app's main menu.
    *   **Failure Scenario:** A 5-second "Error" screen is displayed (red cross, message). The app then returns to the previous payment initiation screen, ready for a new transaction.

---

### **5. Menu Functionality Details (Shop Section):**

*   **Data Storage:** Menu items are stored in local JSON files.
*   **Extensibility:** For developers to easily add/modify shop catalogs.
    *   **Strategy:** Dynamic discovery of JSON files from `assets`.
    *   **Proposed Folder Structure:** `assets/menus/`
    *   **Proposed File Naming Convention:** `menu_[identifier].json` (e.g., `menu_cafe.json`, `menu_restaurant.json`).
*   **Data Structures (Kotlin):** `data class`es mirroring JSON structure: `MenuItem`, `SubCategory`, `MenuCategory`.
*   **JSON Parsing:** Kotlinx Serialization.
*   **Loading Mechanism:** `MenuRepository` uses `AssetManager.list("menus")` to discover files, filters them by naming convention, then loads and parses them on `Dispatchers.IO`. Returns `Result<List<MenuCategory>>`.
*   **Multilingual Support:** User-facing strings (menu names, categories, items) are referenced by `nameStringResourceName` in JSON.
    *   **Resolution:** A `ResourceResolver` utility will use `context.resources.getIdentifier()` to get `R.string` ID from the string name.
    *   **Efficiency:** `ResourceResolver` will implement a `stringIdCache` to avoid repeated `getIdentifier()` calls.
    *   **Fallback:** If a resource name is not found, it will display `"[MISSING: resourceName]"`.
    *   **ViewModel Responsibility:** `MenuViewModel` transforms raw `MenuCategory` data into `MenuCategoryUi` (display models with localized strings) before exposing them to the UI.
*   **Selection Mechanism (Top-Level Shop Navigation):**
    *   **Style:** "Windows Phone tiles" look.
    *   **Implementation:** `RecyclerView` with `GridLayoutManager`.
    *   **Tile Content:** Each tile will be a `MaterialCardView` containing a `TextView` displaying the localized shop name (e.g., "Cafe").
    *   **Interaction:** Tapping a tile in `ShopSelectionFragment` triggers navigation to display the detailed menu for that chosen shop.

---

### **6. Top-Level Navigation:**

*   **Between Main Sections:** "Shop", "Calculator", and "Direct Input."
*   **Proposed Mechanism:** Tabs.
*   **Implementation:** `TabLayout` integrated with `ViewPager2` in the `MainActivity`.
*   **Fragments:** Each tab will host a dedicated `Fragment` (`ShopSelectionFragment`, `CalculatorFragment`, `DirectInputFragment`).
*   **Tab Content:** Kid-friendly text and icons for each tab:
    *   "Shop": "Shop" / "Menu" (Icon: shopping cart/food item).
    *   "Calculator": "Calc" / "Numbers" (Icon: calculator/123).
    *   "Direct Input": "Input" / "Amount" (Icon: keyboard/money).

---

### **7. Calculator Functionality:**

*   **User Interface:** Kid-friendly design with a large keypad.
*   **"Paper" Display:** A dedicated area at the top showing the last 3 operations.
    *   **Implementation:** `LinearLayout` containing three `TextView`s.
    *   **History Management:** `CalculatorViewModel` maintains a `MutableStateFlow<List<String>>` (`operationHistory`) of size `MAX_HISTORY_SIZE = 3`. The `addOperationToHistory(formattedOp: String)` method adds new operations and trims the list.
    *   **History UI:** `CalculatorFragment` observes `operationHistory` and updates the three `TextView`s.
*   **Arithmetic Operations:** Basic (`+`, `-`, `x`, `/`), with support for percentages (`%`).
*   **Calculations Data Type:** All internal numerical calculations (operands, results) will use `java.math.BigDecimal` to ensure precision and avoid floating-point errors.
*   **Operation Precedence:** The calculator engine will handle operator precedence (e.g., `x`, `/` before `+`, `-`) for sequential button presses, ensuring accurate results like `2 + 3 x 4 = 14`.
*   **Final Amount Transfer:** A "Pay" button on the Calculator screen will trigger the payment flow.

---

### **Outstanding Question to be Addressed:**

We have established that a "Pay" button on the `CalculatorFragment` (and `DirectInputFragment`) will initiate navigation to the `PaymentSimulationFragment`, and the `PaymentSimulationFragment` will display the amount.

**The next crucial step is:**

*   **How will the final calculated `BigDecimal` amount be reliably and efficiently passed from the `CalculatorFragment` to the `PaymentSimulationFragment`? What is the recommended mechanism for passing this complex object (or its string representation) between fragments during navigation, ensuring it's available for display and used in the payment simulation?**
