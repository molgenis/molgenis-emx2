import shutil
import os
from pathlib import Path
import pandas as pd
from string import digits
import re


def float_to_int(df):
    """
    Cast float64 Series to Int64.
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
            df = pd.read_csv(file_path, keep_default_na=False)
            df = df.loc[df['profiles'].str.contains(profile)]
            data_model = pd.concat([data_model, df])

    data_model = float_to_int(data_model)
    data_model.to_csv(path_to_write, index=None)


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
        """Get path to data model file and copy molgenis.csv to appropriate folder if it does not exist
        """
        # get molgenis.csv location
        if self.database_type == 'catalogue':
            data_model = os.path.abspath('../../../datacatalogue/molgenis.csv')
            profile_path = Path().cwd().joinpath('..', '..', '..', '_models', 'shared')
            profile = 'DataCatalogueFlat'
            get_data_model(profile_path, data_model, profile)
        # TODO: rewrite to get schema from profile for staging areas
        elif self.database_type == 'network':
            data_model = os.path.abspath('../../../datacatalogue/stagingNetworks/molgenis.csv')
        elif self.database_type == 'cohort':
            data_model = os.path.abspath('../../../datacatalogue/stagingCohorts/molgenis.csv')
        elif self.database_type == 'data_source':
            data_model = os.path.abspath('../../../datacatalogue/stagingRWE/molgenis.csv')
        elif self.database_type == 'cohort_UMCG':
            data_model = os.path.abspath('../../../datacatalogue/stagingCohortsUMCG/molgenis.csv')
        elif self.database_type == 'shared':
            data_model = os.path.abspath('../../../datacatalogue/stagingShared/molgenis.csv')

        # copy molgenis.csv to appropriate folder
        if self.database_type == 'catalogue':
            path = './catalogue_data_model'
            if not os.path.isdir(path):
                os.mkdir(path)
            shutil.copyfile(data_model, os.path.abspath(os.path.join(path, 'molgenis.csv')))
            shutil.make_archive('./catalogue_data_model_upload', 'zip', path)
        else:
            shutil.copyfile(data_model, os.path.abspath(os.path.join(self.path, 'molgenis.csv')))

    def transform_data(self):
        """Make changes per table
        """
        # transformations per table
        self.collections()
        self.organisations()
        self.variables()
        self.network_variables()
        self.variable_mappings()
        self.catalogues()
        self.variable_values()
        # TODO: self.publications()

        # TODO: add dataset type for LongITools, LifeCycle etc
        # TODO: collection counts alter data model & migrate
        for table_name in ['Datasets', 'Dataset mappings', 'Subcohorts',
                           'Collection events', 'External identifiers',
                           'Linked resources', 'Quantitative information', 'Subcohort counts',
                           'DAPs', 'Documentation', 'Contacts', 'Aggregates']:
            self.transform_tables(table_name)
            self.rename_tables(table_name)

    def catalogues(self):
        """Transform columns in Catalogues
        """
        # Cohorts to Collections
        df_catalogues = pd.read_csv(self.path + 'Catalogues.csv')
        df_catalogues['name'] = df_catalogues['network']

        df_catalogues = float_to_int(df_catalogues)  # convert float back to integer
        df_catalogues.to_csv(self.path + 'Catalogues.csv', index=False)

    def organisations(self):
        """ Transform columns in Organisations and alter structure
        """
        df_collection_organisations = pd.DataFrame()
        df_organisations = pd.read_csv(self.path + 'Organisations.csv')

        for table in ['Studies', 'Cohorts', 'Data sources', 'Databanks']:
            df_resource = pd.read_csv(self.path + table + '.csv')

            df_resource = df_resource[['id', 'lead organisation']]
            df_resource.rename(columns={'id': 'collection',
                                        'lead organisation': 'id'}, inplace=True)
            df_resource = df_resource.dropna(axis=0)
            df_resource = df_resource.reset_index()
            df_merged = get_collection_organisations(df_organisations, df_resource)
            df_collection_organisations = pd.concat([df_collection_organisations, df_merged])

        df_collection_organisations = float_to_int(df_collection_organisations)  # convert float back to integer
        df_collection_organisations.to_csv(self.path + 'Collection organisations.csv', index=False)

    def collections(self):
        """Transform columns in Cohorts, Networks, Studies, Data sources, Databanks
        """
        # Cohorts to Collections
        df_cohorts = pd.read_csv(self.path + 'Cohorts.csv')
        df_cohorts.rename(columns={'type': 'cohort type',
                                   'type other': 'cohort type other',
                                   'collection type': 'data collection type'}, inplace=True)
        df_cohorts['collection type'] = 'Cohort'

        # Networks to Collections
        # TODO: Get Networks table out of Collections
        df_networks = pd.read_csv(self.path + 'Networks.csv')
        df_networks.rename(columns={'type': 'network type'}, inplace=True)
        df_networks['collection type'] = 'Network'
        df_networks['models'] = ''

        # Studies to Collections
        df_studies = pd.read_csv(self.path + 'Studies.csv')
        df_studies.rename(columns={'type': 'clinical study type',
                                   'type other': 'study type other'}, inplace=True)
        df_studies['collection type'] = 'Study'

        # Data sources to Collections
        df_data_sources = pd.read_csv(self.path + 'Data sources.csv')
        df_data_sources.rename(columns={'type': 'RWD type',
                                        'type other': 'RWD type other',
                                        'areas of information': 'areas of information rwd'}, inplace=True)
        df_data_sources['collection type'] = 'Data source'

        # Databanks to Collections
        df_databanks = pd.read_csv(self.path + 'Databanks.csv')
        df_databanks.rename(columns={'type': 'datasource type',
                                     'type other': 'datasource type other'}, inplace=True)
        df_databanks['collection type'] = 'Databank'

        # TODO: think about keeping Models as collection type
        df_models = pd.read_csv(self.path + 'Models.csv', keep_default_na=False)
        df_models = df_models[df_models['id'] == 'CRC Screening CDM']  # handles exception CRC Screening CDM
        df_models['collection type'] = ''  # TODO: add term here

        df_models = float_to_int(df_models)  # convert float back to integer

        # concatenate all to Collections
        df_collections = pd.concat([df_cohorts, df_networks, df_studies, df_databanks,
                                    df_data_sources, df_models])

        df_collections = float_to_int(df_collections)  # convert float back to integer
        df_collections.to_csv(self.path + 'Collections.csv', index=False)

    def network_variables(self):
        df = pd.read_csv(self.path + 'Network variables.csv', keep_default_na=False)
        df.rename(columns={'network': 'collection',
                           'variable.resource': 'variable.collection'}, inplace=True)
        df.loc[:, 'collection'] = df['collection'].apply(strip_resource)
        df.loc[:, 'variable.collection'] = df['variable.collection'].apply(strip_resource)
        df.loc[:, 'variable.name'] = df['variable.name'].apply(remove_number)
        df = df.drop_duplicates(subset='variable.name')
        df.rename(columns={'resource': 'collection'}, inplace=True)

        df = float_to_int(df)  # convert float back to integer
        df.to_csv(self.path + 'Collection variables.csv', index=False)

    def variable_values(self):
        # restructure variable values
        df_var_values = pd.read_csv(self.path + 'Variable values.csv', keep_default_na=False)
        df_var_values.rename(columns={'resource': 'collection'}, inplace=True)
        df_var_values.loc[:, 'collection'] = df_var_values['collection'].apply(strip_resource)

        df_var_values_cdm = df_var_values[df_var_values['collection'].isin(['LifeCycle', 'ATHLETE', 'testNetwork1'])]
        df_var_values_cdm.loc[:, 'variable.name'] = df_var_values_cdm['variable.name'].apply(remove_number)

        df_var_values_no_cdm = df_var_values[~df_var_values['collection'].isin(['LifeCycle', 'ATHLETE', 'testNetwork1'])]

        df_all_var_values = pd.concat([df_var_values_no_cdm, df_var_values_cdm])
        # TODO: check if this below is correct
        df_all_var_values = df_all_var_values.drop_duplicates(subset=['collection', 'variable.dataset',
                                                                      'variable.name', 'value'])
        df_all_var_values = float_to_int(df_all_var_values)  # convert float back to integer
        df_all_var_values.to_csv(self.path + 'Variable values.csv', index=False)

    def variables(self):
        # restructure Variables
        df_variables = pd.read_csv(self.path + 'Variables.csv', keep_default_na=False)
        df_variables.loc[:, 'resource'] = df_variables['resource'].apply(strip_resource)
        df_variables.loc[:, 'collection event.resource'] = \
            df_variables['collection event.resource'].apply(strip_resource)

        # restructure repeated variables inside variables dataframe
        df_repeats = pd.read_csv(self.path + 'Repeated variables.csv')
        df_repeats.loc[:, 'resource'] = df_repeats['resource'].apply(strip_resource)
        df_variables.loc[:, 'is_repeated'] = df_variables['name'].apply(is_repeated, df_repeats=df_repeats)

        # select athlete and lifecycle variables and restructure
        df_variables_cdm = df_variables[df_variables['resource'].isin(['LifeCycle', 'ATHLETE', 'testNetwork1'])]
        df_variables_cdm.loc[:, 'name'] = df_variables_cdm['name'].apply(remove_number)
        df_variables_cdm = restructure_repeats(df_variables_cdm, df_repeats)

        # select variables that are not in LifeCycle or ATHLETE or testNetwork1
        df_variables_no_cdm = df_variables[~df_variables['resource'].isin(['LifeCycle', 'ATHLETE', 'testNetwork1'])]
        # select repeated variables that are not in lifecycle or ATHLETE or testNetwork1
        df_repeats_no_cdm = df_repeats[~df_repeats['resource'].isin(['LifeCycle', 'ATHLETE', 'testNetwork1'])]

        # concatenate all variables
        df_all_variables = pd.concat([df_variables_cdm, df_variables_no_cdm, df_repeats_no_cdm])
        df_all_variables.rename(columns={'resource': 'collection',
                                         'collection event.resource': 'collection event.collection'}, inplace=True)

        df_all_variables = float_to_int(df_all_variables)  # convert float back to integer
        df_all_variables.to_csv(self.path + 'Variables.csv', index=False)

    def variable_mappings(self):
        df = pd.read_csv(self.path + 'Variable mappings.csv', keep_default_na=False)
        df.loc[:, 'target'] = df['target'].apply(strip_resource)  # delete appendix '_CDM'
        df.loc[:, 'repeat_num'] = df['target variable'].apply(get_repeat_number)  # get repeat of target variable

        df_cdm = df[df['target'].isin(['LifeCycle', 'ATHLETE', 'testNetwork1'])]
        df_cdm.loc[:, 'target variable'] = df_cdm['target variable'].apply(remove_number)

        #TODO: also get other mappings than those from LifeCycle and ATHLETE
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
        df_mappings = float_to_int(df_mappings)  # convert float back to integer
        df_mappings.to_csv(self.path + 'Mapped variables.csv', index=False)

    def transform_tables(self, table_name):
        df = pd.read_csv(self.path + table_name + '.csv', keep_default_na=False)
        if 'resource' in df.columns:
            df.loc[:, 'resource'] = df['resource'].apply(strip_resource)  # removes _CDM from 'model' name
        if 'target' in df.columns:
            df.loc[:, 'target'] = df['target'].apply(strip_resource)
        if 'subcohorts' in df.columns:
            df.loc[:, 'subcohorts'] = df['subcohorts'].apply(strip_resource)

        df.rename(columns={'resource': 'collection',
                           'main resource': 'main collection',
                           'linked resource': 'linked collection',
                           'other linked resource': 'other linked collection',
                           'subcohort.resource': 'population.collection',
                           'subcohort.name': 'population.name'}, inplace=True)

        df = float_to_int(df)  # convert float back to integer
        df.to_csv(self.path + table_name + '.csv', index=False)

    def rename_tables(self, table_name):
        if table_name == 'Subcohorts':
            os.rename(self.path + 'Subcohorts.csv', self.path + 'Collection subcohorts.csv')
        elif table_name == 'Subcohort counts':
            os.rename(self.path + 'Subcohort counts.csv', self.path + 'Collection subcohort counts.csv')
        elif table_name == 'Datasets':
            os.rename(self.path + 'Datasets.csv', self.path + 'Collection datasets.csv')
        elif table_name == 'Variables':
            os.rename(self.path + 'Variables.csv', self.path + 'Collection variables.csv')
        elif table_name == 'Dataset mappings':
            os.rename(self.path + 'Dataset mappings.csv', self.path + 'Mapped datasets.csv')
        elif table_name == 'DAPs':
            os.rename(self.path + 'DAPs.csv', self.path + 'Collection DAPs.csv')
        elif table_name == 'Quantitative information':
            os.rename(self.path + 'Quantitative information.csv', self.path + 'Collection counts.csv')


def strip_resource(resource_name):
    if not pd.isna(resource_name):
        if '_CDM' in resource_name:  # TODO: handle exception CRC screening CDM under EOSC4Cancer
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
    # TODO: EXPANSE_CDM repeats do not have a repeatUnit
    # restructuring of cdm repeats
    #TODO: rewrite drop duplicates to more stringent version
    df_variables = df_variables.drop_duplicates(subset=['resource', 'dataset', 'name'])   # keep unique entries, gets rid of LongITools 'root' variables
    df_variables.loc[:, 'repeat unit'] = df_variables['name'].apply(get_repeat_unit, df=df_repeats)  # get repeat unit from
    df_variables.loc[:, 'repeat min'] = ''
    df_variables.loc[df_variables['is_repeated'] == True, 'repeat min'] = 0
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
    df_no_duplicates.loc[:, 'number'] = 0  # TODO: lose numbering
    df_no_duplicates.loc[:, 'repeats'] = ''  # TODO: make repeats part of key
    df_mappings = pd.DataFrame()
    # divide df_no_duplicates per source
    list_source = df['source'].drop_duplicates().tolist()
    for source in list_source:
        # select unique mappings per source
        df_no_duplicates_per_source = df_no_duplicates[df_no_duplicates['source'] == source]
        # get mapping numbers
        df_no_duplicates_per_source.loc[:, 'number'] = df_no_duplicates_per_source.reset_index().index
        # select original mappings per source
        df_per_source = df[df['source'] == source]
        df_no_duplicates_per_source = get_repeated_mappings_per_source(df_per_source, df_no_duplicates_per_source)
        df_mappings = pd.concat([df_mappings, df_no_duplicates_per_source])

    # mapping number minus one
    df_mappings.loc[:, 'number'] = df_mappings['number'].apply(minus_one)

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


def get_collection_organisations(df_organisations, df_resource):
    """Merge data resource and organisation to Collection organisations"""
    # get all leading organisations with collection reference in one row
    i = -1
    for row in df_resource['id']:
        i += 1
        if ',' in row:
            org_list = row.split(',')
            for org in org_list:
                new_row = {'collection': df_resource['collection'][i], 'id': org}
                df_resource.loc[len(df_resource)] = new_row
            df_resource = df_resource.drop(index=i)

    # df_resource = df_resource[df_resource['id'].str.contains(",") == False]  # drop rows with multiple organisations

    # merge with df_organisations
    df_merged = pd.merge(df_organisations, df_resource, on='id')

    return df_merged


def minus_one(x):
    x = x - 1

    return x
