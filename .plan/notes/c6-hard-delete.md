# C6 hard-delete recon (verified against source 2026-06-13)

Scratch inventory for the C6 backend agent. file:line verified by lead via Read. Terse.

## 1. FK setup — SqlTableMetadataExecutor.executeSetInherit (150-193)
- 173-180: each parent FK = `foreignKey(other.getPrimaryKeyFields()).references(other.getJooqTable(), other.getPrimaryKeyFields()).onUpdateCascade().onDeleteCascade()`.
  - FK references the **DIRECT parent's PK** (`other`), named `fkey_<table>_extends_<parent>`.
  - **onDeleteCascade IS set.** For module M2 extends M1 extends Root: FK chain M2→M1→Root, each cascade. Deleting M1's row cascades to M2's row.
- 186-192: mg_tableclass block SKIPPED when `tableType==MODULE` → module subtype tables have NO mg_tableclass (composition, not is-a).
- **Consequence for C6:** the removed-module set is **downward-closed** — a child module is active ⟹ its ancestor chain is active, so removing a parent module implies its children are already removed. Cascade is therefore SAFE. But we delete EXPLICITLY child-first (deterministic counts, no cascade reliance); cascade is backstop only.

## 2. DELETE primitive — SqlTable.deleteBatch (821-838)
- `private static int deleteBatch(SqlTable table, Collection<Row> rows)`: `table.getJooq().deleteFrom(table.getJooqTable()).where(getWhereConditionForBatchDelete(rows)).execute()`.
- **This is the canonical batched DELETE-by-PK on ONE table.** Module PK = FK-on-root-PK = already present in each Row → `deleteBatch(moduleTable, subset)` deletes module rows on the shared root PK. REUSE DIRECTLY.
- `getWhereConditionForBatchDelete` (844): `OR` of per-row PK-eq conditions (`getColumnCondition(r, keyPart)` over `getPrimaryKeyColumns()`). Reusable for the prior-array SELECT too.
- `getUpdateCondition(row, pkeyFields)` (722): single-row PK-eq AND-condition (used by updateBatch).
- `delete(Iterable, strict)` (737): public API, iterates is-a superclass chain (line 766-768) — NOT what C6 wants (we delete ONE module table per removed module, not its chain in one call; the chain is handled by the per-removed-table loop).

## 3. In-tx prior-array read (O-6)
- NO existing select-by-pk helper in SqlTable. Build: `getJooq().select(discFields + pkFields).from(<owningTable>.getJooqTable()).where(getWhereConditionForBatchDelete(rows)).fetch()`.
- MODULE_ARRAY column physically lives on its **owning table** (`Column.getTable()`); group discriminators by owning table. Common case (fixtures + Phase H): all MODULE_ARRAY cols on the ROOT → ONE SELECT for the batch = O-6's "one in-tx SELECT".
- Must run BEFORE the is-a update loop (lines 661-685) overwrites R's array column (the array is R's local stored column, written when current==root).

## 4. updateBatch flow today — SqlTable (653-695)
- 654-659: `allTables = ancestorChainRootFirst + table`; rootTable = allTables.get(0).
- 661-685: per-table pure-UPDATE loop (overwrites R's MODULE_ARRAY local col when current==root).
- 687-692: `insertModuleRows(table, rows, true, isATableKeys)` = UPSERT active modules (C3). NO delete today.
- **C6 inserts:** (a) prior-array SELECT BEFORE 661; (b) after 692, compute removed & deleteBatch per removed module table.

## 5. Active-set helpers (reuse) — SqlTable
- `activeModuleTables(entry, row)` (427-469): reads `row.getStringArray(disc.getName())` per discriminator; expands module-ancestors (root-first, deduped via `expandModuleAncestors` 471-495, root excluded); returns ONLY module subtype tables (resolved in entry's schema). Empty if row carries no discriminator values.
- `activeModuleTableKeys(entry, row)` (569-573): same as schema.table key Set.
- `expandModuleAncestors` uses `getAncestorsRootFirst()` (TableMetadata 788) = root + intermediate **modules** only (no DATA/is-a tables; a module's parents are modules or the root). getRootTable (769) → shared root.

## 6. THE PARTIAL-UPDATE ARRAY-OMISSION TRAP (must handle for correctness)
- `activeModuleTables(entry, row)` returns EMPTY when the update row does NOT provide the MODULE_ARRAY column (getStringArray → null).
- Naive `removed = oldActive \ activeModuleTables(row)` would then delete ALL module rows on any partial update that doesn't resend the array. **BUG.**
- FIX: build `effectiveNewArrays` per discriminator = `row.getColumnNames().contains(disc.name) ? row's value : priorValue(from O-6 SELECT)`. newActive = activeModuleTables over a synthetic row carrying effective arrays. A discriminator not provided ⟹ same in old & new ⟹ nets to zero removal. Per-discriminator-aware (one provided, another omitted → diff only the provided axis).

## 7. MODULE_ARRAY-only / mix-smoke safety (scope item 4) — CONFIRMED
- removed set derived ONLY from `getDiscriminatorColumns()` (MODULE_ARRAY) via activeModuleTables → returns ONLY module subtype tables. is-a chain tables (isATableKeys) never appear. mg_tableclass/is-a identity rows CANNOT be deleted by C6. Defensive extra: guard `tableType.isModule()` before delete.

## 8. CARRY FROM C3 REVIEW — partial-module-update NULLing (scope item 3, owner-decided)
- insertModuleRows (538-567) builds cols via `getInsertColumns(moduleTable, providedNames)` (384) which filters ONLY refbacks, NOT by columnsProvided for regular cols → omitted module columns get NULL-overwritten on upsert. Identical to pre-existing is-a upsert behavior (NOT a C3 regression).
- insertModuleRows is SHARED insert+update (updateOnConflict flag is also true for SAVE inserts → does NOT cleanly distinguish). A getUpdateColumns-style provided-filter on the UPDATE path would need an explicit insert-vs-update signal.

## C6 design (proposed; pending owner OK on scope item 3)
1. In `updateBatch`, if `entry.getDiscriminatorColumns()` non-empty: SELECT prior discriminator arrays by PK (group by owning table; ONE select common case) BEFORE the is-a update loop. Build pk→priorArrays.
2. After the existing upsert (692): per row, oldActive = activeModuleTables(syntheticOldRow); newActive = activeModuleTables(syntheticEffectiveRow [provided⊕prior]); removed = oldActive \ newActive (schema.table keys).
3. Group removed tables across the batch; delete CHILD-FIRST (reverse root-first); for each removed module table, `deleteBatch(moduleTable, rowsWhoseRemovedContainsIt)`. Guard tableType.isModule().
4. Empty-discriminator ⟹ whole block skipped = exact no-op (is-a path untouched).
