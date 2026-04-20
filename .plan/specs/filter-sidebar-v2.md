# Filter System v2 — UI Behavior Spec

Living spec. Single source of truth for expected behaviors.

## Architecture

Composable-driven. `useFilters` owns all state and counts. Components are thin display layers.
Counting centralized in useFilters. Parallel API calls per column.

**Filter rendering: use Tree directly for ALL countable types, NOT Ontology.vue/Ref.vue inputs.**
Ontology.vue and Ref.vue are form-editing inputs (complex selection models, own data fetching, pagination).
For filtering we need simpler: useFilters provides counted options, a thin `Column` component renders them.
- ALL countable types (ONTOLOGY, ONTOLOGY_ARRAY, BOOL, RADIO, CHECKBOX) → Tree with checkboxes
  - Flat types (BOOL/RADIO/CHECKBOX) are trees without children — same component, simpler code
  - Ontology types preserve hierarchy
- Range types (INT, DECIMAL, DATE, DATETIME, etc.) → FilterRange with appropriate inputs
- String-like → text input

Concerns addressed:
- **Counting takes time**: show loading skeleton per filter section; tree renders once counts arrive
- **Large tree (>25 nodes)**: collapse tree nodes by default, show top-level only; search-within-filter for finding deep terms
- **Small tree (≤25 nodes)**: all nodes expanded by default — no clicking through a tiny tree
- **Search in tree**: client-side filter of the counted options tree; Tree already supports this

Existing Ontology.vue and Ref.vue stay untouched — they remain form inputs only.

**Countable types (facet counts):** ONTOLOGY, ONTOLOGY_ARRAY, BOOL, RADIO, CHECKBOX
**Range types (min/max):** INT, INT_ARRAY, DECIMAL, DECIMAL_ARRAY, LONG, NON_NEGATIVE_INT, NON_NEGATIVE_INT_ARRAY, DATE, DATE_ARRAY, DATETIME, DATETIME_ARRAY
**Navigable types (expand in picker, not directly filterable):** REF, REF_ARRAY, REFBACK, SELECT, MULTISELECT
**String-like (shown in picker by default):** STRING, STRING_ARRAY, TEXT, EMAIL, HYPERLINK, UUID, AUTO_ID
**Excluded from defaults:** HEADING, SECTION, FILE, mg_* columns
**mg_* columns:** hidden by default but revealed when user searches in filter picker (advanced users)

**filterColumns logic lives in useFilters** — consumers pass raw columns; useFilters handles filtering/defaulting internally.

## Filter Sidebar

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Shows INITIALLY only countable + range filters that have any options with count > 0 | Sidebar.vue | Sidebar.spec.ts; filter-sidebar.spec.ts (e2e): "countable filter sections with zero base count are hidden initially" | visual check |
| First 5 filters uncollapsed, rest collapsed | Sidebar.vue | Sidebar.spec.ts | visual check |
| Filter sections with URL-hydrated filters always start expanded (override collapse) | Sidebar.vue | Sidebar.spec.ts | visual check |
| Chevron toggle per section, `<hr>` dividers between sections | Sidebar.vue | Sidebar.spec.ts | visual check |
| Lazy mount: filter content only renders when section expanded (`v-if`) | Sidebar.vue | Sidebar.spec.ts | visual check |
| Global search input at top (uses backend `_search`) | Sidebar.vue | Sidebar.spec.ts | visual check |
| "Customize" button with Filter icon opens Picker modal | Sidebar.vue | Sidebar.spec.ts | visual check |
| Show/hide sidebar: button in table toolbar (outline, filter-alt icon, matches Columns button) | TableEMX2.vue | - | visual check |
| Button label: "Hide filters" when visible, "Show filters" when hidden | TableEMX2.vue | - | visual check |
| Sidebar visibility controlled by parent via v-show (no internal toggle) | Sidebar.vue | - | - |
| Table expands to full width when sidebar hidden | TableEMX2.vue | - | visual check |
| Visible filter set persisted in URL via `mg_filters` param | useFilters.ts | useFilters.spec.ts | - |
| Collapse state persisted in URL via `mg_collapsed` param (comma-separated IDs); first-5 rule applied when param absent | Sidebar.vue | Sidebar.spec.ts | - |
| Styling matches catalogue: `p-5` padding, `<h3>` headings, `caret-up` icon | Sidebar.vue | - | visual check |
| Sidebar is narrow (w-80, 320px) | TableEMX2.vue | - | visual check |
| Range filter inputs stacked vertically — Min row above Max row | filter/Range.vue | - | visual check |
| DateTime input width 14em (Date stays 10em) | DateTime.vue | - | visual check |
| Styling works in all 5 themes (Light, Dark, Molgenis, UMCG, AUMC) | Sidebar.vue | - | visual check |

