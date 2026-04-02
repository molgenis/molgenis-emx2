# Plan: YAML Profile Format v8

**Spec**: `.plan/specs/yaml-profile-format-v8.md`

## Context

EMX2 data models are defined in flat CSV files (`molgenis.csv`) with per-row comma-separated profile tags. The v8 proposal adds structured YAML: one file per table, structural sections, compositional profile subtables with blocks, import/override mechanics, and schema-level deployment configs. CSV with `tableExtends` remains fully supported.

---

## Architecture

### Subtables and blocks stored in table_metadata

Subtables and blocks are declared in a table's `profiles:` header. Both are stored as rows in `MOLGENIS.table_metadata`:
- Subtables are regular `TableType.DATA` tables
- Blocks are `TableType.BLOCK` — backend tables (FK to parent), but NOT shown in profile dropdown

Profile options = non-BLOCK children of the table with the PROFILE column. Self-referencing metadata — no separate options table needed.

### Subtables and blocks = backend tables (like tableExtends)

Both subtables and blocks become **child tables** extending the parent via FK to parent PK. Same mechanism as `tableExtends` today, but WITHOUT `mg_tableclass`. Blocks are also PG tables to avoid column duplication when shared across subtables.

Example: `Experiments` declares subtables WGS, Imaging and blocks sampling, sequencing:
- `Experiments` = parent table (PK: experiment id)
- `sampling` = block table (FK to Experiments, shared columns)
- `sequencing` = block table (FK to Experiments, shared columns)
- `WGS` = subtable table (FK via sampling→Experiments, contains WGS-specific columns)
- `Imaging` = subtable table (FK to Experiments, contains imaging columns)

### Profile selector = metadata-backed column

- `columnType: profile` (pick-one) → format flavor of STRING, stores subtable name
- `columnType: profiles` (pick-many) → format flavor of STRING_ARRAY, stores subtable names
- PROFILE/PROFILES extend STRING/STRING_ARRAY (not REF-based) — options derived from non-BLOCK children
- No `mg_tableclass` — the profile column IS the source of truth
- Backend manages subtable table rows on save

### Backend treats blocks identically to subtables

**CRITICAL**: `isBlock()` / `TableType.BLOCK` is a **UX-only distinction**. The backend NEVER branches on `isBlock()` for structural decisions (table creation, inheritance style, row management, querying). The SOLE decision factor for subtable-style vs old-style inheritance is: **does any ancestor have a PROFILE/PROFILES column?**

- Profile column in ancestor → subtable-style FK (no `mg_tableclass`)
- No profile column in any ancestor → old-style extends (with `mg_tableclass`) — backward compatibility

### Query behavior

Querying the parent table auto-joins the parent + all active subtable tables (LEFT JOIN), producing a wide sparse result. This is the same behavior as current `tableExtends` + `tableWithInheritanceJoin()`, minus `mg_tableclass`.

### Profiles scoped per table

Subtable names are globally unique within the schema (same namespace as table names). Block names only need to be unique within their declaring table.

---

## Current State

| Concept | Current Implementation |
|---|---|
| Model format | CSV (`molgenis.csv`) parsed by `Emx2.java` → `SchemaMetadata` |
| Table inheritance | `tableExtends` → FK to parent + `mg_tableclass` discriminator |
| TableType | Enum: `DATA`, `ONTOLOGIES` |
| table_metadata | Stores: table_schema, table_name, table_inherits, table_type, import_schema, etc. |
| ColumnType | 40+ types. SELECT/MULTISELECT extend REF/REF_ARRAY. No PROFILE type. |

### Key files

| File | Role |
|---|---|
| `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/ColumnType.java` | All column types |
| `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/TableType.java` | DATA, ONTOLOGIES |
| `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/TableMetadata.java` | `inheritName`, `profiles`, `tableType` |
| `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Column.java` | `profiles`, `visible`, `columnType` |
| `backend/molgenis-emx2/src/main/java/org/molgenis/emx2/SchemaMetadata.java` | Schema container |
| `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTableMetadataExecutor.java` | DDL, `executeSetInherit()` |
| `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlTable.java` | Row save/insert/update, `executeBatch()` |
| `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlQuery.java` | `tableWithInheritanceJoin()` |
| `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/MetadataUtils.java` | Metadata persistence to MOLGENIS schema |
| `backend/molgenis-emx2-io/src/main/java/org/molgenis/emx2/io/emx2/Emx2.java` | CSV metadata parser |
| `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlTableFieldFactory.java` | Dynamic GraphQL types |

---

## Design Decisions (from Phase 1+2 planning)

### inheritNames (String[]) — multi-parent inheritance

