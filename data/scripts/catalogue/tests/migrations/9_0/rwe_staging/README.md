# Real world data staging area (RWEStaging)

- `Datasets.csv` becomes `Tables.csv`, `Dataset mappings.csv` becomes `Table mappings.csv`.
- `source dataset` and `target dataset` become `source table` and `target table`, in `Table mappings.csv` and `Variable mappings.csv`.
- In `Collections.csv` the refbacks `datasets` and `mappings to common data models` are renamed in the same way (not compared by the tests).
- `repeats` keeps the value `NA`. It is part of the key of `Variable mappings`.

Note: in this profile `Table mappings.target table` refers to the local `Tables` table,
but `Variable mappings.target table` refers to `Tables` in the catalogue schema. This is the same as in the old model.
