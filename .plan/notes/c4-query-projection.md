# C4 query-projection recon (verified against source 2026-06-13)

Scratch inventory for the C4 backend agent. file:line verified by lead via Read. Terse.

## 1. tableWithInheritanceJoin — SqlQuery.java 880-900
- 882: `result = table.getJooqTable()`.
- 884-888: ANCESTOR is-a INNER JOINs. `for ancestor in table.getAncestorsRootFirst()` → `result.join(ancestor.getJooqTable()).using(ancestor.getPrimaryKeyFields())`. Root-first post-order (TableMetadata.getAncestorsRootFirst 753-768).
- 890-897: SUBCLASS is-a LEFT JOINs. `for subclassTable in table.getSubclassTables()` → `using = subclassTable.getPrimaryKeyFields()`; **892-894: adds mg_tableclass to USING ONLY IF `subclassTable.getLocalColumn(MG_TABLECLASS) != null`** → under root-only mg_tableclass model this is **DEAD** (subclasses don't own mg_tableclass) ⇒ join is **PK-only**. `result.leftJoin(...).using(PK)`.
- Single combined result aliased ONCE at 145: `.from(tableWithInheritanceJoin(table).as(alias(tableAlias)))`.

## 2. Projection — rowSelectFields SqlQuery.java 194-243
- Iterates `selection.getSubselect()` (the resolved select list), per col `getColumnByName(table, name)` (199).
- file (200-219), ref/refArray (220-225 → reads ref names from tableAlias), refback (226-237 → reads from `tableAlias + "-refbackjoin-" + col`), else scalar (238-239).
- **239 scalar: `field(name(alias(tableAlias), column.getName()), column.getJooqType())` — RAW, NO CASE WHEN.** is-a subclass cols gated PURELY by LEFT-JOIN row presence. Confirms: **today is-a uses NO membership predicate.**

## 3. Default-select population — SqlQuery.java 96-124
- When no explicit columns: iterates **`table.getColumns()`** (98) = own + inherited (UP) only. **is-a SUBCLASS (DOWN) columns are NOT in the default select.** To read a subclass col you must select it BY NAME.

## 4. By-name resolution — getColumnByName SqlQuery.java 1715-1752
- 1722: `table.getColumnByNameIncludingSubclasses(columnName)`; if null tries ref (1725) / file (1736) over `getColumnsIncludingSubclasses()`; else throws (1748).
- `getColumnByNameIncludingSubclasses` (TableMetadata 700) → `getColumnsIncludingSubclasses` (680) = `getColumns()` + `getColumnsFromSubclasses()` (692).
- `getColumnsFromSubclasses` (692-698) iterates `getSubclassTables()` (714) → `collectSubclassTablesDeduped` (722) which **FILTERS `!table.getTableType().isModule()` (725)** ⇒ **module columns NOT resolvable by name on R today; getColumnByName THROWS for a module col.**

## 5. Column surface
- `getColumns()` (132-): inherited(UP)+local deduped. Root R = R's own cols.
- `getColumnsIncludingSubclasses()` (680): + is-a subclass local cols (NO modules).
- `getDiscriminatorColumns()` (771): `getColumns().filter(isDiscriminator)` = MODULE_ARRAY cols on R. `Column.isDiscriminator()` == MODULE_ARRAY. `Column.getValues()` = allowed "schema.Module" qualified list.

## 6. Java query API for a C4 test
- `Table t = schema.getTable("Root"); t.query().select(s("id"), s("modCol")).retrieveRows()` (or `t.select(...)`). retrieveRows = SqlQuery 78-170. Module col must (a) resolve in getColumnByName and (b) have a joined alias providing the value.

## 7. Linchpin is-a tests (must stay green UNCHANGED)
- TestDiamondInheritance: insertAndSelectRoundtripThroughDiamond, diamondSurvivesSchemaReload, mgTableclassLivesOnlyOnRoot, diamondChildHasSingleRootPrimaryKey, diamondChildHasForeignKeyPerParent (13 @Test).
- TestInherits (2 @Test). Both query LEAF (gets all inherited via ancestor INNER JOINs) + root.

## 8. Other axis-needing sites (Section F) — DEFER past C4
- tableWithInheritanceJoin callers: 145 (base), 423/430 (filter), 831/843/854 (ref/refback subq), 923/978 (ref joins).
- whereCondition / whereConditionSearch iterate getAncestorsRootFirst (search over ancestors) — module FILTER would need array predicate. ORDER BY (limitOffsetOrderBy) no subclass iteration. **C4 = read-projection only; module filter/order = later phase.**

---

## C4 DESIGN (proposed — pending owner OK)

### CRITICAL FINDING — owner's "scalar predicate = 'schema.Sub'" breaks diamonds
The owner's stated model = "is-a scalar axis, predicate `mg_tableclass = 'schema.Sub'`; module array axis, predicate `'schema.Mod' = ANY(arr)`; only predicate differs." Taken LITERALLY (per-subtype scalar equality CASE WHEN) it **BREAKS diamonds**: a D-row has `mg_tableclass='schema.D'` but data-bearing rows in intermediate parents B,C; gating bCol by `mg_tableclass='schema.B'` → NULL (wrong; today non-null). Current is-a uses **NO** CASE WHEN (raw projection, §2) gated by **row presence** — that is what makes diamonds correct. Reproducing is-a EXACTLY ⇒ must NOT add a scalar CASE WHEN to is-a cols.

### Resolution (recommended): nullable per-axis membership predicate
- **is-a (scalar) axis:** predicate ABSENT → raw projection (row-presence gating) → byte-identical to today → TestDiamondInheritance/TestInherits green by construction.
- **module (array) axis:** predicate `'schema.Mod' = ANY(<arrayCol>)` → `CASE WHEN <pred> THEN alias.col ELSE NULL`.
- Join LOOP unified (LEFT JOIN each axis-binding subtype on shared root PK); only the nullable predicate differs. CASE WHEN materializes only where a predicate exists.
- Why modules NEED the CASE WHEN (not just row presence): C3 upserts on activate but does NOT hard-delete on deactivate until C6 ⇒ a deactivated module's row is STALE-present; `= ANY(arr)` makes the CURRENT array value the source of truth (correct independent of C6; the spec contract).

### Components
1. **DiscriminatorAxis (query-side, NEW in SqlQuery; the construct that EARNS its place here per plan).** Carries (subtypeTable, nullable membership predicate bits). Bindings: is-a = `getSubclassTables()` (null predicate); composition = for each MODULE_ARRAY `d` in `getDiscriminatorColumns()`, each qualified value in `d.getValues()` → resolve module table + predicate `'<val>' = ANY(field(d))`. Built once; drives BOTH join + projection.
2. **tableWithInheritanceJoin:** keep ancestor INNER JOINs; loop axes' subtype tables LEFT JOIN on root PK (is-a branch unchanged; append module tables; dedup).
3. **rowSelectFields:** if a projected column is owned by a MODULE subtype (column.getTable() isModule + extends R), wrap in CASE WHEN `'<qualified>' = ANY(field(alias(tableAlias), d.getName()))`; else raw (unchanged).
4. **Column surface (KEY open question ANSWERED):** module cols unreachable by name today. Minimal surgical fix = NEW TableMetadata method (e.g. `getColumnsIncludingActiveModules()`) = `getColumnsIncludingSubclasses()` ∪ module-subtype local cols, used ONLY in SqlQuery getColumnByName resolution. Do NOT broaden global getColumnsIncludingSubclasses (keeps GraphQL/Phase E surface untouched). DEFER implicit/default-select inclusion to Phase E (consistent with is-a: subclass cols also need explicit by-name select). C4 = module cols SELECTABLE BY NAME + gated.
5. Other join/where/order sites = DEFER (C4 read-projection only).

### C4 boundary (confirm w/ owner)
SQL-level downward projection of active module cols, selectable BY NAME at Java retrieveRows/select. GraphQL field exposure = Phase E. Implicit/default-select inclusion = deferred.

### Test plan (red-green, backend-test-purity, append to TestModuleArrayDiscriminator)
- queryRootProjectsActiveModuleColumns: activate Mod1+Mod2 → select(id, modCol1, modCol2) → both present, correct values, shared PK.
- queryRootNullsInactiveModuleColumn: activate Mod1 only → modCol1 present, modCol2 NULL.
- (optional gating-robustness) batch w/ row A=Mod1, row B=Mod2 → each projects only its own.
- is-a unchanged: TestDiamondInheritance (13) + TestInherits (2) green UNCHANGED (run, don't modify).
- Observe via SqlQuery/Java retrieveRows (no raw SQL).