`inheritNames` is a String array. Each entry is a direct parent table name. This is true multi-parent inheritance — blocks are NOT treated specially at the DB level. `TableType.BLOCK` is only a UX hint (don't show in profile dropdown).

| Scenario | inheritNames | FK targets | Column inheritance |
|---|---|---|---|
| Old-style single | `["Person"]` | FK to Person | Person's columns |
| Block child | `["Experiments"]` | FK to Experiments | Experiments' columns |
| Multi-parent | `["sampling", "sequencing"]` | FK to sampling AND FK to sequencing | Both parents' columns merged |

Examples:
```
Employee.inheritNames = ["Person"]                    → single parent, old-style (mg_tableclass)
sampling.inheritNames = ["Experiments"]               → single parent, subtable-style (no mg_tableclass)
WGS.inheritNames = ["sampling", "sequencing"]         → multi-parent, FK to each direct parent
Imaging.inheritNames = ["Experiments"]                → single parent, subtable-style
```

Key methods:
- `getInheritedTables()` → `List<TableMetadata>` resolving ALL entries in inheritNames (no skipping, no BLOCK-walking)
- `getRootTable()` → follows first parent chain to the table with no parents
- `getSubclassTables()` → recursive walk with `LinkedHashSet` dedup
- `getIncludedBlockTables()` → returns BLOCK entries from inheritNames (UX helper)

DB migration: `table_inherits` varchar → varchar[].

### PROFILE/PROFILES as format flavors (not base types)

`PROFILE(STRING)` and `PROFILES(STRING_ARRAY)` — extends STRING/STRING_ARRAY. `getBaseType()` returns STRING/STRING_ARRAY → all switch statements work automatically. Zero switch changes needed.

### Both subtables AND blocks are PG tables

All child tables (whether DATA or BLOCK) become PG tables with FK to their direct parent(s). `TableType.BLOCK` is a UX-only distinction — at the DB level, blocks behave identically to subtables. Profile dropdown options = non-BLOCK children.

**Inheritance style decision** (single code path): `executeSetInherit()` checks `hasProfileColumnInAncestors(parent)`. If true → FK only (no `mg_tableclass`). If false → old-style extends (with `mg_tableclass`). No separate `executeSetSubtableInherit()` method — one method with a boolean parameter.

---

## Implementation Phases (Red-Green Approach)

### Phase 1: Java Metadata Model

**Goal**: Extend core metadata with BLOCK table type, PROFILE/PROFILES column types, inheritNames refactor.

#### RED: Failing tests first

- `ProfileMetadataTest.java` in `molgenis-emx2`:
  - Test: `TableType.BLOCK` exists (subtables are regular DATA tables)
  - Test: `ColumnType.PROFILE` has `getBaseType()==STRING`, `isReference()==false`, `isAtomicType()==true`
  - Test: `ColumnType.PROFILES` has `getBaseType()==STRING_ARRAY`, `isArray()==true`
  - Test: `setInheritNames("sampling","sequencing")` stores multiple entries
  - Test: `setInheritName("Person")` wraps as `["Person"]` (backward compat)
  - Test: `getInheritName()` returns first entry
  - Test: `getInheritedTable()` resolves transitively through blocks to DATA parent
  - Test: `getIncludedBlockTables()` returns only BLOCK entries from inheritNames
  - Test: `getSubclassTables()` on parent finds subtables+blocks transitively (deduped)

  - Test: `getProfileColumn()` finds PROFILE/PROFILES column on table
  - Test: `isBlock()` convenience method works

#### GREEN: Implementation

**Modified files**:
- `TableType.java` — add `BLOCK`
- `ColumnType.java` — add `PROFILE(STRING)`, `PROFILES(STRING_ARRAY)` as format flavors
- `TableMetadata.java` — `inheritName` String → String[], deduped `getSubclassTables()`, new helpers (`isBlock`, `getIncludedBlockTables`, `getProfileColumn`)

---

### Phase 2: SQL Layer

**Goal**: (sub)table+block tables created in PostgreSQL. Profile column drives row management. Querying parent auto-joins all child tables.

#### RED: Failing tests first

- `TestSubtables.java` in `molgenis-emx2-sql` (modeled on `TestInherits.java`):

  **Schema (pick-one)**:
  ```
  Experiments (DATA), PK: id, columns: name/date, PROFILE: experiment type
    Block sampling (BLOCK, inheritNames=["Experiments"]), columns: sample type/tissue type
    Block sequencing (BLOCK, inheritNames=["Experiments"]), columns: library strategy/read length
    Subtable WGS (DATA, inheritNames=["sampling","sequencing"]), columns: coverage
    Subtable Imaging (DATA, inheritNames=["Experiments"]), columns: modality/body part
  ```

  **Schema (pick-many)**:
  ```
  Observations (DATA), PK: obs id, PROFILES: observation types
    Subtable Dermatology (DATA, inheritNames=["Observations"]), columns: BSA/lesion type
    Subtable Neurology (DATA, inheritNames=["Observations"]), columns: motor score/cognitive score
  ```

  **Table creation**:
  - Test: subtables+blocks have PK+FK to parent (CASCADE), NO `mg_tableclass`
  - Test: all are real PG tables
  - Test: PROFILE is varchar, PROFILES is varchar[]

  **Row management (pick-one)**:
  - Test: insert with `experiment type:"WGS"` → rows in Experiments + sampling + sequencing + WGS
  - Test: insert with null profile → parent only
  - Test: update WGS→Imaging → sampling/sequencing/WGS rows deleted, Imaging created
  - Test: delete parent → CASCADE deletes all child rows
  - Test: invalid profile name → error

  **Row management (pick-many)**:
  - Test: insert with `observation types:["Dermatology","Neurology"]` → rows in both subtables
  - Test: remove "Neurology" → Neurology row deleted, Dermatology stays
  - Test: set empty → all subtable rows removed

  **Shared blocks**:
  - Test: WGS and WES both include sampling → independent sampling rows per parent row

  **Querying**:
  - Test: query parent → LEFT JOINs all child tables → wide sparse result
  - Test: filter on subtable column via parent → works
  - Test: existing `TestInherits` tests still pass (backward compat)

  **Metadata persistence**:
  - Test: subtable/block stored with correct `table_type`
  - Test: `inheritNames` persisted as varchar[] and round-trips

#### GREEN: Implementation

**Modified files**:
- `SqlTableMetadataExecutor.java` — `executeSetInherit()` gains boolean `addMgTableclass` parameter. When false: FK to parent PK with CASCADE, no mg_tableclass/trigger. No separate `executeSetSubtableInherit()` — one method. `hasProfileColumnInAncestors()` moves to `TableMetadata` (single location, no duplication).
- `SqlTable.java` / `executeTransaction()` — new `handleSubtableRows()`: after parent row save, compute effective table set from profile value + subtable includes, upsert/delete in child tables within transaction. Should reuse existing insert path where possible rather than manual upsert logic.
- `SqlQuery.java` — `tableWithInheritanceJoin()` and `whereConditionSearch()` adapted for multi-parent. Keep changes minimal — use `getInheritedTables()` / `getAllInheritedTables()` from `TableMetadata` instead of custom recursive collectors where possible.
- `MetadataUtils.java` — save/load `inheritNames` as varchar[]; `TABLE_INHERITS` type change.
- `Migrations.java` — `table_inherits` varchar → varchar[] migration.

**Review corrections (from code review of initial implementation)**:

| Issue | Fix |
|---|---|
| `isBlock()` used in `setInheritNames` / `executeCreateTable` to decide inheritance style | Remove. SOLE decision: `hasProfileColumnInAncestors(parent)`. Block is UX-only, backend never branches on it. |
| Separate `executeSetSubtableInherit()` vs `executeSetInherit()` | Merge into one `executeSetInherit(jooq, table, parents, addMgTableclass)`. Only difference: mg_tableclass column + trigger. |
| `hasProfileColumnInAncestors()` duplicated in SqlTableMetadata + SqlTableMetadataExecutor | Move to `TableMetadata` (core model). Single source of truth. |
| `existsInAnyParent()` is static with `tm` as first param | Make instance method on `SqlTableMetadata`. First param is `this`. |
| `setInheritNames` error message dumps whole list instead of specific table | Identify which specific table from `otherTable[]` was not found. |
| `setInheritTransaction` uses `inheritNames[0]` for old-style executeSetInherit | Correct for single-parent old-style, but confusing. Document clearly or pass full parent list. |
| `handleSubtableRows` / `insertChildSubtableRow` duplicates insert logic | Simplify to reuse existing `insertBatch` path where possible. |
| `SqlTable.getInheritedTables()` exists only for SqlTable cast | Valid need (insertBatch calls SqlTable methods), but document why. |
| `whereConditionSearch` has custom `collectAncestorSearchableTableNames` | Use `TableMetadata.getAllInheritedTables()` for ancestors. Only custom logic needed: block inclusion for search. |
| `parents.get(0)` in `executeCreateTable` silently ignores extra parents | Validate: if old-style and multiple parents, throw error. |
| No fallback to mg_tableclass when no profile columns | If no PROFILE column in any ancestor → old-style extends with mg_tableclass (backward compat). |

---

### Phase 3: YAML Parser

**Goal**: Parse v8 YAML table files into `SchemaMetadata` with parent + subtable + block tables.

#### RED: Failing tests first

- `Emx2YamlTest.java` in `molgenis-emx2-io`:
  - Test: parse minimal table YAML (tableName, sections, columns) → correct `SchemaMetadata`
  - Test: parse table with sections → SECTION columns at correct positions
  - Test: parse `columnType: profile` → PROFILE ColumnType
  - Test: parse `profiles:` with subtables → subtable tables with `inheritName` = parent, `tableType = DATA`
  - Test: parse `profiles:` with blocks → block tables with `tableType = BLOCK`
  - Test: `includes:` resolution → block columns merged into subtable tables
  - Test: subtable without `includes:` + direct section tag → columns on that subtable table
  - Test: shared sections (no `profiles:`) → columns on parent table
  - Test: column-level `profiles:` override → column placed on those subtable tables
  - Test: invalid YAML → clear error messages

- `YamlBlockResolverTest.java`:
  - Test: section-level import → block file merged as section
  - Test: section-level import with overrides (name, description, profiles, columns)
  - Test: column-level import → columns spliced in
  - Test: imported `profiles:` filtered to importing table's declared profiles
  - Test: circular import → error
  - Test: missing file → error

- `Emx2YamlSchemaTest.java`:
  - Test: explicit imports → loads table files
  - Test: wildcard `tables/*` → loads all YAML in directory
  - Test: `activeProfiles` filters subtables
  - Test: `settings:`, `permissions:`, `fixedSchemas:` parsed correctly

#### GREEN: Implementation

**New files**:
- `backend/molgenis-emx2-io/src/main/java/org/molgenis/emx2/io/emx2yaml/Emx2Yaml.java` — table parser
- `backend/molgenis-emx2-io/src/main/java/org/molgenis/emx2/io/emx2yaml/Emx2YamlSchema.java` — schema parser
- `backend/molgenis-emx2-io/src/main/java/org/molgenis/emx2/io/emx2yaml/YamlBlockResolver.java` — import resolution
- `backend/molgenis-emx2-io/src/main/java/org/molgenis/emx2/io/emx2yaml/YamlTableModel.java` — POJO
- `backend/molgenis-emx2-io/src/main/java/org/molgenis/emx2/io/emx2yaml/YamlSection.java` — POJO
- `backend/molgenis-emx2-io/src/main/java/org/molgenis/emx2/io/emx2yaml/YamlColumnModel.java` — POJO
- `backend/molgenis-emx2-io/src/main/java/org/molgenis/emx2/io/emx2yaml/YamlSchemaModel.java` — POJO
- Test YAML fixtures

**Parser output**: `SchemaMetadata` with parent tables, subtable tables (`inheritName` includes blocks/parent, `tableType = DATA`), block tables (`tableType = BLOCK`). Same object types as CSV parser produces — rest of system doesn't care about source format.

---

### Phase 4: GraphQL API

**Goal**: Expose profile metadata via GraphQL. PROFILE/PROFILES columns work in queries and mutations.

#### RED: Failing tests first

- `GraphqlProfileTest.java`:
  - Test: schema metadata query returns subtable/block declarations per table
  - Test: PROFILE column appears in table query type
  - Test: PROFILES column appears in table query type
  - Test: mutation with profile value → accepted
  - Test: query parent table → subtable columns included via join
  - Test: query subtable table directly → works
  - Test: `activeProfiles` reflected in schema metadata

#### GREEN: Implementation

**Modified files**:
- `GraphqlTableFieldFactory.java` — handle PROFILE/PROFILES in type generation (string/string-array field, not ref)
- `GraphqlSchemaFieldFactory.java` — expose profile definitions in `_schema` query
- `json/Table.java` — add profile definitions field
- `json/Column.java` — handle PROFILE/PROFILES type
- `json/Schema.java` — add `activeProfiles`

---

### Phase 5: IO Pipeline — Upload/Download API (analogous to CSV)

**Goal**: YAML upload and download via the existing IO pipeline, analogous to how CSV import/export works today.

#### RED: Failing tests first

- `YamlIoTest.java`:
  - Test: upload v8 YAML schema → creates parent + subtable tables with correct FKs
  - Test: upload with fixedSchemas → creates additional schemas
  - Test: upload with activeProfiles → only listed subtables created
  - Test: download schema as YAML → produces valid v8 YAML files
  - Test: roundtrip: upload YAML → download YAML → upload again → same schema
  - Test: existing CSV upload/download still works unchanged

#### GREEN: Implementation

**New file**:
- `backend/molgenis-emx2-datamodels/.../SchemaFromYamlSchema.java`

**Modified files**:
- `ImportProfileTask.java` — format detection (YAML vs CSV)
- `DataModels.java` — register v8 schemas
- `MolgenisIO.java` — YAML format detection, YAML export

---

### Phase 6: Frontend — Schema & Explorer Apps

**Goal**: PROFILE/PROFILES columns work in the UI. Needs analysis — at minimum `schema` and `explorer` apps need adaptation.

PROFILE/PROFILES are new base types (not SELECT/MULTISELECT), so frontend WILL need explicit handling. This phase will be painful but can be deferred until the backend is solid.

#### Analysis needed first

- `apps/schema/` — schema editor must understand BLOCK table type and PROFILE/PROFILES columns
- `apps/explorer/` — table explorer must handle the auto-joined wide sparse results, show/hide subtable columns based on profile value
- `apps/molgenis-components/` — form components need PROFILE dropdown / PROFILES multi-select rendering
- `apps/metadata-utils/` — TypeScript types need PROFILE, PROFILES, BLOCK

#### RED: Tests

- Vitest: PROFILE renders as dropdown, options from metadata (subtable names)
- Vitest: PROFILES renders as multi-select
- Vitest: selecting a profile shows/hides subtable-scoped form sections
- Vitest: schema editor shows subtable/block table types correctly
- Vitest: explorer handles sparse subtable columns

#### GREEN: Implementation (after analysis)

- `apps/metadata-utils/src/types.ts` — add PROFILE, PROFILES, BLOCK
- `apps/molgenis-components/.../FormInput.vue` — render PROFILE/PROFILES
- `apps/molgenis-components/.../formUtils.ts` — validation
- `apps/schema/` — subtable/block awareness
- `apps/explorer/` — profile-aware column display
- Options fetched from schema metadata endpoint (no data table query)

---

### Phase 7: Translate Existing Models (optional)

**Goal**: Create v8 YAML equivalents of existing CSV data models. CSV originals untouched.

- Create `data/_models_v8/` with YAML equivalents
- Create block files for shared column groups
- Create v8 schema files equivalent to existing `_profiles/*.yaml`
- `tableExtends` remains fully supported

---

## Key Design Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Subtables/blocks in table_metadata | Subtables = DATA, blocks = new BLOCK TableType | Profile options = non-BLOCK children of table with PROFILE column |
| PROFILE/PROFILES | New base ColumnTypes (not REF-based) | Options from metadata, not a data table; varchar storage |
| Subtables | Extend parent via FK, no mg_tableclass | Profile column is source of truth |
| Query parent | Auto-joins active subtable tables (LEFT JOIN) | Same as current tableExtends behavior, wide sparse result |
| Pick-many | Parent row in multiple subtable tables | Backend manages on save within transaction |
| Blocks | Stored in metadata, no PostgreSQL table | Composition tag only; freely reusable across tables |
| tableExtends | Fully supported in CSV path | Backward compat; v8 translates same pattern via profiles |

---

## Technical Risks (from code review)

| Risk | Severity | Mitigation |
|---|---|---|
| `executeSetInherit()` always adds `mg_tableclass` | High | New `executeSetSubtableInherit()` method |
| `tableWithInheritanceJoin()` uses `mg_tableclass` in join | High | Subtable join path must work without it (PK-only join) |
| `executeBatch()` has no hook for profile-driven child rows | Medium-High | New in-transaction logic; `TableListener` is post-tx only |
| PROFILE/PROFILES are new base types — every ColumnType switch needs handling | Medium | Audit all switch/if on ColumnType across sql, graphql, frontend |
| Column position numbering for CSV roundtrip | Medium | Parser mirrors CSV auto-increment |
| Subtable names share table namespace — 31 char limit | Low | Parser pre-validates with clear error |

## Open Questions

1. **activeProfiles**: Fixed at deploy time, or changeable at runtime?
2. **Block file resolution**: Classpath (JAR) or filesystem (git repo)?
3. **Format coexistence**: Both CSV and YAML indefinitely, or plan a CSV sunset?

---

## Verification Plan

After each phase:
1. `./gradlew :molgenis-emx2:test` — core model
2. `./gradlew :molgenis-emx2-sql:test` — SQL (the big one)
3. `./gradlew :molgenis-emx2-io:test` — parser
4. `./gradlew :molgenis-emx2-graphql:test` — API
5. `./gradlew test` — full suite (backward compat)
6. Manual: deploy v8 YAML schema, create row with profile, verify subtable table rows + query joins
