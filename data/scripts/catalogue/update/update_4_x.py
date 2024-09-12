import shutil
import os
from pathlib import Path
import pandas as pd
from string import digits
import re
import numpy as np
from decouple import config

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

    def __init__(self, database_name, database_type):
        self.database_name = database_name
        self.database_type = database_type
        self.path = self.database_name + '_data/'

    def delete_data_model_file(self):
        """Delete molgenis.csv
        """
        os.remove(self.path + 'molgenis.csv')

    def update_data_model_file(self):
        """Get data model from profile and copy molgenis.csv to appropriate folder
        """
        profile_path = Path().cwd().joinpath('..', '..', '..', '_models', 'shared')
        path_to_write = self.path
        if self.database_type == 'catalogue':
            profile = 'DataCatalogueFlat'
            path_to_write = './catalogue_data_model'
            if not os.path.isdir(path_to_write):
                os.mkdir(path_to_write)
            get_data_model(profile_path, path_to_write, profile)
            shutil.make_archive('./catalogue_data_model_upload', 'zip', path_to_write)
        else:
            if self.database_type == 'network':
                profile = 'NetworksStaging'
            elif self.database_type == 'cohort':
                profile = 'CohortsStaging'
            elif self.database_type == 'data_source':
                profile = 'RWEStaging'
            elif self.database_type == 'cohort_UMCG':
                profile = 'UMCGCohortsStaging'

            get_data_model(profile_path, path_to_write, profile)

    def transform_data(self):
        """Make changes per table
        """
        # transformations per table
        if self.database_type == 'catalogue':
            self.catalogues()
            self.network_variables()

        self.resources()
        self.organisations()
        self.variables()
        self.variable_values()
        self.publications()

        if self.database_type != 'network':
            self.variable_mappings()
        if self.database_type not in ['data_source', 'network']:
            self.collection_events()

        # TODO: for vac4eu BPE model is an exception, not part of a network, also other model in VAC4EU
        # TODO: move DAPs to Organisations.role = data access provider (remove all other columns)
        for table_name in ['Datasets', 'Dataset mappings', 'External identifiers', 'Subcohorts', 'Subcohort counts',
                           'Collection events', 'Quantitative information', 'Documentation', 'Contacts',
                           'Variables', 'Variable values', 'Linked resources']:
            self.transform_tables(table_name)

        for table_name in ['Subcohorts', 'Quantitative information', 'Subcohort counts', 'Linked resources']:
            self.rename_tables(table_name)

    def catalogues(self):
        """Transform columns in Catalogues
        """
        # Cohorts to Resources
        df_catalogues = pd.read_csv(self.path + 'Catalogues.csv', dtype='object')
        df_catalogues['name'] = df_catalogues['network']
        df_catalogues['description'] = 'to be filled out'  # TODO: data model get Resources.description

        # df_catalogues = float_to_int(df_catalogues)  # convert float back to integer
        df_catalogues.to_csv(self.path + 'Catalogues.csv', index=False)

    def resources(self):
        """Transform columns in Cohorts, Networks, Studies, Data sources, Databanks
        """
        # Cohorts to Resources
        if self.database_type in ['catalogue', 'cohort', 'cohort_UMCG']:

            df_cohorts = pd.read_csv(self.path + 'Cohorts.csv', dtype='object')
            df_cohorts = df_cohorts.rename(columns={'type': 'cohort type',
                                                    'type other': 'cohort type other',
                                                    'collection type': 'data collection type'})
            df_cohorts['type'] = 'Cohort study'

            # get resources that are part of network
            if self.database_type in ['cohort']:
                cols_to_find = ['studies']
                i_cols = [df_cohorts.columns.get_loc(col) for col in cols_to_find]
                df_cohorts['resources'] = df_cohorts[df_cohorts.columns[i_cols]]\
                    .apply(lambda x: ','.join(x.dropna().astype(str)), axis=1)

        # Networks to Resources
        if self.database_type in ['catalogue', 'network']:
            df_networks = pd.read_csv(self.path + 'Networks.csv', dtype='object')
            df_networks = df_networks.rename(columns={'type': 'network type'})
            df_networks['type'] = 'Network'

            # get resources that are part of network
            if self.database_type == 'catalogue':
                cols_to_find = ['networks', 'cohorts', 'data sources', 'databanks']
            elif self.database_type == 'network':
                cols_to_find = ['cohorts', 'data sources', 'databanks']
            i_cols = [df_networks.columns.get_loc(col) for col in cols_to_find]
            df_networks['resources'] = df_networks[df_networks.columns[i_cols]]\
                .apply(lambda x: ','.join(x.dropna().astype(str)), axis=1)

        # Studies to Resources
        if self.database_type in ['catalogue']:
            df_studies = pd.read_csv(self.path + 'Studies.csv', dtype='object')
            df_studies.rename(columns={'study design classification': 'clinical study type',
                                       'type other': 'study type other',
                                       'number of subjects': 'number of participants',
                                       'age groups': 'population age groups'}, inplace=True)
            df_studies['type'] = 'Study'

        # Data sources to Resources
        if self.database_type in ['catalogue', 'data_source']:
            df_data_sources = pd.read_csv(self.path + 'Data sources.csv', dtype='object')
            df_data_sources.rename(columns={'type': 'RWD type',
                                            'type other': 'RWD type other',
                                            'areas of information': 'areas of information rwd'}, inplace=True)
            df_data_sources['type'] = 'Data source'

            # Databanks to Resources
            df_databanks = pd.read_csv(self.path + 'Databanks.csv', dtype='object')
            df_databanks.rename(columns={'type': 'RWD type',
                                         'type other': 'RWD type other',
                                         'areas of information': 'areas of information rwd'}, inplace=True)
            df_databanks['type'] = 'Databank'

        # Models to Resources
        if self.database_type in ['catalogue', 'network']:
            df_models = pd.read_csv(self.path + 'Models.csv', dtype='object')
            df_models = df_models[df_models['id'] == 'CRC Screening CDM']  # handles exception CRC Screening CDM
            df_models['type'] = ''  # TODO: add term here

        # merge all to Resources
        if self.database_type == 'catalogue':
            df_resources = pd.concat([df_cohorts, df_networks, df_studies, df_databanks, df_data_sources, df_models])
        elif self.database_type in ['cohort', 'cohort_UMCG']:
            df_resources = df_cohorts
        elif self.database_type == 'data_source':
            df_resources = df_data_sources
        elif self.database_type == 'network':
            df_resources = pd.concat([df_networks, df_models])
        # df_resources = float_to_int(df_resources)  # convert float back to integer
        df_resources.to_csv(self.path + 'Resources.csv', index=False)

    def organisations(self):
        """ Transform columns in Organisations and alter structure
        """
        df_all_organisations = pd.DataFrame()
        if self.database_type == 'catalogue':
            df_organisations = pd.read_csv(self.path + 'Organisations.csv', dtype='object')

            # get lead organisations
            df_resources = pd.read_csv(self.path + 'Resources.csv', dtype='object')
            df_resources = df_resources[['id', 'lead organisation']]
            df_resources.rename(columns={'id': 'resource',
                                         'lead organisation': 'id'}, inplace=True)
            df_resources = df_resources.dropna(axis=0)
            df_resources.loc[:, 'is lead organisation'] = 'True'
            df_resources = df_resources.reset_index()
            df_merged = get_organisations(df_organisations, df_resources)
            df_all_organisations = pd.concat([df_all_organisations, df_merged])

            # get additional organisations and Contacts.organisation
            for table in ['Resources', 'Contacts']:
                df_resource = pd.read_csv(self.path + table + '.csv', dtype='object')
                if not table == 'Contacts':
                    df_resource = df_resource[['id', 'additional organisations']]
                else:
                    df_resource = df_resource[['resource', 'organisation']]
                df_resource.rename(columns={'organisation': 'id',
                                            'id': 'resource',
                                            'additional organisations': 'id'}, inplace=True)
                df_resource = df_resource.dropna(axis=0)
                df_resource.loc[:, 'is lead organisation'] = 'False'
                df_resource = df_resource.reset_index()
                df_merged = get_organisations(df_organisations, df_resource)
                df_all_organisations = pd.concat([df_all_organisations, df_merged])
            # df_all_organisations = float_to_int(df_all_organisations)  # convert float back to integer
            df_all_organisations = df_all_organisations.drop_duplicates(subset=['resource', 'id'], keep='first')  # keep first to get lead organisations
            df_all_organisations.to_csv(self.path + 'Organisations.csv', index=False)

        # get organisations for staging areas by making subsets on 'resource' for Organisations
        else:
            if self.database_name not in ['testCohort', 'testDatasource']:
                df_organisations = pd.read_csv(CATALOGUE_SCHEMA_NAME + '_data/' + 'Organisations.csv', dtype='object')
                df_resource = pd.read_csv(self.path + 'Resources.csv')
                df_resource = df_resource[['id']]
                df_resource.rename(columns={'id': 'resource'}, inplace=True)
                resources_list = df_resource['resource'].to_list()
                df_organisations.loc[:, 'select_resource'] = \
                    df_organisations.apply(lambda o: True if o['resource'] in resources_list else False, axis=1)
                df_organisations = df_organisations[df_organisations['select_resource']]
            elif self.database_name == 'testCohort':
                data = ['testCohort', 'UMCG', 'University Medical Center Groningen', 'Netherlands (the)',
                        'https://www.umcg.nl/']
                df_organisations = pd.DataFrame(data, columns=['resource', 'id', 'name', 'country', 'website'])
            elif self.database_name == 'testDatasource':
                data = ['testDatasource', 'AU', 'University of Aarhus', 'Denmark']
                df_organisations = pd.DataFrame(data, columns=['resource', 'id', 'name', 'country'])

            df_organisations.to_csv(self.path + 'Organisations.csv', index=False)

    def publications(self):
        """Transform Publications table
        """
        df_resources = pd.read_csv(self.path + 'Resources.csv', dtype='object')
        df_publications = pd.read_csv(self.path + 'Publications.csv', dtype='object')

        if self.database_type != 'network':
            df_design_paper = df_resources[['id', 'design paper']]
            df_design_paper.loc[:, 'is design publication'] = 'true'
            df_design_paper = df_design_paper.rename(columns={'id': 'resource',
                                                              'design paper': 'doi'})
            df_design_paper = df_design_paper.dropna(axis=0)

        if self.database_type != 'cohort_UMCG':
            df_other_pubs = df_resources[['id', 'publications']]
            df_other_pubs.loc[:, 'is design publication'] = 'false'
            df_other_pubs = df_other_pubs.rename(columns={'id': 'resource',
                                                          'publications': 'doi'})
            df_other_pubs = df_other_pubs.dropna(axis=0)

        if self.database_type == 'network':
            df_resource_pubs = get_publications(df_other_pubs, df_publications)
        elif self.database_type == 'cohort_UMCG':
            df_resource_pubs = get_publications(df_design_paper, df_publications)
        else:
            df_merged_pubs = pd.concat([df_design_paper, df_other_pubs])
            df_merged_pubs = df_merged_pubs.reset_index()
            df_resource_pubs = get_publications(df_merged_pubs, df_publications)
        df_resource_pubs = df_resource_pubs.drop_duplicates(subset=['resource', 'doi'], keep='first')
        df_resource_pubs.to_csv(self.path + 'Publications.csv', index=False)

    def network_variables(self):
        df = pd.read_csv(self.path + 'Network variables.csv', keep_default_na=False, dtype='object')
        df.loc[:, 'resource'] = df['network'].apply(strip_resource)
        df.loc[:, 'variable.resource'] = df['variable.resource'].apply(strip_resource)
        df.loc[:, 'variable.name'] = df['variable.name'].apply(remove_number)
        df = df.drop_duplicates(subset=['resource', 'variable.resource', 'variable.name'])

        # df = float_to_int(df)  # convert float back to integer
        df.to_csv(self.path + 'Resource variables.csv', index=False)

    def variable_values(self):
        # restructure variable values
        df_var_values = pd.read_csv(self.path + 'Variable values.csv', keep_default_na=False, dtype='object')
        df_var_values.loc[:, 'variable.resource'] = df_var_values['resource'].apply(strip_resource)

        df_var_values_cdm = df_var_values[df_var_values['variable.resource'].isin(['LifeCycle', 'ATHLETE',
                                                                                   'testNetwork1', 'EXPANSE'])]
        df_var_values_cdm.loc[:, 'variable.name'] = df_var_values_cdm['variable.name'].apply(remove_number)

        df_var_values_no_cdm = df_var_values[~df_var_values['variable.resource'].isin(['LifeCycle', 'ATHLETE',
                                                                                       'testNetwork1', 'EXPANSE'])]

        df_all_var_values = pd.concat([df_var_values_no_cdm, df_var_values_cdm])
        df_all_var_values = df_all_var_values.drop_duplicates(subset=['variable.resource', 'variable.dataset',
                                                                      'variable.name', 'value'])
        # df_all_var_values = float_to_int(df_all_var_values)  # convert float back to integer
        df_all_var_values.to_csv(self.path + 'Variable values.csv', index=False)

    def variables(self):
        # restructure Variables
        df_variables = pd.read_csv(self.path + 'Variables.csv', keep_default_na=False, dtype='object')
        df_variables.loc[:, 'resource'] = df_variables['resource'].apply(strip_resource)

        # restructure repeated variables inside variables dataframe
        df_repeats = pd.read_csv(self.path + 'Repeated variables.csv', dtype='object')
        df_repeats.loc[:, 'resource'] = df_repeats['resource'].apply(strip_resource)
        df_variables.loc[:, 'is_repeated'] = df_variables['name'].apply(is_repeated, df_repeats=df_repeats)

        # select athlete, lifecycle, expanse and testNetwork1 variables and restructure
        if self.database_type in ['catalogue', 'network'] and \
                self.database_name in ['catalogue', 'LifeCycle', 'ATHLETE', 'EXPANSE']:
            df_variables_cdm = df_variables[df_variables['resource'].isin(['LifeCycle', 'ATHLETE',
                                                                           'testNetwork1', 'EXPANSE'])]
            df_variables_cdm.loc[:, 'name'] = df_variables_cdm['name'].apply(remove_number)
            df_variables_cdm = restructure_repeats(df_variables_cdm, df_repeats)

        # select variables that are not in LifeCycle or ATHLETE or testNetwork1
        df_variables_no_cdm = df_variables[~df_variables['resource'].isin(['LifeCycle', 'ATHLETE',
                                                                           'testNetwork1', 'EXPANSE'])]
        # select repeated variables that are not in lifecycle or ATHLETE or testNetwork1
        df_repeats_no_cdm = df_repeats[~df_repeats['resource'].isin(['LifeCycle', 'ATHLETE',
                                                                     'testNetwork1', 'EXPANSE'])]

        # concatenate all variables
        if self.database_type in ['catalogue', 'network'] and \
                self.database_name in ['catalogue', 'LifeCycle', 'ATHLETE', 'EXPANSE']:
            df_all_variables = pd.concat([df_variables_cdm, df_variables_no_cdm, df_repeats_no_cdm])
        else:
            df_all_variables = pd.concat([df_variables_no_cdm, df_repeats_no_cdm])

        df_all_variables = float_to_int(df_all_variables)  # convert float back to integer
        df_all_variables.to_csv(self.path + 'Variables.csv', index=False)

    def variable_mappings(self):
        df = pd.read_csv(self.path + 'Variable mappings.csv', keep_default_na=False, dtype='object')
        if len(df) != 0:
            df.loc[:, 'target'] = df['target'].apply(strip_resource)  # delete appendix '_CDM'
            df.loc[:, 'repeat_num'] = df['target variable'].apply(get_repeat_number)  # get repeat of target variable

            df_cdm = df[df['target'].isin(['LifeCycle', 'ATHLETE', 'testNetwork1', 'EXPANSE'])]
            if len(df_cdm) != 0:
                df_no_cdm = df[~df['target'].isin(['LifeCycle', 'ATHLETE', 'testNetwork1', 'EXPANSE'])]
                df_cdm.loc[:, 'target variable'] = df_cdm['target variable'].apply(remove_number)
                df_cdm = df_cdm.fillna('')

                # drop duplicate mappings
                df_no_duplicates = df_cdm.drop_duplicates(subset=['source', 'source dataset', 'source variables',
                                                                  'source variables other datasets.dataset',
                                                                  'source variables other datasets.name',
                                                                  'target', 'target dataset', 'target variable',
                                                                  'match', 'syntax', 'comments', 'description'])
                df_no_duplicates = df_no_duplicates.fillna('')

                # get repeated mappings in comma separated string
                df_mappings = rewrite_mappings(df_cdm, df_no_duplicates)
                df_mappings = pd.concat([df_mappings, df_no_cdm])
                # df_mappings = float_to_int(df_mappings)  # convert float back to integer
                df_mappings.to_csv(self.path + 'Variable mappings.csv', index=False)

    def collection_events(self):
        """ Transform Collection events table
        """
        df = pd.read_csv(self.path + 'Collection events.csv', dtype='object')
        df.loc[:, 'start month'] = df['start month'].apply(month_to_num, start_or_end='start')
        df['start date'] = df['start year'].astype('Int64').astype('string') + '-' + df['start month'] + '-01'
        df.loc[:, 'end month'] = df['end month'].apply(month_to_num, start_or_end='end')
        df['end day'] = df['end month'].apply(get_end_day)
        df['end date'] = df['end year'].astype('Int64').astype('string') + '-' + df['end month'] + '-' + df['end day']

        # df = float_to_int(df)  # convert float back to integer
        df.to_csv(self.path + 'Collection events.csv', index=False)

    def transform_tables(self, table_name):
        if table_name + '.csv' in os.listdir(self.path):
            df = pd.read_csv(self.path + table_name + '.csv', keep_default_na=False, dtype='object')
            if 'resource' in df.columns:
                df.loc[:, 'resource'] = df['resource'].apply(strip_resource)  # removes _CDM from 'model' name
            if 'target' in df.columns:
                df.loc[:, 'target'] = df['target'].apply(strip_resource)
            if 'subcohorts' in df.columns:
                df.loc[:, 'subcohorts'] = df['subcohorts'].apply(strip_resource)

            df.rename(columns={'subcohort.resource': 'resource',
                               'subcohort.name': 'subpopulation',
                               'collection event.name': 'collection event',
                               'network': 'resource',
                               'main resource': 'resource'}, inplace=True)

            # df = float_to_int(df)  # convert float back to integer
            df.to_csv(self.path + table_name + '.csv', index=False)

    def rename_tables(self, table_name):
        if table_name + '.csv' in os.listdir(self.path):
            if table_name == 'Subcohorts':
                os.rename(self.path + 'Subcohorts.csv', self.path + 'Subpopulations.csv')
            elif table_name == 'Subcohort counts':
                os.rename(self.path + 'Subcohort counts.csv', self.path + 'Subpopulation counts.csv')
            elif table_name == 'Quantitative information':
                os.rename(self.path + 'Quantitative information.csv', self.path + 'Resource counts.csv')
            elif table_name == 'Linked resources':
                os.rename(self.path + 'Linked resources.csv', self.path + 'Linkages.csv')


