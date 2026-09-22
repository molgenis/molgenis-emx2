# Installation

```console
pip install molgenis-emx2-staging-migrator
```

## How to use

Within your Python project import the class StagingMigrator and use it as a context manager.
Specify the URL of the server, the API token, the name of the _target_ of the published data, i.e. the catalogue.
It is recommended that the token not be written directly in your script but stored as an environmental variable and retrieved at runtime.

```py
from molgenis_emx2_staging_migrator import StagingMigrator

token = ...

with StagingMigrator(url='https://example.molgeniscloud.org', token=token, 
                     target="catalogue") as migrator:

    # Retrieve sign-in information
    print(migrator.status)
    
    # Set the source and target schemas
    migrator.set_source('StagingExample')
    migrator.set_target('catalogue')
    
    # Execute the migration
    migrator.migrate()

```

## Build

```console
(venv) $ python -m build

(venv) $ pip install dist/molgenis_emx2_staging_migrator*.whl
```
