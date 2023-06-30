# Molgenis EMX2 Ontology Manager

The Molgenis EMX2 Ontology Manager is a package that handles ontologies on a Molgenis server with functionality to add, delete or update CatalogueOntologies entries and edit the databases present on the server accordingly.

## Installation

The package requires a Python installation version above or equal to 3.7 and the molgenis-emx2-pyclient package.
Install both before installing the Molgenis ontology manager

    pip install molgenis-emx2-pyclient

The molgenis-ontomanager package can then be installed from the test PyPI repository:

    pip install -i https://test.pypi.org/simple/ molgenis-emx2-ontomanager


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

Example:

    ontomanager.add(table='Countries', name='Republic of Molgenia', 
                                       label='Molgenia')
    ontomanager.add(table='Countries', name='Armadilland',
                                       parent='Republic of Molgenia')

The argument `name` is mandatory. 
The optional arguments are `order, label, parent, codesystem, code, ontologyTermURI, definition`.
Note that a term cannot be added to the ontology table if its name is already present.
If a parent term is specified, this must already be present in the table.

#### Updating an ontology term
Updating references to an ontology term can be performed by the ontology manager's update functionality.
This can be done by calling the `update` method, specifying the name of the ontology table and the old and new terms.
The ontology manager will look for any references to the term in every table in all databases on the server and replace those by the new term.

Example:

    ontomanager.update(table='Countries', old='Republic of Molgenia',
                                          new='Armadilland')


#### Deleting an ontology term
Deleting a term from an ontology table can be done by calling the manager's `delete` method, specifying the table and the name of the term to be deleted.
Note that it is not possible to delete a term that is still referenced in a table in one of the databases

Example:

    ontomanager.delete(table='Countries', name='Republic of Molgenia')  

### In command line as module
The above functionality can also be applied in the command line interface:
For add and remove actions the flag `--name` is mandatory, and the flags `--order`, `--label`, `--parent`, `--URI`, `--definition` are optional.
For the update action the flags `--old` and `--new` are mandatory and the only accepted arguments.
The actions in the previous examples can be performed using the cli as follows:

    python3 -m molgenis_ontomanager add Countries --name='Republic of Molgenia' --label='Molgenia'
    python3 -m molgenis_ontomanager add Countries --name=Armadilland --parent='Republic of Molgenia'

    python3 -m molgenis_ontomanager update Countries --old='Republic of Molgenia' --new=Armadilland

    python3 -m molgenis_ontomanager delete Countries --name='Republic of Molgenia'
