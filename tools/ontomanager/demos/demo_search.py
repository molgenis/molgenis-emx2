"""
Script to demonstrate the search functionality of the Molgenis EMX2 Ontomanager package
Create an OntologyManager object by supplying a server's url and login credentials as arguments in the
function below or create a .env file with the following structure

MG_URL = https://myserver.molgeniscloud.org
MG_USERNAME = username
MG_PASSWORD = password

Ensure the CatalogueOntologies database is present on the server before running the script.
"""
import os

from dotenv import load_dotenv
from molgenis_emx2_ontomanager import OntologyManager


def demo_search(search_terms: list | str, url: str = None, username: str = None, password: str = None):
    """Function to demo search. Sign in to the server using the login details provided.

    :param search_terms: a string or list of strings
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

    if isinstance(search_terms, str):
        search_terms = list(search_terms)
    search_terms = [item for item in search_terms]
    if not isinstance(search_terms, list):
        raise ValueError("Supply the search terms as a string or a list.")

    results = dict()
    for st in search_terms:
        _table = manager.search(st)
        results[st] = _table

    return results


if __name__ == '__main__':
    terms = ['Sibling', 'Blabla', 'morbidity', 'Hospital data', 'medication']
    demo_results = demo_search(terms)
    for (term, table) in demo_results.items():
        print(f"{term}: {table}")
