# Plan: Unified Discriminator-Driven Composition (Diamond Inheritance + Modules)

> **COMMIT STATUS (2026-06-14):** all phases A,B,C0–C6,E0–E8,F0–F2/F2b are **COMMITTED** on this branch. The per-section "STAGED, NOT committed" labels below are HISTORICAL (accurate when written, kept for the staging timeline) — do not read them as current. NEXT = Phase H (scalar MODULE + experiment demo), then F3/F5/F6.

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
- [x] **Phase C (REVISED 2026-06-12) — Unified column-based discriminator engine (MULTIPLE). COMPLETE: C0–C4 + C6 DONE & green & reviewed; C5 SUBSUMED BY C3 (locked by test). All STAGED, not committed.** ABSORBS old Phase D (MODULE materialization) + Phase G (multiple discriminator columns) per locked owner decisions. The discriminator is a first-class COLUMN; `SUBCLASS_ARRAY` REPLACES the scalar `mg_tableclass`; a table may have several discriminator columns; values may reference MODULE classes OR TABLE subclasses (both stored table-per-type on root PK); deactivation HARD-DELETEs the subtype/module row. Staged C0–C6 (see "Phase C REVISED" section below). Retires decision #11 (`mg_tableclass`-becomes-array). **Progress:** C0 (abstraction+rename), C1 (MODULE_ARRAY DDL+value validation), C2 (MODULE = real subtype table, Model B), C3 (MULTIPLE write routing), C4 (MULTIPLE query projection — row-presence gating) all DONE & green & reviewed. C5 (visible/required gating) SUBSUMED BY C3's per-row validation union + locked by `requiredModuleColumnEnforcedOnlyWhenModuleActive`. C6 (hard-delete on deactivation) DONE & green & reviewed (S1/S2/N3 follow-ups applied). **Phase C COMPLETE.**
- [ ] **~~Phase D — `MODULE` tableType + per-host materialization~~** — ABSORBED into Phase C (sub-stage C6).
- [ ] **Phase E — IO / GraphQL / JSON / RDF surface.** EMX2 CSV, GraphQL metadata, JSON model; RDF emits multiple `rdf:type` for active modules. **Known gaps (do NOT lose):** `inheritId` scalar truncation in `json/Table.java` (~56) — names round-trip, ids carry parent[0] only; `TestMigration` (nonparallel module) not yet extended for migration33 array upgrade. (The `SqlSchema.merge` / `json` multi-parent NAME gap was found already-fixed + regression-tested 2026-06-11.) **Single-file / cross-module column-order requirement (owner 2026-06-13, for Phase H):** the EMX2 CSV importer must let a modeler declare a root + its `MODULE` tables + `MODULE_ARRAY` axis columns in ONE model file and control DISPLAY COLUMN ORDER spanning those module tables from that one file (modules are separate physical tables but authored together). Verify whether the current `position`/row-order semantics already give cross-table order control or whether a model-file-global ordering needs adding; this is a prerequisite for Phase H.
- [ ] **Update user-facing docs** (lands WITH Phase E — document the type only once a modeler can declare it via CSV/GraphQL). Targets: `docs/molgenis/use_schema.md` (data modeling — `MODULE_ARRAY` composition column, `MODULE` tableType, multi-parent `extends`), `docs/molgenis/CSV.md` (column-type reference + `values` semantics for ENUM/ENUM_ARRAY/MODULE_ARRAY), `docs/molgenis/semantics.md` (RDF multiple `rdf:type` for active modules). NOT in C1 (no user-declarable surface yet). NOTE: enum-family `values` membership is now ENFORCED (insert+update) — call this out in CSV.md so modelers know ENUM `values` are no longer advisory-only.
- [ ] **Merge-on-partial-update (PATCH semantics) — NEW, separate feature (owner agreed 2026-06-13: do NOT fold into C6). Homed alongside Phase E (CSV/IO is the motivating case).** Today's write path has NO retrieve-merge: `applyValidationAndComputed` runs the JS graph over the row AS PROVIDED, never a merged row (verified — the only `.fetch()` in the write path are the insert PK-return at `SqlTable:636` and C6's narrow prior-discriminator read at `:726`). The C6 prior-read IS the seed primitive (select-by-PK in-tx) but is deliberately narrow (discriminator arrays only, for the diff) and is NOT fed into expression eval. Goal: on update/upsert, **retrieve the full existing row → overlay provided columns → run computed/`visible`/`required` over the MERGED row → then write.**
  - **Current half-state to unify:** is-a stored columns are *implicitly merge-ish* (`getUpdateColumns` SETs only provided cols → unprovided keep DB value) BUT expressions still run on the partial row (a computed col referencing an unprovided sibling sees null). Module columns are *replace-ish* (C3/C6 upsert NULLs unprovided — the deferred nit below). Merge unifies both.
  - **THE core decision:** absent column = *keep existing* (PATCH/merge) vs *set null* (PUT/replace) — must be explicit + documented, possibly per-mode (CSV-upsert vs API).
  - **Gotchas:** (1) absent-vs-explicit-null — a `Row` only knows "present" via `getColumnNames()`; confirm the CSV importer distinguishes an empty cell (null) from a missing column (absent) — that distinction is the whole feature. (2) Upsert branch — merge applies only when the row exists; insert has nothing to merge → read-then-branch per row. (3) Behavior change — computed cols currently evaluate on partial data; merge changes their outputs for existing partial-update callers. (4) Cost — full-row batch read (incl. C4 module joins) vs C6's 2-column read. (5) cross-row/refback edge cases.
  - **Reuse:** C4's unified is-a+module LEFT-JOIN already reads module columns back, so the full-row read surface exists. **Payoff:** a proper merge is the *principled* fix for the deferred module-NULLing nit (scope item 3 / C3 carry) — retrieve+merge preserves unprovided module columns automatically, retiring the `getUpdateColumns`-filter workaround entirely.
- [ ] **Phase F — Frontend / UI surface (separate effort; likely its OWN PR after Phase E — owner 2026-06-13).** The engine (C0–C6) + Phase E GraphQL/metadata surface land first; the UI cannot consume MODULE/MODULE_ARRAY until Phase E exposes it. **Concrete surfaces to update (owner-named — do NOT lose):**
  - **Forms / record editor (`molgenis-components` RowEdit/EditModal + the new `ui` form components) — MINIMUM for usability.** A `MODULE_ARRAY` column renders as a multi-select of allowed MODULE names (a module/variant picker); selecting/deselecting a module shows/hides that module's column group in the form; writes go to the root with the discriminator selection (engine routes to module subtype rows via C3, hard-deletes on deselect via C6). Respect per-module `required`/`visible` (C5 gating). `MODULE` tables themselves are option-only — not standalone form targets.
  - **Table / data explorer (`apps/tables`) — MINIMUM.** Show module columns for rows that activate them (C4 row-presence projection → a module col is NULL/absent for non-activating rows). Decide column display: union of active-module columns vs per-row. Filter/order BY a module column is NOT engine-supported yet (C4 deferred it) — gate the UI accordingly.
  - **Data client / app shell (`apps/ui`).** Detail/record views render active-module column groups; navigation treats MODULE tables as non-top-level (catalog-visible but not primary browse targets).
  - **Schema editor (`apps/schema`).** Author MODULE tables (`tableType=MODULE`, `extends <root>`), declare `MODULE_ARRAY` columns + their `values` (BARE same-schema module names per axis), and the multi-parent `extends` (diamond). Surface the column-type + validation (values must be MODULE extending this root, one-axis-per-module).
  - Carries the original Phase F intent: subtype/variant picker UI; writing to root with discriminator selection. **Owner is fine deferring this to a NEXT PR; table explorer + forms are the at-least set.**
  - **Phase E review carry (N2) — module columns are READ-only via GraphQL today.** Phase E surfaced module columns for READ (query/RDF/CSV) but the GraphQL MUTATION input type uses `getColumnsIncludingSubclassesExcludingHeadings()` (no module content cols) — only the `MODULE_ARRAY` discriminator is writable via mutation. Phase F (forms/record editor) MUST widen the GraphQL row INPUT / mutation field builder (`GraphqlTableFieldFactory` ~:925 / ~:1019) so a module's content columns are WRITABLE when its module is activated (engine C3 already routes the write). This is the write half of the module surface.
