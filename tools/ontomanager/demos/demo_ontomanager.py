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

from tools.ontomanager.src.molgenis_ontomanager import OntologyManager
from tools.ontomanager.src.molgenis_ontomanager.exceptions import DuplicateKeyException


def demo_ontomanager(url: str = None, username: str = None, password: str = None):
    load_dotenv()
    if url is None:
        url = os.environ.get('MG_URL')
    if username is None:
        username = os.environ.get('MG_USERNAME')
    if password is None:
        password = os.environ.get('MG_PASSWORD')

    manager = OntologyManager(url, username, password)

    try:
        manager.add(table='Countries', order=1000, name="Republic of Molgenia", label="Molgenia")
    except DuplicateKeyException:
        pass
    try:
        manager.add(table='Countries', name="Armadilland", parent="Republic of Molgenia")
    except DuplicateKeyException:
        pass

    manager.update(table='Countries', old='Croatia', new='Armadilland')
    manager.update(table='Countries', old='Armadilland', new='Republic of Molgenia')
    manager.update(table='Countries', old='Republic of Molgenia', new='Croatia')

    manager.delete(table='Countries', name="Armadilland")
    manager.delete(table='Countries', name="Republic of Molgenia")


if __name__ == '__main__':
    logging.basicConfig(level='INFO')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)
    demo_ontomanager()