## Column Component (NEW — thin rendering wrapper)

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Receives counted options from useFilters (no own data fetching) | Column.vue | Column.spec.ts | - |
| ALL countable types (ONTOLOGY, ONTOLOGY_ARRAY, BOOL, RADIO, CHECKBOX): renders Tree with checkboxes | Column.vue | Column.spec.ts | visual check |
| Flat types (BOOL/RADIO/CHECKBOX) rendered as Tree nodes without children | Column.vue | Column.spec.ts | visual check |
| BOOL filter shows three options: true, false, null (with counts) | fetchCounts.ts | fetchCounts.spec.ts | visual check |
| Ontology types rendered as Tree preserving hierarchy | Column.vue | Column.spec.ts | visual check |
| Range types: renders FilterRange with existing InputDate/InputDateTime for date types, number input for numeric | Column.vue | Column.spec.ts | visual check |
| Min/max labels use `text-search-filter-group-title` for theme consistency | Column.vue | Column.spec.ts | visual check |
| STRING-like: renders text input | Column.vue | Column.spec.ts | visual check |
| Shows loading skeleton while counts are being fetched | Column.vue | Column.spec.ts | visual check |
| Search input always visible when >25 options or tree has children (no toggle button) | Column.vue | Column.spec.ts | visual check |
| Small trees (≤25 total nodes): all nodes start expanded | Tree.vue | Tree.spec.ts | visual check |
| Selecting a child node does NOT collapse/reset expand state of other nodes (expand state preserved across node rebuilds) | Tree.vue | Tree.spec.ts | visual check |
| Expand state is local component state only — NOT persisted in URL | Tree.vue | - | - |
| Emits selection changes; useFilters handles state update | Column.vue | Column.spec.ts | - |

## Filter Options Display

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Default: only show options with initial count > 0 | useFilters.ts | useFilters.spec.ts | - |
| When other filters applied: counts update but initial terms stay visible | useFilters.ts | useFilters.spec.ts | - |
| Options that reach count 0 due to cross-filter stay visible (merged with base counts, show "0") | useFilters.ts | useFilters.spec.ts | visual check |
| On search within filter: show all matching terms regardless of count | Column.vue | Column.spec.ts | visual check |
| On clear filters: go back to showing only initial count > 0 | useFilters.ts | useFilters.spec.ts | - |
| No facet counting on REF/REF_ARRAY (use nested filters via picker instead) | - | - | - |
| Countable filter with zero options (after load) shows "No matching values for this filter" message | Column.vue | Column.spec.ts | visual check |
| Empty-state message only renders when `!loading && options.length === 0` for countable filters (not range/text) | Column.vue | Column.spec.ts | - |