- [ ] **~~Phase G — Multiple discriminator columns per table~~** — ABSORBED into Phase C (multi-axis built in from C0).
- [ ] **Phase H — Real-model migration: `subject` `visible=` → modules (the motivating use case).** Replace the ~1,000 `visible=` expressions / 118 unique predicates on the `subject` table in `data/patient_registry_demo/molgenis.csv` (e.g. `subgroups01?.name=="cs"` repeated across 155 columns) with `MODULE_ARRAY` composition columns (the modular axes — `subgroups01`, `panels`, etc.) + `MODULE` tables (one per column-group), so a `subject` row activates the modules it needs and the engine gates their columns (no per-column JS). **Hard requirement (owner 2026-06-13): keep EVERYTHING in ONE `subject.csv` model file** — root `subject` + all its `MODULE` tables + the `MODULE_ARRAY` axis columns declared together, so **column ORDER can be managed across modules** (display order spanning multiple module tables is authored in one place, not scattered per-file). Depends on: C3 (write) + C4 (query projection) + C5 (gating) so module columns show/hide correctly; **Phase E** for the EMX2 CSV surface (MODULE tableType, MODULE_ARRAY column type, `values`) AND for the importer to honor cross-module column order from a single file (see Phase E note). Deliverable: a migrated `subject.csv`, a before/after column-count + verbosity drop, and confirmation of equivalent form behavior (same columns visible per selector state). This is the end-to-end proof the whole mechanism replaces the `visible=` workaround. Owner of execution: `datamodeler` agent once the engine + CSV surface are ready.
  - **Phase E review carry (N1) — RDF + ontology module columns:** `OntologyIriMapper.addDataTable` (molgenis-emx2-rdf, ~:43) uses `getColumns()` (module-free). VERIFY whether the mapper is invoked per-MODULE-table (then fine) or only on the root (then a gap) — subject modules are ONTOLOGY-heavy, so if it's a gap a MODULE's ONTOLOGY column would emit as a string LITERAL instead of an IRI in RDF. If real, widen to `getColumnsIncludingModules()` (same modules-only redirect as E1/E4). Verify before fixing (review's claim unconfirmed).

- [ ] **Naming-convention refactor of the demo model — DEFERRED (owner 2026-06-14).** Owner convention: in most data models, table + column NAMES should be **human-readable Sentence case**, NOT camelCase/PascalCase. The `diamond_showcase` demo (E8) currently uses camel/Pascal (`relevantmedhistory`, `subjectId`, `CockayneSyndrome`, `AdvancedCockayne`, `subgroups01`, `diseaseGroup`) — inherited from the inspiration source. Refactor `data/diamond_showcase/molgenis.csv` (+ its data CSVs) so NAMES read like "Cockayne syndrome", "Relevant med history", "Subject id", "Disease group", etc. **Ripple to handle:** EMX2 derives the `id`/identifier (GraphQL field name) FROM the name, so changing names changes ids → MUST update every reference to the old ids: `DiamondShowcaseTest`, the Phase F e2e/Playwright test, any F1/F2 fixtures, and `DiamondShowcaseLoader`/`DataModels` if it pins names. Apply the same Sentence-case convention to the Phase H `subject.csv` migration from the start. Owner of execution: `datamodeler`. Pure asset/convention change, no engine impact. **Consider also codifying the convention in `.claude/CLAUDE.md` (data-model section)** — pending owner confirmation.
- [ ] **Exclusive single-valued MODULE discriminator (create-then-specialize type flow) — DESIRED FEATURE (owner 2026-06-14).** A single-valued counterpart to `MODULE_ARRAY`: a column where a row picks EXACTLY ONE module/type (cardinality-1; switching to another module hard-deletes the previous module row — exclusive composition). This is the "exclusive" variant that was folded out in Design B (old SUBCLASS semantics), now wanted back with a concrete UX rationale. **Why (owner):** it enables a much nicer design flow — create a base record and choose (or later SWITCH) its specific type DURING/AFTER creation (create-then-specialize, retypeable), versus the current set-based "choose your table first" where the modeler/user must pick the concrete subtype table up front. **Use case:** switching between experiment types on an existing record. **Scope:** BACKEND — a new exclusive single-valued discriminator column type + exclusive write/routing (mirror the MODULE_ARRAY engine but cardinality-1, hard-delete the prior module on switch); FRONTEND — the `MODULE` single-select `Input.vue` arm + `value/EMX2.vue` display arm already landed in Phase F as a deliberate forward-looking PLACEHOLDER (frontend-only today, no backend column type yet — wire it through when this lands), plus schema-editor support to declare the type. Depends on the Phase C discriminator engine.
- [ ] **Remove the deprecated scalar `inheritName`/`inheritId` GraphQL fields from the BACKEND — owner-confirmed (2026-06-14).** These scalars are LOSSY: `json/Table.java:56,58` derives them as `getInheritNames().get(0)` / `getInheritedTables().get(0).getIdentifier()` — i.e. only the PRIMARY parent — so on a diamond they silently truncate (correctness footgun). The complete `inheritNames`/`inheritIds` lists (added E3) fully supersede them. Phase F already stopped propagating the scalars into the new tailwind/metadata-utils stack (dropped from `ITableMetaData` + `gql/metadata.js`). Backend removal targets: `GraphqlSchemaFieldFactory` table OUTPUT fields (`INHERIT_NAME` ~:372 + `INHERIT_ID` ~:376) and the table INPUT field (`INHERIT_NAME` ~:532 — also kills the input ambiguity of accepting both scalar + list), plus the `inheritName`/`inheritId` fields + derivation in `json/Table.java` (keep the list fields). **Prerequisite:** F5 must first migrate the OLD bootstrap stack `apps/schema` (the only internal reader of scalar `inheritName`, for single-inheritance diagrams/edit) onto `inheritNames`/`inheritIds`. **Caveat:** removing a GraphQL field is API-breaking for any external client reading `_schema{tables{inheritName}}`; since diamonds are new there's no prior contract for diamond behavior, so removal coordinated with F5 is defensible, but consider GraphQL `@deprecated` for one release if external stability matters. `tableExtends` CSV keyword is separate (frozen) — NOT affected.


---

## Phase E REVISED (2026-06-13) — staged E0..E7 (IO / GraphQL / JSON / RDF surface)

> Recon notes: `.plan/notes/phase-e-recon.md` (file:line verified). Owner-locked decisions
> (2026-06-13, AskUserQuestion):
> - **Q1 module-column flow = WIDEN CORE DEFAULT-SELECT.** SqlQuery's default select-all → the
>   module-inclusive set (`getColumnsIncludingActiveModules()`, built by C4) so module columns
>   implicitly flow to RDF, CSV data-export, and every select-all path. Non-module tables = NO-OP
>   (that method == `getColumnsIncludingSubclasses()` when a table has no module subtypes), so the
>   blast radius is naturally scoped to module-bearing roots. GraphQL data-query gets module fields
>   via the output type-builder widening (E2).
> - **Q2 _schema metadata = INCLUDE in Phase E.** Expose `inheritNames` + `inheritIds` (fixes the
>   parent[0] truncation at json/Table.java:56) + column `values` on the GraphQL `_schema` type
>   (output+input) + json model.
> - **Q3 RDF = JUST EMIT MODULE COLUMN VALUES.** NO extra `rdf:type` per active module (FHIR
>   meta.profile[] analog dropped/deferred); module columns emit like any other column.
>
> **No new migration.** Model B derives modules from existing metadata; `values` already persists via
> migration32 (DB v33). `inheritIds` is derived (`getInheritedTables()`) — output-only, no persistence.
> A table-level `modules` field is SKIPPED as redundant (derivable from MODULE tableType + each
> MODULE_ARRAY column's `values`); add only if a real consumer needs it.
> **Deferred (C4-consistent):** filter / order-BY a module column (needs where/order active-set
> semantics; SQL where/order resolution stays `includeModules=false`). Phase E widens read/SELECT only.

Stages (each keeps all suites green; red-green; test-first; backend-test-purity + graphql-test-pattern):

- [x] **E0 — CSV metadata round-trip (molgenis-emx2-io / Emx2.java). DONE & green; STAGED, not committed.** Fixed `tableType` export → `getTableType().equals(DATA) ? null : getTableType().name()` (emits MODULE/ONTOLOGIES/any non-DATA; was ONTOLOGIES-only → MODULE round-tripped as DATA); added `Column.values` CSV round-trip (const `VALUES="values"`, import `setValues(row.getStringArray(VALUES))`, export `row.set(VALUES, ...)` when non-empty, added to `getHeaders()` between SEMANTICS and PROFILES). Multi-parent `extends` + MODULE_ARRAY/ENUM `columnType` already round-trip (untouched). Test `Emx2ModuleRoundtripTest.moduleTableTypeAndColumnValuesRoundtrip` (RED `expected MODULE but was DATA` → GREEN); regression TestExtends + TestOntologyMetadataExport pass; exit 0.
- [x] **E1 — Data-read widening (molgenis-emx2-sql / SqlQuery, FOUNDATIONAL). DONE & green; STAGED, not committed.** New `TableMetadata.getColumnsIncludingModules()` = `getColumns()` ∪ module-subtype local cols (branches from `getColumns()`, NOT the subclass-inclusive variant → is-a default-select byte-identical). `SqlQuery` retrieveRows default-select (`:98`) → `getColumnsIncludingModules()`. JSON default-select (`:305`) and filter/order (`:524`) UNTOUCHED (E2). Tests `TestModuleArrayDiscriminator.defaultSelectIncludesActiveModuleColumn` / `defaultSelectNullsInactiveModuleColumn` (RED→GREEN); TestDiamondInheritance 13 + TestInherits 2 + TestTableMetadataDag 19 green unmodified; exit 0. **Findings:** CSV export uses `retrieveRows` (benefits) but the header list comes from `getDownloadColumnNames()`→`getColumns()` (module-free) → needs E1b. NO null-trimming anywhere (header-driven inclusion).
  - [x] **E1b — CSV download column set. DONE & green; STAGED, not committed.** `TableMetadata.getDownloadColumnNames()` (:198) `getColumns()` → `getColumnsIncludingModules()`. Secondary fix (required): `Emx2Tables.isFileType` called `metadata.getColumn(name)` (null for module cols → NPE) → `getColumnByNameIncludingActiveModules(name)`. CsvApi download + Emx2Tables export now include module cols; ValidatePkeyProcessor recognizes them on import. **Full CSV export→re-import round-trip WORKS** — C3 write-routing fires on `save()` when the row carries the discriminator value + module col value → module subtype row recreated. Tests `TestModuleCsvDownload` (csvExportIncludesModuleColumnAndValue RED→GREEN, csvExportModuleColumnNullForInactiveRow, csvExportReimportRoundTrip); regressions TestImportExportEmx2DataAndMetadata + Emx2ModuleRoundtripTest green; exit 0.
- [x] **E2 — GraphQL data-query module fields. DONE & green; STAGED, not committed.** New `TableMetadata.getColumnsIncludingActiveModulesExcludingHeadings()` (mirrors the subclasses variant). `GraphqlTableFieldFactory.createTableObjectType` → that method (module cols offered). SQL JSON path: default-select (`:305`) → `getColumnsIncludingModules()`; `jsonSubselectFields` plain-projection (`:591`) → `getColumnByName(..., false, true)` (includeModules; `_agg`/`_groupBy` stays false). **Also (required):** added `ENUM_ARRAY` arm to the `STRING_ARRAY` case in 3 `GraphqlTableFieldFactory` switches (createTableField, graphQLTypeOf, getGraphQLInputType) — MODULE_ARRAY's base type was unhandled and would throw once offered. Filter/order/agg BY module col left deferred. Tests: sql `TestModuleArrayDiscriminator.retrieveJsonSelectsActiveModuleColumn`/`retrieveJsonNullsInactiveModuleColumn` (RED `Column modCol unknown`→GREEN); graphql `TestModuleGraphqlDataQuery.*` (end-to-end). TestDiamondInheritance/TestInherits green unmodified; exit 0.
- [x] **E3 — GraphQL _schema metadata + JSON model (molgenis-emx2-graphql). DONE & green; STAGED, not committed.** `GraphqlConstants`: added `INHERIT_NAMES`, `INHERIT_IDS`, `VALUES`. `GraphqlSchemaFieldFactory`: added `inheritNames`+`inheritIds` output fields to table type; `inheritNames` input field to table input type; `values` output+input fields to column types (mirror `semantics` pattern). `json/Table`: added `inheritIds` list field + getter/setter; constructor sets `inheritIds` from `getInheritedTables().stream().map(TableMetadata::getIdentifier)`. `json/Column`: added `values` (List<String>) field + getter/setter; constructor init from `column.getValues()` when non-null/non-empty; `getColumnMetadata()` reverse calls `c.setValues(values)`. `inheritId` scalar and `inheritName` scalar UNTOUCHED (back-compat). New test class `SchemaMetadataGraphqlSurfaceTest`: `jsonModelInheritIdsHasBothParentsForDiamondChild` (RED→GREEN — `inheritIds` now has 2 distinct identifiers for diamond child); `jsonModelColumnValuesRoundtrip` (RED→GREEN — `values` survives JSON round-trip); `graphqlSurfaceExposesInheritNamesInheritIdsAndColumnValues` (RED→GREEN — `_schema` query returns `inheritNames`, `inheritIds`, `columns.values`). `TableJsonRoundtripTest` + `TestGraphqlSchemaFields` all green. Exit 0, spotless+PMD clean.
- [x] **E4 — RDF module column values (molgenis-emx2-rdf). DONE & green; STAGED, not committed.** `Emx2RdfGenerator:282` emission loop + `RdfGenerator:64` `applyComputed` → `getColumnsIncludingModules()` (modules-only, is-a byte-identical). NO extra rdf:type (Q3). **Third change (required):** `RdfRowsGenerator.processRows` early-returns for `tableType==MODULE` — MODULE subtypes pass the inherited-table filter and would hit the `default→throw` (unsupported TableType); their data already emits under the ROOT subject via the widened column set, so no standalone pass. Behavior: MODULE tables produce NO standalone RDF (emitted via host root — composition semantics; revisit only if direct-module RDF is ever needed). Test `RDFTest.moduleColumnValueEmittedAsTripleWhenModuleActive` (RED `Cannot convert unsupported TableType` → GREEN); is-a inheritance RDF fixtures green unmodified; exit 0.
- [x] **E5 — TestMigration migration32→v33 assertion. DONE & green; STAGED, not committed.** `TestMigration.testMigration32`: mirrors the migration2 pre/post idiom — reconstructs the pre-v33 state (downgrade `table_inherits` to scalar VARCHAR + drop `values`), runs `executeMigrationFile("migration32.sql")`, asserts via `information_schema.columns` that BOTH `table_metadata.table_inherits` and `column_metadata."values"` are `data_type=ARRAY` / `udt_name=_varchar` (would RED on scalar or `_text` — non-tautological). Run surgically with all 15 upstream module test tasks `-x`-excluded (the nonparallel module's dependsOn-the-whole-suite was what stalled the first attempt); exit 0, 2 tests pass.
- [x] **E6 — Cross-module column ORDER verify (Phase H prereq). DONE & green; STAGED, not committed.** `Column.getRootTableName()` (Column.java:728) = `getTable().getRootTable().getTableName()`. A module M extends root R, so M's local columns report **R** as root → in the Emx2 export sort `comparing(getRootTableName).thenComparing(getPosition)` (`:231`) module columns INTERLEAVE with R's own columns by position (each row still carries its own `tableName`=M). M is a real table in the export `tables` set (tableType=MODULE, not ONTOLOGIES). **Conclusion: single-file cross-module display order WITHIN a root already works** via position; order round-trips through row-order on re-import even though explicit position VALUES are not emitted on export. **No production change required.** Tests: `Emx2CrossModuleColumnOrderTest.interleavedColumnOrderPreservedAcrossModuleBoundary` (rows-only, no DB; asserts R.colA→M.colX→R.colB→M.colY on first export and second round-trip); `explicitPositionInterleavedColumnOrderPreservedAcrossModuleBoundary` (same with explicit position values). Both RED→GREEN (test was new); TestExtends regression green; exit 0, spotless+PMD clean. OPTIONAL/deferred: emit explicit `position` on CSV export for number-stability (broader blast radius — changes ALL metadata exports; not needed for order-preservation; revisit in Phase H only if required).
- [x] **E7 — Docs. DONE; STAGED, not committed.** `use_schema.md` (multi-parent diamond extends + single-root + dup-column rejection; MODULE tableType composition; MODULE_ARRAY axis col + bare values + one-axis + hard-delete + coexistence, with examples). `CSV.md` (ENUM/ENUM_ARRAY/MODULE_ARRAY type table + `values` semantics incl. the now-ENFORCED membership call-out + MODULE/MODULE_ARRAY round-trip). `semantics.md` (module column values under root subject; NO standalone MODULE RDF; NO rdf:type per module). NOTE (lead to eyeball): CSV.md was nearly empty (only the import-comma FAQ) → new column-type section placed top-level; flag if a canonical type reference lives elsewhere.

- [x] **E8 — Feature-showcase demo model (LOADABLE demo + smoke test). DONE & green; STAGED, not committed.** `data/diamond_showcase/molgenis.csv` (+ data CSVs) — root `Subject` with ENUM `sex` / ENUM_ARRAY `tags` / two MODULE_ARRAY axes `subgroups01` (CockayneSyndrome/XerodermaPigmentosum/Trichothiodystrophy) + `diseaseGroup` (EpidermolysisBullosa); MODULE tables with authentic clinical columns lifted from the real subject model; `AdvancedCockayne` module-extends-module; `ClinicalSubject`+`ResearchSubject`→`ClinicalResearchSubject` diamond on the shared Subject root; demo rows activating none/one/both modules + a diamond row. Registered `DIAMOND_SHOWCASE` in `DataModels` + `DiamondShowcaseLoader`; smoke test `DiamondShowcaseTest` (9 tests: tables/types created, MODULE_ARRAY values, ENUM values, module-extends-module rooted, diamond single-root, active module col projects, inactive nulls, row count) all green, exit 0. **Demo bug found & fixed (NOT engine):** a stray comma had shifted `values` into the label column for 4 rows → `getValues()` null; corrected. Owner-requested; original intent below. A single coherent clinical-themed EMX2 model exercising EVERY new capability, **inspired by the REAL `subject` visible= axes** (see `.plan/notes/subject-visible-axes.md`): primary axis `subgroups01` (disease subgroups cs/xp/ttd/…) + secondary `diseaseGroup` (eb/…) → two orthogonal `MODULE_ARRAY` axes; one MODULE per subgroup value (extends `Subject`), with a handful of REAL columns lifted from `data/patient_registry_demo/molgenis.csv` per subgroup so it feels authentic; module-extends-module (e.g. an advanced variant); a diamond (`ClinicalSubject`+`ResearchSubject`→`ClinicalResearchSubject`, one root); ENUM/ENUM_ARRAY with `values`; is-a + composition coexisting; demo DATA rows activating none/one/both modules + a diamond row. **Home (owner decision):** a standalone loadable demo registered in `molgenis-emx2-datamodels` (`DataModels` + loader + CSV resources, mirror an existing `test/*Example`) so it loads into a running instance for Phase F / visual work, PLUS a smoke test that imports it and queries module columns end-to-end. TRIM (don't port all 557 columns — that's Phase H). Owner of execution: `datamodeler` (reads the real subject file for authentic columns) + `backend` (loader/registry/smoke test). NOT a behavior change — pure demo/test asset.

**Sequencing:** E0 ∥ E1 ∥ E5 (independent); E2, E3 after (graphql, independent of each other); E4 after E1; E6 after E0; E7 last. E8 (demo) independent — after the engine surface (E0–E4) is green. Review after each behavior-changing stage.

### Phase E final review (2026-06-13) — independent review + consolidated regression
**Regression GREEN:** core 107, sql-targeted 61 (TestModuleArrayDiscriminator 38 / TestDiamondInheritance 13 / TestInherits 2 / TestQuery 4 / MetadataPersistenceRoundtripTest 4), io 87, rdf 66, graphql-targeted 31, nonparallel TestMigration 2 — all exit 0. is-a byte-identical confirmed; includeModules boundary correct (projection=true, filter/order/agg=false); no dead code.
- **B1 BLOCKER — FIXED & green.** `webapi/CsvApiTest.shouldUpdateTableMetadata()` had a hardcoded metadata-CSV header (`…semantics,profiles,…`) + row literals that E0's new `values` column broke (webapi wasn't in any earlier agent's surgical run). Realigned header + 8 row literals (added the empty `values` field between semantics and profiles) — NOT a weakening, matches the intentional additive format change. All 14 CsvApiTest methods pass. Grep confirmed NO sibling hardcoded-header tests.
- **S1 (naming) — DONE & green; STAGED (owner approved 2026-06-13).** Renamed `getColumnsIncludingActiveModules*`/`getColumnByNameIncludingActiveModules` → `getColumnsIncludingSubclassesAndModules*`/`getColumnByNameIncludingSubclassesAndModules` (clarifies the subclass-inclusion vs the modules-only `getColumnsIncludingModules`). Updated TableMetadata + GraphqlTableFieldFactory + SqlQuery + Emx2Tables; whole-repo grep `IncludingActiveModules` = ZERO. Pure rename, no behavior change; core/sql/io/graphql targeted suites green, exit 0.
- **N1 (NIT/correctness, RDF ontology) — OPEN, verify-then-decide.** `OntologyIriMapper.addDataTable()` uses `getColumns()` (no modules); IF the mapper isn't called per-module, a MODULE's ONTOLOGY column would emit as a string literal instead of an IRI in RDF. Phase H subject modules ARE ontology-heavy → would surface there. MUST verify the mapper's per-table iteration before "fixing" (review's claim unverified). Likely a 1-line widen (→ getColumnsIncludingModules) if real. Defer to Phase H or a small follow-up.
- **N2 (Phase F, correct scope) — DEFERRED.** GraphQL MUTATION write path uses `getColumnsIncludingSubclassesExcludingHeadings()` → module CONTENT columns not writable via GraphQL mutation (the MODULE_ARRAY discriminator IS writable). Module-column WRITE via GraphQL = Phase F (forms/record editor), consistent with Phase E = read surfaces.

---

## Phase F REVISED (2026-06-14) — staged F0..F6 (Frontend surface)

> Recon notes: `.plan/notes/phase-f-recon.md` (file:line verified by 6 Explore agents + lead). Owner-locked
> decisions (2026-06-14, AskUserQuestion):
> - **D1 — TAILWIND-FIRST.** Full MODULE_ARRAY forms/explorer experience in `apps/ui` + `tailwind-components`
>   (the modern stack, on `apps/metadata-utils`). `apps/molgenis-components` (bootstrap, used by `apps/tables`) gets
>   **safe-degrade only**: gate the one hard break (`FilterInput.vue` throws on an unmapped type) + a sane cell/input
>   fallback so a MODULE_ARRAY schema never crashes. NO full picker/group UX in bootstrap.
> - **D2 — Forms UX = MULTI-SELECT + COLLAPSIBLE GROUPS.** A MODULE_ARRAY axis renders as a multi-select of module
>   names (chips); each selected module reveals a collapsible labeled fieldset of that module's columns; deselect
>   hides+clears the group (engine C6 hard-deletes).
> - **D3 — ONE Phase F PR** (F0–F5 land together; F6 verification woven per surface).
> - **D4 — FRONTEND MODULE-EXPANSION, no extra backend metadata.** DECISIVE FACT: `_schema` metadata lists a root's
>   columns from `json/Table.java:73 = getColumns()` (module-free) → module content cols are NOT under the root's
>   metadata; they live under each MODULE table (real, catalog-visible, in `_schema.tables`). The data-query OUTPUT
>   TYPE exposes module fields on the root object (E2) but `getColumnIds` selects from root metadata → won't fetch
>   them today. So a frontend **module-expansion composable** resolves a root's MODULE_ARRAY `values` (bare module
>   names, same schema) → the named MODULE table → that table's LOCAL columns (its cols minus the root's) = the
>   module's column group; "active" = the discriminator's value array contains that module name. This single
>   composable drives read-selection expansion, form group render+show/hide, and detail-view grouping. Only backend
>   change is F0 (mutation input). E3 already exposes `values`/`inheritNames`/`inheritIds`.

