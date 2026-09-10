"""Script to check whether all Directory data was migrated"""

import asyncio
import os

import pandas as pd
import molgenis_emx2_pyclient as pyclient
from dotenv import load_dotenv

async def main():
    """Main function"""
    load_dotenv()
    server = os.environ.get("SERVER")
    token = os.environ.get("TOKEN")
    schema = "directory-catalogue-integration"
    # Get original data
    data = {}
    tables = ["Collections"]
    with pyclient.Client(url="https://directory.bbmri-eric.eu", schema="ERIC") as client:
        for table in tables:
            data[table] = client.get(table, as_df=True)
    # Get migrated data
    migrated_data = {}
    tables = ["Collections", "Collection events", "Collection facts", "Subpopulations"]
    with pyclient.Client(url=server, schema=schema, token=token) as client:
        for table in tables:
            migrated_data[table] = client.get(table, as_df=True)
    # Check collection IDs, delete if found in migrated data
    data["Collections"] = data["Collections"].set_index("id")
    data["Collections"] = data["Collections"].drop(migrated_data["Collections"]["id"], errors='ignore')
    data["Collections"] = data["Collections"].drop(migrated_data["Collection events"]["pid"], errors='ignore')
    data["Collections"] = data["Collections"].drop(migrated_data["Collection facts"]["id"], errors='ignore')
    data["Collections"] = data["Collections"].drop(migrated_data["Subpopulations"]["pid"], errors='ignore')


if __name__ == "__main__":
    asyncio.run(main())
