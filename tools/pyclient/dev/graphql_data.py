"""
Development script for fetching data from a schema using the GraphQL API.
"""
from pprint import pprint

from tools.pyclient.src.molgenis_emx2_pyclient import Client

URL = "https://emx2.dev.molgenis.org"
SCHEMA = "catalogue"


def get_data() -> list:
    """Fetches data."""
    participant_range = [10_000, 20_000]
    with Client(url=URL, schema=SCHEMA) as client:
        subpop = client.get(table='Subpopulations', columns=['name', 'resource', 'numberOfParticipants'],
                               query_filter=f'`numberOfParticipants` between {participant_range}',
                               as_df=False)

        pprint(subpop)

        excluded_countries = ["Denmark", "France"]
        resources = client.get(table="Resources",
                               columns=["name", "id"],
                               query_filter=f"subpopulations.countries.name != {excluded_countries}",
                               as_df=False)
        pprint(resources)
        print(len(resources))
        resources = client.get(table="Resources",
                               columns=["name", "id"],
                               query_filter=f"subpopulations.countries.name != {excluded_countries}",
                               as_df=True)
        print(resources.to_string())
        print(len(resources.index))


    return subpop



if __name__ == '__main__':
    data = get_data()
    if isinstance(data, list):
        pprint(data)
    else:
        print(data)
