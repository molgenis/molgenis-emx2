"""
Script for publishing collections metadata from one source catalogue to a
target schema. This script is designed to be executed in the _SYSTEM_
environment in the target catalogue.

The source schema should be publicly available.

Specify below: the URL of the source,
               the name of the source schema,
               the name of the target schema,
               the ids of the collections that should be copied to the target.

"""

import asyncio
import logging
import os
import sys
from pathlib import Path
from zipfile import ZipFile

import pandas as pd
from molgenis_emx2_pyclient import Client
from molgenis_emx2_pyclient.metadata import Schema, Table
from molgenis_emx2_pyclient.utils import response_to_dataframe

SOURCE_URL = ""
SOURCE_SCHEMA = ""

TARGET_URL = "http://localhost:8080/"
TARGET_SCHEMA = ""
TARGET_TOKEN = os.environ.get("MOLGENIS_TOKEN")

COLLECTIONS = []


logging.basicConfig(level=logging.DEBUG, stream=sys.stdout)
logger = logging.getLogger("Cross-catalogue publisher")

UPLOAD_DIR = Path(__file__).parent / "upload"


async def main():
    """The main function executing all steps."""
    set_up()
    prepare_source_schema_data()
    create_zip()
    await upload_zip()
    clean_up()


def prepare_source_schema_data():
    """Prepares the data on the source schema for uploading."""
    with Client(url=SOURCE_URL, schema=SOURCE_SCHEMA) as source_client:
        metadata: Schema = source_client.get_schema_metadata()

        for table in filter_tables(metadata):
            logger.info("Preparing table '%s'", table.name)
            if table.name == "Collections":
                prepare_collections(source_client, table)
            elif len(table.get_columns(by="id", value="resource")) != 0:
                prepare_resource_table(source_client, table)
            elif len(table.get_columns(by="id", value="source")) != 0:
                prepare_source_table(source_client, table)


def set_up():
    """Sets up the file structure."""
    if UPLOAD_DIR.exists():
        clean_up()
    UPLOAD_DIR.mkdir()
    (UPLOAD_DIR / "_files").mkdir()


def filter_tables(meta: Schema) -> list:
    """Filters out the tables that other tables inherit from."""
    inherit_tables = {
        getattr(t, "inheritId") for t in meta.tables if hasattr(t, "inheritId")
    }
    return [t for t in meta.tables if t.name not in inherit_tables]


def prepare_collections(client: Client, table: Table):
    """Prepares the collections table."""
    id_filter = ",".join(f'"{c}"' for c in COLLECTIONS)
    table_filter = '{"id":{"equals":[' + id_filter + "]}}"
    url = (f"{SOURCE_URL}/{SOURCE_SCHEMA}/api/csv/"
           f"{table.name}?filter={table_filter}")
    fetch_data(client, table, url)


def prepare_resource_table(client: Client, table: Table):
    """Prepares tables with a 'resource' column."""
    id_filter = ",".join('{"id":"' + c + '"}' for c in COLLECTIONS)
    table_filter = '{"resource":{"equals":[' + id_filter + "]}}"
    url = (f"{SOURCE_URL}/{SOURCE_SCHEMA}/api/csv/"
           f"{table.name}?filter={table_filter}")
    fetch_data(client, table, url)


def prepare_source_table(client: Client, table: Table):
    """Prepares tables with a 'source' column."""
    id_filter = ",".join('{"id":"' + c + '"}' for c in COLLECTIONS)
    table_filter = '{"source":{"equals":[' + id_filter + "]}}"
    url = (f"{SOURCE_URL}/{SOURCE_SCHEMA}/api/csv/"
           f"{table.name}?filter={table_filter}")
    fetch_data(client, table, url)


def fetch_data(client: Client, table: Table, url: str):
    """Fetches data from the URL and writes to CSV file."""
    response = client.session.get(url)
    df = response_to_dataframe(response, table)
    if len(df.index) != 0:
        fetch_files(client, table, df)
        df.to_csv(UPLOAD_DIR / f"{table.name}.csv", index=False)


def fetch_files(client: Client, table: Table, df: pd.DataFrame):
    """Fetches the files stored in the table."""
    file_columns = [c.name for c in table.columns
                    if getattr(c, "columnType") == "FILE"]
    if len(file_columns) == 0:
        return

    for fc in file_columns:
        for f_id, f_name in df[[fc, fc + "_filename"]].dropna(axis=0).values:
            url = (f"{SOURCE_URL}/{SOURCE_SCHEMA}/api/file/"
                   f"{table.name}/{fc}/{f_id}")
            f_ext = f_name.split(".")[-1]
            response = client.session.get(url)
            with open(UPLOAD_DIR / "_files" / f"{f_id}.{f_ext}", "w+b") as f:
                f.write(response.content)


def create_zip():
    """Zips the contents of the UPLOAD_DIR."""
    with ZipFile(UPLOAD_DIR.parent / "upload.zip", "w") as zf:
        for file in UPLOAD_DIR.iterdir():
            if file.is_dir():
                for f in file.iterdir():
                    zf.write(f, "_files/" + f.name)
            else:
                zf.write(file, file.name)


async def upload_zip():
    """Uploads the zip file to the target schema."""
    with Client(
        url=TARGET_URL, schema=TARGET_SCHEMA, token=TARGET_TOKEN, job="${jobId}"
    ) as target_client:
        await target_client.upload_file(UPLOAD_DIR.parent / "upload.zip")


def clean_up():
    """Cleans up the files after uploading."""

    def delete_dir(path: Path):
        for file in path.iterdir():
            if file.is_dir():
                delete_dir(file)
            else:
                file.unlink()
        path.rmdir()

    delete_dir(UPLOAD_DIR)
    (UPLOAD_DIR.parent / "upload.zip").unlink(missing_ok=True)


if __name__ == "__main__":
    asyncio.run(main())
