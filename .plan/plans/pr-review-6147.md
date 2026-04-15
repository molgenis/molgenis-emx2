# PR #6147 Review Response Plan

## Phase 1: Quick fixes (low risk, no design decisions)

### 1.1 Remove plan/spec files from PR
- `.plan/plans/tw-filters-v2.md` — reviewer says remove
- `.plan/specs/filter-sidebar-v2.md` — reviewer says remove
- Author doesn't agree, keep for now.

### 1.2 Theme variables replacing hardcoded colors
- `Column.vue:100` — `bg-gray-200` should use theme variable
- `Column.vue:152` — hardcoded border/color, use theme variable
- Action: replace with existing theme classes

### 1.3 Type badge question (main.css:271)
- Already addressed: type-badge removed, replaced with `<Well>` component
- Action: reply on PR

### 1.4 Log exception (Picker.vue:97)
- Action: add `console.error(e)` in catch block

### 1.5 Range.vue:7 — empty legend
- Legend prop defaults to "" with `v-if="legend"` guard — it's optional
- Action: reply explaining, or change default to undefined if cleaner

### 1.6 Sidebar.vue:5 — two scripts
- This is the WELL_BASE export pattern (documented Vue SFC pattern)
- RESOLVED: actually Sidebar two-scripts is for MG_COLLAPSED_PARAM export
- Action: reply explaining purpose

### 1.7 Picker.vue:21 — optional and null
- Action: check if nullable is needed or if optional alone suffices

### 1.8 Column.vue:66 — null check
- Action: check if null is appropriate or should be undefined

## Phase 2: Refactors (moderate, code changes)

### 2.1 ActiveFilters.vue magic numbers (line 51)
- `max-w-48`, `max-w-32`, `ml-1.5` — where do they come from?
- Action: check Figma, align with design tokens or add comment

### 2.2 ActiveFilters.vue custom inline button (line 76)
- "Clear all" is a raw `<button>` with custom classes
- Action: replace with `<Button type="text" size="tiny">` or similar

### 2.3 Column.vue:32 — undefined check
- Action: investigate, add guard or type narrowing

### 2.4 Column.vue:38 — array-like types missing
- Action: check filterTypes.ts, verify CHECKBOX/SELECT handling

### 2.5 Picker.vue:48 — inline emit, give meaningful name
- Action: extract to named function

### 2.6 Picker.vue:60 — rename "sync" to "reset"
- Action: rename function

### 2.7 Picker.vue:79 — watch with immediate vs onMounted
- Action: replace with single `watch(..., { immediate: true })`

### 2.8 Picker.vue:97 — log exception (see 1.4)

### 2.9 Picker.vue:127 — break up big function with filter
- Action: refactor nested loop into filter chain

### 2.10 Picker.vue:225,238 — extract inline return types
- Action: define named interfaces/types

### 2.11 Picker.vue:354 — "feels like existing component"
- Already addressed with `<Well>` component

### 2.12 Column.vue:153 — use existing input component
- DECISION: reuse `input/Search.vue` instead of plain `<input>`
- This also addresses 3.1 (debounce) and 3.3 (reusable function)
- Search.vue has 500ms debounce; may need a `debounce` prop if 300ms is needed

## Phase 3: Architecture decisions (DECIDED)

### 3.1 Column.vue:22 — debounce
- DECISION: reuse `input/Search.vue` in Column.vue (option A)
- Search.vue already has debounce built in
- If 300ms vs 500ms matters, add `debounce` prop to Search.vue
- Removes need for TEXT_INPUT_DEBOUNCE_MS constant and manual setTimeout

### 3.2 Column.vue:43,47 — function wrapping
- DECISION: inline the logic (option B)
- Move `countedOptionToTreeNode` and `filterValueToTreeSelection` into Column.vue
- Remove from global utils if no other consumers

### 3.3 Column.vue:90 — reusable debounce
- DECISION: covered by 3.1 — reuse Search.vue, no manual debounce needed

### 3.4 Column.vue:104 — loading indicator
- DECISION: create small `Skeleton.vue` component (~10 lines)
- Fixes hardcoded `bg-gray-200`, sets precedent for consistent loading
- Use theme variable for skeleton bar color

### 3.5 Column.vue:138 — template slots over-engineering
- DECISION: remove slots from Range.vue, let Range own Input rendering internally
- Range.vue accepts column type + values, renders Input internally

### 3.6 Sidebar.vue:30 — route vs props
- DECISION: move collapsed URL sync into `useFilters`
- Remove route/router props from Sidebar
- Sidebar gets only `defaultCollapsed?: string[]` prop
- `useFilters` gains: `collapsedIds` (reactive), `toggleCollapse(id)`, mg_collapsed URL sync
- Route/router stay as useFilters options (required — tailwind-components has autoImport:false)
- MG_COLLAPSED_PARAM export moves from Sidebar to useFilters
