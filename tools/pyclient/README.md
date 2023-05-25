Create a virtual python environment

    On macOS:

    `python -m venv venv`

    On Linux:

    `python3.11 -m venv venv`
    
    On Windows:
    
    `py -3.11 venv venv `

Activate the virtual python environment
    
    On macOS and Linux:

    `source venv/bin/activate`
    
    On Windows:
    
    `.venv\Scripts\activate.bat`

Install the script dependencies from requirements.txt file

    `pip install -r requirements.txt`

install package locally inside virtual environment

/tools/pyclient

    (venv) $ python -m pip install -e .

Create .env in pyclient/src/molgenis

See .env_example for reference

tools/pyclient/src

Run library

    python -m molgenis