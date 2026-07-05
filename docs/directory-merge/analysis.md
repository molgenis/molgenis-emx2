# Merging the Directory — Data Analysis & Migration Risk

> Section four. Quantifies, from the **real dumps**, how each row most likely maps — and where automation stops and a human/curator starts. Audience: data manager + Directory stakeholders.

## 1. Biobank shapes (849 biobanks)

| shape | count | share | most likely mapping |
|---|---|---|---|
| 0 collections | 102 | 12% | bare registration → an `Organisations` identity, holds no `Collections` |
| 1 collection | 475 | 56% | the 1:1 case → **triage**: usually collapse to one `Collection` **held by** its legal entity; mint a biobank-`Organisations` only if it is a real org unit |
| 2+ collections | 272 | 32% | umbrella org → a biobank-`Organisations` holding N `Collections` |

The 1:1 majority (56%) is the migration's main judgement call — see §4.

## 2. Legal entities — `juridical_person` (the case for upgrading it)

`juridical_person` is a **required free-text** field (MIABIS-2.0-05), and the values are unmistakably organisations (e.g. *Tel-Aviv University*, *University of British Columbia*, *Instituto de Medicina Molecular*, *Biobanque CHU de Toulouse*).

| metric | value | implication |
|---|---|---|
| populated | 849 / 849 | every biobank names a legal entity |
| distinct values | **500** | legal entities are **shared** — one university hosts several biobanks |
| == biobank name/acronym | 130 (15%) | so **85% name a distinct parent** legal entity, not the biobank itself |

**→ Upgrade `juridical_person` (text) to an `Organisations` identity reference.** Mint ~500 legal-entity `Organisations`; link each biobank via `part of` (legal entity). A curator dedups near-duplicate spellings. This is real organisational structure worth capturing, not noise.

## 3. Existing catalogue organisations — the dedup migration

| metric | value |
|---|---|
| current per-resource `Organisations` rows | **871** |
| distinct names (→ top level identities) | **319** |
| collapse | 871 → 319 (**63% fewer** rows) |

The identity split turns each of the 871 per-resource rows into an `Organisation roles` row **plus** folds it (by name/ROR) into one of ~319 top level `Organisations` identities. **This dedup is the single biggest automated-migration correctness risk** — name-matching quality decides it; curator review of near-duplicates required.

## 4. `parent_collection` — combination cases (206 parents)

| pattern | count | handling |
|---|---|---|
| single-child parent | 46 | review — likely artifacts / one-sub |
| multi-child, **1 dimension varies** | 90 (56% of multi) | **clean → `Collection facts`** (automatable) |
| multi-child, **2+ dimensions vary** | 57 (36%) | combination cases — see below |
| multi-child, 0 dimensions vary | 15 | review — duplicates / vary on name only |

**Combination cases don't break `Collection facts`** — it's a star schema, so children varying by *material AND diagnosis* still land in one facts row-set with both dimensions populated. The genuine splits are only when a varying dimension is **`type`** (a distinct resource/study → promote to a separate `Collections` row) or **`data_categories`/temporal** (→ a modality dimension or `Collection events`). Top varying-dimension sets: `materials` (71), `materials + data_categories` (9), `materials + diagnosis + data_categories` (5), `materials + diagnosis + type + data_categories` (4).

## 5. Risk summary — where automation stops

| item | automatable | needs human/curator |
|---|---|---|
| biobank 0 / 2+ collections (374) | ✓ deterministic | — |
| biobank 1:1 (475) | partial | **is-it-a-real-org?** triage |
| `juridical_person` → 500 legal-entity identities | mint + fuzzy-dedup | curator confirms near-duplicates |
| existing-catalogue org dedup (871 → 319) | fuzzy-match | curator confirms near-duplicates |
| `parent_collection` 1-dim (90) | ✓ → facts | — |
| `parent_collection` 2+dim / single-child / no-vary (~118) | partial | review the `type`-varying + artifact cases |

**Bottom line for the PoC:** ~60% of the hard structural decisions automate cleanly; the concentrated human effort is (a) org/legal-entity dedup (fuzzy name matching + curation) and (b) ~118 of 206 `parent_collection` parents. Both are the PoC's measurement targets.
