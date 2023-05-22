# MOLGENIS EMX2 PY Client

Package Name: `molgenis-emx2-client`

## Getting Started

| Method | Description |
|--------|-------------|
| `add` | import records into a table from a file or from an object
| `delete` | remove records from a table from a file or from an object
| `get` | retrieve records from a table
| `signin` | connect to an EMX2 instance
| `signout` | disconnect from an EMX2 instance

### Importing the client

```py
from molgenis.client import Client

db = Client(host='....')
```

### Signing in and out

```py
db.signin(username="...", password="...")

db.signout()
```

### Adding Data

You can add new records from a file or a data object in python.

> **Note**: Only csv files are supported

```py
# from file
db.add(schema="pet store", table="Pet", file="path/to/csv")

# data
data = [...]
db.add(schema = "pet store", table="Pet", data = data)
```

### Deleting Records

You can remove records by listing them in a file or as a data object in python

> **Note**: Only csv files are supported

```py
# from file
db.add(schema="pet store", table="Pet", file="path/to/csv")

# data
data = [...]
db.add(schema = "pet store", table="Pet", data = data)
```

### Retrieving records

> **Note**: at the moment, data is returned as a csv-string. A parser is still under development.

```py
db.get(schema="pet store", table="Pet")
```

## For Developers

This package is modelled after the [molgenis-py-client](https://github.com/molgenis/molgenis-py-client/) library and contains the minimal configurations to build the package. See the following section for build commands.

### Building the package

Building the package requires the `tox` package. Install the required build dependencies.

```zsh
pip install tox
```

Build command and publish

```zsh
tox -e build
tox -e publish -- --repository pypi --username <USERNAME> --passwrd <PASSWORD>
```
