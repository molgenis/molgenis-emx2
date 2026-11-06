import csv
import os
from pathlib import Path
import pandas as pd

RESOURCE_FILES = ['Resources.csv', 'Networks.csv', 'Catalogues.csv', 'Collections.csv']
CONTACT_COLUMNS = ['resource', 'role', 'first name', 'last name', 'email']
CONTACT_POINT_COLUMNS = ['contact point.resource', 'contact point.first name', 'contact point.last name']


def get_data_model(profile_path, path_to_write, profile):
    # get data model from profile and write to file
    data_model = pd.DataFrame()
    for file_name in os.listdir(profile_path):
        if '.csv' in file_name:
            file_path = Path.joinpath(profile_path, file_name)
            df = pd.read_csv(file_path, keep_default_na=False, dtype='object')
            df['new_profiles'] = df['profiles'].apply(lambda x: x.split(','))
            df = df[df['new_profiles'].apply(lambda x: any(item in profile for item in x))]
            df = df.drop('new_profiles', axis=1, inplace=False)
            data_model = pd.concat([data_model, df])

    data_model.to_csv(path_to_write + '/molgenis.csv', index=None)


def name_from_email(email):
    """'tiina.eriksson@tuni.fi' gives ('Tiina', 'Eriksson').
    A mailbox that is not a 'first.last' name, such as 'info@nkcx.se', gives ('Contact', 'info').
    """
    local_part = email.split('@')[0]
    parts = local_part.split('.')
    if len(parts) == 2 and parts[0] and parts[1]:
        return parts[0].capitalize(), parts[1].capitalize()
    return 'Contact', local_part


def move_contact_emails(resources, contacts):
    """Move 'contact email' of each resource into the Contacts table and remove the column.

    The first email of a resource becomes its contact point, unless the resource already has one.
    Other emails are kept as extra contacts. No email is lost.
    Returns a list of messages for the data manager to review.
    """
    messages = []
    for resource in resources:
        emails = resource.pop('contact email', '').replace(';', ',').split(',')
        emails = [email.strip() for email in emails if email.strip()]

        for position, email in enumerate(emails):
            contact = find_contact_by_email(contacts, resource['id'], email)
            if contact:
                messages.append(f"{resource['id']}: {email} belongs to existing contact "
                                f"{contact['first name']} {contact['last name']}")
            else:
                first_name, last_name = name_from_email(email)
                contact = find_contact_by_name(contacts, resource['id'], first_name, last_name)
                if contact and not contact.get('email'):
                    contact['email'] = email
                    messages.append(f"{resource['id']}: added {email} to existing contact {first_name} {last_name}")
                else:
                    if contact:
                        first_name, last_name = 'Contact', email
                    contact = {'resource': resource['id'], 'role': 'Primary contact',
                               'first name': first_name, 'last name': last_name, 'email': email}
                    contacts.append(contact)
                    messages.append(f"{resource['id']}: NEW contact {first_name} {last_name} <{email}>, please check the name")

            is_first_email = position == 0
            if is_first_email and not resource.get('contact point.first name'):
                resource['contact point.resource'] = contact['resource']
                resource['contact point.first name'] = contact['first name']
                resource['contact point.last name'] = contact['last name']
            elif is_first_email:
                messages.append(f"{resource['id']}: kept existing contact point "
                                f"{resource['contact point.first name']} {resource['contact point.last name']}, "
                                f"{email} is now an extra contact")

    return messages


def find_contact_by_email(contacts, resource_id, email):
    for contact in contacts:
        if contact['resource'] == resource_id and contact.get('email', '').lower() == email.lower():
            return contact
    return None


def find_contact_by_name(contacts, resource_id, first_name, last_name):
    for contact in contacts:
        if (contact['resource'] == resource_id
                and contact['first name'].lower() == first_name.lower()
                and contact['last name'].lower() == last_name.lower()):
            return contact
    return None


def read_csv(file_path):
    with open(file_path, encoding='utf-8', newline='') as file:
        reader = csv.DictReader(file)
        return list(reader.fieldnames), list(reader)


def write_csv(file_path, columns, rows):
    with open(file_path, 'w', encoding='utf-8', newline='') as file:
        writer = csv.DictWriter(file, fieldnames=columns, restval='', lineterminator='\n')
        writer.writeheader()
        writer.writerows(rows)


def add_missing(columns, new_columns):
    return columns + [column for column in new_columns if column not in columns]


class Transform:
    """Update catalogue data from data model 9.x to 10.0.
    """

    def __init__(self, schema_name):
        self.schema_name = schema_name
        self.path = self.schema_name + '_data/'
        self.profile = self.get_profile()

    def get_profile(self):
        df_profile = pd.read_csv(self.path + 'Profiles.csv', dtype='object')
        profile = df_profile.columns.to_list()

        return profile

    def delete_data_model_file(self):
        """Delete molgenis.csv
        """
        os.remove(self.path + 'molgenis.csv')

    def update_data_model_file(self):
        """Get data model from profile and copy molgenis.csv to appropriate folder
        """
        profile_path = Path().cwd().joinpath('..', '..', '..', '_models', 'shared')
        path_to_write = self.path
        get_data_model(profile_path, path_to_write, self.profile)

    def transform_data(self):
        """Make changes per table
        """
        self.contact_emails()

    def contact_emails(self):
        """Move 'contact email' from the resource tables to Contacts
        """
        contacts_file = self.path + 'Contacts.csv'
        if os.path.exists(contacts_file):
            contact_columns, contacts = read_csv(contacts_file)
        else:
            contact_columns, contacts = [], []
        contact_columns = add_missing(contact_columns, CONTACT_COLUMNS)

        changed = False
        for file_name in RESOURCE_FILES:
            file_path = self.path + file_name
            if not os.path.exists(file_path):
                continue
            columns, resources = read_csv(file_path)
            if 'contact email' not in columns:
                continue

            for message in move_contact_emails(resources, contacts):
                print(message)
            columns = add_missing([column for column in columns if column != 'contact email'], CONTACT_POINT_COLUMNS)
            write_csv(file_path, columns, resources)
            changed = True

        if changed:
            write_csv(contacts_file, contact_columns, contacts)
