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
- [ ] **Phase C (REVISED 2026-06-12) — Unified column-based discriminator engine (MULTIPLE). C0–C4 DONE & green; C5 SUBSUMED BY C3 (locked by test); C6 remaining.** ABSORBS old Phase D (MODULE materialization) + Phase G (multiple discriminator columns) per locked owner decisions. The discriminator is a first-class COLUMN; `SUBCLASS_ARRAY` REPLACES the scalar `mg_tableclass`; a table may have several discriminator columns; values may reference MODULE classes OR TABLE subclasses (both stored table-per-type on root PK); deactivation HARD-DELETEs the subtype/module row. Staged C0–C6 (see "Phase C REVISED" section below). Retires decision #11 (`mg_tableclass`-becomes-array). **Progress:** C0 (abstraction+rename), C1 (MODULE_ARRAY DDL+value validation), C2 (MODULE = real subtype table, Model B), C3 (MULTIPLE write routing), C4 (MULTIPLE query projection — row-presence gating) all DONE & green & reviewed. C5 (visible/required gating) SUBSUMED BY C3's per-row validation union + locked by `requiredModuleColumnEnforcedOnlyWhenModuleActive`. NEXT = C6 (hard-delete on deactivation).
- [ ] **~~Phase D — `MODULE` tableType + per-host materialization~~** — ABSORBED into Phase C (sub-stage C6).
- [ ] **Phase E — IO / GraphQL / JSON / RDF surface.** EMX2 CSV, GraphQL metadata, JSON model; RDF emits multiple `rdf:type` for active modules. **Known gaps (do NOT lose):** `inheritId` scalar truncation in `json/Table.java` (~56) — names round-trip, ids carry parent[0] only; `TestMigration` (nonparallel module) not yet extended for migration33 array upgrade. (The `SqlSchema.merge` / `json` multi-parent NAME gap was found already-fixed + regression-tested 2026-06-11.) **Single-file / cross-module column-order requirement (owner 2026-06-13, for Phase H):** the EMX2 CSV importer must let a modeler declare a root + its `MODULE` tables + `MODULE_ARRAY` axis columns in ONE model file and control DISPLAY COLUMN ORDER spanning those module tables from that one file (modules are separate physical tables but authored together). Verify whether the current `position`/row-order semantics already give cross-table order control or whether a model-file-global ordering needs adding; this is a prerequisite for Phase H.
- [ ] **Update user-facing docs** (lands WITH Phase E — document the type only once a modeler can declare it via CSV/GraphQL). Targets: `docs/molgenis/use_schema.md` (data modeling — `MODULE_ARRAY` composition column, `MODULE` tableType, multi-parent `extends`), `docs/molgenis/CSV.md` (column-type reference + `values` semantics for ENUM/ENUM_ARRAY/MODULE_ARRAY), `docs/molgenis/semantics.md` (RDF multiple `rdf:type` for active modules). NOT in C1 (no user-declarable surface yet). NOTE: enum-family `values` membership is now ENFORCED (insert+update) — call this out in CSV.md so modelers know ENUM `values` are no longer advisory-only.
- [ ] **Phase F — Frontend redesign** (separate effort): subtype/variant picker UI; writing to root with discriminator selection.
- [ ] **~~Phase G — Multiple discriminator columns per table~~** — ABSORBED into Phase C (multi-axis built in from C0).
- [ ] **Phase H — Real-model migration: `subject` `visible=` → modules (the motivating use case).** Replace the ~1,000 `visible=` expressions / 118 unique predicates on the `subject` table in `data/patient_registry_demo/molgenis.csv` (e.g. `subgroups01?.name=="cs"` repeated across 155 columns) with `MODULE_ARRAY` composition columns (the modular axes — `subgroups01`, `panels`, etc.) + `MODULE` tables (one per column-group), so a `subject` row activates the modules it needs and the engine gates their columns (no per-column JS). **Hard requirement (owner 2026-06-13): keep EVERYTHING in ONE `subject.csv` model file** — root `subject` + all its `MODULE` tables + the `MODULE_ARRAY` axis columns declared together, so **column ORDER can be managed across modules** (display order spanning multiple module tables is authored in one place, not scattered per-file). Depends on: C3 (write) + C4 (query projection) + C5 (gating) so module columns show/hide correctly; **Phase E** for the EMX2 CSV surface (MODULE tableType, MODULE_ARRAY column type, `values`) AND for the importer to honor cross-module column order from a single file (see Phase E note). Deliverable: a migrated `subject.csv`, a before/after column-count + verbosity drop, and confirmation of equivalent form behavior (same columns visible per selector state). This is the end-to-end proof the whole mechanism replaces the `visible=` workaround. Owner of execution: `datamodeler` agent once the engine + CSV surface are ready.


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
- **C6 — Hard-delete on deactivation.** Diff old/new active set in `updateBatch`, DELETE removed module rows. Acceptance: dropping a module removes its row, root + other modules intact; AND a table with BOTH `extends`-identity and a `MODULE_ARRAY` works together (mix smoke).
  - **CARRY FROM C3 REVIEW (2026-06-13):** the C3 module upsert reuses the existing is-a SAVE/upsert path, which does NOT per-column filter non-refback columns by `columnsProvided` (`getInsertColumns` only filters refbacks) — so updating a row that activates a module but OMITS some module columns writes NULL over their prior values. This is identical to the pre-existing is-a upsert behavior (NOT a C3 regression), but C6 revisits update semantics → apply a `getUpdateColumns`-style `columnsProvided.contains(...)` filter to the module upsert path here if partial module update is needed.

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
