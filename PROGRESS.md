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
**Next task:** (none active) — all post-release phases done & compiling (assembleDebug ✓; DB v3). See "POST-RELEASE PLAN" below.

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
