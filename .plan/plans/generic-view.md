# Generic View - Plan

## Current Status
Filter system committed (e28f16c62). Next: URL sync.

## v6.3.0 Summary (DONE)
- FilterRange, FilterColumn, FilterSidebar components
- useFilters composable with debounced GraphQL output
- buildFilter utility with IGraphQLFilter types
- Emx2DataView unified component (slots, displayOptions)
- TableEMX2 filter prop
- 21 unit tests

## Next Story: URL Sync

**Goal**: Persist filter state to URL for sharing/bookmarking.

**Files**:
- `composables/useFilters.ts` - add urlSync, urlPrefix options
- `tests/vitest/composables/useFilters.spec.ts` - add URL sync tests

**Reference**: `apps/catalogue/app/utils/filterUtils.ts` (JSON approach)

**Test requirements**:
- [ ] Filter change updates URL
- [ ] Page load restores filters from URL
- [ ] Multiple instances with different prefixes don't conflict
- [ ] Clear filters clears URL params

## Remaining Stories

| Story | Status | Notes |
|-------|--------|-------|
| URL sync | TODO | Next |
| ActiveFilters | DEFERRED | Horizontal tag bar |
| E2E tests | TODO | After URL sync |

## Decisions

1. Container merged into Column (self-contained)
2. filterConfig removed (not needed)
3. Standardize on `null` (not `undefined`)
4. Large Vue files OK up to 750 lines
5. Keep useFilters separate from useTableData

## Unresolved Questions

1. URL format: JSON array vs key-value params?
2. Handle ontology selections that exceed URL limits?
