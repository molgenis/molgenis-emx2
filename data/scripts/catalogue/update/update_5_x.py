import logging
import os
import re
import shutil
from pathlib import Path
from string import digits

import numpy as np
import pandas as pd
from decouple import config

CATALOGUE_SCHEMA_NAME = config('MG_CATALOGUE_SCHEMA_NAME')
SHARED_STAGING_NAME = config('MG_SHARED_STAGING_NAME')

FILES_DIR = Path(__file__).parent.joinpath('..', 'files').resolve()


def float_to_int(df):
    """
    Cast float64 Series to Int64. Floats are not converted to integers by EMX2
    """
    float_columns = df.dtypes[df.dtypes == 'float64'].index.values
    df[float_columns] = df[float_columns].astype('Int64')

    return df


def get_data_model(profile_path: Path, path_to_write: Path, profile: str):
    # get data model from profile and write to file
    data_model = pd.DataFrame()

    for file_name in profile_path.iterdir():
        if file_name.suffix == '.csv':
            file_path = profile_path / file_name
            df = pd.read_csv(file_path, keep_default_na=False, dtype='object')
            df = df.loc[df['profiles'].apply(lambda p: profile in p.split(','))]
            data_model = pd.concat([data_model, df])

    data_model.to_csv(path_to_write / 'molgenis.csv', index=False)


