"""
Script to convert the BBMRI-ERIC directory data to the flat model
"""

import argparse
import molgenis_emx2_pyclient
import pandas as pd

# Map SexTypes (partly MIABIS v2) to Sex types (MIABIS v3)
sex_mapping = {
    "*": "*",
    "FEMALE": "Female",
    "MALE": "Male",
    "NASK": "Unknown",
    "NAV": "Unknown",
    "NEUTERED_FEMALE": "Not applicable",
    "NEUTERED_MALE": "Not applicable",
    "UNDIFFERENTIAL": "Undifferentiated",
    "UNKNOWN": "Unknown",
}

# Map MIABIS-V2-based CollectionTypes to MIABIS-V3-based Sample collection designs
# TODO: perhaps some currently mapped to other fit in a different attribute?
design_mapping = {
    "QUALITY_CONTROL": "Quality control study",
    "BIRTH_COHORT": "Birth cohort",
    "POPULATION_BASED": "Population-based cohort",
    "DISEASE_SPECIFIC": "Disease-specific cohort",
    "NON_HUMAN": "Other",
    "CASE_CONTROL": "Case-control",
    "OTHER": "Other",
    "SAMPLE": "Other",
    "RD": "Rare disease collection",
    "IMAGE": "Other",
    "PROSPECTIVE_COLLECTION": "Other",
    "HOSPITAL": "Other",
    "CROSS_SECTIONAL": "Cross-sectional",
    "COHORT": "Other",
    "TWIN_STUDY": "Twin-study",
    "LONGITUDINAL": "Longitudinal cohort",
}

# Map DataCategories (partly MIABIS v2) to Dataset types (MIABIS v3)
data_category_mapping = {
    "ANTIBODIES": "Other",
    "BIOLOGICAL_SAMPLES": "Other",
    "BLOOD": "Physiological dataset",
    "CLINICAL_SYMPTOMS": "Clinical dataset",
    "CT": "Body (Radiological) image",
    "DiseaseDuration": "Clinical dataset",
    "GENEALOGICAL_RECORDS": "Genealogical records",
    "IMAGING_DATA": "Body (Radiological) image",
    "MEDICAL_RECORDS": "Clinical dataset",
    "NATIONAL_REGISTRIES": "Clinical dataset",
    "NAV": "",
    "OTHER": "Other",
    "PHYSIOLOGICAL_BIOCHEMICAL_MEASUREMENTS": 'Physiological dataset","Biochemical dataset', # Hack
    "SURVEY_DATA": "Other",
    "TREATMENT_PROTOCOL": "Clinical dataset",
}

# Map MaterialTypes (partly MIABIS v2) to BiospecimenType (MIABIS v3 + NCIT)
sample_type_mapping = {
    "*": '',
    "BUFFY_COAT": 'Buffy Coat',
    "CDNA": 'cDNA', # Added to BiospecimenType
    "CELL_LINES": 'Cell Line',
    "DNA": 'DNA',
    "FECES": 'Faeces',
    'MICRO_RNA': 'MicroRNA', # Added to BiospecimenType
    "NASAL_SWAB": 'Nasal Swab or Nose Specimen',
    "NAV": '',
    "OTHER": 'Other',
    "PATHOGEN": 'Isolated Pathogen',
    "PERIPHERAL_BLOOD_CELLS": 'Peripheral Blood',
    "PLASMA": 'Plasma',
    "RNA": 'RNA',
    "SALIVA": 'Saliva',
    "SERUM": 'Serum',
    "THROAT_SWAB": 'Oropharyngeal Swab Specimen',
    "TISSUE_FROZEN": 'Frozen Tissue',
    "TISSUE_PARAFFIN_EMBEDDED": 'Paraffin Embedded Tissue',
    "TISSUE_STAINED": 'Stained Specimen',
    "URINE": 'Urine',
    "WHOLE_BLOOD": 'Whole Blood',
}

# Map StorageTemperatureTypes (MIABIS v2) to Storage temperatures (MIABIS v3)
temperature_mapping = {
    "temperature-18to-35": "-18 °C to -35 °C",
    "temperature-60to-85": "-60 °C to -85 °C",
    "temperatureOther": "Other",
    "temperatureRoom": "RT (Room temperature)",
    "temperature2to10": "2 °C to 10°C",
    "temperatureLN": "Liquid nitrogen liquid-phase",
}

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

