# Molgenis ontology manager

The Molgenis ontology manager is a package that handles ontologies on a Molgenis server with functionality to add, delete or rename Catalogue Ontology entries and edit the databases present on the server accordingly.

## Installation

You can install the Molgenis ontology manager from PyPI sometime hopefully:

    python -m pip install molgenis-ontomanager

The Molgenis ontology manager is supported on Python 3.7 and above. 

## How to use

- specify server details (url, username, password) in .env file
- command line application named `ontomanager`
- specify action in command, i.e. `ontomanager delete ...`
- 