class Transform:
    """General functions to update catalogue data model.
    """

    def __init__(self, database_name, database_type):
        self.database_name = database_name
        self.database_type = database_type
        self.path = FILES_DIR / f"{self.database_name}_data"

    def __repr__(self):
        return f"Transform(database_name={self.database_name!r}, database_type={self.database_type!r})"

    def delete_data_model_file(self):
        """Delete molgenis.csv
        """
        self.path.joinpath('molgenis.csv').unlink()

    def update_data_model_file(self):
        """Get data model from profile and copy molgenis.csv to appropriate folder
        """
        profile_path = Path(__file__).parent.joinpath('..', '..', '..', '_models', 'shared').resolve()
        path_to_write = self.path
        if self.database_type == 'catalogue':
            profile = 'DataCatalogueFlat'
            path_to_write = FILES_DIR / f"{self.database_name}_data_model"
            if not path_to_write.exists():
                path_to_write.mkdir()
            get_data_model(profile_path, path_to_write, profile)
            shutil.make_archive(str(FILES_DIR / f"{self.database_name}_data_model_upload"), 'zip', path_to_write)
        else:
            if self.database_type == 'network':
                profile = 'NetworksStaging'
            elif self.database_type == 'cohort':
                profile = 'CohortsStaging'
            elif self.database_type == 'data_source':
                profile = 'RWEStaging'
            elif self.database_type == 'cohort_UMCG':
                profile = 'UMCGCohortsStaging'
            elif self.database_type == 'shared_staging':
                profile = 'SharedStaging'
            else:
                raise NotImplementedError(f"Cannot update data model for profile {self.database_type!r}.")

            get_data_model(profile_path, path_to_write, profile)

    def transform_data(self):
        """Make changes per table
        """
        if self.database_type == 'shared_staging':
            return
        # transformations per table
        if self.database_type == 'catalogue':
            self.catalogues()
            self.network_variables()

        if self.database_type != 'shared_staging':
            self.resources()
            self.organisations()
            self.publications()
            self.external_identifiers()

        if self.database_type != 'cohort_UMCG':
            self.variables()
            self.variable_values()

        if self.database_type != 'network':
            self.variable_mappings()
        if self.database_type not in ['data_source', 'network']:
            self.collection_events()
            self.subcohorts()

        # TODO: for vac4eu BPE model is an exception, not part of a network, also other model in VAC4EU
        # TODO: move DAPs to Organisations.role = data access provider (remove all other columns)
        # TODO: move 'Data sources.data holder' to Organisations.role = 'data holder'
        for table_name in ['Datasets', 'Dataset mappings', 'External identifiers', 'Subcohorts', 'Subcohort counts',
                           'Collection events', 'Quantitative information', 'Documentation', 'Contacts',
                           'Variables', 'Variable values', 'Linked resources']:
            self.transform_tables(table_name)

        for table_name in ['Subcohorts', 'Quantitative information', 'Subcohort counts', 'Linked resources']:
            self.rename_tables(table_name)

    def catalogues(self):
        """Transform columns in Catalogues
        """
        logging.debug("Transforming 'Catalogues'")
        # Cohorts to Resources
        df_catalogues = pd.read_csv(self.path / 'Catalogues.csv', dtype='object')
        df_catalogues['name'] = df_catalogues['network']

        df_catalogues.to_csv(self.path / 'Catalogues.csv', index=False)

    def resources(self):
        """Transform columns in Cohorts, Networks, Studies, Data sources, Databanks
        """
        logging.debug(f"Transforming 'Resources'")
        # Cohorts to Resources
        df_cohorts, df_networks, df_studies = pd.DataFrame(), pd.DataFrame(), pd.DataFrame()
        df_databanks, df_data_sources, df_models = pd.DataFrame(), pd.DataFrame(), pd.DataFrame()
        if self.database_type in ['catalogue', 'cohort', 'cohort_UMCG']:

            df_cohorts = pd.read_csv(self.path / 'Cohorts.csv', dtype='object')
            df_cohorts = df_cohorts.rename(columns={'type': 'cohort type',
                                                    'type other': 'cohort type other',
                                                    'collection type': 'data collection type',
                                                    'inclusion criteria other': 'other inclusion criteria'})
            # infer resource type from cohort type
            df_cohorts['type'] = df_cohorts['cohort type'].apply(get_resource_type)
            # delete cohort type 'Study', 'Registry', 'Clinical trial' and 'Biobank'
            df_cohorts['cohort type'] = df_cohorts['cohort type'].apply(get_cohort_type)
            # reformat keywords
            df_cohorts['keywords'] = df_cohorts['keywords'].apply(reformat_keywords)
            # get resources that are part of network
            if self.database_type in ['cohort']:
                cols_to_find = ['studies']
                i_cols = [df_cohorts.columns.get_loc(col) for col in cols_to_find]
                df_cohorts['resources'] = df_cohorts[df_cohorts.columns[i_cols]]\
                    .apply(lambda x: ','.join(x.dropna().astype(str)), axis=1)

        # Networks to Resources
        if self.database_type in ['catalogue', 'network']:
            df_networks = pd.read_csv(self.path / 'Networks.csv', dtype='object')
            df_networks = df_networks.rename(columns={'type': 'network type'})
            df_networks['type'] = 'Network'

            # get resources that are part of network
            cols_to_find = ['cohorts', 'data sources', 'databanks']
            if self.database_type == 'catalogue':
                cols_to_find.append('networks')
            i_cols = [df_networks.columns.get_loc(col) for col in cols_to_find]
            df_networks['resources'] = df_networks[df_networks.columns[i_cols]]\
                .apply(lambda x: ','.join(x.dropna().astype(str)), axis=1)

        # Studies to Resources
        if self.database_type in ['catalogue']:
            df_studies = pd.read_csv(self.path / 'Studies.csv', dtype='object')
            df_studies.rename(columns={'study design classification': 'clinical study type',
                                       'type other': 'study type other',
                                       'number of subjects': 'number of participants',
                                       'age groups': 'population age groups'}, inplace=True)
            df_studies['type'] = 'Clinical trial'

        # Data sources to Resources
        if self.database_type in ['catalogue', 'data_source']:
            df_data_sources = pd.read_csv(self.path / 'Data sources.csv', dtype='object')
            df_data_sources.rename(columns={'type': 'RWD type',
                                            'type other': 'RWD type other',
                                            'areas of information': 'areas of information rwd',
                                            'informed consent': 'informed consent required'}, inplace=True)

            # transform dates to years
            df_data_sources['start year'] = df_data_sources['date established'].apply(get_year_from_date)
            df_data_sources['end year'] = df_data_sources['end data collection'].apply(get_year_from_date)
            # transform keywords
            df_data_sources['keywords'] = df_data_sources['keywords'].apply(reformat_keywords)

            df_data_sources['type'] = 'Data source'

            # Databanks to Resources
            df_databanks = pd.read_csv(self.path / 'Databanks.csv', dtype='object')
            df_databanks.rename(columns={'type': 'RWD type',
                                         'type other': 'RWD type other',
                                         'areas of information': 'areas of information rwd',
                                         'informed consent': 'informed consent required'}, inplace=True)

            # transform dates to years
            df_databanks['start year'] = df_databanks['date established'].apply(get_year_from_date)
            df_databanks['end year'] = df_databanks['end data collection'].apply(get_year_from_date)
            # transform keywords
            df_databanks['keywords'] = df_databanks['keywords'].apply(reformat_keywords)

            df_databanks['type'] = 'Databank'

        # Models to Resources
        if self.database_type in ['catalogue', 'network']:
            df_models = pd.read_csv(self.path / 'Models.csv', dtype='object')
            df_models = df_models[df_models['id'].isin(['CRC Screening CDM', 'OMOP-CDM'])]  # handles exceptions
            df_models['type'] = 'Common data model'

        # merge all to Resources
        if self.database_type == 'catalogue':
            df_resources = pd.concat([df_cohorts, df_networks, df_studies, df_databanks, df_data_sources, df_models])
        elif self.database_type in ['cohort', 'cohort_UMCG']:
            df_resources = df_cohorts
        elif self.database_type == 'data_source':
            df_resources = df_data_sources
        elif self.database_type == 'network':
            df_resources = pd.concat([df_networks, df_models])
        else:
            raise NotImplementedError(f"Cannot update data model for profile {self.database_type!r}.")

        df_resources.to_csv(self.path / 'Resources.csv', index=False)

    def organisations(self):
        """ Transform columns in Organisations and alter structure
        """
        logging.debug(f"Transforming 'Organisations'")
        if self.database_name not in ['testCohort', 'testNetwork', 'testDatasource']:
            if self.database_type == 'catalogue':
                df_organisations = pd.read_csv(self.path / 'Organisations.csv', dtype='object')
            else:
                df_organisations = pd.read_csv(FILES_DIR / (SHARED_STAGING_NAME + '_data') / 'Organisations.csv',
                                               dtype='object')

            # Get lead and additional organisations from Resources table
            df_resources = pd.read_csv(self.path / 'Resources.csv', dtype='object')

            df_resources['lead organisation'] = df_resources['lead organisation'].str.split(',')
            df_resources['additional organisations'] = df_resources['additional organisations'].str.split(',')
            df_resources = df_resources.explode('lead organisation').explode('additional organisations')

            leads = df_resources.rename(columns={'id': 'resource', 'lead organisation': 'id'})[['id', 'resource']]
            additional = df_resources.rename(columns={'id': 'resource', 'additional organisations': 'id'})[['id', 'resource']]
            leads['is lead organisation'] = True
            additional['is lead organisation'] = False

            # Get contacts organisations from Contacts table
            df_contacts = pd.read_csv(self.path / 'Contacts.csv', dtype='object')[['resource', 'organisation']]
            df_contacts = df_contacts.rename(columns={'organisation': 'id'})
            df_contacts['is lead organisation'] = False

            # Merge the tables with the original Organisations table and concatenate them into a DataFrame containing
            # all information about all organisations
            df_all_organisations = pd.concat([pd.merge(df_organisations, leads, on='id', how='left'),
                                              pd.merge(df_organisations, additional, on='id', how='left'),
                                              pd.merge(df_organisations, df_contacts, on='id', how='left')])

            # Keep only organisations referenced from the schema's Resources table
            df_all_organisations = df_all_organisations.sort_values(by=['id', 'resource', 'is lead organisation'],
                                                                    ascending=[True, True, False])
            df_all_organisations = df_all_organisations.loc[df_all_organisations['resource'].isin(df_resources['id'])]
            df_all_organisations = df_all_organisations.drop_duplicates(subset=['resource', 'id'], keep='first')

            df_all_organisations.to_csv(self.path / 'Organisations.csv', index=False)

        else:
            # get information from test staging areas
            if self.database_name == 'testCohort':
                data = [['testCohort', 'UMCG', 'University Medical Center Groningen', 'Netherlands (the)',
                        'https://www.umcg.nl/']]
                df_organisations = pd.DataFrame(data, columns=['resource', 'id', 'name', 'country', 'website'])
                df_organisations.to_csv(self.path / 'Organisations.csv', index=False)

            elif self.database_name == 'testDatasource':
                data = [['testDatasource', 'AU', 'University of Aarhus', 'Denmark']]
                df_organisations = pd.DataFrame(data, columns=['resource', 'id', 'name', 'country'])
                df_organisations.to_csv(self.path / 'Organisations.csv', index=False)


    def publications(self):
        """Transform Publications table
        """
        logging.debug(f"Transforming 'Publications'")
        df_resources = pd.read_csv(self.path / 'Resources.csv', dtype='object')
        df_publications = pd.read_csv(self.path / 'Publications.csv', dtype='object')

        df_other_pubs = pd.DataFrame()
        df_design_paper = pd.DataFrame()
        if not len(df_publications) == 0:
            if self.database_type != 'network':
                df_design_paper = df_resources[['id', 'design paper']].copy()
                df_design_paper['is design publication'] = 'true'
                df_design_paper = df_design_paper.rename(columns={'id': 'resource',
                                                                  'design paper': 'doi'})
                df_design_paper = df_design_paper.dropna(axis=0)

            if self.database_type != 'cohort_UMCG':
                df_other_pubs = df_resources[['id', 'publications']].copy()
                df_other_pubs['is design publication'] = 'false'
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
            df_resource_pubs.to_csv(self.path / 'Publications.csv', index=False)

    def external_identifiers(self):
        logging.debug(f"Transforming 'External identifiers'")
        df = pd.read_csv(self.path / 'External identifiers.csv', keep_default_na=False, dtype='object')
        df_internal = df[df['external identifier type'].isin(['UMCG register Utopia', 'UMCG PaNaMaID'])]
        df_external = df[~df['external identifier type'].isin(['UMCG register Utopia', 'UMCG PaNaMaID'])]

        df_internal.to_csv(self.path / 'Internal identifiers.csv', index=False)
        df_external.to_csv(self.path / 'External identifiers.csv', index=False)

    def network_variables(self):
        logging.debug(f"Transforming 'Network variables'")
        df = pd.read_csv(self.path / 'Network variables.csv', keep_default_na=False, dtype='object')
        df_repeats = pd.read_csv(self.path / 'Repeated variables.csv', dtype='object')
        df['resource'] = df['network'].apply(strip_resource)
        df['variable.resource'] = df['variable.resource'].apply(strip_resource)

        # remove end digits from repeated variable names
        df['stripped_var'] = df['variable.name'].apply(remove_number)  # remove end digits from all variable names
        # df['repeated'] = df['variable.name'].apply(is_repeated_variable, df_repeats=df_repeats)  # check if variable is repeated
        df['repeated'] = df['variable.name'].isin(df_repeats[['name', 'is repeat of.name']].stack())  # check if variable is repeated
        df['variable.name'] = df.apply(lambda x: x['stripped_var'] if x.repeated else x['variable.name'], axis=1)  # if repeated, keep stripped variable name

        df = df.drop_duplicates(subset=['resource', 'variable.resource', 'variable.name'])
        df.to_csv(self.path / 'Reused variables.csv', index=False)

    def variable_values(self):
        logging.debug(f"Transforming 'Variable values'")

        networks = ['LifeCycle', 'ATHLETE', 'EXPANSE']
        # restructure variable values
        df_var_values = pd.read_csv(self.path / 'Variable values.csv', keep_default_na=False, dtype='object')
        df_var_values = df_var_values.rename(columns={'variable.dataset': 'dataset',
                                                      'variable.name': 'variable'})

        df_var_values['resource'] = df_var_values['resource'].apply(strip_resource)
        df_repeats = pd.read_csv(self.path / 'Repeated variables.csv', dtype='object')

        df_var_values_cdm = df_var_values[df_var_values['resource'].isin(['testNetwork1', *networks])].copy()
        # remove end digits from repeated variable names
        df_var_values_cdm['stripped_var'] = df_var_values_cdm['variable'].apply(remove_number)  # remove end digits from all variable names
        df_var_values_cdm['repeated'] = df_var_values_cdm['variable'].isin(df_repeats[['name', 'is repeat of.name']].stack())
        df_var_values_cdm['variable'] = df_var_values_cdm.apply(lambda x: x['stripped_var'] if x.repeated else x['variable'], axis=1)  # if repeated, keep stripped variable name

        df_var_values_no_cdm = df_var_values[~df_var_values['resource'].isin(['testNetwork1', *networks])]

        df_all_var_values = pd.concat([df_var_values_no_cdm, df_var_values_cdm])
        df_all_var_values = df_all_var_values.drop_duplicates(subset=['resource', 'dataset',
                                                                      'variable', 'value'])
        df_all_var_values.to_csv(self.path / 'Variable values.csv', index=False)

    def variables(self):
        logging.debug(f"Transforming 'Variables'")

        networks = ['LifeCycle', 'ATHLETE', 'EXPANSE']

        # restructure Variables
        df_variables = pd.read_csv(self.path / 'Variables.csv', keep_default_na=False, dtype='object')
        df_variables['resource'] = df_variables['resource'].apply(strip_resource)
        df_variables_cdm = pd.DataFrame()

        # restructure repeated variables inside variables dataframe
        df_repeats = pd.read_csv(self.path / 'Repeated variables.csv', dtype='object')
        df_repeats['resource'] = df_repeats['resource'].apply(strip_resource)
        df_variables['is_repeated'] = df_variables['name'].isin(df_repeats['is repeat of.name'])

        # select athlete, lifecycle, expanse and testNetwork1 variables and restructure (these contain repeats)
        if self.database_type in ['catalogue', 'network'] and \
                self.database_name in ['catalogue', *networks]:
            df_variables_cdm = df_variables[df_variables['resource'].isin(['testNetwork1', *networks])].copy()

            # remove end digits from repeated variable names
            df_variables_cdm['stripped_name'] = df_variables_cdm['name'].apply(remove_number)  # remove end digits from all vars
            df_variables_cdm['name'] = df_variables_cdm.apply(lambda v: v.stripped_name if v.is_repeated else v['name'], axis=1)  # if repeated, keep stripped var name
            df_variables_cdm = restructure_repeats(df_variables_cdm, df_repeats)

        # select variables that are not in LifeCycle or ATHLETE or testNetwork1
        df_variables_no_cdm = df_variables[~df_variables['resource'].isin(['testNetwork1', *networks])]
        # select repeated variables that are not in lifecycle or ATHLETE or testNetwork1
        df_repeats_no_cdm = df_repeats[~df_repeats['resource'].isin(['testNetwork1', *networks])]

        # concatenate all variables
        if self.database_type in ['catalogue', 'network'] and \
                self.database_name in ['catalogue', *networks]:
            df_all_variables = pd.concat([df_variables_cdm, df_variables_no_cdm, df_repeats_no_cdm])
        else:
            df_all_variables = pd.concat([df_variables_no_cdm, df_repeats_no_cdm])

        df_all_variables = float_to_int(df_all_variables)  # convert float back to integer
        df_all_variables.to_csv(self.path / 'Variables.csv', index=False)

    def variable_mappings(self):
        logging.debug(f"Transforming 'Variable mappings'")
        df = pd.read_csv(self.path / 'Variable mappings.csv', keep_default_na=False, dtype='object')
        if len(df.index) == 0:
            return

        networks = ['testNetwork1', 'LifeCycle', 'ATHLETE', 'EXPANSE']

        df_repeats = pd.read_csv(self.path / 'Repeated variables.csv', dtype='object')
        df_repeats['resource'] = df_repeats['resource'].apply(strip_resource)
        # Strip the '_CDM' suffix from the target resource
        df['target'] = df['target'].apply(strip_resource)

        df['is_repeated'] = df['target variable'].isin(df_repeats[['name', 'is repeat of.name']].stack())
        df['in_network'] = df['target'].isin(networks)

        # Split the target variable name in the base name and repeat number
        df['repeat_num'] = df['target variable'].apply(get_repeat_number)
        df['stripped_var'] = df['target variable'].apply(remove_number)

        df['target variable'] = df.apply(lambda row: row['stripped_var']
                                         if (row['is_repeated'] and row['in_network'])
                                         else row['target variable'], axis=1)

        # Aggregate the repeat numbers for the same target variables into a sorted list under the 'repeats' column
        columns = df.columns.drop(['repeat_num']).to_list()
        df_n = df.groupby(columns).agg({'repeat_num': list}).reset_index()
        df_n['repeats'] = df_n.apply(lambda row: ', '.join(map(str, sorted(row['repeat_num']))), axis=1)

        # Correct the columns and save to file
        df_n = df_n.drop(columns=['repeat_num', 'is_repeated', 'in_network', 'stripped_var'])
        df_n.to_csv(self.path / 'Variable mappings.csv', index=False)


    def collection_events(self):
        """ Transform Collection events table
        """
        logging.debug(f"Transforming 'Collection events'")
        df = pd.read_csv(self.path / 'Collection events.csv', dtype='object')
        df['start month'] = df['start month'].apply(month_to_num, start_or_end='start')
        df['start date'] = df['start year'].astype('Int64').astype('string') + '-' + df['start month'] + '-01'
        df['end month'] = df['end month'].apply(month_to_num, start_or_end='end')
        df['end day'] = df['end month'].apply(get_end_day)
        df['end date'] = df['end year'].astype('Int64').astype('string') + '-' + df['end month'] + '-' + df['end day']

        df.to_csv(self.path / 'Collection events.csv', index=False)

    def subcohorts(self):
        logging.debug(f"Transforming 'Subcohorts'")
        """ Transform Subcohorts table
        """
        df = pd.read_csv(self.path / 'Subcohorts.csv', dtype='object')
        df = df.rename(columns={'inclusion criteria': 'other inclusion criteria'})

        df.to_csv(self.path / 'Subcohorts.csv', index=False)

    def transform_tables(self, table_name):
        logging.debug(f"Transforming {table_name!r}")
        if table_name + '.csv' in os.listdir(self.path):
            df = pd.read_csv(self.path / (table_name + '.csv'), keep_default_na=False, dtype='object')
            if 'resource' in df.columns:
                df['resource'] = df['resource'].apply(strip_resource)  # removes _CDM from 'model' name
            if 'target' in df.columns:
                df['target'] = df['target'].apply(strip_resource)
            if 'subcohorts' in df.columns:
                df['subcohorts'] = df['subcohorts'].apply(strip_resource)

            df.rename(columns={'subcohort.resource': 'resource',
                               'subcohort.name': 'subpopulation',
                               'subcohorts': 'subpopulations',
                               'collection event.name': 'collection event',
                               'network': 'resource',
                               'main resource': 'resource'}, inplace=True)

            df.to_csv(self.path / (table_name + '.csv'), index=False)

    def rename_tables(self, table_name):
        if table_name + '.csv' in os.listdir(self.path):
            if table_name == 'Subcohorts':
                os.rename(self.path / 'Subcohorts.csv', self.path / 'Subpopulations.csv')
            elif table_name == 'Subcohort counts':
                os.rename(self.path / 'Subcohort counts.csv', self.path / 'Subpopulation counts.csv')
            elif table_name == 'Quantitative information':
                os.rename(self.path / 'Quantitative information.csv', self.path / 'Resource counts.csv')
            elif table_name == 'Linked resources':
                os.rename(self.path / 'Linked resources.csv', self.path / 'Linkages.csv')


