# MINI POS — PROGRESS (resume here every session)

> This is the file you open first. It holds the session prompts, the current position,
> the task checklist, and a short log. The AI updates this after every task.

---

## ▶ SESSION PROMPTS (copy one into the AI to start/continue)

**KICKOFF (first session):**
```
You are building "MINI POS", a native Android app (Kotlin + Jetpack Compose + Room).
Three files in the project root define everything:
- BUILD_PLAN.md  = full spec + the 12 build phases (WHAT)
- CONVENTIONS.md = code rules, package layout, color tokens (HOW)
- PROGRESS.md    = checklist + current position (WHERE)
Rules:
1. Read all three files fully before writing any code.
2. Work ONLY on the next unchecked task under CURRENT POSITION. Do not skip ahead or refactor unrelated code.
3. Follow CONVENTIONS.md exactly (package structure, color tokens, Room/MVVM patterns).
4. When the task/phase is done: tick its box, move CURRENT POSITION to the next task, add a one-line SESSION LOG entry, then STOP and give me a 3-line summary.
5. If you are running low on output space, finish the current file cleanly, update PROGRESS.md to the exact resume point, and stop. Never leave code half-written.
Begin now with Phase 1. First confirm in one line that you've read all three files, then build.
```

**RESUME (every later session):**
```
Read PROGRESS.md (then CONVENTIONS.md and the current phase in BUILD_PLAN.md).
Continue from CURRENT POSITION — the next unchecked task only. Follow CONVENTIONS.md exactly.
When done: tick the box, advance CURRENT POSITION, add a SESSION LOG line, then stop.
```

---

## ★ CURRENT POSITION
**Phase:** POST-RELEASE — Phases 1 (Bug Fix), 2 (Sell/Products/Nav) & 3 (Cash mgmt · Daily report · Backup reminder) COMPLETE.
**Next task:** None — **all Future Updates roadmap phases (1–37, incl. all sub-phases) are COMPLETE** (latest:
Phase 37 — Sales Report shows Total Sales + Total Profit and full invoice cards with every product
sold; **v1.37**).
**Standing rule: completing Future Updates Phase N sets versionName to "1.N" + versionCode to N** (app/build.gradle.kts).
Awaiting the owner's next request.
See the FUTURE UPDATES (ROADMAP) section below.

**STATUS THIS SESSION (post-release work; all 12 original build phases were already complete):**
- ✅ **Finished:** Phase 1 (crash fixes + yellow theme + dashboard credit + About + yellow icon), Phase 2 (Sell
  defaults to Products, read-only Product Details with Sell·Buy·Edit + in-edit stock adjustment, bottom-nav
  Buy→Products), Phase 3 (Cash Management, Daily Transactions Report, Backup Reminder). DB migrated to **v3**
  (`cash_transactions` added). Added **CLAUDE.md** (project overview + "read BUILD_PLAN.md & PROGRESS.md first" rule).
  Every phase ends with a green `assembleDebug`; latest `app-debug.apk` builds clean.
- ⏳ **In progress:** nothing — no half-written code; all edits compile.
- ⤷ **Next (no active task; awaiting owner):** suggested follow-ups, NOT started:
  1. On-device QA: Sell→add→charge, Stock Report, Cash Management balance math, Daily Report 1-month clamp, and the
     10 PM backup notification actually firing (this env had no device — verified by clean compile + Room/KSP/serialization codegen).
  2. Legacy raster launcher icons (`mipmap-*/ic_launcher.webp`, used only on API 24–25) are still the old art —
     only the adaptive (API 26+) icon was recoloured to yellow. Needs Android Studio's Image Asset tool to regenerate.

**DECISIONS MADE THIS SESSION (be aware before changing related code):**
- **Crashes were runtime-only** (compiled fine): Compose `LazyColumn` needs globally-unique keys. Namespaced keys
  (`"cart_"`/`"prod_"`/`"mv_"`) in Sell/Buy/Stock Report. Rule: any LazyColumn merging multiple data sets must prefix keys per section.
- **Yellow primary theme:** `BrandYellow` chrome + `OnYellow` (dark) content. `PrimaryBlue`/`OnBlue` tokens kept in
  Color.kt but effectively unused (OnBlue still = on-error white). Money green/red unchanged; former blue amount texts → `OnSurface`.
- **Cash adjustments affect the dashboard Current Balance ONLY** — deliberately NOT in Business/Daily reports, NOT
  recorded as sales/purchases (owner intent). They ARE included in backup. Balance: `HomeViewModel.balance += cashIn − cashOut`.
- **Daily report "Profit/Loss" = sales margin** (Σ lineTotal − cost) via `observeProfitBetween`, consistent with Business Report; custom range hard-capped at 1 month (clamped, with a notice).
- **Backup reminder** stored app-wide in DataStore (`BackupReminderPrefs`, file `minipos_notif_prefs`), NOT per-shop;
  default ON at 22:00, time customizable; self-rescheduling one-time WorkManager job (fires at the exact chosen time).
- **Backup backward compatibility:** `BackupManager.CURRENT_SCHEMA_VERSION=3` but restore accepts v2 & v3
  (`MIN_SUPPORTED_SCHEMA_VERSION=2`); new `BackupData.cashTransactions` defaults to empty so old v2 backups still load.
- **Product Details Sell/Buy** use root routes `sell?productId=` / `buy?productId=` (pushed, with back) passing
  `initialProductId` so a fresh VM reliably pre-adds the product; the Sell/Buy bottom-tab + Home-button paths are unchanged.
  **Buy** left the bottom bar but stays reachable via the Home "Buy" button and Product Details "Buy".
- **Tapping a product** now opens read-only Product Details (edit-protected); Edit there opens the existing product form.

**Toolchain (keep):** AGP 9 **built-in Kotlin** 2.2.10 (no `kotlin-android` plugin), lifecycle 2.9.4, Coil 3.4.0,
Room 2.8.4 / KSP 2.3.9. Build: `JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"` then `./gradlew :app:assembleDebug`.

---

## ▶ POST-RELEASE PLAN (3 phases)
> Owner feedback after first APK test. The 12 build phases above are complete; this is the follow-up plan.

### Phase 1 – Bug Fix Phase  ✅ COMPLETE (2026-06-28)
- [x] Fix all application crashes
      - Sell: tapping a product crashed (LazyColumn duplicate key — cart item id == product-list item id). Namespaced keys.
      - Buy: same root cause as Sell. Namespaced keys.
      - Reports → Stock Report: crashed (product.id collided with movement.id in one LazyColumn). Namespaced keys.
      - "Selling a product already in inventory" / "selecting a product" / "navigating to Sell/Buy" were all the
        same duplicate-key crash, now resolved.
- [x] Update the UI colour scheme (yellow theme): bottom bar, buttons, FABs, big Sell/Buy buttons, filter chips,
      accent & Settings icons → BrandYellow; chrome text → OnYellow (dark). Money green/red kept; blue amount texts → OnSurface.
- [x] Add dashboard credit: "Mini POS by Jahirul Islam" on Home, "Jahirul Islam" links to https://jahirulislam.info/
- [x] Update About: App Name: Mini POS · Owner: Jahirul Islam · Website https://jahirulislam.info/ (clickable) + version
- [x] Ensure all reported issues are resolved before Phase 2 — assembleDebug ✓ (crashes were runtime-only; verified by
      code audit of every LazyColumn key + clean compile; recommend a device smoke-test of Sell→add→charge & Stock Report).
- [x] App launcher icon recoloured to yellow (adaptive icon: BrandYellow background + dark shopping-bag foreground).

### Phase 2 – Sell / Products / Navigation updates  ✅ COMPLETE (2026-06-28)
- [x] Sell defaults to the **Products** tab (was Quick Sell): `SellScreen` initial `mode = SellMode.PRODUCTS`.
- [x] **Product Details** page (new, read-only): `ProductDetailScreen` + `ProductDetailViewModel`. Tapping a product
      (list/tab) opens it; shows name, category, sub-category, unit, sell/buy price, current stock, low-stock + VAT/
      warranty/wholesale/discount when set. Edit protection = details are read-only until you tap Edit.
- [x] Quick action buttons on Product Details: **Sell** / **Buy** (open Sell/Buy pre-loaded with the product via root
      `sell?productId=` / `buy?productId=` pushes + `initialProductId`), **Edit** (opens the product form).
- [x] **Manual stock adjustment** inside Edit mode (ProductFormScreen, edit only): add/remove quantity + reason →
      `ProductViewModel.applyStockChange` → `StockMovement(ADJUSTMENT)`. NOT a sale; never hits sales reports; audited
      in Stock Report movement history. (The standalone Update-Stock screen still exists from the product list "Update".)
- [x] **Bottom navigation:** replaced **Buy** with **Products** (Home · Sell · Products · Reports · Settings).
      Products is now an inner tab (`ProductListScreen`); Buy remains reachable from the Home "Buy" button and the
      Product Details "Buy" action. Home/Settings "Products" entries now switch to the Products tab.
- [x] assembleDebug ✓.

### Phase 3 – Cash management · Daily report · Backup reminder  ✅ COMPLETE (2026-06-29)
- [x] **Manual Cash Adjustment** (Settings → Cash Management): new `CashTransaction` entity (CashType CASH_IN/CASH_OUT,
      amount, note, auto date/time) via **DB v2→v3 migration** (`MIGRATION_2_3`, `cash_transactions` table). New
      `CashTransactionDao`/`CashRepository`, `CashViewModel` + `CashManagementScreen` (Add Cash / Withdraw Cash dialog +
      totals + history with delete). Cash In/Out adjust the dashboard **Current Balance only** (HomeViewModel.balance +=
      cashIn − cashOut); NOT recorded as sales/purchases and excluded from all reports. Included in backup (v3) + restore.
- [x] **Daily Transactions Report** (Reports → Daily Transactions): `DailyReportViewModel`/`DailyReportScreen`. Single-day
      or custom range (clamped to **max 1 month**). Shows Total Sales, Total Purchases, Profit/Loss (sales margin), and
      per-transaction itemised lists (product name · qty × unit price · line total, date/time) for sales & purchases.
      Read-only; existing reports untouched.
- [x] **Backup Reminder** (Settings → Notifications): daily local notification (default 10:00 PM, time customizable;
      enable/disable). `BackupReminderPrefs` (DataStore), `BackupReminderWorker` + `BackupReminderScheduler`
      (self-rescheduling one-time WorkManager job at the chosen time); scheduled on app start. Offline only.
- [x] DB now v3 (schema exported, 16 tables); backup `CURRENT_SCHEMA_VERSION=3`, restores v2 & v3 (backward compatible).
- [x] clean assembleDebug ✓.

---

## 🔮 FUTURE UPDATES (ROADMAP)
> Owner-requested post-release work. **All phases (1–37, incl. sub-phases) are implemented and building clean** (Phase 15
> adds Settings → Activities + Undo, DB v4; Phase 16 adds the Home Stock Value card; Phase 17 makes Current Balance
> petty-cash + adds the Cash Management Report; Phase 18 reuses Activities on the Home recent list; Phase 19 adds the
> Buy Report; Phase 20 caps due payments at the outstanding balance; Phase 21 removes cash-row delete (Activities
> only); Phase 22 adds the common smart search; Phases 23/23.1 add the Category Report + its stock summary; Phase 24
> adds the Products multi-filter; Phase 25 redesigns the Home dashboard), including Phase 2
> (Offline License Activation), Phases 7 & 9 (GUI + Android license generators), Phase 8 (stock adjustment on
> Product Details), Phase 10 (default quantity 0), Phase 11 (Home dashboard summary), Phase 12 (removed the
> product-card Update option), Phase 13 (Product History, filter-only) and Phase 14 (branded splash). The owner-only
> `license_generator/`, `license_generator_gui/` and `mini_pos_license_generator_android/` live locally and are
> git-ignored (never pushed).

### Future Updates Phase 1 – First-Time Setup Improvement   ✅ COMPLETE (2026-06-29)
**Goal:** Improve the first-time setup experience (must work completely offline).
- [x] On first launch (no shops yet), show a chooser with **Create a New Shop** and **Restore from Backup**
      (`feature/shop/FirstRunSetupScreen`; `AppRoot` renders it for the `ShopGate.None` state instead of the create form).
