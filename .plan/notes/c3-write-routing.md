# C3 write-routing recon (verified against source 2026-06-13)

Scratch inventory for the C3 backend agent. file:line verified by lead via Read. Terse.

## 1. Write path — SqlTable.java
- `save(rows)` (154) → `executeTransaction(db, schema, table, rows, SAVE)`.
- `executeTransaction` (196-314): buckets rows by `MG_TABLECLASS` into `subclassRows` map.
  - 223-257: if row has MG_TABLECLASS, validate/qualify it; ELSE (256) `row.set(MG_TABLECLASS, tableClass)` = R's own class. So a plain root R with NO is-a subclass still buckets under R.
  - 273/292: flushes each bucket via `executeBatch(schema, type, count, subclassRows, subclassName, columnsProvided)`.
- `executeBatch` (320-350): resolves concrete `table = schema.getTable(subclassName.split(".")[1])`.
  - UPDATE → `getUpdateColumns(table, columnsProvided)` + `applyValidationAndComputed(table.getColumns(), rows)` → `updateBatch`.
  - SAVE/INSERT → `getInsertColumns(table, columnsProvided)` + `applyValidationAndComputed(insertColumns, rows)` → `insertBatch(table, rows, SAVE?, insertColumns)`.
- `getInsertColumns(table, provided)` (352): `table.getColumnsWithoutHeadings()` minus pure refback. **Filters by `table`'s metadata** → for R this EXCLUDES module-owned columns (they live on the module table, not R).
- `getUpdateColumns` (363): insertColumns minus readonly/pkey/inserted-audit, kept if AUTO_ID/computed/provided.

## 2. insertBatch (395-427) — the is-a chain writer
- 397: `chain = ancestorChainRootFirst(table); chain.add(table)` → root-first, then self.
- 402-424 loop: `isRoot = (i==0)`; `columns = getLocalStoredColumns(current, updateColumns)`; skip if empty; `insertIntoSingleTable(current, rows, updateOnConflict, columns, isRoot)`.
- 411-413: `if (isRoot) rootRecords = inserted`.
- 415-423: AUTO_ID PK columns of `current` copied back into every row via `copyRecordValuesIntoRows` → **shared root PK propagated to all chain tables + remains in the Row for later use**.
- returns rootRecords.

