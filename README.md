<h1 align="center">🏪 MINI POS</h1>

<p align="center">
  <b>A 100% offline, multi-shop Point-of-Sale &amp; bookkeeping app for small shopkeepers.</b><br/>
  Android · Kotlin · Jetpack Compose · Room
</p>

<p align="center">
  <img alt="Version" src="https://img.shields.io/badge/Version-v1.37-blue">
  <img alt="Platform" src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white">
  <img alt="Language" src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white">
  <img alt="UI" src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="Database" src="https://img.shields.io/badge/DB-Room%20(SQLite)%20v7-003B57?logo=sqlite&logoColor=white">
  <img alt="minSdk" src="https://img.shields.io/badge/minSdk-24-orange">
  <img alt="targetSdk" src="https://img.shields.io/badge/targetSdk-34-orange">
  <img alt="Offline" src="https://img.shields.io/badge/100%25-Offline-success">
</p>

MINI POS lets a shopkeeper run **sell / buy / expense / due (baki)** bookkeeping and **inventory** for one or
more shops, entirely on-device — with **barcode scanning**, **receipt printing**, **PDF reports**, a **cash
drawer**, **automatic scheduled backups**, and a central **activity log with undo**. No accounts, no internet,
no cloud, no ads. Currency is **BDT (৳)**.

**Current app version: v1.37** — the version tracks the latest completed feature phase (v1.\<phase\>) and is
shown in **Settings → About MINI POS**.

