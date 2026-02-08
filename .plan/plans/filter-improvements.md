# Plan: Improved Filter Selection

## Problem
Current filter sidebar overwhelming with many columns. Users need:
- Fast add/remove filters
- Understand column context (sections, types)
- See counts before applying filter
- Click values to filter immediately

## Current State (v6.3.14)
Already built on feat/generic-view:
- `FilterSidebar.vue` (175 lines): shows filterable columns, nested ref expansion
- `FilterColumn.vue` (190 lines): per-column filter UI with type detection
- `ActiveFilters.vue` (253 lines): filter pills with clear buttons
- `Columns.vue` (328 lines): column/filter visibility toggle (mode="columns"|"filters")
- `useFilters.ts` (437 lines, 925 tests): URL sync, GraphQL filter building
- `Ontology.vue` (798 lines): tree-based ontology input with lazy loading
- `Emx2DataView.vue` (456 lines): unified view integrating all above
- GraphQL: `Table_groupBy(filter:...) { count }` works

### How Filter Visibility Works Today
1. `Columns.vue` in `mode="filters"` toggles `column.showFilter` property
2. `Sidebar.vue` shows columns where `showFilter !== false` (excludes HEADING/SECTION/mg_*)
3. No smart defaults - all filterable columns shown unless manually toggled off
4. No priority ordering by type

## Design Decision
**Extend, don't replace.** `Columns.vue` stays as-is. New `FilterPicker.vue` added alongside.

## Features

### 1. FilterPicker Component (Priority: High)
**New component** - searchable dropdown for quick add/remove of filters

**UX Flow:**
1. "Add filter" button in Sidebar header → dropdown opens
2. Type to search columns by name
3. Columns grouped by section/heading
4. Click column → `showFilter` toggled on, filter appears in sidebar
5. Already-visible filters show checkmark
6. Click outside or Escape to close

**Type grouping within sections:**
| Type | Priority | Visual |
|------|----------|--------|
| ONTOLOGY/ONTOLOGY_ARRAY | 1 | Tag icon |
| REF/REF_ARRAY | 2 | Link icon |
| INT/DECIMAL/LONG | 3 | Hash icon |
| DATE/DATETIME | 4 | Calendar icon |
| STRING/TEXT | 5 | Text icon |
| BOOL | 6 | Toggle icon |

### 2. Smart Default Filters (Priority: High)
**Problem:** No sensible defaults, all columns shown
**Solution:** Auto-select first N ontology/ref columns

- Default: first 5 ontology columns (from metadata order)
- If < 5 ontology, fill with ref columns
- User overrides persist via URL param `?mg_filters=col1,col2,col3`
- "Reset to defaults" in picker
- Empty `mg_filters` = use defaults

### 3. Filter Visibility in URL (Priority: High)
**Problem:** Filter selection not bookmarkable
**Solution:** `mg_filters` URL param (uses mg_ prefix to match existing convention)

- `?mg_filters=col1,col2,col3` - which filters are visible
- Separate from filter values (`?col1=value`)
- Managed in Sidebar, synced via router
- Absent = use smart defaults

### 4. Faceted Counts (Priority: High, Complex)
**Problem:** User doesn't know filter impact until applied
**Solution:** Show counts per filter option

For ontology/ref filters:
```
[ ] Diabetes (234)
[ ] Cancer (156)
    [ ] Lung cancer (42)
    [ ] Breast cancer (89)
```

**Implementation:**
- New `useFilterCounts.ts` composable
- Uses `Table_groupBy(filter: currentFilters) { count, column { name } }`
- Fetches on: filter expand, filter state change
- Only for visible/expanded terms (lazy)
- Debounced to avoid query storms
- Ontology: try flat counts first, frontend parent rollup if needed

### 5. Click-to-Filter from Values (Priority: Medium)
**Problem:** User sees value, wants to filter by it
**Solution:** Clickable values in table/cards

- Hover on categorical value → filter icon appears
- Click → filter added with that value (+ filter made visible if hidden)
- Works for: ontology, ref columns
- Emits event up to Emx2DataView which updates filterStates

### 6. Path-Based Filtering Default (Priority: Medium)
**Already exists:** `_match_any_including_children` operator
**Enhancement:** Make it the default for ontology filters

- Selecting "Cancer" includes all children automatically
- Toggle: "Include children" (default on)

