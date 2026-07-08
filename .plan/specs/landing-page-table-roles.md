# Landing Page Table Grouping by TableRole

## Context
Tables carry an optional table-level `role` (MAIN/DETAIL, default MAIN). DETAIL marks nested tables only meaningful via a parent record (e.g. Collection events, Subpopulations under Resources). The schema landing page groups — never hides — tables by role. Owner decision log: `.plan/decisions.md` 2026-07-05 entries.

## Behaviors

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Landing page renders four groups in DOM order: "data tables" (DATA + MAIN/unset, always), "detail tables" (DATA + DETAIL, v-if non-empty), "ontologies" (ONTOLOGIES + MAIN/unset, v-if), "detail ontologies" (ONTOLOGIES + DETAIL, v-if); nothing hidden | `apps/ui/app/pages/[schema]/index.vue` via `filterTablesByTypeAndRole` | `tests/vitest/utils/groupTablesByRole.spec.ts` (10 cases: null/undefined role = MAIN, case handling, empty input) | visual check (catalogue demo) |
| Search input filters all four groups; alphabetical sort within each group | `apps/ui/app/pages/[schema]/index.vue` | — | visual check |
| Table role survives CSV export/import roundtrip (lowercase `detail` accepted, exporter emits lowercase) | `Emx2.java` | `TestImportExportAllExamples.testSummaryAndDisplayColumnProperties` | — |
| Table role survives GraphQL mutation (case-insensitive input, e.g. `role:"detail"`) | `json/Table.java`, `SqlSchema.migrateTransaction` | `TestGraphqlSchemaFields.testSummaryAndDisplayInSchemaMetadata` | — |
| Catalogue model marks Collection events + Subpopulations as `detail` (table-metadata rows) | `data/_models/shared/{Collection events,Subpopulations}.csv` | `CatalogueTest` loaders (4 profiles) | visual check on demo landing |

## Deferred candidates (proposed by datamodeler review, not applied — owner call)
Subpopulation counts, Resource counts, Tables, Variables, Table mappings, Variable mappings — all keyed on Resources, only meaningful via a parent resource.

## Constraint
`role` on a table-metadata row is table-wide; it cannot be scoped per profile. Staging profiles sharing the row get the same grouping (acceptable since grouping shows, not hides).
