# Plan: Multiple Inheritance & YAML Profile Format v8

**Spec**: `.plan/specs/yaml-profile-format-v8.md`

## Context

EMX2 data models are defined in flat CSV files (`molgenis.csv`). This branch adds:
1. **Multiple inheritance** — a table can extend multiple parents via `tableExtends`
2. **PROFILE/PROFILES column types** — discriminator columns that drive which child tables get rows (replaces `mg_tableclass` for new-style inheritance)
3. **Structured YAML format** — one file per table, sections, imports (future phase)

CSV with `tableExtends` remains fully supported.

---

## Core Concept: Multiple Inheritance

The core backend feature is **multiple inheritance**. A table's `inheritNames` is a String array — each entry is a direct parent. FK is created to each parent. All parents must share the same root table.

Everything else is layered on top:

| Concept | What it is | Backend impact |
|---|---|---|
| Multiple inheritance | `inheritNames = ["sampling", "sequencing"]` | Core feature: FKs, column merging, recursive insert/update/delete |
| `mg_tableclass` | Old-style discriminator column (auto-managed) | Used when root has NO profile column (backward compat) |
| PROFILE column | New-style discriminator (user-visible, pick-one) | Same role as `mg_tableclass` but explicit. Extends STRING. |
| PROFILES column | Multi-value discriminator (pick-many) | Like PROFILE but extends STRING_ARRAY — rows in multiple children |
| `TableType.INTERNAL` | Table type for non-selectable child tables | **UX-only** — backend never branches on it. Means "don't show in profile selector dropdown". Was called BLOCK. |

### Terminology

- **Parent table** = any table listed in `inheritNames`
- **Child table** = any table whose `inheritNames` includes this table
- **Root table** = the top of the inheritance tree (no parents)
- **Internal table** = a child table marked `TableType.INTERNAL` (UX hint: not selectable by users). Previously called "block". Backend treats it identically to any other child table.
- **Profile column** = a PROFILE or PROFILES column on the root table that drives which child tables get rows on save

### How it works

```
Experiments (root, PK: id, columns: name/date, PROFILE: experiment type)
  ├── sampling (INTERNAL, extends Experiments, columns: sample type/tissue type)
  ├── sequencing (INTERNAL, extends Experiments, columns: library strategy/read length)  
  ├── WGS (extends sampling + sequencing, columns: coverage)
  └── Imaging (extends Experiments, columns: modality/body part)
```

- `sampling` and `sequencing` are **internal tables** — they group shared columns but users don't select them directly
- `WGS` and `Imaging` are **selectable child tables** — they appear in the profile dropdown
- When a user inserts with `experiment type = "WGS"`, `insertBatch` recursively creates rows in WGS → sampling → Experiments and WGS → sequencing → Experiments (with upsert for diamond)
- Querying Experiments auto-joins all children via LEFT JOIN (wide sparse result)

### Inheritance style decision

One check: `getRootTable().getProfileColumn() != null`
- **Yes** → new-style: no `mg_tableclass`, profile column is the discriminator
- **No** → old-style: `mg_tableclass` added to root (backward compat, single parent only)

---

## Key Files

| File | Role |
|---|---|
| `TableMetadata.java` | Core model: `inheritNames`, `getInheritedTables()`, `getAllInheritedTables()`, `getRootTable()`, `getSubclassTables()`, `getProfileColumn()`, `hasColumnInParent()`, `getQualifiedName()` |
| `SqlTableMetadataExecutor.java` | DDL: `executeSetInherit()` with `addMgTableclass` boolean |
| `SqlTableMetadata.java` | `setInheritNames()` validation: same root, no reroot, PK check |
| `SqlTable.java` | Row management: unified discriminator, `subclassRows` batching, per-row delete of non-matching children |
| `SqlQuery.java` | `tableWithInheritanceJoin()`: ancestor INNER JOIN + child LEFT JOIN (PK-only). `whereConditionSearch()`: self + ancestors |
| `MetadataUtils.java` | Persistence: `inheritNames` as varchar[] |
| `Emx2.java` | CSV parser: `tableExtends` as comma-separated |
| `GraphqlSchemaFieldFactory.java` | GraphQL: `inheritNames` as String[] list |
| `ColumnTypeRdfMapper.java` | RDF: PROFILE/PROFILES mapped to STRING |

