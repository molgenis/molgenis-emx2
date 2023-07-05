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


def demo_csv():
    """
    Upload terms to a CatalogueOntologies table from a .csv file using the OntologyManager.
    The .csv file is first loaded into memory as a pandas DataFrame.
    This DataFrame is then passed as a parameter in the OntologyManager.add() method.
    """
    load_dotenv()

    url = os.environ['MG_URL']
    username = os.environ['MG_USERNAME']
    password = os.environ['MG_PASSWORD']

    manager = OntologyManager(url, username, password)

    countries = pd.read_csv('data/Countries.csv')
    update_df = pd.read_csv('data/update.csv')

    try:
        manager.add(table='Countries', data=countries)
    except DuplicateKeyException:
        pass

    # TODO allow updating multiple terms in update method
    # manager.update(table='Countries', data=update_df)

    manager.delete(table='Countries', names=countries['name'].tolist())


if __name__ == '__main__':
    logging.basicConfig(level='INFO')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)
    demo_csv()
