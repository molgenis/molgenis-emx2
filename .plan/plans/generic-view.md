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

#### Fix #1: Router Injection (CURRENT)

**Problem**: Runtime `require("#app/composables/router")` is fragile:
- Breaks in test environments
- Silent catch hides real errors
- Can't inject mocks for testing

**Solution**: Accept route/router as optional params, fallback to Nuxt composables.

**Interface Change**:
```ts
export interface UseFiltersOptions {
  debounceMs?: number;
  urlSync?: boolean;
  route?: { query: Record<string, string | string[] | undefined> };
  router?: { replace: (opts: { query: Record<string, unknown> }) => void };
}
```

**Implementation Steps**:
1. Add `route` and `router` to `UseFiltersOptions` interface (line 7-10)
2. Remove try/catch require block (lines 246-254)
3. Use injected route/router if provided, else try Nuxt composables:
   ```ts
   if (urlSyncEnabled) {
     route = options?.route ?? null;
     router = options?.router ?? null;
     if (!route || !router) {
       try {
         const nuxt = require("#app/composables/router");
         route = route ?? nuxt.useRoute();
         router = router ?? nuxt.useRouter();
       } catch {
         // Nuxt not available, URL sync disabled silently
       }
     }
   }
   ```
4. Update type annotations for route/router variables

**Test Updates**:
- Add test: `urlSync with injected route/router`
- Add test: `urlSync updates URL on filter change` (mock router.replace)
- Add test: `urlSync initializes from URL` (mock route.query)
- Add test: `urlSync without route/router provided` (graceful degradation)

**Files to Modify**:
- `composables/useFilters.ts`: Interface + init logic
- `tests/vitest/composables/useFilters.spec.ts`: Add URL sync tests

---

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

- [x] Router injection: works with mock in tests (Fix #1)
- [ ] Filter change updates URL (immediate) (Fix #5)
- [ ] Page load restores filters from URL (Fix #4)
- [ ] Multiple instances with different prefixes don't conflict
- [ ] Clear filters clears URL params
- [ ] Browser back/forward restores filter state (Fix #2)
- [ ] SSR hydration: no mismatch warnings (Fix #4)

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
