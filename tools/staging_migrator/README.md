# Installation

```console
pip install molgenis-emx2-staging-migrator
```

## How to use

Within your Python project import the class StagingMigrator and use it as a context manager.
Specify the URL of the server, the API token, the name of the destination of the published data, i.e. the catalogue, and lastly the name of the table that is central in the data model.
For the data catalogue this table is _Resources_.

```py
from molgenis_emx2_staging_migrator import StagingMigrator

token = '...'

with StagingMigrator(url='https://example.molgeniscloud.org', token=token, 
                     catalogue="catalogue", table="Resources") as migrator:

    # Retrieve sign-in information
    print(migrator.status)
    
    # Set the staging area and catalogue
    migrator.set_staging_area('StagingExample')
    migrator.set_catalogue('catalogue')
    
    # Execute the migration
    migrator.migrate()

```

## Build

```console
(venv) $ python -m build

(venv) $ pip install dist/molgenis-emx2-staging_migrator*.whl
```
