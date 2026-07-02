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
        if any(item in ['DataCatalogueFlat','CohortsStaging', 'UMCUCohorts', 'UMCGCohortsStaging', 'INTEGRATE',
                        'RWEStaging'] for item in self.profile):
            self.collections()
        if any(item in ['DataCatalogueFlat', 'CohortsStaging', 'UMCUCohorts', 'UMCGCohortsStaging', 'RWEStaging',
                        'NetworksStaging'] for item in self.profile):
            self.datasets()
            self.datasets()
            self.variables()
            self.variable_values()
        if any(item in ['DataCatalogueFlat', 'CohortsStaging', 'UMCUCohorts', 'UMCGCohortsStaging',
                        'RWEStaging'] for item in self.profile):
            self.variable_mappings()

    def collections(self):
        """ Transform Collections
        """
        df_collections = pd.read_csv(self.path + 'Collections.csv', dtype='object', keep_default_na=False)
        df_collections = df_collections.rename(columns = {'datasets.resource': 'tables.resource',
                                                          'datasets.name': 'tables.name',
                                                          'mappings to common data models.source dataset': 'mappings to common data models.source table',
                                                          'mappings to common data models.target dataset': 'mappings to common data models.target table'})
        df_collections.to_csv(self.path + 'Collections.csv', index=False)

    def catalogues(self):
        """ Transform Catalogues
        """
        df_catalogues = pd.read_csv(self.path + 'Catalogues.csv', dtype='object', keep_default_na=False)
        df_catalogues = df_catalogues.rename(columns = {'datasets.resource': 'tables.resource',
                                                        'datasets.name': 'tables.name'})
        df_catalogues.to_csv(self.path + 'Catalogues.csv', index=False)

    def datasets(self):
        """ Transform Datasets and rename
        """
        df_datasets = pd.read_csv(self.path + 'Datasets.csv', dtype='object', keep_default_na=False)
        df_datasets = df_datasets.rename(columns = {'dataset type': 'table type',
                                                    'mapped to.source dataset': 'mapped to.source table',
                                                    'mapped to.target dataset': 'mapped to.target table',
                                                    'mapped from.source dataset': 'mapped from.source table',
                                                    'mapped from.target dataset': 'mapped from.target table'})
        df_datasets.to_csv(self.path + 'Tables.csv', index=False)

    def dataset_mappings(self):
        """ Transform Dataset mappings and rename
        """
        df_dataset_mappings = pd.read_csv(self.path + 'Dataset mappings.csv', dtype='object', keep_default_na=False)
        df_dataset_mappings = df_dataset_mappings.rename(columns = {'source dataset': 'source table',
                                                                    'target dataset': 'target table'})
        df_dataset_mappings.to_csv(self.path + 'Table mappings.csv', index=False)

    def variables(self):
        """ Transform Variables
        """
        df_variables = pd.read_csv(self.path + 'Variables.csv', dtype='object', keep_default_na=False)
        df_variables = df_variables.rename(columns = {'dataset': 'table',
                                                      'useExternalDefinition.dataset': 'useExternalDefinition.table',
                                                      'reused in resources.variable.dataset': 'reused in resources.variable.table',
                                                      'mappings.source dataset': 'mappings.source table',
                                                      'mappings.target dataset': 'mappings.target table',
                                                      'permitted values.dataset': 'permitted values.table'})
        df_variables.to_csv(self.path + 'Variables.csv', index=False)

    def variable_mappings(self):
        """ Transform Variable mappings
        """
        df_variable_mappings = pd.read_csv(self.path + 'Variable mappings.csv', dtype='object', keep_default_na=False)
        df_variable_mappings = df_variable_mappings.rename(columns = {'source dataset': 'source table',
                                                                      'source variables other datasets.dataset': 'source variables other tables.table',
                                                                      'source variables other datasets.name': 'source variables other tables.name',
                                                                      'target dataset': 'target table'})
        df_variable_mappings.to_csv(self.path + 'Variable mappings.csv', index=False)

    def variable_values(self):
        """ Transform Variable Values
        """
        df_variable_values = pd.read_csv(self.path + 'Variable values.csv', dtype='object', keep_default_na=False)
        df_variable_values = df_variable_values.rename(columns={'dataset': 'table'})
        df_variable_values.to_csv(self.path + 'Variable values.csv', index=False)

    def reused_variables(self):
        """ Transform Reused Variables
        """
        df_reused_variables = pd.read_csv(self.path + 'Reused variables.csv', dtype='object', keep_default_na=False)
        df_reused_variables = df_reused_variables.rename(columns={'variable.dataset': 'variable.table'})
        df_reused_variables.to_csv(self.path + 'Reused variables.csv', index=False)

