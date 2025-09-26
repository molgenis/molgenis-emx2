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
        if self.profile in ['CohortsStaging', 'DataCatalogueflat']:
            self.variable_mappings()

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

        # transformations
        df_organisations['type'] = 'Organisation'
        # get demo email for demo data only
        if self.schema_name == 'testCatalogue':
            df_organisations['email'] = 'test@email.com'

        if len(df_organisations) != 0:
            # load organisations ontology
            ontology_path = str(Path().cwd().joinpath('..', '..', '..', '_ontologies'))
            df_ror = pd.read_csv(ontology_path + '/Organisations.csv')
            df_ror = df_ror[['ontologyTermURI', 'name', 'website']]
            df_ror.rename(columns={'ontologyTermURI': 'pid',
                                   'name': 'organisation name',
                                   'website': 'organisation website'}, inplace=True)
            # get organisation details
            df_organisations = df_organisations.merge(df_ror, how='left', on='pid')
            df_organisations.rename(columns={'pid': 'organisation pid'}, inplace=True)
            # get organisation name in ref column
            df_organisations['organisation'] = df_organisations['organisation name']
            # get pids not in ror out of organisation pid column
            df_organisations['organisation pid'] = df_organisations.apply(clean_pid, axis=1)
            # get other organisation name or pid not found in ror
            df_organisations['other organisation'] = df_organisations.apply(get_other_name, axis=1)

        # drop columns and write table to file
        df_organisations.drop(labels='name', axis=1, inplace=True)
        df_organisations.to_csv(self.path + 'Organisations.csv', index=False)

    def resources(self):
        """ Transform data in Resources
        """
        df_resources = pd.read_csv(self.path + 'Resources.csv', dtype='object')
        df_resources.rename(columns={'resources': 'data resources'}, inplace=True)

        # for demo data only
        if self.schema_name == 'testCatalogue':
            df_resources['issued'] = ''
            df_resources['modified'] = ''
            df_resources['keywords'] = df_resources['keywords'].apply(add_keywords)
            df_resources['pid'] = 'https://placeholder-pid/' + df_resources['id'].str.lower() + '.org'
            df_resources['pid'] = df_resources['pid'].str.replace(' ', '')

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
        """ Transform data in Collection events
        """
        df_col_event = pd.read_csv(self.path + 'Collection events.csv', dtype='object')
        df_resources = pd.read_csv(self.path + 'Resources.csv', dtype='object')

        # transformations
        df_col_event['issued'] = ''
        df_col_event['modified'] = ''
        # concatenate name from resource and name columns
        df_col_event['name'] = df_col_event.apply(concat_resource_name, axis=1)
        # get keywords from Resources
        dict_keywords = dict(zip(df_resources.id, df_resources.keywords))
        df_col_event['keywords'] = df_col_event['resource'].apply(get_keywords, dict_keywords=dict_keywords)
        # get descriptions from Resources when missing
        dict_descriptions = dict(zip(df_resources.id, df_resources.description))
        df_col_event['description'] = df_col_event.apply(get_description, dict_descriptions=dict_descriptions, axis=1)
        # get publisher from Resources
        dict_publisher = dict(zip(df_resources.id, df_resources.publisher))
        df_col_event['publisher'] = df_col_event['resource'].apply(get_publisher, dict_publisher=dict_publisher)
        # get creator from Resources
        dict_creator = dict(zip(df_resources.id, df_resources.creator))
        df_col_event['creator'] = df_col_event['resource'].apply(get_creator, dict_creator=dict_creator)

        # only for demo data:
        if self.schema_name == 'testCatalogue':
            df_col_event['pid'] = 'https://placeholder-pid/' + df_col_event['resource'].str.lower() + '/' + \
                                       df_col_event['name'].str.lower() + '.org'
            df_col_event['pid'] = df_col_event['pid'].str.replace(' ', '').str.replace('(', '').str.replace(')', '').str.replace("'", "")

        # write table to file
        df_col_event.to_csv(self.path + 'Collection events.csv', index=False)

    def subpopulations(self):
        """ Transform data in Subpopulations
        """
        df_subpopulations = pd.read_csv(self.path + 'Subpopulations.csv', dtype='object')
        df_resources = pd.read_csv(self.path + 'Resources.csv', dtype='object')

        # transformations
        df_subpopulations['issued'] = ''
        df_subpopulations['modified'] = ''
        # concatenate name from resource and name columns
        df_subpopulations['name'] = df_subpopulations.apply(concat_resource_name, axis=1)
        # get keywords from Resources
        dict_keywords = dict(zip(df_resources.id, df_resources.keywords))
        df_subpopulations['keywords'] = df_subpopulations['resource'].apply(get_keywords, dict_keywords=dict_keywords)
        # get descriptions from Resources when missing
        dict_descriptions = dict(zip(df_resources.id, df_resources.description))
        df_subpopulations['description'] = df_subpopulations.apply(get_description, dict_descriptions=dict_descriptions, axis=1)
        # get publisher from Resources
        dict_publisher = dict(zip(df_resources.id, df_resources.publisher))
        df_subpopulations['publisher'] = df_subpopulations['resource'].apply(get_publisher, dict_publisher=dict_publisher)
        # get creator from Resources
        dict_creator = dict(zip(df_resources.id, df_resources.creator))
        df_subpopulations['creator'] = df_subpopulations['resource'].apply(get_creator, dict_creator=dict_creator)

        # only for demo data:
        if self.schema_name == 'testCatalogue':
            df_subpopulations['pid'] = 'https://placeholder-pid/' + df_subpopulations['resource'].str.lower() + '/' + \
                                       df_subpopulations['name'].str.lower() + '.org'
            df_subpopulations['pid'] = df_subpopulations['pid'].str.replace(' ', '').str.replace('(', '').str.replace(')', '')

        # write table to file
        df_subpopulations.to_csv(self.path + 'Subpopulations.csv', index=False)

    def variable_mappings(self):
        """ Transform data in Variable mappings
        """
        df_mappings = pd.read_csv(self.path + 'Variable mappings.csv', dtype='object')
        if not len(df_mappings) == 0:
            df_mappings['repeats'] = df_mappings['repeats'].apply(clean_repeats)

        # write table to file
        df_mappings.to_csv(self.path + 'Variable mappings.csv', index=False)


