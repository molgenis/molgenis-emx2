"""
Development script for fetching data from a schema using the GraphQL API.
"""
from pprint import pprint

from tools.pyclient.src.molgenis_emx2_pyclient import Client

URL = "https://emx2.dev.molgenis.org"
SCHEMA = "catalogue"


def get_data():
    """Fetches data."""
    participant_range = [10_000, 20_000]
    with Client(url=URL, schema=SCHEMA) as client:

        organisations = client.get(table="Organisations",
                                   columns=["resource", "id", "is lead organisation"],
                                   query_filter="id != UMCG",
                                   as_df=True)
        print(organisations)
        print(len(organisations.index))


        excluded_countries = ["Denmark", "France"]
        resources = client.get(table="Resources",
                               columns=["id", "name"],
                               query_filter=f"subpopulations.countries.name != {excluded_countries}",
                               as_df=True)
        pprint(resources)
        print(len(resources))

        resources = client.get(table="Resources",
                               columns=["id", "name"],
                               query_filter=f"subpopulations.countries.name != {excluded_countries}",
                               as_df=False)
        pprint(resources)
        print(len(resources))

        subpop = client.get(table='Subpopulations', columns=['name', 'resource', 'numberOfParticipants'],
                               query_filter=f'`numberOfParticipants` between {participant_range}',
                               as_df=False)

        pprint(subpop)

        resources = client.get(table="Resources",
                               columns=["id", "name", "start year"],
                               query_filter=f"startYear < 1999",
                               as_df=True)
        print(resources.to_string())
        print(len(resources.index))



if __name__ == '__main__':
    get_data()