**Live fixture for all visual/e2e:** `DataModels.DIAMOND_SHOWCASE` (`data/diamond_showcase/`) — root `Subject` + 2
MODULE_ARRAY axes (`subgroups01`={Cockayne,Xeroderma,Trichothiodystrophy}, `diseaseGroup`={EpidermolysisBullosa}),
`AdvancedCockayne` module-extends-module, `ClinicalSubject`+`ResearchSubject`→`ClinicalResearchSubject` diamond,
ENUM `sex` / ENUM_ARRAY `tags`.

Stages (each keeps suites green; red-green; test-first; frontend-conventions; pnpm run test-ci + format + lint):

- [x] **F0 — Backend write-enable (the N2 carry; prereq). DONE + reviewed (2026-06-14); STAGED, not committed.**
  Two planned swaps (rowInputType :1019 + getMutationDefinition :925) PLUS two emergent fixes: `TypeUtils.convertToRows`
  non-pk path → `getColumnsIncludingSubclassesAndModules()` (module col was accepted by input type but dropped before
  the Row — additive, key-presence-guarded at :527, is-a unaffected); review follow-up widened `convertMapSelection`
  (:776) so module cols resolve directly in query projection (was via implicit string-name fallback). Tests:
  TestModuleGraphqlDataMutation (3, w/ pre-condition assert) + TestModuleGraphqlDataQuery (2) + TestTypeUtils (17,
  incl. new additive-widening test); is-a regression TestDiamondInheritance 12 / TestInherits 2 green; spotless+pmd
  clean; exit 0. RED was "field name 'modCol' not defined for input object type 'MutRootInput'".
- [ ] ~~**F0 (original spec)**~~ `GraphqlTableFieldFactory.rowInputType()` (~:1019)
  `getColumnsIncludingSubclassesExcludingHeadings()` → `getColumnsIncludingSubclassesAndModulesExcludingHeadings()`
  (exists, `TableMetadata.java:770`; output side already uses it at `:160`). Widen the `getMutationDefinition()`
  guard (~:925) to match for consistency. Engine already routes (C3) + gates required to active modules — only the
  input type omits the cols. Red-green graphql roundtrip (mirror `TestModuleGraphqlDataQuery`): mutation insert
  activating a module writes the module col → query reads it back; update toggles; deselect → C6 delete + null
  read. **Agent:** backend. **Skill:** graphql-test-pattern, backend-test-purity.