- [x] Restore a backup immediately, without creating a new shop first
      (the chooser's "Restore from Backup" opens the SAF `.zip` picker → `BackupManager.import`; no existing shop needed).
- [x] After a successful restore, the restored shop becomes the active shop and the app opens normally
      (`BackupManager.import` calls `setCurrentShop`; the AppRoot gate flips to the main shell automatically).
- [x] "Create a New Shop" continues the existing setup flow (the chooser branches into the existing `ShopFormScreen`; back returns to the chooser).
- [x] Works completely offline (SAF + Room only; no network).
- [x] assembleDebug ✓.

### Future Updates Phase 2 – Offline License Activation System   ✅ COMPLETE (2026-06-29)
**Goal:** A 100% offline licensing system for selling the app (no internet, server, or Play services).
**Crypto:** RSA-2048, signature = SHA256withRSA (PKCS#1 v1.5). Key format `base64(deviceId|expiryMillis).base64(signature)`. Verified end-to-end: Python round-trip + OpenSSL independent verify of a generated signature (= what Android's `SHA256withRSA` checks).
- **License Activation screen**
  - [x] Activation gate before app access (`LicenseActivationScreen` via `LicenseGate` wrapping `AppRoot` in `MainActivity`): logo, app name, version, read-only Device ID + Copy button, license-key field, Activate button.
  - [x] App stays locked until a valid key is entered (gate shows activation for any non-Active state).
- **Device ID**
  - [x] `LicenseManager.deviceId()` generates a readable unique ID once (`MPOS-XXXX-XXXX-XXXX-XXXX`), persists in a dedicated DataStore, shows it, one-tap copy to clipboard.
- **Python License Generator (separate, owner-only project)**
  - [x] `license_generator/` created: `main.py` (CLI: keygen/pubkey/make/verify), `generator.py`, `requirements.txt` (cryptography), `README.md`. Independent of the app.
  - [x] Owner runs `python main.py make --device <id> --expiry YYYY-MM-DD` → signs a key for that device. The app only ever **verifies** (public key), never generates.
- **Git**
  - [x] `license_generator/` + `*.pem` added to `.gitignore` (verified with `git check-ignore`); never pushed.
- **Offline verification**
  - [x] `LicenseVerifier.verify()` checks signature (genuine), Device ID match, and expiry — pure `java.security`, no network.
- **Device lock**
  - [x] Key embeds the Device ID; on another device → `VerifyResult.WrongDevice` → "belongs to another device".
- **License expiry**
  - [x] `LicenseManager.state` re-verifies (incl. expiry) on every start/collection; expired → `Locked(EXPIRED)` → activation screen, can enter a new key.
- **Successful activation**
  - [x] Stored in app-private DataStore (tamper-proof via signature); gate flips to the app automatically; skipped on future launches.
- **License Management (Settings)**
  - [x] Settings → License → `LicenseManagementScreen`: status (Active/Expired…), Device ID + copy, expiry date, days remaining, **Renew** & **Replace** (paste a new key). Only writes the license pref — never touches shop/Room data.
- **Contact info**
  - [x] `LicenseContactInfo` ("Need a license key? Contact the Software Owner — Jahirul Islam — https://jahirulislam.info/", clickable) on both Activation and License Management screens.
- **Security**
  - [x] Private key only in `license_generator/private_key.pem` (git-ignored); only the public key ships (`LicenseKeys.PUBLIC_KEY_B64`, verified to match). Users cannot forge keys.
- **Architecture**
  - [x] All licensing code isolated in `com.minipos.feature.license`; no shared dependency between the app and the Python generator. assembleDebug ✓ (clean, no warnings).

### Future Updates Phase 3 – Product Management Improvements   ✅ COMPLETE (2026-06-29)
**Goal:** Better product management in **Products** and **Settings → Products & inventory**.
- [x] Add a **Delete Product** option to the product edit screen — wired `ProductViewModel.delete` to a red "Delete product" button in `ProductFormScreen` (edit mode only), with a `ConfirmDialog`.
- [x] Allow deletion only when current stock is **0**; otherwise block with: *"This product still has stock. Remove all stock before deleting."* (inline red message; the confirm dialog never opens).
- [x] Hide **0-stock** products from **Sell → Product List** (`SellScreen` "Tap to add" now filters `stock > 0`).
- [x] Keep 0-stock products visible in the **Products** screen (unchanged — still shows all products).
- [x] **Only the selling screen** hides out-of-stock products. **Decision:** the spec also said "hide from the Stock Report," but that contradicts "only the selling screen should hide." Honored the final/most-specific instruction → **Stock Report left unchanged** (keeps inventory auditing complete). Trivial to also hide there later if wanted.
- [x] In the stock-adjustment (both `ProductFormScreen` edit mode **and** `UpdateStockScreen`), prevent removing more than the available stock — no negative stock; shows *"Cannot remove more than current stock (N)."*
- After delete, `AppRoot` pops both the edit form and the now-stale Product Details page (back to the Products list). assembleDebug ✓

### Future Updates Phase 4 – Product Summary   ✅ COMPLETE (2026-06-29)
**Goal:** Show an inventory summary on the Products screen.
- [x] Show **Total units** and **Stock value** in a two-card header at the top of the **Products** page (`ProductListScreen`, above the search box).
- [x] Reuse the same calculation/design as the **Stock Report** — `totalStockValue` from `ProductRepository.observeStockValue`, `totalUnits` = Σ stock; cards mirror the report's StatCard/units-card look. Numbers always match the Stock Report.
- [x] Updates automatically whenever inventory changes (Room Flow → StateFlow). The summary is **shop-wide** — intentionally unaffected by the search/category filter.

### Future Updates Phase 5 – Sell Screen & Cart Redesign   ✅ COMPLETE (2026-06-29)
**Goal:** Simplify/modernize selling (was: inline cart + ChargeBar + CheckoutDialog).
- [x] Cart is no longer a bulky inline section — the **product list is the primary focus**; in-cart products show a small yellow "In cart: N" pill on their row.
- [x] Compact **Continue** bottom bar (item count + total) replaces the old ChargeBar. On **Continue** a popup cart summary opens (per-line name, unit price, qty, line total, grand total).
- [x] Large **Confirm Sell ৳total** button inside the popup — the sale is committed **only** when it is pressed (`confirmCartSale`).
- [x] Popup is **scrollable** (items + payment scroll together) with the **Confirm Sell** button **sticky** at the bottom (`Dialog` + `Surface`, middle `Column.weight(1f).verticalScroll`).
- [x] Modern POS-style UX; payment (Cash/Due, customer pick/create, partial paid, note) lives in the same popup so Confirm Sell finalises in one place.
- **Quick Sell** unchanged → still uses the shared `CheckoutDialog`. **Buy's** shared `CheckoutDialog` left fully intact. Editing qty to 0 in the popup removes the line; emptying the cart auto-closes the popup. assembleDebug ✓

# Future Updates Phase 6 – Stock Validation for Selling

**Status:** ✅ COMPLETE (2026-06-29)

### Goal

Prevent users from selling more products than the available stock.

### Requirements

* Currently, the app allows users to sell more than the available stock. For example, if a product has **5** units in stock, the user can still sell **6 or more**, which should not be allowed.
* Update **all Sell options** in the app to ensure users can only sell up to the available stock quantity.
* This validation must apply to:

  * The main **Sell** screen.
  * **Products → Product Details → Sell**.
* Do **not** modify any other options. Only update the **Sell** functionality to enforce the available stock limit.

### Implementation
- [x] Cap enforced centrally in `SellViewModel` (`maxSellable(product) = floor(stock)`), so **both** Sell paths are covered — the main Sell screen *and* Product Details → Sell both funnel through this cart.
  - `addToCart`: out-of-stock products add nothing; once the line hits stock it stops incrementing.
  - `setQuantity`: requested qty is clamped to available stock.
- [x] In the **Review sale** popup, the qty stepper's **+** is disabled at the stock limit (`QtyStepper` gained an optional `max`; default `null` keeps Update-Stock unbounded).
- [x] User feedback: a one-shot `SellViewModel.messages` flow shows a snackbar (*"Only N in stock"* / *"… is out of stock"*) when a limit is hit.
- [x] **Quick Sell** exempt (no product/stock). **Buy**, stock adjustments, and all other flows untouched. assembleDebug ✓.


# Future Updates Phase 7 – GUI Version of the License Generator

**Status:** ✅ COMPLETE (2026-06-29)

### Implementation
- [x] CLI generator (`license_generator/`) left **unchanged**; new GUI lives in a separate, independent folder `license_generator_gui/` (own self-contained code, no import of the CLI).
- [x] Single self-contained `license_gui.py` (Tkinter, stdlib) — embeds the **same** RSA private key, so its keys match the app's public key. Verified: OpenSSL + the CLI verifier both accept GUI-generated keys; wrong device rejected.
- [x] Runs as a `.py` (`python license_gui.py`) **and** builds to a standalone Windows `.exe` (`build_exe.bat` / `pyinstaller --onefile --windowed`; pyinstaller in `requirements.txt`).
- [x] UI: **Device ID** field, **License Duration (Days)** field, **Generate License** button, read-only key box, **Copy License Key** button (+ status/expiry line, input validation).
- [x] Duration is in **days** (expiry = now + N days); the signing **algorithm & security are identical** to the CLI — only the front-end differs.
- [x] `license_generator_gui/` added to `.gitignore` (it embeds the private key); never pushed. No Android changes (the app is untouched).

### Goal

Create a GUI (Graphical User Interface) version of the existing Python License Generator. This update is only for improving the user interface of the license generator and will **not** change the existing licensing system or Android application.

### Requirements

* Keep the current Python License Generator unchanged.
* Create a new GUI version in a separate file/folder without modifying the existing generator logic.
* The GUI version must be able to run as:

  * A single Python (`.py`) file.
  * A standalone Windows executable (`.exe`) file.
* When the application starts, it should display a simple interface with:

  * **Device ID** input field.
  * **License Duration (Days)** input field.
  * **Generate License** button.
* The software owner enters:

  * The customer's Device ID.
  * The number of days the license should remain valid.
* After clicking **Generate License**, the application should:

  * Generate the license key using the existing licensing logic.
  * Display the generated license key in a read-only text box.
  * Provide a **Copy License Key** button for easy copying.
* Do **not** change the existing license generation algorithm or security implementation.
* The GUI must simply provide a more user-friendly interface for the existing Python License Generator.
* Keep the GUI project completely separate from the Android application and independent from the existing command-line version of the license generator.


# Future Updates Phase 8 – Stock Adjustment in Product Details

**Status:** ✅ COMPLETE (2026-06-29)

### Implementation
- [x] Extracted the adjustment UI into a single shared composable `StockAdjustmentSection` (add/remove chips, qty stepper, optional reason, confirm, no-negative-stock guard, "not a sale" note).
- [x] **Edit Product** (`ProductFormScreen`) now renders that shared section — behaviour unchanged (same UI, same rules); removed its old inline copy so there is **no duplicated logic**.
- [x] Added the same section to the bottom of **Product Details** (`ProductDetailScreen`), below the info card.
- [x] Both call the same business logic — `ProductRepository.adjustStock(... ADJUSTMENT ...)` (records a StockMovement, never a sale); Product Details gets it via new `ProductDetailViewModel.applyStockChange`.
- [x] Product Details stock updates **immediately** after an adjustment (its `product` is a Room `Flow` → re-emits). assembleDebug ✓ (clean).

### Goal

Add the **Stock Adjustment** section to the **Product Details** screen for quicker inventory management while keeping the existing Stock Adjustment section in the **Edit Product** screen unchanged.

### Requirements

* Add a **Stock Adjustment** section at the bottom of the **Product Details** screen, below the product information, as shown in the reference image.
* **Do not remove or modify** the existing Stock Adjustment section in the **Edit Product** screen. It must continue to work exactly as it does now.
* The new Stock Adjustment section in **Product Details** should have the same functionality as the one in **Edit Product**.
* Users should be able to:

  * Add Stock
  * Remove Stock
  * Enter the adjustment quantity
  * Enter an optional reason
  * Confirm the stock adjustment
* After a successful stock adjustment, update the current stock immediately in the Product Details screen.
* Reuse the existing stock adjustment logic and business rules. Do not duplicate or rewrite the logic—both screens should use the same implementation.
* Keep the UI consistent with the current app design and use the provided reference image as the layout guide.

# Future Updates Phase 9 – Android Version of the License Generator

**Status:** ✅ COMPLETE (2026-06-30)

### Implementation
- [x] New **completely separate** Android Studio project at repo root: `mini_pos_license_generator_android/` (own Gradle wrapper/catalog/settings; no dependency on the MINI POS app; builds on open).
- [x] App: **MINI POS License Generator**, id `com.jahirulislam.miniposlicensegenerator`, Kotlin, minSdk 24, targetSdk 36, MVVM, Material 3, Compose.
- [x] One-page UI (`MainActivity` + `LicenseViewModel`): Device ID field, License Duration (Days) field, Generate License button, read-only License Key, Copy button, Clear button.
- [x] `LicenseGenerator` reuses the **same algorithm + same keypair** (embedded PKCS#8 private key) via `java.security` — keys are **100% compatible** with the app (verified: a signed key checks out against the app's public key).
- [x] 100% offline — **no permissions** in the manifest (no internet/login/DB/cloud/Play services).
- [x] Builds clean: **assembleDebug ✓ (app-debug.apk ~12 MB)** and **assembleRelease ✓ (app-release-unsigned.apk ~7.5 MB)**; debug APK installs directly.
- [x] Has its own `README.md`; folder is git-ignored (embeds the private key) — never pushed.

### Goal

Create a standalone Android application for generating MINI POS license keys. This application is **only for the software owner** and must remain completely separate from the MINI POS project.

### Project Requirements

* Create this as a **completely separate Android Studio project**.
* Do **not** add it inside the MINI POS Android app module.
* Create it in its own folder, for example:

```text
MINI_POS/
│
├── app/
├── docs/
├── license_generator/
├── mini_pos_license_generator_android/
└── ...
```

* The project must be fully independent and have no dependency on the MINI POS application.
* The project should build immediately after opening in Android Studio without requiring any configuration changes.

### Android Project Information

Use the following values:

* **Application Name:** MINI POS License Generator
* **Package Name (Application ID):** `com.jahirulislam.miniposlicensegenerator`
* **Language:** Kotlin
* **Minimum SDK:** 24
* **Target SDK:** Latest stable Android SDK
* **Architecture:** MVVM
* **Material Design 3**

### Functionality

Reuse the existing license generation algorithm from the Python License Generator.

Do **not** modify the licensing logic or cryptography.

The generated license keys must be **100% compatible** with the MINI POS application.

### User Interface

Create a simple one-page application containing:

* Device ID input field.
* License Duration (Days) input field.
* Generate License button.
* Generated License Key (read-only).
* Copy License Key button.
* Clear button.

### Workflow

1. Enter the customer's Device ID.
2. Enter the number of license days.
3. Tap **Generate License**.
4. Display the generated license key.
5. Copy the license key and send it to the customer.

### Requirements

* Work completely offline.
* No internet permission required.
* No login or authentication.
* No database required.
* No cloud services.
* No Google Play services.
* The app should only generate license keys.

### Build Requirements

* The project must compile without errors.
* Generate both:

  * Debug APK
  * Release APK
* After building, the generated APK should be installable directly on an Android device without requiring any project modifications.

### Documentation

Create a `README.md` explaining:

* Project structure.
* How to open the project in Android Studio.
* How to build the APK.
* How the license generator works.
* Any required dependencies.
* Keep all documentation inside this project only.

This Android application must remain completely independent from the MINI POS application and should only be used by the software owner to generate license keys.


# Future Updates Phase 10 – Default Quantity Value

**Status:** ✅ COMPLETE (2026-06-30)

### Implementation
- [x] **Stock Adjustment** (shared `StockAdjustmentSection`, used by **Product Details** and **Edit Product**): default quantity now **0** (`mutableIntStateOf(0)`, stepper `min = 0`); the Add/Remove action is blocked at 0 with *"Enter a quantity of 1 or more."*; resets to 0 after a successful adjustment. Existing no-negative-stock rule unchanged.
- [x] **Buy** cart: items are added at quantity **0** (`addToCart` → `quantity = 0`); `confirmPurchase` skips any 0-qty line. The Pay bar stays gated on `total > 0`, so the purchase can't run until a quantity is ≥1 (no UI/layout change).
- [x] **Sell** intentionally left as-is — not in the spec's list of locations.
- [x] Reused the existing `QtyStepper` + validation; once quantity is ≥1 everything behaves exactly as before. assembleDebug ✓ (clean).

### Goal

Improve the quantity selection experience by changing the default quantity from **1** to **0** in all applicable locations.

### Requirements

* Change the default quantity value from **1** to **0**.
* The user must increase the quantity to **1 or more** before performing any action.
* If the quantity is **0**, the action must **not** execute and an appropriate validation message should be shown.

### Apply this update to the following locations:

* **Buy** section (shopping cart quantity selector).
* **Product Details → Stock Adjustment** section.
* **Edit Product → Stock Adjustment** section.

### Additional Requirements

* Reuse the existing quantity selector and validation logic wherever possible.
* Do not change the UI design or layout.
* Only update the default quantity value and validation behavior.
* Once the quantity is **1 or greater**, the functionality should continue to work exactly as it does now.

# Future Updates Phase 11 – Home Dashboard Summary

**Status:** ✅ COMPLETE (2026-06-30)

### Implementation
- [x] Renamed the Home card **"Products in stock" → "Types of Products"** (still shows `stats.productCount` = unique product types via `productRepo.observeCount`).
- [x] Added a **"Total Units"** card beside it in a two-up Row — shows Σ stock using the **same calculation** as the Products page header (`HomeViewModel.totalUnits` = `observeByShop` → `sumOf { it.stock }`; no new metric/method invented).
- [x] Both use the existing `CountTile` (generalised to take a `String` value + `Modifier`), same size/style as other dashboard cards (`Modifier.weight(1f)`).
- [x] No other dashboard stats/logic touched. assembleDebug ✓ (clean).

### Goal

Improve the Home Dashboard by displaying additional inventory information and using clearer labels.

### Requirements

* On the **Home** screen, rename the existing dashboard card:

  * **Products in Stock** → **Types of Products**
* This card should continue to display the **total number of unique product types** in the inventory.
* Next to the **Types of Products** card, add a new dashboard card labeled:

  * **Total Units**
* The **Total Units** card should display the **total quantity of all products currently in stock**, using the same calculation that is already shown at the top of the **Products** page.
* Reuse the existing calculation logic from the **Products** page. Do not create a new calculation method.
* Keep both cards the same size and style as the other dashboard summary cards.
* Follow the layout shown in the provided reference image.
* Do not modify any other dashboard functionality or statistics.

# Future Updates Phase 12 – Remove Update Option from Products

**Status:** ✅ COMPLETE (2026-06-30)

### Implementation
- [x] Removed the clickable **"Update"** text from every product card in `ProductListScreen.ProductRow` (the card now shows only the product + stock).
- [x] Dropped the now-unused `onUpdateStock` wiring into `ProductRow` and the unused `OnYellow` import (kept the screen's outer signature + nav plumbing + `UpdateStockScreen` intact — nothing else changed).
- [x] Stock adjustment is still available in **Product Details → Stock Adjustment** and **Edit Product → Stock Adjustment** (Phase 8 shared section). assembleDebug ✓ (clean).

### Goal

Remove the **Update** option from the **Products** page, since the same stock adjustment feature is already available in **Product Details** and **Edit Product**.

### Requirements

- In the **Products** page, every product currently has an **Update** option below the stock information.
- Remove the **Update** option from all product cards.
- The stock adjustment feature will remain available in:
  - **Product Details → Stock Adjustment**
  - **Edit Product → Stock Adjustment**
- Do not change any other functionality or UI.
- Keep everything else exactly as it is.

# Future Updates Phase 13 – Product History

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] Replaced the product-card option with **"History"** (`ProductListScreen.ProductRow`, same spot/style as the old "Update") → opens a new read-only `ProductHistoryScreen`.
- [x] Reuses the existing `stock_movements` data (no new table/columns): shows **Buy / Sell / Stock Adjustment** (+ Opening stock) for one product, **newest first**, last **30 days** (`StockMovementDao.observeByProductSince`).
- [x] Each row shows **date/time, type, signed quantity (+/−), and the stock balance after** (computed as a running balance anchored on current stock — the "if available" balance).
- [x] **View-only** — no edit/delete on this screen.
- [x] **30-day window = filter-only** (owner's choice): the screen shows only the last 30 days via `StockMovementDao.observeByProductSince` (`since = now − ProductRepository.MOVEMENT_RETENTION_MILLIS`). **No physical deletion** — all `stock_movements` rows are kept, so the **Stock Report and everything else are fully unaffected**. DB version unchanged (v3); assembleDebug ✓ (clean).

### Goal

Add a **Product History** option to the **Products** page, replacing the previous **Update** option removed in Phase 12.

### Requirements

- In the **Products** page, replace the removed **Update** option with a new option called **Product History**.
- When the user taps **Product History**, open a screen showing the complete movement history for that specific product.
- The history should include all stock movements, including:
  - Product purchases (Buy)
  - Product sales (Sell)
  - Stock adjustments (Add Stock / Remove Stock)
- Display the history in chronological order, with the newest records shown first.
- Each history entry should clearly show:
  - Date and time
  - Transaction type (Buy, Sell, Stock Adjustment)
  - Quantity changed
  - Stock balance after the transaction (if available)
- Keep the product history for the **last 30 days only**.
- Any history record older than **30 days** should be automatically deleted, keeping only the most recent 30 days of records.
- This feature is for viewing history only. Users should not be able to edit or delete any history records from this screen.
- Reuse the existing transaction and stock adjustment data wherever possible. Do not create duplicate records or change the existing database structure unless necessary.
- Keep all other functionality and UI unchanged.

# Future Updates Phase 14 – Splash Screen Enhancement

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] New `feature/splash/SplashScreen.kt` — full-screen **BrandYellow** background, centred **MINI POS logo** (`ic_launcher_foreground`), and **"MINI POS"** below with a subtle **wave animation** (per-letter bob, `rememberInfiniteTransition`).
- [x] Shown for **2 seconds** (`SPLASH_MILLIS`), then `Crossfade` into the normal flow (`LicenseGate { AppRoot() }`) — no business logic/flow changed.
- [x] Added `Theme.MINIPOS.Splash` (yellow `windowBackground`) as the launch theme + `setTheme(R.style.Theme_MINIPOS)` in `onCreate`, so the cold-start window is yellow (no white flash before Compose draws).
- [x] Animation is cosmetic only and doesn't affect the fixed 2s timing or startup logic. assembleDebug ✓ (clean).

### Goal

Improve the app's splash screen by giving it a more professional appearance that matches the MINI POS branding.

### Requirements

- Increase the splash screen display time to **2 seconds** before opening the app.
- Change the splash screen background to use the same **yellow theme color** as the MINI POS application.
- Display the **MINI POS** logo in the center of the screen.
- Display the app name **MINI POS** below the logo.
- Apply a smooth and modern text animation to the app name, such as:
  - Wave effect (preferred), or
  - Another clean and professional animation if it provides a better user experience.
- Keep the animation subtle and smooth without affecting the overall loading time.
- The splash screen should feel modern, clean, and professional while maintaining the MINI POS branding.
- After the 2-second splash screen finishes, continue with the normal app startup flow without changing any existing functionality.
- Do not modify any business logic or application flow. This update is only for improving the splash screen UI and user experience.


# Future Updates Phase 14.1 – Splash Screen Refinement

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] Splash time **2s → 1.5s** (`MainActivity.SPLASH_MILLIS = 1500L`).
- [x] Removed the **large logo at the beginning** = the **Android 12+ system splash icon** (the OS draws the launcher icon big on cold start before Compose). Suppressed it via `res/values-v31/themes.xml` (`windowSplashScreenAnimatedIcon = @drawable/splash_blank` transparent, `windowSplashScreenBackground = brand_yellow`).
- [x] Kept the in-app Compose splash exactly as-is — small centred MINI POS logo + the wave-animated "MINI POS" text (`SplashScreen.kt` untouched).
- [x] On API < 31 there was never a system icon, so nothing to remove there. assembleDebug ✓ (clean).

### Goal

Make a few refinements to the new Splash Screen for a cleaner and more professional startup experience.

### Requirements

- Reduce the splash screen display time from **2 seconds** to **1.5 seconds**.
- Remove the large logo that appears at the beginning of the splash screen.
- Keep only the existing **small MINI POS logo** displayed in the center of the screen.
- Keep the **MINI POS** text animation exactly as it is currently and other thin as well dont touch


# Future Updates Phase 15 – Activities & Undo Transactions

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] **Settings → Activities** (`ActivityScreen` + `ActivityViewModel`, route `ACTIVITIES`): one chronological list (newest first) aggregating the **last 30 days** of Sell, Buy, Expense, Cash In, Cash Out, Stock adjustment, and Undo records. Each row shows date/time, type, details (party / category / product + signed qty), and amount.
- [x] **Undo** on each eligible row (not on undo entries) with a confirm dialog. Undo **reverses + removes** the original in one `db.withTransaction` (re-add/subtract stock, drop its movements + due + due-payments, delete the row) and logs an **`ActivityUndo`** audit entry. Because reports/ledgers/balance all re-aggregate the tables, the reversal shows everywhere with **no existing transaction code changed**.
- [x] New `ActivityUndo` entity + DAO + **DB v3→v4 migration** (`MIGRATION_3_4`); new `ActivityRepository` (aggregation + 6 undo methods); undo/since DAO queries added to Sale/Purchase/Expense/Cash/StockMovement/Party DAOs.
- [x] **30-day window is filter-only** (no old data deleted). Undone originals disappear from the list (can't undo twice); the undo entry remains as the audit trail. assembleDebug ✓ (Room v4 validated, clean).
- Notes: undo of a sale/purchase that has dependent payments/stock is fully reversed (a purchase whose stock was already partly sold can drive stock negative — true reversal). `ActivityUndo` is local audit and intentionally not added to backup.

### Goal

Add an **Activities** section to the **Settings** page where all application activities are recorded and can be undone if entered by mistake.

### Requirements

- Add a new **Activities** option inside the **Settings** page.
- The Activities screen should display all application activities in chronological order, with the newest first.
- The Activities screen should include records for:
  - Buy transactions.
  - Sell transactions.
  - Expense transactions.
  - Stock adjustments.
  - Undo activities.
  - any other activities. 
  - cash in 
  - cash out
- Every activity should display the necessary details, including:
  - Date and time.
  - Activity type.
  - Related product or expense information (where applicable).
- Each activity should have an **Undo** option.
- Users should be able to undo activities for up to **30 days** from the original transaction date.
- Once an activity is undone, it should no longer be possible to undo it again.
- Every undo action must also be recorded as a new activity in the Activities list so there is a complete audit trail.
- The Activities screen should display records for the **last 30 days (1 month)** only.
- Activities older than **30 days** should no longer appear in the Activities screen.
- Do not modify any existing transaction functionality. This feature is only for viewing activities and allowing eligible transactions to be undone from one central location.
```
Future Updates Phase 16 – Dashboard Stock Summary

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] Added a third **"Stock Value"** card to the Home dashboard row, beside **Types of Products** and **Total Units** (now a 3-up Row, each `weight(1f)`).
- [x] Reused the **same calculation** as the Products page — `HomeViewModel.stockValue` from `ProductRepository.observeStockValue` (Σ stock × buyPrice); no new method. Rendered with the existing `StatCard` (money).
- [x] No other dashboard functionality changed. assembleDebug ✓ (clean).

### Goal

Add one more inventory summary card to the Home Dashboard.

### Requirements

- On the **Home** dashboard, keep the existing summary cards:
  - **Types of Products**
  - **Total Units**
- Add one more summary card on the same row called:
  - **Stock Value**
- The **Stock Value** should display the total value of all current inventory, using the same calculation that is already shown on the **Products** page.
- Reuse the existing calculation logic. Do not create a new calculation method.
- Keep the UI consistent with the existing dashboard design.
- Do not modify any other dashboard functionality.


# Future Updates Phase 17 – Cash Management System

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] **Current Balance = petty cash.** New `BalanceRepository` (single source): `balance = sales cash (paidAmount + customer due-payments received) + Cash In − Expenses − Cash Out`. **Buying no longer reduces it** (removed purchases' paidAmount *and* supplier due-payments `given`). `HomeViewModel` balance updated to match.
- [x] **Cash Buy** still recorded (Buy screen untouched) → still shows in Business Report Money Out + Daily Report; just doesn't touch the balance. **Due Buy** unchanged. **Sell** still increases balance as before.
- [x] **Expense guard**: `ExpenseViewModel.add` blocks an expense > current balance (dialog stays open, shows "Not enough balance…"). Balance never negative.
- [x] **Cash Out guard**: `CashViewModel.add` blocks a Cash Out > current balance (same error UX). Cash In/Out update the balance immediately (unchanged).
- [x] **Cash Management Report** (`CashReportScreen`/VM, Reports hub + route `CASH_REPORT`): Cash In/Out by **Day / Month / Custom range**, each row shows date-time/type/amount/note, with **Total Cash In** and **Total Cash Out**. Added DAO sum + between queries; no DB schema change.
- Decisions/notes: kept `received` (customer due collection still raises petty cash — "sells increase balance as now") but dropped `given` (supplier payments are buy-related). Expense **edit** and cash-row **delete** are not balance-guarded (only new expense / cash-out are, per spec). No separate "Buy Report" exists — cash buys appear in Business + Daily reports. assembleDebug ✓ (clean).

### Goal

Change the **Current Balance** to represent the shop's **petty cash** only. Product purchases should no longer affect the Current Balance.

### Requirements

- The **Current Balance** shown on the Home Dashboard should represent the shop's **petty cash** only.
- Buying products should **NOT** reduce the Current Balance.

### Buy Transactions

- The **Buy** screen currently has two payment methods:
  - **Cash**
  - **Due**
- The **Due** payment method should remain unchanged.
- If the user selects **Cash**, the purchase should still be recorded as a **Cash Buy** transaction.
- **Cash Buy** transactions should continue to appear in:
  - **Reports → Business Report → Money Out**
  - The new **Buy Report**
- However, **Cash Buy** transactions must **NOT** reduce the Current Balance or affect the petty cash balance in any way.

### Sell Transactions

- Selling products should continue to increase the Current Balance exactly as it does now.

### Expense Transactions

- Expense transactions should continue to reduce the Current Balance.
- Do not allow an expense that is greater than the Current Balance.
- If there is not enough Current Balance, prevent the transaction and display an appropriate error message.
- The Current Balance must never become negative.

### Cash Management

- Use the existing **Cash Management** option in **Settings**.
- From **Cash Management**, users should be able to:
  - Add Cash (Cash In)
  - Withdraw Cash (Cash Out)
- Every Cash In and Cash Out transaction should immediately update the Current Balance.
- Do not allow a Cash Out amount that is greater than the Current Balance.
- If there is not enough Current Balance, prevent the Cash Out transaction and display an appropriate error message.
- The Current Balance must never become negative.

### Cash Management Report

Add a new report called **Cash Management Report**.

The report should display all Cash In and Cash Out transactions.

Each record should include:

- Date and time
- Transaction type (Cash In / Cash Out)
- Amount
- Note or reason (if available)

The report should support:

- Single date report
- Monthly report
- Custom date range report

The report should also display:

- Total Cash In
- Total Cash Out

### Summary

After this update:

- Product **Sales** increase the Current Balance.
- **Expenses** reduce the Current Balance.
- **Cash In** increases the Current Balance.
- **Cash Out** reduces the Current Balance.
- **Cash Buy** transactions are recorded for reporting purposes only and do **NOT** affect the Current Balance.
- **Due Buy** transactions remain unchanged.
- The Current Balance always represents the shop's actual petty cash and must never become negative.

Do not modify any other functionality except as described above.

# Future Updates Phase 18 – Home Recent Activities

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] Home "Recent Activity" now uses the **same `ActivityViewModel` + logic** as Settings → Activities (`HomeScreen` gets its own `ActivityViewModel`, observes `activities`, `.take(10)`). Removed the old sales/purchases-only `HomeViewModel.recent` + its `ActivityItem` — **no duplicate activity system**.
- [x] Extracted the Activities row into a shared public `ActivityRowCard(item, onUndo?)` reused by both screens: Activities passes the undo lambda; Home passes none → **view-only, no Undo button** on Home.
- [x] Shows the **10 most recent** activities, same types (sell/buy/expense/cash in/out/stock adjustment/undo). Home layout otherwise unchanged. assembleDebug ✓ (clean).

### Goal

Update the **Recent Activities** section on the Home Dashboard to reuse the same data and logic as the **Activities** screen in **Settings**.

### Requirements

- The **Recent Activities** section on the Home Dashboard should use the **same data source and logic** as the **Activities** screen in **Settings**.
- Do **not** create a separate activity system or duplicate the logic.
- Display only the **10 most recent activities** on the Home Dashboard.
- The Home Dashboard should display the same activity types as the Activities screen.
- The Home Dashboard is **view-only**.
- Do **not** display the **Undo** button or allow any activity to be undone from the Home Dashboard.
- Users who want to view the complete activity history or undo eligible activities should use the **Activities** screen in **Settings**.
- Keep the current Home Dashboard layout unchanged, only update the Recent Activities section to reuse the existing Activities logic.
```
# Future Updates Phase 19 – Buy Report

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] New **Buy Report** in the Reports hub (`BuyReportScreen` + `BuyReportViewModel`, route `BUY_REPORT`).
- [x] Lists **all purchases (Cash + Due)** for the period — each purchase card shows date-time, payment type (Cash/Due), total, and its line items (**product name, quantity, buy price, line total**). Reuses `TxnEntry`/`TxnLine` + `purchaseRepo.observeBetween`/`getItemsForShop` — no new data/DB.
- [x] Filters: **Day / Month / Custom range**; shows **Total Buy Amount** for the period.
- [x] Buy functionality untouched (report reads only). assembleDebug ✓ (clean).

### Goal

Add a dedicated **Buy Report** to the **Reports** section so users can view and track all product purchase transactions.

### Requirements

- Add a new report called **Buy Report** in the **Reports** section.
- The report should display all product purchase transactions.
- Each record should include:
  - Date and time
  - Product name
  - Quantity purchased
  - Purchase price
  - Total amount
  - Payment type (**Cash** or **Due**)
- The report should support:
  - Single date report
  - Monthly report
  - Custom date range report
- Display all purchase transactions, including both **Cash** and **Due** purchases.
- At the end of the report, display the **Total Buy Amount** for the selected period.
- Reuse the existing buy transaction data. Do not create duplicate records or a separate database.
- Keep the report layout consistent with the other reports in the application.
- Do not modify the existing Buy functionality. This update only adds a reporting feature.

# Future Updates Phase 20 – Due Payment Validation

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] `PartyDetailViewModel.recordPayment` now validates against the party's live `net`: a **Received** payment can't exceed `net.coerceAtLeast(0)` (customer's outstanding receivable); a **Given** payment can't exceed `(-net).coerceAtLeast(0)` (supplier's outstanding payable). Over-amounts are blocked with *"Amount exceeds the outstanding due (…)."*
- [x] The **Record payment** dialog (`AmountChoiceDialog`) stays open and shows the error on failure (added an `onError` callback); the **Add due** dialog is unchanged (no cap on adding a due).
- [x] Reuses the existing due-payment logic; this is the only place payments are recorded (Due ledger → party detail), so validation is applied everywhere. Customer & supplier due balances can no longer go negative. assembleDebug ✓ (clean).

### Goal

Prevent customer and supplier due payments from exceeding the actual outstanding due amount.

### Requirements

#### Customer Due

- When collecting a due payment from a customer, the entered payment amount must not be greater than the customer's current outstanding due.
- If the entered amount exceeds the outstanding due, prevent the transaction and display an appropriate error message.
- The customer's due balance must never become negative.

#### Supplier Due

- When paying a supplier due, the entered payment amount must not be greater than the supplier's current outstanding due.
- If the entered amount exceeds the outstanding due, prevent the transaction and display an appropriate error message.
- The supplier's due balance must never become negative.

### Validation

- Apply this validation everywhere customer or supplier due payments can be made.
- Reuse the existing due payment logic wherever possible.
- Do not modify any other functionality.
- After this update, customer and supplier due balances must never become negative under any circumstances.


# Future Updates Phase 21 – Cash Management Transaction Deletion

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] Removed the **Delete** icon button from every Cash In / Cash Out row in `CashManagementScreen` (+ dropped the now-unused `Delete`/`IconButton` imports). All cash records are still fully **viewable** there.
- [x] Deleting/undoing a Cash In or Cash Out is now only possible via **Settings → Activities** (Phase 15's `undoCash` already reverses + removes the cash txn). `CashViewModel.delete` is left in place but no longer wired to any UI.
- [x] No other Cash Management functionality/UI changed. assembleDebug ✓ (clean).

### Goal

Restrict the deletion of Cash Management transactions so they can only be deleted from the **Activities** screen.

### Requirements

- In **Settings → Cash Management**, remove the **Delete** option for both **Cash In** and **Cash Out** transactions.
- Users should still be able to view all Cash In and Cash Out records in the Cash Management screen.
- Do **not** allow users to delete any Cash In or Cash Out transaction from the Cash Management screen.
- The only place where Cash In and Cash Out transactions can be deleted or undone should be:
  - **Settings → Activities**
- Reuse the existing Activities undo/delete functionality.
- Do not modify any other Cash Management functionality or UI.
- Keep everything else unchanged.

# Future Updates Phase 22 – Search Feature Improvement

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] **One common implementation**: new `core/util/SearchUtil` — query and text are lowercased and stripped of whitespace; the query splits into words and **every word must appear** in one of the item's fields. Case-insensitive, space-insensitive, partial and joined-word matching ("Note 14" ⇄ "Note14"); verified against the spec's exact example (Samsung/Note/Note14/Note 14/14/5G/sams/note1 → both products, 5G → only the 5G one).
- [x] **Relevance ranking**: earlier match position (e.g. name prefix) sorts first; ties keep original order. Pure in-memory string work — fast at on-device scale.
- [x] Applied to **every screen with a search box** (all previously used naive `contains`): **Products** (`ProductViewModel`, name), **Sell** (`SellScreen`, name — stock>0 rule kept), **Buy** (`BuyScreen`, name), **Sales ledger** (note/customer/amount), **Purchase ledger** (note/supplier/amount).
- [x] No UI changed and no new search boxes added — screens without a search (Expenses, Activities, Due ledger, Reports) are unchanged per "do not change any UI/functionality other than search behavior". assembleDebug ✓ (clean).

### Goal

Improve the search functionality throughout the entire application to make it easier and more flexible for users to find products and other records.

### Requirements

- Improve the search logic in all searchable sections of the application.
- The search should support partial word matching, regardless of spaces or formatting.

### Example

If the products are:

1. Samsung Note 14
2. Samsung Note14 5G

Searching for any of the following should return **both** products whenever applicable:

- Samsung
- Note
- Note14
- Note 14
- 14
- 5G
- sams
- note1

The search should not depend on exact spacing or exact text matching.

### Search Improvements

- Ignore spaces when matching search keywords.
- Ignore uppercase and lowercase letters (case-insensitive search).
- Support partial keyword matching.
- Match words even if they are joined together (for example, "Note14" and "Note 14").
- Return the most relevant matching results first.
- The search should feel fast and responsive, even with a large number of records.

### Apply to All Searchable Sections

Update the search functionality everywhere in the app where a search feature exists, including but not limited to:

- Products
- Sell
- Buy
- Customers
- Suppliers
- Reports
- Expenses
- Activities
- Any other screen that contains a search function.

### Requirements

- Reuse the existing search functionality wherever possible instead of creating separate search logic for each screen.
- Create one common search implementation that can be used throughout the application.
- Do not change any UI or existing functionality other than improving the search behavior.

# Future Updates Phase 23 – Category Sales & Purchase Report

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] New **Category Report** in the Reports hub (`CategoryReportScreen` + `CategoryReportViewModel`, route `CATEGORY_REPORT`).
- [x] Selectors: **Category** dropdown (top-level categories) + **Subcategory** dropdown with an **"All Subcategories"** default (switching category resets to All). Products directly under the category are included in "All".
- [x] Filters: **Day / Month / Custom range** (same pattern as Cash/Buy reports).
- [x] Per matching product: **name, qty purchased, purchase amount, qty sold, sales amount, profit** (profit = sales − buyPrice×qtySold, the app-wide convention used by Business/Daily reports).
- [x] **Summary** at the end: Total Purchase Quantity/Amount, Total Sales Quantity/Amount, Total Profit (or Loss).
- [x] Reuses existing sale/purchase items + products/categories flows — no new tables/records; no existing report touched. assembleDebug ✓ (clean).