## Architecture

### Principle: Extend, Don't Replace
- `Columns.vue` stays unchanged (handles column visibility + sort)
- `FilterPicker.vue` is new, focused only on filter visibility
- `Sidebar.vue` gets minor additions (FilterPicker integration, smart defaults)
- `useFilters.ts` extended with `mg_filters` URL param support

### Component Hierarchy
```
Emx2DataView
  └─ FilterSidebar
       ├─ FilterPicker (NEW - "Add filter" dropdown)
       │    └─ Searchable column list grouped by section
       ├─ FilterColumn (existing - per visible filter)
       │    └─ Input → Ontology/Ref/Range (existing)
       └─ ActiveFilters (existing - pills)
```

### Data Flow
```
FilterPicker toggles column.showFilter
  → Sidebar recomputes filterableColumnsComputed
  → URL updated with mg_filters param
  → Filters appear/disappear in sidebar
```

## Implementation Phases

### Phase 1: FilterPicker + Smart Defaults + URL Sync
**Status:** Complete

**Components:**
- `filter/FilterPicker.vue` (179 lines) - searchable dropdown with expand/collapse for REF columns
- `filter/FilterPicker.story.vue` (63 lines) - story with DemoDataControls for backend testing
- `tests/vitest/filter/FilterPicker.spec.ts` (375 lines) - comprehensive unit tests
- `filter/Sidebar.vue` - integrated FilterPicker, smart defaults, mg_filters URL sync
- `filter/Column.vue` - always expanded (no collapse), Clear/Remove buttons
- `tests/vitest/filter/Column.spec.ts` (357 lines) - updated tests for no-collapse behavior
- Smart default logic: first 5 ONTOLOGY/ONTOLOGY_ARRAY, fill remaining with REF/REF_ARRAY

**Design decisions:**
- FilterPicker uses checkboxes for all columns
- REF columns get expand caret for nested field selection
- Nested filter labels use dot notation: `Parent.child`
- FilterColumn `removable` prop + `remove` emit - shows "Remove" link in header
- FilterColumn `labelPrefix` prop for nested labels (e.g., "Hospital.")
- Filters always expanded (no collapse/expand toggle) - FilterPicker controls visibility
- Removing a filter (via FilterPicker or Remove button) also clears its filter value
- Resetting to defaults clears values for removed filters
- Clearing a filter value (Clear button or ActiveFilters X) keeps filter column visible
- URL nested REF encoding: 3-segment `order.pet.name=spike` (column.nestedCol.refField)
- `mg_filters` URL param: comma-separated visible filter IDs, omitted when equals defaults
- Sidebar search uses default size, FilterPicker dropdown search uses `size="tiny"`
- Tooltips via `v-tooltip.right` on rows and headings
- Story uses DemoDataControls for real backend data testing

**Missing tests:**
- No Sidebar.spec.ts yet (complex: needs router mocks, async metadata fetching)

### Phase 2: Faceted Counts
**Goal:** Show option counts in filter inputs

**Files to create:**
- `composables/useFilterCounts.ts` - groupBy query builder + caching

**Files to modify:**
- `input/Ontology.vue` - optional `counts` prop, display in TreeNode
- `input/TreeNode.vue` - count badge next to label
- `filter/Column.vue` - fetch counts for visible filters, pass to inputs

### Phase 3: Click-to-Filter
**Goal:** Click values in table/cards to filter

**Files to modify:**
- Value display components - add hover state + filter icon
- `display/Emx2DataView.vue` - handle click-to-filter events

## Decisions
1. **Columns.vue stays as-is** - no breaking changes
2. **FilterPicker is new** - focused, single-purpose
3. **Nested ref depth:** 2 levels max (existing behavior)
4. **Counts fetching:** Lazy, debounced, only visible terms
5. **String/text filters:** Lower priority in picker, not in defaults
6. **URL param:** `mg_filters` (matches mg_ convention)
7. **Default filters:** First 5 ontology, fill with ref if needed
8. **Mobile:** FilterPicker same dropdown, responsive width

## Out of Scope (Future)
- Saved filter presets per user
- Filter templates per table (admin-configured)
- Tree view in FilterPicker (sections are enough for now)
- Nested ref browsing in FilterPicker (use Sidebar's existing expand)
