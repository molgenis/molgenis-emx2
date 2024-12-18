"""
Development script for fetching data from a schema using the GraphQL API.
"""
from pprint import pprint

from tools.pyclient.src.molgenis_emx2_pyclient import Client

URL = "https://emx2.dev.molgenis.org"
SCHEMA = "catalogue"


def get_data() -> list:
    """Fetches data."""

    with Client(url=URL, schema=SCHEMA) as client:
        resources = client.get(table='Resources')


    return resources



if __name__ == '__main__':
    data = get_data()
    pprint(data)