def strip_resource(resource_name):
    if not pd.isna(resource_name):
        if '_CDM' in resource_name:
            resource_name = resource_name[:-4]

    return resource_name


def is_repeated(var_name, df_repeats):
    # Checks whether a variable is repeated or not
    if var_name in df_repeats['is repeat of.name'].to_list():
        return True
    elif var_name.endswith('_'):  # selects 'root' variables that were used for LongITools mappings
        return True
    else:
        return False


def restructure_repeats(df_variables, df_repeats):
    # restructuring of cdm repeats
    df_variables = df_variables.drop_duplicates(subset=['resource', 'dataset', 'name'])   # keep unique entries, gets rid of LongITools 'root' variables

    # get collection events from repeats for EXPANSE_CDM
    collection_events = 'Baseline,'
    for i in range(1, 25):
        collection_events += 'Followup' + str(i) + ','
    collection_events += 'Followup25'

    df_variables['collection event.name'] = np.where((df_variables['resource'] == 'EXPANSE') &
                                                     (df_variables['is_repeated']), collection_events,
                                                     df_variables['collection event.name'])

    # derive repeat unit and repeat min and max
    df_variables.loc[:, 'repeat unit'] = df_variables['name'].apply(get_repeat_unit, df=df_repeats)  # get repeat unit from repeat_num
    df_variables.loc[:, 'repeat min'] = ''
    df_variables.loc[df_variables['is_repeated'], 'repeat min'] = 0
    df_variables.loc[df_variables['repeat unit'] == 'Month', 'repeat max'] = 270
    df_variables.loc[df_variables['repeat unit'] == 'Week', 'repeat max'] = 42
    df_variables.loc[df_variables['repeat unit'] == 'Year', 'repeat max'] = 21
    df_variables.loc[df_variables['repeat unit'] == 'Trimester', 'repeat max'] = 3

    return df_variables


