"""
Script to demonstrate the search functionality of the Molgenis EMX2 Ontomanager package
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


def dev_search(search_terms: list | str):
    """Function to demo search. Sign in to the server using the login details provided.

    :param search_terms: a string or list of strings
    :param url: the url of the Molgenis EMX2 server
    :param username: the username or email address of the user
    :param password: the password for this username
    """
    load_dotenv()

    url = os.environ['MG_URL']
    username = os.environ['MG_USERNAME']
    password = os.environ['MG_PASSWORD']

    manager = OntologyManager(url, username, password)

    if isinstance(search_terms, str):
        search_terms = list(search_terms)
    if not isinstance(search_terms, list):
        raise ValueError("Supply the search terms as a string or a list.")

    results = dict()
    for st in search_terms:
        _table = manager.search(st, find_usage=True)
        results[st] = _table

    return results


if __name__ == '__main__':
    logging.basicConfig(level='DEBUG')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)
    terms = ['Sibling', 'Croatia', 'Non-profit organisations', 'Hospital data', 'medication']
    demo_results = dev_search(terms)
    for (term, table) in demo_results.items():
        print(f"{term}: {table}")
