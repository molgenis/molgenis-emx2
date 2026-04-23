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

## Affected Components
- `displayUtils.ts` — `getListColumns()` needs to enforce max 5 columns for all layouts (not just non-TABLE), flatten TITLE into first position
- `DataTable.vue` — title column detection already uses `role=TITLE` (line 37), already bold+clickable
- `DataCards.vue` — uses `getDetailColumns()`, `getDescriptionColumn()`, already supports description layout
- `DataList.vue` — default pageSize stays 10, search already shown in smart+TABLE mode

## Key Changes Needed
1. `getListColumns()`: for TABLE layout, also limit to 5 columns using role-based selection (currently shows all)
2. `getListColumns()`: ensure TITLE columns count as 1 in the cap
3. `DataList.vue`: show search for all layouts when >10 rows (currently TABLE only)
