import shutil
import os
from pathlib import Path
import pandas as pd
from string import digits
import re
import numpy as np
from decouple import config
from molgenis_emx2_pyclient import Client
import numpy as np

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
        self.resources()
        self.endpoint()
        if self.profile == 'UMCGCohortsStaging':
            self.contacts()
        if self.schema_name == 'testCatalogue':
            self.collection_events()
            self.subpopulations()

    def agents(self):
        """ Transform data in Agents
        """
        df_agents = pd.read_csv(self.path + 'Agent.csv', dtype='object')
        df_agents.rename(columns={'name': 'id',
                                  'url': 'website',
                                  'mbox': 'email'}, inplace=True)
        df_agents['resource'] = 'MOLGENIS'

        # write table to file
        df_agents.to_csv(self.path + 'Agents.csv', index=False)

    def organisations(self):
        """ Transform data in Organisations
        """
        df_organisations = pd.read_csv(self.path + 'Organisations.csv', dtype='object')
        df_organisations['type'] = 'Organisation'

        if len(df_organisations) != 0:
            # load organisations ontology
            ontology_path = str(Path().cwd().joinpath('..', '..', '..', '_ontologies'))
            df_ror = pd.read_csv(ontology_path + '/Organisations.csv')
            df_ror = df_ror[['ontologyTermURI', 'name']]
            dict_ror = dict(zip(df_ror.ontologyTermURI, df_ror.name))
            # get ror name from pid
            df_organisations['organisation name'] = df_organisations['pid'].apply(get_ror_name, dict_ror=dict_ror)
            # get other organisation names not found in ror
            df_organisations['other organisation'] = df_organisations.apply(get_other_name, dict_ror=dict_ror, axis=1)

        # drop columns and write table to file
        df_organisations.drop(labels='name', axis=1, inplace=True)
        df_organisations.to_csv(self.path + 'Organisations.csv', index=False)

    def resources(self):
        """ Transform data in Resources
        """
        df_resources = pd.read_csv(self.path + 'Resources.csv', dtype='object')
        df_resources.rename(columns={'resources': 'data resources'}, inplace=True)

        # for demo data only: delete issued and modified
        if self.schema_name == 'testCatalogue':
            df_resources['issued'] = ''
            df_resources['modified'] = ''

        # get publisher and creator from Organisations table
        df_organisations = pd.read_csv(self.path + 'Organisations.csv', dtype='object')
        df_resources['publisher.resource'] = ''
        df_resources['publisher.id'] = ''
        df_resources['creator.resource'] = ''
        df_resources['creator.id'] = ''

        i = 0
        for r in df_resources['id']:
            # get creator(s)
            df_organisations_c = df_organisations[df_organisations['resource'] == r].reset_index()
            df_resources.loc[i, 'creator.resource'] = ','.join(df_organisations_c['resource'])
            df_resources.loc[i, 'creator.id'] = ','.join(df_organisations_c['id'])
            df_organisations_p = df_organisations_c[df_organisations_c['is lead organisation'] == 'true'].reset_index()
            # get publisher from lead organisation
            if not len(df_organisations_p) == 0:
                df_resources.loc[i, 'publisher.resource'] = df_organisations_p['resource'][0]
                df_resources.loc[i, 'publisher.id'] = df_organisations_p['id'][0]
            # for demo data only: otherwise get first organisation in list as publisher
            if self.schema_name == 'testCatalogue':
                if not len(df_organisations_c) == 0:
                    df_resources.loc[i, 'publisher.resource'] = df_organisations_c['resource'][0]
                    df_resources.loc[i, 'publisher.id'] = df_organisations_c['id'][0]
            i += 1

        # get contact point from Contacts table for demo data only
        if self.schema_name == 'testCatalogue':
            df_contacts = pd.read_csv(self.path + 'Contacts.csv', dtype='object')
            df_resources['contact point.resource'] = ''
            df_resources['contact point.first name'] = ''
            df_resources['contact point.last name'] = ''

            i = 0
            for r in df_resources['id']:
                df_contacts_r = df_contacts[df_contacts['resource'] == r].reset_index()
                df_contacts_p = df_contacts_r[df_contacts_r['role'] == 'Primary contact'].reset_index()
                # get contact point from primary contact
                if not len(df_contacts_p) == 0:
                    df_resources.loc[i, 'contact point.resource'] = df_contacts_p['resource'][0]
                    df_resources.loc[i, 'contact point.first name'] = df_contacts_p['first name'][0]
                    df_resources.loc[i, 'contact point.last name'] = df_contacts_p['last name'][0]
                # otherwise get first contact as contact point
                elif not len(df_contacts_r) == 0:
                    df_resources.loc[i, 'contact point.resource'] = df_contacts_r['resource'][0]
                    df_resources.loc[i, 'contact point.first name'] = df_contacts_r['first name'][0]
                    df_resources.loc[i, 'contact point.last name'] = df_contacts_r['last name'][0]
                i += 1

        # write table to file
        df_resources.to_csv(self.path + 'Resources.csv', index=False)

    def contacts(self):
        """ Transform data in Contacts
        """
        df_contacts = pd.read_csv(self.path + 'Contacts.csv', dtype='object')
        df_contacts['statement of consent personal data'] = df_contacts.apply(calculate_consent)

        # write table to file
        df_contacts.to_csv(self.path + 'Contacts.csv', index=False)

    def endpoint(self):
        """ Transform data in Endpoint
        """
        df_endpoint = pd.read_csv(self.path + 'Endpoint.csv', dtype='object')

        df_endpoint.rename(columns={'publisher': 'publisher.id'}, inplace=True)
        df_endpoint['publisher.resource'] = 'MOLGENIS'  # change dependent on server

        # write table to file
        df_endpoint.to_csv(self.path + 'Endpoint.csv', index=False)

    def collection_events(self):
        """ Transform data in Endpoint
        """
        df_col_event = pd.read_csv(self.path + 'Collection events.csv', dtype='object')

        df_col_event['issued'] = ''
        df_col_event['modified'] = ''

        # write table to file
        df_col_event.to_csv(self.path + 'Collection events.csv', index=False)

    def subpopulations(self):
        """ Transform data in Endpoint
        """
        df_subpopulations = pd.read_csv(self.path + 'Subpopulations.csv', dtype='object')

        df_subpopulations['issued'] = ''
        df_subpopulations['modified'] = ''

        # write table to file
        df_subpopulations.to_csv(self.path + 'Subpopulations.csv', index=False)



def get_ror_name(pid, dict_ror):
    if not pd.isna(pid):
        try:
            organisation_name = dict_ror[pid]
            return organisation_name
        except KeyError:
            return None


def get_other_name(row, dict_ror):
    if pd.isna(row['pid']):
        return row['name']
    else:
        try:
            valid_pid = dict_ror[row['pid']]
            return None
        except KeyError:
            return row['pid']


def calculate_consent(row):
    if row['statement of consent personal data'] is True and row['statement of consent email'] is True:
        return True
    elif row['statement of consent personal data'] is False:
        return False
    elif row['statement of consent email'] is False:
        return False
