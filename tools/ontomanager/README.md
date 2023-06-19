# Molgenis ontology manager

The Molgenis ontology manager is a package that handles ontologies on a Molgenis server with functionality to add, delete or update CatalogueOntologies entries and edit the databases present on the server accordingly.

## Installation

The package requires the installation of the molgenis-emx2-client package, which depends on the requests package. 
Install both before installing the Molgenis ontology manager

    pip install requests
    pip install -i https://test.pypi.org/simple/ molgenis-emx2-client

The molgenis-ontomanager package can then be installed from the test PyPI repository:

    pip install -i https://test.pypi.org/simple/ molgenis-ontomanager

The Molgenis ontology manager is supported on Python 3.7 and above. 

## How to use

### In Python scripts
Import the OntologyManager class from the package and create an instance by providing login details.

Example usage:

    from molgenis_ontomanager import OntologyManager
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

The argument `name` is mandatory. The optional arguments are `order, label, parent, codesystem, code, ontologyTermURI, definition`.
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
The above functionality can also be employed in the command line interface:
For add and remove actions the flag `--name` is mandatory, and the flags `--order`, `--label`, `--parent`, `--URI`, `--definition` are optional.
For the update action the flags `--old` and `--new` are mandatory and the only accepted arguments.

    python3 -m molgenis_ontomanager add Countries --name='Republic of Molgenia', --label='Molgenia'
    python3 -m molgenis_ontomanager add Countries --name=Armadilland', --parent='Republic of Molgenia'

    python3 -m molgenis_ontomanager update Countries --old=Armadilland --new='Republic of Molgenia'

    python3 -m molgenis_ontomanager delete Countries --name='Republic of Molgenia'
