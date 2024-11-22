"""
Script to convert the BBMRI-ERIC directory data to the flat model
"""

import argparse
import molgenis_emx2_pyclient
import pandas as pd


def add_to_array(array: str, item: str):
    """Add an item to an existing array in string format"""
    if pd.isna(array) or array == "":
        array = item
    else:
        array += ","
        array += item
    return array


def map_persons_to_contacts(persons, collections, biobanks, networks, resources):
    """Maps the BBRMI-ERIC Persons table to the flat data model's Contacts table"""
    persons.rename(
        columns={"first_name": "first name", "last_name": "last name", "role": "role description"},
        inplace=True,
    )
    # Add dummy values for missing first and last names
    persons["first name"] = persons["first name"].replace(r"^$", "FirstName", regex=True)
    persons["last name"] = persons["last name"].replace(r"^$", "LastName", regex=True)
    # Do partial mappings of title_before_name to pre-defined titles
    # TODO: update when flat data model supports post-nominal titles
    persons["title_before_name"] = (
        persons["title_before_name"].str.lower().str.replace("[^a-z]", "", regex=True)
    )
    title_mapping = {
        "dr": "dr.",
        "profdr": "prof. dr.",
        "prof": "prof.",
        "mwdr": "dr.",
        "profdrmed": "prof. dr.",
        "pddr": "dr.",
        "drrernat": "dr.",
        "drmed": "dr.",
    }
    persons["title"] = persons["title_before_name"].map(title_mapping)
    # Partial mappings of manually entered roles to pre-defined roles
    role_mapping = {
        "PI": "Principal Investigator",
        "Principal Investigator": "Principal Investigator",
        "Principal Investigators": "Principal Investigator",
        "Principle Investigator": "Principal Investigator",
        "Project coordinator": "Project manager",
        "Project Co-ordinator": "Project manager",
        "Project Coordinator": "Project manager",
    }
    persons["role"] = persons["role description"].map(role_mapping)
    # Add role 'Other' for un-mapped roles
    persons.loc[(persons["role description"] != "") & (persons["role"].isna()), "role"] = "Other"
    # Link people to resources, create new entries
    persons["resource"] = ""
    persons = persons.set_index("id")
    linked_persons = []
    # Go through networks, biobanks, add a new entry for each link
    for _, row in biobanks.iterrows():
        contact = persons.loc[row["contact"]].copy()
        contact["resource"] = row["id"]
        contact["role"] = add_to_array(contact["role"], "Primary contact")
        if row["head"]:
            head = persons.loc[row["head"]].copy()
            # Check whether head and contact are duplicates w.r.t. first and last name,
            # regardless of id
            if (
                head["first name"] == contact["first name"]
                and head["last name"] == contact["last name"]
            ):
                contact["role"] = add_to_array(contact["role"], "Biobank head")
            else:
                head["resource"] = row["id"]
                head["role"] = add_to_array(head["role"], "Biobank head")
                linked_persons.append(head)
        linked_persons.append(contact)

    for _, row in networks.iterrows():
        contact = persons.loc[row["contact"]].copy()
        contact["resource"] = row["id"]
        contact["role"] = add_to_array(contact["role"], "Primary contact")
        linked_persons.append(contact)

    linked_persons = pd.DataFrame(linked_persons)
    linked_persons["id"] = linked_persons.index

    return linked_persons


def map_collections_to_resources(collections):
    """Maps the BBMRI-ERIC Collections table to the flat data model's Resources table"""
    # TODO: Collections will be mapped to Samplesets instead
    # Rename and create columns
    collections.rename(columns={"url": "website"}, inplace=True)
    collections["resources"] = ""
    # Create a unique name
    collections["name"] = (
        collections["name"]
        + " from "
        + collections["biobank_label"]
        + " (id: "
        + collections["id"]
        + ")"
    )
    # Add collection-to-subcollection links
    collections.index = collections["id"]
    for idx, row in collections.iterrows():
        p_c = row["parent_collection"]
        if p_c:
            if collections.loc[p_c, "resources"] != "":
                collections.loc[p_c, "resources"] += ","
            collections.loc[p_c, "resources"] += idx
    # Map types from different ontologies, map to 'Other'
    # if no suitable candidate exists in the target ontology
    # TODO: if there are still unmapped types ultimately,
    # add the original type description in the type_other column
    type_mapping = {
        "QUALITY_CONTROL": "Other",
        "BIRTH_COHORT": "Cohort study",
        "POPULATION_BASED": "Cohort study",
        "DISEASE_SPECIFIC": "Disease specific",
        "NON_HUMAN": "Other",
        "CASE_CONTROL": "Other",
        "OTHER": "Other",
        "SAMPLE": "Other",
        "RD": "Rare disease",
        "IMAGE": "Other",
        "PROSPECTIVE_COLLECTION": "Study",
        "HOSPITAL": "Registry",
        "CROSS_SECTIONAL": "Cohort study",
        "COHORT": "Cohort study",
        "TWIN_STUDY": "Other",
        "LONGITUDINAL": "Cohort study",
    }
    collections["type"] = collections["type"].map(
        lambda l: ",".join(set([type_mapping[t] for t in l.split(",")]))
    )
    # Add default type 'Sample collection'
    collections["type"] += ",Sample collection"
    return collections


