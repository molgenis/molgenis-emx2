"""
Script to demonstrate adding terms to a table from a .csv file with the Molgenis EMX2 Ontology Manager package.
Create an OntologyManager object by supplying a server's url and login credentials as arguments in the
function below or create a .env file with the following structure

MG_URL = https://myserver.molgeniscloud.org
MG_USERNAME = username
MG_PASSWORD = password

Ensure the CatalogueOntologies database is present on the server before running the script.
"""
import logging
import os

import pandas as pd
from dotenv import load_dotenv

from tools.ontomanager.src.molgenis_emx2_ontomanager import OntologyManager
from tools.ontomanager.src.molgenis_emx2_ontomanager.exceptions import DuplicateKeyException


def dev_csv_upload(url: str = None, username: str = None, password: str = None):
    """
    Upload terms to a CatalogueOntologies table from a .csv file using the OntologyManager.
    The .csv file is first loaded into memory as a pandas DataFrame.
    This DataFrame is then passed as a parameter in the OntologyManager.add() method.

    :param url: the url of the Molgenis EMX2 server
    :param username: the username or email address of the user
    :param password: the password for this username
    """
    load_dotenv()
    if url is None:
        url = os.environ.get('MG_URL')
    if username is None:
        username = os.environ.get('MG_USERNAME')
    if password is None:
        password = os.environ.get('MG_PASSWORD')

    manager = OntologyManager(url, username, password)

    countries = pd.read_csv('data/Countries.csv')
    update_df = pd.read_csv('data/update.csv')

    print(manager.ontology_tables)
    for tbl in manager.ontology_tables:
        print(manager.list_ontology_terms(table=tbl, fmt='list'))

    # Add the terms from the countries table to the Countries ontology table on the server
    # try:
    #     manager.add(table='Countries', data=countries)
    # except DuplicateKeyException:
    #     pass
    additions = manager.add(table='Countries', data=countries)
    print(additions)

    # Update the references to the countries in the update_df dataset sequentially
    # manager.update(table='Countries', data=update_df)

    # Delete the countries that were previously added from the Countries ontology table
    deletions = manager.delete(table='Countries', names=countries['name'].tolist())
    print(deletions)


if __name__ == '__main__':
    logging.basicConfig(level='INFO')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)
    dev_csv_upload()
