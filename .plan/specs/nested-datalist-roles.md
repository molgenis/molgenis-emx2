# Nested DataList Role-Based Column Selection

## Context
When a detail page shows REF_ARRAY/REFBACK columns, nested records render as a DataList (TABLE, CARDS, LIST, or LINKS layout). The column selection and ordering should respect the `role` property on the referenced table's columns.

## Rules

### Column Selection (max 5 visible columns)

1. **Title column(s)**: `role=TITLE` columns are flattened into a single combined value, shown first, rendered **bold** and **clickable** (navigates to detail page). If no TITLE role exists, use primary key fields displayed via `refLabelDefault`.

2. **Detail columns**: Columns with `role=DETAIL` are shown after the title. If no DETAIL columns exist, take up to 4 other non-title, non-description, non-key, non-system columns from the start.

3. **Description column**: If `role=DESCRIPTION` exists, layout becomes: title on top → description below → detail fields below that.

4. **Total visible columns capped at 5** (title counts as 1 even if multiple TITLE columns are flattened).

5. **Excluded from display**: `role=INTERNAL`, `role=LOGO`, system columns (`mg_*`), SECTION, HEADING column types.

### Layout Rendering

| Layout | Without DESCRIPTION | With DESCRIPTION |
|--------|-------------------|-----------------|
| **TABLE** | Header row = column label/name, data in rows. Title column first, bold+clickable. | Same but description column included after title. |
| **CARDS** | Title bold+clickable on top. Detail fields side-by-side: label/name above, value below. | Title on top, description below title, detail fields side-by-side below. |
| **LIST** | Same as CARDS but single column (gridColumns=1). | Same as CARDS with description, single column. |
| **LINKS** | Bulleted list of clickable title values. | Same (no detail/description shown). |

### Pagination & Search
- When **>10 rows**: show pagination controls and search input.
- Default page size remains 10.

### Column Ordering (in TABLE header / CARDS detail fields)
1. Title (flattened, always first)
2. Description (if present)
3. DETAIL role columns (in metadata order)
4. Fallback columns (in metadata order)

## Behaviors

| Behavior | Component | Test | Visual |
|----------|-----------|------|--------|
| Search shown for ALL layouts (not just TABLE) when >10 rows | `DataList.vue` | `DataList.spec.ts` "shows search box in non-TABLE layout (CARDS) when truncated and paginated" | — |
| `role=LOGO` excluded from list columns | `displayUtils.ts` `getListColumns()` | `displayUtils.spec.ts` "excludes LOGO role columns from the result" | — |
| Multiple TITLE columns collectively count as 1 slot in the 5-slot cap | `displayUtils.ts` `getListColumns()` | `displayUtils.spec.ts` "multiple TITLE columns count as 1 toward the 5-column limit" | — |
| MULTISELECT/CHECKBOX with `refTableId` render as full-width DataList — single shared predicate governs DetailSection placement and DetailColumn rendering | `displayUtils.ts` `isDataListColumn()`, `DetailSection.vue`, `DetailColumn.vue` | `displayUtils.spec.ts` "isDataListColumn" block (6 tests) | — |
| Title column detection via `role=TITLE`, bold+clickable | `DataTable.vue` | — | visual check |
| Description layout: title → description → detail fields | `DataCards.vue` (`getDetailColumns()`, `getDescriptionColumn()`) | — | visual check |
