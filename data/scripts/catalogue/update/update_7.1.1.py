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
            df = df.loc[df['profiles'].apply(lambda p: profile in p.split(','))]
            data_model = pd.concat([data_model, df])

    # data_model = float_to_int(data_model)
    data_model.to_csv(path_to_write + '/molgenis.csv', index=None)


class Transform:
    """General functions to update catalogue data model.
    """

    def __init__(self, schema_name, profile):
        self.schema_name = schema_name
        self.profile = profile
        self.path = self.schema_name + '_data/'

    def delete_data_model_file(self):
        """Delete molgenis.csv
        """
        os.remove(self.path + 'molgenis.csv')

        def update_data_model_file(self):
        """Get data model from profile and copy molgenis.csv to appropriate folder
        """
        profile_path = Path().cwd().joinpath('..', '..', '..', '_models', 'shared')
        path_to_write = self.path
        if self.profile == 'DataCatalogueFlat':
            path_to_write = './catalogue_data_model'
            if not os.path.isdir(path_to_write):
                os.mkdir(path_to_write)
            get_data_model(profile_path, path_to_write, self.profile)
            shutil.make_archive('./catalogue_data_model_upload', 'zip', path_to_write)
        else:
            get_data_model(profile_path, path_to_write, self.profile)

    def transform_data(self):
        """Make changes per table
        """
        # transformations per table
        self.resources()

    def resources(self):
        """ Transform data in Resources
        """
        df_resources = pd.read_csv(self.path + 'Resources.csv', dtype='object')

        # update resource types
        df_resources['registry or health record type'] = df_resources['type'].apply(get_registry_types)
        df_resources['type'] = df_resources['type'].apply(update_types)

        # write table to file
        df_resources.to_csv(self.path + 'Resources.csv', index=False)


def get_registry_types(types):
    registry_types = ''
    types = types.split(',')

    for t in types:
        if t == 'Disease specific':
            registry_types += ',' + 'Disease registry'
        if t == 'Rare disease':
            registry_types += ',' + 'Rare disease registry'

    registry_types = registry_types.strip()

    return registry_types



def update_types(types):
    updated_types = ''
    types = types.split(',')

    for t in types:
        if t in ['Disease specific', 'Other type']:
            


    return updates_types
