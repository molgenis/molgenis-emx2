"""
Script to convert the BBMRI-ERIC directory data to the flat model
"""

import argparse
import molgenis_emx2_pyclient
import pandas as pd
import numpy as np


def map_persons_to_contacts(persons):
    """Maps the BBRMI-ERIC Persons table to the flat data model's Contacts table"""
    persons.rename(columns={'first_name': 'first name',
                   'last_name': 'last name', 'role': 'role description'}, inplace=True)
    # Add dummy values for missing first and last names
    persons['first name'] = persons['first name'].replace(
        r'^$', 'FirstName', regex=True)
    persons['last name'] = persons['last name'].replace(
        r'^$', 'LastName', regex=True)
    # Do partial mappings of title_before_name to pre-defined titles
    persons['title_before_name'] = persons['title_before_name'].str.lower(
    ).str.replace('[^a-z]', '', regex=True)
    title_mapping = {'dr': 'dr.', 'profdr': 'prof. dr.', 'prof': 'prof.', 'mwdr': 'dr.',
                     'profdrmed': 'prof. dr.', 'pddr': 'dr.', 'drrernat': 'dr.', 'drmed': 'dr.'}
    persons['title'] = persons['title_before_name'].map(title_mapping)
    # Partial mappings of hand-entered roles to pre-defined roles
    role_mapping = {'PI': 'Principal Investigator', 'Principal Investigator': 'Principal Investigator',
                    'Principal Investigators': 'Principal Investigator', 'Principle Investigator': 'Principal Investigator',
                    'Project coordinator': 'Project manager', 'Project Co-ordinator': 'Project manager'}
    persons['role'] = persons['role description'].map(role_mapping)
    # Add everyone to dummy organisation for now
    persons['resource'] = 'INMA'
    # Ugly temporary fix to make duplicate keys unique by appending id after last name
    persons['last name'] = persons['last name'] + ' ' + persons['id']

    return persons


def map_collections_to_resources(collections):
    """Maps the BBMRI-ERIC Collections table to the flat data model's Resources table"""
    # Rename and create columns
    collections.rename(columns={'url': 'website'}, inplace=True)
    collections['resources'] = ''
    # Create a unique name
    collections['name'] = collections['name'] + ' ' + collections['biobank'] + \
        ' (' + collections['id'] + ')'
    # Add collection-to-subcollection links
    collections.index = collections['id']
    for idx, row in collections.iterrows():
        p_c = row['parent_collection']
        if p_c:
            if collections.loc[p_c, 'resources'] != '':
                collections.loc[p_c, 'resources'] += ','
            collections.loc[p_c, 'resources'] += idx
    # Map types from different ontologies, map to 'Other' if no suitable candidate exists in the target ontology
    # TODO: add type 'Biobank' for every collection?
    # TODO: if there are still unmapped types ultimately, add the original type description in the type_other column
    type_mapping = {'QUALITY_CONTROL': 'Other', 'BIRTH_COHORT': 'Cohort study', 'POPULATION_BASED': 'Cohort study',
                    'DISEASE_SPECIFIC': 'Disease specific', 'NON_HUMAN': 'Other', 'CASE_CONTROL': 'Other', 'OTHER': 'Other',
                    'SAMPLE': 'Other', 'RD': 'Rare disease', 'IMAGE': 'Other', 'PROSPECTIVE_COLLECTION': 'Study',
                    'HOSPITAL': 'Registry', 'CROSS_SECTIONAL': 'Cohort study', 'COHORT': 'Cohort study', 'TWIN_STUDY': 'Other', 'LONGITUDINAL': 'Cohort study'}
    collections['type'] = collections['type'].map(lambda l: ','.join(set([type_mapping[t] for t in l.split(',')])))
    return collections


def main():
    """Main function doing the conversion"""
    # Read password from command line
    parser = argparse.ArgumentParser(description="Command line arguments")
    parser.add_argument("-source-url", type=str, required=True,
                        dest="source_server", help="URL of server to get source data from")
    parser.add_argument("-source-pw", type=str, dest="source_password",
                        required=False, help="Password for source server access")
    parser.add_argument("-target-url", type=str, required=True,
                        dest="target_server", help="URL of server to upload transformed data to")
    parser.add_argument("-target-pw", type=str, dest="target_password",
                        required=False, help="Password for target server access")
    args = parser.parse_args()
    # Connect to source server
    with molgenis_emx2_pyclient.Client(args.source_server) as client:
        if args.source_password:
            client.signin('admin', args.source_password)
        client.set_schema('ERIC')
        # Connect to target server
        with molgenis_emx2_pyclient.Client(args.target_server) as catalogue_client:
            if args.target_password:
                catalogue_client.signin('admin', args.target_password)
            catalogue_client.set_schema('catalogue-demo-hsl')
            # Map persons to contacts
            persons = client.get('Persons', as_df=True)
            mapped_contacts = map_persons_to_contacts(
                persons.copy())  # Unnecessary copy?
            mapped_contacts = mapped_contacts.reindex(columns=['resource', 'role', 'role description', 'display name', 'first name',
                                                               'last name', 'prefix', 'initials', 'title', 'organisation', 'email',
                                                               'orcid', 'homepage', 'photo', 'photo_filename', 'expertise'])
            # Map collections to resources
            collections = client.get('Collections', as_df=True)
            mapped_collections = map_collections_to_resources(
                collections.copy())  # Unnecessary copy?
            mapped_collections = mapped_collections.reindex(
                columns=['id', 'name', 'acronym', 'description', 'website', 'resources', 'type'])
            # Mappings involving multiple tables
            # TODO: add role for 'contact's in resources (also for 'head's?)
            # TODO: Link contacts and resources
            # Upload mapped tables
            catalogue_client.save_schema(
                table='Contacts', data=mapped_contacts)
            catalogue_client.save_schema(table='Resources',
                                         data=mapped_collections)


if __name__ == "__main__":
    main()
