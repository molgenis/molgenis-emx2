# Plan: Unified Discriminator-Driven Composition (Diamond Inheritance + Modules)

## Context

EMX2 today supports only **single, linear table inheritance** (`extends`): a table has one
`TableMetadata.inheritName`, enforced single (`SqlTableMetadata.setInheritName()` throws on a
second parent). Storage is table-per-subclass on a shared root primary key, with a single-valued
`mg_tableclass` discriminator naming the one concrete type of each row.

This is too rigid for real models. Concretely, the `data/patient_registry_demo/molgenis.csv`
`subject` table fakes "modular column groups" with ~1,000 `visible=` expressions and 118 unique
predicates (e.g. `subgroups01?.name=="cs"` repeated across 155 columns). Data modelers need:

1. **Diamond inheritance** — a subtype with multiple parents, constrained to a single common
   root so every branch shares one PK to join on. One concrete type per row.
2. **Multiple types per row** — a row asserts several types/modules at once (e.g. a clinical
   observation that is simultaneously a Diabetes panel and a Renal panel), each contributing a
   column group.
3. **Reusable, composable modules** — define a column group once and reuse it across hierarchies,
   with modules able to extend other modules. Replace the verbose `visible=` workaround.

**Prior art** that shaped the design: C++ virtual inheritance (single shared base → the
single-root invariant), FHIR `meta.profile[]` (an instance conforming to several profiles at
once), openEHR archetypes-vs-templates (reusable maximal models composed/constrained per use
case), and RDF multiple `rdf:type` (an entity is a member of several class-sets).

### Target model (the unifying idea)

There is **one mechanism: a *discriminator column* that declares which types it allows and
steers column-group composition.** Inheritance becomes a special case.

- A **discriminator column** has two new column types:
  - **`SUBCLASS`** (single-valued) → **EXCLUSIVE**: a row is exactly one type (classic + diamond inheritance).
  - **`SUBCLASS_ARRAY`** (multi-valued) → **MULTIPLE**: a row activates several types/modules.
  - The column **cardinality replaces a separate flag** — it alone decides exclusive vs multiple.