## insertIntoSingleTable (429-473)
- writes `columns` (a single table's local stored cols) for all `rows`.
- `getSelectedRowValues(columns, row)` (536) → per-column typed value; AUTO_ID generated if null.
- isRoot: sets MG_INSERTEDBY/ON + MG_UPDATEDBY/ON.
- `updateOnConflict` (453-470): `onConflict(pkeyFields).doUpdate()` set each non-readonly col from `excluded.*` → this is the **upsert** primitive C3 update reuses.
- returns `step.returningResult(pkeyFields).fetch()`.

## copyRecordValuesIntoRows (475-479)
`for (col in toCopy) row.set(col.getName(), from.getValue(col.getName()))`. Used to copy generated PK down.

## 3. updateBatch (489-524)
- 490-495: `allTables = ancestorChainRootFirst(table) + table`; `rootTable = allTables.get(0)`.
- 497-521 loop: per table, `columns = getLocalStoredColumns(current, updateColumns)`; build per-row `update(jooqTable).set(values).where(getUpdateCondition(row, pkey))`; batch execute.
- **Pure UPDATE per table — no delete+reinsert, no onConflict.** Newly-needed rows are NOT created here today.

## getLocalStoredColumns (526-534)
`table.getStoredColumns()` ∩ updateColumn names, then `getExpandedColumns`. For a subtype this includes its PK (the FK-on-root-PK) — that is how is-a subtype rows get keyed today; same applies to a module subtype table.

## ancestorChainRootFirst (382-393)
maps `table.getMetadata().getAncestorsRootFirst()` → SqlTable list (resolves cross-schema via db.getSchema/getTable).

## 4. applyValidationAndComputed (SqlTypeUtils 26-81)
- iterates ONLY the passed `columns` list. Module columns NOT in R's list are **never touched** → they SURVIVE in the Row into insertBatch. Confirms module data is available for routing.
- invisible-col clear at 72-78 (C5 will gate module cols here).

## 5. Discriminator accessors
- `TableMetadata.getDiscriminatorColumns()` (771): `getColumns().filter(Column::isDiscriminator)` — includes inherited cols.
- `Column.isDiscriminator()` (724): `getColumnType() == MODULE_ARRAY`.
- `Column.getValues()` (107): allowed `"schema.Module"` list (modeler-declared, qualified).
- `getSubclassTables()` (TableMetadata 714-731): filters `!table.getTableType().isModule()` → **modules excluded from is-a enumeration**. So is-a chain loop never auto-writes module tables; C3 adds them.
- Module resolution pattern — `SqlColumnExecutor.validateModuleArrayValues` (459-508): `qualifiedName.split("\\.",2)` → `[schema, table]`; `database.getSchema(s).getTable(t)`. Reuse for write-time resolution.

## 6. Test fixture — TestModuleArrayDiscriminator.java
- `freshSchema(suffix)` parallel-safe schema.
- fixture: `create(table("Root").add(column("id").setPkey()))`; `create(table("Mod").setTableType(MODULE).setInheritNames("Root").add(column("modCol")))`; `Root.add(column("panels").setType(MODULE_ARRAY).setValues(schema+".Mod"))`.
- 15 existing @Test (persist/reload, value validation, insert/update membership, coexist, extends-root, exclude-from-is-a, one-axis, reload). C3 tests append here; reuse `freshSchema` + add a 2nd MODULE table for the "activate 2 modules" case.

## C3 hook (design — LOCKED 2026-06-13; owner steer = GENERALIZE the loop, no parallel helper)
**Minimal new logic = extend the table list + filter the row-batch per table.** Reuse `insertIntoSingleTable`, `copyRecordValuesIntoRows`, PK copy-down unchanged.
- Table list per write: `chain = ancestorChainRootFirst(table)+table` (is-a) THEN append active module subtype tables (root-first; expand each active module to module-ancestors up to excl. root for module-extends-module FK; skip tables already in chain). Empty `getDiscriminatorColumns()` ⇒ list == today ⇒ is-a UNTOUCHED.
- Per `current` table compute `rowsFor(current)`: is-a chain ⇒ ALL rows; module ⇒ rows whose ancestor-expanded active set (union of `row.getStringArray(d)` over discriminators, normalized "schema.Module") contains `current`. Skip if empty.
- **Safe to filter:** PK copy-down (415-423) fires only for AUTO_ID PK = root only; root processed first w/ full list (index-aligned). Modules have no AUTO_ID PK (PK already in row) ⇒ their copy-down no-ops ⇒ subset filtering can't desync.
- Column source: is-a chain ⇒ today's `getLocalStoredColumns(current, updateColumns)`. Module ⇒ `getLocalStoredColumns(M, getInsertColumns(M, providedNames))` (M's OWN metadata + row provided names — passed `updateColumns` is R-leaf's list, lacks module cols).
- DECISION 1 (ONE-GO, revised): validation stays at executeBatch (is-a already one-go over leaf.getColumns() before chain — 333/337-338, NOT a bug). Extend to PER-ROW union `entry.getColumns() ∪ {active modules' columns}` → `applyValidationAndComputed(union, row)` ONCE before chain. Shared graph; inactive modules absent ⇒ natural gate. NOT per-module mid-chain. insertBatch/updateBatch = writing only.
- DECISION 2: insertBatch ⇒ existing SAVE conflict flag; updateBatch generalizes same way with `updateOnConflict=true` (upsert active modules; is-a tables keep pure-UPDATE). DEFER removed-module hard-delete to C6.
- MODULE_ARRAY varchar[] value itself = R's own local column → already written by chain loop; C3 adds only module subtype rows.
