# Column Display Properties — Spec

## Overview
EMX2 columns have two display-related properties (`role` and `display`) that control how fields render in detail views (Emx2RecordView) and card layouts (ListCard). These are first-class Column properties stored in `column_metadata`, exposed via GraphQL, and set in data model CSVs.

## Column Properties

### `role` (ColumnRole enum)
Controls how a field is used in card/summary contexts (ListCard component).

| Value | Purpose | Example |
|-------|---------|---------|
| `TITLE` | Card heading, row label | `acronym` on Resources, `first name`/`last name` on Contacts |
| `SUBTITLE` | Secondary heading | `name` on Resources |
| `DESCRIPTION` | Card description (line-clamped) | `description` on Resources |
| `LOGO` | Image rendered in card | `logo` on Resources/Agents |
| `DETAIL` | Shown in card's definition list | `type`, `start year`, `role` on various tables |

**How ListCard uses roles:**
- Scans columns for `role` values to decide what to render
- Multiple columns can have `role=TITLE` (concatenated)
- `DESCRIPTION` is line-clamped to 2 lines
- `DETAIL` columns render as `<dt>/<dd>` pairs
- `LOGO` column's `url` property is used as `<img src>`

**Set in CSV:** Add `role` column to header, values are lowercase (`title`, `subtitle`, `description`, `logo`, `detail`).

### `display` (DisplayType enum)
Controls the layout mode when rendering REF_ARRAY/REFBACK columns in detail views.

| Value | Layout | Use Case |
|-------|--------|----------|
| `TABLE` | Paginated table (default) | Datasets, Collection Events, Subpopulations |
| `CARDS` | Responsive card grid (2 cols) | Organisations, People, Networks |
| `LIST` | Bullet list | (Future: Publications) |

**How Emx2RecordView uses display:**
- Reads `display` from column metadata
- Sets `listConfig.layout` on the IColumnDisplay
- ListView renders the appropriate layout
- Cards layout uses ListCard, which reads `role` from the *referenced table's* columns

**Set in CSV:** Add `display` column to header, values are lowercase (`table`, `cards`, `list`).

### `summary` (Boolean) — NOT USED
The `summary` column property exists in the backend schema but is NOT needed for display. The `role` property fully controls which fields appear on cards: fields with any `role` value are shown, fields without `role` are hidden from cards.

## HEADING columns
`columnType: heading` creates section groupings in detail views. Emx2RecordView auto-generates sidebar navigation from HEADING columns.

**In CSV:** Add a row with `columnType=heading` before the fields that belong to that section. The `columnName` becomes the section ID, the `description` becomes the section title shown in the UI.

Example from Resources.csv:
```
Resources,,overview,heading,,,,,,,,,General information,"DataCatalogueFlat,..."
Resources,,id,,1,TRUE,,,,,,,Internal identifier,"DataCatalogueFlat,..."
Resources,,name,text,3,TRUE,,,,,,,Name used in international projects,"DataCatalogueFlat,...",,,,,subtitle
```

## TableRole (table-level)
Controls whether a table appears on the schema landing page.

| Value | Purpose |
|-------|---------|
| `MAIN` | Default, shown on landing page |
| `DETAIL` | Hidden from landing, only shown as nested detail |

Set on TableMetadata, not Column. Collection Events, Subpopulations, Datasets use `DETAIL`.

## Data Flow
```
CSV (role/display columns) → Column.java (role: ColumnRole, display: DisplayType)
  → MetadataUtils.java (persisted in column_metadata table)
  → GraphQL schema (exposed as fields)
  → IColumn type (frontend, role?: string, display?: string)
  → Emx2RecordView (reads display → listConfig.layout)
  → ListView/ListCard (reads role → card rendering)
```

## Backend Files
- `Column.java` — `role` (ColumnRole) and `display` (DisplayType) fields
- `ColumnRole.java` — enum: TITLE, SUBTITLE, DESCRIPTION, LOGO, DETAIL
- `DisplayType.java` — enum: TABLE, CARDS, LIST
- `MetadataUtils.java` — persistence in column_metadata
- `migration32.sql` — ALTER TABLE adding columns
- `Emx2.java` — CSV import/export constants
- `GraphqlSchemaFieldFactory.java` — GraphQL exposure

## Frontend Files
- `metadata-utils/src/types.ts` — `IColumn.role`, `IColumn.display`
- `gql/metadata.js` — GraphQL query includes role/display
- `Emx2RecordView.vue` — reads display → sets listConfig.layout
- `ListView.vue` — renders TABLE/CARDS/LIST based on layout
- `ListCard.vue` — reads role from columns for card rendering
- `displayUtils.ts` — helper functions (getTitle, getDescription, etc.)
