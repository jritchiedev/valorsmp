# Economy

The player-driven currency system: wallets, transactions, and integrity rules. Distinct from Valor score — see `docs/valor-system.md` §1–2 for why these are kept strictly separate.

---

## 1. Currency Representation

- Balance stored as `long` minor units (e.g., cents of the server's named currency, "Valor Coin" or whatever display name is chosen) — **never** `double`/`float`, per `DECISIONS.md` ADR-003.
- Display formatting converts minor units to major units for player-facing text via a single centralized formatting utility (`EconomyFormatUtil` or equivalent), so every UI surface (chat, GUI, scoreboard) formats currency identically and any future formatting change (e.g., adding a currency symbol) is a one-place edit.

## 2. Earning Currency

| Source | Notes |
|---|---|
| Selling items (to server shop or player market, depending on which is implemented) | |
| Quest/event rewards | Overlaps in *source* with Valor rewards but tracked as a separate mutation through `EconomyService`, not derived from Valor score |
| Admin grants | Logged with reason, per `SECURITY.md` §9 |

## 3. Spending Currency

- Cosmetic purchases (where a cosmetic is currency-purchasable rather than Valor-rank-gated — the two unlock paths can coexist per item, as a server-operator content decision).
- Crate keys (if key purchase via currency is enabled — server-operator config choice, `crates.yml`).
- Player-to-player trading/market, if implemented (see `docs/future-features.md`).

## 4. Transaction Integrity

Per `SECURITY.md` §9, all mutations go through `EconomyService`, which:

- Performs atomic balance adjustments at the database level (`WalletRepository#adjustBalance` as a single `UPDATE balance = balance + ?`, never read-then-write).
- Never allows a balance to go negative unless the specific operation is explicitly designed to allow overdraft (not currently planned; would need its own design/config if ever proposed).
- Logs every non-player-initiated-transfer mutation (admin grants, quest/event rewards) with enough detail to audit: who, how much, why, when.
- Fires `BalanceChangedEvent` on every mutation, carrying old balance, new balance, and a reason string, for any feature that needs to react (e.g., `CosmeticsService` re-checking currency-gated unlock eligibility).

## 5. Player-to-Player Transfers

- If direct player-to-player transfer (`/pay`) is enabled: rate-limited per player to prevent spam/abuse, and — per `SECURITY.md` §8 — any transfer above a configurable threshold may require confirmation (`/pay confirm`) to reduce costly typo-driven mistakes (e.g., an extra zero).
- Transfers are a single atomic operation (debit sender, credit recipient) — never two separate non-transactional steps that could leave the system in an inconsistent state if interrupted mid-operation (server crash between steps).

## 6. Starting Balance & Sinks

- New players start with a configurable starting balance (`economy.yml`, default a small, deliberately modest amount — avoid making currency trivially abundant from the start, which undermines the economy's meaningfulness).
- Currency sinks (things that remove currency from circulation — cosmetic purchases, crate key purchases, market listing fees if implemented) are a deliberate design lever against inflation; `economy.yml`'s pricing should be periodically reviewed against actual currency-in-circulation metrics (`OBSERVABILITY.md` §2's economy mutation tracking) rather than set once and forgotten.

## 7. Class Responsibilities

| Class | Responsibility |
|---|---|
| `EconomyService` | All balance mutation logic, validation, atomic adjustment orchestration |
| `WalletRepository` | Persistence of `wallets` table, atomic `adjustBalance` |
| `Wallet` (model) | Data shape: owner UUID, balance in minor units |
| `BalanceChangedEvent` | Domain event carrying old/new balance and reason |
| `EconomyFormatUtil` | Centralized minor-unit-to-display-string formatting |

## 8. Open Questions

- Whether a full player-run market (listing items for sale, browsable by others) is in scope, versus a simpler server-shop-only model initially. Flagged in `docs/future-features.md` pending a `ROADMAP.md` milestone decision — affects `EconomyService`'s eventual scope significantly (a market needs its own repository/service, likely `MarketService`, not folded into `EconomyService` itself).