### Goal

Add a dedicated **Category Report** to the **Reports** section so users can view sales and purchase information based on product categories and subcategories.

### Requirements

- Add a new report called **Category Report** in the **Reports** section.
- The report should allow the user to select:
  - **Category**
  - **Subcategory**
- Under the selected Category, include:
  - All available Subcategories.
  - An additional option called **All Subcategories** to include every subcategory within the selected Category.

### Report Filters

The report should support:

- Single date report.
- Monthly report.
- Custom date range report.

### Report Information

Display all matching products for the selected Category/Subcategory.

Each product should display:

- Product Name
- Total Quantity Purchased
- Total Purchase Amount
- Total Quantity Sold
- Total Sales Amount
- Total Profit

### Report Summary

At the end of the report, display:

- Total Purchase Quantity
- Total Purchase Amount
- Total Sales Quantity
- Total Sales Amount
- Total Profit

### Additional Requirements

- Reuse the existing sales and purchase transaction data. Do not create duplicate records or a separate database.
- Keep the report layout consistent with the other reports in the application.
- Do not modify any existing reporting functionality.
- This update only adds a new **Category Report** for viewing purchase, sales, quantity, and profit statistics by Category and Subcategory.

# Future Updates Phase 23.1 – Category Report Stock Summary

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] Added a **Current Stock Summary** section to the Category Report (below Products + Summary, shown whenever a category is selected): **Current Stock Quantity** card + **Current Stock Value** StatCard.
- [x] Computed over the same `matching` product list the report already builds — so **All Subcategories** = every product under the category, a specific subcategory = that subcategory only. Range-independent (always the *current* stock).
- [x] Reuses the Products-page stock calculation (Σ `stock`, Σ `stock × buyPrice` — same formula as `observeStockValue`); no duplicate data/queries. Existing Products/Summary sections untouched. assembleDebug ✓ (clean).

