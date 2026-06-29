<h1 align="center">🏪 MINI POS</h1>

<p align="center">
  <b>A 100% offline, multi-shop Point-of-Sale &amp; bookkeeping app for small shopkeepers.</b><br/>
  Android · Kotlin · Jetpack Compose · Room
</p>

<p align="center">
  <img alt="Platform" src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white">
  <img alt="Language" src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white">
  <img alt="UI" src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="Database" src="https://img.shields.io/badge/DB-Room%20(SQLite)-003B57?logo=sqlite&logoColor=white">
  <img alt="minSdk" src="https://img.shields.io/badge/minSdk-24-orange">
  <img alt="targetSdk" src="https://img.shields.io/badge/targetSdk-34-orange">
  <img alt="Offline" src="https://img.shields.io/badge/100%25-Offline-success">
</p>

MINI POS lets a shopkeeper run **sell / buy / expense / due (baki)** bookkeeping and **inventory** for one or
more shops, entirely on-device. No accounts, no internet, no cloud, no ads. Currency is **BDT (৳)**.

Made by **[Jahirul Islam](https://jahirulislam.info/)**.

> ℹ️ **Note:** screenshots can be added under a `docs/` or `screenshots/` folder and embedded here once available.

---

## Table of contents

- [Overview](#overview)
- [Features](#features)
- [Tech stack](#tech-stack)
- [Architecture](#architecture)
- [Project structure](#project-structure)
- [Requirements](#requirements)
- [Getting started](#getting-started)
- [Open, build & run in Android Studio](#open-build--run-in-android-studio)
- [Build from the command line](#build-from-the-command-line)
- [Data, storage & migrations](#data-storage--migrations)
- [Backup & restore](#backup--restore)
- [Notifications](#notifications)
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

## Features

### Shops (multi-shop)
- Create / edit / delete shops (name, logo, address, phone, currency label, default low-stock threshold).
- Shop switcher; switching reloads all data scoped to the selected shop.
- First-run onboarding to create the first shop.

### Home dashboard
- **Current balance** (cash in − cash out across sales, purchases, due collections/payments, expenses & manual cash).
- Today / This-month toggle for sales & expenses.
- Tiles: today's sale, today's expense, "you'll receive", "you'll give", products in stock.
- Big **Sell** and **Buy** buttons + shortcut grid (Products, Sales, Purchases, Expenses, Due, Reports).
- **Recent Activity** feed (latest sales & purchases) with tap-through to detail.

### Sell
- **Quick Sell** (type an amount) or a **product cart** (add items, quantity steppers, per-item discount).
- Cash or **Due** checkout — pick or create a customer; supports partial payment.
- Commits the sale, decrements stock, logs a stock movement, and records a receivable due when on credit.

### Buy
- Supplier purchase cart (editable buy price + quantity), Cash or Due.
- Commits the purchase, increments stock, logs a stock movement, and records a payable due when on credit.

### Products & inventory
- Full product CRUD: name, sell price, buy price, opening stock, category, sub-category, unit, photo, and
  optional toggles (low-stock alert, VAT %, warranty, wholesale price, discount %).
- Product list with **search** + **category filter**.
- **Product Details** page (read-only by default) with quick actions: **Sell**, **Buy**, **Edit**.
- **Update Stock** screen and an in-edit **stock adjustment** (inventory correction that is *not* a sale).
- Stock-movement history (audit trail) for every change.

### Catalog management
- Fully custom **categories** + **sub-categories** (no fixed presets).
- Custom **measurement units** (e.g. pcs, kg, litre).
- Custom **expense categories** (seeded with Salary / Rent / Bill / Purchase, all editable).

### Ledgers
- **Sales Ledger** and **Purchase Ledger** — history (newest first) with Day / Month / Year / All / Custom date
  filters, search, period totals, and tap-to-detail.
- **Expenses** — add/edit with category, note and date; date filters + totals.
- **Due Ledger (Baki)** — parties grouped as Customer / Supplier / Employee; "You'll receive" vs "You'll give"
  headline; per-party net balance, record payments, add manual dues, and a per-party statement with running balance.

### Cash management
- Manual **Cash In / Cash Out** adjustments (amount, optional note, automatic date/time) for cash not tied to a
  sale or purchase. Affects the **Current Balance only** — never recorded as a sale/purchase, never in reports.

### Reports
- **Daily Transactions Report** — sales + purchases for a single day or a custom range (max 1 month): totals,
  profit/loss, and itemized transactions (product, quantity, unit price, line total, date/time).
- **Stock Report** — total units, total stock value, per-product breakdown, and movement history.
- **Business Report** — money in/out (cash sale, due collected, cash purchase, due paid, other expense), net
  balance and profit (cash / due / total), date-filtered.

### Backup & restore (per shop)
- Export the current shop to a single **`.zip`** (via the Storage Access Framework): `data.json` + `images/`
  (product photos + logo) + `manifest.json`.
- Restore from a `.zip`: validates the manifest, inserts under a new shop with full foreign-key remapping and
  image-path rewriting. Backward compatible (restores older backup versions).

### Settings & notifications
- Shop, catalog, money and data sections; default low-stock threshold editor; About page.
- Local notifications (no internet): **low-stock** alert, **due** reminder, and a daily **backup reminder**
  (enable/disable, customizable time — default 10:00 PM), powered by WorkManager.

---

## Tech stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3), Navigation-Compose
- **Architecture:** MVVM (ViewModel + StateFlow), manual DI via a `ServiceLocator`
- **Persistence:** Room (SQLite) with KSP, exported schemas, explicit migrations
- **Preferences:** Jetpack DataStore (current shop, backup-reminder settings)
- **Background work / notifications:** WorkManager + NotificationCompat
- **Serialization:** kotlinx.serialization (backup JSON)
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
  (e.g. `commitSale`, `commitPurchase`, shop create/delete cascade).
- **`ServiceLocator`** builds the Room database, repositories and prefs once at app start (`MiniPosApp`).
- **Navigation:** a root `AppRoot` gates on the current shop (onboarding vs. main shell); `TabShell` hosts the
  bottom-tab navigation (Home · Sell · Products · Reports · Settings); deeper screens are root destinations.

**Money & dates:** money is `Long` paisa formatted via `core/util/Money`; dates are `Long` millis handled via
`core/util/DateUtil`.

---

## Project structure

```
MiniPOS/
├── app/
│   ├── build.gradle.kts            # module build config (SDK levels, deps, KSP, desugaring)
│   ├── schemas/                    # exported Room schemas (v1, v2, v3) — migration ground truth
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/minipos/
│       │   ├── MainActivity.kt
│       │   ├── MiniPosApp.kt        # Application: ServiceLocator init, notification channel, reminders
│       │   ├── ServiceLocator.kt    # manual DI (DB + repositories + prefs)
│       │   ├── core/
│       │   │   ├── theme/           # Color, Type, Shape, Theme (yellow brand)
│       │   │   ├── ui/              # reusable composables (AppTopBar, StatCard, AppDropdown, dialogs, …)
│       │   │   ├── util/            # Money, DateUtil, DateFilter, ImageStorage
│       │   │   └── nav/             # Routes, AppRoot, TabShell, NavGraph
│       │   ├── data/
│       │   │   ├── entity/          # Room @Entity classes + enums
│       │   │   ├── dao/             # one DAO per domain
│       │   │   ├── db/              # MiniPosDatabase, Converters, Migrations
│       │   │   ├── repo/            # repositories (one per domain)
│       │   │   ├── prefs/           # CurrentShopManager, BackupReminderPrefs (DataStore)
│       │   │   └── backup/          # BackupManager (ZIP export/import)
│       │   ├── feature/             # home, sell, buy, product, category, salesledger,
│       │   │                        # purchaseledger, expense, due, report, shop, settings, cash, backup
│       │   └── notify/              # Notifier, ReminderWorker, BackupReminderScheduler/Worker
│       └── res/                     # icons, themes, strings
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
   - (Or from the welcome screen: **Open** → choose the folder.)

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
   - Select the **`app`** run configuration and press **Run ▶** (Shift+F10). Android Studio builds, installs and
     launches the app.
   - On first launch, allow the **notifications** permission (Android 13+) so reminders can be shown.

6. **First use**
   - Create your first shop when prompted, then start adding products and recording sales/purchases.

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

- **Database:** Room/SQLite, file `minipos.db`, current **version 3**.
- **Entities:** Shop, ShopSettings, Category, Product, StockMovement, Sale, SaleItem, Purchase, PurchaseItem,
  Expense, ExpenseCategory, Party, Due, DuePayment, MeasureUnit, CashTransaction.
- **Migrations** (no destructive fallback — data is always preserved):
  - `1 → 2`: add `units` table (custom measurement units).
  - `2 → 3`: add `cash_transactions` table (manual cash in/out).
- **Schemas** are exported to `app/schemas/` and committed, so every migration has a verifiable ground truth.
- **Preferences (DataStore):** the selected shop id, and the backup-reminder enabled flag + time.
- **Images:** product photos and shop logos are copied into app-private storage and referenced by relative path
  (loaded with Coil); they're included in backups.

---

## Backup & restore

- **Settings ▸ Backup & restore.**
- **Export** writes a single `.zip` to a location you choose (Storage Access Framework — no storage permission
  needed): `manifest.json` (app/schema version, shop name, row counts) + `data.json` (all rows for that shop) +
  `images/` (photos + logo).
- **Restore** reads a `.zip`, validates the manifest, and inserts everything under a **new shop**, remapping all
  foreign keys and rewriting image paths. Older backup versions remain restorable (backward compatible).

---

## Notifications

All notifications are **local** (no internet). Managed with WorkManager and gated by Settings toggles:

- **Low-stock alert** — daily check; notifies when products are at/below their threshold.
- **Due reminder** — daily reminder of money to collect.
- **Backup reminder** — daily reminder to back up your data; **enable/disable** and a **customizable time**
  (default **10:00 PM**).

On Android 13+ the app requests the `POST_NOTIFICATIONS` permission on first launch.

---

## Build configuration & versions

Defined centrally in `gradle/libs.versions.toml`:

| Component | Version |
|---|---|
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
- **`PROGRESS.md`** — WHERE the project stands: current position, task checklist, decisions, and session log.
- **`CLAUDE.md`** — a short pointer file with the rule: *always read `BUILD_PLAN.md` and `PROGRESS.md` before
  starting work.*

---

## Troubleshooting

- **Gradle sync fails on the Kotlin/AGP version** — ensure you're on a recent Android Studio that supports AGP
  9.x and that the **Gradle JDK is 17+** (use the bundled JBR).
- **"SDK location not found"** — open the project in Android Studio once (it writes `local.properties`), or create
  `local.properties` with `sdk.dir=/path/to/Android/Sdk`.
- **First build is slow / needs internet** — the first sync downloads Gradle and dependencies; later builds are cached.
- **Notifications don't appear** — grant the notification permission (Settings ▸ Apps ▸ MINI POS ▸ Notifications)
  and keep the relevant toggle on in the app's Settings.
- **Command-line build can't find Java** — set `JAVA_HOME` to the Android Studio JBR (see the build section above).

---

## License

No license file is currently included; all rights reserved by the author unless a license is added later.

**Author:** Jahirul Islam — <https://jahirulislam.info/>
