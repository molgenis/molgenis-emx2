"""
Script to convert the BBMRI-ERIC directory data to the flat model
"""

import argparse
import molgenis_emx2_pyclient

def map_persons_to_contacts(persons):
    """Maps the BBRMI-ERIC Persons table to the flat data model's Collection Contacts"""
    persons.rename(columns={'first_name': 'first name',
                   'last_name': 'last name', 'role': 'role description'}, inplace=True)
    # Add dummy values for missing first and last names
    persons['first name'] = persons['first name'].replace(r'^$', 'FirstName', regex=True)
    persons['last name'] = persons['last name'].replace(r'^$', 'LastName', regex=True)
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
    persons['collection'] = 'INMA'
    # Ugly temporary fix to make duplicate keys unique by appending id after last name
    persons['last name'] = persons['last name'] + ' ' + persons['id']
    return persons


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
        # Get and transform Persons table
        persons = client.get('Persons', as_df=True)
        # Connect to target server
        with molgenis_emx2_pyclient.Client(args.target_server) as cat_client:
            if args.target_password:
                cat_client.signin('admin', args.target_password)
            cat_client.set_schema('catalogue-demo-hsl')
            mapped_contacts = map_persons_to_contacts(
                persons.copy())  # Unnecessary copy?
            mapped_contacts = mapped_contacts.reindex(columns=['collection', 'role', 'role description', 'display name', 'first name',
                                                               'last name', 'prefix', 'initials', 'title', 'organisation', 'email',
                                                               'orcid', 'homepage', 'photo', 'photo_filename', 'expertise'])
            # Upload mapped contacts
            cat_client.save_schema(table = 'Collection contacts', data = mapped_contacts)

if __name__ == "__main__":
    main()
