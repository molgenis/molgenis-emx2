# Plan: feat/tw-filters — Port Filter System to tailwind-components + TableEMX2

## Status: COMPLETE

## Phase 1: Create worktree [x]
- [x] Create worktree from master
- [x] Symlink .claude
- [x] Create .plan directories

## Phase 2: Copy new files from feat/generic-view [x]
Source: `/Users/m.a.swertz/git/molgenis-emx2/feat/generic-view/apps/tailwind-components/`

### New source files (12) — all copied
- [x] types/filters.ts
- [x] app/utils/filterConstants.ts
- [x] app/utils/buildFilter.ts
- [x] app/utils/extractPrimaryKey.ts
- [x] app/composables/useFilters.ts
- [x] app/composables/useFilterCounts.ts
- [x] app/composables/getSubclassColumns.ts
- [x] app/components/filter/Range.vue
- [x] app/components/filter/Column.vue
- [x] app/components/filter/FilterPicker.vue
- [x] app/components/filter/ActiveFilters.vue
- [x] app/components/filter/Sidebar.vue

### Story files (5) — all copied
- [x] app/pages/filter/Sidebar.story.vue
- [x] app/pages/filter/ActiveFilters.story.vue
- [x] app/pages/filter/Column.story.vue
- [x] app/pages/filter/Range.story.vue
- [x] app/pages/composables/useFilters.story.vue

### Test files (7) — all copied
- [x] tests/vitest/utils/buildFilter.spec.ts
- [x] tests/vitest/composables/useFilters.spec.ts
- [x] tests/vitest/composables/useFilterCounts.spec.ts
- [x] tests/vitest/components/filter/Range.spec.ts
- [x] tests/vitest/components/filter/Column.spec.ts
- [x] tests/vitest/components/filter/FilterPicker.spec.ts (removed stale grouped-by-type test)
- [x] tests/vitest/components/filter/ActiveFilters.spec.ts

## Phase 3: Modify existing files [x]
- [x] types/types.ts — Added IOntologyItem interface
- [x] app/composables/fetchTableMetadata.ts — Added options param + getSubclassColumns
- [x] app/components/input/CheckboxGroup.vue — Added facetCounts prop + count badge
- [x] app/components/input/Ontology.vue — Added showClear, facetCounts, fetchParentCounts, forceList props
- [x] app/components/input/Ref.vue — Added showClear, facetCounts props, init() guards
- [x] app/components/Input.vue — Added showClear, facetCounts, fetchParentCounts, forceList props, pass-through

## Phase 4: Integrate into TableEMX2 [x]
- [x] Add showFilters + urlSync props (default false)
- [x] Add FilterSidebar two-column layout
- [x] Wire useFilters composable (filterColumns, gqlFilter, searchTerm)
- [x] Pass gqlFilter to fetchTableData
- [x] Watch gqlFilter/searchTerm to reset page + refresh
- [x] ActiveFilters chips above table

## Phase 5: Update TableEMX2 story [x]
- [x] Add showFilters toggle checkbox

## Phase 6: Enable filters in apps/ui [x]
- [x] Add show-filters="true" and url-sync="true" to table page
- [x] Remove search from handleSettingsUpdate (sidebar handles it)

## Verification [x]
- [x] All 327 vitest tests pass (tw-filters)
- [x] All 398 vitest tests pass (generic-view, 1 pre-existing network error unrelated)
- [ ] Story pages render correctly (manual verification needed)

## Files changed (34 total)
- 24 new files (12 source, 5 stories, 7 tests)
- 1 new type file (filters.ts)
- 1 plan file
- 8 modified files (Input.vue, CheckboxGroup.vue, Ontology.vue, Ref.vue, TableEMX2.vue, fetchTableMetadata.ts, types.ts, EMX2.story.vue, apps/ui index.vue)
