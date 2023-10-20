# Installation

```console
pip install molgenis_emx2_staging_migrator
```

## How to use

Within your Python project import the class StagingMigrator and use it as a context manager

```py
from molgenis_emx2_staging_migrator import StagingMigrator

username = 'username'
password = '...'

with StagingMigrator('https://example.molgeniscloud.org') as migrator:
    migrator.signin(username, password)

    # Retrieve sign-in information
    print(migrator.status)
    
    migrator.set_staging_area('TestStaging')
    migrator.set_catalogue('catalogue')
    
    migrator.migrate()
    

```

## Development


```

## Build

```console
(venv) $ python -m build

(venv) $ pip install dist/molgenis_emx2_staging_migrator*.whl
```
