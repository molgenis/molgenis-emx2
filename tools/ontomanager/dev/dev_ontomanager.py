# demo_ontomanager.py

"""Demonstration of the capabilities of the OntologyManager.
Create an OntologyManager object by supplying a server's url and login credentials as arguments in the
function below or create a .env file with the following structure

MG_URL = https://myserver.molgeniscloud.org
MG_USERNAME = username
MG_PASSWORD = password

Ensure the CatalogueOntologies database is present on the server before running the script.
"""

import logging
import os

from dotenv import load_dotenv

from tools.ontomanager.src.molgenis_emx2_ontomanager import OntologyManager
from tools.ontomanager.src.molgenis_emx2_ontomanager.exceptions import DuplicateKeyException


def dev_ontomanager(url: str = None, username: str = None, password: str = None):
    """
    Script to demonstrate the add, update, and delete functionality of the Molgenis EMX2
    Ontology Manager package.

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

    try:
        # manager.add(table='Countries', order=1000, name="Republic of Molgenia")
        manager.add(table='Countries', data={'name': 'Republic of Molgenia', 'order': 1000})
    except DuplicateKeyException as e:
        logging.error(e)
    try:
        manager.add(table='Countries', name="Armadilland", parent="Republic of Molgenia")
    except DuplicateKeyException as e:
        logging.error(e)

    manager.update(table='Countries', old='Netherlands (the)', new='Armadilland')
    manager.update(table='Countries', old='Armadilland', new='Republic of Molgenia')
    manager.update(table='Countries', old='Republic of Molgenia', new='Netherlands (the)')

    manager.delete(table='Countries', name="Armadilland")
    manager.delete(table='Countries', name="Republic of Molgenia")
    manager.delete(table='Countries', names=["Armadilland", "Republic of Molgenia"])


if __name__ == '__main__':
    logging.basicConfig(level='DEBUG')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)
    dev_ontomanager()