---

## Implementation Phases

### Phase 1: Java Metadata Model — COMPLETE

Extended core metadata: `TableType.INTERNAL` (was BLOCK), `ColumnType.PROFILE/PROFILES`, `inheritNames` as String[], helper methods on `TableMetadata`.

### Phase 2: SQL Layer — COMPLETE

Multiple inheritance in PostgreSQL. Single `executeSetInherit()` method. Unified row management (same `subclassRows` batching for both mg_tableclass and profile discriminator). Simplified joins and search. Migration for `table_inherits` varchar → varchar[].

**Key design decisions implemented:**
- `getRootTable().getProfileColumn()` = single inheritance style check
- Per-row delete with keepSet (target + ancestors kept, rest deleted)
- PK-only joins (no mg_tableclass in join key)
- Search covers self + ancestors only
- All parents must share same root (validated)
- Can't reroot table with subclasses (validated)
- `getInheritedTables()` throws only when DB-backed, skips in-memory (cross-schema graceful handling)

### Phase 3: GraphQL API — complete remaining work

**Done so far**: `inheritNames` exposed as String[] list, 3 smoke tests pass.

**Remaining**:
- Verify PROFILE/PROFILES columns work in GraphQL queries and mutations
- Verify `TableType.INTERNAL` exposed correctly in schema metadata
- Verify mutations with profile values create correct child table rows
- Add smoke tests for any gaps

### Phase 4: IO Pipeline — complete remaining work

**Done so far**: CSV `tableExtends` round-trips comma-separated multi-parent. Smoke test passes.

**Remaining**:
- Verify `TableType.INTERNAL` round-trips in CSV export/import
- Verify all IO paths handle multi-parent (zip import, bulk upload)
- Add smoke tests for any gaps

### Phase 5: Frontend — Schema & Explorer Apps

**Goal**: PROFILE/PROFILES columns work in the UI.

- `apps/schema/` — schema editor: INTERNAL table type, PROFILE/PROFILES columns
- `apps/explorer/` — table explorer: wide sparse results, show/hide child columns based on profile
- `apps/molgenis-components/` — form components: PROFILE dropdown, PROFILES multi-select
- `apps/metadata-utils/` — TypeScript types: PROFILE, PROFILES, INTERNAL

### Phase 6: YAML Parser

**Goal**: Parse v8 YAML table files into `SchemaMetadata`.

- One file per table, sections, imports
- Section-scoped profiles
- Block import resolution
- Schema-level deployment configs

### Phase 7: Documentation

**Goal**: Update user-facing docs in `docs/molgenis/`.

**Files to update**:
- `docs/molgenis/use_schema.md` — multiple inheritance, INTERNAL table type
- `docs/molgenis/CSV.md` — `tableExtends` with comma-separated parents
- `docs/molgenis/dev_profiles.md` — PROFILE/PROFILES column types, row management
- `docs/molgenis/dev_architecture.md` — architecture overview

### Phase 8: Translate Existing Models (optional)

Create v8 YAML equivalents of existing CSV data models. CSV originals untouched.

---

## Verification

After each phase:
1. `./gradlew :backend:molgenis-emx2:test` — core model
2. `./gradlew :backend:molgenis-emx2-sql:test` — SQL (290 tests)
3. `./gradlew :backend:molgenis-emx2-io:test` — IO (68 tests)
4. `./gradlew :backend:molgenis-emx2-graphql:test` — GraphQL
5. `./gradlew :backend:molgenis-emx2-rdf:test` — RDF (103 tests)
6. Full suite for backward compat