Made by **[Jahirul Islam](https://jahirulislam.info/)**.

> ℹ️ **Note:** screenshots can be added under a `docs/` or `screenshots/` folder and embedded here once available.

---

## Table of contents

- [Overview](#overview)
- [Feature highlights](#feature-highlights)
- [Features in detail](#features-in-detail)
- [Tech stack](#tech-stack)
- [Architecture](#architecture)
- [Project structure](#project-structure)
- [Requirements](#requirements)
- [Getting started](#getting-started)
- [Open, build & run in Android Studio](#open-build--run-in-android-studio)
- [Build from the command line](#build-from-the-command-line)
- [Data, storage & migrations](#data-storage--migrations)
- [Backup & restore](#backup--restore)
- [Automatic backups](#automatic-backups-offline-scheduled)
- [Printing](#printing)
- [Notifications](#notifications)
- [Permissions](#permissions)
- [Build configuration & versions](#build-configuration--versions)
- [Conventions & source-of-truth docs](#conventions--source-of-truth-docs)
- [Troubleshooting](#troubleshooting)
- [License](#license)

---

## Overview

MINI POS is a single-user, on-device app modeled on the workflow of small-shop bookkeeping apps. Everything is
stored locally in an SQLite (Room) database; the only "network" use is downloading dependencies the first time
you build the project. Each shop's data is fully isolated, and the app supports multiple shops with a switcher.

Key design rules:

- **Money is stored as `Long` paisa** (1 ৳ = 100 paisa) — never floating point. Displayed as Taka (`৳`).
- **Dates are stored as `Long` epoch milliseconds.**
- **Every shop-owned row carries a `shopId`**, and every query is scoped to the active shop.
- **Offline-first:** no network calls, sign-in, analytics, or ads at runtime.

---

## Feature highlights

| | |
|---|---|
| 🛒 **Sell & Buy** | Product carts, quick sell, cash/due checkout, partial payments, stock validation |
| 📦 **Inventory** | Products with photos, categories/subcategories, units, barcodes, stock history |
| 📷 **Barcode scanner** | Camera scanning (offline, ZXing) in Sell, Buy, Products & product form |
| 🏷️ **Barcode labels** | CODE-128 label sheets — pick products & counts, customize, PDF/print |
| 🧾 **Receipts** | Thermal-style receipts after every sale/purchase + reprint from the ledgers |
| 📄 **PDF reports** | Every report exports to PDF — preview, save, or print |
| 🖨️ **Printing Settings** | One hub: per-item receipt/report content, shop logo, paper, fonts, alignment |
| 💰 **Cash Drawer** | Daily opening→closing cash, auto-carried, per-day transaction list |
| 📊 **Reports** | Sales, Buy, Expense, Business, Stock, Category, Cash Management, Daily, Due Ledger, Product History |
| ↩️ **Activities & Undo** | Central 30-day activity log; safely undo any transaction |
| 🔐 **License activation** | Offline, device-locked RSA license system |
| 💾 **Backup & restore** | Per-shop `.zip` export/import, backward compatible |
| ⏰ **Automatic backups** | Scheduled backups to a folder you pick — verified on setup, keeps the latest 15 |

---

## Features in detail

### Shops (multi-shop)
- Create / edit / delete shops (name, logo, address, phone, currency label, default low-stock threshold).
- Shop switcher; switching reloads all data scoped to the selected shop.
- **First-run setup**: create a new shop **or restore from a backup** — fully offline.

### Home dashboard (redesigned)
- Yellow hero header that blends into the status bar, titled **MINI POS | Shop Name**, with a prominent
  **Current Balance** (the shop's petty cash) and a Day/Month toggle.
- Summary cards: today's/this month's sales & expenses, you'll receive / you'll give, plus an equal-tile
  **Inventory** row (**Types of Products**, **Total Units**, **Stock Value**).
- Big **Sell**/**Buy** buttons.
- **Ledger Books** — a balanced 2×2 grid: **Sales Ledger** (Daily Transactions Report), **Buy Ledger**
  (Buy Report), **Due Ledger** (Baki) and **Expense Ledger** (Expenses).
- **Quick Access** — Cash Drawer, Sales (completed sales & receipt reprint) and Products.
- **Recent Activity** — the 10 latest activities, same unified feed as Settings → Activities (view-only).

### Sell
- **Products-first** flow with smart search, barcode **scan-to-sell** (continuous), and out-of-stock items hidden.
- Compact cart with a **Review sale** popup (line items, quantities, discounts, grand total) and a sticky
  **Confirm Sell** button; **Quick Sell** for amount-only sales.
- Cash or **Due** checkout — pick or create a customer; partial payments supported.
- **Stock validation** — you can never sell more than the available stock.
- **"Print receipt?"** prompt after every completed sale.

### Buy
- Supplier purchase cart (editable buy price + quantity, items start at qty 0), Cash or Due, scan-to-buy.
- Increments stock, logs movements, records payable dues on credit; **"Print receipt?"** prompt after checkout.
- **Buying never affects the cash balance** — the Current Balance tracks petty cash only.

### Products & inventory
- Full product CRUD: name, prices, stock, category/subcategory, unit (pre-selects the shop's **default unit**),
  photo, **barcode** (auto-generated when blank; duplicates blocked; scan-to-fill), plus optional toggles
  (low-stock alert, VAT %, warranty, wholesale price, discount %).
- Product list with **smart search** (case/space-insensitive, partial & joined-word matching), **multi-select
  Category/Subcategory filter**, scan-to-find, and a live inventory summary (total units + stock value that
  follow the active filters).
- **Product Details** (read-only) with Sell / Buy / Edit quick actions and an inline **stock adjustment**.
- **Product History** — per-product movement log (buys, sells, adjustments) for the last 30 days with running
  balances; exportable to PDF.
- Delete protection: products can only be deleted at zero stock; stock can never go negative.

### Barcodes
- Every product gets a **unique barcode** (per shop) — auto-generated, manually entered, or scanned from the
  manufacturer's packaging; restored/legacy products are backfilled automatically.
- **Camera scanning** (ZXing, fully offline) with vibration, beep and an on-screen "✓ Scanned" flash;
  continuous multi-scan with smart repeat-prevention.
- **Barcode Printing** (Settings): select any products with per-product label counts (search/filter/scan to
  select), toggle label fields (barcode number, name, category, subcategory, prices), customize label size,
  margins and sheet layout — then **save as PDF** or **print**.

### Ledgers
- **Sales** and **Purchase** ledgers — date filters, smart search, period totals, tap-to-detail, and a
  **reprint receipt** button on every row.
- **Expenses** — categories, notes, dates, filters and totals; an expense can never exceed the cash balance.
- **Due Ledger (Baki)** — customers/suppliers/employees, per-party statements with running balances, payments
  and manual dues; **due payments are capped at the outstanding due** (balances can't go negative).

### Cash
- **Current Balance = petty cash**: sales cash and customer due collections increase it; expenses and cash-outs
  decrease it; **buying and supplier payments never touch it**. It can never go negative.
- **Cash Management** (Settings): manual Cash In / Cash Out with validation; records are deletable only through
  Activities (audited undo).
- **Cash Drawer**: per-day Opening Cash (carried forward automatically from yesterday's closing), cash sales,
  due collections, cash in/out, expenses and the computed **Closing Cash** — plus the full day's cash
  transaction list with running balances, and Month/Custom history. PDF-exportable.

### Activities & Undo
- **Settings → Activities**: every sale, purchase, expense, cash in/out and stock adjustment of the last 30
  days in one chronological list.
- Each entry can be **undone once** (with confirmation) — the transaction is fully reversed (stock, dues,
  balances) and the undo itself is recorded for a complete audit trail.

### Reports (all PDF-exportable: preview · save · print)
- **Daily Transactions** — sales + purchases by day or range (≤ 1 month), totals + profit + itemized lines.
- **Sales Report** — **Total Sales + Total Profit** for the period, then one invoice card per sale
  (date/time, invoice number, Cash/Due, prominent total, customer when available) listing **every product
  sold** (qty × price, line total) with a Paid/Due footer; day / month / custom range.
- **Stock Report** — total units, stock value, per-product breakdown, movement history.
- **Business Report** — money in/out, net balance, profit (cash/due/total), date-filtered.
- **Buy Report** — all purchases (cash & due) with line items and the period's total buy amount.
- **Expense Report** — record & review expenses (lives on the Reports page).
- **Category Report** — per-product bought/sold quantities, amounts and profit by category/subcategory,
  plus a current-stock summary for the selection.
- **Cash Management Report** — cash in/out by day, month or custom range with totals.
- **Due Ledger (Baki)** — also reachable from the Reports page.

### Receipts & printing
- **Thermal-style receipts** for every sale and purchase (invoice number, items, totals, payment info, footer)
  with an automatic **"Print receipt?"** prompt and reprint from the ledgers.
- **Printing Settings** — one hub for all printed documents: 19 individually toggleable receipt items
  (incl. shop logo, email/website, thank-you & footer messages), report decoration (logo, shop info, generated
  date, footer, custom notes), receipt paper width/margins/font/alignment, report font/margins, and the base
  PDF paper size (A4/Letter). Printing goes through Android's print framework — Bluetooth/USB/Wi-Fi printers,
  any paper the printer supports, with the printer remembered by the system.

### License activation (offline)
- The app is protected by an **offline, device-locked license**: on first launch it shows a Device ID; a signed
  license key (RSA-2048) unlocks the app permanently until expiry. License management (status, expiry, renew /
  replace) lives in Settings. Only the **public key** ships in the app; the license-generator tooling is kept
  in a private folder that is **never** committed to this repository.

### Automatic backups (offline, scheduled)
- **Settings → Backup & restore → Automatic backup** — enabled by default; runs only after you pick a backup
  folder once with the system folder picker (remembered across restarts, changeable anytime).
- Configurable **frequency** (default every day) and **time** (default 11:00 PM); backups run in the background
  (WorkManager) using the **same zip format as manual backups**.
- **Folder verification**: picking a folder immediately creates a real backup — success is confirmed on screen;
  failures show the exact reason (access denied, storage unavailable, insufficient space, …) and the folder is
  not adopted.
- **Retention**: keeps the newest **15 automatic** backups (manual backups are never touched).
- **Missed-backup catch-up**: if the phone was off at the scheduled time, the backup runs on the next app open.
- **Reminders**: a daily 11:00 AM "finish setup" notification while no folder is chosen (tap → Backup page),
  a failure notification with the reason, and an alert if no backup succeeded for 3 days. The manual backup
  reminder is suppressed while automatic backup is active.

### Settings & notifications
- **Reorganized Settings** — seven grouped sections (Shop · Inventory · Money · Printing · Notifications ·
  Data & history · App), one card per group.
- **About MINI POS** shows the current app version (**v1.37**), which tracks the latest completed feature phase.
- Local notifications: **low-stock**, **due reminder**, a daily **backup reminder** (customizable time), and the
  automatic-backup notifications above. Tapping any notification opens the app correctly (no duplicate
  instances); the backup-setup reminder deep-links to the Backup page.
- Branded **splash screen** (1.5 s, yellow, wave-animated wordmark).

---

## Tech stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3), Navigation-Compose
- **Architecture:** MVVM (ViewModel + StateFlow), manual DI via a `ServiceLocator`
- **Persistence:** Room (SQLite) with KSP, exported schemas, explicit migrations (currently **v7**)
- **Preferences:** Jetpack DataStore (current shop, reminders, automatic backup, license, printing settings)
- **Background work / notifications:** WorkManager + NotificationCompat (reminders + scheduled automatic backups)
- **Folder access:** Storage Access Framework + `androidx.documentfile` (automatic backups into a user-picked folder)
- **Serialization:** kotlinx.serialization (backup JSON)
- **Barcodes:** ZXing (`zxing-android-embedded` + `core`) — camera scanning & CODE-128 generation, offline
- **Printing / PDF:** Android `PdfDocument` + print framework (receipts, labels, reports), FileProvider previews
- **Security:** `java.security` RSA (SHA256withRSA) for offline license verification
- **Images:** Coil 3 (loads product/logo files from app storage; no network)
- **Async:** Kotlin Coroutines + Flow
- **Java APIs:** `java.time` via core-library desugaring

---

## Architecture

Clean, layered MVVM:

```
UI (Composable screens)  →  ViewModel (StateFlow)  →  Repository  →  Room DAO / DataStore
```

- **Screens** are stateless-ish Composables that observe `StateFlow`s from a `ViewModel` and call its functions.
- **ViewModels** read repositories from `ServiceLocator` and expose `StateFlow`s; per-shop scoping is done with a
  `shopId` flow + `flatMapLatest`.
- **Repositories** wrap DAOs (Flow reads, suspend writes) and own transactional operations
  (e.g. `commitSale`, `commitPurchase`, undo reversals, shop create/delete cascade).
- **`ServiceLocator`** builds the Room database, repositories and prefs once at app start (`MiniPosApp`).
- **Navigation:** a **license gate** wraps the whole app; a root `AppRoot` then gates on the current shop
  (first-run setup vs. main shell); `TabShell` hosts the bottom tabs (Home · Sell · Products · Reports ·
  Settings); deeper screens are root destinations.
- **Shared building blocks:** one smart-search implementation (`SearchUtil`), one product filter
  (`ProductFilters` + dialog), one barcode scanner dialog, one PDF/report renderer — reused everywhere.

**Money & dates:** money is `Long` paisa formatted via `core/util/Money`; dates are `Long` millis handled via
`core/util/DateUtil`.

---

## Project structure

```
MiniPOS/
├── app/
│   ├── build.gradle.kts            # module build config (SDK levels, deps, KSP, desugaring)
│   ├── schemas/                    # exported Room schemas (v1 … v7) — migration ground truth
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/minipos/
│       │   ├── MainActivity.kt      # splash + license gate + app root
│       │   ├── MiniPosApp.kt        # Application: ServiceLocator init, channels, reminders, backfills
│       │   ├── ServiceLocator.kt    # manual DI (DB + repositories + prefs)
│       │   ├── core/
│       │   │   ├── theme/           # Color, Type, Shape, Theme (yellow brand)
│       │   │   ├── ui/              # reusable composables (AppTopBar, StatCard, dialogs, …)
│       │   │   ├── util/            # Money, DateUtil, DateFilter, ImageStorage, SearchUtil
│       │   │   ├── print/           # PrintPrefs, ReceiptPdf, ReportPdf, PdfShare, ReportPdfAction
│       │   │   └── nav/             # Routes, AppRoot, TabShell, NavGraph
│       │   ├── data/
│       │   │   ├── entity/          # Room @Entity classes + enums
│       │   │   ├── dao/             # one DAO per domain
│       │   │   ├── db/              # MiniPosDatabase, Converters, Migrations (v1→v7)
│       │   │   ├── repo/            # repositories (sales, purchases, products, cash drawer, …)
│       │   │   ├── prefs/           # CurrentShopManager, BackupReminderPrefs, AutoBackupPrefs (DataStore)
│       │   │   └── backup/          # BackupManager (ZIP export/import)
│       │   ├── feature/             # home, sell, buy, product, category, salesledger, purchaseledger,
│       │   │                        # expense, due, report, shop, settings, cash, cashdrawer, backup,
│       │   │                        # activity (undo log), barcode (scanner), barcodeprint (labels),
│       │   │                        # license (offline activation), splash
│       │   └── notify/              # Notifier, ReminderWorker, BackupReminder* + AutoBackup* (runner,
│       │                            # scheduler, workers — scheduled automatic backups)
│       └── res/                     # icons, themes (incl. v31 splash), strings, FileProvider paths
├── gradle/
│   ├── libs.versions.toml          # version catalog (single source of dependency versions)
│   └── wrapper/                    # Gradle wrapper (9.4.1)
├── build.gradle.kts                # root build file
├── settings.gradle.kts
├── BUILD_PLAN.md                   # WHAT to build (spec + phases)
├── CONVENTIONS.md                  # HOW to build it (package layout, tokens, rules)
├── PROGRESS.md                     # WHERE we are (status, checklist, decisions, log)
├── CLAUDE.md                       # short project guide
└── README.md                       # this file
```

> The offline **license-generator tools** (Python CLI/GUI and a companion Android app) live in private,
> git-ignored folders and are intentionally **not** part of this repository.

---

## Requirements

- **Android Studio** — a recent version that supports **Android Gradle Plugin 9.x** (e.g., 2025.x / current stable).
- **JDK 17+** to run Gradle/AGP 9 — the JDK **bundled with Android Studio (JBR)** works out of the box.
- **Android SDK** with the **API 36** platform + matching build-tools (Android Studio will offer to install these
  on first sync).
- **Gradle 9.4.1** — provided automatically by the included Gradle wrapper (`./gradlew`); no manual install needed.
- A device or emulator running **Android 7.0 (API 24) or newer**.
- **Internet** for the first Gradle sync only (to download dependencies). The app itself runs fully offline.

App SDK levels: **minSdk 24**, **targetSdk 34**, **compileSdk 36**.

---

## Getting started

Clone the repository:

```bash
git clone https://github.com/jahirulislamdms/Mini-POS.git
cd Mini-POS
```

> `local.properties` (which points to your Android SDK) is **not** committed. Android Studio generates it for you
> on first open. If you build only from the command line, create it with: `sdk.dir=/path/to/Android/Sdk`.

---

## Open, build & run in Android Studio

1. **Open the project**
   - Launch Android Studio → **File ▸ Open** → select the cloned `Mini-POS` folder → **OK**.

2. **Let Gradle sync**
   - Android Studio runs **Gradle Sync** automatically. Accept any prompts to **install missing SDK platforms /
     build-tools** (API 36). Wait for "Sync finished".

3. **Confirm the JDK (if sync complains)**
   - **Settings/Preferences ▸ Build, Execution, Deployment ▸ Build Tools ▸ Gradle ▸ Gradle JDK** → choose the
     **Android Studio default JBR** (JDK 17+).

4. **Pick a device**
   - Create/launch an emulator via **Device Manager** (API 24+), or connect a physical device with **USB
     debugging** enabled.

5. **Run**
   - Select the **`app`** run configuration and press **Run ▶** (Shift+F10).
   - On first launch, allow the **notifications** permission (Android 13+); the **camera** permission is asked
     the first time you open the barcode scanner.

6. **First use**
   - Activate the license (enter the license key for the shown Device ID), then create your first shop — or
     restore one from a backup — and start selling.

---

## Build from the command line

From the project root (the Gradle wrapper handles the Gradle version):

```bash
# Build a debug APK
./gradlew :app:assembleDebug          # macOS/Linux/Git-Bash
gradlew.bat :app:assembleDebug        # Windows cmd/PowerShell

# Install on a connected device/emulator
./gradlew :app:installDebug

# Clean build
./gradlew clean :app:assembleDebug
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`.

> **Windows note:** Gradle/AGP 9 needs a JDK 17+. If `java` isn't on your `PATH`, point `JAVA_HOME` at Android
> Studio's bundled JBR, for example:
> ```bash
> export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
> ./gradlew :app:assembleDebug
> ```

---

## Data, storage & migrations

- **Database:** Room/SQLite, file `minipos.db`, current **version 7**.
- **Entities:** Shop, ShopSettings, Category, Product, StockMovement, Sale, SaleItem, Purchase, PurchaseItem,
  Expense, ExpenseCategory, Party, Due, DuePayment, MeasureUnit, CashTransaction, ActivityUndo,
  CashDrawerOpening.
- **Migrations** (no destructive fallback — data is always preserved):
  - `1 → 2`: `units` table (custom measurement units).
  - `2 → 3`: `cash_transactions` table (manual cash in/out).
  - `3 → 4`: `activity_undos` table (undo audit trail).
  - `4 → 5`: `cash_drawer_openings` table (per-day opening cash).
  - `5 → 6`: `products.barcode` column + index (barcode system).
  - `6 → 7`: `shop_settings.defaultUnit` column (default product unit).
- **Schemas** are exported to `app/schemas/` and committed, so every migration has a verifiable ground truth.
- **Preferences (DataStore):** current shop, backup-reminder settings, license/device-id store, printing settings.
- **Images:** product photos and shop logos are copied into app-private storage and referenced by relative path
  (loaded with Coil); they're included in backups.

---

## Backup & restore

- **Settings ▸ Backup & restore** (and **Restore from Backup** on the first-run screen).
- **Export** writes a single `.zip` to a location you choose (Storage Access Framework — no storage permission
  needed): `manifest.json` (schema version, shop name, row counts) + `data.json` (all rows for that shop) +
  `images/` (photos + logo).
- **Restore** reads a `.zip`, validates the manifest, and inserts everything under a **new shop**, remapping all
  foreign keys and rewriting image paths. Products without barcodes (from older backups) get unique barcodes
  automatically. Backup schema is currently **v4**; older versions (v2+) remain restorable.
- **Automatic backups** write the same `.zip` format to your chosen folder on a schedule, named
  `MiniPOSAuto_<shop>_<timestamp>.zip`, keeping the newest 15 (manual backups are never deleted) — see
  [Automatic backups](#automatic-backups-offline-scheduled).

---

## Printing

- All printing goes through **Android's print framework** — Bluetooth, USB and Wi-Fi/network printers work via
  the printer maker's installed print service; the system dialog previews the job, supports **any paper size the
  printer offers**, and remembers your printer.
- **Receipts** are generated as thermal-style PDFs sized to your configured roll width (default 58 mm).
- **Reports** render on a configurable base page (A4/Letter) with automatic page breaks; **preview / save PDF /
  print** from every report's top bar.
- **Barcode labels** render CODE-128 sheets from your products' existing barcodes.
- Everything is customizable once in **Settings ▸ Printing Settings** and applied to every future job.

---

## Notifications

All notifications are **local** (no internet). Managed with WorkManager and gated by Settings toggles:

- **Low-stock alert** — daily check; notifies when products are at/below their threshold.
- **Due reminder** — daily reminder of money to collect.
- **Backup reminder** — daily reminder to back up your data; **enable/disable** and a **customizable time**
  (default **10:00 PM**). Automatically suppressed while **automatic backup** is active.
- **Automatic backup notifications** — a daily 11:00 AM setup reminder while no backup folder is chosen
  (tapping it opens the Backup page), a failure alert with the reason, and a warning when no automatic backup
  has succeeded for 3 days.

Tapping any notification opens the app (cold-launches it, or brings the existing instance to the foreground —
never a duplicate). On Android 13+ the app requests the `POST_NOTIFICATIONS` permission on first launch.

---

## Permissions

| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` (13+) | Local low-stock / due / backup reminders |
| `CAMERA` | Barcode scanning (on-device only; asked when the scanner is first opened) |
| `VIBRATE` | Haptic feedback on successful barcode scans |

No internet permission is used at runtime.

---

## Build configuration & versions

Defined centrally in `gradle/libs.versions.toml`:

| Component | Version |
|---|---|
| **App (versionName / versionCode)** | **1.37 / 37** — tracks the latest completed feature phase |
| Android Gradle Plugin | 9.2.1 |
| Kotlin (built-in via AGP) | 2.2.10 |
| Gradle (wrapper) | 9.4.1 |
| Compose BOM | 2026.02.01 |
| KSP | 2.3.9 |
| Room | 2.8.4 |
| Navigation-Compose | 2.9.8 |
| Lifecycle | 2.9.4 |
| DataStore | 1.2.1 |
| WorkManager | 2.11.2 |
| Coil | 3.4.0 |
| ZXing (android-embedded / core) | 4.3.0 / 3.5.3 |
| DocumentFile (SAF folders) | 1.0.1 |
| Coroutines / Serialization JSON | 1.11.0 |
| desugar_jdk_libs | 2.1.5 |
| minSdk / targetSdk / compileSdk | 24 / 34 / 36 |

> This project uses **AGP 9 built-in Kotlin support** — there is intentionally **no `org.jetbrains.kotlin.android`
> plugin**; Kotlin compilation is provided by the Android Gradle Plugin.

---

## Conventions & source-of-truth docs

Three control files define the project and should be read before contributing:

- **`BUILD_PLAN.md`** — WHAT the app is and the build phases (the full spec).
- **`CONVENTIONS.md`** — HOW the code is written: package layout, color/type tokens, MVVM + Room rules,
  money-as-Long-paisa, multi-shop `shopId` scoping, reusable components.
- **`PROGRESS.md`** — WHERE the project stands: current position, task checklist, decisions, and a log of all
  37 completed post-release feature phases (the app version mirrors the latest phase: **v1.37**).
- **`CLAUDE.md`** — a short pointer file with the rule: *always read `BUILD_PLAN.md` and `PROGRESS.md` before
  starting work.*

---

## Troubleshooting

- **Gradle sync fails on the Kotlin/AGP version** — ensure you're on a recent Android Studio that supports AGP
  9.x and that the **Gradle JDK is 17+** (use the bundled JBR).
- **"SDK location not found"** — open the project in Android Studio once (it writes `local.properties`), or create
  `local.properties` with `sdk.dir=/path/to/Android/Sdk`.
- **First build is slow / needs internet** — the first sync downloads Gradle and dependencies; later builds are cached.
- **App shows an activation screen** — this build is license-protected; enter a license key issued for the shown
  Device ID (license tooling is private to the author).
- **Scanner shows a permission message** — grant the Camera permission (Settings ▸ Apps ▸ MINI POS ▸ Permissions).
- **Nothing prints** — install your printer maker's Android **print service** app, then pick the printer in the
  system print dialog (it will be remembered).
- **Notifications don't appear** — grant the notification permission and keep the relevant toggle on in Settings.
- **Command-line build can't find Java** — set `JAVA_HOME` to the Android Studio JBR (see the build section above).

---

## License

No open-source license file is currently included; all rights reserved by the author unless a license is added
later. The app itself uses an offline activation system — usage licenses are issued by the author.

**Author:** Jahirul Islam — <https://jahirulislam.info/>