def remove_number(var_name):
    new_var_name = var_name.strip(digits)

    return new_var_name


def get_repeat_unit(var_name, df):
    # monthly (0-270), yearly (0-21 or 0-17), weekly (0-42), trimester (t1-t3)
    if var_name + '270' in df['name'].to_list():
        return 'Month'
    elif var_name + '42' in df['name'].to_list():
        return 'Week'
    elif var_name + '17' in df['name'].to_list():
        return 'Year'
    elif var_name + '3' in df['name'].to_list():
        return 'Trimester'


def get_repeat_number(s):
    # get repeat number from target variable
    repeat_num = re.sub('.*?([0-9]*)$',r'\1',s)
    return repeat_num


def rewrite_mappings(df, df_no_duplicates):
    df_no_duplicates.loc[:, 'repeats'] = ''
    df_mappings = pd.DataFrame()
    # divide df_no_duplicates per source
    list_source = df['source'].drop_duplicates().tolist()  # list all sources (e.g. cohorts)
    for source in list_source:
        # select unique mappings per source
        df_no_duplicates_per_source = df_no_duplicates[df_no_duplicates['source'] == source]
        # select original mappings per source
        df_per_source = df[df['source'] == source]
        df_no_duplicates_per_source = get_repeated_mappings_per_source(df_per_source, df_no_duplicates_per_source)
        df_mappings = pd.concat([df_mappings, df_no_duplicates_per_source])

    return df_mappings