## Facet Counting (Centralized)

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Counts managed centrally in useFilters, not per-component | useFilters.ts | useFilters.spec.ts | - |
| Parallel API calls per column (don't wait for slowest) | useFilters.ts | useFilters.spec.ts | - |
| Cross-filter: each column's count excludes its own filter | useFilters.ts | useFilters.spec.ts | - |
| Initial (base) counts fetched once, determine which options/filters to show | useFilters.ts | useFilters.spec.ts | - |
| Base counts also shown in sidebar section headers (how many records match) | Sidebar.vue | Sidebar.spec.ts | visual check |
| Count updates debounced when filters change | useFilters.ts | useFilters.spec.ts | - |
| ONTOLOGY (single): `_groupBy` for direct counts; ancestor chain resolved via `_match_any_including_parents` in one query; parent counts rolled up client-side (sum of children) | fetchCounts.ts | fetchCounts.spec.ts | - |
| ONTOLOGY_ARRAY: `_groupBy` for direct counts; parent counts via `_agg` with `_match_any_including_children` (avoids double-counting) | fetchCounts.ts | fetchCounts.spec.ts | - |
| Empty branches pruned: parent nodes with 0 count and no counted descendants are hidden | fetchCounts.ts | fetchCounts.spec.ts | - |
| BOOL counts use `_groupBy` per column (scalar true/false/null) | useFilters.ts | useFilters.spec.ts | - |
| RADIO/CHECKBOX counts use `_groupBy` with key field expansion via getColumnIds | fetchCounts.ts | fetchCounts.spec.ts | - |
| RADIO/CHECKBOX filters use `_match_any` operator (backend resolves against primary key) | buildFilter.ts | buildFilter.spec.ts | - |
| RADIO/CHECKBOX single-key: plain string values, flat URL (`status=active`) | filterUrlCodec.ts | filterUrlCodec.spec.ts | - |
| RADIO/CHECKBOX composite-key: key objects with `keyObject` on CountedOption, JSON in URL when needed | fetchCounts.ts, Column.vue | fetchCounts.spec.ts, Column.spec.ts | - |
| STRING/TEXT filters use `like` operator (no ref key issues) | buildFilter.ts | buildFilter.spec.ts | - |
| RANGE filters use `between` operator with min/max (no ref key issues) | buildFilter.ts | buildFilter.spec.ts | - |

## Filter Picker (Modal)

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Opens as modal dialog from "Customize" button | Picker.vue | Picker.spec.ts | visual check |
| Shows all column types except mg_* (unless searching); STRING-like columns shown by default | Picker.vue | Picker.spec.ts | visual check |
| Search reveals mg_* columns that match | Picker.vue | Picker.spec.ts | - |
| Modal is 90% screen width (100% on mobile) | Picker.vue | - | visual check |
| Each column shows: InputCheckboxIcon + label + description (truncated, tooltip) + columnType badge | Picker.vue | Picker.spec.ts | visual check |
| Uses InputCheckboxIcon (same as Tree) not plain HTML checkbox | Picker.vue | - | visual check |
| Description truncated to one line with tooltip (does not overrun type badge) | Picker.vue | - | visual check |
| Column type shown as small badge/tag (e.g. "ONTOLOGY", "BOOL") | Picker.vue | - | visual check |
| REF/REF_ARRAY shown as expandable rows with caret (same size as checkbox) + "→ tableName" | Picker.vue | Picker.spec.ts | visual check |
| REF expands 2 levels deep, REF_ARRAY/REFBACK 1 level deep | Picker.vue | Picker.spec.ts | - |
| Self-referencing back-refs to parent table excluded | Picker.vue | Picker.spec.ts | - |
| Expanded ref shows sub-columns indented with same checkbox + description + type layout | Picker.vue | - | visual check |
| Selecting nested column adds filter as "root → child [→ child]" label in sidebar | Picker.vue, Sidebar.vue | Picker.spec.ts, filter-sidebar.spec.ts (e2e) | verified |
| Shows selected state (checkmarks via InputCheckboxIcon) | Picker.vue | Picker.spec.ts | visual check |
| Search input to find columns by name/description | Picker.vue | Picker.spec.ts | visual check |
| "Apply" button applies selection and closes modal | Picker.vue | Picker.spec.ts | visual check |
| "Cancel" button discards changes and closes modal | Picker.vue | Picker.spec.ts | visual check |
| "Select all" button selects all visible columns (does not apply until "Apply") | Picker.vue | Picker.spec.ts | visual check |
| "Clear" button deselects all filters (does not apply until "Apply") | Picker.vue | Picker.spec.ts | visual check |
| "Reset" button restores default filter set (does not apply until "Apply") | Picker.vue | Picker.spec.ts | visual check |
| Footer: [Select all] [Clear] [Reset] left, [Cancel] [Apply] right | Picker.vue | Picker.spec.ts | visual check |
| Long filter lists scroll inside modal content area; footer stays pinned; page does not scroll | Modal.vue | - | visual check |

## Active Filter Bar

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Sits above table results | ActiveFilters.vue | ActiveFilters.spec.ts | visual check |
| One chip per column with active filters | ActiveFilters.vue | ActiveFilters.spec.ts | visual check |
| Chip shows column name + number of active values (e.g. "Status (2)") | ActiveFilters.vue | ActiveFilters.spec.ts | visual check |
| Single filter value: chip shows actual value (e.g. "Status: active") | ActiveFilters.vue | ActiveFilters.spec.ts | visual check |
| Hover tooltip shows filter details (individual values) | ActiveFilters.vue | ActiveFilters.spec.ts | visual check |
| Click X on chip clears that column's filter | ActiveFilters.vue | ActiveFilters.spec.ts | visual check |
| "Clear all" button removes all filters | ActiveFilters.vue | ActiveFilters.spec.ts | visual check |
| Chip values show user-facing labels (BOOL Yes/No/Not set, ontology `label`, RADIO/CHECKBOX refLabel), matching sidebar Column display | formatFilterValue.ts, filterLabels.ts | formatFilterValue.spec.ts; filterLabels.spec.ts; ActiveFilters.spec.ts; filter-sidebar.spec.ts (e2e) | visual check |

## URL Sync

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Filter state serialized to URL query params | useFilters.ts | filterUrlCodec.spec.ts | - |
| Path filters supported (e.g. `order.pet.category.name=dogs`) | useFilters.ts | filterUrlCodec.spec.ts | - |
| Visible filter set stored in URL (`mg_filters` param) | useFilters.ts | filterUrlCodec.spec.ts | - |
| Collapse state stored in URL (`mg_collapsed` param, comma-separated IDs) | Sidebar.vue | Sidebar.spec.ts | - |
| Search stored as `mg_search` param | useFilters.ts | filterUrlCodec.spec.ts | - |
| Range values use `min..max` format | useFilters.ts | filterUrlCodec.spec.ts | - |
| Multi-value equals use pipe `|` separator | useFilters.ts | filterUrlCodec.spec.ts | - |
| Back/forward navigation restores filter state | useFilters.ts | useFilters.spec.ts | visual check |
| Non-filter URL params preserved (page, sort, view) | useFilters.ts | useFilters.spec.ts | - |
| Bookmarkable: copy URL reproduces exact filter state including collapse state | useFilters.ts | - | visual check |
| No localStorage usage (URL is the only persistence) | - | - | - |

## Table Integration

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| `enableFilters` prop on TableEMX2 (default true) — embeds full filter system | TableEMX2.vue | - | visual check |
| When enableFilters=true: useFilters initialized internally with table columns | TableEMX2.vue | - | - |
| When enableFilters=false: no sidebar, no filter composable, filter/hideSearch props work as before | TableEMX2.vue | - | - |
| Sidebar sits left of table in flex layout (w-80 sidebar + flex-1 table) | TableEMX2.vue | - | visual check |
| ActiveFilters bar between toolbar and table rows (auto-rendered, not via slot) | TableEMX2.vue | - | visual check |
| Show/hide sidebar: button in table toolbar (outline, filter-alt icon) | TableEMX2.vue | - | visual check |
| Button label: "Hide filters" when visible, "Show filters" when hidden | TableEMX2.vue | - | visual check |
| Table expands to full width when sidebar hidden | TableEMX2.vue | - | visual check |
| Filter changes reset pagination to page 1 and trigger data refresh | TableEMX2.vue | filter-sidebar.spec.ts (e2e) | - |
| Pagination hides when filtered count ≤ pageSize | TableEMX2.vue | filter-sidebar.spec.ts: "pagination count updates when filter applied" | - |
| Pagination count reflects filtered total (count > pageSize case) | TableEMX2.vue | filter-sidebar.spec.ts: "pagination OF count decreases but stays visible when filter leaves >pageSize results" | - |
| Search from filter sidebar drives table search (filters.searchValue → settings.search) | TableEMX2.vue | - | - |
| URL sync via useFilters (mg_filters, mg_search, mg_collapsed) | TableEMX2.vue | - | - |
| Sort/page URL sync remains in page consumer ([table]/index.vue) | [table]/index.vue | - | - |
| Story uses enableFilters=false to avoid route dependency | EMX2.story.vue | - | - |
| filterColumns logic lives in useFilters; consumers pass raw columns | useFilters.ts | useFilters.spec.ts | - |

## CSS / Theming

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Date/DateTime width set by consumer, not by component (removed scoped CSS) | Date.vue, DateTime.vue | - | visual check |
| DateTime popup teleport fix (`:teleport="true"`) | DateTime.vue | - | visual check all themes |
| Sidebar works in Light, Dark, Molgenis, UMCG, AUMC | Sidebar.vue | - | visual check all themes |
| ActiveFilters works in all themes | ActiveFilters.vue | - | visual check all themes |
| Picker modal works in all themes | Picker.vue | - | visual check all themes |

## Fixed Bugs (this PR)

- **BOOL all-NULL columns incorrectly kept visible**: `useFilters.ts:fetchAllBaseCounts` included `_null_` in the total count, causing BOOL columns where every value is NULL to appear as having data and therefore remain visible instead of being hidden. Fixed by excluding `_null_` from the non-zero count check.
- **`visibleFilterIds` watcher wrongly set `userHasCustomized=true`**: the watcher fired on programmatic changes (e.g. URL hydration), which disabled the auto-hide pruning before base counts arrived. Fixed by moving the `userHasCustomized=true` flag into the explicit user action (`toggleFilter`) only.

## Known Issues (backend — not this PR)

### Backend GraphQL `_agg` filter variable sharing bug
When the same GraphQL variable (e.g. `$filter`) is referenced in both `Table(filter:$filter)` and `Table_agg(filter:$filter)` in a single query, the `_agg` clause silently ignores the filter value when the filter uses the `_match_any_including_children` ontology operator.

- **Symptom**: pagination count stays at unfiltered total after an ontology filter is applied.
- **Reproducible with**: catalogue-demo/Resources, any ontology filter on `type` or `country`.
- **Frontend workaround (Phase 10.12)**: `fetchTableData.ts` introduces a separate `$aggFilter` variable with the same runtime value. A comment in the file documents this and flags it for backend follow-up.
- **Action needed**: file a separate backend issue; remove the workaround once fixed.