def strip_resource(resource_name):
    if str(resource_name).endswith('_CDM'):
        return resource_name.split('_CDM')[0]

    return resource_name


def is_repeated(var_name, df_repeats):
    # Checks whether a variable is repeated or not
    if var_name in df_repeats['is repeat of.name'].to_list():
        return True
    else:
        return False


def is_repeated_variable(var_name, df_repeats):
    # Checks whether a variable is in repeated variables
    if var_name in df_repeats['name'].values:
        return True
    elif var_name in df_repeats['is repeat of.name'].values:
        return True
    else:
        return False


def restructure_repeats(df_variables, df_repeats):
    # restructuring of cdm repeats
    df_variables = df_variables.drop_duplicates(subset=['resource', 'dataset', 'name']).copy()   # keep unique entries, gets rid of LongITools 'root' variables

    # get collection events from repeats for EXPANSE_CDM
    collection_events = 'Baseline,' + ','.join(f"Followup{month}" for month in range(1, 26))

    df_variables['collection event.name'] = np.where((df_variables['resource'] == 'EXPANSE') &
                                                     (df_variables['is_repeated']), collection_events,
                                                     df_variables['collection event.name'])

    # derive repeat unit and repeat min and max
    df_variables['repeat unit'] = df_variables['name'].apply(get_repeat_unit, df=df_repeats)  # get repeat unit from repeat_num
    df_variables['repeat min'] = ''
    df_variables.loc[df_variables['is_repeated'], 'repeat min'] = 0
    df_variables.loc[df_variables['repeat unit'] == 'Month', 'repeat max'] = 270
    df_variables.loc[df_variables['repeat unit'] == 'Week', 'repeat max'] = 42
    df_variables.loc[df_variables['repeat unit'] == 'Year', 'repeat max'] = 21
    df_variables.loc[df_variables['repeat unit'] == 'Trimester', 'repeat min'] = 1
    df_variables.loc[df_variables['repeat unit'] == 'Trimester', 'repeat max'] = 3

    return df_variables


