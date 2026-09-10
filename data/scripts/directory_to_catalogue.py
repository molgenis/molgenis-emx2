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
    await client.upload_file(file_path=staging_schema_csv, schema=staging_schema)
    os.unlink(schema_csv)
    os.unlink(staging_schema_csv)


def get_directory_data():
    """Get Directory data"""
    data = {}
    tables = ["Biobanks", "Collections", "CollectionFacts", "Networks"]
    with pyclient.Client(url="https://directory.bbmri-eric.eu", schema="ERIC") as client:
        for table in tables:
            # FIXME: reading from file is only for speeding up testing, remove afterwards
            try:
                data[table] = pd.read_csv(f"{table}.csv")
            except FileNotFoundError:
                data[table] = client.get(table, as_df=True)
                pyclient.utils.data_to_csv(data=data[table], filename=f"{table}.csv")
    return data


def convert_biobanks_to_biobanks(bb, mappings):
    """Convert Biobanks to Biobanks"""
    # Select only those directory Biobanks which will be converted to the Biobanks table
    biobank_ids = mappings.loc[
        (mappings["mapping_rule"].str.startswith("biobank 2+coll"))
        | (mappings["mapping_rule"].str.startswith("biobank 1:1 real-org")),
        "directory_id",
    ]
    bb = bb.loc[bb["id"].isin(biobank_ids)]
    bb["part of"] = bb["juridical_person"]
    return bb


def convert_biobanks_networks_to_organisations(bb, networks, jp_prefix):
    """Convert Biobanks and Networks to Organisations"""
    # Select biobanks without collections
    bb_no_coll = bb.loc[bb["collections"].isna()]
    # Convert juridical persons of all biobanks with collections to organisations
    jp = bb.loc[bb["collections"].notna()]
    # Add juridical persons from networks
    jp = pd.concat([jp, networks.loc[networks["juridical_person"].notna()]])
    # Prevent duplicate names
    jp["name"] = jp["juridical_person"]
    jp = jp.drop_duplicates(subset="name", ignore_index=True)
    jp["id"] = jp_prefix + jp.index.astype(str)
    orgs = pd.concat([bb_no_coll, jp])
    return orgs


def convert_collections_to_collections(coll, mappings):
    """
    Convert standalone/parent Collections to Collections
    Convert subcollections marked as such to Collections + Linkages
    Convert subcollections needing curation to temporary Collections + Linkages
    """
    parent_collection_ids = mappings.loc[
        mappings["mapping_rule"] == "standalone/parent collection -> Collections (direct)",
        "directory_id",
    ]
    parent_coll = coll.loc[coll["id"].isin(parent_collection_ids)]
    to_promote_collection_ids = mappings.loc[
        mappings["mapping_rule"]
        == "sub-collection type-varies (robust) -> promoted Collection + Linkage(wasDerivedFrom)",
        "directory_id",
    ]
    to_promote_coll = coll.loc[coll["id"].isin(to_promote_collection_ids)]
    to_curate_collection_ids = mappings.loc[
        (mappings["directory_table"] == "Collections")
        & (mappings["disposition"] == "needs_curation"),
        "directory_id",
    ]
    to_curate_coll = coll.loc[coll["id"].isin(to_curate_collection_ids)]
    # Add linkages from to-be-curated and promoted subcollections to their parent collections
    links_1 = to_promote_coll.reindex(columns=["id", "parent_collection"])
    links_1 = links_1.rename(columns={"id": "resource", "parent_collection": "linked resource"})
    links_1["relationship type"] = (
        "hasParentCollection (Directory subcollection which was promoted)"
    )
    links_2 = to_curate_coll.reindex(columns=["id", "parent_collection"])
    links_2 = links_2.rename(columns={"id": "resource", "parent_collection": "linked resource"})
    links_2["relationship type"] = (
        "Directory subcollections migrated as temporary Collections, to be curated."
    )
    links = pd.concat([links_1, links_2])
    coll = pd.concat([parent_coll, to_promote_coll, to_curate_coll])
    # Attribute-level operations which apply to all Collections
    coll["held by"] = coll["biobank"]
    coll["type"] = "Biobank"
    return coll, links


