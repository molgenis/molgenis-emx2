# Filter System v2 — UI Behavior Spec

Living spec. Single source of truth for expected behaviors.

## Architecture

Composable-driven. `useFilters` owns all state and counts. Components are thin display layers.
Counting centralized in useFilters. Parallel API calls per column.

**Filter rendering: use Tree directly for ALL countable types, NOT Ontology.vue/Ref.vue inputs.**
Ontology.vue and Ref.vue are form-editing inputs (complex selection models, own data fetching, pagination).
For filtering we need simpler: useFilters provides counted options, a thin `FilterOptions` component renders them.
- ALL countable types (ONTOLOGY, ONTOLOGY_ARRAY, BOOL, RADIO, CHECKBOX) → Tree with checkboxes
  - Flat types (BOOL/RADIO/CHECKBOX) are trees without children — same component, simpler code
  - Ontology types preserve hierarchy
- Range types (INT, DECIMAL, DATE, DATETIME, etc.) → FilterRange with appropriate inputs
- String-like → text input

Concerns addressed:
- **Counting takes time**: show loading skeleton per filter section; tree renders once counts arrive
- **Large tree**: collapse tree nodes by default, show top-level only; search-within-filter for finding deep terms
- **Search in tree**: client-side filter of the counted options tree; Tree already supports this

Existing Ontology.vue and Ref.vue stay untouched — they remain form inputs only.

**Countable types (facet counts):** ONTOLOGY, ONTOLOGY_ARRAY, BOOL, RADIO, CHECKBOX
**Range types (min/max):** INT, INT_ARRAY, DECIMAL, DECIMAL_ARRAY, LONG, NON_NEGATIVE_INT, NON_NEGATIVE_INT_ARRAY, DATE, DATE_ARRAY, DATETIME, DATETIME_ARRAY
**Navigable types (expand in picker, not directly filterable):** REF, REF_ARRAY, REFBACK, SELECT, MULTISELECT
**String-like (hidden unless searching in picker):** STRING, STRING_ARRAY, TEXT, EMAIL, HYPERLINK, UUID, AUTO_ID
**Excluded from defaults:** HEADING, SECTION, FILE, mg_* columns
**mg_* columns:** hidden by default but revealed when user searches in filter picker (advanced users)

## Filter Sidebar

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Shows INITIALLY only countable + range filters that have any options with count > 0 | Sidebar.vue | Sidebar.spec.ts | visual check |
| First 5 filters uncollapsed, rest collapsed | Sidebar.vue | Sidebar.spec.ts | visual check |
| Filter sections with URL-hydrated filters always start expanded (override collapse) | Sidebar.vue | Sidebar.spec.ts | visual check |
| Chevron toggle per section, `<hr>` dividers between sections | Sidebar.vue | Sidebar.spec.ts | visual check |
| Lazy mount: filter content only renders when section expanded (`v-if`) | Sidebar.vue | Sidebar.spec.ts | visual check |
| Global search input at top (uses backend `_search`) | Sidebar.vue | Sidebar.spec.ts | visual check |
| "Customize" button with Filter icon opens FilterPicker modal | Sidebar.vue | Sidebar.spec.ts | visual check |
| Show/hide sidebar: button in table toolbar (outline, filter-alt icon, matches Columns button) | [table]/index.vue | - | visual check |
| Button label: "Hide filters" when visible, "Show filters" when hidden | [table]/index.vue | - | visual check |
| Sidebar visibility controlled by parent via v-show (no internal toggle) | Sidebar.vue | - | - |
| Table expands to full width when sidebar hidden | [table]/index.vue | - | visual check |
| Visible filter set persisted in URL via `mg_filters` param | useFilters.ts | useFilters.spec.ts | - |
| Collapse state NOT in URL (first-5 rule reapplied on load, active filters override) | Sidebar.vue | - | - |
| Styling matches catalogue: bg-sidebar-gradient, rounded-t-[3px] rounded-b-[50px], semantic filter colors | Sidebar.vue | - | visual check |
| Sidebar wide enough to fit date-time range inputs side by side (~400-500px) | [table]/index.vue | - | visual check |
| DateTime input width 14em (Date stays 10em) | DateTime.vue | - | visual check |
| Styling works in all 5 themes (Light, Dark, Molgenis, UMCG, AUMC) | Sidebar.vue | - | visual check |

