# Plan: Phase 2 Review Corrections — Round 2

**Spec**: `.plan/specs/yaml-profile-format-v8.md`

## Design Principles (refined)

1. **Profile column always on root table.** `getProfileColumn()` should walk to root and check there. A profile column on a non-root table is invalid.
2. **`hasProfileColumnInAncestors()` is unnecessary.** If profile columns must be on root, then `getRootTable().getProfileColumn() != null` is the check. Remove `hasProfileColumnInAncestors()`.
3. **`isBlock()` and `getIncludedBlockTables()` removed from TableMetadata.** Backend never branches on block status. `TableType.BLOCK` stays in the enum and in `table_metadata` (UX marker), but no methods in `TableMetadata` expose it.
4. **One inheritance style in code.** No `subtableStyle` boolean — every extends is the same FK mechanism. The only variable is whether `mg_tableclass` is added (when root has no profile column).
5. **Unified row management.** Both old-style (`mg_tableclass`) and profile-based subclass rows use the same `insertBatch` recursive mechanism. No separate `handleSubtableRows` with manual JOOQ upsert.

---

## Changes

### 1. TableMetadata.java — cleanup and move methods here

**a) `getProfileColumn()` — check from root**
Current: checks `getLocalColumns()` on `this`.
Change: walk to root first: `getRootTable().getLocalColumns()` scan.
Add validation: if a non-root table has a PROFILE/PROFILES column in its local columns, throw error during `add(column)`.

**b) Remove `hasProfileColumnInAncestors()`**
All callers replace with `getRootTable().getProfileColumn() != null`.

**c) Remove `isBlock()` and `getIncludedBlockTables()`**
Remove both methods. Callers:
- `SqlTable.handleSubtableRows` line 404: will be eliminated (see §5)
- `SqlTable.handleSubtableRows` line 427: will be eliminated (see §5)
- `SqlQuery.collectAncestorSearchableTableNames` line 1637: rewrite (see §4d)
- `ProfileMetadataTest`: update tests — remove `isBlock()` assertions, remove `getIncludedBlockTables()` assertions. `TableType.BLOCK` still exists, test via `getTableType() == TableType.BLOCK` if needed.

**d) Move `existsInAnyParent()` here** (from SqlTableMetadata)
Rename to `hasColumnInParent(String columnName)`. Instance method on TableMetadata. Checks `getInheritedTables()` (direct parents only — sufficient for column overlap detection).

**e) Keep `getAllInheritedTables()`** — already added in round 1.

**f) Add `getAllSubclassTables()`** (recursive, all descendants)
`getSubclassTables()` already does this (lines 723-737 — it recurses). Rename it to `getAllSubclassTables()` if desired, or just use it as-is. Actually, current `getSubclassTables()` IS already recursive. So we just use `getRootTable().getSubclassTables()` to get all tables in the hierarchy.

---

### 2. SqlTableMetadata.java — simplify `setInheritNames`

**a) Remove `subtableStyle` variable** (line 387)
Replace:
```java
boolean subtableStyle = false;
for (String parentName : otherTable) { ... if (p.hasProfileColumnInAncestors()) { subtableStyle = true; break; } }
final boolean isSubtableStyle = subtableStyle;
```
With: resolve any parent, check `parent.getRootTable().getProfileColumn() != null`. Derive `addMgTableclass` directly as `rootHasProfileColumn ? false : true` → simplify to `!rootHasProfileColumn`.

**b) Remove `existsInAnyParent()`** — now on TableMetadata as `hasColumnInParent()`.
Update callers at lines 53, 190, 199 to call `this.hasColumnInParent(...)` (inherited from TableMetadata).

**c) Fix error message** — already done in round 1, verify still correct.

---

### 3. SqlTableMetadataExecutor.java — fix `executeSetInherit`

**a) Remove `hasColumnInAnyParent()`** static method (lines 189-194).
Callers at lines 79, 92 → call `table.hasColumnInParent(column.getName())` (instance method on TableMetadata).

**b) Fix `executeSetInherit` line 163-164**: `copyTm.setInheritNames(firstParent.getTableName())`
Should set ALL parent names: `copyTm.setInheritNames(parents.stream().map(TableMetadata::getTableName).toArray(String[]::new))`.

**c) Fix line 180**: mg_tableclass on ALL parent tables in old-style
Currently only adds to `firstParent`. For old-style single-parent (validated at line 153), there IS only one parent, so this is actually correct. But make it explicit by using `parents.get(0)` consistently and add comment that old-style is always single-parent (validated above).

Wait — re-reading the user's concern: "line 180, should apply to all, unless there is profile column." In old-style (addMgTableclass=true), we already validate single parent at line 153. So `firstParent` == only parent. This is correct. But the mg_tableclass column should be added to the ROOT of the inheritance tree, not just the direct parent. Current code adds it to `firstParent` — if that's not root, it's wrong.

**Fix**: Add mg_tableclass to `parents.get(0).getRootTable()` instead of just `parents.get(0)`. This ensures mg_tableclass is always on the root, even in deep inheritance chains.

**d) Line 164**: PK fields — should handle multi-parent
Currently copies PK fields from `firstParent` only. For profile-style multi-parent, all parents must share the same root PK (they all extend the same root). So copying from ANY parent's PK is equivalent. This is correct but worth asserting: validate all parents share the same PK fields.

---

### 4. SqlQuery.java — simplify using root-based traversal