def get_repeated_mappings_per_source(df_per_source, df_no_duplicates_per_source):
    for i in df_no_duplicates_per_source.index:
        # select all repeats with a matching source, target, match and syntax etc.
        df_select_repeats = df_per_source[(df_per_source['source'] == df_no_duplicates_per_source['source'][i]) &
                                          (df_per_source['source dataset'] == df_no_duplicates_per_source['source dataset'][i]) &
                                          (df_per_source['source variables'] == df_no_duplicates_per_source['source variables'][i]) &
                                          (df_per_source['source variables other datasets.dataset'] == df_no_duplicates_per_source['source variables other datasets.dataset'][i]) &
                                          (df_per_source['source variables other datasets.name'] == df_no_duplicates_per_source['source variables other datasets.name'][i]) &
                                          (df_per_source['target'] == df_no_duplicates_per_source['target'][i]) &
                                          (df_per_source['target dataset'] == df_no_duplicates_per_source['target dataset'][i]) &
                                          (df_per_source['target variable'] == df_no_duplicates_per_source['target variable'][i]) &
                                          (df_per_source['match'] == df_no_duplicates_per_source['match'][i]) &
                                          (df_per_source['syntax'] == df_no_duplicates_per_source['syntax'][i]) &
                                          (df_per_source['comments'] == df_no_duplicates_per_source['comments'][i]) &
                                          (df_per_source['description'] == df_no_duplicates_per_source['description'][i])]
        # get matching repeat numbers in repeat_num column
        repeats = df_select_repeats['repeat_num'].to_list()  # list repeats
        repeats = [int(x) for x in repeats if not x == '']  # cast repeats to integers
        repeats.sort()  # sort repeat numbers
        repeats = str(repeats)
        repeats = repeats.translate({ord(c): None for c in "[]"})
        df_no_duplicates_per_source.loc[i, 'repeats'] = repeats

    return df_no_duplicates_per_source


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


def month_to_num(month, start_or_end):
    if pd.isna(month) and start_or_end == 'start':
        return '01'
    if pd.isna(month) and start_or_end == 'end':
        return '12'
    else:
        return {
                'January': '01',
                'February': '02',
                'March': '03',
                'April': '04',
                'May': '05',
                'June': '06',
                'July': '07',
                'August': '08',
                'September': '09',
                'October': '10',
                'November': '11',
                'December': '12'
        }[month]


def get_end_day(end_month):
    return {
            '01': '31',
            '02': '28',
            '03': '31',
            '04': '30',
            '05': '31',
            '06': '30',
            '07': '31',
            '08': '31',
            '09': '30',
            '10': '31',
            '11': '30',
            '12': '31'
    }[end_month]
