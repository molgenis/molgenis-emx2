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

logging.basicConfig(level="INFO")


async def main(url: str, schema: str):
    load_dotenv()

    with Client(url=url, token=os.environ.get("MOLGENIS_TOKEN")) as client:
        await client.create_schema(name=schema)
        client.set_schema(schema)
        Path("catalogue.csv").rename("molgenis.csv")
        await client.upload_file("molgenis.csv")
        Path("molgenis.csv").rename("catalogue.csv")

        Path("organisations-only.csv").rename("molgenis.csv")
        try:
            await client.upload_file("molgenis.csv")
        except PyclientException as e:
            logging.error(e)
        finally:
            Path("molgenis.csv").rename("organisations-only.csv")

        await client.delete_schema(schema)


if __name__ == '__main__':
    asyncio.run(main("https://emx2.dev.molgenis.org", "issue fix 4735"))
