# MINI POS — BUILD PLAN (source of truth)

> This file is written once and rarely changes. It defines WHAT we are building.
> `CONVENTIONS.md` defines HOW to write the code. `PROGRESS.md` tracks WHERE we are.
> The AI reads these three files, never the whole chat history.

---

## 1. What this app is
**MINI POS** — a native Android app for small shopkeepers to run sell / buy / expense /
due bookkeeping and inventory for one or more shops. Modeled on the "Hishabee" app's
features and visual style. Single user, on-device, no accounts.

## 2. Hard constraints (never violate)
- **Stack:** Kotlin · Jetpack Compose (Material 3) · Room (SQLite) · Android only.
- **Min SDK 24, Target SDK 34.** Built in Android Studio, Gradle (Kotlin DSL).
- **English only.** No Bangla strings anywhere.
- **Currency:** BDT, symbol `৳`. Currency LABEL is editable per shop; default `৳`.
- **100% offline.** No network calls, no cloud, no sign-in, no analytics, no ads.
- **Multi-shop.** Many shops in one app; each shop's data is fully separate.
- **Backup = LOCAL only, PER SHOP.** Export one shop to a single **ZIP bundle** on the device —
  a **complete snapshot: data + settings + logo + product photos** — and restore from it
  (see `CONVENTIONS.md` §11).
- **Excluded for now:** barcode, WhatsApp share, online storefront, PDF export,
  a standalone "Parties" tab, Google Drive / any cloud, AI photo-upload.

## 3. Customization rule (important)
Nothing user-facing is hardcoded. These live in the DB and are editable in Settings:
shop name / logo / address / phone, currency label, **product categories (fully custom —
user creates all of them, no fixed presets)**, sub-categories, units, expense categories,
low-stock threshold (per product, with a shop default). Adding a new category/unit/expense
type must never require code changes.

## 4. Visual style (match Hishabee)
- Golden-yellow top app bars, dark text on yellow.
- Strong blue for primary buttons, report headers, bottom bars; white text on blue.
- White rounded cards on a light-grey background.
- Green = income / money-in; Red = expense / due / money-out.
- Bottom tab bar on the main shell.
- Exact tokens (hex, radius, type) are in `CONVENTIONS.md` and must be used everywhere.

## 5. Data model (entities — details in Phase 2)
Shop, Category, Product, StockMovement, Sale, SaleItem, Purchase, PurchaseItem,
Expense, ExpenseCategory, Party, Due, DuePayment, ShopSettings.
**Every shop-owned row carries `shopId`. Every query filters by the current shop.**
Money is stored as **Long paisa** (1 ৳ = 100 paisa). Dates as **Long epoch millis**.

## 6. Modules (what each screen does)
1. **Home** — current balance, today's sale, today's expense, dues (receive / give),
   stock count; Day/Month toggle; big Buy & Sell buttons; shortcut grid to ledgers;
   a **Recent Activity** list (latest sells & buys, newest first, tap → detail).
2. **Sell** — Quick Sell (type amount) + Product List (cart with qty & per-item discount);
   mark Cash or Due (Due → pick/create a party). Decrements stock.
3. **Buy** — purchase items from a supplier; Cash or Due. Increments stock.
4. **Products / Inventory** — CRUD: name, sell price, buy price, stock, category,
   sub-category, unit, photo, toggles (low-stock alert, VAT, warranty, wholesale price,
   discount). List with search + category filter. Update-stock screen with +/− steppers.
5. **Sales Ledger** — history with Day / Month / Year / All / Custom filters + search + totals.
6. **Purchase Ledger** — same, for purchases.
7. **Expense Ledger** — expenses under custom categories; filters + totals.
8. **Due Ledger (Baki)** — parties grouped as Customer / Supplier / Employee; "You'll receive"
   vs "You'll give"; record payments; per-party statement & running balance. (Parties live here.)
9. **Stock Report** — total units, total stock value (Σ qty×buy price), per-product breakdown,
   stock-movement history.
10. **Business Report** — cash sale, due collected, other income, cash purchase, due paid,
    other expense → net balance; profit (cash / due / total); date-filtered.
11. **Settings** — shop profile, shop switcher, add/edit/delete shops, manage categories /
    units / expense categories, low-stock threshold, **local backup + restore (per shop)**,
    low-stock & due notifications, about.

---

## 7. BUILD PHASES (each = a clean stop/resume point)
Build strictly in order. Each phase must compile, run, and not crash before moving on.
Detailed task checkboxes live in `PROGRESS.md`.

- **P1 — Project & Theme:** new project, package structure, Gradle deps, color/type/shape
  tokens, reusable composables (TopBar, PrimaryButton, Card, StatCard, AmountText,
  SectionHeader, EmptyState, BottomBar), app shell + nav skeleton with placeholder screens.
- **P2 — Database & multi-shop core:** all Room entities, DAOs, DB class, TypeConverters,
  repositories, `CurrentShop` manager (DataStore), first-run seeding.
- **P3 — Shop management:** shop list/switcher, create/edit/delete shop (all customizable
  fields), switching reloads scoped data.
- **P4 — Products & categories:** category CRUD (fully custom), product CRUD, list +
  search + category filter, update-stock (+/−), stock-movement logging.
- **P5 — Sell:** Quick Sell + cart, per-item discount, Cash/Due, stock decrement, records.
- **P6 — Buy:** supplier purchase, cart, Cash/Due, stock increment, records.
- **P7 — Expenses:** custom expense categories, add/list/filter/totals.
- **P8 — Due Ledger:** parties embedded, receive/give balances, payments, per-party statement.
- **P9 — Reports:** Stock Report + Business Report with date filters.
- **P10 — Home dashboard:** wire all tiles/toggles/buttons to real data + Recent Activity feed.
- **P11 — Backup/restore:** export current shop → **ZIP bundle** (`data.json` + `images/` +
  `manifest.json`) via SAF; import/restore (extract photos, rewrite paths, validate).
- **P12 — Settings, notifications & polish:** settings screen, low-stock/due notifications
  (WorkManager), empty/loading/error states, app icon, final QA pass.

## 8. Definition of Done (every phase)
Compiles · launches · no crash on the happy path · feature works against the real DB ·
follows `CONVENTIONS.md` · `PROGRESS.md` updated (boxes checked, CURRENT POSITION moved,
one-line session log added). Half-written code is never left across a stop.
