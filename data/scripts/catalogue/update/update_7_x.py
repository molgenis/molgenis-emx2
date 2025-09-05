import shutil
import os
from pathlib import Path
import pandas as pd
from string import digits
import re
import numpy as np
from decouple import config
from molgenis_emx2_pyclient import Client

CATALOGUE_SCHEMA_NAME = config('MG_CATALOGUE_SCHEMA_NAME')


def float_to_int(df):
    """
    Cast float64 Series to Int64. Floats are not converted to integers by EMX2
    """
    for column in df.columns:
        if df[column].dtype == 'float64':
            df.loc[:, column] = df[column].astype('Int64')

    return df


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
        self.agents()
        self.organisations()
        if self.profile == 'UMCGCohortsStaging':
            self.contacts()

    def organisations(self):
        """ Transform data in Organisations
        """
        df_organisations = pd.read_csv(self.path + 'Organisations.csv', dtype='object')

        if len(df_organisations) != 0:
            # load organisations ontology
            ontology_path = str(Path().cwd().joinpath('..', '..', '..', '_ontologies'))
            df_ror = pd.read_csv(ontology_path + '/Organisations.csv')
            df_ror = df_ror[['ontologyTermURI', 'name']]
            dict_ror = dict(zip(df_ror.ontologyTermURI, df_ror.name))
            # get ror name from pid
            df_organisations['organisation'] = df_organisations['pid'].apply(get_ror_name, dict_ror=dict_ror)
            # get other organisation names not found in ror
            df_organisations['other organisation'] = df_organisations.apply(get_other_name, axis=1)

            # write table to file
            df_organisations.to_csv(self.path + 'Organisations.csv', index=False)

    def agents(self):
        """ Transform data in Agents
        """
        df_agents = pd.read_csv(self.path + 'Agent.csv', dtype='object')
        df_agents.rename(columns={'name': 'id',
                                  'url': 'website',
                                  'mbox': 'email'}, inplace=True)

        # write table to file
        df_agents.to_csv(self.path + 'Agents.csv', index=False)

    def contacts(self):
        """ Transform data in Contacts
        """
        df_contacts = pd.read_csv(self.path + 'Contacts.csv', dtype='object')
        df_contacts['statement of consent personal data'] = df_contacts.apply(calculate_consent)

        # write table to file
        df_contacts.to_csv(self.path + 'Contacts.csv', index=False)


def get_ror_name(pid, dict_ror):
    if not pd.isna(pid):
        organisation_name = dict_ror[pid]
        return organisation_name
    else:
        return None


def get_other_name(row):
    if pd.isna(row['pid']):
        return row['name']
    else:
        return None


def calculate_consent(row):
    if row['statement of consent personal data'] is True and row['statement of consent email'] is True:
        return True
    elif row['statement of consent personal data'] is False:
        return False
    elif row['statement of consent email'] is False:
        return False