**a) `addParentSearchConditions` (line 586)**: Replace recursion with loop
```java
for (TableMetadata ancestor : table.getAllInheritedTables()) {
  search.add(field(...searchColumnName(ancestor.getTableName())...).likeIgnoreCase(...));
}
```
No recursion needed.

**b) `tableWithInheritanceJoin` (line 920)**: Rewrite using root
Instead of separate `joinAllParentsRecursive` (up) + `getSubclassTables` (down), get ALL tables from root:
```java
TableMetadata root = table.getRootTable();
List<TableMetadata> allTables = root.getSubclassTables(); // all descendants
// join root + all descendants
```
Join logic: INNER JOIN for ancestors of `table`, LEFT JOIN for other tables (siblings, subclasses).

Actually simpler: for parents use INNER JOIN (must have row), for children/siblings use LEFT JOIN (may not have row).

```java
Set<String> ancestors = table.getAllInheritedTables().stream()
    .map(TableMetadata::getTableName).collect(toSet());
Table result = root.getJooqTable();
for (TableMetadata t : root.getSubclassTables()) {
  List<Field<?>> using = t.getPrimaryKeyFields();
  Column mgTableclass = t.getLocalColumn(MG_TABLECLASS);
  if (mgTableclass != null) using.add(mgTableclass.getJooqField());
  if (ancestors.contains(t.getTableName())) {
    result = result.join(t.getJooqTable()).using(using...);  // INNER
  } else {
    result = result.leftJoin(t.getJooqTable()).using(using...);  // LEFT
  }
}
```
This eliminates `joinAllParentsRecursive`.

**c) `collectAncestorSearchableTableNames` (line 1631)**: Simplify
Replace with: `getRootTable().getSubclassTables()` + root itself → all table names in hierarchy.
No need for `isBlock()` check — all tables in hierarchy contribute search columns.

---

### 5. SqlTable.java — unify row management

**Current state:**
- Old-style (`mg_tableclass`): `executeTransaction` buckets rows by `MG_TABLECLASS` into `subclassRows` map → calls `executeBatch()` on each subclass table → `insertBatch` recurses into parents.
- Profile-style: After main insert, `handleSubtableRows()` does manual JOOQ upsert into child subtable/block tables.

**Problem:** Two completely different code paths for the same concept (managing rows across an inheritance hierarchy).

**Proposed unification:**
Both styles should use the `insertBatch` recursive mechanism. The key insight: `insertBatch` already handles multi-parent by recursing into ALL parents with upsert. It uses `getLocalStoredColumns()` to only insert columns that belong to each table.

**For profile-based tables:**
1. When inserting into parent table with a profile column, determine target subtable(s) from the profile column value
2. For each target subtable, call `insertBatch` on the subtable with the row → it recursively inserts into the subtable's parents (blocks, parent) via upsert
3. The parent row is created via the recursive upsert (same as old-style parents)
4. Delete subtable rows that are no longer in the profile selection (for UPDATE)

**Changes:**
- Remove `handleSubtableRows()` and `insertChildSubtableRow()`
- In `executeTransaction`, after determining target table (from mg_tableclass OR profile column), route all rows through `executeBatch` → `insertBatch`
- For profile-based: the "target table" is the subtable. `insertBatch` handles parent insertion.
- For old-style: same as today — target table is the subclass from mg_tableclass.

**Insert/Update flow (root-first, close to master):**
```
Row arrives at parent table with profile="WGS"
→ collect allParents of WGS: [Experiments (root), sampling (block), WGS (self)]
  (root is first — getAllInheritedTables() returns root-first order)
→ for each table in allParents order: insert/upsert local columns
  - Experiments (root): insert PK + shared columns
  - sampling (block): upsert PK + sampling columns
  - WGS (self): upsert PK + WGS-specific columns
→ need dedup guard: if diamond inheritance, same parent may appear twice in allParents — use Set<String> visited
```

**Delete flow (reverse order, child-first):**
```
→ collect allParents of WGS in reverse: [WGS, sampling, Experiments]
→ for each: delete row by PK
→ CASCADE handles most of this, but explicit delete ensures clean removal
```

**Key**: stick close to master's `insertBatch` pattern. The main change vs master is handling a LIST of parents (multi-parent) instead of single parent, plus dedup for diamond inheritance.

---

### 6. Test updates

- `ProfileMetadataTest.java`: Remove `isBlock()` and `getIncludedBlockTables()` test assertions. Keep `TableType.BLOCK` check via `getTableType()`.
- All existing tests (`TestSubtables`, `TestInherits`, full SQL suite) must still pass.
- No new tests needed — this is refactoring, not behavior change.

---

## Execution Order

1. TableMetadata changes (§1) — foundation, no callers break yet
2. SqlTableMetadata changes (§2) — uses new TableMetadata methods
3. SqlTableMetadataExecutor changes (§3) — uses new TableMetadata methods
4. SqlQuery changes (§4) — independent simplification
5. SqlTable unification (§5) — biggest change, depends on §1
6. Test updates (§6) — after all code changes
7. Run full test suite

## Resolved Questions

1. **Profile column on root only**: Yes — throw error if PROFILE column added to non-root table.
2. **Insertion order**: Root-first (same as master). Collect allParents (root first), insert/upsert each in order. Use visited set to prevent duplicate inserts in diamond inheritance. Delete is reverse order (child-first).
3. **`getSubclassTables()` rename**: No — keep as-is, user will handle later.
