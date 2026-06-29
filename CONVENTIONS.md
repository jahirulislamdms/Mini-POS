# MINI POS — CONVENTIONS (how to write the code)

> Read fully before writing code. These rules make every session produce identical-looking,
> consistent code. If something here conflicts with a habit, follow this file.

---

## 1. Package structure (under `com.minipos`)
```
com.minipos
├── MainActivity.kt
├── MiniPosApp.kt                 // Application class, DB/repo wiring
├── core/
│   ├── theme/                    // Color.kt, Type.kt, Shape.kt, Theme.kt
│   ├── ui/                       // reusable composables (AppTopBar, PrimaryButton, ...)
│   ├── util/                     // Money.kt, DateUtil.kt, formatters
│   └── nav/                      // Routes.kt, NavGraph.kt
├── data/
│   ├── entity/                   // Room @Entity classes
│   ├── dao/                      // @Dao interfaces
│   ├── db/                       // MiniPosDatabase, Converters, seeding
│   ├── repo/                     // repositories (one per domain)
│   └── prefs/                    // CurrentShopManager (DataStore)
└── feature/
    ├── home/  sell/  buy/  product/  category/
    ├── salesledger/  purchaseledger/  expense/  due/
    ├── report/  shop/  settings/  backup/
        // each feature folder: <Name>Screen.kt + <Name>ViewModel.kt (+ small UI parts)
```
One feature = one folder. UI in `Screen`, logic/state in `ViewModel`. No business logic in composables.

## 2. Architecture
- **MVVM + Repository.** Screen → ViewModel (StateFlow) → Repository → DAO.
- ViewModel exposes a single `data class XUiState` via `StateFlow`; collect with
  `collectAsStateWithLifecycle()`.
- Coroutines for all DB work (`viewModelScope`, `Dispatchers.IO` in repo). No DB on main thread.
- Manual DI is fine: build DB + repos in `MiniPosApp`, pass down (or a tiny ServiceLocator).
  Do NOT add Hilt/Dagger unless P12 says so.
- Navigation: Compose Navigation, routes as constants in `Routes.kt`.

## 3. Multi-shop scoping (critical)
- Every shop-owned entity has `shopId: Long`. Every DAO read takes `shopId` and filters by it.
- `CurrentShopManager` stores the selected `shopId` in DataStore and exposes it as Flow.
- Screens observe current shopId and re-query when it changes. Never show data across shops.
- Seeding/new shop creates that shop's default expense categories and a `ShopSettings` row.

## 4. Money & dates
- **Money = `Long` paisa** (the currency is always **Taka `৳`**; paisa is ONLY the internal integer unit, like cents, so percentage discounts/VAT never cause rounding bugs). Store `priceInPaisa`. 1 ৳ = 100 paisa. The user always sees and types Taka.
  - `Money.format(paisa)` → `"৳ 1,800"` (group thousands, drop `.00`, keep paisa only if non-zero).
  - Parse user input "1800" → `180000` paisa. Centralize in `core/util/Money.kt`. Never use Float/Double for money.
- **Dates = `Long` epoch millis.** Helpers in `DateUtil.kt` for start/end of day/month/year and display `"dd MMM yy"`. Use `java.time` (desugaring on).

## 5. Color tokens (define in `core/theme/Color.kt`, use ONLY these)
These approximate Hishabee — tune the hex slightly if you want an exact match, but keep the names.
```
BrandYellow   = #FFC107   // top app bars
OnYellow      = #1F2937   // text/icons on yellow
PrimaryBlue   = #1565C0   // primary buttons, report headers, bottom bar
OnBlue        = #FFFFFF
IncomeGreen   = #16A34A   // money in / positive
ExpenseRed    = #E53935   // money out / due / negative
AppBackground = #F4F5F7   // screen background (light grey)
Surface       = #FFFFFF   // cards
OnSurface     = #1F2937   // primary text
TextMuted     = #6B7280   // secondary text / hints
Divider       = #E5E7EB
DueReceiveBg  = #FCEBEA   // light red tile (you'll receive)
DueGiveBg     = #E9F7EF   // light green tile (you'll give)
```
Map into a Material 3 `lightColorScheme` (primary = PrimaryBlue). Dark theme not required (ship light only).

## 6. Typography & shape
- Font: system default (Roboto). Optional: bundle Inter later. Sizes: title 20sp bold,
  section header 16sp semibold, body 14sp, caption 12sp.
- Corner radius: cards/buttons **12.dp**; top bars square. Card elevation 1–2.dp + 1.dp divider feel.
- Standard padding: screen 16.dp, card inner 16.dp, gap between cards 12.dp.

## 7. Reusable components (build in P1, reuse everywhere — don't re-style per screen)
`AppTopBar(title, onBack?, actions?)` (yellow) · `PrimaryButton` / `SecondaryButton` (blue/outlined) ·
`AppCard` · `StatCard(label, amount, color)` · `AmountText(paisa, type)` (green/red) ·
`SectionHeader(text)` · `EmptyState(icon, message)` · `AppBottomBar(tabs)` ·
`AppTextField` · `FilterChipsRow(Day/Month/Year/All/Custom)` · `QtyStepper(value, +/−)`.

## 8. Room rules
- Entities: `@Entity(tableName="...")`, `id: Long = 0` `@PrimaryKey(autoGenerate=true)`, `shopId` indexed.
- DAOs: suspend for writes; `Flow<List<...>>` for reads that drive UI. Filter by `shopId` + date range in SQL.
- `MiniPosDatabase` holds version; **always provide a Migration** when schema changes
  (no destructive fallback in release). Keep `exportSchema = true`.
- `Converters` for any enum (store enums as String name).

## 9. UI/UX rules
- Every list screen has: loading state, empty state (use `EmptyState`), and content state.
- Money in green/red via `AmountText` only. Destructive actions (delete) need a confirm dialog.
- Show the active shop name in the Home top bar. Forms validate before save; show inline errors.
- No hardcoded strings for categories/units/expense types — read from DB.

## 10. Quality bar
- No crashes on empty data (fresh shop with zero records must render cleanly).
- No `!!` on nullable DB results; handle nulls. No work on main thread.
- Keep files small; split a screen if it grows past ~300 lines.
- Comment only non-obvious logic. Match existing naming exactly.

## 11. Files, images & backup (per shop)
- **Product photos & shop logo:** when the user picks an image, COPY it into app storage —
  products at `files/shop_<shopId>/products/<uuid>.jpg`, the shop logo at
  `files/shop_<shopId>/logo.jpg` — and store that **relative path** in the DB.
  Never store a gallery `content://` URI (it can break). Load with Coil from the file.
- **Backup is a ZIP bundle, NOT a bare `.json`** (a bare JSON would lose the photos).
  Each backup `MiniPOS_<shopName>_<yyyyMMdd>.zip` contains:
  - `data.json` — that shop's **complete state**: the Shop profile row + ShopSettings (currency
    label, low-stock default, notification prefs) + all custom categories / sub-categories / units /
    expense categories + every transactional table (products, sales, saleItems, purchases,
    purchaseItems, expenses, parties, dues, duePayments, stockMovements) — all filtered by `shopId`.
  - `images/` — every product photo **and the shop logo** for that shop.
  - `manifest.json` — app version, schema/DB version, shop name, row counts (for validation).
- **Restore:** open the zip → validate `manifest` (schema version must match/upgrade) →
  insert rows under a shopId (new shop or replace) → extract `images/` into
  `files/shop_<id>/products/` → rewrite photo paths to the new location → report counts.
- Use the Storage Access Framework (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`) so the
  user picks where the zip is saved/loaded. No external storage permissions needed.
