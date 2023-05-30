# Installation

    pip install molgenis_emx2_client


# How to use
Within your Python project import the class Client and use it to sign in
    
    from molgenis_emx2_client import Client
    ...

    ...
    client = Client('https://example.molgeniscloud.org')
    client.signin('username', 'password')

# Development

Clone the `molgenis-emx2` repository from GitHub

    git clone git@github.com:molgenis/molgenis-emx2.git

Change the working directory to `.../tools/pyclient`

### Create a virtual Python environment

On macOS:

    python -m venv venv

On Linux:

    python3.11 -m venv venv
    
On Windows:

    py -3.11 venv venv

### Activate the virtual environment
    
On macOS and Linux:

    source venv/bin/activate
    
On Windows:

    .venv\Scripts\activate.bat

### Install the script dependencies

    pip install -r requirements.txt


# Build

    (venv) $ python -m build

    (venv) $ pip install dist/molgenis_emx2_client*.whl
