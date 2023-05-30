# Installation

    pip install molgenis_emx2_client

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

### Install the package

    pip install -e .

# Configuration
Create file `.env` using  `.env_example` as a reference.



# How to use
After installing the dependencies and creating the `.env` file run
    
    python -m molgenis_emx2_client

# Build

    (venv) $ python -m build

    (venv) $ pip install dist/molgenis_emx2_client*.whl