def convert_collections_to_facts(facts, mappings):
    """Convert subcollections to Collection facts"""
    fact_ids = mappings.loc[
        (mappings["directory_table"] == "Collections")
        & (mappings["catalogue_table"] == "Collection facts"),
        "directory_id",
    ]
    facts = facts[facts["id"].isin(fact_ids)]
    facts["collection"] = facts["parent_collection"]
    # TODO for all dimensions: if more than 1 value, then *,
    # if 1 value, then that value, if no value, then 'Unknown' or some other null value
    return facts


def convert_collections_to_events(events, mappings):
    """Convert Collections to Collection events"""
    event_ids = mappings.loc[
        (mappings["directory_table"] == "Collections")
        & (mappings["catalogue_table"] == "Collection events"),
        "directory_id",
    ]
    events = events[events["id"].isin(event_ids)]
    events = events.rename(columns={'parent_collection': 'resource', 'id': 'pid'})
    return events


def convert_collections_to_subpopulations(subpop, mappings):
    """Convert Collections to Subpopulations"""
    subpop_ids = mappings.loc[
        (mappings["directory_table"] == "Collections")
        & (mappings["catalogue_table"] == "Subpopulations"),
        "directory_id",
    ]
    subpop = subpop[subpop["id"].isin(subpop_ids)]
    subpop = subpop.rename(columns={'parent_collection': 'resource', 'id': 'pid'})
    return subpop