### Goal

Enhance the **Category Report** by adding a stock summary section that displays the current inventory status for the selected Category and Subcategory.

### Requirements

- In the **Category Report**, keep the existing sections:
  - Products
  -  Summary
- Add a new section called **Current Stock Summary**.

### Current Stock Summary

For the selected Category/Subcategory, display:

- Current Stock Quantity
- Current Stock Value

### All Subcategories

- If the user selects **All Subcategories**, the Current Stock Summary should display:
  - Total Current Stock Quantity of all products under the selected Category.
  - Total Current Stock Value of all products under the selected Category.

### Individual Subcategory

- If the user selects a specific Subcategory, the Current Stock Summary should display:
  - Current Stock Quantity for that Subcategory only.
  - Current Stock Value for that Subcategory only.

### Additional Requirements

- Reuse the existing stock calculation logic already used in the **Products** page.
- Do not create duplicate calculations or duplicate data.
- Keep the report layout consistent with the other reports in the application.
- Do not modify any existing Category Report functionality except for adding this new **Current Stock Summary** section.


# Future Updates Phase 24 – Product Filter by Category & Subcategory



**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] **Filter button next to the search box** (`FilterList` icon; tinted yellow while filters are active) → opens a **filter dialog** with checkboxes: every Category with its Subcategories indented beneath it; **Done** + **Clear all** buttons. Checkbox changes apply immediately and **persist until cleared/changed**.
- [x] **Multi-select**, union semantics: a product matches when its category is among the selected categories **or** its subcategory is among the selected subcategories — supports single/multiple categories, single/multiple subcategories, and combinations (`ProductViewModel.selectedCategoryIds`/`selectedSubCategoryIds`, replacing the old single-category filter).
- [x] **Default = no filters → all products**, and the header cards show all-product totals. With filters, **Total Units + Stock Value follow the filtered products** (same Σ stock / Σ stock×buyPrice calc); clearing restores the all-product totals. (Search narrows the list only, not the summary.)
- [x] The existing category chips row kept (layout unchanged) — chips now toggle categories in the same multi-select state; "All" = clear filters. No other Products-page functionality changed. assembleDebug ✓ (clean).



### Goal



Improve the **Products** page by adding filtering options for **Category** and **Subcategory**, allowing users to quickly view only the products they need.



### Requirements



- Add a **Filter** option next to the product search box.

- When the user taps the Filter option, open a filter dialog.



### Filter Options



The filter dialog should allow users to select:



- One or more Categories.

- One or more Subcategories.



The filter should support:



- Single Category selection.

- Multiple Category selection.

- Single Subcategory selection.

- Multiple Subcategory selection.

- A combination of Categories and Subcategories.



### Default Behavior



- By default, no filters are applied.

- The Products page should display **all products**.

- The summary cards at the top should display:

  - Total Units of all products.

  - Total Stock Value of all products.



### Filter Behavior



After the user applies one or more filters:



- Only the matching products should be displayed in the Products list.

- The summary cards at the top should automatically update to show:

  - Total Units of the filtered products.

  - Total Stock Value of the filtered products.



If the user clears all filters:



- Display all products again.

- Restore the Total Units and Total Stock Value to represent all products.



### Additional Requirements



- The selected filters should remain active until the user clears them or changes them.

- Reuse the existing Category and Subcategory data.

- Reuse the existing stock quantity and stock value calculation logic already used in the Products page.

- Do not create duplicate calculations or duplicate data.

- Keep the existing Products page layout unchanged except for adding the Filter option.

- Do not modify any other Products page functionality.

# Future Updates Phase 25 – Home Dashboard UI Redesign

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] **Yellow hero header** that extends behind the Android status bar: `TabShell`'s Scaffold now excludes the status-bar inset (`contentWindowInsets.exclude(WindowInsets.statusBars)`) so each tab draws its own chrome edge-to-edge — Home's header uses `background(BrandYellow).statusBarsPadding()`; the other tabs' yellow `AppTopBar`s now also blend into the status bar (standard M3 behaviour, no visual change below).
- [x] Header shows **"MINI POS | Shop Name"** (bold, ellipsized) + switch-shop icon, a prominent **Current Balance** hero (label + headline amount), and restyled **Day/Month** chips (dark-on-yellow `PeriodChip`).
- [x] Content below reorganized with clear grouping: money row (Today's/This Month's Sales · Expenses), dues row (You'll Receive · You'll Give), **Inventory** section (Types of Products · Total Units · Stock Value), Sell/Buy buttons, **Quick Access** section (same 6 shortcuts), **Recent Activity** (unchanged behaviour, now a SectionHeader) + credit line.
- [x] UI-only: same VMs/stats/actions, colors unchanged (BrandYellow/OnYellow), all features kept; bottom nav untouched. assembleDebug ✓ (clean).

### Goal

Redesign the **Home Dashboard** to provide a cleaner, more modern, and professional user experience while keeping all existing functionality unchanged.

### Design Freedom

You are free to redesign and rearrange the Home Dashboard using your own UI/UX judgment. The objective is to create a layout that looks modern, clean, organized, and easy to use. Feel free to improve spacing, alignment, grouping, icons, cards, and visual hierarchy while maintaining the MINI POS design language.

### Theme & Branding

- Keep the existing **MINI POS** color scheme unchanged.
- Do not change the application's branding or primary color palette.
- The redesigned Home Dashboard should feel like an improved version of the current design, not a completely different application.

### Top Section

- Extend the Home page background into the Android status bar so the status bar blends seamlessly with the app theme color.
- Display the shop name prominently at the top of the Home Dashboard.
- Update the title format to:

  **MINI POS | Shop Name**

### Dashboard Summary

Keep the following summary information at the top of the Home Dashboard:

- Current Balance
- Today's Sales
- Today's Expenses
- You Will Receive
- You Will Give
- Types of Products
- Total Units
- Stock Value

You are free to arrange these summary cards in the most user-friendly and visually balanced layout.

### Home Menu

- Redesign and reorganize the Home menu sections to make navigation cleaner and more intuitive.
- Group related features together where appropriate.
- Improve spacing, alignment, and overall visual hierarchy.
- Keep all existing menu functions and navigation unchanged.
- Do not remove any existing features.

### Bottom Section

Keep the following sections unchanged in terms of functionality:

- Recent Activities
- Bottom Navigation Bar

You may improve their visual appearance if needed, but do not change how they work.

### Additional Requirements

- Follow modern Material Design principles.
- Ensure the layout works well on both small and large Android devices.
- Keep the Home Dashboard lightweight and responsive.
- Reuse all existing business logic and data sources.
- This update is a **UI/UX redesign only**. Do not modify any business logic or existing functionality unless required for the new layout.

# Future Updates Phase 26 – Notification Tap Behavior

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] **Root cause:** `Notifier.show` never attached a `contentIntent`, so tapping any notification (low-stock, due, backup reminder) did nothing.
- [x] Added `Notifier.openAppIntent`: uses the package's **launcher intent** (`getLaunchIntentForPackage`, fallback explicit `MainActivity` + ACTION_MAIN/CATEGORY_LAUNCHER) + `FLAG_ACTIVITY_NEW_TASK`, wrapped in `PendingIntent.getActivity` with **`FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`** (Android 12+ requirement; works down to minSdk 24). Set via `setContentIntent` on every notification.
- [x] Added **`android:launchMode="singleTask"`** to `MainActivity` — closed → normal launch; backgrounded → existing task brought to foreground; already open → reused, never a second instance/copy.
- [x] Notification design/content/sound/scheduling untouched — only the tap behaviour fixed. assembleDebug ✓ (clean).

### Goal

Fix the notification behavior so that tapping a notification always opens the MINI POS app correctly.

### Requirements

- When the app displays a notification and the user taps it, the MINI POS app should open automatically.
- If the app is completely closed, tapping the notification should launch the app normally.
- If the app is running in the background, tapping the notification should bring the existing app to the foreground instead of creating another instance.
- If the app is already open, tapping the notification should navigate to the appropriate screen instead of opening multiple copies of the app.
- Ensure the notification uses the correct `PendingIntent`, `Intent` flags, and Android launch mode so the behavior is consistent across supported Android versions.

### Additional Requirements

- Keep all existing notification functionality unchanged.
- Do not change the notification design, content, sound, or scheduling.
- Only fix the notification tap behavior so users can reliably open the app from any notification.
- Ensure the solution follows Android best practices and works consistently on supported Android versions.

# Future Updates Phase 27 – Cash Drawer

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] New **Cash Drawer** feature (`feature/cashdrawer/` Screen+VM, route `CASH_DRAWER`); the Home **Products shortcut is replaced by Cash Drawer** (`PointOfSale` icon — Products remains in the bottom nav, nothing lost).
- [x] **Per-day view** (Day mode, defaults to today, any day pickable): Opening Cash (with **Set/Edit** dialog), **Cash Sales** (Σ sale `paidAmount`), **Customer Due Collections** (due payments RECEIVED), **Cash In**, **Cash Out**, **Expenses**, and computed **Closing Cash = opening + cash sales + collections + cash in − cash out − expenses**. Updates live as transactions happen.
- [x] **Buying / supplier payments / due purchases never touch the drawer** (simply not part of the formula) — cash-movement only, fully **independent of the Current Balance** (untouched).
- [x] **History**: Month / Custom-range modes list one card per day (all 7 values), newest first — days appear when they have an opening or any cash activity.
- [x] Data: only the user-entered Opening Cash is stored — new `cash_drawer_openings` table (one row per shop+day, unique index, upsert-replace), **DB v4→v5** (`MIGRATION_4_5`), `CashDrawerDao` + `CashDrawerRepository`; everything else derived live from existing sales/due-payment/cash/expense data (no duplicated records). assembleDebug ✓ (clean).

### Goal

Add a new **Cash Drawer** feature to track all daily cash movements separately from the Current Balance.

### Overview

The Cash Drawer should represent the cash movement for the current business day, similar to a physical cash drawer used in retail stores.

Unlike the Current Balance, the Cash Drawer is intended to show today's cash activity only.

### Home Dashboard

- Replace the **Products** shortcut on the Home Dashboard with a new **Cash Drawer** shortcut.
- Tapping the shortcut should open the Cash Drawer screen.

### Cash Drawer Information

The Cash Drawer should display the following information for the selected day:

- Opening Cash
- Cash Sales
- Customer Due Collections
- Cash In
- Cash Out
- Expenses
- Closing Cash

### Cash Drawer Calculation

The Cash Drawer should calculate the daily cash as follows:

```
Closing Cash =
Opening Cash
+ Cash Sales
+ Customer Due Collections
+ Cash In
- Cash Out
- Expenses
```

### Important

- Buying products must **NOT** reduce the Cash Drawer.
- Supplier payments must **NOT** reduce the Cash Drawer.
- Due purchases remain unchanged and do not affect the Cash Drawer.
- The Cash Drawer should only include transactions that involve actual cash movement.

### Daily Operation

- At the beginning of each day, the user can enter the **Opening Cash**.
- If an Opening Cash already exists for that day, allow it to be edited.
- Throughout the day, the Cash Drawer should update automatically as cash transactions occur.
- At the end of the day, the Closing Cash should be calculated automatically.

### Cash Drawer History

Allow users to view previous Cash Drawer records.

Support:

- Daily view
- Monthly view
- Custom date range

Each day's record should display:

- Opening Cash
- Cash Sales
- Customer Due Collections
- Cash In
- Cash Out
- Expenses
- Closing Cash

### Additional Requirements

- Reuse the existing transaction data wherever possible.
- Do not duplicate existing sales, expense, or cash management records.
- Keep the Cash Drawer completely independent from the Current Balance.
- The Current Balance should continue to work exactly as it does now.
- The Cash Drawer is an additional feature for monitoring daily cash movement only.
- Keep all existing functionality unchanged except for adding the Cash Drawer feature.


# Future Updates Phase 27.1 – Cash Drawer Enhancement

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] **Automatic Opening Cash**: `Opening(day) = manual base opening + net cash flow of every day since` — mathematically identical to carrying forward the previous day's Closing (empty days pass through unchanged). Computed live in `CashDrawerRepository.observeDay`/`observeHistory` (history chains openings day-by-day inside the range, seeded from before the range).
- [x] ~~Manual entry = first day only~~ **Owner follow-up (same day): Opening Cash editing removed entirely** — no Set/Edit button or dialog anywhere; the opening is always the auto-carried value (fresh install starts at ৳0) and the card shows *"Carried forward from the previous day's closing."*. Removed `OpeningCashDialog` + VM `canEditOpening`/`setOpening`; the `cash_drawer_openings` table and repo write API remain (any previously saved seed is still honored).
- [x] **Daily transaction list** (Day mode, under the drawer card): all cash-related transactions — Cash Sale (paid amount), Due Collection (customer name), Cash In/Out (note), Expense (note) — each with **date-time, type, description, signed amount and the running drawer balance after it** (oldest first). **No purchases / supplier payments / due buys** anywhere.
- [x] Live-updating; reuses existing records (no duplicates); drawer formula unchanged; no schema change (still DB v5 — the openings table just holds the first-day seed now). assembleDebug ✓ (clean).

### Goal

Improve the Cash Drawer by making the opening balance automatic and displaying all daily cash transactions in one place.

### Opening Cash

- The **Opening Cash** for a new day should automatically be the **Closing Cash** from the previous day.
- Users should not need to enter the Opening Cash manually each day.
- If there is no previous day's record (for example, after a fresh installation), the user can manually enter the Opening Cash for the first day only.
- From the second day onward, the Opening Cash should always be carried forward automatically from the previous day's Closing Cash.

### Daily Transactions

The Cash Drawer should automatically display **all cash-related transactions** for the selected day.

The transaction list should include:

- Cash Sales
- Customer Due Collections
- Cash In
- Cash Out
- Expenses

Do **not** include:

- Cash Buy
- Due Buy
- Supplier Due Payments
- Any purchase-related transactions

### Transaction Details

Each transaction should display:

- Date and Time
- Transaction Type
- Description or Reference
- Amount
- Running Cash Drawer Balance after the transaction (if available)

### Additional Requirements

- The transaction list should update automatically whenever a new cash-related transaction is created.
- Reuse the existing transaction records wherever possible. Do not create duplicate data.
- Keep the existing Cash Drawer calculations unchanged.
- Do not modify any other functionality except as described above.

# Future Updates Phase 28 – Barcode Management, Scanner & Barcode Printing

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] **Barcode column** on `products` (nullable, indexed) — **DB v5→v6** (`MIGRATION_5_6`). Uniqueness enforced **per shop** in app logic (global-unique would break restore-into-new-shop).
- [x] **Auto-generation**: blank barcode on save → `ProductRepository.generateBarcode` (13-digit numeric "2"+epoch+random, uniqueness-checked loop). **Validation**: duplicate barcode blocks the save with an inline error (`ProductViewModel.save` now returns an error message).
- [x] **Manual entry + scan-to-fill** in the product form (barcode field with camera-scan button; "leave empty to auto-generate").
- [x] **Backfill**: app start (`MiniPosApp.backfillBarcodes`) + after **backup restore** (`BackupManager`) — every product without a barcode gets a unique one; existing barcodes untouched. Backup schema **v4** (barcode in `Product` @Serializable; old v2/v3 backups still restore, MIN stays 2).
- [x] **Camera scanning** (`feature/barcode/BarcodeScannerDialog`, ZXing `zxing-android-embedded` — fully offline, CAMERA permission w/ runtime request, continuous decode with 2s same-code dedupe, per-scan feedback line): **Products** (scan → open product; not-found message), **Sell** (scan → add to cart, respects out-of-stock; continuous), **Buy** (scan → add to cart; continuous) — scan buttons on each search field.
- [x] **Barcode Printing** (Settings → Catalog → "Barcode Printing", route `BARCODE_PRINT`; `feature/barcodeprint/`): select one/many/all products with per-product **label counts** (QtyStepper), toggle label fields (**barcode number, name, category, subcategory, selling price, buying price**), customize **width/height/margin (mm), labels per row, labels per page** on A4; **Save as PDF** (SAF) and **Print** (Android print framework → wireless/USB/Bluetooth printers) via `LabelPdf` (CODE-128, oversampled bitmaps, cut borders, ellipsized text) + `PdfPrintAdapter`.
- [x] New deps: `com.journeyapps:zxing-android-embedded:4.3.0` + `com.google.zxing:core:3.5.3` (offline). Uses existing product data only — no duplicate barcode records. assembleDebug ✓.

### Goal

Add a complete barcode management system that supports automatic barcode generation, manual barcode entry, barcode scanning using the device's camera, and barcode label printing.

---

## Barcode Generation

- Every product must have a unique barcode.
- When a new product is created, if no barcode is provided, the app should automatically generate a unique barcode.
- Automatically generated barcodes must always be unique.
- One barcode can belong to only one product.

---

## Manual Barcode Entry

When creating or editing a product, users should be able to:

- Enter a barcode manually.
- Scan an existing barcode using the device's camera to automatically fill the barcode field.

This is useful because many products already have manufacturer barcodes printed on the packaging.

---

## Barcode Validation

- Do not allow duplicate barcodes.
- Before saving a product, verify that the barcode is not already assigned to another product.
- If a duplicate barcode is detected, prevent saving and display an appropriate error message.

---

## Existing Products

Some users already have backups created before this feature is added.

After restoring an old backup:

- Check every product.
- If a product does not have a barcode, automatically generate a unique barcode for that product.
- Existing products that already have a barcode should remain unchanged.

---

## Barcode Scanning

Use the mobile device's camera as a barcode scanner.

Add barcode scanning support in the following areas:

### Products

- Scan a barcode to quickly find and open the product.

### Buy

- Scan a barcode to automatically find the product and add it to the Buy transaction.

### Sell

- Scan a barcode to automatically find the product and add it to the Sell transaction.

### Scanner Behavior