- [x] **F1 — Metadata plumbing + module-expansion composable (tailwind/metadata-utils). DONE + reviewed (2026-06-14);
  STAGED, not committed.** Composable lives at `apps/metadata-utils/src/moduleColumns.ts` (pure `expandModuleColumns`/
  `activeModules`, exported via index.ts; 2-consumer rule = F2 forms + F4 detail). Group = MODULE table cols minus the
  ROOT's cols (matched by id/name), unknown module names skipped; `activeModules` unions discriminator arrays across
  the root's MODULE_ARRAY cols. ENUM input = new input/Enum.vue (wraps InputListbox; a11y describedBy threaded into the
  toggle button), ENUM_ARRAY = input/EnumArray.vue (wraps InputCheckboxGroup); EMX2.vue ENUM→ValueString /
  ENUM_ARRAY→ValueList. `isModuleType` NOT added (no consumer — per the rule). Tests: moduleColumns.test.ts (22, incl.
  genuine module-extends-module chain) + fieldHelpers.test.ts (8); metadata-utils + tailwind-components test-ci exit 0;
  format+lint clean. Review confirmed both suites green (the F1 agent's "10 pre-existing nuxt timeouts" claim was FALSE).
- **Live server smoke (2026-06-14, verification-only, nothing staged) — F0+F1 PASS end-to-end on `./gradlew dev`
  with `DIAMOND_SHOWCASE` loaded.** F1: `_schema` exposes 5 MODULE tables (tableType=MODULE; AdvancedCockayne
  module-extends-module), diamond child `ClinicalResearchSubject` returns `inheritNames`/`inheritIds` with BOTH
  parents, MODULE_ARRAY cols (`subgroups01`,`diseaseGroup`) return non-empty `values`. F0: insert Subject row
  activating `subgroups01:["CockayneSyndrome"]` + writing module col `relevantmedhistory` → query reads it back
  → update `subgroups01:[]` (deselect) → module col reads null (C6 hard-delete). Dev server killed, port freed.
- **Testing-enablement (staged, part of Phase F PR):** `apps/central/.../SchemaCreateModal.vue` template dropdown
  now lists `DIAMOND_SHOWCASE` so the demo is loadable from the UI (backend `createSchema(template:...)` already
  accepted it).
- [ ] ~~**F1 (original spec)**~~ (a) `apps/metadata-utils/
  src/types.ts`: add `values?: string[]` to `IColumn`; add `inheritName?`/`inheritId?`/`inheritNames?`/`inheritIds?`
  to `ITableMetaData`; add `ENUM`/`ENUM_ARRAY`/`MODULE`/`MODULE_ARRAY` to the `CellValueType`/`ColumnType` union.
  (b) `tailwind-components/app/gql/metadata.js`: request `values` (cols) + `inheritName,inheritId,inheritNames,
  inheritIds` (tables). (c) NEW composable (location per step-down/2-consumer rule, likely `apps/metadata-utils` or
  a `tailwind-components` composable) `expandModuleColumns(rootTable, schema)` → ordered list of `{module, columns}`
  groups + `activeModules(row, rootTable)` → set; pure functions, unit-tested. (d) `fieldHelpers.ts`: confirm
  `isArrayType(MODULE_ARRAY)`=true, `isRefType`=false; add `isModuleType` if a consumer needs it. (e) ENUM/
  ENUM_ARRAY display+input arms in `value/EMX2.vue` + `Input.vue` (the discriminator base type is ENUM_ARRAY;
  fixture has ENUM/ENUM_ARRAY) — render ENUM as a select over `values`, ENUM_ARRAY as multi-select. **Agent:**
  frontend. Tests: vitest for the composable + helpers; story updates for ENUM/ENUM_ARRAY inputs.
- [ ] **F2 — Forms / record editor (tailwind; the minimum + owner's key concern).** In `useForm.ts` +
  `form/Fields.vue`/`FormField.vue` + `Input.vue`: (a) MODULE_ARRAY column renders as a multi-select of its
  `values` (module names) — reuse the ENUM_ARRAY/array multi-select widget. (b) For each ACTIVE module (via
  `activeModules`), render a collapsible labeled fieldset (D2) of that module's local columns (via
  `expandModuleColumns`); deselect removes+clears the group's values. (c) Module columns participate in
  `visibilityMap`/`requiredMap` — but their effective `visible`/`required` is ALSO gated by module-active (a module
  col is never required when its module is inactive — mirrors engine C5). (d) Submit: the mutation payload includes
  the discriminator + active module cols → root `<T>Input` (needs F0); deselect nets to engine C6 hard-delete.
  Verify column→module grouping uses the owning MODULE table's local cols (D4). **Agent:** frontend. Tests: vitest
  on the pure group/active/required-gating logic; story file for the MODULE_ARRAY field; e2e in F6.
- [ ] **F3 — Table / data explorer.** (a) **tailwind** (apps/ui table/browse views + `tailwind-components/table/
  CellEMX2.vue`): expand module cols into the row selection (via F1 composable) so they display (null for
  non-activating rows, C4); add a MODULE_ARRAY cell render (chips); **GATE filter + order-BY** on MODULE_ARRAY/
  module cols in the tailwind filter/sort UI (backend doesn't support it — C4 deferred). (b) **bootstrap
  safe-degrade** (apps/tables via molgenis-components): `FilterInput.vue:101-103` no longer throws on MODULE_ARRAY
  (exclude from filterable set at `TableExplorer.vue:14` + `FilterSidebar`); `onColumnClick` sort guard for module
  cols; `DataDisplayCell.vue` MODULE_ARRAY → list/chips (not raw). NO group UX. **Agent:** frontend. Tests: vitest
  on gating helpers; story for the cell.
- [ ] **F4 — Data client / app shell (apps/ui).** Detail/record view (`[schema]/[table]/[entity].vue`) renders
  active-module column groups (reuse F1 `expandModuleColumns` + `activeModules`, grouped like the chosen forms UX,
  read-only); `value/EMX2.vue` MODULE_ARRAY display arm. Confirm MODULE tables stay non-top-level in nav
  (`[schema]/index.vue` already filters DATA/ONTOLOGIES → MODULE excluded; verify no leak / no 404 on direct nav).
  **Agent:** frontend. Tests: story for the detail group; e2e in F6.
- [ ] **F5 — Schema editor (apps/schema).** (a) `TableEditModal.vue`: tableType selector incl. `MODULE` (today
  ontology-only special-case). (b) `columnTypes.js`: add `ENUM`/`ENUM_ARRAY`/`MODULE`/`MODULE_ARRAY`. (c)
  `ColumnEditModal.vue`: a `values[]` editor shown for ENUM/ENUM_ARRAY/MODULE_ARRAY (sibling to semantics). (d)
  multi-parent extends: `TableEditModal.vue` single scalar `inheritName` → multi-select `inheritNames`; `utils.ts`
  schemaQuery requests `inheritNames`/`inheritIds`/`values`; `addTableIdsLabelsDescription` consumes the lists; save
  sends `inheritNames`. (e) diagrams: `SchemaDiagram.vue`/`NomnomDiagram.vue` loop `inheritNames[]` → diamond edges.
  **Agent:** frontend. Tests: vitest on utils.ts inherit-list derivation + diagram edge generation. NOTE: apps/schema
  is the ONLY app reading inheritance metadata (blast radius) — diamond display work is local here.
- [ ] **F6 — e2e + visual verification.** Playwright (playwright-cli) on `diamond_showcase` per the Frontend
  Verification Workflow (`./gradlew dev` + load DIAMOND_SHOWCASE; tailwind apps via `NUXT_PUBLIC_API_BASE`). Verify:
  forms picker reveals/hides module groups + writes; explorer shows module cols + filter/sort gated; detail groups;
  schema editor authors a MODULE/MODULE_ARRAY/diamond. 5 themes (Light/Dark/Molgenis/UMCG/AUMC) × 3 sizes
  (1280/768/375). **Agent:** e2e + review (template/CSS vs spec). Kill dev servers when done.

**Sequencing:** F0 first (unblocks F2 write). F1 next (foundation for F2/F3/F4). Then F2 ∥ F3 ∥ F4 (all consume F1)
and F5 (independent — schema editor). F6 woven after each surface is green, consolidated at the end. ONE PR.

**Open verify-before-code (lead, during F1/F2):** (1) confirm the data-query output type field name for a module col
equals the module's local column id (so the expanded selection resolves). (2) confirm MODULE tables + their columns
appear in the `_schema.tables` payload the tailwind metadata query receives (catalog-visible per C2). (3) confirm a
column's owning-table identity is derivable frontend-side (resolve via MODULE_ARRAY `values`→table, not a per-column
`table` field, per D4).

---

## Phase H (NEXT — owner-requested 2026-06-14) — Scalar `MODULE` discriminator + `experiment` demo

**STATUS (2026-06-14):** H.1 BACKEND **DONE & green, STAGED not committed** (24 scalar + 5 graphql + 2 io + 62 rdf green; TestModuleArrayDiscriminator 42 / TestDiamondInheritance 13 / TestInherits 2 byte-identical). Two emergent fixes beyond the change list: `ColumnTypeRdfMapper` gained `MODULE→SKIP` (was NPE on a DATA table carrying a MODULE discriminator); a scalar-ENUM arm added to GraphqlTableFieldFactory. NEXT = **H.2 (experiment demo)** then **H.3 (frontend wire-up)**.

**Goal:** add a scalar `MODULE` column type (single-choice composition discriminator) mirroring `MODULE_ARRAY` but single-valued, and exercise it with an `experiment` table in the demo. Owner wants THIS as the first next phase (type=MODULE column + experiment demo together).

**Current state (verified 2026-06-14):** scalar `MODULE` does NOT exist in the backend — `ColumnType.java:79` has only `MODULE_ARRAY(ENUM_ARRAY)`; `Column.isDiscriminator()` (Column.java:724) = `MODULE_ARRAY` only; `getDiscriminatorColumns()` (TableMetadata.java:850) filters that. Frontend has a STUB only: `Input.vue` `MODULE → InputEnum` arm + `MODULE` in the type union, but `activeModules` (moduleColumns.ts) reads `MODULE_ARRAY` only. So this is a real backend feature + a tiny frontend wire-up.

### Design decision — derived-default `values` (owner-agreed 2026-06-14)
- A `MODULE` column's allowed set **DEFAULTS to all direct + indirect `tableType=MODULE` subclasses** of the declaring (root) table, **computed at READ-TIME** (live via `getModuleSubtypeTables()`, NOT frozen at creation — a module added later just appears).
- Explicit `values` **OVERRIDES** the default — used to restrict/exclude subclasses, and (required) to **partition modules across multiple discriminator columns** on one table (auto-default can't partition; every axis would offer every subclass, breaking the one-module↔one-axis rule O-5).
- Transitive (direct+indirect) is intentional: picking a more-specific module (e.g. `RNAseq extends RNA`) activates its ancestor chain via the existing module-extends-module write path. Trade-off noted: a flat picker mixes specificity levels — acceptable ("generic or specific").
- Derived set surfaces in a NEW `Column.getEffectiveValues()` (owner 2026-06-14, see decisions.md) — `getValues()` stays the RAW declared field so CSV export (`Emx2.java:323`) + JSON save-back (`json/Column.java:114`) never FREEZE the derived set on round-trip (preserving "live, not frozen"). The frontend picker (GraphQL `_schema` OUTPUT) AND the write-time enum-membership check read `getEffectiveValues()` so they share one set; serializers read `getValues()`.
- **Symmetry (LOCKED 2026-06-14):** scalar `MODULE` ONLY for now — `MODULE_ARRAY` keeps requiring explicit `values` (untouched). Revisit MODULE_ARRAY symmetry later.

### Backend (mirror the MODULE_ARRAY engine, single-valued)
- `ColumnType.MODULE(ENUM)` (scalar; MODULE_ARRAY is ENUM_ARRAY).
- `isDiscriminator()` → `MODULE_ARRAY || MODULE`; `getDiscriminatorColumns()` picks up both automatically.
- DDL: `MODULE` = `varchar` scalar (vs MODULE_ARRAY `varchar[]`).
- `values`: derived-default (via `getEffectiveValues()`, raw `getValues()` unchanged) + explicit override; validation = each value is a `MODULE` extending the root + one-axis (reuse `validateModuleArrayValues` logic, read effective set); enum membership on write (the single value ∈ effective values, when non-empty/derived).
- Active-module resolution: generalise the per-row discriminator read so the active set = the array (MODULE_ARRAY) OR `{value}`/`{}` (MODULE). Write routing (C3), upsert, and C6 hard-delete then work unchanged in shape; changing a MODULE value deactivates the old module (hard-delete) + activates the new.
- Query projection (C4) + `_schema` serialization (F2b): NO change — modules are collected structurally via the root-anchored `getModuleSubtypeTables()`, independent of MODULE vs MODULE_ARRAY.
- IO/CSV/RDF/GraphQL input+output: `MODULE` columnType round-trips (mirror MODULE_ARRAY, which is done).
- Tests (mirror TestModuleArrayDiscriminator for scalar): DDL+reload; validation rejects non-module / non-root-extending / out-of-set; insert activates the chosen module row; change-value deactivates old + activates new (C6); query projects active / nulls inactive; **derived-default values** (empty values → subclasses live; explicit override restricts); enum membership; is-a + MODULE_ARRAY suites stay green.

### Frontend (tiny — gate already generalises)
- `activeModules` (metadata-utils/moduleColumns.ts): also collect from `MODULE` (scalar) columns — add the single string value to the active set (currently MODULE_ARRAY-only at lines 21,76). `isModuleColumn` + the useForm visibility gate then work unchanged (module cols tagged by `col.table`).
- `Input.vue` `MODULE → InputEnum` arm already exists; verify it renders the picker over derived `values` + writes the scalar discriminator. Display `value/EMX2.vue` MODULE arm (single) if needed.
- Tests: vitest — `activeModules` includes a scalar MODULE value; a module col visible⇔its scalar value selected; deselect/clear hides.

### Demo data — `experiment` in `diamond_showcase` (`data/diamond_showcase/molgenis.csv`)
- `experiment` (DATA) with `experimentType` `MODULE` column (allowed set DERIVES from the RNA/DNA modules, or explicit list RNA,DNA).
- `RNA` (`tableType=MODULE` extends `experiment`) + a few RNA-specific columns.
- `DNA` (`tableType=MODULE` extends `experiment`) + a few DNA-specific columns.
- Extend `DiamondShowcaseTest` / loader to cover the MODULE scalar end-to-end (load + query the chosen module's cols).

### LOCKED DECISIONS (owner, 2026-06-14)
1. **`experiment` = STANDALONE** in `diamond_showcase` (no Subject ref; keeps focus on the scalar MODULE feature; linkage can come later).
2. **Derived-default = scalar MODULE ONLY** for now. `MODULE_ARRAY` stays explicit-`values` (untouched — zero regression risk to the committed diamond Subject demo). Symmetry revisited later.
3. **RNA/DNA columns = proposed set:** RNA = {libraryStrategy, platform, RIN, readLength, strandedness}; DNA = {platform, captureKit, referenceGenome, meanCoverage, readLength}.
4. **Discriminator naming = LEAVE AS-IS** (`getDiscriminatorColumns`/`isDiscriminator`). Spec/code in sync; no rename.

### Sequencing after Phase H
F3 (bootstrap UI safe-degrade — spec rows 112-113), F5 (schema editor MODULE/MODULE_ARRAY authoring + the `col.table===self` editor-local filter — rows 114-115), F6 (visual matrix). All already in the spec.

---

## Phase C REVISED (2026-06-12) — Unified column-based discriminator engine

> Supersedes the old detailed "Phase C", "Phase D", and "Phase G" sections below (kept for history,
> annotated superseded). Merges them per locked owner decisions. Designed against current code
> (`SqlTable`, `SqlQuery`, `SqlTableMetadataExecutor`, `TableMetadata`, `SqlTypeUtils`).
>
> **NAMING (2026-06-12):** the column type `SUBCLASS_ARRAY` is renamed **`MODULE_ARRAY`** (its values are
> always `tableType=MODULE` tables; semantics = composition, not is-a). The `tableExtends` declaration keyword
> is unchanged (frozen back-compat; a different layer from the column type — NOT unified). Below, read
> historical `SUBCLASS_ARRAY` as `MODULE_ARRAY`.
>
> **DESIGN B (2026-06-12, owner decision — supersedes all `SUBCLASS`-scalar-type references below):** the
> scalar **`SUBCLASS` column type is DROPPED entirely.** `mg_tableclass` is NOT a typed discriminator — it
> stays the master-style untyped, engine-managed, immutable system column exactly as in Phase B (name-keyed
> by the `MG_TABLECLASS` constant, with its own dedicated `createMgTableClassCannotUpdateCheck` trigger). The
> discriminator engine operates ONLY on `MODULE_ARRAY` columns: `getDiscriminatorColumns()` returns
> `MODULE_ARRAY` columns, `Column.isDiscriminator()` == `MODULE_ARRAY`. Rationale: `mg_tableclass` is
> immutable + engine-derived + needs no `values`, whereas `MODULE_ARRAY` is mutable + modeler-declared +
> value-validated — opposite semantics, so forcing both through one "discriminator" type/loop bought nothing
> and created the multi-axis trigger-clobber + `values=null` problems. The is-a path is unchanged from master;
> only the composition (`MODULE_ARRAY`) path is new. A scalar "pick-exactly-one-module" `MODULE` type stays
> DEFERRED (different future need, not `SUBCLASS`). So below, read every "`mg_tableclass` typed `SUBCLASS`" /
> "values auto-derived" / "mg_tableclass in the discriminator loop" as REVERSED.

### Locked owner decisions (2026-06-12)
| # | Decision |
|---|----------|
| C-1 | A discriminator is a first-class **named column**: scalar `SUBCLASS` = EXCLUSIVE is-a identity; array `MODULE_ARRAY` = MULTIPLE composition. |
| C-2 | Inheritance and composition **MAY COEXIST** on one table (orthogonal axes on the shared root PK). `extends` auto-creates the `SUBCLASS` discriminator `mg_tableclass`; `MODULE_ARRAY` columns are additional — NOT a replacement (drops the earlier suppress-`mg_tableclass` rule). Models FHIR `meta.profile[]` / RDF multiple `rdf:type`. Retires plan decision #11. |
| C-3 | A modeler may declare **several** `MODULE_ARRAY` columns (independent modular choice-sets — large clinical tables). Engine loops `getDiscriminatorColumns()` **by name**; never hardcodes `MG_TABLECLASS`. (Absorbs old Phase G.) |
| C-4 | `SUBCLASS`/`extends` values = **TABLE/DATA** subclasses incl. **cross-schema** (already supported). `MODULE_ARRAY` values = **MODULE** classes **ALWAYS**. MODULE = materialized per consuming table (absorbs old Phase D). |
| C-5 | Deactivation = **HARD DELETE**: dropping a module from a `MODULE_ARRAY` on update DELETEs its materialized row (root PK; FK `onDeleteCascade`; root row intact). |
| C-6 | Storage unchanged: table-per-type on the single shared root PK (Phase B invariant). |

> Terminology: reuse spans PostgreSQL **schemas** (call it **cross-schema**, not "cross-host").

### Discriminator naming model
- A discriminator is a `MODULE_ARRAY` column with a NAME. `getDiscriminatorColumns()` = columns of type `MODULE_ARRAY` (via `Column.isDiscriminator()`). (DESIGN B: scalar `SUBCLASS` dropped.)
- **Modeler-named:** `MODULE_ARRAY` composition columns (e.g. `panels`, `subgroups01`).
- **Auto/reserved (is-a, NOT a `getDiscriminatorColumns()` member):** `extends` creates the reserved system column `mg_tableclass` — one, on the root; read-only; immutable (trigger); value = `"schema.ConcreteType"`. Stays the untyped master-style system column, name-keyed by `MG_TABLECLASS`, handled by the existing is-a code (NOT the composition discriminator loop).
- The composition engine never touches `mg_tableclass`; the is-a engine never touches `MODULE_ARRAY`. They coexist on the shared root PK (decision C-2/O-4).
- **Deferred (not built now):** a scalar `MODULE` type ("pick exactly one module" — single exclusive composition). Add later only if a real "choose one variant" case appears.

### Model
- Active type set for a row = union over discriminator columns of their selected `"schema.Table"` value(s). Values stay schema-qualified `"schema.Table"` exactly as `mg_tableclass` works today (dots are NOT allowed in names → the `.` split is unambiguous).
- `MODULE_ARRAY` values are always MODULE classes; scalar `SUBCLASS`/`extends` values are TABLE subclasses.
- Internal helper `DiscriminatorAxis(column, isArray, allowedTypes)` drives DDL/write/query/validation.

### C2 design — MODEL B (LOCKED 2026-06-12, owner via AskUserQuestion) — supersedes the materialization design in the next three subsections

**Pivot:** a MODULE is a **real, queryable table bound to ONE root**, NOT an abstract definition
materialized per consumer. This collapses Phase C onto the Phase B storage engine: one storage model
(table-per-type on the shared root PK), two discriminator flavors differing only in cardinality + which
discriminator drives them:

| Axis | Discriminator | Cardinality | mg_tableclass | Mutable |
|------|---------------|-------------|---------------|---------|
| is-a (extends, DATA subtype) | `mg_tableclass` | EXCLUSIVE (one) | yes | no |
| composition (MODULE subtype) | `MODULE_ARRAY` | MULTIPLE (subset) | no | yes |

A module is a `tableType=MODULE` table that **`extends` its one root** (reusing existing
`setInheritNames`/`executeSetInherit`), producing a real subtype table keyed on the root PK
(PK + FK-on-root-PK + KEY1) but **with NO mg_tableclass** (composition, not identity).
Module-extends-module = normal inheritance, all rooted at the one root → columns flatten via existing
`getColumns()`; NO copy/flatten step. The module table is real and queryable; it is simply excluded from
**is-a** enumeration (`getSubclassTables()`), not from the catalog.

**Consequences vs the superseded materialization design:**
- DROPPED: reserved `mg_<Root>_<Mod>` naming + hash/length handling (O-1) — a module is a real table with
  its own real name. The earlier naming decision is MOOT.
- REVERSED: the earlier "suppress standalone MODULE storage" decision — the MODULE table KEEPS storage
  (it IS the real table). No suppression of MODULE physical DDL.
- DROPPED: per-consumer copy, flatten-on-materialize, off-surface-by-reserved-prefix, hidden tables — the
  whole materialization subsystem.
- KEPT: the `executeSetInherit` flag split (skipPk + skipMgTableclass) — still required so a MODULE subtype
  extends the root WITHOUT mg_tableclass.
- LOST (accepted): cross-root reuse of one module definition across unrelated roots (define per root).
  Reversible later by adding a "materialize a module into another root" feature on top, with the real
  table as the canonical definition.

**Binding model (LOCKED 2026-06-12, owner via AskUserQuestion):** explicit — a MODULE declares
`extends <root>` (its single root) + `tableType=MODULE` (the storage binding); the root declares one or
more `MODULE_ARRAY` columns whose `values` list the modules in that axis (the axis assignment). Validation
cross-checks each value is a MODULE that extends this root (extends C1's exists+tableType check) and O-5
one-axis-per-module. The `values` partition modules into named orthogonal axes (e.g. `panels`,
`subgroups01`) — multi-axis = multiple MODULE_ARRAY columns. Both declarations are necessary (storage
binding + axis assignment); no hidden side-effects (a column edit never mutates the module table).

**Revised C2 sub-steps (storage groundwork only; red-green; existing suites stay green):**
- **C2.A** — Split `executeSetInherit`'s `skipPkAndMgTableclass` → `skipPk` + `skipMgTableclass` (KEY1
  moves under skipPk; mg_tableclass under skipMgTableclass). Diamond behavior preserved exactly (later
  parents = both true). Pure refactor; TestDiamondInheritance is the safety net.
  - **REFINEMENT (planned 2026-06-13, owner-raised) — DROP both params, derive internally:** revert to
    `executeSetInherit(jooq, table, other)`. Derive `skipPk` from *primary-parent* (`other` ==
    `table.getInheritedTables().get(0)`) → PK-copy + KEY1 + meta-removal only for the primary parent.
    Derive mg_tableclass-suppression from `table.getTableType() == MODULE` (the existing
    `getLocalColumn(MG_TABLECLASS)==null` guard already makes the diamond-additional case idempotent, so
    composition is the only real skip). Behavior-preserving (verified vs diamond primary/additional, MODULE,
    multi-level module chain); guarded by TestDiamondInheritance + TestCrossSchemaForeignKeysAndInheritance +
    TestModuleArrayDiscriminator. Apply AFTER the C2 review confirms the green base.
- **C2.B** — A `tableType=MODULE` table extending a root creates a real composition-subtype table with
  `skipMgTableclass=true` (PK+FK+KEY1, no mg_tableclass). `executeCreateTable` passes
  `skipMgTableclass = table.getTableType()==MODULE`.
- **C2.C** — Exclude `tableType==MODULE` composition subtypes from is-a enumeration (`getSubclassTables()`
  and the is-a query joins) so they are not treated as `mg_tableclass` subclasses; they stay real,
  catalog-visible tables.
  - **Rationale (verified 2026-06-13, owner-raised):** `getSubclassTables()` has exactly TWO callers,
    both is-a-semantics keyed by scalar `mg_tableclass` — the is-a join `SqlQuery.tableWithInheritanceJoin`
    (~890) and its column aggregation `TableMetadata.getColumnsFromSubclasses` (~692). A module has no
    `mg_tableclass`, so including it would create a never-matching join + dangling column refs. NO
    DROP/truncate/structural caller uses `getSubclassTables()` (drop is per-table + schema-level ordering),
    so the filter has zero DDL blast radius. Module subtype tables are STILL joined in the query — through
    the SAME generalized join (C4 generalizes `tableWithInheritanceJoin` to be discriminator-driven; it is
    NOT a second join method): the composition axis adds module subtype tables gated by array membership
    (`'schema.Mod' = ANY(arr)`) instead of scalar `mg_tableclass = '...'`. `getSubclassTables()` is just the
    **is-a axis's binding source**; composition bindings come from `getDiscriminatorColumns()`. One join
    mechanism, two binding sources — they are NOT lost. If an all-flavors structural sweep is ever needed,
    add a new `getAllSubtypeTables()` (is-a + module) rather than un-filtering `getSubclassTables()`.
- **C2.D** — Tighten `MODULE_ARRAY` value validation: each value is a MODULE that **extends this root**
  (shares the root) + O-5 one-axis-per-module (extends C1 `validateModuleArrayValues`).
- **C2.E** — Reload round-trip (O-2): module subtype tables + MODULE_ARRAY persist & survive reload via
  existing metadata (no new column/migration).

Acceptance: a MODULE extending a root → real table keyed on root PK, FK onDeleteCascade, KEY1, NO
mg_tableclass; NOT in `getSubclassTables()`; module-extends-module flattens columns; MODULE_ARRAY value
referencing a non-extending / non-MODULE / wrong-axis target rejected; survives reload. (No
write/query/gating/delete yet — C3–C6.)

> Below: C3–C6 write/query/validation prose still applies, but read every "materialized table" as
> "module subtype table" (a real `extends`-root table); the per-type LEFT JOIN/membership logic is
> unchanged in spirit and now joins real subtype tables exactly like Phase B is-a joins.
>
> **UNIFIED JOIN (LOCKED 2026-06-13, owner-raised):** C4 GENERALIZES `tableWithInheritanceJoin` into ONE
> discriminator-driven join — do NOT write a second composition-join method. Model is-a as a single scalar
> axis (column `mg_tableclass`, predicate `= 'schema.Sub'`, bindings = `getSubclassTables()`) and each
> `MODULE_ARRAY` column as an array axis (predicate `'schema.Mod' = ANY(arr)`, bindings = the column's
> `values`). The join loop is identical for both — LEFT JOIN each subtype on the shared root PK +
> membership-gated column projection; only the per-axis predicate (scalar `=` vs array `= ANY`) differs.
> This is where the query-side `DiscriminatorAxis(column, cardinality, bindings)` EARNS ITS PLACE. It is a
> QUERY construct only — it does NOT revive the typed `SUBCLASS` column (Design B stands: `mg_tableclass`
> remains untyped/master-style).
>
> **⚠ SUPERSEDED IN PART (owner 2026-06-13):** recon proved the CURRENT is-a join uses **NO** CASE-WHEN —
> subclass cols are projected RAW (SqlQuery:239), gated by LEFT-JOIN **row presence** (the
> `mg_tableclass`-in-USING branch at 892-894 is dead under root-only `mg_tableclass`). A literal per-subtype
> `mg_tableclass = 'schema.Sub'` CASE-WHEN would BREAK diamonds (a D-row has data-bearing B/C rows but
> `mg_tableclass='schema.D'`). Owner decision: gate BOTH axes by **row presence only — NO CASE-WHEN, NO
> `= ANY` predicate**; rely on the write side (C3 upsert / C6 delete) to keep row⇔active. Consequence:
> `DiscriminatorAxis`-with-predicate is **NOT** introduced (premise void); the join is unified at the
> LEFT-JOIN-set level (two binding sources: `getSubclassTables()` + module subtypes), projection unchanged.
> See the C4 staged-delivery bullet for the final design.

### ~~MODULE materialization per consuming table (O-1 RESOLVED → root-namespaced, reserved prefix)~~ — SUPERSEDED by Model B (2026-06-12)
- When `MODULE_ARRAY` `d` on consuming root `R` (schema `s`) allows `"m.Mod"` where `Mod.tableType==MODULE`: materialize a physical table for *this consumer* keyed on `R`'s root PK.
- **Stored discriminator value** = the module's logical ref `"m.Mod"` (what the modeler picked), unchanged. **Physical materialized table** = root-namespaced with a reserved `mg_`-prefix (e.g. `mg_<Root>_<Mod>`) so it cannot collide with a user table and stays off the queryable surface. Exact collision-safe token verified against EMX2 identifier rules during C2.
- Reuse `SqlTableMetadataExecutor.executeSetInherit(..., skipPkAndMgTableclass=true)` — it already copies parent PK + FK-on-root-PK + KEY1 and SKIPS `mg_tableclass`/is-a, which is exactly MODULE semantics.
- Module-extends-module → **flatten** all module-ancestor columns into the one materialized table (modules have no standalone storage / no is-a join). Collision check via existing Phase B validation.
- Same module reused by another consuming table (possibly in another schema) → an **independent** materialized table, FK'd to ITS own root PK → preserves single-root-PK join invariant; a single shared module table cannot serve two root PKs.
- Materialization record: **derive from `values`** — the materialized table + its `table_metadata` row IS the record (no new metadata column / migration; O-2). Materialized table is NOT in `getSubclassTables()` (not is-a); query/validation enumerate **allowed discriminator targets** → need a unified `getComposedColumns()` (or extend `getColumnsIncludingSubclasses`) including materialized-module columns.

### Multi-discriminator write/query/validation
- **Write** (`SqlTable.executeTransaction`/`executeBatch`/`insertBatch`/`updateBatch`, ~196-524): bucket each row into ALL active write-targets (root once + the `extends` subtype chain if any + each active module's materialized table). Set scalar `mg_tableclass` (if `extends`) and each `MODULE_ARRAY` `TEXT[]` union — they coexist. Root written once (existing `ancestorChainRootFirst` dedup).
- **Hard-delete (C6)**: in `updateBatch`, read prior discriminator array for the row (one SELECT in-tx, O-6), diff, `DELETE FROM <materialized> WHERE pk=root_pk` for removed modules.
- **Query** (`SqlQuery.tableWithInheritanceJoin` ~880, `rowSelectFields` ~238): keep ancestor INNER JOINs; replace the single subclass `USING(pk, mg_tableclass)` with per-allowed-type `LEFT JOIN <target> ON pk-match AND '<value>' = ANY(<d>)` (array) / `= <d>` (scalar). Wrap module-owned column projections in membership `CASE WHEN '<owner>' = ANY(<d>) THEN alias.col ELSE NULL`.
- **Validation** (`SqlTypeUtils.applyValidationAndComputed` ~26, `columnIsVisible` ~157, `checkRequired` ~100): array discriminators already appear in the JS `graph` as real array columns (user `visible=` can do `d.includes('m.X')`); ADD engine-side per-axis `__active_<columnId>` + union `__active`; auto-gate a module-owned column to not-visible (→ cleared, not-required) when its owner is not in the row's active set. Mirrors the existing invisible-column clear (~72-78) — no new clearing logic.

### Migration / DDL
- No destructive migration: scalar `mg_tableclass` unchanged (now typed `SUBCLASS`); `MODULE_ARRAY` is a newly-declared `TEXT[]` column (base `STRING_ARRAY`, already mapped in Phase A under the old name). The `SUBCLASS_ARRAY`→`MODULE_ARRAY` rename is in-code only (ColumnType + type-maps + tests; all test data, `cleandb` — no persisted-data migration). Start with NO new `migrationNN.sql` (derive materialization from `values`). Add a marker column + migration ONLY if schema-reload round-trip proves derivation insufficient (O-2).

### Staged delivery (each keeps all existing suites green; red-green per stage)
- [x] **C0 — Discriminator abstraction + rename (FOUNDATIONAL; O-1-independent). DONE & green (2026-06-12, reviewed; trimmed to Design B).** `getDiscriminatorColumns()` (returns `MODULE_ARRAY` columns) + `Column.isDiscriminator()` (== `MODULE_ARRAY`); renamed `SUBCLASS_ARRAY`→`MODULE_ARRAY`; **dropped the scalar `SUBCLASS` column type** — `mg_tableclass` stays the master-style untyped system column (executor unchanged from Phase B), **with no behavior change** (existing diamond/extends tests are the safety net). Acceptance MET: existing diamond/extends/inherits suites green + `getDiscriminatorColumnsFindsModuleArrayColumns`. See "C0 status" block below.
- [x] **C1 — MODULE_ARRAY DDL + value validation. DONE & green (2026-06-12, reviewed PASS-WITH-FIXES — all fixes applied).** Declare `MODULE_ARRAY` → `varchar[]`; values constrained to MODULE classes; enforce array-element membership against `Column.values` (MODULE_ARRAY is MUTABLE — NO immutability trigger; that is is-a/`mg_tableclass`-only). `mg_tableclass` (is-a) and `MODULE_ARRAY` (composition) **coexist** untouched. See "C1 status" block below.
  - **C1 LOCKED DECISIONS (2026-06-12, owner via AskUserQuestion):**
    1. **Membership enforcement = Java-level at insert/update**, NOT a DB CHECK constraint. Hook in `SqlTypeUtils.checkValidation()` (the shared row-prep path → runs on BOTH insert and update — owner explicitly flagged "should also be done on update"). Reads `column.getValues()` fresh each write → mutable-friendly, no DDL lifecycle, no axis-keyed object to clobber. Discovery: enum membership is enforced NOWHERE today (`ColumnType.validate` is regex-only → no-op for ENUM; `Column.getValues()` only consumed by `MetadataUtils` persistence) → this is net-new, nothing to reuse.
    2. **Scope = generalize to the whole enum family** (ENUM / ENUM_ARRAY / MODULE_ARRAY). ⚠ Back-compat risk: existing enum data outside its `values` would now be rejected. MITIGATION: enforce ONLY when `getValues()` is non-null AND non-empty (no declared set ⇒ free string, unchanged). Surface — do NOT silently "fix" — any existing-suite failure this causes.
    3. **DiscriminatorAxis = DEFERRED to C2.** C1 loops `getDiscriminatorColumns()` directly (one column type, always array, allowedTypes == `getValues()` — a wrapper carries no new info). Reintroduce in C2 when materialization/write-routing genuinely loop over axes.
    4. **MODULE existence + tableType==MODULE validation** in `SqlColumnExecutor.validateColumn()` (line ~354; already called from column-add `addTransaction`→63 AND table-create `executeCreateTable`→89) — mirror the existing ref-table-existence throw pattern. Each `MODULE_ARRAY` value parses as `schema.Table` (reuse the `mg_tableclass` schema-qualified convention) and must resolve to an EXISTING table whose `tableType==MODULE`; else `MolgenisException`.
    5. **MODULE_ARRAY → varchar[]** already maps via base-type `ENUM_ARRAY` in all 5 type switches → NO new type-map code; just an assertion test.
  - **C1 design notes (from C0 review, simplified by Design B):** there is no shared discriminator trigger; `mg_tableclass` keeps its own `createMgTableClassCannotUpdateCheck`. No new DB object for MODULE_ARRAY (Java validation only) → the multi-axis clobber concern is moot. Acceptance: column is `varchar[]`; `mg_tableclass` still present + immutable when `extends` used; allowed MODULE values persisted/validated; non-existent/non-MODULE value rejected; out-of-set element rejected on insert AND update; in-set accepted; a table with mg_tableclass + two MODULE_ARRAY columns keeps ALL their checks independently.
- [x] **C2 — MODULE = real composition-subtype table bound to one root (MODEL B). DONE & green (2026-06-13, independently re-verified; executeSetInherit simplification folded in; STAGED).** See "C2 design — MODEL B" block above (supersedes materialization) + "C2 status (2026-06-13)" block below. Sub-steps C2.A–C2.E: flag split → then SIMPLIFIED to param-free derived `executeSetInherit`; MODULE-extends-root → real table, no mg_tableclass; exclude MODULE subtypes from is-a enumeration; tighten MODULE_ARRAY value validation to "extends this root" + one-axis; reload round-trip. (Materialization/reserved-naming DROPPED; cross-root reuse intentionally LOST, reversible later.)
- [x] **C3 — MULTIPLE write routing. DONE & green (2026-06-13, reviewed NO-BLOCKERS); STAGED, NOT committed.** Multi-bucket insert: activating modules writes rows into their module subtype tables; the `varchar[]` array itself is R's own local column (already written by the existing chain loop — C3 adds ONLY the module subtype rows). Acceptance MET: a row activating 2 modules → rows in both module subtype tables on shared root PK; array = both values; activating one → only that module's row. See "C3 status (2026-06-13)" block below.
  - **C3 DESIGN — LOCKED 2026-06-13 (owner via AskUserQuestion + mid-task steer). IMPLEMENTED 2026-06-13 (see C3 status block below).** Recon: `.plan/notes/c3-write-routing.md` (verified file:line).
    - **APPROACH (owner steer 2026-06-13): GENERALIZE the existing chain loop — no separate parallel helper.** Minimal new logic = (a) extend the per-row table list with active module tables; (b) FILTER the row-batch per table before each `insertIntoSingleTable` call. is-a ancestors/root/self = needed by ALL rows (filter ⇒ everyone); a module table = needed only by rows whose active set contains it. Everything else (`insertIntoSingleTable`, `copyRecordValuesIntoRows`, PK copy-down) is reused unchanged.
    - **Why per-table row-filtering is safe:** the PK copy-down (insertBatch ~415-423) only fires for AUTO_ID PK columns, which exist ONLY on the root; root is processed first with the FULL row list (index-aligned with its `inserted`). Module tables have no AUTO_ID PK (PK = FK-on-root-PK, already in each row) ⇒ their copy-down block is a no-op ⇒ filtering their row subset can't break index alignment.
    - **Table set per write:** `chain = ancestorChainRootFirst(table) + table` (is-a, all rows) THEN append active module subtype tables (root-first / parent-module-before-child), EXPANDING each active module to its module-ancestors up to (excluding) root for FK integrity (module-extends-module); skip any table already in the is-a chain. Empty discriminator set ⇒ list == today ⇒ is-a path 100% untouched, the two coexist.
    - **Active set per row** = union over `getDiscriminatorColumns()` (MODULE_ARRAY) of `row.getStringArray(d)`, normalized to `"schema.Module"` (reuse the `mg_tableclass` / `validateModuleArrayValues` qualify+split convention). Per-table predicate `rowsFor(current)`: is-a chain ⇒ all rows; module ⇒ rows whose (ancestor-expanded) active set contains `current`.
    - **Column source per table:** is-a chain tables keep today's `getLocalStoredColumns(current, updateColumns)`. Module tables need `getLocalStoredColumns(M, getInsertColumns(M, providedNames))` — derived from M's OWN metadata + the row's provided names, because the passed `updateColumns` is R-leaf's column list and does NOT carry module-owned columns. Shared root PK already in each row from `copyRecordValuesIntoRows`.
    - **DECISION 1 (validate active modules, ONE-GO — revised per owner 2026-06-13):** validation/computed stays at `executeBatch` (where is-a ALREADY does it one-go over `leaf.getColumns()` incl. inherited, BEFORE the chain fires — `SqlTable.java:333`/`337-338`; NOT a pre-existing bug). C3 EXTENDS that pass to a PER-ROW union `entry.getColumns() ∪ {active modules' columns}` (active set read from the row's MODULE_ARRAY) → `applyValidationAndComputed(union, row)` ONCE before the chain. Module computed/visible/required share ONE graph with R's columns. Inactive modules' columns are absent from the union ⇒ never required ⇒ natural gate. Do NOT validate per-module mid-chain (earlier draft). insertBatch/updateBatch stay purely about writing. Full active-set-aware `__active` gating still refined in C5.
    - **DECISION 2 (update = upsert active, defer delete to C6):** `updateBatch` generalizes the same way with `updateOnConflict=true` (insert-on-conflict-do-update) for the module tables → creates rows for newly-activated modules, updates already-active ones. is-a chain tables keep today's pure-UPDATE. Modules REMOVED from the array keep STALE rows until C6 hard-delete. `insertBatch` passes the existing SAVE flag for the conflict mode.
    - **MODULE_ARRAY varchar[] value** = R's OWN local column ⇒ already written by the chain loop; C3 adds ONLY the module subtype rows.
    - **DEFERRED (NOT in C3):** C4 query projection (module cols read back through R), C5 visible/required gating, C6 hard-delete-on-deactivation. Module rows are observable in C3 by querying the module subtype table directly (real queryable table under Model B) — purity-safe, no raw SQL.
- [x] **C4 — MULTIPLE query projection (GENERALIZE the join, do NOT duplicate it). DONE & green (2026-06-13; STAGED, not committed; review + full-sql/core regression in flight).** Generalize `SqlQuery.tableWithInheritanceJoin` into ONE join whose LEFT-JOIN set is sourced from TWO binding sources: is-a (`getSubclassTables()`) + composition (module subtype tables of R). Both LEFT JOIN on the shared root PK exactly as is-a does today.
  - **C4 status (2026-06-13) — DONE, reviewed NO-BLOCKERS, regression green:** test-first/red-green (RED = `getColumnByName` threw "Column unknown" before resolution). 3 code files staged: `TableMetadata.java` (`getModuleSubtypeTables`/`collectModuleSubtablesDeduped`, `getColumnsIncludingActiveModules`, `getColumnByNameIncludingActiveModules`), `SqlQuery.java` (additive module LEFT-JOIN loop after the UNTOUCHED is-a loop in `tableWithInheritanceJoin`; module-aware name resolution), `TestModuleArrayDiscriminator.java` (+4 C4 tests → 19).
    - **Review (independent, NO BLOCKERS) + fixes applied:** (1) SCOPE CONTAINMENT — module-col name resolution confined to the TWO row-OUTPUT-projection sites only: `rowSelectFields` (~199) + the `refJoins` selection-join loop (~954, "only used for row based queries"); `getColumnByName` gained a `boolean includeModules` (4-arg) param, all filter/where/order/agg/JSON callers delegate with `includeModules=false` = exact pre-C4 is-a-only behavior (THROW on a module col name there — filter/order BY module col stays deferred). This also REVIVED `getColumnByNameIncludingSubclasses` (it is the `includeModules=false` resolver again → no longer dead). (2) NIT `.orElse(null)` aligned. (3) NIT new test `queryMixedBatchProjectsPerRowModuleColumns` (row A=Mod1, row B=Mod2 in one batch → each projects only its own col — proves per-row LEFT-JOIN gating).
    - **Verified exit 0:** TestModuleArrayDiscriminator 19, TestDiamondInheritance 13 (UNMODIFIED), TestInherits 2 (UNMODIFIED), TestQuery 4 (filter paths unaffected); FULL regression after cleandb: molgenis-emx2-sql 351/0-fail/1-skip, molgenis-emx2 core 106/0-fail. spotless + PMD clean. No GraphQL/IO/RDF leak; default-select untouched.
    - **CARRY → C5/C6:** row-presence gating means a deactivated module's STALE row would still project UNTIL C6 hard-delete makes row⇔active truthful (C4 tests don't deactivate, so green; C6 closes this). Filter/order BY a module col is deliberately NOT enabled (would need `includeModules=true` on those paths + active-set semantics — revisit with C5).
- [x] **MODULE_ARRAY = BARE table names only (owner-requested simplification, 2026-06-13; DONE & green; STAGED, not committed).** Evolved in TWO owner steps the SAME day: first "bare allowed, default to current schema, store as-typed, qualified still accepted, identity membership" — then owner chose to **drop cross-schema modules entirely**, so the `schema.Table` syntax for MODULE_ARRAY was REMOVED wholesale. FINAL model: a MODULE_ARRAY value is a BARE table name resolved in the declaring table's OWN schema; **ANY dotted value (trailing/leading-dot or `schema.Table`, even same-schema) is REJECTED**; membership is plain EXACT-string match (uniform with ENUM/ENUM_ARRAY). The earlier identity machinery (`TypeUtils.moduleRefIdentity` + the MODULE_ARRAY membership branch + the dot-split/cross-schema check + the `activeModuleTables` qualify+split) was DELETED. is-a/`mg_tableclass` is UNAFFECTED — stays `schema.Table`, cross-schema-capable (the qualification is meaningful there because is-a genuinely crosses schemas; composition does not). Files: `TypeUtils.checkEnumMembership` (uniform exact-match; helpers deleted), `SqlColumnExecutor.validateModuleArrayValues` (reject dotted, resolve bare in declaring schema, keep exists/MODULE/extends-root) + `validateNoModuleInMultipleAxes` (plain contains), `SqlTable.activeModuleTables` (resolve bare in entry schema), tests in `TestModuleArrayDiscriminator`/`TestTypeUtils`/`MetadataPersistenceRoundtripTest` converted qualified→bare. Reviewed NO-BLOCKERS (stale spec rows fixed by lead; minor diagnostic-ordering + 2 test-gap nits queued). Verified green: TestModuleArrayDiscriminator 27 (incl. leading-dot rejection + a write-path integration test inserting a dotted row value → rejected), TestTypeUtils 16, TestDiamondInheritance 13, TestInherits 2; FULL regression molgenis-emx2-sql 356/0-fail/1-skip + core 107/0-fail, exit 0; spotless+PMD clean; zero `moduleRefIdentity` refs remain. Reviewed twice (bare-name layer + bare-only refactor), NO-BLOCKERS; the per-value validation now precedes the multi-axis check (cleaner diagnostics). **NOTE for commit:** logically distinct from C4 — candidate for its own commit. **Phase E (EMX2 CSV / GraphQL) MUST surface MODULE_ARRAY values as BARE same-schema table names (no `schema.` prefix) — reversible later (re-accepting dots is additive) if a cross-schema module library ever becomes a real need.**
  - **GATING DECISION (owner 2026-06-13, supersedes the CASE-WHEN/predicate framing in the UNIFIED JOIN note below):** projection is gated by **ROW PRESENCE ONLY** (LEFT JOIN) for BOTH axes — **NO CASE-WHEN, NO per-axis `= ANY` predicate.** "We only care what rows actually exist; we assume updates manage this (C3 upsert / C6 hard-delete keep row⇔active) and don't gate-keep at query time." This makes the current is-a projection byte-identical (linchpin: TestDiamondInheritance/TestInherits green UNCHANGED) and is correct for modules once C6 lands (until C6 a deactivated module's stale row would still project — acceptable, C4 tests don't deactivate). **`DiscriminatorAxis`-with-predicate is therefore NOT introduced** (its sole justification was the predicate difference, now gone); the "axis" collapses to a binding-source enumeration.
  - **Changes (minimal):** (1) `tableWithInheritanceJoin` — after the is-a subclass LEFT JOINs, also LEFT JOIN each transitive MODULE subtype table of R on root PK (new `TableMetadata` method, e.g. `getModuleSubtypeTables()`, symmetric to `getSubclassTables()` but `isModule()`==true; auto-covers module-extends-module ancestors). (2) Column surface — `getColumnByName` (SqlQuery ~1722/1725/1736) must RESOLVE module cols: add `getColumnsIncludingActiveModules()` (= `getColumnsIncludingSubclasses()` ∪ module-subtype local cols) used ONLY in SqlQuery resolution; **global `getColumnsIncludingSubclasses` UNTOUCHED** (keeps GraphQL/Phase E surface out of scope). (3) `rowSelectFields` **UNCHANGED** — raw projection (line 239) already reads the module col by name from the single combined alias once it's joined.
  - **Boundary (owner 2026-06-13):** C4 = module cols SELECTABLE BY NAME at the Java `retrieveRows`/`select` level (`R.query().select(s("id"), s("modCol")).retrieveRows()`). DEFER: implicit/default-select inclusion + GraphQL field surface (Phase E); filter/where/order BY a module col (later); hard-delete (C6).
  - **Acceptance:** activating 2 modules → both module cols present on shared PK; activating 1 → other module's col NULL (by row absence); module-extends-module → deep + ancestor module cols both project; existing is-a/diamond queries unchanged.
- [x] **C5 — Validation gating. SUBSUMED BY C3 + locked by test (2026-06-13).** Investigation (owner-prompted): the visible/required/computed gating of module columns by the active set was ALREADY delivered by C3's per-row validation iteration — `SqlTable.applyValidationPerRow` → `buildValidationUnion` builds `base ∪ ACTIVE-module columns` and calls `SqlTypeUtils.applyValidationAndComputed(union, row)`. ACTIVE modules' columns get full computed/visible/required/validation; INACTIVE modules' columns are excluded from the union → never required, never validated, never computed (the natural gate). The MODULE_ARRAY discriminator columns are base columns of R, so they're already in the JS graph — a modeler's `visible`/`required` expression can reference activation directly (e.g. `panels.includes('DiabetesPanel')`) without engine help.
  - **Locked by** `TestModuleArrayDiscriminator.requiredModuleColumnEnforcedOnlyWhenModuleActive` (28th test): module with a REQUIRED column — activate it + omit the value → REJECTED (`MolgenisException`, message names the column); activate + provide → OK (module row written); module INACTIVE + omit → OK (the gate; this arm regresses RED if anyone validates module columns regardless of activation). All 3 arms green on current code; exit 0.
  - **NOT built (deliberately):** (1) an engine-provided `__active`/`__active_<axis>` binding — pure ergonomics; the axis column is already in the graph, so deferred until a real need appears (revisit in Phase H). (2) visible-FLAG surfacing for form rendering (so a frontend knows which module fields to show) — that is **Phase F** (frontend), NOT write-validation. The earlier "auto-gate inside `columnIsVisible` by ownership" idea was the wrong mechanism for the write path (C3's union-exclusion already suffices); it would only matter for the Phase F read/flag surface.
- [x] **C6 — Hard-delete on deactivation. DONE & green, reviewed NO-BLOCKERS + S1/S2/N3 follow-ups applied (2026-06-13); STAGED, not committed.** Diff old/new active set in `updateBatch`, DELETE removed module rows. Acceptance MET: dropping a module removes its row, root + other modules intact; AND a table with BOTH `extends`-identity and a `MODULE_ARRAY` works together (mix smoke). Recon `.plan/notes/c6-hard-delete.md`. See "C6 status (2026-06-13)" block below.
  - **C6 DESIGN (LOCKED 2026-06-13):**
    - **FK is `onDeleteCascade`** (`SqlTableMetadataExecutor:178-179`, references the DIRECT parent's PK). Removed-module set is **downward-closed** (a child module active ⟹ its ancestor chain active) so cascade is safe — but C6 deletes **EXPLICITLY, child-first** (reverse root-first) for deterministic counts; cascade stays backstop only.
    - **Reuse `SqlTable.deleteBatch(moduleTable, subset)` (`:821`)** — batched DELETE by PK on ONE table; module PK = shared root PK already in each Row. No new delete plumbing. Defensive `tableType.isModule()` guard.
    - **O-6 prior read:** before the is-a update loop overwrites R's array column, SELECT prior discriminator arrays by PK (`getJooq().select(disc+pk).from(owningTable).where(getWhereConditionForBatchDelete(rows)).fetch()`), grouped by the discriminator's owning table — common case (array on root) = ONE select per batch.
    - **Diff:** per row `removed = oldActive \ effectiveNewActive`, both via existing `activeModuleTables()` (expands module-ancestors, returns only module subtype tables). **Array-omission correctness fix:** `effectiveNew[disc] = row provides disc ? row value : priorValue` so a partial update that doesn't touch an axis nets to ZERO removal (without this, an omitted array would delete everything).
    - **Where:** new logic only inside `updateBatch` (prior-read before the chain loop; diff+delete after the existing C3 `insertModuleRows` upsert). Empty-discriminator ⟹ whole block skipped = is-a path byte-identical (mix-smoke: is-a/`mg_tableclass` rows never enter the diff → never deleted).
  - **CARRY FROM C3 REVIEW (2026-06-13) — RESOLVED, owner decision = LEAVE AS-IS:** the C3 module upsert reuses the is-a SAVE/upsert path (`getInsertColumns` filters only refbacks) so updating a row that activates a module but OMITS some module columns NULL-overwrites their prior values. Identical to pre-existing is-a upsert behavior (NOT a C3 regression). **Owner chose 2026-06-13 to keep it consistent with is-a — NOT fixed in C6** (C6 stays strictly hard-delete; `insertModuleRows` stays shared insert/update with no branching). Documented limitation; revisit in a dedicated partial-update pass if ever needed.

### C0 status (2026-06-12) — DONE & green, reviewed; trimmed to Design B

First landed the full unified version (mg_tableclass typed `SUBCLASS` + generalized discriminator trigger
loop + `DiscriminatorAxis`), reviewed it green, then on owner decision TRIMMED to **Design B** (drop scalar
`SUBCLASS`; keep mg_tableclass master-style). The first review's findings (`values=null` deviation, multi-axis
trigger clobber) were what motivated Design B — both are eliminated at the source by the trim.

**Final C0 (staged, NOT committed):**
- Rename `SUBCLASS_ARRAY`→`MODULE_ARRAY` — `ColumnType.java`, `TypeUtils.getArrayType` (SUBCLASS line removed), `ColumnTypeRdfMapper` (SUBCLASS entry removed; MODULE_ARRAY→SKIP kept), `TestTableMetadataDag`. Whole-repo grep `SUBCLASS_ARRAY` = ZERO; grep `ColumnType.SUBCLASS` = ZERO.
- **Scalar `SUBCLASS` column type DELETED** from `ColumnType`.
- `Column.isDiscriminator()` == `getColumnType() == MODULE_ARRAY`; `TableMetadata.getDiscriminatorColumns()` = `MODULE_ARRAY` columns (filters `getColumns()` by `isDiscriminator()`).
- `mg_tableclass` UNCHANGED from Phase B — untyped, engine-managed, immutable system column; `SqlTableMetadataExecutor` reverted to HEAD (its own `createMgTableClassCannotUpdateCheck` intact). `DiscriminatorAxis` removed (no consumer until C1).
- New test `TestTableMetadataDag.getDiscriminatorColumnsFindsModuleArrayColumns`.

**Suites (to re-confirm after the trim, post `./gradlew cleandb`):** molgenis-emx2, -sql (TestDiamondInheritance, MetadataPersistenceRoundtripTest, TestInherits), -io (TestExtends), -rdf (ColumnTypeRdfMapperTest), -graphql. spotless+PMD clean.

**Carry into C1:**
- Reintroduce `DiscriminatorAxis` (for `MODULE_ARRAY` axes) with `allowedTypes` populated from the column's `values`.
- `MODULE_ARRAY` is MUTABLE → it gets a per-column array-membership consistency check, NOT an immutability trigger (that is mg_tableclass-only). Design any such trigger axis-keyed per column from the start so multiple `MODULE_ARRAY` columns can't clobber each other (the clobber bug from the first C0 cannot recur because mg_tableclass is out of the loop and keeps its own trigger).
- `values` existence-validation (schema.Table must exist; MODULE_ARRAY values must be MODULE classes) lands here.

### C1 status (2026-06-12) — DONE & green, reviewed (PASS-WITH-FIXES, all fixes applied); STAGED, NOT committed

Implemented per the C1 LOCKED DECISIONS above. Files (6, staged):
- `molgenis-emx2/.../utils/TypeUtils.java` — new `public static checkEnumMembership(Column, Object)`: enforces each scalar/array element ∈ `Column.getValues()` for the ENUM family (ENUM/ENUM_ARRAY/MODULE_ARRAY via `isEnum()`), ONLY when `value != null` and `getValues()` non-empty. Logic lives in CORE (owner decision — DB-independent, reusable by GraphQL/IO).
- `molgenis-emx2-sql/.../SqlTypeUtils.java` — `checkValidation()` now invokes `TypeUtils.checkEnumMembership(...)` (the shared row-prep path → runs on INSERT **and** UPDATE). Latent-bug fix (owner-approved, bundled): the pre-existing regex `validate(...)` arg was read with `getName()` on the identifier-keyed validation graph (`convertRowToMap` keys by `getIdentifier()`, 298/302) → silently no-op'd for name≠identifier columns; changed to `getIdentifier()` so guard + regex + membership all use the same key. **Note for commit message.**
- `molgenis-emx2-sql/.../SqlColumnExecutor.java` — `validateModuleArrayValues(c)` called from `validateColumn` (runs on column-add AND table-create) when type==MODULE_ARRAY and DB-attached: each value parses as `schema.Table` (`split("\\.",2)`, both parts non-empty else MolgenisException — AIOOB hardened), resolves cross-schema, must exist with `tableType==MODULE`.
- `molgenis-emx2-sql/src/test/.../TestModuleArrayDiscriminator.java` (new, 10 tests) — persistence+reload (varchar[]), non-existent/DATA/ONTOLOGY/malformed rejection, insert+update membership, generic ENUM membership + no-values free-string, mg_tableclass+2×MODULE_ARRAY coexistence (mg_tableclass present + **readonly** assertion).
- `molgenis-emx2/src/test/.../TestTypeUtils.java` — 5 `checkEnumMembership*` core unit tests.
- `molgenis-emx2-sql/src/test/.../MetadataPersistenceRoundtripTest.java` — enum + MODULE_ARRAY `values` roundtrip.

**Decisions resolved in C1 (supersede the "Carry into C1" placeholders above):**
- **DiscriminatorAxis NOT reintroduced** — deferred to C2 (one column type, always-array, allowedTypes==`values`; a wrapper carried no info). C1 reads `getDiscriminatorColumns()`/`MODULE_ARRAY` directly.
- **Membership = Java-only, NO DB CHECK/trigger** (MODULE_ARRAY mutable). The "axis-keyed per-column trigger" carry-note is MOOT — no DB object created, so no clobber surface.
- **Scope = generalized to whole ENUM family** (owner decision), guarded to `values` non-empty → zero back-compat fallout (all suites green confirm no existing enum data is out-of-set).

**Suites green after `./gradlew cleandb`:** molgenis-emx2 core (106), -sql (TestModuleArrayDiscriminator 10, MetadataPersistenceRoundtripTest 4, TestDiamondInheritance 13, TestInherits), -graphql (54), -rdf (159/2 skip), -io (79). spotless+PMD clean.

**Carry into C2 (do NOT lose):**
- **mg_tableclass immutability is enforced at TWO layers**: (1) API — `mg_tableclass` is `setReadonly(true)` so `getUpdateColumns()` (SqlTable.java:365 `!isReadonly()`) drops it from the write path; (2) DB trigger `createMgTableClassCannotUpdateCheck` backstops raw SQL. The DB trigger has NO dedicated raw-SQL update test (testing it would violate `backend-test-purity`); the project relies on the readonly API guarantee. C1 asserts the readonly layer (pure-Java); the trigger stays defense-in-depth. If a trigger test is ever wanted, it must be an explicit, isolated exception to test-purity.
- `getName()→getIdentifier()` regex-validation fix is in the C1 diff — call it out in the eventual commit message as an intentional latent-bug fix bundled with C1.

### C2 status (2026-06-13) — DONE & green, independently re-verified; executeSetInherit simplification applied; STAGED, NOT committed

MODEL B implemented test-first (C2.A–C2.E), then the `executeSetInherit` param-elimination simplification folded on top. Files staged (3 main + 3 test; overlaps the C1 stage):
- `SqlTableMetadataExecutor.java` — `executeSetInherit` collapsed to a SINGLE 3-arg `(jooq, table, other)`; derives
  `isPrimaryParent` (resolved `getInheritedTables().get(0)` matched by schema+name; empty list ⇒ primary) gating
  PK-copy + meta-removal + KEY1; mg_tableclass block gated by `tableType != MODULE` (keeps the existing
  `getLocalColumn(MG_TABLECLASS)==null` null-guard). NO caller-passed flags.
- `SqlTableMetadata.java` — `bulkAddInheritTransaction` calls the 3-arg form; dead `isAdditionalParent` removed.
- `TableMetadata.java` — `collectSubclassTablesDeduped` filters `!getTableType().isModule()` (C2.C: MODULE excluded
  from is-a enumeration; stays catalog-visible). See the C2.C rationale block above.
- `SqlColumnExecutor.java` — `validateModuleArrayValues` tightened (C2.D): each value must be a MODULE that EXTENDS
  this root (root-match by schema+name; transitive module-extends-module resolves to the shared root) +
  one-axis-per-module (`validateNoModuleInMultipleAxes`).
- Tests: `TestModuleArrayDiscriminator` (15 @Test — C1's 10 fixtures updated to extends-root + 5 new C2:
  `moduleExtendsRootIsRealTableWithPkFkKey1NoMgTableclass`, `moduleSubtypesExcludedFromIsAEnumeration`,
  `moduleArrayValueMustExtendThisRoot`, `moduleArrayValueOneAxisPerModule`, `moduleSubtypesAndModuleArraySurviveReload`);
  `MetadataPersistenceRoundtripTest` (modules extend root before the MODULE_ARRAY ref; values roundtrip).

**Verification (independent, per-class, stall-proof — exit 0):** `TestModuleArrayDiscriminator` (15), `TestDiamondInheritance`
(13), `TestTableMetadataDag` (19), `MetadataPersistenceRoundtripTest` (4), `TestInherits` (2), `TestExtends` (1),
`TestCrossSchemaForeignKeysAndInheritance` (4); spotless + PMD clean. C1 fixture edits preserved coverage (review
confirmed; the `PanelC` added for O-5 strengthens `mgTableclassAndModuleArrayCoexist`). `XmlAccessType.FIELD` warning
confirmed pre-existing/benign (transitive jakarta XML bind; absent from all `--console=plain` runs; no exit-code impact).
NOTE: a simplification agent under-reported the diamond/module-array counts as 8/9 in its summary — reconciled against
the actual `@Test` counts (13/15) + the independent verification; the same `--tests` command runs the full class, so it
was a miscount, not skipped tests.

**Commit-message carries:** (1) the `getName()→getIdentifier()` regex-validation latent-bug fix bundled in C1; (2) C2
tightening MODULE_ARRAY validation to "extends this root" REQUIRED updating C1 module fixtures to extend their root —
a deliberate requirement change from the Model B pivot, NOT a test weakening.

**Carry into C3+:** write routing into module subtype tables (C3); C4 GENERALIZES `tableWithInheritanceJoin` into the
unified discriminator-driven join (see "UNIFIED JOIN (LOCKED 2026-06-13)" note above) — do NOT write a parallel
composition-join method.

### C3 status (2026-06-13) — DONE & green, independently reviewed (NO BLOCKERS); STAGED, NOT committed

Implemented test-first per the C3 DESIGN block above. Files staged (2; both C3-only — `git diff --cached` clean):
- `SqlTable.java` (+185/-4) — 4 changes: (1) `executeBatch` validation extended via new `applyValidationPerRow`/`buildValidationUnion`
  → per-row union `base columns ∪ active modules' columns` before the chain (one-go; empty-discriminator rows fall through to the
  original `applyValidationAndComputed(baseColumns, rows)` call = exact no-op). (2) `insertBatch` calls new `insertModuleRows(table,
  rows, updateOnConflict, isATableKeys)` after the is-a chain loop. (3) `updateBatch` calls `insertModuleRows(..., true, ...)` =
  upsert. (4) new private helpers `activeModuleTables`/`activeModuleTableKeys` (active-set, normalized "schema.Module", root-first,
  module-ancestor-expanded, deduped), `expandModuleAncestors` (FK-order: parent module before child), `collectAllActiveModuleTables`
  (batch-wide, skips is-a tables), `insertModuleRows` (filters row-subset by schema.table KEY string — NOT SqlTable reference —
  builds columns from M's OWN metadata, calls `insertIntoSingleTable` ON the module's own SqlTable instance so `getSelectedRowValues`
  uses module metadata).
- `TestModuleArrayDiscriminator.java` — 4 C3 tests (15→19): `insertActivatingTwoModulesWritesRowInEachModuleTable`,
  `insertActivatingOneModuleWritesOnlyThatModuleRow`, `updateUpsertsNewlyActivatedModuleRow` (+ C6-deferral comment), and (follow-up
  per review SHOULD-FIX) `moduleExtendsModuleWritesFullModuleChainInFkOrder`. Observe module rows by querying the module table DIRECTLY
  (`retrieveRows()`) — C4-independent, purity-clean.

**Red→green:** all 3 (later 4) tests RED first (0 rows in module tables / Fk-order). First green attempt hit a reference-equality bug
(`contains` on SqlTable instances) → fixed to schema.table key-string compare.

**Verification (independent clean run, cleandb first, exit 0, `--console=plain`):** TestModuleArrayDiscriminator (19, incl. all 4 C3),
TestDiamondInheritance (13), TestInherits (2) — 34 tests, 0 fail/error/skip; also TestCrossSchemaForeignKeysAndInheritance (4),
MetadataPersistenceRoundtripTest (4), TestExtends (io, 1) green during implementation. spotless + PMD clean. No warnings in output.

**Review findings (independent, NO BLOCKERS):**
- ✅ key-string row filtering (not reference equality); ✅ `getSelectedRowValues` runs on module instance (module PK is FK ref, not
  AUTO_ID → AUTO_ID branch never evaluated for module cols, `getSelectedRowValues` correctly unchanged); ✅ module phase after is-a
  chain in both insert+update; ✅ PK copy-down no-op for modules (no AUTO_ID PK) → subset filtering safe; ✅ empty-discriminator =
  exact no-op (explicit guards); ✅ no DELETE in update (C6 deferred); ✅ validation union deduped by name.
- SHOULD-FIX (addressed): module-extends-module ancestor ordering shipped untested → follow-up lock test added.
- DEFERRED to C6: upsert NULLs unprovided module columns (= pre-existing is-a upsert behavior, not a C3 regression) — see C6 carry note.
- NIT (not fixed, low value/risk): `activeModuleTables` resolved twice per row (validation + routing) — O(rows×modules) redundant
  schema/table lookup; memoize only if write perf matters.

**Carry into C4:** GENERALIZE `tableWithInheritanceJoin` into the unified discriminator-driven join (see UNIFIED JOIN note) — module
subtype rows now EXIST on the shared root PK (C3), so C4 reads them back via the array axis (`'schema.Mod' = ANY(arr)`).

### C6 status (2026-06-13) — DONE & green, independently reviewed NO-BLOCKERS; S1/S2/N3 follow-ups applied; STAGED, NOT committed

Implemented test-first per the LOCKED C6 design (recon `.plan/notes/c6-hard-delete.md`). Files staged (3 — overlaps the C-stage set):
- `SqlTable.java` — `updateBatch` extended: (1) `selectPriorDiscriminatorArrays` (O-6 one batched in-tx SELECT of prior discriminator arrays by PK, BEFORE the update loop overwrites R's array column); (2) after the C3 `insertModuleRows` upsert, `deleteRemovedModuleRows` diffs `oldActive \ effectiveNewActive` per row (synthetic old/effective rows; **array-omission fix** = an axis the update did not provide uses its prior value → nets to zero removal), groups removed module tables, sorts CHILD-FIRST by `getAncestorsRootFirst().size()` desc, and calls the existing `deleteBatch` per removed module table (`tableType.isModule()` guard). Empty-discriminator ⇒ whole block skipped = is-a path byte-identical. Helpers `buildPkKey`/`buildPkKeyFromRow`/`resolveModuleTable` added.
- `SqlColumnExecutor.java` — **review S1 guard**: `validateModuleArrayValues` now rejects a MODULE_ARRAY column declared on a NON-root subtype (declaring table must equal its own root). Converts the latent prior-array-read SQL error into an early clear validation error; no capability change (canonical Model B = root declares axes). Fixture `mgTableclassAndModuleArrayCoexist` moved its axis columns to the root (`Animal`) to satisfy this — a requirement change from the new invariant, NOT a test weakening.
- `TestModuleArrayDiscriminator.java` — 6 C6-related tests (34 @Test total, 0 disabled): `updateRemovesDeactivatedModuleRow`, `deactivatedModuleColumnProjectsNullAfterDelete` (ties to C4 row-presence projection), `moduleExtendsModuleDeactivationRemovesFullChainInOrder`, `moduleExtendsModuleDeactivationKeepsSharedAncestorModule` (S2 — proves the removed set is the downward-closed DIFF, shared ancestor kept, not a chain-wipe), `mixIsAAndModuleArrayDeactivationLeavesIsAIdentityIntact` (N3 — asserts `mg_tableclass` value survives, not just the row), `moduleArrayColumnMustBeDeclaredOnRoot` (S1 red-green).

**Verification (exit 0):** TestModuleArrayDiscriminator (34), TestDiamondInheritance (13, UNMODIFIED), TestInherits (2, UNMODIFIED), 0 disabled; FULL regression after cleandb: `:molgenis-emx2-sql:test` all-pass (1 pre-existing unrelated skip) + `:molgenis-emx2:test` all-pass; spotless + pmdMain + pmdTest clean. (Agent textual test-counts were miscounts of console output — actual `@Test` counts verified by lead via grep; BUILD SUCCESSFUL/exit-0 on the full classes is the reliable signal.)

**Review verdict:** NO BLOCKERS. SHOULD-FIX S1 (root-declaration) + S2 (granularity test) and NIT N3 (mg_tableclass assertion) APPLIED. **DEFERRED nits (documented, NOT C6 regressions):** N2 composite-REF-PK key-matching (`buildPkKey` string compare fails SAFE = missed-not-wrongful deletion; no fixture exercises it); N4 `deleteBatch`/`insertModuleRows` rows not added to `updateBatch`'s returned count (pre-existing C3 pattern); N1 `resolveModuleTable` resolved twice per sort entry (cosmetic).

**Owner decision honored:** partial-module-update NULLing (omitted module columns → NULL on upsert) LEFT AS-IS (consistent with is-a; C6 strictly hard-delete). **The principled fix is the new "Merge-on-partial-update (PATCH semantics)" item** (owner agreed 2026-06-13 NOT to fold into C6) — retrieve+merge would preserve unprovided columns and retire this nit + the `getUpdateColumns` workaround.

**Phase C COMPLETE** (C0–C6 all DONE & green). Next phases (separate PRs): E (IO/GraphQL/RDF surface), F (frontend — see expanded bullet), H (real-model migration).

### Open decisions
- **O-1 module materialization storage + naming:** ❌ MOOT under Model B (2026-06-12) — no materialization; a MODULE is a real table bound to one root with its own real name. (Was: per-consumer reserved `mg_`-prefixed materialized table.)
- **O-2 materialization persistence:** ✅ derive from `values` (no new column/migration); add a marker only if reload needs it.
- **O-3 cross-schema TABLE subclasses:** ✅ already work cross-schema as-is; MULTIPLE composition is MODULE-only (C-4), so the old "TABLE-subtype as MULTIPLE option" question is moot.
- **O-4 mix inheritance + composition:** ✅ REVISED — **ALLOWED to coexist** (drops the suppress-`mg_tableclass` rule; the C0 loop handles mixed-cardinality discriminators uniformly). `getRootTable()` single-root invariant holds across all axes.
- **O-5 cross-discriminator overlap:** ✅ a given MODULE appears in at most ONE discriminator column per table.
- **O-6 update old-row read:** ✅ one extra in-tx `SELECT` of the prior array per update batch.
- **(naming) `SUBCLASS` vs `tableExtends`:** NOT unified — different layers (column-type vs declaration keyword) and `tableExtends` is frozen back-compat. Broader `inherits`/`extends`/`subclass`/`class` sprawl = optional later cleanup, NOT in C0.

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

> **SUPERSEDED by "Phase C REVISED (2026-06-12)" above.** Kept for history. The single-`mg_tableclass`
> cardinality-flip model below was replaced by the column-based, multi-discriminator engine (decision C-2
> retires the `mg_tableclass`-becomes-array approach).

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

> **ABSORBED into "Phase C REVISED (2026-06-12)" sub-stage C6.** Kept for history; the materialization
> design below is carried forward (now keyed off discriminator `values` rather than a separate module list).

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

> **ABSORBED into "Phase C REVISED (2026-06-12)".** Multi-axis is built in from sub-stage C0 (engine
> loops `getDiscriminatorColumns()`). Note `allowedSubclasses[]` is retired — `Column.values` IS the
> allowed set (decision #4). Kept for history.

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
- **Real-model smoke → Phase H (full migration):** prototype migrating one `subject` selector axis
  (e.g. `subgroups01`'s 5 disease groups) from `visible=` expressions to modules; confirm
  column-count/verbosity drop and equivalent form behavior. Then scale to the full `subject` table per
  **Phase H** (one `subject.csv` file, cross-module column order). This is the motivating end-to-end proof.
- **Frontend (Phase G, separate)**: subtype/variant picker; verify across themes/sizes per
  `frontend-conventions`.

## Out of scope (this plan)

- Cross-discriminator nesting (stays in user-space `visible=`).
- A subtype being simultaneously top-level AND a reusable option.
- Module columns referencing host-specific columns.
- Hard removal of the `extends` syntax (kept as back-compat sugar; deprecate later if desired).
- Full frontend redesign beyond Phase G outline.
