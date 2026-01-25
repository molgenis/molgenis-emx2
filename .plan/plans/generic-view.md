# Generic View - Plan

## Completed
- v6.3.0: Filter system - FilterRange, FilterColumn, FilterSidebar, useFilters composable, buildFilter util, Emx2DataView, TableEMX2 filter prop (21 tests)
- v6.3.1: URL sync WIP - serialize/parse functions, bidirectional sync
- v6.3.2: Router injection - optional route/router params, fallback to Nuxt
- v6.3.3: One-way data flow - URL as source of truth, removed bidirectional sync complexity (64 tests)
- v6.3.4: REF path syntax - explicit `category.name=value` format, backward compat (69 tests)
- v6.3.5: Writable computed - filterStates writable via setter, Emx2DataView urlSync prop (default true), Story markdown spec, visibleColumns auto-default (225 tests)

## Current Status

URL sync complete:
- `filterStates` is writable computed (getter reads URL, setter updates URL)
- One-way flow maintained: URL → state, mutations → URL → recalculate
- REF filters use dotted keys: `category.name=Cat1|Cat2`
- Emx2DataView has `urlSync` prop (default: true)
- Story component supports markdown `spec` prop
- See `.plan/specs/generic-view.md` for full specification

## Remaining

### SSR Handling (TODO)

**Problem**: URL state exists server-side but computed may not work correctly during SSR.

Options:
1. Sync init (simple): Initialize from URL immediately, not in onMounted
2. useAsyncData (Nuxt-idiomatic): Wrap in useAsyncData for SSR support

Current code already initializes immediately when no component instance (tests). May work for SSR already - needs verification.

## Test Coverage

- [x] Basic filter operations (set/clear/remove)
- [x] URL sync with injected route/router
- [x] URL updates on filter change (immediate)
- [x] gqlFilter updates (debounced)
- [x] Browser back/forward updates filters
- [x] Graceful degradation (no router)
- [x] Reserved params preserved
- [x] Serialize/parse round-trips
- [x] REF with explicit path syntax (`category.name=value`)
- [x] Backward compat: fallback for old format (`category=value`)
- [ ] SSR hydration

## Remaining Stories

| Story | Status | Notes |
|-------|--------|-------|
| SSR verification | TODO | May already work |
| ActiveFilters | DEFERRED | Horizontal tag bar |
| E2E tests | TODO | After URL sync complete |

## Decisions

1. Container merged into Column (self-contained)
2. filterConfig removed (not needed)
3. Standardize on `null` (not `undefined`)
4. Large Vue files OK up to 750 lines
5. Keep useFilters separate from useTableData
6. URL format: key-value with explicit paths (`column.name=value`)
7. Router via injection, not runtime require
8. One-way data flow: URL is source of truth
9. No inline comments - use clear naming
10. Writable computed for filterStates (setter updates URL, maintains one-way flow)
11. Story specs in markdown (via `marked` library)

## Unresolved Questions

1. Handle ontology selections that exceed URL limits?
2. SSR pattern: verify current code works or needs adjustment?
