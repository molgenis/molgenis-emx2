# Molgenis EMX2 Ontology Manager

The Molgenis EMX2 Ontology Manager is a package that handles ontologies on a Molgenis server with functionality to add, delete or update CatalogueOntologies entries and edit the databases present on the server accordingly.

## Installation

The package requires a Python installation version above or equal to 3.7 and the molgenis-emx2-pyclient package.
Install both before installing the Molgenis ontology manager

    pip install molgenis-emx2-pyclient

The molgenis-ontomanager package can then be installed from the PyPI repository:

    pip install molgenis-emx2-ontomanager

## Development
Clone the Molgenis EMX2 repository using git
    
    git clone https://github.com/molgenis/molgenis-emx2.git

Change the working directory to `tools/ontomanager`

Set up the virtual environment and install the required packages

    python3 -m venv venv
    source venv/bin/activate
    pip install -r requirements.txt

#### Building the package
Specify the package version by creating the `version.txt` file and writing the version number on the first line of this file. 


Build the package

    python3 -m build

Install the built package

    pip install dist/molgenis-emx2-ontomanager-x.y.z-py3-none-any.whl
where `x.y.z` is the version you set in the `version.txt` file.


## How to use

### In Python scripts
Import the OntologyManager class from the package and create an instance by providing login details.

Example usage:

    from molgenis_emx2_ontomanager import OntologyManager
    ...

    ...

    server_url = "https://example.molgeniscloud.org"
    username = "username"
    password = ...
    ontomanager = OntologyManager(url=server_url, 
                                  username=username, 
                                  password=password)

#### Adding an ontology term
Specify the table in which the term is to be added, the name of the term to be added, and optionally additional information such as a label, parent term or definition.

##### Example:

    ontomanager.add(table='Countries', name='Republic of Molgenia', 
                                       order=1000)
    ontomanager.add(table='Countries', name='Armadilland',
                                       parent='Republic of Molgenia', order=1001)

The argument `name` is mandatory. 
The optional arguments are `order, label, parent, codesystem, code, ontologyTermURI, definition`.
Note that a term cannot be added to the ontology table if its name is already present.
If a parent term is specified, this must already be present in the table.

Terms can also be added to a CatalogueOntologies table through a pandas DataFrame from, for example, a .csv file.
Firstly load the .csv file into memory in a DataFrame, then pass this DataFrame as `data` in the `add` method.

##### Example:
The `demos/data/Countries.csv` file contains the following data

| name                 |   order | parent               |
|----------------------|---------|----------------------|
| Republic of Molgenia |    1000 |                      |
| Armadilland          |    1001 | Republic of Molgenia |

    import pandas as pd
    ...
    countries = pd.read_csv('demos/data/Countries.csv')
    ontomanager.add(table='Countries', data=countries)

#### Updating an ontology term
Updating references to an ontology term can be performed by the ontology manager's update functionality.
This can be done by calling the `update` method, specifying the name of the ontology table and the old and new terms.
The ontology manager will look for any references to the term in every table in all databases on the server and replace those by the new term.

##### Example:

    ontomanager.update(table='Countries', old='Republic of Molgenia',
                                          new='Armadilland')

A pandas DataFrame object with columns _old_ and _new_ can also be passed using the `data` argument in the `update` method  .
The entries in that DataFrame are then sequentially implemented.

##### Example:
The `demos/data/update.csv` file contains the following data

| old                  | new                  |
|----------------------|----------------------|
| Croatia              | Republic of Molgenia |
| Republic of Molgenia | Armadilland          |
| Armadilland          | Croatia              |

This dataset can be loaded into memory as a pandas DataFrame object and then implemented using the following code
    
    update_df = pd.read_csv('data/update.csv')
    ontomanager.update(table='Countries', data=update_df)


#### Deleting an ontology term
Deleting a term from an ontology table can be done by calling the manager's `delete` method, specifying the table and the name of the term to be deleted.
Note that it is not possible to delete a term that is still referenced in a table in one of the databases

##### Example:
In order to delete the term _Republic of Molgenia_ use the following command:

    ontomanager.delete(table='Countries', name='Republic of Molgenia')  

Multiple terms can be deleted with one command by supplying the argument `names` with the term names passed as a list

    ontomanager.delete(table='Countries', names=['Republic of Molgenia', 'Armadilland'])  

### In command line as module
The above functionality can also be applied in the command line interface:
For add and remove actions the flag `--name` is mandatory, and the flags `--order`, `--label`, `--parent`, `--URI`, `--definition` are optional.
For the update action the flags `--old` and `--new` are mandatory and the only accepted arguments.
The actions in the previous examples can be performed using the cli as follows:

    python3 -m molgenis_ontomanager add Countries --name='Republic of Molgenia' --label='Molgenia'
    python3 -m molgenis_ontomanager add Countries --name=Armadilland --parent='Republic of Molgenia'

    python3 -m molgenis_ontomanager update Countries --old='Republic of Molgenia' --new=Armadilland

    python3 -m molgenis_ontomanager delete Countries --name='Republic of Molgenia'