- Use the device's built-in camera.
- Support continuous scanning so users can quickly scan multiple products.
- If a scanned barcode does not exist, display an appropriate message indicating that the product was not found.
- Keep the scanning process fast and responsive.

---

# Barcode Printing

Add a new **Barcode Printing** option inside **Settings**.

The Barcode Printing feature should allow users to generate barcode labels for one or multiple products.

## Product Selection

Users should be able to:

- Select one product.
- Select multiple products.
- Select all products if desired.

For every selected product, users can specify how many barcode labels they want to print.

Example:

- Product A → 5 Labels
- Product B → 10 Labels
- Product C → 2 Labels

The system should automatically generate the requested number of barcode labels.

---

## Barcode Label Customization

Allow users to choose what information appears on the barcode label.

Each item should have an Enable/Disable option.

Supported fields include:

- Barcode
- Product Name
- Category
- Subcategory
- Selling Price
- Buying Price

Users can choose exactly which information should appear on the printed barcode label.

---

## Label Size Customization

Allow users to customize the barcode label size.

Support:

- Width
- Height
- Margins
- Number of labels per row
- Number of labels per page

The layout should automatically adjust based on the selected paper size and label dimensions.

---

## PDF Export

Allow users to save barcode labels as a PDF file.

The generated PDF should be ready for professional printing without requiring additional formatting.

---

## Printing

Allow users to print barcode labels directly from the app using Android's built-in printing framework.

Support:

- PDF printing
- Wireless printers
- USB printers (if supported by Android)
- Bluetooth printers (if supported by Android)

---

## Additional Requirements

- Reuse the existing product and barcode database.
- Do not create duplicate barcode records.
- Keep the barcode system completely offline.
- Ensure barcode data is included in the existing Backup and Restore system.
- Barcode Printing should use the existing product barcode data and should not generate duplicate barcode records.
- Keep the interface simple and user-friendly.
- Do not modify any existing functionality except for adding the barcode management, barcode scanning, and barcode printing features.

# # Future Updates Phase 28.1 – Barcode Printing Search, Filter & Scanner Enhancement

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] **Barcode Printing search**: search box (name or barcode digits) using the shared `SearchUtil` — same partial / case-insensitive / space-insensitive matching as the Products page; instant filtering.
- [x] **Barcode Printing filter**: same multi-select Category/Subcategory filter as Products — the dialog was **extracted to a shared `ProductFilterDialog`** and the union-match logic to a shared **`ProductFilters`** object; both pages now use the single implementation (Products page refactored to it, zero behaviour change). Search + filters compose; **Select all selects the visible (filtered) list** while keeping prior selections; hidden-but-selected products still print (PDF resolves against the unfiltered list).
- [x] Bonus per spec's scanner list: **scan-to-select** on the printing page — each scan selects the product / adds one more label ("Name — N label(s)").
- [x] **Scanner enhancement (shared `BarcodeScannerDialog`, applies everywhere — Products/Sell/Buy/form/printing)**: short **vibration** (VibratorManager on S+, VIBRATE permission) + **confirmation beep** (ToneGenerator on the notification stream, follows system volume) + green **"✓ Scanned: …" flash** (~1.6s) on every accepted scan; **sliding-window dedupe** — while the camera keeps seeing the same code the window refreshes, so it can't re-fire until the code leaves view ~2s (single-shot screens still auto-close via their callbacks).
- [x] One shared implementation for search/filter/scanner — future improvements apply everywhere. assembleDebug ✓ (clean).

### Goal

Improve the **Barcode Printing** page by adding advanced search and filtering features, and improve the barcode scanning experience across the entire application.

---

## Barcode Printing Search

- Add a search box to the Barcode Printing page.
- Reuse the same search functionality already implemented on the **Products** page.
- The search should support:
  - Partial keyword matching.
  - Case-insensitive search.
  - Matching regardless of spaces or formatting.
- Searching should instantly filter the product list.

---

## Barcode Printing Filter

- Add a **Filter** option to the Barcode Printing page.
- Reuse the same filter functionality already available on the **Products** page.

The filter should support:

- Single Category selection.
- Multiple Category selection.
- Single Subcategory selection.
- Multiple Subcategory selection.
- A combination of Categories and Subcategories.

---

## Product List

- By default, display all products.
- After applying one or more filters, display only the matching products.
- The search and filters should work together.
- Users should be able to search within the filtered product list.
- After searching and filtering, users should be able to select one or multiple products for barcode printing.
- Keep all existing barcode printing functionality unchanged.

---

## Barcode Scanner Enhancement

Improve the barcode scanning experience throughout the entire application.

Apply these improvements to every barcode scanning feature, including:

- Products
- Buy
- Sell
- Product Create/Edit
- Barcode Printing
- Any future barcode scanning screens

### Scanner Behavior

- Once a barcode is successfully scanned, immediately provide clear feedback to the user.
- For screens that require only a single barcode scan (such as Product Create/Edit), automatically close the scanner after a successful scan.
- For screens that support continuous scanning (such as Buy , Sell, products , ets), keep the scanner open but clearly indicate that the barcode has already been scanned before allowing the next scan.
- Make the device vibrate briefly after every successful scan.
- Play a short confirmation sound (if supported by the device and enabled by the system settings).
- Display a small success message confirming that the barcode was scanned successfully.
- Prevent the same barcode from being scanned repeatedly while the camera is still pointed at the barcode.
- Always provide clear visual or haptic feedback so the user immediately knows whether the scan was successful.

---

## Code Reuse

- Reuse the existing **Search**, **Category Filter**, **Subcategory Filter**, and **Barcode Scanner** implementation wherever possible.
- Do not create duplicate search, filtering, or barcode scanning logic.
- Maintain a single shared implementation so future improvements automatically apply to the **Products** page, **Barcode Printing** page, and every other barcode scanning feature.

---

## Additional Requirements