# Do partial mappings of title_before_name to pre-defined titles
# TODO: update when flat data model supports post-nominal titles
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

def add_to_array(array: str, item: str):
    """Add an item to an existing array in string format"""
    if pd.isna(array) or array == "":
        array = item
    else:
        array += ","
        array += item
    return array

def apply_mapping(column, mapping):
    """Apply the given mapping to the given column containing arrays in string format"""
    return column.map(
        lambda l: ",".join({f'"{mapping[x]}"' for x in l.split(",") if l})
    )

def map_disease_types_to_diseases(disease_types, diseases):
    """Maps the BBMRI-ERIC DiseaseTypes table to the flat data model's Diseases table"""
    # TODO: remove this function when flat model has moved to DiseaseTypes table as well
    mapping = {}
    disease_types = pd.merge(disease_types, diseases, how='left', left_on='label', right_on='name', suffixes=['_dt', '_d'])
    for _, row in disease_types.iterrows():
        source = row['name_dt']
        target = 'Tarsal kink syndrome' # Dummy value
        # Already mapped on identical label/name
        if pd.notna(row['name_d']):
            target = row['name_d']
        # Don't map null value
        elif source == '*':
            target = ''
        # Use ORPHAnet or ICD-10 code
        elif source.startswith('ORPHA:') or source.startswith('urn:miriam:icd:'):
            # Don't map obsolete ORPHAnet values
            if source.startswith('ORPHA:') and row['label_dt'].startswith('OBSOLETE:'):
                target = ''
            else:
                code_dt = source.split(':')[-1]
                result = diseases.loc[diseases['code'] == code_dt, 'name']
                if not result.empty:
                    target = result.item()
        mapping[source] = target
    # Report unmapped types
    count_unmapped = len([x for x in mapping.values() if x == 'Tarsal kink syndrome'])
    print(f"WARNING: {count_unmapped} of {len(mapping)} diseases were not mapped")
    return mapping

