import shutil
import os
from pathlib import Path
import pandas as pd
from decouple import config

CATALOGUE_SCHEMA_NAME = config('MG_CATALOGUE_SCHEMA_NAME')

def get_data_model(profile_path, path_to_write, profile):
    # get changed profile tags
    new_profile_tags = get_new_profiles(profile)

    # get data model from profile and write to file
    data_model = pd.DataFrame()
    for file_name in os.listdir(profile_path):
        if '.csv' in file_name:
            file_path = Path.joinpath(profile_path, file_name)
            df = pd.read_csv(file_path, keep_default_na=False, dtype='object')
            df['new_profiles'] = df['profiles'].apply(lambda x: x.split(','))
            df = df[df['new_profiles'].apply(lambda x: any(item in new_profile_tags for item in x))]
            df = df.drop('new_profiles', axis=1, inplace=False)
            data_model = pd.concat([data_model, df])

    data_model.to_csv(path_to_write + '/molgenis.csv', index=None)

def get_new_profiles(profile):
    templates = {'DataCatalogueFlat': ['DataCatalogueFlat'],
                 'CohortsStaging': ['CohortsBasis', 'CohortsExtended', 'DataDictionaries', 'Mappings', 'Samplesets',
                                    'DataDictionariesColEvent', 'ResourceCounts'],
                 'NetworksStaging': ['NetworksStaging', 'DataDictionaries'],
                 'RWEStaging': ['RWEStaging', 'DataDictionaries', 'Mappings', 'ResourceCounts', 'Samplesets'],
                 'StudiesStaging': ['StudiesStaging', 'DataDictionaries', 'Mappings', 'DataDictionariesColEvent',
                                    'ResourceCounts', 'Samplesets'],
                 'UMCGCohortsStaging': ['CohortsBasis', 'CohortsExtended', 'DataDictionaries', 'Mappings',
                                        'UMCGCohortsStaging', 'Samplesets', 'DataDictionariesColEvent'],
                 'UMCUCohorts': ['CohortsBasis', 'CohortsExtended', 'DataDictionaries', 'Mappings', 'Samplesets',
                                 'DataDictionariesColEvent'],
                 'CohortsBasis': ['CohortsBasis']}

    new_profile_tags = []
    for p in profile:
        if not p == 'Patient registry':
            try:
                new_profile_tags += templates[p]
            except KeyError:
                pass

    return new_profile_tags

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
        if any(item in ['NetworksStaging','RWEStaging'] for item in self.profile):
            self.collections()
            self.subpopulations()

    def collections(self):
        """ Transform Collections
        """
        df_collections = pd.read_csv(self.path + 'Collections.csv', dtype='object', keep_default_na=False)
        df_collections = df_collections.drop(columns=['inclusion criteria', 'exclusion criteria'], axis=1, inplace=False)
        df_collections = df_collections.rename(columns = {'other inclusion criteria': 'inclusion criteria',
                                                          'other exclusion criteria': 'exclusion criteria'})
        df_collections.to_csv(self.path + 'Collections.csv', index=False)

    def subpopulations(self):
        """ Transform Subpopulations
        """
        df_subpopulations = pd.read_csv(self.path + 'Subpopulations.csv', dtype='object', keep_default_na=False)
        df_subpopulations = df_subpopulations.drop(columns=['inclusion criteria', 'exclusion criteria'], axis=1,
                                                   inplace=False)
        df_subpopulations = df_subpopulations.rename(columns={'other inclusion criteria': 'inclusion criteria',
                                                              'other exclusion criteria': 'exclusion criteria'})
        df_subpopulations.to_csv(self.path + 'Subpopulations.csv', index=False)