## FilterOptions Component (NEW — thin rendering wrapper)

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Receives counted options from useFilters (no own data fetching) | FilterOptions.vue | FilterOptions.spec.ts | - |
| ALL countable types (ONTOLOGY, ONTOLOGY_ARRAY, BOOL, RADIO, CHECKBOX): renders Tree with checkboxes | FilterOptions.vue | FilterOptions.spec.ts | visual check |
| Flat types (BOOL/RADIO/CHECKBOX) rendered as Tree nodes without children | FilterOptions.vue | FilterOptions.spec.ts | visual check |
| BOOL filter shows three options: true, false, null (with counts) | fetchCounts.ts | fetchCounts.spec.ts | visual check |
| Ontology types rendered as Tree preserving hierarchy | FilterOptions.vue | FilterOptions.spec.ts | visual check |
| Range types: renders FilterRange with existing InputDate/InputDateTime for date types, number input for numeric | FilterOptions.vue | FilterOptions.spec.ts | visual check |
| STRING-like: renders text input | FilterOptions.vue | FilterOptions.spec.ts | visual check |
| Shows loading skeleton while counts are being fetched | FilterOptions.vue | FilterOptions.spec.ts | visual check |
| Search-within-filter input for large option lists (client-side tree filter) | FilterOptions.vue | FilterOptions.spec.ts | visual check |
| Emits selection changes; useFilters handles state update | FilterOptions.vue | FilterOptions.spec.ts | - |

## Filter Options Display

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Default: only show options with initial count > 0 | useFilters.ts | useFilters.spec.ts | - |
| When other filters applied: counts update but initial terms stay visible | useFilters.ts | useFilters.spec.ts | - |
| Options that reach count 0 due to cross-filter stay visible (merged with base counts, show "0") | useFilters.ts | useFilters.spec.ts | visual check |
| On search within filter: show all matching terms regardless of count | FilterOptions.vue | FilterOptions.spec.ts | visual check |
| On clear filters: go back to showing only initial count > 0 | useFilters.ts | useFilters.spec.ts | - |
| No facet counting on REF/REF_ARRAY (use nested filters via picker instead) | - | - | - |

