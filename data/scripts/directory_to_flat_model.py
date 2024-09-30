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
    collections['name'] = collections['name'] + ' from ' + collections['biobank_label'] + \
        ' (id: ' + collections['id'] + ')'
    # Add collection-to-subcollection links
    collections.index = collections['id']
    for idx, row in collections.iterrows():
        p_c = row['parent_collection']
        if p_c:
            if collections.loc[p_c, 'resources'] != '':
                collections.loc[p_c, 'resources'] += ','
            collections.loc[p_c, 'resources'] += idx
    # Map types from different ontologies, map to 'Other' if no suitable candidate exists in the target ontology
    # TODO: if there are still unmapped types ultimately, add the original type description in the type_other column
    type_mapping = {'QUALITY_CONTROL': 'Other', 'BIRTH_COHORT': 'Cohort study', 'POPULATION_BASED': 'Cohort study',
                    'DISEASE_SPECIFIC': 'Disease specific', 'NON_HUMAN': 'Other', 'CASE_CONTROL': 'Other', 'OTHER': 'Other',
                    'SAMPLE': 'Other', 'RD': 'Rare disease', 'IMAGE': 'Other', 'PROSPECTIVE_COLLECTION': 'Study',
                    'HOSPITAL': 'Registry', 'CROSS_SECTIONAL': 'Cohort study', 'COHORT': 'Cohort study', 'TWIN_STUDY': 'Other', 'LONGITUDINAL': 'Cohort study'}
    collections['type'] = collections['type'].map(lambda l: ','.join(set([type_mapping[t] for t in l.split(',')])))
    # Add default type 'Sample collection'
    collections['type'] += ',Sample collection'
    return collections

def map_biobanks_to_resources(biobanks, resources):
    """Maps the BBMRI-ERIC Biobanks table to the flat data model's Resources table"""
    # Rename and create columns
    biobanks = biobanks.rename(columns={'url': 'website'})
    biobanks['resources'] = ''
    # Create a unique name
    biobanks['name'] = biobanks['name'] + ' (id: ' + biobanks['id'] + ')'
    # Add default type 'Biobank
    biobanks['type'] = 'Biobank'
    # TODO: link biobank to its component sample collections
    return biobanks

def map_networks_to_resources(networks, resources):
    """Maps the BBMRI-ERIC Networks table to the flat data model's Resources table"""
    # Rename and create columns
    networks = networks.rename(columns={'url': 'website'})
    networks['resources'] = ''
    # Add default type 'Network'
    networks['type'] = 'Network'
    # TODO: link network to its parent network
    return networks


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
            catalogue_client.set_schema('BBMRI-demo')
            # Initialise resources table
            mapped_columns = ['id', 'name', 'acronym', 'description', 'website', 'resources', 'type']
            resources = pd.DataFrame(columns = mapped_columns)
            # Map collections to resources
            collections = client.get('Collections', as_df=True)
            mapped_collections = map_collections_to_resources(
                collections.copy())  # Unnecessary copy?
            resources = pd.concat([resources, mapped_collections.reindex(columns = mapped_columns)])
            # Map biobanks to resources
            biobanks = client.get('Biobanks', as_df=True)
            mapped_biobanks = map_biobanks_to_resources(biobanks.copy(), resources) # Unnecessary copy?
            resources = pd.concat([resources, mapped_biobanks.reindex(columns = mapped_columns)])
            # Map networks to resources
            networks = client.get('Networks', as_df=True)
            mapped_networks = map_networks_to_resources(networks.copy(), resources) # Unnecessary copy?
            resources = pd.concat([resources, mapped_networks.reindex(columns = mapped_columns)])
            # Create BBMRI-ERIC network, add all resources # TODO: or add only the BBMRI networks to this network?
            BBMRI_network = [{'id': 'BBMRI-ERIC', 'name': 'BBMRI-ERIC', 'type': 'Network', 'description': 'BBMRI-ERIC directory mapped to flat model'}]
            BBMRI_network[0]['resources'] = ','.join(resources['id'])
            resources = pd.concat([resources, pd.DataFrame.from_records(BBMRI_network)])
            # Map persons to contacts
            persons = client.get('Persons', as_df=True)
            mapped_contacts = map_persons_to_contacts(
                persons.copy())  # Unnecessary copy?
            mapped_contacts = mapped_contacts.reindex(columns=['resource', 'role', 'role description', 'display name', 'first name',
                                                               'last name', 'prefix', 'initials', 'title', 'organisation', 'email',
                                                               'orcid', 'homepage', 'photo', 'photo_filename', 'expertise'])
            # Upload mapped tables
            # catalogue_client.save_schema(
                # table='Contacts', data=mapped_contacts)
            catalogue_client.save_schema(table='Resources',
                                         data=resources)


if __name__ == "__main__":
    main()
