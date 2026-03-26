# Test Restructuring Plan

Baseline: 46 files, 387 tests, all green.
Final: 47 files, 382 tests, all green.

## Step 1: Create shared column fixtures ✅
- [x] Created `tests/vitest/fixtures/columns.ts` with canonical IColumn constants + `makeColumn()` builder
- [x] Replaced inline column definitions in: Column, buildFilter, Sidebar, FilterPicker specs
- [x] Tests: 387 pass

## Step 2: Create shared UseFilters mock factory ✅
- [x] Created `tests/vitest/fixtures/mockFilters.ts` with unified factory (reactive + stub modes)
- [x] Updated Sidebar.spec.ts and FilterPicker.spec.ts to use shared factory
- [x] Tests: 387 pass

## Step 3: Move pure-function tests from useFilters.spec.ts ✅
- [x] Created `tests/vitest/utils/filterUrlCodec.spec.ts` with 51 tests
- [x] useFilters.spec.ts now has only ~25 composable tests
- [x] Tests: 387 pass

## Step 4: Remove duplicate/redundant tests in Sidebar.spec.ts ✅
- [x] Deleted 1 duplicate "passes filters object to FilterPicker"
- [x] Deleted 5 smart-default tests (logic covered in computeDefaultFilters.spec.ts)
- [x] Deleted 2 URL sync tests (covered by filter toggle tests)
- [x] Tests: 379 pass (-8)

## Step 5: Move createCountFetcher tests from Ref.spec.ts ✅
- [x] Moved 1 createCountFetcher test from Ref.spec.ts to createCountFetcher.spec.ts
- [x] Tests: 379 pass

## Step 6: Fix broken assertions ✅
- [x] CheckboxGroup/RadioGroup: fixed bare `expect(emitted())` → `.toBeTruthy()`, fixed event triggers (focus→focusin, blur→focusout)
- [x] Listbox: fixed `it.each([options])` → `it.each(options)` (+2 tests)
- [x] Form.spec.ts: fixed mock path to correct absolute path
- [x] Tests: 382 pass (379 + 2 from Listbox fix + 1 existing)

## Step 7: Type fetchGraphql responses (FUTURE — separate PR)

Goal: catch mock↔code drift at compile time, catch backend↔code drift via e2e.

Currently `fetchGraphql` returns `Promise<any>`. Tests mock responses with untyped inline objects.
9 test files fake backend responses (101 tests total) with zero type safety.

### 7a. Add generic typing to fetchGraphql
- [ ] Change `fetchGraphql(schemaId, query, variables)` → `fetchGraphql<T>(schemaId, query, variables): Promise<T>`
- [ ] Only touches 1 file: `app/composables/fetchGraphql.ts`

### 7b. Define response types for GraphQL query patterns
- [ ] Create `types/graphqlResponses.ts` with interfaces for:
  - `IGroupByResponse<T>` — for `_groupBy` queries (used by createCountFetcher)
  - `IAggResponse` — for `_agg` count queries
  - `IOntologySizeProbe` / `IOntologyTermsResponse` — for Ontology two-phase fetch
  - `ISchemaQueryResponse` — for `$fetch` _schema queries
- [ ] Use these types in production code call sites (Ontology.vue, createCountFetcher.ts, etc.)

### 7c. Type test mock data
- [ ] Create `tests/vitest/fixtures/mockResponses.ts` with typed factory functions
- [ ] Replace inline mock response objects in: createCountFetcher.spec.ts, Ontology.spec.ts, Ref.spec.ts, Sidebar.spec.ts, Column.spec.ts
- [ ] TypeScript will now error if mocks don't match response interfaces

### 7d. E2e smoke tests for API contract anchoring
- [ ] Add 1 e2e test: filter sidebar renders with real backend (anchors _schema + metadata shapes)
- [ ] Add 1 e2e test: ontology input loads and displays terms (anchors ontology fetch shapes)
- [ ] Add 1 e2e test: ref input shows facet counts (anchors _groupBy/_agg shapes)
- [ ] These catch backend drift that TypeScript cannot

### Note on PR scope
Steps 7a-7d should be a **separate PR** to keep this PR focused on test restructuring.
The current PR changes are self-contained and reduce test maintenance burden.

## Future considerations (lower priority)
- Simplify gqlFilter assertions in useFilters.spec.ts that overlap with buildFilter.spec.ts
- Shared `$fetch` stub helper (duplicated in Sidebar.spec.ts and Column.spec.ts)
- Standardise mockRoute/mockRouter pattern across files
