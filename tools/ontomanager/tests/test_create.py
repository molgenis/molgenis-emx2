import os
import logging

from dotenv import load_dotenv

from tools.ontomanager.src.ontomanager import OntologyManager


def test_create(url: str = None, username: str = None, password: str = None):
    load_dotenv()
    if url is None:
        url = os.environ['MG_URL']
    if username is None:
        username = os.environ['MG_USERNAME']
    if password is None:
        password = os.environ['MG_PASSWORD']

    manager = OntologyManager(url, username, password)

    manager.add(table='Countries', order=1000, name="Republic of Molgenia", label="Molgenia")
    manager.add(table='Countries', name="Armadilland", parent="Republic of Molgenia")
    manager.delete(table='Countries', name="Armadilland")
    manager.delete(table='Countries', name="Republic of Molgenia")


if __name__ == '__main__':
    logging.basicConfig(level='INFO')
    test_create()
