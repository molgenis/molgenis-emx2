# Installation

```console
pip install molgenis_emx2_pyclient
```

## How to use

Within your Python project import the class Client and use it to sign in

```py
from molgenis_emx2_pyclient import Client

client = Client('https://example.molgeniscloud.org')
client.signin('username', 'password')
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

```console
(venv) $ python -m build

(venv) $ pip install dist/molgenis_emx2_pyclient*.whl
```
