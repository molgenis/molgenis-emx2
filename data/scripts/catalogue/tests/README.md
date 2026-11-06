# Migration tests

These tests show what a data migration must do, with small example data.
Each test case has a folder with data **before** the migration (`from/`) and the data you expect **after** the migration (`to/`).

## Run the tests

From `data/scripts/catalogue`:

```console
pip install -r requirements.txt pytest
python -m pytest tests
```

## Folder layout

```
tests/migrations/<version>/            e.g. 9_0, tested with update/update_9_0.py
    model_commit.txt                   git commit with the new data model (optional)
    <case>/                            one case, e.g. catalogue_flat
        README.md                      what this case shows, in plain words
        from/                          part of an export made with the old data model
            molgenis.csv               the old data model of this schema
            Profiles.csv               the profile of this schema, e.g. DataCatalogueFlat
            <Table>.csv ...            the tables that this case is about
        to/                            the data you expect after the migration
            Profiles.csv
            <Table>.csv ...
```

## What the tests check

For each case, the test copies `from/`, runs the migration on the copy, and then checks:

1. **`from/` is a valid export of the old model.** This checks the test case itself.
2. **The result is the same as `to/`.** All files, columns and rows must be the same.
   The order of columns and rows does not matter.
3. **The result fits the new model.** Every file must be a table of the new model, and every column must exist.
   For a reference column such as `useExternalDefinition.resource`, the part after the dot must be a key of the referenced table.

Check 3 is important because EMX2 does not stop an upload for these errors.
EMX2 skips a file with an unknown table name and ignores unknown columns.
So a forgotten rename causes data loss without an error message.

## Rules

- `molgenis.csv` is not compared. The migration makes it again from the new data model.
- Refback columns (for example `permitted values.value` in `Variables.csv`) are not compared.
  EMX2 calculates them and does not store them.
- Data values do not change, unless the README of the case says so.
- After the migration, the folder contains only tables of the new model.
  Remove the old file when you rename a table.
- A file with only a header line is the same as no file, because an upload of it changes nothing.
  So `to/` does not need files for empty tables.

## Add a test case

1. Download a schema export (zip) from a server that uses the old model, and unzip it.
2. Make a new case folder. Copy `molgenis.csv`, `Profiles.csv` and the tables that the change is about to `from/`.
   A real export contains a file for every table of the profile, also when the table is empty.
   So also copy the files that the migration reads. An empty table only needs its header line.
3. Keep only a few rows per table: rows that show the change, and one or two rows that must not change.
   Do not use personal data, such as real names or email addresses.
4. Copy `from/` to `to/`, remove `molgenis.csv`, and change the files by hand to what you expect.
5. Write in `README.md` what the case shows and why.

## The migration code

A migration for version `X_Y` is the class `Transform` in `update/update_X_Y.py`:

```python
transform = Transform(schema_name, data_dir, model_dir)
transform.delete_data_model_file()   # remove the old molgenis.csv
transform.update_data_model_file()   # write molgenis.csv from model_dir for the profile in Profiles.csv
transform.transform_data()           # change the data files in data_dir
```

The tests use the model of the commit in `model_commit.txt`. If that file is not there, the tests use `data/_models/shared` of this checkout.
To use another model folder, set `MG_MIGRATION_MODEL_DIR`.