def map_age_to_age_groups(age_columns):
    """Maps the BBMRI-ERIC attributes age low, age high, age unit to the flat data model's age groups"""
    for idx, row in age_columns.iterrows():
        age_groups = ''
        if row['age_unit']:
            # Normalise all ages to years
            divider = 1
            match row['age_unit']:
                case 'DAY':
                    divider = 365
                case 'WEEK':
                    divider = 52
                case 'MONTH':
                    divider = 12
                case 'YEAR':
                    divider = 1
            if row['age_low']:
                age_low = float(row['age_low'])/divider
            if row['age_high']:
                age_high = float(row['age_high'])/divider
            # Add groups based on normalised age range
            if row['age_low']:
                # All values filled in
                if row['age_high']:
                    if age_low < 0:
                        age_groups = add_to_array(age_groups, 'Prenatal')
                    if age_low < 2/12 and age_high >= 0:
                        age_groups = add_to_array(age_groups, 'Newborn (0-1 months)')
                    if age_low < 2 and age_high >= 2/12:
                        age_groups = add_to_array(age_groups, 'Infants and toddlers (2-23 months)')
                    if age_low < 13 and age_high >= 2:
                        age_groups = add_to_array(age_groups, 'Child (2-12 years)')
                    if age_low < 18 and age_high >= 13:
                        age_groups = add_to_array(age_groups, 'Adolescent (13-17 years)')
                    if age_low < 25 and age_high >= 18:
                        age_groups = add_to_array(age_groups, 'Young adult (18-24 years)')
                    if age_low < 45 and age_high >= 25:
                        age_groups = add_to_array(age_groups, 'Adult (25-44 years)')
                    if age_low < 65 and age_high >= 45:
                        age_groups = add_to_array(age_groups, 'Middle-aged (45-64 years)')
                    if age_low < 80 and age_high >= 65:
                        age_groups = add_to_array(age_groups, 'Aged (65-79 years)')
                    if age_high >= 80:
                        age_groups = add_to_array(age_groups, 'Aged (80+ years)')
                # Only age low filled in, assume single data point of that age
                else:
                    if age_low < 0:
                        age_groups = add_to_array(age_groups, 'Prenatal')
                    elif age_low < 2/12:
                        age_groups = add_to_array(age_groups, 'Newborn (0-1 months)')
                    elif age_low < 2:
                        age_groups = add_to_array(age_groups, 'Infants and toddlers (2-23 months)')
                    elif age_low < 13:
                        age_groups = add_to_array(age_groups, 'Child (2-12 years)')
                    elif age_low < 18:
                        age_groups = add_to_array(age_groups, 'Adolescent (13-17 years)')
                    elif age_low < 25:
                        age_groups = add_to_array(age_groups, 'Young adult (18-24 years)')
                    elif age_low < 45:
                        age_groups = add_to_array(age_groups, 'Adult (25-44 years)')
                    elif age_low < 65:
                        age_groups = add_to_array(age_groups, 'Middle-aged (45-64 years)')
                    elif age_low < 80:
                        age_groups = add_to_array(age_groups, 'Aged (65-79 years)')
                    else:
                        age_groups = add_to_array(age_groups, 'Aged (80+ years)')
            # Only age high filled in, assume single data point of that age
            elif row['age_high']:
                if age_high >= 80:
                    age_groups = add_to_array(age_groups, 'Aged (80+ years)')
                elif age_high >= 65:
                    age_groups = add_to_array(age_groups, 'Aged (65-79 years)')
                elif age_high >= 45:
                    age_groups = add_to_array(age_groups, 'Middle-aged (45-64 years)')
                elif age_high >= 25:
                    age_groups = add_to_array(age_groups, 'Adult (25-44 years)')
                elif age_high >= 18:
                    age_groups = add_to_array(age_groups, 'Young adult (18-24 years)')
                elif age_high >= 13:
                    age_groups = add_to_array(age_groups, 'Adolescent (13-17 years)')
                elif age_high >= 2:
                    age_groups = add_to_array(age_groups, 'Child (2-12 years)')
                elif age_high >= 2/12:
                    age_groups = add_to_array(age_groups, 'Infants and toddlers (2-23 months)')
                elif age_high >= 0:
                    age_groups = add_to_array(age_groups, 'Newborn (0-1 months)')
                else:
                    age_groups = add_to_array(age_groups, 'Prenatal')
            else:
                row['age_groups'] = ''
        else:
            row['age_groups'] = ''
        age_columns.loc[idx, 'age groups'] = age_groups
    return age_columns['age groups']

def map_persons_to_contacts(persons, collections, biobanks, networks, resources):
    """Maps the BBRMI-ERIC Persons table to the flat data model's Contacts table"""
    persons.rename(
        columns={"first_name": "first name", "last_name": "last name", "role": "role description"},
        inplace=True,
    )
    # Add dummy values for missing first and last names
    persons["first name"] = persons["first name"].replace(r"^$", "FirstName", regex=True)
    persons["last name"] = persons["last name"].replace(r"^$", "LastName", regex=True)
    # Map titles and roles
    persons["title_before_name"] = (
        persons["title_before_name"].str.lower().str.replace("[^a-z]", "", regex=True)
    )
    persons["title"] = persons["title_before_name"].map(title_mapping)
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

def map_collections_to_samples(collections, disease_mapping):
    """Maps the BBMRI-ERIC Collections table to the flat data model's Sample collections table"""
    # TODO: columns to map: head, contact, also_known, categories, quality, combined_quality, facts
    # Rename and create columns
    collections.rename(columns={"type": "design",
                                "biobank": "resource",
                                "data_categories": "dataset type",
                                "size": "number of samples",
                                "number_of_donors": "number of donors",
                                "diagnosis_available": "main medical condition",
                                "materials": "sample type",
                                "storage_temperatures": "storage temperature",
                                "body_part_examined": "body part examined",
                                "imaging_modality": "imaging modality",
                                "image_dataset_type": "image types",
                                }, inplace=True)
    collections["parent sample collection.name"] = ""
    collections["parent sample collection.resource"] = ""
    collections["age groups"] = ""
    # Remove withdrawn collections
    collections = collections.loc[~collections['withdrawn']]
    # Create a unique name
    collections["name"] = (
        collections["name"]
        + " from "
        + collections["biobank_label"]
        + " (id: "
        + collections["id"]
        + ")"
    )
    collections["name"] = collections["name"].str.slice(stop=255)
    # Update reference to parent collection to use resource + name instead of id
    collections.index = collections["id"]
    for idx, row in collections.iterrows():
        p_c_id = row["parent_collection"]
        if p_c_id:
            p_c = collections.loc[p_c_id]
            collections.loc[idx, 'parent sample collection.name'] = p_c['name']
            collections.loc[idx, 'parent sample collection.resource'] = p_c['resource']
    # Apply mappings to attributes which need it
    collections["design"] = apply_mapping(collections["design"], design_mapping)
    collections["dataset type"] = apply_mapping(collections["dataset type"], data_category_mapping)
    collections["sex"] = apply_mapping(collections["sex"], sex_mapping)
    collections['main medical condition'] = apply_mapping(collections['main medical condition'], disease_mapping)
    collections['age groups'] = map_age_to_age_groups(collections[['age_low', 'age_high', 'age_unit', 'age groups']])
    collections['sample type'] = apply_mapping(collections['sample type'], sample_type_mapping)
    collections['storage temperature'] = apply_mapping(collections['storage temperature'], temperature_mapping)
    return collections


