"""
Script to test the 'get' functionality on a Directory schema with reference to ontology table.
TODO delete when done
"""
import asyncio
import logging

from dotenv import load_dotenv

from tools.pyclient.src.molgenis_emx2_pyclient import Client


def get_collections():
    """Gets Collections from directory-demo in list[dict] format."""
    with Client(url="http://localhost:8080", schema="directory-demo") as client:
        collections = client.get(table="Collections", as_df=False)
        for col in collections:
            for (k, v) in col.items():
                print(f"{k:30}: {v}")

        collections = client.get_graphql(table="Collections")
        for col in collections:
            for (k, v) in col.items():
                print(f"{k:30}: {v}")

def get_orders():
    """Gets Orders from pet store in DataFrame format."""

    with Client(url="http://localhost:8080", schema="pet store") as client:
        orders = client.get(table="Order", as_df=True)

        print(orders.to_string())

def get_longlat():
    """Gets long/lat info from directory-demo::Biobanks"""
    with Client(url="http://localhost:8080", schema="directory-demo") as client:
        biobanks = client.get(table="Biobanks", as_df=True)

    print(biobanks.to_string())
    print(biobanks.dtypes)



async def main():
    # Set up the logger
    logging.basicConfig(level='INFO')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    # Load the login details into the environment
    load_dotenv()

    get_collections()



if __name__ == '__main__':
    asyncio.run(main())