def map_biobanks_to_resources(biobanks, resources):
    """Maps the BBMRI-ERIC Biobanks table to the flat data model's Resources table"""
    # Rename and create columns
    biobanks = biobanks.rename(columns={"url": "website"})
    biobanks["resources"] = ""
    # Create a unique name
    biobanks["name"] = biobanks["name"] + " (id: " + biobanks["id"] + ")"
    # Add default type 'Biobank
    biobanks["type"] = "Biobank"
    # TODO: link biobank to all its component sample collections
    # TODO: collections will be samplesets, so change to use samplesets column,
    # which has reverse reference direction
    return biobanks


def map_networks_to_resources(networks, biobanks):
    """Maps the BBMRI-ERIC Networks table to the flat data model's Resources table"""
    # Rename and create columns
    networks = networks.rename(columns={"url": "website"})
    networks["resources"] = ""
    # Add default type 'Network'
    networks["type"] = "Network"
    # Link network to its parent network
    networks.index = networks["id"]
    for idx, row in networks.iterrows():
        p_n = row["parent_network"]
        if p_n:
            if networks.loc[p_n, "resources"] != "":
                networks.loc[p_n, "resources"] += ","
            networks.loc[p_n, "resources"] += idx
    # Link network to its component biobanks

    return networks


def main():
    """Main function doing the conversion"""
    # Read password from command line
    parser = argparse.ArgumentParser(description="Command line arguments")
    parser.add_argument(
        "-source-url",
        type=str,
        required=True,
        dest="source_server",
        help="URL of server to get source data from",
    )
    parser.add_argument(
        "-source-pw",
        type=str,
        dest="source_password",
        required=False,
        help="Password for source server access",
    )
    parser.add_argument(
        "-target-url",
        type=str,
        required=True,
        dest="target_server",
        help="URL of server to upload transformed data to",
    )
    parser.add_argument(
        "-target-pw",
        type=str,
        dest="target_password",
        required=False,
        help="Password for target server access",
    )
    args = parser.parse_args()
    # Connect to source server
    with molgenis_emx2_pyclient.Client(args.source_server) as client:
        if args.source_password:
            client.signin("admin", args.source_password)
        client.set_schema("ERIC")
        # Connect to target server
        with molgenis_emx2_pyclient.Client(args.target_server) as catalogue_client:
            if args.target_password:
                catalogue_client.signin("admin", args.target_password)
            catalogue_client.set_schema("BBMRI-demo")
            # Initialise resources table
            mapped_columns = [
                "id",
                "name",
                "acronym",
                "description",
                "website",
                "resources",
                "type",
            ]
            resources = pd.DataFrame(columns=mapped_columns)
            # Map collections to resources
            collections = client.get("Collections", as_df=True)
            mapped_collections = map_collections_to_resources(
                collections.copy()
            )  # Unnecessary copy?
            resources = pd.concat([resources, mapped_collections.reindex(columns=mapped_columns)])
            # Map biobanks to resources
            biobanks = client.get("Biobanks", as_df=True)
            # Unnecessary copy?
            mapped_biobanks = map_biobanks_to_resources(biobanks.copy(), resources)
            resources = pd.concat([resources, mapped_biobanks.reindex(columns=mapped_columns)])
            # Map networks to resources
            networks = client.get("Networks", as_df=True)
            # Unnecessary copy?
            mapped_networks = map_networks_to_resources(networks.copy(), biobanks)
            resources = pd.concat([resources, mapped_networks.reindex(columns=mapped_columns)])
            # Create BBMRI-ERIC network, add all top-level networks as sub-networks
            bbmri_network = [
                {
                    "id": "BBMRI-ERIC",
                    "name": "BBMRI-ERIC",
                    "type": "Network",
                    "description": "BBMRI-ERIC directory mapped to flat model",
                }
            ]
            bbmri_network[0]["resources"] = ",".join(
                mapped_networks.loc[mapped_networks["parent_network"] == "", "id"]
            )
            resources = pd.concat([resources, pd.DataFrame.from_records(bbmri_network)])
            # Map persons to contacts
            persons = client.get("Persons", as_df=True)
            mapped_contacts = map_persons_to_contacts(
                persons.copy(), mapped_collections, mapped_biobanks, mapped_networks, resources
            )  # Unnecessary copy?
            mapped_contacts = mapped_contacts.reindex(
                columns=[
                    "resource",
                    "role",
                    "role description",
                    "display name",
                    "first name",
                    "last name",
                    "prefix",
                    "initials",
                    "title",
                    "organisation",
                    "email",
                    "orcid",
                    "homepage",
                    "photo",
                    "photo_filename",
                    "expertise",
                ]
            )
            # Upload mapped tables
            catalogue_client.save_schema(table="Contacts", data=mapped_contacts)
            catalogue_client.save_schema(table="Resources", data=resources)


if __name__ == "__main__":
    main()