- The column's **value(s) are schema-qualified `"schema.Table"` references** to subtype tables
  (same convention as today's `mg_tableclass`); selecting a value composes that table's columns
  onto the row.
- **Allowed types**: reuse single `refTable`/`refSchema` to name the **base** whose subtypes are
  allowed (common case, cross-schema works as today). In a later iteration, a new `allowedSubclasses[]` String[] field
  holds an explicit, schema-qualified subset to modules — needed for subsets and for **multiple discriminator
  columns on one table** (orthogonal modularity axes).
- **Storage is unchanged in spirit**: every subtype/module is a table keyed on the single root
  PK (table-per-type). The single-root invariant guarantees all branches join on one key.
- **`extends` desugars** into a built-in single-valued `SUBCLASS` discriminator (`mg_tableclass`)
  whose allowed types are the subclasses. **All existing schemas keep working with zero
  migration**; the old engine behavior is reproduced on the new one.
- **Top-level vs option-only** is indicated by `tableType`:
  - `extends`/`DATA` subtype → **top-level class** (standalone queryable table, as today).
  - **`tableType = MODULE`** → **option-only**, reusable, **materialized per host** (a distinct
    physical table per hierarchy that uses it). Modules may extend other modules.
- **Collisions**: any duplicate column name across parents/modules is a **validation error**
  (no merge, no override).
- **Nesting is out of scope**: each discriminator maps values→types independently (flat).
  Conditional/nested selectors stay in user-space `visible=` expressions for now.

## Decisions (locked with product owner)

| # | Decision |
|---|----------|
| 1 | Build diamond inheritance AND multiple-types-per-row as one unified discriminator engine |
| 2 | New column types `SUBCLASS` (exclusive) / `SUBCLASS_ARRAY` (multiple); cardinality drives behavior. They are the **enum family**, NOT ref. |
| 3 | Discriminator is **enum-like**, not a ref: a string column whose chosen value(s) come from an explicit `values: List<String>` metadata list. For discriminators the values are schema-qualified `"schema.Table"` names that must overlap existing table definitions (validated to exist). |
| 4 | One general `values` String[] field replaces the planned `allowedSubclasses[]` AND the `refTable`-as-base auto-expansion: `values` IS the allowed set. `extends` auto-derives its `values` from its subclasses for zero-migration back-compat. |
| 4b | Also introduce a generic `ENUM` / `ENUM_ARRAY` column type now (long-requested): string + `values` list. `SUBCLASS`/`SUBCLASS_ARRAY` are the specialization whose `values` must be existing table defs. |
| 5 | `extends` desugars to a built-in single `SUBCLASS` discriminator (`mg_tableclass`); back-compat, zero migration |
| 6 | `tableType=MODULE` = option-only, reusable, materialized per host; `extends`/DATA = top-level class |
| 7 | Modules can extend modules (column flattening at materialization) |
| 8 | Multiple discriminator columns per table allowed (orthogonal axes); flat value→type binding only |
| 9 | Single common root invariant for any inheritance DAG (shared PK); cycle detection |
| 10 | Duplicate column name across parents/modules = validation error |
| 11 | `mg_tableclass` scalar when EXCLUSIVE, array when MULTIPLE (hybrid; minimal migration) |
| 12 | Cross-discriminator nesting OUT of scope (stays in `visible=`) |

## Locked execution decisions (2026-06-10, with product owner)

- **Run scope**: execute Phase A + Phase B autonomously, then checkpoint for review.
- **Persistence (FINAL design — supersedes the `table_inherits_array` plan below)**: `table_inherits`
  itself was refactored to a single `VARCHAR[]` column holding the full ordered parent list (owner
  preferred this over a separate `table_inherits_array` + scalar fallback). `migration32.sql` does
  `ALTER COLUMN table_inherits TYPE VARCHAR[] USING ARRAY[table_inherits]`. `MetadataUtils` reads/writes
  the array only (no fallback); `SqlColumnExecutor` + collision CTEs use `= ANY(table_inherits)`.
  `getInheritName()` = `inheritNames.get(0)`. The text below mentioning `table_inherits_array` is the
  earlier (now-replaced) additive approach, kept for history.
- **Phase B tests**: synthetic `A` root ← `B`, `A` ← `C`, `D extends B,C` only. Defer the
  patient_registry real-model smoke to a later phase.
- DB reset capability confirmed (psql Postgres.app v15 @ localhost:5432, molgenis/molgenis).
- **Discriminator modeling (revised, supersedes decisions #3/#4 above)**: `SUBCLASS`/`SUBCLASS_ARRAY`
  are **enum-like, not ref**. Introduce generic `ENUM`/`ENUM_ARRAY` column types now; both families
  carry a new `values: List<String>` metadata field on `Column` (initially plain strings). For a
  discriminator the `values` are schema-qualified `schema.Table` names validated to exist; this single
  field replaces `allowedSubclasses[]` and `refTable`-as-base. `extends` auto-derives its `values`
  from its subclasses (back-compat). DONE: enum re-model landed green — `SUBCLASS(ENUM)`,
  `SUBCLASS_ARRAY(ENUM_ARRAY)`, generic `ENUM`/`ENUM_ARRAY`, and `Column.values` (List<String>,
  mirrors `semantics`, in-memory only so far).

  **Phase B persistence checklist (explicit, per owner — do NOT forget):** `values` persistence
  rides the Phase B migration alongside `table_inherits_array` — ONE DB-version bump, not two.
  - Migration `migrationNN.sql`: `column_metadata ADD COLUMN IF NOT EXISTS "values" VARCHAR[]` and
    `table_metadata ADD COLUMN IF NOT EXISTS table_inherits_array VARCHAR[]` + backfill
    `table_inherits_array = ARRAY[table_inherits]` where set. Bump `SOFTWARE_DATABASE_VERSION`.
  - `MetadataUtils.java`: add `VALUES` field to the COLUMN_METADATA definition in `init()`; read it
    in the column-record→Column mapping; write it in column save — mirror the `semantics` String[]
    machinery exactly. Same for `table_inherits_array` in table metadata save/load (keep scalar
    `table_inherits` = primary parent for back-compat fallback read).
  - Verify `./gradlew cleandb` works BOTH on a fresh DB AND upgrading a pre-migration DB (exercise
    the migration path, not only fresh create).

### Phase A + B status: DONE & green (reviewed twice)
- Phase A (DAG model), enum re-model (`ENUM`/`ENUM_ARRAY`, `SUBCLASS→ENUM→STRING`, `Column.values`),
  B1 (migration32 → DB v33: `table_inherits` collapsed to `VARCHAR[]` + `column_metadata."values"`,
  cleandb dual-path), B2 (multi-parent DDL/write/query) all landed and staged.
- Late refactors (owner review): multi-parent API is `setInheritNames(String...)` only (`addInheritName`
  removed) with validate-once-before-DDL (partial-state fix); `table_inherits` is a single `VARCHAR[]`
  (no separate array column / fallback); `findConflictingParentByName` returns `Optional<TableMetadata>`;
  `isEnum()` hybrid (direct ENUM/ENUM_ARRAY + base-type for SUBCLASS family). `SqlQuery` still keys on the
  built-in `MG_TABLECLASS` discriminator by convention — custom-named discriminators are Phase C/G.
- Tests: `TestDiamondInheritance` (11), `TestTableMetadataDag` (28), `MetadataPersistenceRoundtripTest`
  (4), all back-compat suites incl. RDF/GraphQL green. spotless+pmd clean.
- Two review passes. First pass caught: C1 mg_tableclass-on-root (was on first parent), C2 false-cycle
  in getAllInheritNames on diamonds, S1 getSubclassTables dup, S2 alterColumn primary-parent-only,
  S3 collision CTE scalar-only (+ TableSort drop-order bug). All FIXED & re-confirmed.

### Helper-method cleanup cycle (post-B2, 2026-06-11) — pure refactors, no behavior change, TestDiamondInheritance green

**Inlined & deleted single-caller helpers:**
- `validateInheritanceDagForAll` and `resolveParentOrThrow` inlined into `SqlTableMetadata.setInheritNames`.
- `getDirectSubclassTables` inlined into `TableMetadata.collectSubclassTablesDeduped`.

**`SqlTable` simplifications:**
- `insertBatch` collapsed from 3-way branch to a single root-first loop over the full ancestor chain (ancestors + self); `isRoot = (i==0)`.
- `insertIntoSingleTable` kept — it is the per-table INSERT block extracted from the original inline; justified by 3 call sites.
- `updateBatch` minor tidy: `rootTable = allTables.get(0)`.

**Kept (load-bearing for diamond inheritance):** `insertIntoSingleTable`, `findConflictingParentByName`, `bulkAddInheritTransaction`.

**Open items — need product-owner decision, NOT yet applied:**
- `TableMetadata.getDiscriminatorColumns()` is public but has zero production callers (Phase C placeholder) — drop to package-private or remove until Phase C.
- Latent bug: `TableMetadata.resolveParentTable` returns null on a missing parent (silently dropped by the `if (parent != null)` guard in `getInheritedTables`), while the `SqlTableMetadata` path throws — so `getInheritedTables()` can return fewer parents than `getInheritNames()` advertises if a parent table was deleted. Decide: make base version throw, or log.
- Minor: `bulkAddInheritTransaction` uses O(n) `parentsToAdd.indexOf(parentName)`; could use a loop index. Cosmetic.

### Verification cycle (post-cleanup, 2026-06-11)

**`resolveParentTable` throw boundary (IMPLEMENTED & fixed).** Throws
`"Cannot inherit ...: not found (declared parent of '<table>')"` ONLY when
`getSchema() != null && getSchema().getDatabase() != null` — i.e., when attached to a live DB.
Returns null during the loading phase (`getSchema() == null`) and in detached in-memory parses
(`Emx2.fromRowList`, `database == null`) so validation is deferred to migrate-time. This caught a
regression: an over-broad throw broke `TestExtends.importColumnOrderSubclass` (cross-schema extends
parsed by `fromRowList`) — fixed and green.

**Full-suite triage.** Most full-module failures were stale embedded-DB state (over-nested
`{{{{...}}}}` `table_inherits` + whole-class `InitializationError`). After `./gradlew cleandb` all
pass: `MetadataPersistenceRoundtripTest`, `TestDiamondInheritance`, `TestAutoIdGeneration`,
`TestMgColumns`, `TestRefBack`, `TestCrossSchemaForeignKeysAndInheritance`,
`TestCompositeForeignKeys`, `TestInherits`, `TestTruncate`, `TestLegacyImport`.
**Rule for future runs: always run `./gradlew cleandb` before a full-suite run on this branch.**

**Remaining red — RESOLVED (2026-06-11, Option A — local case arms):**
`SUBCLASS`/`SUBCLASS_ARRAY` are now wired into all type-mapping switches. `SUBCLASS.getBaseType()==ENUM`
(one level), so `ENUM`/`ENUM_ARRAY` arms were added alongside `STRING`/`STRING_ARRAY` handling at:
- `TypeUtils.toJooqType`: `VARCHAR(255)` / `[]`
- `TypeUtils.getTypedValue`: `toString` / `toStringArray`
- `TypeUtils.getArrayType`: `SUBCLASS → SUBCLASS_ARRAY` (explicit mapping)
- `SqlTypeUtils.getPsqlType`: `"character varying"` / `"[]"`
- `SqlTypeUtils.getTypedValue`: `row.getString` / `getStringArray`

**Full-suite status after `./gradlew cleandb` (2026-06-11):** `molgenis-emx2` 100/100 pass;
`molgenis-emx2-sql` 329 run / 0 fail / 1 skipped; `molgenis-emx2-io` 79/79 pass. Branch is GREEN
across core / sql / io modules.

### Known gaps to address in later phases (found in B2 review — do NOT lose)
- **Phase E (IO/GraphQL) multi-parent NAME preservation — RESOLVED & regression-tested (2026-06-11).**
  The B2-review note (`SqlSchema.merge` + `json/Table.java` drop the 2nd parent) was STALE: verification
  found commit `facea6808` had ALREADY converted `SqlSchema.merge` (create + alter paths),
  `json/Table.java`, and `json/Schema.java` to the full `getInheritNames()`/`setInheritNames()` list.
  This cycle proved it via red-green (injected the scalar regression → RED, removed it → GREEN) and
  LOCKED it with two regression tests. See "Phase E IO round-trip fix" below.
- **Phase E (IO/GraphQL) `inheritId` scalar truncation — STILL OPEN (found 2026-06-11).** `json/Table.java`
  (~56) sets `inheritId = getInheritedTables().get(0).getIdentifier()` — only the PRIMARY parent's id.
  Parent NAMES round-trip fully (`inheritNames`), but there is no `inheritIds` list, so any consumer
  reading `inheritId` for a diamond gets only parent[0]. Add `inheritIds` (or drop the scalar) in full Phase E.
- **TestMigration:** migration33 was NOT added to the nonparallel `TestMigration` (deferred per
  backend-test-purity guidance on the nonparallel module). Add a migration-upgrade assertion when Phase E
  touches that module.
- **Nits (cosmetic, optional):** migration file is named `migration33.sql` though the `if(version<33)`
  convention elsewhere maps to `migrationN-1.sql`; `SqlTableMetadata.findConflictingParent` is a no-op
  wrapper; `SqlQuery.collectAllSubclassesDeduped` duplicates `getSubclassTables()` (now itself deduped).

### Phase E IO round-trip fix (pulled forward, 2026-06-11) — DONE (verified already-correct + regression-locked)

**Outcome:** the production code was ALREADY multi-parent-correct in `facea6808` — the B2-review note
warning of a scalar drop was stale. Verification (independent review agent) confirmed `SqlSchema.merge`
(create AND alter paths), `json/Table.java`, and `json/Schema.java` all use the full
`getInheritNames()`/`setInheritNames()` list. NO production change was needed; the deliverable is two
regression tests that prove and lock the behavior.

**How verified (red-green):** injected the equivalent scalar regression (set only `inheritName` /
`setInheritNames(List.of(getInheritName()))`) → both new tests went RED for the right reason; removed
the injection → GREEN. Tests:
- `TestDiamondInheritance.mergePreservesAllParentsOfDiamondChild` (sql) — asserts merged `D` keeps BOTH `B`,`C`.
- `TableJsonRoundtripTest.jsonRoundtripPreservesAllParentsOfDiamondChild` (graphql, new file) — JSON model round-trip keeps BOTH parents.

**Suite status:** `molgenis-emx2-sql` 330 pass / 1 skipped / 0 fail. `molgenis-emx2-graphql` 54/54 GREEN
when the module runs alone (verified). The 5 user/JWT/session failures the implementer saw were
pre-existing parallel-fork test-isolation flakiness (shared DB-user/JWT state across the full multi-module
run), NOT caused by the new test — confirmed: the 5 pass in isolation and the new test is in-memory only.

**Still open (carried to full Phase E):** `inheritId` scalar truncation (see Known-gaps above) — names
round-trip, identifiers only carry parent[0].

**Out of scope (stays in full Phase E):** EMX2 CSV column surface for MODULE/SUBCLASS_ARRAY/`values`,
RDF multiple `rdf:type`, `TestMigration` migration33 upgrade assertion.

## Build phasing

Land in order; each phase keeps existing tests green.

- [x] **Phase A — Foundations (model + back-compat refactor).** DONE — DAG model, enum re-model, `SUBCLASS`/`SUBCLASS_ARRAY`/`ENUM`/`ENUM_ARRAY` types, `Column.values`, `getInheritNames()`, `setInheritNames()`, `getRootTable()` single-root invariant, `getAllInheritNames()` cycle-safe BFS, `getColumns()` diamond dedup. All back-compat tests green.
- [x] **Phase B — Diamond inheritance (EXCLUSIVE).** DONE — migration32.sql → DB v33 (`table_inherits` collapsed to `VARCHAR[]`, `column_metadata."values"`), per-parent FK on shared root PK, `mg_tableclass` on root only, ancestor-dedup insert/query, collision validation, `setInheritNames()` API. `TestDiamondInheritance` (11) + `MetadataPersistenceRoundtripTest` (4) + all back-compat green. Two review passes, all issues fixed.
- [x] **Helper-method cleanup cycle.** DONE — single-caller inlines (`validateInheritanceDagForAll`, `resolveParentOrThrow`, `getDirectSubclassTables`), `insertBatch` collapsed to root-first loop, `resolveParentTable` throw boundary scoped to DB-attached contexts.
- [x] **SUBCLASS/SUBCLASS_ARRAY type-mapping wiring.** DONE — local case arms added at `TypeUtils.toJooqType`, `TypeUtils.getTypedValue`, `TypeUtils.getArrayType`, `SqlTypeUtils.getPsqlType`, `SqlTypeUtils.getTypedValue`. Full suite green after `cleandb`.
- [ ] **Phase C — `SUBCLASS_ARRAY` (MULTIPLE) within one hierarchy.** Array discriminator, set activation, per-row column projection, module-aware validation gating.
- [ ] **Phase D — `MODULE` tableType + per-host materialization + module-extends-module.** Cross-hierarchy reuse.
- [ ] **Phase E — IO / GraphQL / JSON / RDF surface.** EMX2 CSV, GraphQL metadata, JSON model; RDF emits multiple `rdf:type` for active modules. **Known gaps (do NOT lose):** `SqlSchema.merge` (~233, ~255-261) and `json/Table.java` (~52-54) still read only the primary parent (`getInheritName`) — diamond round-trip silently drops 2nd parent. `TestMigration` (nonparallel module) not yet extended for migration33 array upgrade.
- [ ] **Phase F — Frontend redesign** (separate effort): subtype/variant picker UI; writing to root with discriminator selection.
- [ ] **Phase G — Multiple discriminator columns per table** (orthogonal axes; `allowedSubclasses[]`).


---

## Phase A — Foundations: metadata model → DAG + discriminator abstraction  [x] DONE (green)

> Status: landed & staged. DAG model + enum re-model done. Validated by `TestTableMetadataDag` (28)
> and all back-compat suites. `SUBCLASS→ENUM→STRING`, `Column.values` mirrors `semantics`.
> Single-extends throw removed in Phase B (not here).

**`backend/molgenis-emx2/src/main/java/org/molgenis/emx2/ColumnType.java`**
- Add `SUBCLASS` and `SUBCLASS_ARRAY` enum constants with their type metadata (base type ref-like;
  array variant). Wire into the type registry / `getBaseType` mappings used across SQL/IO/GraphQL.

**`backend/molgenis-emx2/src/main/java/org/molgenis/emx2/TableType.java`**
- Add `MODULE` constant (alongside `DATA`, `ONTOLOGIES`). Add `isModule()` helper usage points.

**`backend/molgenis-emx2/src/main/java/org/molgenis/emx2/TableMetadata.java`**
- Keep `inheritName` (String) as the **primary parent** shim; add `List<String> inheritNames`.
  Invariant: `inheritName == (inheritNames empty ? null : inheritNames.get(0))`.
  - `getInheritName()` (~399) returns primary; add `getInheritNames()`.
  - `setInheritName()` (~412) = replace-primary (clear+add one); add `addInheritName()` /
    `setInheritNames()`; `removeInherit()` clears list.
  - Sync `inheritNames` in `sync()` (~96) and `clearCache()` (~466); update `toString()`.
- Add `getInheritedTables(): List<TableMetadata>`; keep `getInheritedTable()` returning primary so
  existing call sites compile, then migrate column-aggregating methods to iterate all parents.
- **DAG generalization** (add `visited` cycle guards):
  - `getRootTable()` (~666): walk all ancestors; collect roots (empty `inheritNames`);
    **throw if >1 root** (`MolgenisException`).
  - `getAllInheritNames()` (~403): BFS/DFS over `getInheritedTables()`, dedupe (diamonds).
  - `getSubclassTables()` (~655): `table.getInheritNames().contains(getTableName())`; dedupe.
  - `getColumns()` (~132), `getLocalColumns()` (~263), `getNonInheritedColumns()` (~247),
    `getColumn()`/`getColumnByIdentifier()` (~329): iterate **all** parents; PK copied **once**
    from the (single) root; LinkedHashMap dedups by name.
  - `add()` (~350): collision check loops **all** parents.
- Add `getModules()`/`addModule()`/`setModules()` (host's available modules) — backed by new
  persistence (Phase F/E); follow `inheritNames` pattern.
- Add discriminator accessors: a column flagged `SUBCLASS`/`SUBCLASS_ARRAY` is a discriminator;
  helper `getDiscriminatorColumns()` returns them; default is the built-in `mg_tableclass`.

**`backend/molgenis-emx2-sql/.../SqlTableMetadata.java`**
- `setInheritName()` (~342): **remove** the single-extends throw. Append parent, then run new
  `validateInheritanceDag(this)`: resolve all parents (reuse import-schema lookup ~357), enforce
  single root via `getRootTable()`, detect cycles, run cross-parent collision check.

**Acceptance**: all existing inheritance tests green; `getInheritNames()` size ≤1 in practice;
pure refactor, no DDL change yet.

---

## Phase B — Diamond inheritance (EXCLUSIVE)  [x] DONE (green, reviewed ×2)

> Status: landed & staged. B1 persistence (migration32.sql → DB v33, `table_inherits_array` +
> `column_metadata."values"`, scalar fallback, cleandb dual-path) + B2 runtime (per-parent FK on one
> root PK, `mg_tableclass` on root only, ancestor-set dedup write/query, single-root + collision
> validation, array-aware CTE). Validated by `TestDiamondInheritance` (11) + `MetadataPersistenceRoundtripTest`
> (4). Two review passes; fixes recorded in "Phase A + B status" above. Known Phase E round-trip gap noted.
>
> API (owner decision): multi-parent declared via `setInheritNames(String...)` ONLY — `addInheritName`
> removed. `SqlTableMetadata.setInheritNames(List)` validates the full DAG ONCE before any DDL and
> applies DDL only for not-yet-wired parents (incremental, idempotent), fixing a partial-state bug.
> `setInheritName(String)` kept for single-parent back-compat. Phase E EMX2 import will parse "B,C" →
> setInheritNames(...).

**`backend/molgenis-emx2-sql/.../SqlTableMetadataExecutor.java`**
- `executeSetInherit()` (~141): callable **once per parent**.
  - Add PK fields + KEY1 **only for the first parent / only if not already present** (all diamond
    parents share the same root PK; adding twice errors). Guard on existing PK columns.
  - FK creation (~162) runs **per parent** (constraint name already includes parent name → N FKs
    over the same columns; Postgres permits this).
  - `MG_TABLECLASS` added **only to the single root** (~171).
- `executeCreateTable()` (~60): loop `getInheritedTables()` → `executeSetInherit` per parent.
- Collision validation: generalize `checkNoColumnWithSameNameExistsInSubclass` (~426) and the
  recursive CTE that walks `b.table_inherits` to walk the **array** (`= ANY(...)` / unnest) once
  array storage lands (Phase F migration); for Phase B with scalar storage, validate in-model.

**`backend/molgenis-emx2-sql/.../SqlTable.java`**
- `insertBatch()` (~382)/`updateBatch()` (~461): replace single-parent recursion with insert over
  the **deduped ancestor set** from root downward, each table **once**, copying generated root PK
  down (generalize `copyRecordValuesIntoRows` ~447). Diamonds must not insert the shared root twice.
- `mg_tableclass` population (~222) unchanged (scalar; derived from target table).

**`backend/molgenis-emx2-sql/.../SqlQuery.java`**
- `tableWithInheritanceJoin()` (~882): iterate the **distinct ancestor set** (INNER JOIN each on
  the shared root PK; `mg_tableclass` lives only on root — join non-root ancestors on PK only).
  Subclass LEFT JOINs use DAG-aware deduped `getSubclassTables()`.

**Acceptance / test** (embedded Postgres, follow `backend-test-purity`):
`A` root ← `B`, `A` ← `C`; `D extends B, C`. Verify single PK, dual FK, insert/select roundtrip,
duplicate-column rejection, single-root enforcement (reject a DAG with two roots).

---

## Phase C — `SUBCLASS_ARRAY` (MULTIPLE) within one hierarchy

**`backend/molgenis-emx2/.../Constants.java`** — `mg_tableclass` becomes cardinality-aware; when a
root declares a `SUBCLASS_ARRAY` discriminator, the stored column is `TEXT[]`.

**`SqlTableMetadataExecutor.java`**
- When discriminator is array-typed: create `mg_tableclass` as `STRING_ARRAY` on root; adjust the
  immutability/consistency trigger (`createMgTableClassCannotUpdateCheck` ~180) to validate array
  membership against allowed types rather than scalar equality.

**`SqlTable.java`**
- On write, for each row read the active type set from the discriminator; for each active subtype,
  write a row into that subtype's physical table keyed by the root PK; populate the array
  discriminator from the union of active types. On update, **delete** subtype rows for types removed
  from the set (keep storage consistent with the set).

**`SqlQuery.java`**
- `tableWithInheritanceJoin()`: LEFT JOIN each allowed subtype on root PK; project its columns
  guarded by membership: `CASE WHEN '<schema.Type>' = ANY(mg_tableclass) THEN <t>.<col> ELSE NULL`.
- `rowSelectFields()` (~194): resolve subtype-owned columns to the subtype join alias + membership
  CASE; expose the discriminator as selectable.

**`backend/molgenis-emx2-sql/.../SqlTypeUtils.java`**
- `applyValidationAndComputed()` (~26) / `checkRequired()` (~100): for a column owned by a subtype,
  gate `required`/`visible` on whether that subtype is active for the row (mirrors the existing
  invisible-column clear at ~68). Pass the active type set into the expression `graph` map so
  `visible`/`required` JS can reference it.

**Acceptance / test**: root with `SUBCLASS_ARRAY` discriminator + two subtypes; row activating both
→ both column groups present & validated; row activating one → other group NULL & not required;
deactivation deletes the subtype row.

---

## Phase D — `MODULE` tableType + per-host materialization + module-extends-module

- A **`MODULE`** table is an abstract, reusable definition: no own storage. Modules may
  `extends` other modules → flatten columns at materialization.
- **Materialization**: when a host (root) references a module via a discriminator's allowed types,
  create a host-specific physical table (e.g. `"<Host>.<Module>"`) keyed on the host root PK using
  the same `executeSetInherit` machinery, but **without** `mg_tableclass`/is-a subclass semantics.
  The same module definition reused by another host materializes as a separate table there →
  preserves the single-root-PK join invariant.
- Per-host collision checks reuse the Phase B collision logic.
- Constraint: module columns must be self-contained (no references to host-specific columns) for v1.

**Acceptance / test**: module `VitalsPanel`; module `DiabetesPanel extends VitalsPanel`; two hosts
each materialize `DiabetesPanel`; verify independent physical tables, flattened columns, per-host
PK join, reuse without collision.

---

## Phase E — IO / GraphQL / JSON / RDF + DB migration

**Migration** — `backend/molgenis-emx2-sql/.../Migrations.java` + new `migrationNN.sql`:
- Bump `SOFTWARE_DATABASE_VERSION`. Add `table_metadata.table_inherits_array VARCHAR[]`,
  `table_metadata.table_modules VARCHAR[]`, `column_metadata.allowed_subclasses VARCHAR[]`
  (`ADD COLUMN IF NOT EXISTS`). Backfill `table_inherits_array = ARRAY[table_inherits]` where set.
  No physical table rewrite needed (existing FKs from prior migrations remain valid).

**`MetadataUtils.java`**: extend `init()` create, `saveTableMetadata()` (~350), `recordToTable()`
(~497) to read/write the array fields; keep scalar `table_inherits` = primary parent for back-compat
(fallback read when array null).

**`backend/molgenis-emx2-io/.../emx2/Emx2.java`**: `tableExtends` parses comma/semicolon list →
`addInheritName` per token (output joins `getInheritNames()`); add `tableType=MODULE` handling and
column types `SUBCLASS`/`SUBCLASS_ARRAY`; add `allowedSubclasses` column. Update `getHeaders()`.

**`backend/molgenis-emx2-graphql/.../GraphqlSchemaFieldFactory.java`** + `json/Table.java`/`Schema.java`:
add `inheritNames: [String]`, `modules: [String]`, `allowedSubclasses` to input/output and JSON
model; keep `inheritName`/`inheritId` primary for back-compat; accept `tableType=MODULE`.

**`backend/molgenis-emx2-rdf/.../generators/Emx2RdfGenerator.java`**: `dataRowToRdf()` (~257)
emits the primary `rdf:type` from the (single) is-a class plus an **additional `rdf:type` per
active module** in the row's discriminator set (+ each module table's semantics). FHIR
`meta.profile[]` analog. No change to the per-class iteration `where` clause.

---

## Phase F — UI redesign

...

---

## Phase G — Multiple discriminator columns per table

- A table may declare several `SUBCLASS`/`SUBCLASS_ARRAY` columns (orthogonal axes, e.g.
  `diseaseGroup` and `sampleType`). Each uses `allowedSubclasses[]` (schema-qualified) to scope its
  own type set; flat binding (no nesting). Write/query/validation generalize to iterate all
  discriminator columns.

**Persistence** — `backend/molgenis-emx2-sql/.../MetadataUtils.java`:
- Add `COLUMN_METADATA` field `allowed_subclasses VARCHAR[]` (copy String[] machinery from
  `semantics`/`profiles`). `refTable`/`refSchema` reused for the base case.

---

## Critical files

- `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/TableMetadata.java`
- `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/ColumnType.java`
- `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/TableType.java`
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTableMetadataExecutor.java`
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTableMetadata.java`
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTable.java`
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlQuery.java`
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTypeUtils.java`
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/MetadataUtils.java`
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/Migrations.java`
- `backend/molgenis-emx2-io/.../emx2/Emx2.java`
- `backend/molgenis-emx2-graphql/.../GraphqlSchemaFieldFactory.java`, `json/Table.java`, `json/Schema.java`
- `backend/molgenis-emx2-rdf/.../generators/Emx2RdfGenerator.java`

## Riskiest parts (enforce / test hardest)

1. **Root-PK-copy-once vs FK-per-parent** in `executeSetInherit`/`insertBatch` — inserting the
   shared root twice or duplicating PK columns. Drive all DDL/writes off the **deduped ancestor
   set**; copy PK from the single root.
2. **`getColumns`/`getLocalColumns` diamond dedup** — a column reachable via two parents appears
   once with stable position.
3. **Single-root invariant** enforced at `setInheritName` (validate), `getRootTable` (throw on >1
   root), and `executeSetInherit` (DDL guard). The linchpin.
4. **Recursive collision CTE** rewritten for array `table_inherits_array` or it silently misses
   diamond/module collisions.
5. **Sparse module projection** — wrong join key (a secondary path instead of the single root PK)
   silently returns wrong rows.
6. **`mg_tableclass` scalar↔array** dual representation — query/filter call sites must branch on
   the discriminator cardinality.

## Verification (end-to-end)

- **Unit/integration (per phase)**: `./gradlew :backend:molgenis-emx2-sql:test` (and `-io`,
  `-graphql`, `-rdf`) using embedded Postgres; new tests per phase Acceptance blocks above. Follow
  `backend-test-purity`, `graphql-test-pattern`, `backend-test-runner` (surgical invocation, reset
  DB between suites only when needed). Red-green: write the failing diamond/module test first.
- **Back-compat**: existing `TestInherits`, `TestExtends`, catalogue model load tests must stay
  green unchanged → proves `extends` desugaring preserves behavior.
- **Migration**: `TestMigration` verifies an old DB upgrades (array columns added, backfilled).
- **Roundtrip**: import an EMX2 CSV using `tableType=MODULE` + `SUBCLASS_ARRAY` discriminator,
  insert rows activating multiple modules, query via GraphQL (per-row module columns present only
  when active), export CSV (stable), emit RDF (multiple `rdf:type`).
- **Real-model smoke**: prototype migrating one `subject` selector axis (e.g. `subgroups01`'s 5
  disease groups) from `visible=` expressions to modules; confirm column-count/verbosity drop and
  equivalent form behavior.
- **Frontend (Phase G, separate)**: subtype/variant picker; verify across themes/sizes per
  `frontend-conventions`.

## Out of scope (this plan)

- Cross-discriminator nesting (stays in user-space `visible=`).
- A subtype being simultaneously top-level AND a reusable option.
- Module columns referencing host-specific columns.
- Hard removal of the `extends` syntax (kept as back-compat sugar; deprecate later if desired).
- Full frontend redesign beyond Phase G outline.
