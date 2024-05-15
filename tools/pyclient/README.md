The Molgenis EMX2 Pyclient is Python package developed to be used for data management on Molgenis EMX2 servers.
A detailed overview of the capabilities is presented in the [MOLGENIS documentation](https://molgenis.github.io/molgenis-emx2/#/molgenis/use_usingpyclient).

## Installation
The releases of the package are hosted at [PyPI](https://pypi.org/project/molgenis-emx2-pyclient/).
The recommended way to install the latest version is through `pip`.

```console
pip install molgenis-emx2-pyclient
```

## How to use

Within your Python project import the class Client and instantiate it as a context manager.
Operations and queries can then be executed from within the context.

```py
from molgenis_emx2_pyclient import Client

username = 'username'
password = '...'

with Client('https://example.molgeniscloud.org') as client:
    client.signin(username, password)

    # Retrieve signin information
    print(client.status)
    """ Output:
    Host: https://example.molgeniscloud.org
    Status: Signed in
    Schemas:
        CatalogueOntologies
        catalogue
        ExampleSchema
        ...
    Version: v10.10.1
    """
    
    # Retrieve data from a table on a schema
    data = client.get(schema='ExampleSchema', table='Cohorts')
    
    # Create a new schema on the server
    client.create_schema(name='New Schema')
    
    # Delete a schema from the server
    client.delete_schema(name='New Schema')

```
Instead of signing in with a username and password the client can also be used while authorized by a (temporary) token that is generated on the server.
See the [MOLGENIS documentation for generating tokens](https://molgenis.github.io/molgenis-emx2/#/molgenis/use_tokens)
```py
from molgenis_emx2_pyclient import Client

token = '...'

with Client('https://example.molgeniscloud.org', token=token) as client:

    # Retrieve signin information
    print(client.status)
    """ Output:
    Host: https://example.molgeniscloud.org
    User: token
    Status: session-less
    Schemas:
        CatalogueOntologies
        catalogue
        ExampleSchema
        ...
    Version: v10.32.1
    """
    
    ...
    ...

```

## Development

Clone the `molgenis-emx2` repository from GitHub

```console
git clone git@github.com:molgenis/molgenis-emx2.git
```

Change the working directory to `.../tools/pyclient`

### Create a virtual Python environment

On macOS:

```console
python -m venv venv
```

On Linux:

```console
python3.11 -m venv venv
```

On Windows:

```console
py -3.11 venv venv
```

### Activate the virtual environment

On macOS and Linux:

```console
source venv/bin/activate
```

On Windows:

```console
.venv\Scripts\activate.bat
```

### Install the script dependencies

```console
pip install -r requirements.txt
```

## Build
Before building the source, the package `build` needs to be installed.
```console
(venv) $ pip install build

(venv) $ python -m build

(venv) $ pip install dist/molgenis_emx2_pyclient*.whl
```
