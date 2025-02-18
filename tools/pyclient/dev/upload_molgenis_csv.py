"""
Script to upload a molgenis.csv schema file to a schema using the Pyclient.

Generate a token on the dev server as admin and create a .env file in this folder and add the line
MOLGENIS_TOKEN="..."
where you put the generated token in place of the dots.
"""

import asyncio
import logging
import os
from pathlib import Path

from dotenv import load_dotenv

from tools.pyclient.src.molgenis_emx2_pyclient import Client
from tools.pyclient.src.molgenis_emx2_pyclient.exceptions import PyclientException

log = logging.getLogger("upload test")
logging.basicConfig(level="DEBUG")

SCHEMA_NAME = "issue fix 4735"
SCHEMA_DESCRIPTION = "https://github.com/molgenis/molgenis-emx2/issues/4735"


async def main(url: str, schema: str):
    load_dotenv()

    with Client(url=url, token=os.environ.get("MOLGENIS_TOKEN")) as client:
        await client.create_schema(name=schema, description=SCHEMA_DESCRIPTION)
        client.set_schema(schema)
        Path("catalogue.csv").rename("molgenis.csv")
        await client.upload_file("molgenis.csv")
        Path("molgenis.csv").rename("catalogue.csv")

        organisation_columns = client.get_schema_metadata().get_table('id', "Organisations").columns
        log.debug(f"{len(organisation_columns)} columns in Organisations before update.")
        log.debug(list(map(str, organisation_columns)))

        Path("organisations-only.csv").rename("molgenis.csv")
        try:
            await client.upload_file("molgenis.csv")
        except PyclientException as e:
            log.error(e)
        finally:
            Path("molgenis.csv").rename("organisations-only.csv")

        client.get_schema_metadata.cache_clear()
        organisation_columns = client.get_schema_metadata().get_table('id', "Organisations").columns
        log.debug(f"{len(organisation_columns)} columns in Organisations after update.")
        log.debug(list(map(str, organisation_columns)))

        await client.upload_file("Resources.csv")
        await client.upload_file("Organisations.csv")

        await client.delete_schema(schema)


if __name__ == '__main__':
    asyncio.run(main("https://emx2.dev.molgenis.org", SCHEMA_NAME))