- Keep the Barcode Printing page fast and responsive, even with a large number of products.
- Do not modify any existing Barcode Printing functionality except for adding the search, filtering, and barcode scanner enhancements.
# Future Updates Phase 29 – Receipt Printing & PDF Reports

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] **Receipts (Sell + Buy)**: after every completed sale/purchase a **"Print receipt?" Yes/No dialog** appears (checkout callbacks now return the new sale/purchase id). Thermal-style narrow PDF (`core/print/ReceiptPdf`, width from settings, auto height) with shop name/address/phone, invoice no (INV-S/B-######), date-time, customer/supplier, items (qty × price, line totals), TOTAL, discount, payment type, paid, remaining due, footer message — printed via Android's print framework (offline).
- [x] **Reprint**: a print icon on every row of the Sales ledger and Purchase ledger regenerates the same receipt from the stored transaction (nothing modified).
- [x] **PDF export on ALL reports** — one generic renderer (`core/print/ReportPdf`, title + line model, auto page breaks, A4/Letter from settings; new sizes = one map entry) + a shared top-bar action (`ReportPdfAction`: **Preview / Save PDF / Print**) wired into **Business, Stock, Buy, Category, Cash Management, Cash Drawer (day + history) and Product History** reports. Preview opens the device PDF viewer via a new **FileProvider**; Print's system dialog also previews.
- [x] **Settings → Printer Settings** (`PrinterSettingsScreen`, route `PRINTER_SETTINGS`): receipt **width/margin/font**, **footer message**, **PDF paper (A4/Letter)** — stored once in DataStore (`core/print/PrintPrefs`) and applied to every job; **"Print test receipt"** button. Printer connection = Android system print dialog (Bluetooth/USB/Wi-Fi via installed print services; system remembers the printer — noted on the screen).
- [x] Reuses existing transaction/report data; no duplicate records; fully offline; independent of Backup/Restore. assembleDebug ✓ (clean).

### Goal

Add a complete receipt printing system for transactions and PDF export for reports, with centralized printer settings and customizable paper sizes.

---

## Transaction Receipts

Generate printable receipts for the following transactions:

- Sell
- Buy

Each completed transaction should automatically generate a receipt.

### Print Confirmation

After a Sell or Buy transaction is completed:

- Display a confirmation dialog asking:
  - **Do you want to print the receipt?**
- If the user selects **Yes**, print the receipt using the configured printer.
- If the user selects **No**, simply complete the transaction without printing.

---

## Receipt Format

Receipts should use the standard small receipt paper commonly used in POS systems.

The receipt should include all necessary transaction details, including:

- Shop Name
- Shop Address (if available)
- Contact Information (if available)
- Invoice Number
- Date & Time
- Customer or Supplier Name (if applicable)
- Product List
- Quantity
- Unit Price
- Total Amount
- Discount (if applicable)
- Payment Type (Cash / Due)
- Amount Paid
- Remaining Due (if applicable)
- Footer message (if configured)

The receipt layout should be clean, professional, and optimized for thermal receipt printers.

---

## Receipt Reprinting

Users should be able to reprint receipts at any time.

### Sell

- In the **Sell** page, there is already a transaction history.
- Add a **Reprint Receipt** option for every Sell transaction.

### Buy

- In the **Buy** page, there is already a transaction history.
- Add a **Reprint Receipt** option for every Buy transaction.

Reprinting should generate the same receipt without modifying the original transaction.

---

# PDF Reports

All reports in the application should support PDF export.

Generate reports using standard full-size paper suitable for printing.

Supported reports include, but are not limited to:

- Business Report
- Stock Report
- Buy Report
- Category Report
- Cash Management Report
- Cash Drawer Report
- Product History
- Any future reports added to the application

Users should be able to:

- Preview the PDF.
- Save the PDF.
- Print the PDF directly.

---

# Printer Settings

Add a new **Printer Settings** option inside **Settings**.

From this page, users should be able to configure all printing options.

### Printer Connection

Allow users to connect printers using supported Android printing methods, including:

- Bluetooth Printer
- USB Printer (if supported)
- Wi-Fi / Network Printer
- Android System Print Service

The selected printer should be remembered for future printing.

---

## Receipt Paper Settings

Allow users to configure the default receipt paper.

Supported settings include:

- Paper Width
- Paper Height (if required)
- Margins
- Font Size
- Print Density (if supported)

These settings should be saved and automatically used for every receipt.

Users should not need to configure the paper size every time they print.

---

## PDF Paper Settings

Allow users to configure the default paper size for PDF reports.

Support common paper sizes such as:

- A4
- Letter


Future paper sizes can be added without changing the existing implementation.

---

## Additional Requirements

- Reuse the existing transaction and report data.
- Do not create duplicate transaction records.
- Keep all printing functionality completely offline.
- Ensure receipt printing and PDF generation work independently of the Backup and Restore system.
- Keep the interface simple and easy to use.
- All printer settings should be stored once and automatically applied to future print jobs.
- Do not modify any existing transaction or reporting functionality except for adding printing and PDF export capabilities.

# Future Updates Phase 30 – Advanced Printing Settings & Customization

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] **Settings → Printing Settings** (renamed/expanded from Phase 29's screen) — the single hub for all print customization, applied to every printable document.
- [x] **Receipt/invoice content — 19 individually toggleable items** (`ReceiptField` enum): logo, shop name, address, phone, **email**, **website** (new text fields — Shop has no such columns; stored in print prefs), invoice no, date & time, customer/supplier, payment method, product details, quantity & unit price, discount, **subtotal** (new line), grand total, paid, due, thank-you message, custom footer. `ReceiptPdf` rewritten to honour every toggle (no empty space for disabled/missing items).
- [x] **Shop logo on documents**: auto-scaled/positioned on receipts (from `Shop.logoPath`) and on report PDFs; skipped cleanly when absent or disabled.
- [x] **Report decoration — toggleable** (`ReportField`): logo, shop information, report title, generated date & time, footer, custom notes — applied centrally in `ReportPdfAction` (now takes `shopId`) so ALL reports get it automatically; `ReportPdf` gained a `ReportDecor` (logo/header/footer/notes + **font size + margins** from settings).
- [x] **Appearance**: receipt font size + margins + **text alignment (Center/Left)**; report font size + margins; header/footer visibility via the LOGO/SHOP_INFO/TITLE/FOOTER toggles.
- [x] **Paper flexibility**: A4/Letter is only the base layout; the Android print dialog outputs to **any paper size the printer supports** (auto-scaled) — stated on the screen. **Preview** available on every export (Preview action + the print dialog's own preview).
- [x] Disabled-fields stored as sets → future items default to enabled (easily extendable). Toggle storage in `PrintPrefs`; no business logic touched. Notes: a Tax line is omitted (sales don't record tax — "if applicable" ⇒ not applicable); report date-range/summary/totals remain part of each report's own body content. assembleDebug ✓ (clean).

### Goal

Create a centralized **Printing Settings** section where users can customize the appearance and content of **all printed documents** in the application, including receipts, invoices, reports, QR codes, barcodes, and any future printable documents.

### Requirements

### Printing Settings

Add a dedicated **Printing Settings** section inside **Settings**.

All printing-related customization options should be managed from this single location.

### Supported Print Types

The Printing Settings should apply to all printable documents, including:

- Sales Receipts / Invoices
- All Reports
- QR Code printing
- Barcode printing
- Any future printable documents

### Print Layout

- Use the Android printing system's default page settings.
- Do not limit printing to only A4 or Letter paper sizes.
- Allow printing on any paper size supported by the selected printer.
- Automatically adapt the layout to the selected paper size.
- Ensure all printed documents remain clean, professional, and properly aligned regardless of paper size.

### Shop Logo

- Allow users to enable or disable the Shop Logo for printed documents.
- If a Shop Logo is available, print it on receipts, invoices, and reports.
- Automatically resize and position the logo so it fits perfectly with the selected paper size and document layout.
- If no Shop Logo has been added, generate the document normally without leaving empty space.

### Receipt & Invoice Customization

Allow users to customize what information appears on printed receipts and invoices, including:

- Shop Logo
- Shop Name
- Shop Address
- Phone Number
- Email
- Website
- Invoice Number
- Invoice Date & Time
- Customer Information
- Payment Method
- Product Details
- Quantity
- Unit Price
- Discount
- Tax (if applicable)
- Subtotal
- Grand Total
- Paid Amount
- Due Amount
- Thank You Message
- Custom Footer Message

Users should be able to enable or disable each item individually.

### Report Customization

Allow users to customize printed reports, including:

- Shop Logo
- Shop Information
- Report Title
- Report Date
- Generated Date & Time
- Selected Date Range
- Report Summary
- Totals
- Footer
- Custom Notes

Users should be able to enable or disable each item individually.

### Print Appearance

Allow users to customize:

- Font Size
- Text Alignment
- Margins
- Header Visibility
- Footer Visibility

### Preview

- Provide a print preview before printing whenever practical.

### General Requirements

- Keep all customization options inside **Settings → Printing Settings**.
- Reuse the existing printing logic wherever possible.
- Do not modify any business logic or report calculations.
- Ensure all printable documents maintain a clean and professional appearance on any supported printer and paper size.
- Design the Printing Settings to be easily extendable for future printable documents and customization options.


# Future Updates Phase 31 – Default Unit Selection

**Status:** ✅ COMPLETE (2026-07-01)

### Implementation
- [x] **Default unit stored per shop**: `ShopSettings.defaultUnit` (default **"pcs"** — the PCS unit every shop already seeds) — **DB v6→v7** (`MIGRATION_6_7`, `DEFAULT 'pcs'` for existing shops); included in backups automatically (old backups restore with "pcs").
- [x] **New products pre-select the default unit** (`ProductFormScreen` applies it once on create, only while no unit is chosen and the unit exists) — users can still pick any other unit or none; **editing existing products is untouched**.
- [x] **Settings → Units**: the default unit shows a yellow **"Default"** badge; every other unit has a **"Set default"** action — newly created products immediately use the new choice. Renaming the default unit keeps the default pointing at the new name.
- [x] Reuses the existing Units system (no new tables); unit management (add/rename/delete) unchanged. assembleDebug ✓ (clean).

### Goal

Set **PCS (Pieces)** as the default unit throughout the application to simplify product creation and reduce manual selection.

### Requirements

- In **Settings → Units**, set **PCS** as the default unit.
- Whenever a user creates a new product, the unit should automatically be set to **PCS**.
- Users can still select a different unit if needed.
- The default unit should only be pre-selected; it should not prevent users from choosing another available unit.
- If the user changes the default unit in **Settings → Units**, all newly created products should use the newly selected default unit.
- Existing products should remain unchanged and continue using their currently assigned unit.
- Reuse the existing Units system and do not modify any other product or inventory functionality.
- Keep the UI and existing unit management features unchanged.


# Future Updates Phase 32 – Automatic Backup System

**Status:** ✅ COMPLETE (2026-07-04)

### Implementation
- [x] **Auto Backup section in Backup & restore** (`BackupScreen`): enable/disable switch (**enabled by default** for new installs), backup-folder row (Android folder picker via SAF `OpenDocumentTree`; persisted with `takePersistableUriPermission`, remembered across restarts, changeable anytime — old folder permission released), **frequency** dialog (Every day / 2 / 3 / 7 / 15 / 30 days, default **1 day**), **backup time** dialog (Material TimePicker, default **11:00 PM**), plus "Last automatic backup: …" status line.
- [x] **Settings persist** in new `AutoBackupPrefs` (DataStore `minipos_auto_backup_prefs`: enabled / folderUri / frequencyDays / hour / minute / lastSuccessAt; `active` = enabled AND folder chosen).
- [x] **Background job**: `AutoBackupWorker` + `AutoBackupScheduler` — self-rescheduling one-time WorkManager job (same pattern as the backup reminder) firing at the exact configured time. **Reuses `BackupManager.export()`** — identical zip format as manual backups; backs up the **current shop** into the chosen folder as `MiniPOSAuto_<shop>_<yyyyMMdd_HHmmss>.zip` (writes via `DocumentFile`; new `androidx.documentfile:1.0.1` dep). Half-written files are deleted on export failure.
- [x] **Retention**: after each success, keeps the newest **15** `MiniPOSAuto_*` zips and deletes older ones — manual backups (always `MiniPOS_*`, different prefix) are never touched.
- [x] **Missed-backup catch-up**: `AutoBackupScheduler.sync()` on every app open — next run = last-success date + frequency at the set time; if that moment already passed (phone off / app closed), the backup runs **immediately in the background**, then the normal schedule continues.
- [x] **Reminder interplay**: the daily backup-reminder notification keeps working while auto backup is off, and is **suppressed while auto backup is active** (gated in `MiniPosApp`, `BackupReminderWorker`, `SettingsViewModel` and on every auto-setting change; re-scheduled per its own prefs when auto backup is turned off).
- [x] **Notifications** (offline, existing `Notifier` channel): folder unavailable / export error → "Automatic backup failed" (ID 1004); **no successful auto backup for 3 days** → "Automatic backup needs attention" (ID 1005). Failed runs retry the next day at the set time (never a hot loop).
- [x] Manual backup & restore untouched; 100% offline. assembleDebug ✓ (clean).

### Goal

Improve the existing backup system by adding a fully automatic backup feature that allows users to schedule backups without manual intervention.

### Requirements

## Auto Backup

- Add a new **Automatic Backup** section inside the existing **Backup** settings.
- The feature should be **enabled by default** for new installations.
- Users should be able to **enable or disable** Automatic Backup at any time.

## Backup Location

- Allow users to select the backup folder **once** using the Android folder picker.
- The application should remember the selected folder and use it for all future automatic backups.
- Users can change the backup folder at any time from the Backup settings.

## Backup Schedule

- Allow users to configure:
  - **Backup Frequency** (default: **every 1 day**).
  - **Backup Time** (default: **11:00 PM** every day).
- The application should automatically create a backup based on the configured schedule.

## Backup Retention

- Keep the **latest 15 automatic backups**.
- When a new automatic backup is created and the total exceeds **15**, automatically delete the oldest automatic backup.
- Manual backups should **not** be affected by this cleanup process.

## Backup Notifications

- The existing daily backup reminder notification should continue to work when **Automatic Backup is disabled**.
- If **Automatic Backup is enabled**, disable the daily backup reminder notification since backups are being created automatically.
- If an automatic backup has **not been successfully created for 3 consecutive days**, display a notification reminding the user to open the app and check the backup system.

## Missed Backup Handling

- If the scheduled backup time passes while the device is turned off or the application is not running, the backup cannot be created at that time.
- In this case, when the user next opens the application, it should automatically detect the missed backup schedule and create the pending backup immediately.
- After the backup is completed, the normal backup schedule should continue as configured.

## Backup Process

- Automatic backups should use the same backup format and logic as the existing manual backup system.
- Reuse the existing backup functionality wherever possible.
- Do not create a separate backup format.
- Manual backups should continue to work alongside automatic backups.

## General Requirements

- Automatic backups should run in the background without interrupting normal app usage.
- If the selected backup folder is unavailable, notify the user with an appropriate error message.
- Keep the existing manual backup and restore features unchanged.
- The Automatic Backup feature should work completely offline.
- The automatic backup settings should remain saved after the device restarts or the application is reopened.
- Do not modify any existing backup or restore functionality except to add the new automatic backup feature.

# 
# Future Updates Phase 33 – Settings Page Reorganization

**Status:** ✅ COMPLETE (2026-07-04)

### Implementation
- [x] **Pure layout change in `SettingsScreen`** — every item, label, dialog, callback and behavior is untouched; only grouping, order, spacing and icons changed. No other file modified.
- [x] **9 fragmented sections → 7 logical groups**, each rendered as **one grouped card** (`AppCard(contentPadding = false)`) with thin inset dividers between rows — replaces 17 separate one-row cards for a much cleaner, standard settings look:
  - **Shop** (Manage shops) · **Inventory** (Products & inventory, Categories, Units, **Low-stock threshold** — moved next to its inventory siblings) · **Money** (Cash Management, Expenses, Expense categories, Due ledger) · **Printing** (Printing Settings, Barcode Printing — pulled out of "Catalog" into their own group) · **Notifications** (Low-stock alerts, Due reminders, Backup reminder, Backup reminder time) · **Data & history** (Backup & restore, Activities) · **App** (License Management, About MINI POS).
- [x] **De-duplicated icons** so every row is visually distinct: Expense categories → Label (was Category twice), Low-stock threshold → Tune (was Warning twice), Backup reminder → Alarm (was Backup twice), Activities → History. All icons keep the BrandYellow tint; theme/design language unchanged.
- [x] Every setting appears exactly once; no duplicates existed or were created. assembleDebug ✓ (clean).

### Goal

Reorganize the **Settings** page to make it cleaner, more professional, and easier to navigate.

### Requirements

- This update is **only** for reorganizing the **Settings** menu.
- Give full freedom to redesign the layout, grouping, and order of the Settings page.
- Use your best UI/UX judgment to create the most professional and user-friendly Settings page possible.

### Important

- **Do NOT change any existing functionality.**
- **Do NOT add any new features.**
- **Do NOT remove any existing features.**
- **Do NOT change the behavior of any setting.**
- **Do NOT rename existing features unless absolutely necessary for better clarity.**
- **Do NOT modify any business logic.**
- **Do NOT change any workflows or user interactions.**

### Scope

- Rearrange all existing menu items into the most logical categories.
- Remove duplicate menu items or duplicate sections if any exist.
- Ensure every setting appears only once.
- Improve the visual hierarchy, spacing, grouping, icons, and overall organization.
- Make related settings easy to find.
- Keep the MINI POS theme, colors, and design language consistent.

### General Requirements

- Preserve **100% of the existing functionality**.
- The only purpose of this update is to make the **Settings** page look cleaner, more organized, and more professional.
- Think like a professional UI/UX designer and reorganize the menu for the best possible user experience without changing how the application works.

# Future Updates Phase 34 – Home Dashboard Layout Reorganization

**Status:** ✅ COMPLETE (2026-07-04)

### Implementation
- [x] **Dashboard summary untouched**: yellow hero header (Current Balance) + Today's Sales / Today's Expenses / You'll Receive / You'll Give cards — same calculations, layout and position.
- [x] **Inventory boxes equalized**: all three tiles (Types of Products, Total Units, Stock Value) now render through one `CountTile` at identical size (`Row(height(IntrinsicSize.Min))` + `weight(1f).fillMaxHeight()`); Stock Value shows the same `Money.format` value it always did. Sell/Buy big buttons keep their spot below.
- [x] **New "Ledger Books" section** — balanced 2×2 grid of equal tiles reusing existing screens (no new features): **Sales Ledger** → Daily Transactions Report, **Buy Ledger** → Buy Report, **Due Ledger** → Due (Baki) ledger, **Expense Ledger** → Expenses.
- [x] **Quick Access trimmed to 3 equal tiles**: Cash Drawer, **Sales** (the original shortcut — opens the Sales Ledger to view completed sales & reprint receipts; owner follow-up replaced the briefly-added "Sale"/Sell-tab tile) and **Products** (opens the Products tab). Purchase ledger stays reachable from the Buy screen; Reports has its own tab — nothing became unreachable.
- [x] **Recent Activity unchanged** (same source, rows and view-only behavior); dashboard credit line unchanged.
- [x] **Due Ledger (Baki) moved from Settings → Reports page** (new "Due Ledger (Baki)" entry after Category Report; row + callback removed from `SettingsScreen`, added to `ReportScreen`; same `DUE_LEDGER` route & screen). `HomeActions` rewired (adds onOpenProducts/onOpenDailyReport/onOpenBuyReport, keeps onOpenSalesLedger; drops the now-unused purchase-ledger/report/detail callbacks). Zero business-logic changes. assembleDebug ✓ (clean).

### Goal

Improve the layout of the **Home Dashboard** by reorganizing existing sections to create a cleaner, more balanced, and more professional appearance.

### Important

- **Do NOT change any existing functionality.**
- **Do NOT add any new features.**
- **Do NOT remove any existing features.**
- **Only rearrange the layout and improve the visual appearance as described below.**

---

## Dashboard Summary

Keep the following dashboard summary cards **exactly as they are**. Do **not** change their functionality, calculations, layout, or position.

- Current Balance
- Today's Sales
- Today's Expenses
- You'll Receive
- You'll Give

Everything in this section should remain unchanged.

---

## Inventory

- Keep the existing Inventory functionality unchanged.
- Adjust the size of all Inventory boxes so they are the **same size**.
- Make the layout look balanced, aligned, and visually consistent.

---

## Ledger Books

Create a **Ledger Books** section containing the following four options:

1. Sales Ledger
2. Buy Ledger
3. Due Ledger
4. Expense Ledger

Use the existing functionality for each option:

- **Sales Ledger** → Open the existing **Daily Transaction Report** from the Reports section.
- **Buy Ledger** → Open the existing **Buy Report** from the Reports section.
- **Due Ledger** → Open the existing **Due (Baki)** feature.
- **Expense Ledger** → Open the existing **Expenses** feature.

### Layout

- Display all four Ledger Books together in a clean and balanced layout.
- Make every option box exactly the **same size**.
- Keep consistent spacing, padding, icons, and alignment.
- The section should look clean, balanced, and professional.

---

## Quick Access

Keep only the following Quick Access options:

1. Cash Drawer
2. Sale
3. Products

Reuse the existing functionality for each option.

---

## Recent Activities

- Keep the **Recent Activities** section exactly as it is.
- Do not change its functionality or appearance.

---

## Move Existing Feature

- Move the **Due Ledger (Baki)** menu from the **Settings** page to the **Reports** page.
- Do **not** change its functionality.
- Only change its location within the application.

---

## General Requirements

- Preserve **100% of the existing functionality**.
- Reuse all existing screens, reports, and logic.
- Do not create duplicate features.
- Do not modify any business logic.
- Only reorganize the layout and menu positions as described above.
- Ensure the Home Dashboard has a clean, modern, balanced, and professional UI while keeping all existing functionality intact.

# Future Updates Phase 35 – Sales Report, Expense Report & Version Management

**Status:** ✅ COMPLETE (2026-07-04)

### Implementation
- [x] **Sales Report** (new `SalesReportScreen`/`SalesReportViewModel`, route `SALES_REPORT`, threaded AppRoot→TabShell→NavGraph→ReportScreen): mirrors the Buy Report — Day / Month / Custom range chips + date pickers, **Total Sales Amount** StatCard (green), and one card per sale showing **date & time, invoice number** (receipt format `INV-S000123`), **customer name** (from the sale's party, when available), **total, payment method (Cash/Due), paid amount and due amount** (due shown in red when > 0). Reads existing `Sale` rows via `SaleRepository.observeBetween` + party names via `PartyRepository.observeParties` — no new tables, no duplicated data. PDF export via the standard `ReportPdfAction` ("sales-report.pdf").
- [x] **Expense Report moved to Reports**: the "Expenses" entry left Settings → Money and now appears on the Reports page as **"Expense Report"** (same `EXPENSES` route, same screen, functionality & calculations untouched; also still reachable from Home → Ledger Books → Expense Ledger). Settings → Money keeps Cash Management + Expense categories.
- [x] **Reports page order**: Daily Transactions · Stock · Business · Cash Management · **Sales Report** · Buy Report · **Expense Report** · Category Report · Due Ledger (Baki). Owner follow-up: the Reports list is now **scrollable** (`verticalScroll`) so all entries stay reachable on any screen size.
- [x] **Version management**: `versionName = "1.35"` / `versionCode = 35` (About MINI POS reads the version from PackageManager, so it now shows "Version 1.35" and will always match). **Standing rule from now on: completing Future Updates Phase N sets the app version to v1.N** (Phase 36 → 1.36, …).
- [x] No business logic or calculations touched. assembleDebug ✓ (clean).

### Goal

Improve the **Reports** section by adding a dedicated **Sales Report**, moving the existing **Expense Report** into the Reports page, and introduce a versioning system that is updated with every completed phase.

---

## Sales Report

- Add a new report called **Sales Report** in the **Reports** section.
- The Sales Report should be similar in structure and functionality to the existing **Buy Report**.
- Each record should include:
  - Date and time
  - Invoice Number
  - Customer Name (if available)
  - Total Sale Amount
  - Payment Method
  - Paid Amount
  - Due Amount (if applicable)
- The report should support:
  - Single date report
  - Monthly report
  - Custom date range report
- Display the **Total Sales Amount** for the selected period.
- Reuse the existing sales transaction data. Do not create duplicate records or a separate database.

---

## Expense Report

- Move the existing **Expense Report** from **Settings → Money** to the **Reports** page.
- Reuse the existing Expense Report functionality.
- Do **not** create a new Expense Report.
- Do **not** modify its functionality or calculations.
- Only change its location so it appears with the other reports.

---

## Application Version Management

- Starting with this phase, update the application version according to the completed phase number.
- For this phase, update the application version to **v1.35**.
- Display the current application version in **Settings → About MINI POS**.
- Going forward, every completed phase should automatically increment the application version using this format:
  - Phase 36 → **v1.36**
  - Phase 37 → **v1.37**
  - Phase 38 → **v1.38**
  - And so on...
- Ensure the version shown in **About MINI POS** always matches the current application version after each completed phase.

---

## General Requirements

- Preserve **100% of the existing functionality**.
- Reuse all existing reports, data, and business logic.
- Do not create duplicate features.
- Do not modify any report calculations.
- Only add the new **Sales Report**, relocate the existing **Expense Report** to the **Reports** page, and implement the application version management described above.

# Future Updates Phase 36 – Automatic Backup Setup Improvement

**Status:** ✅ COMPLETE (2026-07-04)

### Implementation
- [x] **Defaults unchanged**: auto backup stays enabled by default, never runs without a folder, and the chosen folder is remembered (all from Phase 32).
- [x] **Daily setup reminder at 11:00 AM** (new `AutoBackupSetupReminderWorker`, unique work `minipos_auto_backup_setup_reminder`): fires while auto backup is **enabled but no folder is chosen**, repeats every day until a valid folder is selected, and is cancelled automatically by `AutoBackupScheduler.sync()` (runs at app start & on every setting change) once a folder exists or the switch goes off. Notification ID 1006.
- [x] **Tap → Backup & restore page**: notification carries an `EXTRA_OPEN_BACKUP` extra; `MainActivity` (onCreate + onNewIntent, singleTask) forwards it to new `PendingNav` (one-shot StateFlow), consumed in `MainScaffold` → `navigate(Routes.BACKUP)`. Works cold-start (survives splash + license gate) and while the app is open.
- [x] **First-time folder verification**: picking a folder immediately creates a real backup **before the folder is adopted** — extracted the worker's backup execution into shared `AutoBackupRunner.run()` (same manual-backup zip format, same retention pruning, sets lastSuccessAt). On success: folder persisted, "Backup completed successfully…" shown, normal schedule continues. On failure: clear reason shown (**storage unavailable / folder access denied / could not create file / insufficient storage (ENOSPC) / permission lost / other error message**), the new grant is released, the previous folder (or none) stays — so scheduled backups never start against a broken folder. Progress spinner ("Creating a backup to verify the folder…") while verifying.
- [x] `AutoBackupWorker` refactored onto the same runner (behavior, scheduling & retention identical); manual backup/restore untouched. **Version → v1.36** (versionCode 36; About auto-updates). assembleDebug ✓ (clean).

### Goal

Improve the initial setup experience of the Automatic Backup system by validating the selected backup folder immediately and reminding the user to complete the setup if a backup folder has not yet been selected.

---

## Default Behavior

- Keep **Automatic Backup** enabled by default for new installations.
- The Automatic Backup feature should **not** function until the user selects a backup folder.
- The selected backup folder should be remembered and used for all future automatic backups.

---

## Backup Folder Reminder

- If **Automatic Backup** is enabled but **no backup folder has been selected**, send a reminder notification **every day at 11:00 AM**.
- Continue sending the reminder notification every day until the user selects a valid backup folder.
- The notification should remind the user to complete the Automatic Backup setup by selecting a backup folder.
- Tapping the notification should open the **Settings → Backup & Restore** page so the user can select a backup folder.
- Once a valid backup folder has been selected, automatically stop the daily reminder notifications.

---

## First-Time Folder Selection

- When the user selects a backup folder for the **first time**, immediately create a backup using the current application data.
- This first backup should use the same backup format and logic as the existing manual backup system.
- The purpose of this backup is to verify that the selected folder is valid and writable before scheduled automatic backups begin.

---

## Backup Result

### If the backup is successful

- Display a confirmation message indicating that the backup was completed successfully.
- Continue using the selected folder for all future automatic backups.
- Future backups should follow the configured automatic backup schedule.

### If the backup fails

- Display a clear error message indicating that the backup failed.
- Show the reason for the failure whenever possible, such as:
  - Folder access denied
  - Storage unavailable
  - Insufficient storage space
  - Any other applicable error
- Do not start automatic backups until the issue is resolved or a valid backup folder is selected.

---

## General Requirements

- After the initial backup is successfully completed, the Automatic Backup system should continue working normally according to the configured schedule.
- Reuse the existing backup format and backup logic.
- Do not modify the existing manual Backup and Restore functionality.
- Do not change any existing Automatic Backup scheduling or backup retention logic.
- This update only improves the first-time Automatic Backup setup, folder validation, and reminder notification process.

# Future Updates Phase 37 – Sales Report Enhancement

**Status:** ✅ COMPLETE (2026-07-04)

### Implementation
- [x] **Summary now shows Total Sales + Total Profit** side-by-side (two StatCards). Total Sales = period sum of sale totals (same as the ledger); Total Profit reuses the **existing** `SaleDao.observeProfitBetween` SQL already used by the Daily & Business reports — no duplicate calculations.
- [x] **Each record is now a full invoice card** (ledger-style): Date & Time (bold) · Invoice Number (INV-S######) · Payment method (Cash/Due) · **Total prominent** (bold green) — then **every product sold in the invoice** (name, qty × selling price, product total; same row layout as the Sales Ledger / Buy Report cards), then the existing Paid / Due footer. Customer name still shown when available.
- [x] Product lines come from the existing `SaleItem` data (`getItemsForShop().groupBy { saleId }` — the same pattern the Daily Report uses); nothing new is stored.
- [x] **Filters unchanged**: Day / Month / Custom range. PDF export updated to match (Total Sales + Total Profit + per-invoice product lines).
- [x] **Version → v1.37** (versionCode 37; About auto-updates). assembleDebug ✓ (clean).

### Goal

Improve the existing **Sales Report** to display complete invoice details in a clean, card-based layout similar to the **Sales Ledger** available from the Home Dashboard.

### Requirements

#### Sales Summary

Keep the summary section at the top of the report and display:

- Total Sales
- Total Profit

Use the same calculation and business logic currently used in the **Sales Ledger**.

---

#### Sales Report Layout

Update each sales record to display as a single invoice card.

Each invoice card should display:

- Date & Time
- Invoice Number
- Payment Method (Cash / Due)
- Total Invoice Amount (displayed prominently)

Below the invoice information, display **all products sold in that invoice**.

For each product, display:

- Product Name
- Quantity × Selling Price
- Product Total

The layout should be similar to the existing **Sales Ledger** so users can quickly see everything sold in one invoice without opening additional screens.

---

#### Report Filters

Keep the existing filters:

- Single Date
- Monthly
- Custom Date Range

---

#### General Requirements

- Reuse the existing Sales Report.
- Reuse the existing Sales Ledger business logic and calculations.
- Do **not** create duplicate data or duplicate calculations.
- Preserve **100%** of the existing functionality.
- Do **not** modify any business logic.
- Only improve the information displayed and the layout of the Sales Report.
- The report should remain clean, easy to read, and professional.
---

## ✅ TASK CHECKLIST

### P1 — Project & Theme
- [x] P1.1 Create project (`com.minipos`), Kotlin DSL Gradle, min 24 / target 34, Compose + desugaring on
- [x] P1.2 Add deps: Compose BOM, Material3, Navigation-Compose, Room (+ksp), DataStore, Coroutines, kotlinx-serialization, Coil, WorkManager
- [x] P1.3 Package structure per CONVENTIONS §1 (empty packages/placeholders)
- [x] P1.4 Theme: Color.kt (all tokens), Type.kt, Shape.kt, Theme.kt (light scheme, primary = PrimaryBlue)
- [x] P1.5 Reusable composables: AppTopBar, PrimaryButton, SecondaryButton, AppCard, StatCard, AmountText, SectionHeader, EmptyState, AppBottomBar, AppTextField, FilterChipsRow, QtyStepper
- [x] P1.6 App shell: Scaffold + AppBottomBar + NavGraph with placeholder screens (Home/Sell/Buy/Reports/Settings)
- [x] P1.7 Compile confirmed (assembleDebug ✓); nav wired (4 tabs switch top-level destinations) → update PROGRESS

### P2 — Database & multi-shop core
- [x] P2.1 Entities: Shop, ShopSettings, Category, Product, StockMovement, Sale, SaleItem, Purchase, PurchaseItem, Expense, ExpenseCategory, Party, Due, DuePayment (Long paisa, Long millis, shopId indexed)
- [x] P2.2 DAOs (filter by shopId; Flow reads, suspend writes) + Converters
- [x] P2.3 MiniPosDatabase (version 1, exportSchema) + build in MiniPosApp
- [x] P2.4 Repositories per domain
- [x] P2.5 CurrentShopManager (DataStore) exposing current shopId Flow
- [x] P2.6 First-run seeding (ShopRepository.createShop seeds settings + default expense categories; no demo data)
- [x] P2.7 assembleDebug ✓; Room KSP validated all queries; schema v1 exported (14 tables) → update PROGRESS

### P3 — Shop management
- [x] P3.1 Shop list / switcher UI (ShopSwitcherScreen; reached from Home top bar + Settings)
- [x] P3.2 Create/Edit shop (name, logo via photo picker→app storage, address, phone, currency label, low-stock default)
- [x] P3.3 Delete shop (ConfirmDialog; cascades to all shop data + deletes shop image files)
- [x] P3.4 Switching shop sets CurrentShop; MainScaffold keyed on shopId reloads scoped data; first-run onboarding → update PROGRESS

### P4 — Products & categories
- [x] P4.1 Category CRUD (top-level + sub-categories) + unit management (new MeasureUnit entity, DB v1→v2 migration)
- [x] P4.2 Product add/edit (all fields + toggles: low-stock, VAT, warranty, wholesale, discount) + photo (copy into app storage, Coil)
- [x] P4.3 Product list: search + category filter chips + empty state
- [x] P4.4 Update-stock screen (QtyStepper add/remove, note) writes StockMovement + shows movement history → update PROGRESS

### P5 — Sell
- [x] P5.1 Quick Sell (amount) → Sale (cash/due)
- [x] P5.2 Product List cart: add items, qty stepper, per-item discount, running total
- [x] P5.3 Cash vs Due (CheckoutDialog: pick/create CUSTOMER, optional partial paid) → creates RECEIVABLE Due
- [x] P5.4 Commit: SaleRepository.commitSale (Sale+SaleItems, decrement stock, StockMovement(SALE), Due) in one txn → update PROGRESS
- [x] P5.5 Sales Ledger: newest-first, Day/Month/Year/All/Custom (DateButton) + search + totals + tap→SaleDetail

### P6 — Buy
- [x] P6.1 Supplier purchase cart (BuyScreen/BuyViewModel; editable buy price + qty; shared CheckoutDialog → SUPPLIER)
- [x] P6.2 Commit: PurchaseRepository.commitPurchase (Purchase+items, increment stock, StockMovement(PURCHASE), PAYABLE Due) in one txn → update PROGRESS
- [x] P6.3 Purchase Ledger: newest-first, filters + custom range + search + totals + tap→PurchaseDetail

### P7 — Expenses
- [x] P7.1 Expense categories management (ExpenseCategoryViewModel/Screen; defaults Salary/Rent/Bill/Purchase seeded per shop, all editable)
- [x] P7.2 Add/edit expense (amount, category, note, date) + list + Day/Month/Year/All/Custom filter + total → update PROGRESS

### P8 — Due Ledger (Baki)
- [x] P8.1 Party create/edit (PartyFormDialog, Customer/Supplier/Employee) — parties live in the Due ledger
- [x] P8.2 Receive vs Give headline + per-party net balances + party-type tabs (DueLedgerViewModel/Screen)
- [x] P8.3 Record payment (RECEIVED/GIVEN) + add manual due; updates running balance
- [x] P8.4 Per-party statement: merged dues+payments newest-first with running balance (PartyDetail) → update PROGRESS

### P9 — Reports
- [x] P9.1 Stock Report: total units, total stock value, per-product breakdown, movement history (StockReport VM/Screen)
- [x] P9.2 Business Report: cash sale, due collected, other income, cash purchase, due paid, other expense → net + profit (cash/due/total), date filters → update PROGRESS

### P10 — Home dashboard
- [x] P10.1 Tiles wired to real data: balance, period sale, period expense, dues receive/give, product count (HomeViewModel)
- [x] P10.2 Day/Month toggle + big Sell/Buy buttons (switch tabs) + shortcut grid (Products/Sales/Purchases/Expenses/Due/Reports)
- [x] P10.3 Recent Activity: latest sales & purchases merged newest-first, tap → Sale/Purchase detail → update PROGRESS

### P11 — Backup / restore (per shop)
- [x] P11.1 Export current shop → single `.zip` via SAF (CreateDocument): data.json + images/ (photos+logo) + manifest.json
- [x] P11.2 Import/restore from `.zip` (OpenDocument): validate manifest schema v2, insert under new shopId with full FK remap, extract photos, rewrite paths, switch to it, show counts → update PROGRESS

### P12 — Settings, notifications & polish
- [x] P12.1 Settings screen complete: shop switcher, catalog (products/categories/units), money (expenses/expense cats/due ledger), low-stock threshold editor, notification toggles, backup/restore, About (SettingsViewModel)
- [x] P12.2 Notifications: ReminderWorker (daily, WorkManager) for low-stock + due reminders, gated by ShopSettings toggles; channel + POST_NOTIFICATIONS request
- [x] P12.3 Polish: rebranded launcher icon (blue + white bag), app name MINI POS, empty/loading/error states present; clean assembleDebug ✓ → DONE

---

## 📝 SESSION LOG (newest at top, one line each)
- 2026-07-04 — Future Updates Phase 37 done (Sales Report Enhancement): summary now Total Sales + Total Profit (profit via existing SaleDao.observeProfitBetween — same SQL as Daily/Business reports, no duplicate math); each record is a full invoice card (date/time, INV-S###### invoice no, Cash/Due, prominent total, customer when available) listing every product sold (TxnLine rows via getItemsForShop().groupBy{saleId}, ledger-style qty × price + line total) + Paid/Due footer; Day/Month/Custom filters unchanged; PDF export updated to match; v1.37/versionCode 37. assembleDebug ✓.
- 2026-07-04 — Future Updates Phase 36 done (Automatic Backup Setup Improvement): AutoBackupRunner extracted (shared backup exec: manual-format zip, prune 15, lastSuccessAt, reason-mapped failures incl. ENOSPC/SecurityException); folder pick now verified by an immediate real backup before adoption (success msg / clear error + previous folder kept, new grant released; spinner in BackupScreen); AutoBackupSetupReminderWorker — daily 11:00 AM "finish setup" notification (ID 1006) while enabled-but-folderless, auto-cancelled via sync(); tap opens Backup page via EXTRA_OPEN_BACKUP → MainActivity(onNewIntent) → PendingNav → MainScaffold navigate(BACKUP); worker refactor behavior-identical; v1.36/versionCode 36. assembleDebug ✓.
- 2026-07-04 — Future Updates Phase 35 done (Sales Report, Expense Report & Version Management): new SalesReportScreen/VM (mirrors Buy Report: Day/Month/Custom, Total Sales Amount, per-sale date/time + INV-S invoice no + customer name + total + Cash/Due + paid + due, ReportPdfAction PDF; reads existing Sale rows + party names, no new tables); route SALES_REPORT threaded through AppRoot/TabShell/NavGraph/ReportScreen; "Expenses" entry moved Settings→Reports as "Expense Report" (same EXPENSES route/screen); versionName "1.35" + versionCode 35 — About auto-shows it; standing rule: Phase N ⇒ v1.N. assembleDebug ✓.
- 2026-07-04 — Future Updates Phase 34 done (Home Dashboard Layout Reorganization): summary cards & Recent Activity untouched; Inventory tiles equalized (one CountTile style, IntrinsicSize.Min row, Stock Value same Money.format value); new Ledger Books 2×2 grid reusing existing screens (Sales Ledger→Daily Report, Buy Ledger→Buy Report, Due Ledger→Due (Baki), Expense Ledger→Expenses); Quick Access trimmed to Cash Drawer/Sales (Sales Ledger, kept per owner follow-up — views completed sales & reprints receipts)/Products (Products tab); Due Ledger (Baki) entry moved SettingsScreen→ReportScreen (same route/screen); HomeActions rewired (3 new callbacks, unused ones dropped). No logic changes. assembleDebug ✓.
- 2026-07-04 — Future Updates Phase 33 done (Settings Page Reorganization): SettingsScreen-only layout change — 9 fragmented sections → 7 logical groups (Shop / Inventory / Money / Printing / Notifications / Data & history / App), one grouped AppCard per section with inset dividers instead of 17 one-row cards; Low-stock threshold moved beside inventory items, Printing split out of Catalog; duplicate icons replaced (Label/Tune/Alarm/History); all 17 items, labels, dialogs, callbacks & behavior identical. assembleDebug ✓.
- 2026-07-04 — Future Updates Phase 32 done (Automatic Backup System): AutoBackupPrefs (DataStore: enabled default ON, SAF folder Uri, frequency default 1 day, time default 23:00, lastSuccessAt) + AutoBackupWorker/Scheduler (self-rescheduling WorkManager job; reuses BackupManager.export → MiniPOSAuto_*.zip via DocumentFile; keeps newest 15 auto backups, manual untouched; failure & 3-day-stale notifications IDs 1004/1005; failed runs retry next day); missed-run catch-up via sync() in MiniPosApp.onCreate; backup reminder suppressed while auto backup active (MiniPosApp/BackupReminderWorker/SettingsViewModel gates); BackupScreen Auto Backup section (switch, folder picker w/ persistable permission, frequency & time dialogs, last-backup line); new androidx.documentfile:1.0.1 dep. assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 31 done (Default Unit): ShopSettings.defaultUnit (default "pcs", DB v6→v7 MIGRATION_6_7, in backups); new products pre-select it once via ProductFormScreen LaunchedEffect (only when unit exists & none chosen; edits untouched); Settings→Units shows a yellow "Default" badge + "Set default" per unit (UnitViewModel.setDefault; rename follows the default). Units system otherwise unchanged. assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 30 done (Advanced Printing Settings): PrintPrefs expanded (ReceiptField 19 toggles incl. new email/website/subtotal/thank-you, ReportField 6 toggles, receipt alignment Center/Left, report font/margins, footers/notes; disabled-sets → future fields default on); ReceiptPdf rewritten (per-toggle rendering + auto-scaled shop logo, no gaps when off); ReportPdf gained ReportDecor (logo/shop info/title/generated-at/footer/notes + font/margins) applied centrally via ReportPdfAction(shopId) → all 7 reports decorated automatically; Printer Settings screen expanded → "Printing Settings" hub (all toggles + texts + test print); base A4/Letter + any printer paper via system dialog. Tax omitted (no tax on sales). assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 29 done (Receipts & PDF Reports): new core/print — PrintPrefs (DataStore: receipt width/margin/font/footer + PDF paper A4/Letter), ReceiptPdf (thermal-style narrow PDF, all txn details, INV-S/B numbers), ReportPdf (generic line-model renderer w/ page breaks), PdfShare (save/print/preview via new FileProvider) + ReceiptPrinter; Sell/Buy checkout callbacks now return ids → "Print receipt?" Yes/No dialog after every sale/purchase; reprint icons on Sales & Purchase ledger rows; ReportPdfAction (Preview/Save/Print) added to Business, Stock, Buy, Category, Cash Mgmt, Cash Drawer & Product History reports; Settings→Printer Settings screen (route PRINTER_SETTINGS) with test-receipt print. Offline via Android print framework. assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 28.1 done (Printing search/filter + scanner feedback): Barcode Printing gained SearchUtil search (name/barcode) + the Category/Subcategory multi-filter — dialog extracted to shared ProductFilterDialog + ProductFilters object (Products page refactored, same behaviour); select-all = visible list (merge), PDF resolves from unfiltered list; scan-to-select added (+1 label per scan). Shared BarcodeScannerDialog now vibrates (VIBRATE perm) + beeps (ToneGenerator) + shows a green "✓ Scanned" flash, with sliding-window dedupe (no re-fire while code stays in view) — applies to all scan screens. assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 28 done (Barcodes): products.barcode column (DB v5→v6, per-shop uniqueness); auto-gen on save + duplicate validation (ProductViewModel.save returns error); form field w/ scan-to-fill; backfill at app start + after restore (backup schema v4, old backups fine); ZXing scanner dialog (offline, continuous+dedupe, runtime CAMERA permission) wired into Products (find/open), Sell (scan-to-cart, stock-aware) and Buy (scan-to-cart); Settings→Barcode Printing (select products+label counts, field toggles, mm sizes/per-row/per-page on A4, Save-PDF via SAF + Print via print framework) with LabelPdf CODE-128 renderer + PdfPrintAdapter; new deps zxing-android-embedded 4.3.0 + zxing core 3.5.3. assembleDebug ✓.
- 2026-07-01 — Cash Drawer follow-up (owner request): removed the Opening Cash Set/Edit option entirely — DrawerCard is read-only (always shows "Carried forward…"), deleted OpeningCashDialog + VM canEditOpening/setOpening; repo/DAO/table kept (existing seeds still honored; fresh installs open at ৳0). No other change. assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 27.1 done (Cash Drawer Enhancement): Opening Cash now auto-carries — Opening(day) = first-day manual seed + net cash flow since (≡ previous day's closing; history chains per-day openings seeded from before the range); Set/Edit only while no earlier manual opening exists (else "Carried forward…" note). Day view gains a Transactions list (Cash Sale/Due Collection with customer name/Cash In/Out/Expense — date-time, description, signed amount, running balance after; oldest first; purchases & supplier payments excluded) via new repo observeDayTransactions + VM DrawerTxnRow. No schema change (DB v5). assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 27 done (Cash Drawer): new feature/cashdrawer Screen+VM (route CASH_DRAWER) replacing the Home Products shortcut (PointOfSale icon; Products still in bottom nav). Day view: editable Opening Cash + live Cash Sales (Σ paidAmount), Due Collections (RECEIVED), Cash In/Out, Expenses, computed Closing (open+sales+collect+in−out−exp); buying/supplier payments excluded; independent of Current Balance. History via Month/Custom (per-day cards, newest first). New cash_drawer_openings table (unique shop+day, upsert) — DB v4→v5 (MIGRATION_4_5), CashDrawerDao/Repository; all other numbers derived live (no duplicate records). assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 26 done (Notification Tap): root cause = no contentIntent on notifications; Notifier now attaches a launcher-style PendingIntent (getLaunchIntentForPackage fallback MainActivity ACTION_MAIN/LAUNCHER, NEW_TASK, FLAG_IMMUTABLE|UPDATE_CURRENT) + MainActivity launchMode="singleTask" — closed→launch, background→foreground existing task, open→reused (no duplicates). Design/content/scheduling unchanged. assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 25 done (Home Redesign): TabShell Scaffold excludes the statusBars inset so tab chrome extends behind the status bar; HomeScreen rebuilt — yellow hero header (statusBarsPadding) with "MINI POS | Shop Name", switch-shop icon, Current Balance hero + dark-on-yellow Day/Month PeriodChips; content regrouped (money row, dues row, Inventory section 3-up, Sell/Buy, Quick Access shortcuts, Recent Activity via SectionHeader, credit). UI-only — same VMs/stats/actions/colors; other tabs' yellow AppTopBars now blend into the status bar too. assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 24 done (Product Filter): Filter button beside the Products search box (yellow when active) opens a checkbox dialog (categories + indented subcategories, Done/Clear all, immediate apply, persists); ProductViewModel now multi-select (selectedCategoryIds/selectedSubCategoryIds, union match cat∈cats OR sub∈subs) replacing the single-category filter; header Total Units/Stock Value now follow the filtered set (no filters = all, same Σ stock / Σ stock×buyPrice); category chips kept as multi-select toggles ("All" = clear). assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 23.1 done (Category Report Stock Summary): added "Current Stock Summary" section (Current Stock Quantity card + Current Stock Value StatCard) to CategoryReportScreen, computed over the report's matching-products list (All Subcategories = whole category; specific sub = that sub only); reuses the Products-page calc (Σ stock, Σ stock×buyPrice), range-independent; CategoryReportData gained stockQty/stockValue. Existing sections untouched. assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 23 done (Category Report): new Reports → Category Report (CategoryReportScreen/VM, route CATEGORY_REPORT) — Category dropdown + Subcategory dropdown with "All Subcategories" default, Day/Month/Custom filters; per-product qty purchased/purchase amount/qty sold/sales amount/profit (sales − buyPrice×qtySold, app convention) + end summary (total purchase qty/amount, sales qty/amount, profit/loss). Reuses sale/purchase items + product/category flows; no schema change, no existing report touched. assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 22 done (Search Improvement): new common core/util/SearchUtil (case- & space-insensitive, per-word partial matching incl. joined words "Note14"⇄"Note 14", relevance-ranked by match position, stable ties); replaced naive contains() in all 5 search boxes — Products VM, Sell, Buy, Sales ledger (note/party/amount), Purchase ledger (note/party/amount). Spec example verified (all 8 queries match both products; 5G only the applicable one). No UI changes; screens without search untouched. assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 21 done (Cash Management Transaction Deletion): removed the Delete button from Cash In/Out rows in CashManagementScreen (+ unused Delete/IconButton imports); cash records still viewable there; deletion/undo now only via Settings→Activities (existing undoCash). CashViewModel.delete left but unwired. assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 20 done (Due Payment Validation): PartyDetailViewModel.recordPayment now caps a payment at the party's outstanding due (Received ≤ net.coerceAtLeast(0); Given ≤ (−net).coerceAtLeast(0)) with error "Amount exceeds the outstanding due (…)"; AmountChoiceDialog gained an onError path so the Record-payment dialog stays open on failure (Add-due unchanged). Only payment entry point, so covered everywhere; customer/supplier due balances can't go negative. assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 18 done (Home Recent Activities): Home "Recent Activity" now reuses the same ActivityViewModel/logic as Settings→Activities (own ActivityViewModel in HomeScreen, activities.take(10)); extracted shared public ActivityRowCard(item, onUndo?) used by both — Home passes no undo (view-only). Removed old HomeViewModel.recent + home ActivityItem (no dup). Same 10 types, layout unchanged. assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 19 done (Buy Report): new Reports → Buy Report (BuyReportScreen/VM, route BUY_REPORT) listing all Cash+Due purchases with date-time, payment type, total + line items (product/qty/buy price/line total); Day/Month/Custom filters + Total Buy Amount. Reuses purchase data (observeBetween/getItemsForShop) + TxnEntry/TxnLine; Buy logic untouched. Confirmed Phase 17 Sell/Buy/cash rule already correct (Sell untouched; cash/due buys + supplier due payments don't affect balance). Phase 18 deferred per owner. assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 17 done (Cash Management System): Current Balance = petty cash — new BalanceRepository (sales cash + received + cashIn − expenses − cashOut; buying/supplier-payments excluded), HomeViewModel balance matched. Expense add + Cash Out now blocked when > balance (dialog error, never negative). New Cash Management Report (CashReportScreen/VM, Day/Month/Custom, Total Cash In/Out) in Reports hub (route CASH_REPORT); added DAO sum/between queries. Buy screen & Business/Daily reports untouched (cash buys still reported). assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 16 done (Dashboard Stock Summary): added a "Stock Value" StatCard to the Home dashboard row (now 3-up: Types of Products · Total Units · Stock Value); HomeViewModel.stockValue reuses ProductRepository.observeStockValue (Σ stock×buyPrice) — same calc as the Products page, no new method. assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 15 done (Activities & Undo): new Settings→Activities (ActivityScreen/VM, route ACTIVITIES) aggregating last 30 days of sell/buy/expense/cash-in/cash-out/stock-adjustment/undo, newest first, each with date/type/details/amount. Per-row Undo (confirm) reverses+removes the original in a db.withTransaction (re-add/subtract stock, drop movements+due+payments, delete row) and logs an ActivityUndo audit entry — reports/balance/ledgers re-aggregate, no existing txn code touched. New ActivityUndo entity+DAO+ActivityRepository; DB v3→v4 (MIGRATION_3_4); undo/since queries added to Sale/Purchase/Expense/Cash/StockMovement/Party DAOs. 30-day filter-only; undone originals leave the list; not backed up (local audit). assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 14.1 done (Splash refinement): splash 2s→1.5s (SPLASH_MILLIS=1500); removed the big Android-12+ system splash icon via values-v31/themes.xml (windowSplashScreenAnimatedIcon=@drawable/splash_blank transparent + windowSplashScreenBackground=brand_yellow) so only the in-app small logo + wave text shows; SplashScreen.kt untouched. assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 14 done (Splash Screen): new feature/splash/SplashScreen (BrandYellow bg, centred ic_launcher_foreground logo, "MINI POS" wave-animated wordmark via infinite transition); 2s then Crossfade → LicenseGate{AppRoot()}. Added Theme.MINIPOS.Splash (yellow windowBackground) + setTheme(Theme_MINIPOS) in onCreate to kill the white cold-start flash. No app logic/flow changed. assembleDebug ✓.
- 2026-07-01 — Future Updates Phase 13 done (Product History): product card "Update"→"History" opens new read-only ProductHistoryScreen (reuses stock_movements: Buy/Sell/Stock Adjustment, newest first, last 30 days; rows show date/time, type, signed qty, computed balance-after) via StockMovementDao.observeByProductSince + ProductRepository.MOVEMENT_RETENTION_MILLIS (30d). **Filter-only per owner** — no physical deletion, all movements kept, Stock Report unaffected. No schema change (DB v3). assembleDebug ✓.
- 2026-06-30 — Future Updates Phase 12 done (Remove Update Option): removed the clickable "Update" text from product cards in ProductListScreen.ProductRow (+ dropped its onUpdateStock wiring and unused OnYellow import); stock adjustment still available in Product Details & Edit Product; UpdateStockScreen + nav left intact. assembleDebug ✓.
- 2026-06-30 — Future Updates Phase 11 done (Home Dashboard Summary): renamed Home "Products in stock" → "Types of Products" (still productCount), added a "Total Units" card beside it in a 2-up row (HomeViewModel.totalUnits = Σ stock via observeByShop, same calc as the Products page header); generalised CountTile(label, value, modifier); restructured stats combine to 5 sources. No other dashboard stats changed. assembleDebug ✓.
- 2026-06-30 — Future Updates Phase 10 done (Default Quantity 0): shared StockAdjustmentSection (Product Details + Edit Product) now defaults qty 0 (stepper min 0) and blocks the Add/Remove action at 0 with "Enter a quantity of 1 or more."; Buy cart items added at qty 0 (confirmPurchase skips 0-qty lines; Pay still gated on total>0 so it can't run at 0). Sell left unchanged (not in scope). Reused QtyStepper + existing validation. assembleDebug ✓.
- 2026-06-30 — Future Updates Phase 9 done (Android license generator): new standalone Android Studio project `mini_pos_license_generator_android/` (id com.jahirulislam.miniposlicensegenerator, Kotlin/Compose/MVVM/M3, minSdk 24, no permissions, offline). One-page UI (Device ID + Days + Generate + read-only key + Copy + Clear); `LicenseGenerator` signs via java.security with the embedded PKCS#8 private key — same algorithm/keypair as the Python tools, output verified compatible with the app's public key. assembleDebug ✓ (~12 MB) + assembleRelease ✓ (~7.5 MB). Independent of the MINI POS app; folder git-ignored (embeds private key).
- 2026-06-29 — Future Updates Phase 8 done (Stock Adjustment in Product Details): extracted shared `StockAdjustmentSection` composable (add/remove, qty, reason, no-negative guard); ProductFormScreen now uses it (old inline block removed — no dup); added it to the bottom of ProductDetailScreen; both call the same `ProductRepository.adjustStock` (new `ProductDetailViewModel.applyStockChange`); detail stock updates instantly via the Room Flow. Edit-screen behaviour unchanged. assembleDebug ✓.
- 2026-06-29 — Future Updates Phase 7 done (GUI license generator): new independent `license_generator_gui/` — single self-contained Tkinter `license_gui.py` (Device ID + Duration-in-days fields, Generate, read-only key box, Copy button) embedding the same private key; same RSA/SHA256 algorithm as the CLI (CLI left untouched). Runs as `.py` and builds to a standalone `.exe` (build_exe.bat / pyinstaller; requirements.txt). GUI keys cross-verified via OpenSSL + CLI verifier; wrong-device rejected. Folder git-ignored (embeds private key); no Android changes.
- 2026-06-29 — Future Updates Phase 2 done (Offline License Activation): new isolated `com.minipos.feature.license` (LicenseKeys/Verifier/Manager/ViewModel/Gate/ActivationScreen/ManagementScreen); LicenseGate wraps AppRoot in MainActivity → app locked until a valid, device-locked, non-expired RSA-signed key is entered; Device ID (MPOS-XXXX…) generated once + DataStore-persisted + copyable; offline verify via java.security (SHA256withRSA, embedded public key); Settings→License Management (status/expiry/days/Renew/Replace, never touches shop data); contact info on both screens. Owner-only `license_generator/` (Python: main.py/generator.py/requirements/README) git-ignored (+ *.pem). Keypair generated; embedded public key verified to match; OpenSSL cross-verified a generated signature. assembleDebug ✓ (clean).
- 2026-06-29 — Future Updates Phase 6 done (Stock Validation for Selling): SellViewModel now caps cart qty at available stock (maxSellable=floor(stock)) in addToCart + setQuantity — covers both the main Sell screen and Product Details→Sell (same cart); Review-popup QtyStepper "+" disabled at the limit (new optional QtyStepper.max, default null so Update-Stock is unbounded); one-shot SellViewModel.messages→snackbar ("Only N in stock"/"… is out of stock"). Quick Sell exempt; Buy & all other flows untouched. assembleDebug ✓.
- 2026-06-29 — Future Updates Phase 5 done (Sell/Cart redesign): product list is now primary (removed bulky inline cart; in-cart rows show a yellow "In cart: N" pill); ChargeBar→compact ContinueBar; new CartSummaryDialog ("Review sale") — scrollable items+payment with a sticky "Confirm Sell ৳total" button that's the only commit action; payment (Cash/Due, customer pick/create, partial paid, note) moved into the popup. Quick Sell still uses shared CheckoutDialog; Buy's CheckoutDialog untouched. assembleDebug ✓.
- 2026-06-29 — Future Updates Phase 4 done (Product Summary): added a two-card header (Total units · Stock value) at the top of ProductListScreen; new ProductViewModel.totalUnits (Σ stock) + totalStockValue (reuses ProductRepository.observeStockValue) — shop-wide (not filtered by search/category), live via Room Flow→StateFlow, matches the Stock Report totals/design. assembleDebug ✓.
- 2026-06-29 — Future Updates Phase 3 done (Product Management): wired Delete product in ProductFormScreen edit mode (red button + ConfirmDialog) — blocked unless stock==0 with "This product still has stock. Remove all stock before deleting."; SellScreen "Tap to add" now hides stock≤0 (only the selling screen hides — Stock Report deliberately left showing all, resolving the spec contradiction); stock-adjustment in both ProductFormScreen & UpdateStockScreen refuses removing more than current stock (no negative); AppRoot pops form+stale detail after delete. Phase 2 (license) deferred per user. assembleDebug ✓.
- 2026-06-29 — Future Updates Phase 1 done: first-run setup chooser (FirstRunSetupScreen) — Create a New Shop (→ existing ShopFormScreen) or Restore from Backup (→ SAF .zip → BackupManager.import, no existing shop needed; restored shop becomes active & app opens). AppRoot's ShopGate.None now shows the chooser. Offline. assembleDebug ✓.
- 2026-06-29 — Roadmap: added "Future Updates (Roadmap)" to PROGRESS.md — 5 owner-requested phases (First-time setup chooser, Offline license activation + Python generator, Product management improvements, Product summary on Products page, Sell cart redesign), all as checkboxes; pre-ticked only the 2 items the app already satisfies (restore→active shop, offline). No code changed.
- 2026-06-29 — Docs: added CLAUDE.md (project overview, tech stack, source-of-truth pointers, "read BUILD_PLAN.md & PROGRESS.md first" rule); consolidated PROGRESS.md CURRENT POSITION (status + decisions for post-release Phases 1–3, replaced stale P11/P12 planning notes).
- 2026-06-29 — Post-release Phase 3: Cash Management (CashTransaction entity + DB v3 migration, repo/VM/screen; affects Current Balance only, in backup); Daily Transactions Report (sales+purchases, single day or ≤1-month range, totals+profit+itemised lines); Backup Reminder (daily local notification at customizable time, default 10 PM, enable/disable via DataStore + self-rescheduling WorkManager). assembleDebug ✓ (schema v3, 16 tables).
- 2026-06-28 — Post-release Phase 2: Sell defaults to Products mode; new read-only Product Details page (Sell/Buy/Edit quick actions, edit-protected) + manual stock adjustment in edit mode (ADJUSTMENT movement, no sale); bottom nav Buy→Products (Buy still via Home button & detail). Nav: Products is an inner tab; product detail/form/update-stock are root pushes; Sell/Buy accept optional initialProductId. assembleDebug ✓.
- 2026-06-28 — Post-release Phase 1 (Bug Fix): fixed all crashes (LazyColumn duplicate keys in Sell/Buy/Stock Report → namespaced), switched app to yellow-primary theme (chrome/icons BrandYellow, blue amount texts→OnSurface, money green/red kept), added Home credit "Mini POS by Jahirul Islam" (links to jahirulislam.info), updated About (owner + website); assembleDebug ✓.
- 2026-06-28 — P12 done (PROJECT COMPLETE): SettingsViewModel + full Settings (low-stock editor, notif toggles, About), WorkManager ReminderWorker (low-stock + due daily reminders) + channel + POST_NOTIFICATIONS, rebranded launcher icon; clean assembleDebug ✓ (app-debug.apk ~22.6 MB). All 12 phases done.
- 2026-06-28 — P11 done: all entities/enums @Serializable; BackupManager (export ZIP: manifest.json+data.json+images/ via SAF CreateDocument; import: validate v2, restore into new shop with full FK remap + image path rewrite, switch to it) in ServiceLocator; suspend getAllForShop DAO queries; BackupViewModel/Screen; Settings "Backup & restore" entry; assembleDebug ✓. Next: P12 Settings/notifications/polish.
- 2026-06-28 — P10 done: HomeViewModel (balance, period sale/expense via Day/Month toggle, dues receive/give, product count, recent activity merge), HomeScreen real dashboard (tiles, Sell/Buy buttons→tabs, shortcut grid, recent activity→detail), HomeActions bundle threaded via TabShell/TabNavGraph (+ onOpenSaleDetail/onOpenPurchaseDetail); assembleDebug ✓. Next: P11 Backup/restore.
- 2026-06-28 — P9 done: SaleDao profit aggregates (lineTotal−COGS, total + by-payment) + PartyDao payments-between; StockReport VM/Screen (units, value, per-product, movements), BusinessReport VM/Screen (money in/out, net, profit cash/due/total, date-filtered), Reports hub; Reports tab wired with shopId; assembleDebug ✓. Next: P10 Home dashboard.
- 2026-06-28 — P8 done: PartyDao/Repo shop-wide dues+payments + recordPayment/addManualDue; DueLedger VM/Screen (receive/give headline, party tabs, per-party net), PartyFormDialog, PartyDetail VM/Screen (statement w/ running balance, record payment + add due dialogs); Settings "Due ledger (Baki)" entry; assembleDebug ✓. Next: P9 Reports.
- 2026-06-28 — P7 done: ExpenseCategory management (VM/Screen), ExpenseViewModel + ExpenseScreen (filter + total + add/edit dialog w/ category dropdown + date), ExpenseRepository.add gained date param, Settings "Money" entries (Expenses, Expense categories); assembleDebug ✓. Next: P8 Due Ledger.
- 2026-06-28 — P6 done: PurchaseRepository.commitPurchase (txn: Purchase+items, stock increment+StockMovement, PAYABLE Due), BuyViewModel+BuyScreen (supplier cart, editable buy price), generalized CheckoutDialog→core/ui (reused by Sell+Buy), PurchaseLedger VM/Screen + PurchaseDetail; assembleDebug ✓. Next: P7 Expenses.
- 2026-06-28 — P5 done: SaleRepository.commitSale (txn: Sale+items, stock decrement+StockMovement, RECEIVABLE Due), SellViewModel+SellScreen (Quick Sell + product cart, qty/discount), CheckoutDialog (Cash/Due, pick/create customer, partial pay), SalesLedger VM/Screen (filters+search+totals) + SaleDetail; DateButton + DateUtil.rangeFor added; assembleDebug ✓. Next: P6 Buy.
- 2026-06-28 — P4 done: MeasureUnit entity + DB v2 migration, Category/Unit management (CategoryViewModel/Screen, UnitViewModel/Screen), ProductViewModel + ProductListScreen (search/category filter), ProductFormScreen (all fields+toggles+photo), UpdateStockScreen (QtyStepper→StockMovement+history); AppDropdown/NameInputDialog/DateUtil added; Settings catalog entries; assembleDebug ✓ (schema v2, 15 tables). Next: P5.1 Quick Sell.
- 2026-06-28 — P3 done: root AppRoot gate (first-run onboarding) + MainScaffold keyed on shopId, ShopViewModel, ShopSwitcherScreen, ShopFormScreen (logo picker→ImageStorage), ConfirmDialog, Home shows active shop + switch action, Settings manage-shops entry; assembleDebug ✓. Next: P4.1 categories.
- 2026-06-28 — P2 done: 14 Room entities + enums, Converters, 8 DAOs (shopId-scoped, Flow/suspend), MiniPosDatabase v1, 7 repos + ServiceLocator/MiniPosApp wiring, CurrentShopManager (DataStore), createShop seeding; assembleDebug ✓, schema v1 exported (14 tables). Next: P3.1 shop list/switcher.
- 2026-06-28 — P1 done: deps+KSP+desugaring wired for AGP9 built-in Kotlin, brand theme, all 12 reusable composables, Money util, nav shell with 5 placeholder screens; assembleDebug ✓. Next: P2.1 entities.
