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

    def __init__(self, schema_name):
        self.schema_name = schema_name
        self.path = self.schema_name + '_data/'


    def delete_data_model_file(self):
        """Delete molgenis.csv
        """
        os.remove(self.path + 'molgenis.csv')

    def transform_data(self):
        """Make changes per table
        """
        # transformations per table
        self.resources()
        self.organisations()

    # TODO: for vac4eu BPE model is an exception, not part of a network, also other model in VAC4EU


    def resources(self):
        """Transform columns in Data sources, Databanks
        """
        # Studies to Resources

        df_studies = pd.read_csv(self.path + 'Studies.csv', dtype='object')
        df_studies.rename(columns={'study design classification': 'clinical study type',
                                   'number of subjects': 'number of participants',
                                   'age groups': 'population age groups'}, inplace=True)
        df_studies['type'] = 'Clinical trial'
        # get data resources that were used in the study
        cols_to_find = ['data sources', 'databanks']
        i_cols = [df_studies.columns.get_loc(col) for col in cols_to_find]
        df_studies['data resources'] = df_studies[df_studies.columns[i_cols]].apply(lambda x: ','.join(x.dropna().astype(str)), axis=1)

        # Data sources to Resources
        df_data_sources = pd.read_csv(self.path + 'Data sources.csv', dtype='object')
        df_data_sources.rename(columns={'type': 'registry or health record type',
                                        'areas of information': 'areas of information rwd',
                                        'informed consent': 'informed consent required'}, inplace=True)
        # infer resource type from RWD type
        df_data_sources['type'] = df_data_sources['registry or health record type'].apply(get_resource_type)
        # transform dates to years
        df_data_sources.loc[:, 'start year'] = df_data_sources['date established'].apply(get_year_from_date)
        df_data_sources.loc[:, 'end year'] = df_data_sources['end data collection'].apply(get_year_from_date)
        # transform keywords
        df_data_sources.loc[:, 'keywords'] = df_data_sources['keywords'].apply(reformat_keywords)
        # transform regions
        df_data_sources.loc[:, 'regions'] = df_data_sources['regions'].apply(remove_all_regions)
        # transform datasource types
        df_data_sources.loc[:, 'RWD type'] = df_data_sources['RWD type'].apply(transform_datasource_types)

        # Databanks to Resources
        df_databanks = pd.read_csv(self.path + 'Databanks.csv', dtype='object')
        df_databanks.rename(columns={'type': 'registry or health record type',
                                     'areas of information': 'areas of information rwd',
                                     'informed consent': 'informed consent required'}, inplace=True)

        # infer resource type from RWD type
        df_databanks['type'] = df_databanks['registry or health record type'].apply(get_resource_type)
        # transform dates to years
        df_databanks.loc[:, 'start year'] = df_databanks['date established'].apply(get_year_from_date)
        df_databanks.loc[:, 'end year'] = df_databanks['end data collection'].apply(get_year_from_date)
        # transform keywords
        df_databanks.loc[:, 'keywords'] = df_databanks['keywords'].apply(reformat_keywords)
        # transform regions
        df_databanks.loc[:, 'regions'] = df_databanks['regions'].apply(remove_all_regions)
        # transform datasource types
        df_databanks.loc[:, 'RWD type'] = df_databanks['RWD type'].apply(transform_datasource_types)

        # Networks to Resources
        df_networks = pd.read_csv(self.path + 'Networks.csv', dtype='object')
        df_networks = df_networks.rename(columns={'type': 'network type'})
        df_networks['type'] = 'Network'

        # get resources that are part of network
        cols_to_find = ['data sources', 'databanks']
        i_cols = [df_networks.columns.get_loc(col) for col in cols_to_find]
        df_networks['data resources'] = df_networks[df_networks.columns[i_cols]].apply(lambda x: ','.join(x.dropna().astype(str)), axis=1)

        # Models to Resources
        df_models = pd.read_csv(self.path + 'Models.csv', dtype='object')
        df_models['type'] = 'Common data model'

        # merge all to Resources
        df_resources = pd.concat([df_networks, df_studies, df_databanks, df_data_sources, df_models])

        # save to file
        df_resources.to_csv(self.path + 'Resources.csv', index=False)

    def organisations(self):
        """ Transform columns in Organisations and alter structure
        """
        # TODO: move DAPs to Organisations.role = data access provider (remove all other columns)
        # TODO: move 'Data sources.data holder' to Organisations.role = 'data holder'

        df_all_organisations = pd.DataFrame()
        df_organisations = pd.read_csv(self.path + 'Organisations.csv', dtype='object')

        # get lead organisations
        df_resources = pd.read_csv(self.path + 'Resources.csv', dtype='object')
        df_resources = df_resources[['id', 'lead organisation']]
        df_resources.rename(columns={'id': 'resource',
                                     'lead organisation': 'id'}, inplace=True)
        df_resources = df_resources.dropna(axis=0)
        df_resources = df_resources.reset_index()
        if len(df_resources) != 0:
            df_resources.loc[:, 'is lead organisation'] = 'True'
            df_merged = get_organisations(df_organisations, df_resources)
            df_all_organisations = pd.concat([df_all_organisations, df_merged])

        # get additional organisations and Contacts.organisation
        for table in ['Resources', 'Contacts']:
            df_resource = pd.read_csv(self.path + table + '.csv', dtype='object')
            if table == 'Resources':
                df_resource = df_resource[['id', 'additional organisations']]
            elif table == 'Contacts':
                df_resource = df_resource[['resource', 'organisation']]

            df_resource.rename(columns={'organisation': 'id',
                                        'id': 'resource',
                                        'additional organisations': 'id'}, inplace=True)
            df_resource = df_resource.dropna(axis=0)
            if len(df_resource) != 0:
                df_resource = df_resource.reset_index()
                df_resource.loc[:, 'is lead organisation'] = 'False'
                df_merged = get_organisations(df_organisations, df_resource)
                df_all_organisations = pd.concat([df_all_organisations, df_merged])
        df_all_organisations = df_all_organisations.drop_duplicates(subset=['resource', 'id'], keep='first')  # keep first to get lead organisations
        df_all_organisations.to_csv(self.path + 'Organisations.csv', index=False)

    def publications(self):
        """Transform Publications table
        """
        df_resources = pd.read_csv(self.path + 'Resources.csv', dtype='object')
        df_publications = pd.read_csv(self.path + 'Publications.csv', dtype='object')

        if not len(df_publications) == 0:
            df_design_paper = df_resources[['id', 'design paper']]
            df_design_paper.loc[:, 'is design publication'] = 'true'
            df_design_paper = df_design_paper.rename(columns={'id': 'resource',
                                                              'design paper': 'doi'})
            df_design_paper = df_design_paper.dropna(axis=0)
            df_design_paper = df_design_paper.reset_index()

            df_other_pubs = df_resources[['id', 'publications']]
            if not len(df_other_pubs) == 0:
                df_other_pubs.loc[:, 'is design publication'] = 'false'
                df_other_pubs = df_other_pubs.rename(columns={'id': 'resource',
                                                              'publications': 'doi'})
                df_other_pubs = df_other_pubs.dropna(axis=0)
                df_other_pubs = df_other_pubs.reset_index()

            if len(df_other_pubs) == 0:
                df_resource_pubs = get_publications(df_design_paper, df_publications)
            else:
                df_merged_pubs = pd.concat([df_design_paper, df_other_pubs])
                df_merged_pubs = df_merged_pubs.reset_index()
                df_resource_pubs = get_publications(df_merged_pubs, df_publications)



    def variable_values(self):
        # restructure variable values
        df_var_values = pd.read_csv(self.path + 'Variable values.csv', keep_default_na=False, dtype='object')
        if not len(df_var_values) == 0:
            df_var_values = df_var_values.rename(columns={'variable.dataset': 'dataset',
                                                          'variable.name': 'variable'})

            df_var_values.to_csv(self.path + 'Variable values.csv', index=False)