def remove_number(var_name):
    var_name = var_name.strip(digits)

    return var_name


def get_repeat_unit(var_name, df):
    # monthly (0-270), yearly (0-21 or 0-17), weekly (0-42), trimester (t1-t3)
    if var_name + '270' in df['name'].values:
        return 'Month'
    elif var_name + '42' in df['name'].values:
        return 'Week'
    elif var_name + '17' in df['name'].values:
        return 'Year'
    elif var_name + '3' in df['name'].values:
        return 'Trimester'


def get_repeat_number(s):
    # get repeat number from target variable
    try:
        return int(re.sub('.*?([0-9]*)$',r'\1',s))
    except ValueError:
        return ''


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


def get_year_from_date(date):
    if not pd.isna(date):
        year = date[0:4]

        return year


def reformat_keywords(keywords):
    if not pd.isna(keywords):
        keywords = keywords.replace(';', ',')

    return keywords


def get_resource_type(cohort_type):
    resource_type = []
    if not pd.isna(cohort_type):
        if any(c in cohort_type for c in ['Clinical trial', 'Study']):
            resource_type.append('Clinical trial')
        if 'Registry' in cohort_type:
            resource_type.append('Registry')
        if 'Biobank' in cohort_type:
            resource_type.append('Biobank')
        if any(c in cohort_type for c in ['Birth cohort', 'Clinical cohort', 'Case-control', 'Case only', 'Population cohort']):
            resource_type.append('Cohort study')
    else:
        resource_type.append('Cohort study')

    resource_type = ','.join(resource_type)

    return resource_type


def get_cohort_type(cohort_type):
    if not pd.isna(cohort_type):
        cohort_type = cohort_type.replace('Biobank', '')
        cohort_type = cohort_type.replace('Clinical trial', '')
        cohort_type = cohort_type.replace('Registry', '')
        cohort_type = cohort_type.replace('Study', '')
        cohort_type = cohort_type.replace(',,', ',')
        cohort_type = cohort_type.strip(',')

    return cohort_type
