import shutil
import os
from pathlib import Path
import pandas as pd
from decouple import config

CATALOGUE_SCHEMA_NAME = config('MG_CATALOGUE_SCHEMA_NAME')

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


class Transform:
    """General functions to update catalogue data model.
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
        # general transformations per table
        for table_name in ['Collections.csv', 'Collection events.csv', 'Subpopulations.csv']:
            if table_name in os.listdir(self.path):
                self.transform_tables(table_name)

        # rename attributes
        for table_name in ['Collection events.csv', 'Contacts.csv', 'Endpoint.csv', 'Catalogues.csv', 'Networks.csv',
                           'Collections.csv', 'Subpopulations.csv']:
            if table_name in os.listdir(self.path):
                self.rename_attributes(table_name)

        # specific transformations per table
        self.organisations()

    def transform_tables(self, table_name):
        """General transformations for some tables
        """
        df = pd.read_csv(self.path + table_name, dtype='object', keep_default_na=False)

        # make dict for look-up from Organisations.resource and Organisations.id
        df_organisations = pd.read_csv(self.path + 'Organisations.csv', dtype='object', keep_default_na=False)
        df_organisations['resource_id'] = df_organisations['resource'] + df_organisations['id']
        dict_organisations = dict(zip(df_organisations['resource_id'], df_organisations['organisation']))

        # get Organisations.organisation instead of Organisations.id for reference
        if table_name == 'Collections.csv':
            df['organisations involved.id'] = df.apply(
                lambda row: get_organisation_name_from_resource_id(row['organisations involved.resource'],
                                                                   row['organisations involved.id'],
                                                                   dict_organisations=dict_organisations), axis=1)
            df['publisher.id'] = df.apply(
                lambda row: get_organisation_name_from_resource_id(row['publisher.resource'], row['publisher.id'],
                                                                   dict_organisations=dict_organisations), axis=1)
            df['creator.id'] = df.apply(
                lambda row: get_organisation_name_from_resource_id(row['creator.resource'], row['creator.id'],
                                                                   dict_organisations=dict_organisations), axis=1)
        else:
            df['publisher'] = df.apply(
                lambda row: get_organisation_name_from_resource_id(row['resource'], row['publisher'],
                                                                   dict_organisations=dict_organisations), axis=1)
            df['creator'] = df.apply(
                lambda row: get_organisation_name_from_resource_id(row['resource'], row['creator'],
                                                                   dict_organisations=dict_organisations), axis=1)

        df.to_csv(self.path + table_name, index=False)


    def rename_attributes(self, table_name):
        """ Transform tables
        """
        if table_name in os.listdir(self.path):
                df = pd.read_csv(self.path + table_name, keep_default_na=False, dtype='object')
                df.rename(columns={'publisher.id': 'publisher',
                                   'creator.id': 'creator',
                                   'organisation.id': 'organisation',
                                   'organisations involved.id': 'organisations involved.organisation'}, inplace=True)
                df.to_csv(self.path + table_name, index=False)



    def organisations(self):
        """Transform Organisations data table and rename to Organisation roles
        Transform Organisations ontology table to Organisations data table
        """
        df = pd.read_csv(self.path + 'Organisations.csv', dtype='object', keep_default_na=False)
        df.to_csv(self.path + 'Organisation roles.csv', index=False)

        df_organisations = pd.read_csv(Path().cwd().joinpath('..', '..', '..', '_ontologies', 'Organisations.csv'),
                                       dtype='object', keep_default_na=False)
        df_organisations['id'] = df_organisations['name']
        df_organisations.to_csv(self.path + 'Organisations.csv', index=False)

def get_organisation_name_from_resource_id(resource, ids, dict_organisations):
    if pd.isna(ids) or ids == '':
        return ''

    resource_list = resource.split(',')
    id_list = ids.split(',')
    organisation_names = ''

    for i in range(0, len(resource_list)):
        organisation_id = resource_list[i] + id_list[i]
        organisation_name = dict_organisations[organisation_id]
        organisation_names += organisation_name + ','

    return organisation_names.strip(',')