## Facet Counting (Centralized)

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Counts managed centrally in useFilters, not per-component | useFilters.ts | useFilters.spec.ts | - |
| Parallel API calls per column (don't wait for slowest) | useFilters.ts | useFilters.spec.ts | - |
| Cross-filter: each column's count excludes its own filter | useFilters.ts | useFilters.spec.ts | - |
| Initial (base) counts fetched once, determine which options/filters to show | useFilters.ts | useFilters.spec.ts | - |
| Base counts also shown in sidebar section headers (how many records match) | Sidebar.vue | Sidebar.spec.ts | visual check |
| Count updates debounced when filters change | useFilters.ts | useFilters.spec.ts | - |
| Ontology counts use `_groupBy` (discovers all terms with records) | useFilters.ts | useFilters.spec.ts | - |
| BOOL/RADIO/CHECKBOX counts use `_groupBy` per column | useFilters.ts | useFilters.spec.ts | - |

## Filter Picker (Modal)

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Opens as modal dialog from "Customize" button | FilterPicker.vue | FilterPicker.spec.ts | visual check |
| Shows all column types except STRING-like and mg_* (unless searching) | FilterPicker.vue | FilterPicker.spec.ts | visual check |
| Search reveals STRING-like and mg_* columns that match | FilterPicker.vue | FilterPicker.spec.ts | - |
| Modal is 90% screen width (100% on mobile) | FilterPicker.vue | - | visual check |
| Each column shows: InputCheckboxIcon + label + description (truncated, tooltip) + columnType badge | FilterPicker.vue | FilterPicker.spec.ts | visual check |
| Uses InputCheckboxIcon (same as Tree) not plain HTML checkbox | FilterPicker.vue | - | visual check |
| Description truncated to one line with tooltip (does not overrun type badge) | FilterPicker.vue | - | visual check |
| Column type shown as small badge/tag (e.g. "ONTOLOGY", "BOOL") | FilterPicker.vue | - | visual check |
| REF/REF_ARRAY shown as expandable rows with caret (same size as checkbox) + "→ tableName" | FilterPicker.vue | FilterPicker.spec.ts | visual check |
| REF expands 2 levels deep, REF_ARRAY/REFBACK 1 level deep | FilterPicker.vue | FilterPicker.spec.ts | - |
| Self-referencing back-refs to parent table excluded | FilterPicker.vue | FilterPicker.spec.ts | - |
| Expanded ref shows sub-columns indented with same checkbox + description + type layout | FilterPicker.vue | - | visual check |
| Selecting nested column adds filter as "root → child [→ child]" label in sidebar | FilterPicker.vue, Sidebar.vue | FilterPicker.spec.ts | visual check |
| Shows selected state (checkmarks via InputCheckboxIcon) | FilterPicker.vue | FilterPicker.spec.ts | visual check |
| Search input to find columns by name/description | FilterPicker.vue | FilterPicker.spec.ts | visual check |
| "Apply" button applies selection and closes modal | FilterPicker.vue | FilterPicker.spec.ts | visual check |
| "Cancel" button discards changes and closes modal | FilterPicker.vue | FilterPicker.spec.ts | visual check |
| "Select all" button selects all visible columns (does not apply until "Apply") | FilterPicker.vue | FilterPicker.spec.ts | visual check |
| "Clear" button deselects all filters (does not apply until "Apply") | FilterPicker.vue | FilterPicker.spec.ts | visual check |
| "Reset" button restores default filter set (does not apply until "Apply") | FilterPicker.vue | FilterPicker.spec.ts | visual check |
| Footer: [Select all] [Clear] [Reset] left, [Cancel] [Apply] right | FilterPicker.vue | FilterPicker.spec.ts | visual check |

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

## URL Sync

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Filter state serialized to URL query params | useFilters.ts | filterUrlCodec.spec.ts | - |
| Path filters supported (e.g. `order.pet.category.name=dogs`) | useFilters.ts | filterUrlCodec.spec.ts | - |
| Visible filter set stored in URL (`mg_filters` param) | useFilters.ts | filterUrlCodec.spec.ts | - |
| Search stored as `mg_search` param | useFilters.ts | filterUrlCodec.spec.ts | - |
| Range values use `min..max` format | useFilters.ts | filterUrlCodec.spec.ts | - |
| Multi-value equals use pipe `|` separator | useFilters.ts | filterUrlCodec.spec.ts | - |
| Back/forward navigation restores filter state | useFilters.ts | useFilters.spec.ts | visual check |
| Non-filter URL params preserved (page, sort, view) | useFilters.ts | useFilters.spec.ts | - |
| Bookmarkable: copy URL reproduces exact filter state | useFilters.ts | - | visual check |
| No localStorage usage (URL is the only persistence) | - | - | - |

## Table Integration

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Sidebar sits left of table | [table]/index.vue | - | visual check |
| ActiveFilters bar between toolbar and table rows | [table]/index.vue | - | visual check |
| Filter changes reset pagination to page 1 | TableEMX2.vue | - | - |
| `fetchTableMetadata` includes subclass columns | fetchTableMetadata.ts | - | - |
| Minimal changes to TableEMX2 (just plug in sidebar + activefilters) | TableEMX2.vue | - | - |

## CSS / Theming

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Date input styling fix | Date.vue | - | visual check all themes |
| DateTime popup teleport fix | DateTime.vue | - | visual check all themes |
| Sidebar works in Light, Dark, Molgenis, UMCG, AUMC | Sidebar.vue | - | visual check all themes |
| ActiveFilters works in all themes | ActiveFilters.vue | - | visual check all themes |
| FilterPicker modal works in all themes | FilterPicker.vue | - | visual check all themes |
