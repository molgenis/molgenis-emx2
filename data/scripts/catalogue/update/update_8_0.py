import shutil
import os
from pathlib import Path
import pandas as pd
from decouple import config

CATALOGUE_SCHEMA_NAME = config('MG_CATALOGUE_SCHEMA_NAME')

def get_data_model(profile_path, path_to_write, profile):
    # get data model from profile and write to file
    data_model = pd.DataFrame()
    pattern = '|'.join(profile)
    for file_name in os.listdir(profile_path):
        if '.csv' in file_name:
            file_path = Path.joinpath(profile_path, file_name)
            df = pd.read_csv(file_path, keep_default_na=False, dtype='object')
            df = df[df['profiles'].str.contains(pattern, na=False)]
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
        # transformations per table
        self.resources()

    def resources(self):
        """ Transform data in Resources: split into separate tables based on type
        """
        df_resources = pd.read_csv(self.path + 'Resources.csv', dtype='object')

        # split Resources table
        df_resources['table_type'] = df_resources['type'].apply(assign_table_type)
        df_catalogues = df_resources[df_resources['table_type'] == 'Catalogue']
        df_networks = df_resources[df_resources['table_type'] == 'Network']
        df_collections = df_resources[df_resources['table_type'] == 'Collection']

        # write tables to file
        df_catalogues.to_csv(self.path + 'Catalogues.csv', index=False)
        df_networks.to_csv(self.path + 'Networks.csv', index=False)
        df_collections.to_csv(self.path + 'Collections.csv', index=False)

        # remove resources table
        os.remove(self.path + 'Resources.csv')


def assign_table_type(resource_type):
    if pd.isna(resource_type):
        table_type = 'Collection'
    elif 'Catalogue' in resource_type:
        table_type = 'Catalogue'
    elif 'Network' in resource_type:
        table_type = 'Network'
    else:
        table_type = 'Collection'

    return table_type