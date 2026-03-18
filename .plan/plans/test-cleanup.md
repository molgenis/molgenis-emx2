# Plan: Test cleanup — filter system vitest suite

## Status: READY FOR APPROVAL

## Goal
Fix test bugs, remove bloat, improve coverage and readability across filter vitest files.

## Phase 1: High-priority fixes

### 1.1 Create `formatFilterValue.spec.ts`
- New file: `tests/vitest/utils/formatFilterValue.spec.ts`
- Test all operators: between (both/min/max/neither), in (empty/single/multi/objects), notNull, isNull, like, equals
- Test `extractDisplayValue`: object with name, label, neither
- Critical gap — this utility has zero tests

### 1.2 Fix dead assertion in Ref.spec.ts
- Line 67: `setTimeout` blur assertion never executes (not awaited)
- Either use `vi.useFakeTimers()` + `advanceTimersByTime` or remove the dead code

### 1.3 Fix vacuous test in Sidebar.spec.ts
- Line 439: conditional `if (mockRouter.replace.mock.calls.length > 0)` makes test pass when nothing happens
- Make assertion unconditional

### 1.4 Fix wrong test name in Ref.spec.ts
- Line 62: "deselect on non-array version should yield empty array" — actually emits `null`
- Rename to match actual behavior

### 1.5 Strengthen count refetch test in Ref.spec.ts
- Line 124: "re-fetches counts when crossFilter changes" only checks call count increased
- Assert the new call includes the updated crossFilter value

## Phase 2: Remove test bloat

### 2.1 useFilters.spec.ts
- Collapse 9 per-operator GQL tests to 2 representative cases (one simple, one complex)
- Remove duplicate URL-sync test ("should use injected route/router" is subset of "should update URL on filter change")
- Shrink `extractStringKey` block from 8 to 3 tests (keep: nested value, empty object, recursion limit)
- Trim round-trip suite: keep 3 representative cases, remove duplicates of buildFilter tests

### 2.2 Column.spec.ts
- Remove duplicate "clears modelValue when Clear is clicked" (keep one, in Dispatch logic)
- Remove both Integration lifecycle tests (duplicate coverage, fragile CSS selectors)
- Remove "renders REF column label" (same as label test for any type)
- Merge absent/false removable tests into one

### 2.3 buildFilter.spec.ts
- Remove duplicate OR-terms test ("preserves existing behavior without quotes")
- Merge notNull/isNull into `it.each`
- Remove "builds notNull filter for nested path" (nested wrapping already tested)
- Fix misleading "preserves 'and' in middle of word" test name

### 2.4 FilterPicker.spec.ts
- Merge 4 column exclusion tests into 1 `it.each`
- Remove "dropdown is closed by default" (implicit in "opens dropdown" test)
- Remove "shows unchecked checkboxes" (complement of checked test)
- Merge HEADING/SECTION exclusion in computeDefaultFilters

### 2.5 Sidebar.spec.ts
- Collapse 3 FilterPicker props tests into 1
- Remove refSchemaId passthrough test
- Remove removable=true test (constant, tested implicitly)

### 2.6 ActiveFilters.spec.ts
- Remove "shows single value display" (duplicate of chip text test)

### 2.7 createCountFetcher.spec.ts
- Merge 4 identical guard tests (dotted-path + empty-array for leaf/parent) into 2
- Drop standalone getCrossFilter describe (already covered)

## Phase 3: Add missing coverage

### 3.1 `like_or` / `like_and` operators
- Add tests in buildFilter.spec.ts for both operators
- Add tests in useFilters.spec.ts for serialize/parse round-trip

### 3.2 Edge cases in buildFilter.spec.ts
- `between [null, null]` → should skip filter
- `in []` → should skip filter
- `equals []` → should skip filter

### 3.3 Sidebar.spec.ts
- MAX_VISIBLE_FILTERS (25) cap enforcement
- fetchTableMetadata failure on mount → graceful degradation

### 3.4 createCountFetcher.spec.ts
- Error path for fetchOntologyLeafCounts
- Error path for fetchOntologyParentCounts

## Phase 4: Readability improvements

### 4.1 Timer management
- Standardize on `beforeEach`/`afterEach` for fake timers across all files
- Remove raw `setTimeout(resolve, 600)` in FilterPicker.spec.ts → use fake timers
- Fix inline `vi.useFakeTimers()`/`vi.useRealTimers()` in Ref.spec.ts

### 4.2 Test naming
- Remove `should` prefix inconsistency — pick one style per file
- Rename `extractStringKey` describe to `"serializeFilterValue with object values"`
- Fix misleading test names flagged in reviews

### 4.3 Sidebar.spec.ts
- Extract `waitForMetadataLoad()` helper for double `nextTick()` pattern
- Fix `toBeLessThanOrEqual(5)` → `toBe(5)` for defaults limit test

### 4.4 Minor
- Move `computeDefaultFilters` tests from FilterPicker.spec.ts to own spec file
- Extract shared column fixtures in buildFilter.spec.ts

## Estimated impact
- ~25 tests removed/merged (bloat reduction)
- ~20 tests added (formatFilterValue + missing coverage)
- Net: ~5 fewer tests, significantly better coverage and readability
