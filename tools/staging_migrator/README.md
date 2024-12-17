# Installation

```console
pip install molgenis-emx2-staging-migrator
```

## How to use

Within your Python project import the class StagingMigrator and use it as a context manager

```py
from molgenis_emx2_staging_migrator import StagingMigrator

token = '...'

with StagingMigrator('https://example.molgeniscloud.org', token=token) as migrator:

    # Retrieve sign-in information
    print(migrator.status)
    
    # Set the staging area and catalogue
    migrator.set_staging_area('StagingExample')
    migrator.set_catalogue('catalogue')
    
    # Execute the migration
    migrator.migrate()
    

```

## Development




## Build

```console
(venv) $ python -m build

(venv) $ pip install dist/molgenis-emx2-staging_migrator*.whl
```
