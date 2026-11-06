# Data model tests

These tests check what a MOLGENIS server makes of the data model files (`data/_models`) and the ontology lists (`data/_ontologies`).
The tests create schemas from templates on a server, and then read the tables, columns and ontology terms that the server made.
The tests do not read the model files themselves, so they always follow the rules of the server.

## Before you start

You need a MOLGENIS server that runs the code of the branch that you test.
The model files are part of the server, so a server that runs other code tests another model.
Do not use a production server: the tests create and delete schemas.

Start a local server from the root of the repository, with one of these:

- `./gradlew run`, or `./gradlew dev` (faster: no user interface, which these tests do not need)
- a Docker image that you built from the branch

The server must have its own database. See `docs/molgenis/dev_quickstart.md`.

You need an account that can create and delete schemas, for example `admin`.

## Run the tests

From the root of the repository:

```console
python -m venv .venv
source .venv/bin/activate
pip install -r data/tests/requirements.txt
MG_SERVER=http://localhost:8080 MG_USERNAME=admin MG_PASSWORD=admin python -m pytest data/tests
```

You can use `MG_TOKEN` instead of `MG_USERNAME` and `MG_PASSWORD`.
To run one test, add `-k` and a part of its name, for example `-k staging`.

## What the tests do on the server

1. Create the schema `pytest RD3 registry` from the template `PATIENT_REGISTRY`, without demo data.
2. Create the schemas `Staging area ids` and `erdera`, if they do not exist. The staging model refers to these names.
3. Create the schema `pytest RD3 staging` from the template `PATIENT_REGISTRY_STAGING`.
4. At the end, delete only the schemas that the tests created. `CatalogueOntologies` is also deleted if the tests created it.

If `pytest RD3 registry` or `pytest RD3 staging` already exists, the tests stop. Delete the schema, and run the tests again.

## What the tests check

`test_patient_registry.py` checks the RD3 rules for ERDERA submissions:

- Columns that are required in the `Patient registry staging` profile, but not in `Patient registry`.
- The kit columns of RNA samples and experiments refer to the right list in `CatalogueOntologies`.
  The kit columns of DNA samples and experiments stay free text.
- `sequencing centre` is free text for RNA experiments. For lrGS and srDNA experiments it refers to `Organisations`.
- `library kit manufacturer` is renamed to `library preparation kit manufacturer`.
- The terms of the ERDERA lists have the tags `erdera` and `RNA`, so that the Excel templates show them. The lists have no term `Other`.
- In a `Samples RNA` row, the server accepts only sample type `RNA`. Other sample tables accept other sample types.

The schemas must be created without errors. For example, a list name that is too long for a table name stops the test.

## What the tests do not check

- The demo data. The schemas are created without demo data.
- The rules for a row in the staging schema. The staging tests only read the columns.
- The forms in the user interface, for example whether a required field shows as required.
- The Excel templates that the ERDERA template generator makes.
- The other data models and profiles.