def convert_networks_to_networks(networks):
    """Convert Networks to Networks"""
    networks = networks.rename(columns={"juridical_person": "organisations involved"})
    return networks


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
    jp_prefix = "organisation_minted_from_juridical_person_"
    # Get mappings
    mappings = pd.read_csv(os.path.join(os.getcwd(), "docs/directory-merge/mapping_ledger.csv"))
    with pyclient.Client(url=server, token=token) as client:
        if reset or schema not in client.schema_names or staging_schema not in client.schema_names:
            await clear_schemas(client, schema, staging_schema)
        # Get data
        data = get_directory_data()
        # Pre-process data
        # Deal with duplicate names in Biobanks table
        data["Biobanks"].loc[
            (data["Biobanks"]["name"].duplicated(keep=False)) & (data["Biobanks"]["withdrawn"]),
            "name",
        ] += " (withdrawn)"
        data["Biobanks"].loc[(data["Biobanks"]["name"].duplicated(keep="first")), "name"] += " (2)"
        # Deal with duplicate names in Collections table
        count = 1
        while data["Collections"]["name"].duplicated(keep=False).sum() > 0:
            data["Collections"].loc[
                data["Collections"]["name"].duplicated(keep="first"), "name"
            ] += f" ({count})"
            count += 1
        # Deal with problematic subsubcollection
        data["Collections"].loc[
            data["Collections"]["id"] == "bbmri-eric:ID:HU_SUB:collection:DGCI_OSC",
            "parent_collection",
        ] = "bbmri-eric:ID:HU_SUB:collection:DGCI"
        # Deal with duplicate Networks entry
        data["Networks"] = data["Networks"].drop(
            data["Networks"].loc[data["Networks"]["id"] == "bbmri-eric:networkID:FI_finbb"].index
        )
        # Convert data
        organisations = convert_biobanks_networks_to_organisations(
            data["Biobanks"].copy(), data["Networks"].copy(), jp_prefix
        )
        organisations = organisations.reindex(
            columns=[
                "id",
                "name",
            ]
        )
        biobank_mappings = mappings.loc[mappings["directory_table"] == "Biobanks"]
        biobanks = convert_biobanks_to_biobanks(data["Biobanks"].copy(), biobank_mappings)
        biobanks = biobanks.reindex(
            columns=[
                "id",
                "name",
                "part of",
            ]
        )
        collections, linkages = convert_collections_to_collections(
            data["Collections"].copy(), mappings
        )
        collections = collections.reindex(
            columns=[
                "id",
                "name",
                "held by",
                "type",
                "description",
            ]
        )
        linkages = linkages.reindex(columns=["resource", "linked resource", "relationship type"])
        collection_facts = convert_collections_to_facts(data["Collections"].copy(), mappings)
        collection_facts = collection_facts.reindex(columns=["id", "collection"])
        collection_events = convert_collections_to_events(data["Collections"].copy(), mappings)
        collection_events = collection_events.reindex(columns=["resource", "name", "pid"])
        subpopulations = convert_collections_to_subpopulations(data["Collections"].copy(), mappings)
        subpopulations = subpopulations.reindex(columns=["resource", "name", "pid"])
        networks = convert_networks_to_networks(data["Networks"].copy())
        networks = networks.reindex(columns=["id", "name", "organisations involved"])
        # Post-process data
        # Link collections to their newly minted legal-entity organisations
        jp_orgs = organisations.loc[organisations["id"].str.startswith(jp_prefix)]
        collections.loc[~collections["held by"].isin(biobanks["id"]), "held by"] = (
            jp_orgs.set_index("name")
            .loc[
                data["Biobanks"]
                .set_index("id")
                .loc[
                    collections.loc[~collections["held by"].isin(biobanks["id"]), "held by"],
                    "juridical_person",
                ],
                "id",
            ]
            .values
        )
        # Create organisation roles
        organisation_roles = networks.loc[networks["organisations involved"].notna()]
        del organisation_roles["name"]
        organisation_roles = organisation_roles.rename(
            columns={"id": "resource", "organisations involved": "organisation"}
        )
        organisation_roles["organisation"] = (
            jp_orgs.set_index("name").loc[organisation_roles["organisation"], "id"].values
        )
        organisation_roles["role"] = "Other"  # TODO: TBD what role this should be
        # Link biobanks to their legal-entity organisations
        biobanks["part of"] = jp_orgs.set_index("name").loc[biobanks["part of"], "id"].values
        # Ensure newly minted organisations have a distinct name from biobanks, collections and networks
        organisations.loc[
            organisations["id"].isin(jp_orgs["id"])
            & (
                (organisations["name"].isin(biobanks["name"]))
                | (organisations["name"].isin(collections["name"]))
                | (organisations["name"].isin(networks["name"]))
            ),
            "name",
        ] += " (juridical person)"
        # Ensure networks have a distinct name from biobanks and collections
        networks.loc[
            (networks["name"].isin(collections["name"]))
            | (networks["name"].isin(biobanks["name"])),
            "name",
        ] += " (network)"
        # Ensure biobanks have a distinct name from collections
        biobanks.loc[biobanks["name"].isin(collections["name"]), "name"] += " (biobank)"
        # Ensure organisations derived from 0-coll biobanks have a distinct name from collections
        organisations.loc[
            organisations["name"].isin(collections["name"]), "name"
        ] += " (organisation)"
        # Clear and upload data
        if truncate:
            print("Truncating...")
            client.truncate(table="Linkages", schema=schema)
            client.truncate(table="Organisation roles", schema=schema)
            client.truncate(table="Networks", schema=schema)
            client.truncate(table="Subpopulations", schema=schema)
            client.truncate(table="Collection events", schema=schema)
            client.truncate(table="Collection facts", schema=schema)
            client.truncate(table="Collections", schema=schema)
            client.truncate(table="Biobanks", schema=schema)
            client.truncate(table="Organisations", schema=schema)
        print("Uploading...")
        client.save_table(table="Organisations", schema=schema, data=organisations)
        client.save_table(table="Biobanks", schema=schema, data=biobanks)
        client.save_table(table="Collections", schema=schema, data=collections)
        client.save_table(table="Collection facts", schema=schema, data=collection_facts)
        client.save_table(table="Collection events", schema=schema, data=collection_events)
        client.save_table(table="Subpopulations", schema=schema, data=subpopulations)
        client.save_table(table="Networks", schema=schema, data=networks)
        client.save_table(table="Organisation roles", schema=schema, data=organisation_roles)
        client.save_table(table="Linkages", schema=schema, data=linkages)
        pass


if __name__ == "__main__":
    asyncio.run(main())
