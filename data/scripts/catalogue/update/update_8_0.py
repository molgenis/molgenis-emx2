import shutil
import os
from pathlib import Path
import pandas as pd

RESOURCE_TABLES = {'Catalogue': 'Catalogues', 'Network': 'Networks', 'Collection': 'Collections'}

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

    def __init__(self, schema_name, data_dir=None, model_dir=None):
        self.schema_name = schema_name
        self.path = os.path.join(data_dir, '') if data_dir else self.schema_name + '_data/'
        self.model_dir = model_dir
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
        if self.model_dir:
            profile_path = Path(self.model_dir)
        else:
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
        df_resources = pd.read_csv(self.path + 'Resources.csv', dtype='object', keep_default_na=False)
        data_model = pd.read_csv(self.path + 'molgenis.csv', dtype='object', keep_default_na=False)

        table_types = df_resources['type'].apply(assign_table_type)
        for table_type, table_name in RESOURCE_TABLES.items():
            df_table = df_resources[table_types == table_type]
            if table_name not in set(data_model['tableName']):
                if len(df_table) != 0:
                    raise ValueError(f"{self.schema_name}: no table {table_name} in the new model for "
                                     f"{', '.join(df_table['id'])}")
                continue
            columns = columns_of_table(data_model, table_name)
            keep = [c for c in df_resources.columns
                    if c == 'mg_draft' or c.split('.')[0] in columns or c.removesuffix('_filename') in columns]
            df_table[keep].to_csv(self.path + table_name + '.csv', index=False)

        os.remove(self.path + 'Resources.csv')


def columns_of_table(data_model, table_name):
    """Columns of a table and of the tables it extends
    """
    columns = set()
    while table_name:
        table_rows = data_model[data_model['tableName'] == table_name]
        columns |= set(table_rows['columnName'])
        parents = [parent for parent in table_rows['tableExtends'] if parent]
        table_name = parents[0] if parents else None
    return columns


def assign_table_type(resource_type):
    if not resource_type:
        table_type = 'Collection'
    elif 'Catalogue' in resource_type:
        table_type = 'Catalogue'
    elif 'Network' in resource_type:
        table_type = 'Network'
    else:
        table_type = 'Collection'

    return table_type
