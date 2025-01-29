"""
Script to test the 'get' functionality on a Directory schema with reference to ontology table.
TODO delete when done
"""
import asyncio
import logging
import os
from pprint import pprint

import numpy
import openpyxl
import pandas as pd
from dotenv import load_dotenv

from tools.pyclient.src.molgenis_emx2_pyclient import Client
from tools.pyclient.src.molgenis_emx2_pyclient.exceptions import (NoSuchSchemaException, NoSuchTableException,
                                                                  GraphQLException, PermissionDeniedException)


async def main():
    # Set up the logger
    logging.basicConfig(level='INFO')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    # Load the login details into the environment
    load_dotenv()
    token = os.environ.get('MG_TOKEN')

    with Client(url="http://localhost:8080", schema="directory-demo") as client:
        collections = client.get(table="Collections", as_df=False)

        pprint(collections)


if __name__ == '__main__':
    asyncio.run(main())
