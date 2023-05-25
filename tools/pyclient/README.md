# Installation

Ensure the current working directory is `.../tools/pyclient`

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

### Install the package

    pip install -e .

# Configuration
Create file `.env` using  `.env_example` as a reference.



# How to use
After installing the dependencies and creating the `.env` file run
    
    python -m molgenis

# Build

    (venv) $ python -m pip install pip-tools
    (venv) $ pip-compile pyproject.toml

    (venv) $ python -m build
    #(venv) $ pip-sync

    (venv) $ pip install molgenis_emx2_client_py-8.185.0-py3-none-any.whl