def map_biobanks_to_resources(biobanks):
    """Maps the BBMRI-ERIC Biobanks table to the flat data model's Resources table"""
    # Rename and create columns
    biobanks = biobanks.rename(columns={"url": "website"})
    biobanks["resources"] = ""
    # Create a unique name
    biobanks["name"] = biobanks["name"] + " (id: " + biobanks["id"] + ")"
    # Add default type 'Biobank
    biobanks["type"] = "Biobank"
    return biobanks


def map_networks_to_resources(networks, biobanks):
    """Maps the BBMRI-ERIC Networks table to the flat data model's Resources table"""
    # Rename and create columns
    networks = networks.rename(columns={"url": "website"})
    networks["resources"] = ""
    # Add default type 'Network'
    networks["type"] = "Network"
    # Create a unique name
    networks["name"] = networks["name"] + " (id: " + networks["id"] + ")"
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
            catalogue_client.set_schema("catalogue-BBMRI")
            # Initialise Resources table
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
            # Map Biobanks to Resources
            print('Get and map Biobanks...')
            biobanks = client.get("Biobanks", as_df=True)
            # Unnecessary copy?
            mapped_biobanks = map_biobanks_to_resources(biobanks.copy())
            resources = pd.concat([resources, mapped_biobanks.reindex(columns=mapped_columns)])
            # Map DiseaseTypes to Diseases
            print('Get and map DiseaseTypes...')
            disease_types = client.get("DiseaseTypes", schema="DirectoryOntologies", as_df=True)
            diseases = catalogue_client.get("Diseases", schema="CatalogueOntologies", as_df=True)
            disease_mapping = map_disease_types_to_diseases(disease_types, diseases)
            # Map Collections to Sample resources
            print('Get and map Collections...')
            collections = client.get("Collections", as_df=True)
            mapped_collections = map_collections_to_samples(
                collections.copy(), disease_mapping)  # Unnecessary copy?
            mapped_collections = mapped_collections.reindex(
                columns = [
                    "resource",
                    "name",
                    "acronym",
                    "description",
                    "url",
                    "parent sample collection.name",
                    "parent sample collection.resource",
                    "design",
                    "dataset type",
                    "number of donors",
                    "number of samples",
                    "sex",
                    "main medical condition",
                    "age groups",
                    "sample type",
                    "storage temperature",
                    "body part examined",
                    "imaging modality",
                    "image types",
                ]
            )
            # Map Networks to Resources
            print('Get and map Networks...')
            networks = client.get("Networks", as_df=True)
            # Unnecessary copy?
            mapped_networks = map_networks_to_resources(networks.copy(), biobanks)
            resources = pd.concat([resources, mapped_networks.reindex(columns=mapped_columns)])
            # Create BBMRI-ERIC network, add all top-level networks as sub-networks
            print('Networking...')
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
            # Map Persons to Contacts
            print('Get and map Persons...')
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
            print('Upload...')
            catalogue_client.save_schema(table="Resources", data=resources)
            catalogue_client.save_schema(table="Sample collections", data=mapped_collections)
            catalogue_client.save_schema(table="Contacts", data=mapped_contacts)


if __name__ == "__main__":
    main()
    print('Done!')
