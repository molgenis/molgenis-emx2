"""Script to migrate Directory data to the integrated catalogue model"""

import asyncio
import os

import pandas as pd
import molgenis_emx2_pyclient as pyclient
from dotenv import load_dotenv


def build_data_model(path, profile):
    """Build data model from CSVs in directory based on profile"""
    data_model = pd.DataFrame()
    for file_name in os.listdir(path):
        if os.path.splitext(file_name)[-1] == ".csv":
            file_path = os.path.join(path, file_name)
            df = pd.read_csv(file_path, keep_default_na=False, dtype="object")
            df = df.loc[df["profiles"].apply(lambda p: profile in p.split(","))]
            data_model = pd.concat([data_model, df])
    data_model.to_csv("molgenis_" + profile + ".csv", index=None)


async def clear_schemas(client, schema, staging_schema):
    """Delete existing testing schemas and create empty new ones"""
    profile = "DataCatalogueFlat"
    staging_profile = "CohortsStaging"
    path = os.path.join(os.getcwd(), "data/_models/shared/")
    build_data_model(path, profile)
    build_data_model(path, staging_profile)
    if schema in client.schema_names:
        await client.delete_schema(schema)
    if staging_schema in client.schema_names:
        await client.delete_schema(staging_schema)
    await client.create_schema(schema)
    await client.create_schema(staging_schema)
    schema_csv = "molgenis_" + profile + ".csv"
    staging_schema_csv = "molgenis_" + staging_profile + ".csv"
    await client.upload_file(file_path=schema_csv, schema=schema)
    # FIXME: upload main schema instead until issue with creating staging areas is fixed
    await client.upload_file(file_path=schema_csv, schema=staging_schema)
    os.unlink(schema_csv)
    os.unlink(staging_schema_csv)


def get_directory_data():
    """Get Directory data"""
    data = {}
    tables = ["Biobanks"]  # , "Collections"]
    with pyclient.Client(url="https://directory.bbmri-eric.eu", schema="ERIC") as client:
        for table in tables:
            data[table] = client.get(table, as_df=True)
    return data


def convert_biobanks_to_biobanks(bb, mappings):
    """Convert Biobanks table"""
    # Select only those directory Biobanks which will be converted to catalogue Biobanks
    biobank_ids = mappings.loc[
        mappings["mapping_rule"].str.contains("-> Biobanks holding"), "directory_id"
    ]
    bb = bb.loc[bb["id"].isin(biobank_ids)]
    # Deal with duplicate names
    bb.loc[(bb["name"].duplicated(keep=False)) & (bb["withdrawn"]), "name"] += " (withdrawn)"
    bb.loc[(bb["name"].duplicated(keep="first")), "name"] += " (2)"
    return bb


async def main():
    """Main function"""
    load_dotenv()
    server = os.environ.get("SERVER")
    token = os.environ.get("TOKEN")
    reset = False
    if os.environ.get("RESET").strip().lower() == "true":
        reset = True
    truncate = False
    if os.environ.get("TRUNCATE").strip().lower() == "true":
        truncate = True
    schema = "directory-catalogue-integration"
    staging_schema = schema + "-staging"
    # Get mappings
    mappings = pd.read_csv(os.path.join(os.getcwd(), "docs/directory-merge/mapping_ledger.csv"))
    with pyclient.Client(url=server, token=token) as client:
        if reset:
            await clear_schemas(client, schema, staging_schema)
        # Get data
        data = get_directory_data()
        # Convert data
        biobank_mappings = mappings.loc[mappings["directory_table"] == "Biobanks"]
        biobanks = convert_biobanks_to_biobanks(data["Biobanks"].copy(), biobank_mappings)
        biobanks = biobanks.reindex(
            columns=[
                "id",
                "name",
            ]
        )
        # Clear and upload data
        if truncate:
            client.truncate(table="Biobanks", schema=schema)
        client.save_table(table="Biobanks", schema=schema, data=biobanks)
        pass


if __name__ == "__main__":
    asyncio.run(main())
