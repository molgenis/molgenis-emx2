# 9.0: the table Datasets is now called Tables

Model change: commit 73e8c8692, PR #6416.

What the migration must do:

- Rename `Datasets.csv` to `Tables.csv` and `Dataset mappings.csv` to `Table mappings.csv`. Remove the old files.
- Rename every column name and column part `dataset` to `table`, for example:
  - `dataset` → `table` (Variables, Variable values)
  - `dataset type` → `table type` (Tables)
  - `source dataset` / `target dataset` → `source table` / `target table` (Table mappings, Variable mappings)
  - `useExternalDefinition.dataset` → `useExternalDefinition.table` (Variables)
  - `variable.dataset` → `variable.table` (Reused variables)
  - `source variables other datasets.dataset` → `source variables other tables.table` (Variable mappings)
- Do not change data values. A table named `test dataset for testCohort3` keeps that name.
  (The demo data in the same PR did change such values. That was an edit of the demo data, not a migration rule.)
- The ontology `Dataset types` is now `Table types`, with the same terms. The values of `table type` stay the same.
