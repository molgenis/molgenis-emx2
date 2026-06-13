# How to format your CSV files for import

# Column types

MOLGENIS supports the following column types in the EMX2 CSV/TSV metadata format. The `columnType` header
(also written as `type`) is case-insensitive.

## Enumeration types

| columnType    | Description |
|---------------|-------------|
| `ENUM`        | A string restricted to a fixed set of allowed values declared in the `values` column. |
| `ENUM_ARRAY`  | A list of strings, each restricted to the same allowed `values` set. |
| `MODULE_ARRAY`| A composition-discriminator column (declared on a root table). Its value is a subset of the MODULE tables listed in `values`. See [MODULE composition](use_schema.md#module_array-composition-column). |

### The `values` column

The `values` column in the metadata sheet holds a comma-separated list of allowed choices for
`ENUM`, `ENUM_ARRAY`, and `MODULE_ARRAY` columns:

- **ENUM / ENUM_ARRAY** — the allowed string literals (e.g. `values=red,green,blue`).
- **MODULE_ARRAY** — the allowed MODULE table names, as **bare names** within the same schema (e.g. `values=DiabetesData,RenalData`). Schema-qualified `schema.Table` names are rejected.

**Important:** `values` membership is now **enforced on INSERT and UPDATE**. A value outside the
declared (non-empty) `values` set is rejected with a validation error. Previously, `ENUM` `values`
were advisory-only — that is no longer the case.

If `values` is empty or absent the column accepts any string (useful during schema development).

### MODULE and MODULE_ARRAY round-trip

`tableType=MODULE` tables and `MODULE_ARRAY` columns round-trip through the EMX2 CSV metadata
format — export and re-import recreate the MODULE tables, their `extends` binding, and the
`MODULE_ARRAY` axis declarations.

Module columns (the columns defined inside a MODULE table) appear in CSV **data** download/export
for the root table, alongside the root's own columns. Re-importing that CSV recreates the module
rows via the normal write path (the `MODULE_ARRAY` discriminator value triggers module-row creation).

# FAQ

## How to import comma values

Example for REF_ARRAY or STRING_ARRAY type where you have multiple values with commas in them.

```
col1,col2
col1value,"\"value in col 2 with, comma\",\"and, more, commas\""
```