def clean_pid(row):
    if pd.isna('organisation'):
        return None
    else:
        return row['organisation pid']


def get_other_name(row):
    if pd.isna(row['organisation pid']):
        return row['name']
    elif pd.isna(row['organisation']):
        return row['organisation pid']
    else:
        return None


def calculate_consent(row):
    if row['statement of consent personal data'] is True and row['statement of consent email'] is True:
        return True
    elif row['statement of consent personal data'] is False:
        return False
    elif row['statement of consent email'] is False:
        return False


def add_keywords(keywords):
    if pd.isna(keywords):
        keywords = 'test1, test2'
    return keywords


def get_keywords(resource, dict_keywords):
    keywords = dict_keywords[resource]
    return keywords


def clean_repeats(repeats):
    if pd.isna(repeats):
        repeats = 'NA'
    elif repeats in ['na', '<NA>', 'preg']:
        repeats = 'NA'
    elif repeats in ['t1', 't2', 't3']:
        repeats = repeats[1:]
    return repeats


def concat_resource_name(row):
    if not row['resource'].lower() in row['name'].lower():
        return row['resource'] + ' ' + row['name']
    else:
        return row['name']


def get_description(row, dict_descriptions):
    if pd.isna(row['description']):
        return dict_descriptions[row['id']]
    else:
        return row['description']


def get_publisher(resource, dict_publisher):
    return dict_publisher[resource]


def get_creator(resource, dict_creator):
    return dict_creator[resource]
