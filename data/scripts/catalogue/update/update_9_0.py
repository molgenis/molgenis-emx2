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
        # transformations per table
        if 'DataCatalogueFlat' in self.profile:
            self.reused_variables()
        if any(item in ['NetworksStaging','DataCatalogueFlat'] for item in self.profile):
            self.catalogues()
        if any(item in ['CohortsStaging', 'UMCUCohorts', 'UMCGCohortsStaging', 'INTEGRATE', 'RWEStaging'] for item in self.profile):
            self.collections()
        self.datasets()
        self.datasets()
        self.variables()
        self.variable_mappings()
        self.variable_values()

    def collections(self):
        """ Transform Collections
        """
        df_collections = pd.read_csv(self.path + 'Collections.csv', dtype='object')
        df_collections = df_collections.rename({'datasets': 'tables'})

        # write tables to file
        df_collections.to_csv(self.path + 'Collections.csv', index=False)

    def catalogues(self):
        """ Transform Catalogues
        """
        df_catalogues = pd.read_csv(self.path + 'Catalogues.csv', dtype='object')
        df_catalogues = df_catalogues.rename({'datasets': 'tables'})
        df_catalogues.to_csv(self.path + 'Catalogues.csv', index=False)

    def datasets(self):
        """ Transform Datasets and rename
        """
        df_datasets = pd.read_csv(self.path + 'Datasets.csv', dtype='object', keep_default_na=False)
        df_datasets = df_datasets.rename({'dataset type': 'table type'})
        df_datasets = df_datasets.drop(columns=['mapped to.source','mapped to.source dataset','mapped to.target',
                                                'mapped to.target dataset','mapped from.source',
                                                'mapped from.source dataset','mapped from.target',
                                                'mapped from.target dataset'])
        df_datasets.to_csv(self.path + 'Tables.csv', index=False)

    def dataset_mappings(self):
        """ Transform Dataset mappings and rename
        """
        df_dataset_mappings = pd.read_csv(self.path + 'Dataset mappings.csv', dtype='object', keep_default_na=False)
        df_dataset_mappings = df_dataset_mappings.rename({'source dataset': 'source table',
                                                          'target dataset': 'target table'})
        df_dataset_mappings.to_csv(self.path + 'Table mappings.csv', index=False)

    def variables(self):
        """ Transform Variables
        """
        df_variables = pd.read_csv(self.path + 'Variables.csv', dtype='object', keep_default_na=False)
        df_variables = df_variables.rename({'dataset': 'table'})
        df_variables = df_variables.drop(columns=['useExternalDefinition.resource','useExternalDefinition.dataset',
                                                  'useExternalDefinition.name','reused in resources.resource',
                                                  'reused in resources.variable.resource','reused in resources.variable.dataset',
                                                  'reused in resources.variable.name','mappings.source','mappings.source dataset',
                                                  'mappings.target','mappings.target dataset','mappings.target variable',
                                                  'mappings.repeats'])
        df_variables.to_csv(self.path + 'Variables.csv', index=False)

    def variable_mappings(self):
        """ Transform Variable mappings
        """
        df_variable_mappings = pd.read_csv(self.path + 'Variable mappings.csv', dtype='object', keep_default_na=False)
        df_variable_mappings = df_variable_mappings.rename({'source dataset': 'source table',
                                                            'source variables other tables.dataset': 'source variables other tables.table',
                                                            'target dataset': 'target table'})
        df_variable_mappings.to_csv(self.path + 'Variable mappings.csv', index=False)

    def variable_values(self):
        """ Transform Variable Values
        """
        df_variable_values = pd.read_csv(self.path + 'Variable values.csv', dtype='object', keep_default_na=False)
        df_variable_values = df_variable_values.rename({'dataset': 'table'})
        df_variable_values.to_csv(self.path + 'Variable values.csv', index=False)

    def reused_variables(self):
        """ Transform Reused Variables
        """
        df_reused_variables = pd.read_csv(self.path + 'Reused variables.csv', dtype='object', keep_default_na=False)
        df_reused_variables.rename({'variable.dataset': 'variable.table'}, inplace=True)

