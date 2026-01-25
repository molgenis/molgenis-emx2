# Generic View - Plan

## Completed
- v6.3.0: Filter system - FilterRange, FilterColumn, FilterSidebar, useFilters composable, buildFilter util, Emx2DataView, TableEMX2 filter prop (21 tests)
- v6.3.1: URL sync initial impl - serialize/parse functions, bidirectional sync (staged, pending review fixes)

## Current: URL Sync - Review Fixes

**Status**: Implementation started, review feedback pending.

**Goal**: Persist filter state to URL for sharing/bookmarking.

**Files**:
- `composables/useFilters.ts` - add urlSync, urlPrefix options
- `tests/vitest/composables/useFilters.spec.ts` - add URL sync tests

**Reference**: `apps/catalogue/app/utils/filterUtils.ts` (JSON approach)

### Review Feedback (To Fix)

1. **Router injection** - Replace `require("#app/composables/router")` with optional params
   - Accept `route` and `router` as optional constructor params
   - Improves testability, removes runtime require
   - Pattern: `useFilters(columns, { urlSync: true, route, router })`

2. **Infinite loop risk** - Consolidate watch handlers
   - Current: separate watches for filterStates, gqlFilter, route.query
   - Fix: single sync point, clear state machine for direction

3. **REF primary key** - Use explicit `column.name` syntax
   - Current: `?category=Cat1|Cat2` (assumes primary key)
   - Fix: `?category.name=Cat1|Cat2` (explicit path)
   - Enables future nested queries: `?parent.child.name=value`

4. **SSR handling** - Prevent hydration mismatches
   - URL state exists server-side but init happens client-side
   - Fix: init synchronously if SSR, or use `useAsyncData` pattern

5. **URL sync timing** - Separate debouncing
   - URL update: immediate (user expects instant feedback)
   - gqlFilter: debounced (API rate limiting)
   - Current code debounces both together

### Test Requirements

- [ ] Filter change updates URL (immediate)
- [ ] Page load restores filters from URL
- [ ] Multiple instances with different prefixes don't conflict
- [ ] Clear filters clears URL params
- [ ] Browser back/forward restores filter state
- [ ] SSR hydration: no mismatch warnings
- [ ] Router injection: works with mock in tests

## Remaining Stories

| Story | Status | Notes |
|-------|--------|-------|
| URL sync fixes | IN PROGRESS | Address review feedback |
| ActiveFilters | DEFERRED | Horizontal tag bar |
| E2E tests | TODO | After URL sync |

## Decisions

1. Container merged into Column (self-contained)
2. filterConfig removed (not needed)
3. Standardize on `null` (not `undefined`)
4. Large Vue files OK up to 750 lines
5. Keep useFilters separate from useTableData
6. URL format: key-value with explicit paths (`column.name=value`)
7. Router via injection, not runtime require

## Unresolved Questions

1. Handle ontology selections that exceed URL limits?
2. SSR pattern: sync init vs useAsyncData?