def get_resource_type(r_type):
    resource_type = []
    if not pd.isna(r_type):
        if any(t in r_type for t in ['Clinical trial', 'Study']):
            resource_type.append('Clinical trial')
        if any(t in r_type for t in ['Registry', 'registry']):
            resource_type.append('Registry')
        if 'Biobank' in r_type:
            resource_type.append('Biobank')
        if 'records' in r_type:
            resource_type.append('Health records')

    resource_type = ','.join(resource_type)

    return resource_type


def get_year_from_date(date):
    if not pd.isna(date):
        year = date[0:4]

        return year
    else:
        return None


def reformat_keywords(keywords):
    if not pd.isna(keywords):
        keywords = keywords.replace(';', ',')

    return keywords


def remove_all_regions(regions):
    if not pd.isna(regions):
        regions = regions.replace('All', '')
        regions = regions.strip(',')

    return regions


def transform_datasource_types(datasource_types):
    if not pd.isna(datasource_types):
        datasource_types = datasource_types.replace('Pharmacy dispensation records', 'Pharmacy dispensing records')

    return datasource_types


def get_organisations(df_organisations, df_resource):
    """Merge data resource and organisation to Organisations, split out multiple
    references from ref_array into separate rows
    """
    # get all organisations with resource reference in one row
    df_new_rows = pd.DataFrame(columns=['resource', 'id', 'is lead organisation'])
    for i in range(len(df_resource)):
        if ',' in df_resource['id'][i]:
            org_list = df_resource['id'][i].split(',')
            for org in org_list:
                new_row = {'resource': df_resource['resource'][i], 'id': org,
                           'is lead organisation': df_resource['is lead organisation'][i]}
                df_new_rows.loc[len(df_new_rows)] = new_row
            df_resource = df_resource.drop(index=i)
    df_resource = pd.concat([df_resource, df_new_rows])

    # merge with df_organisations
    df_merged = pd.merge(df_organisations, df_resource, on='id')

    return df_merged


def get_organisation_role(role):
    if pd.isna(role):
        return 'Lead'
    else:
        return role + ', Lead'


def get_publications(df_merged_pubs, df_publications):
    """Merge information from publications from Resources and Publications tables, split out
    multiple references from ref_array into separate rows
    """
    # get all doi's with resource reference in one row
    df_new_rows = pd.DataFrame(columns=['resource', 'doi', 'is design publication'])
    for i in range(len(df_merged_pubs)):
        if ',' in df_merged_pubs['doi'][i]:
            doi_list = df_merged_pubs['doi'][i].split(',')
            for doi in doi_list:
                new_row = {'resource': df_merged_pubs['resource'][i], 'doi': doi,
                           'is design publication': df_merged_pubs['is design publication'][i]}
                df_new_rows.loc[len(df_new_rows)] = new_row
            df_merged_pubs = df_merged_pubs.drop(index=i)
    df_merged_pubs = pd.concat([df_merged_pubs, df_new_rows])

    # merge information from Resources and Publications tables on doi
    df_resource_pubs = pd.merge(df_merged_pubs, df_publications, on='doi')

    return df_resource_pubs
