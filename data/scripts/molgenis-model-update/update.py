import shutil
import stat
import numpy as np
import os
import pandas as pd
# import pathlib
import sys
import logging
from spaces import spaces

from zipfile import ZipFile


def float_to_int(df):
    """
    Cast float64 Series to Int64.
    """
    for column in df.columns:
        if df[column].dtype == 'float64':
            df.loc[:, column] = df[column].astype('Int64')

    return df


def get_new_column_names(df, database):
    """Remove spaces from column names
    """
    old_names = df.columns.to_list()
    new_names = []

    for name in old_names:
        new_name = spaces(name, database)
        new_names.append(new_name)
    df.columns = new_names

    return df


class TransformDataCatalogue:
    """Update data model from 2.8 to 3.0.
    """

    def __init__(self, database):
        self.database = database
        self.path = './' + self.database + '_data/'
        self.logger = logging.getLogger(' data update and transform')

    def remove_unzipped_data(self):
        """Remove extracted unzipped data from previous run of script
        """
        try:
            # remove unzipped data and avoid Windows PermissionError
            shutil.rmtree(self.path, onerror=lambda func, path, _: (os.chmod(path, stat.S_IWRITE), func(path)))
        except FileNotFoundError:
            self.logger.info('No unzipped data was found')

    def unzip_data(self):
        """Extract data.zip
        """
        data = ZipFile(self.database + '_data.zip')
        try:
            data.extractall(self.path)
        except FileNotFoundError:
            self.logger.error('unzip failed')
            exit()
        except PermissionError:
            self.logger.error('Error: unzip failed, permission denied')
            exit()
        try:
            if os.path.exists(self.database + '_data.zip'):
                os.remove(self.database + '_data.zip')
        except PermissionError:
            # remove fails on windows, is not needed on Windows, pass
            self.logger.warning('Warning: Error deleting data.zip')

    def delete_data_model_file(self):
        """Delete molgenis.csv
        """
        os.remove(self.path + 'molgenis.csv')

    def update_data_model_file(self):
        """Replace data model file
        """
        # copy updated molgenis.csv to os.listdir(self.path)
        shutil.copy(os.path.abspath('../../datacatalogue3/molgenis.csv'),
                    os.path.abspath(self.path))

    def transform_data(self):
        """Make changes per table
        """
        self.contacts()
        self.tables()
        self.variables()
        self.variable_values()
        self.repeated_variables()
        self.dataset_mappings()
        self.variable_mappings()
        self.datasources()

    def get_spaces(self):
        """Get spaces and lowercase in table names and column names
        """
        for file_name in os.listdir(self.path):
            if not file_name == '_files':
                try:
                    df = pd.read_csv(self.path + file_name, keep_default_na=False)
                    df = get_new_column_names(df, self.database)
                    df = float_to_int(df)  # convert float back to integer
                    df.to_csv(self.path + file_name, index=False)
                except pd.errors.EmptyDataError:
                    pass
                new_file_name = spaces(file_name, self.database)
                os.rename(os.path.join(self.path, file_name), os.path.join(self.path, new_file_name))

    def contacts(self):
        """Merge Contributions & Contacts on firstName and surname and rename columns
        """
        df_contributions = pd.read_csv(self.path + 'Contributions.csv')
        df_contributions.rename(columns={'contributionType': 'role',
                                         'contact.firstName': 'firstName',
                                         'contact.surname': 'surname'}, inplace=True)
        df_contacts = pd.read_csv(self.path + 'Contacts.csv')
        df_contacts_merged = pd.merge(df_contributions, df_contacts, on=['firstName', 'surname'])
        df_contacts_merged.rename(columns={'surname': 'lastName'}, inplace=True)
        df_contacts_merged = float_to_int(df_contacts_merged)  # convert float back to integer
        df_contacts_merged.to_csv(self.path + 'Contacts.csv', index=False)

    def tables(self):
        """Merge TargetTables and SourceTables and rename columns
        """
        df_target_tables = pd.read_csv(self.path + 'TargetTables.csv')
        df_source_tables = pd.read_csv(self.path + 'SourceTables.csv')
        df_datasets = pd.concat([df_source_tables, df_target_tables])
        df_datasets.rename(columns={'dataDictionary.resource': 'resource'}, inplace=True)
        df_datasets = float_to_int(df_datasets)  # convert float back to integer
        df_datasets.to_csv(self.path + 'Datasets.csv', index=False)

    def variables(self):
        """Merge TargetVariables and SourceVariables and rename columns
        """
        df_target_vars = pd.read_csv(self.path + 'TargetVariables.csv', keep_default_na=False)
        df_source_vars = pd.read_csv(self.path + 'SourceVariables.csv', keep_default_na=False)
        df_variables = pd.concat([df_source_vars, df_target_vars])
        df_variables = float_to_int(df_variables)  # convert float back to integer
        df_variables.rename(columns={'dataDictionary.resource': 'resource',
                                     'table': 'dataset'}, inplace=True)
        df_variables.to_csv(self.path + 'Variables.csv', index=False)

    def variable_values(self):
        """Merge TargetVariableValues and SourceVariableValues and rename columns
        """
        df_target_vars_values = pd.read_csv(self.path + 'TargetVariableValues.csv', keep_default_na=False)
        df_source_vars_values = pd.read_csv(self.path + 'SourceVariableValues.csv', keep_default_na=False)
        df_variable_values = pd.concat([df_source_vars_values, df_target_vars_values])
        df_variable_values.rename(columns={'dataDictionary.resource': 'resource',
                                           'variable.table': 'variable.dataset',
                                           'ontologyTermIRI': 'ontologyTermURI'}, inplace=True)
        df_variable_values = float_to_int(df_variable_values)  # convert float back to integer
        df_variable_values.to_csv(self.path + 'VariableValues.csv', index=False)

    def repeated_variables(self):
        """Merge RepeatedTargetVariables and RepeatedSourceVariables and rename columns
        """
        df_repeated_target_vars = pd.read_csv(self.path + 'RepeatedTargetVariables.csv', keep_default_na=False)
        df_repeated_source_vars = pd.read_csv(self.path + 'RepeatedSourceVariables.csv', keep_default_na=False)
        df_repeated_variables = pd.concat([df_repeated_source_vars, df_repeated_target_vars])
        df_repeated_variables.rename(columns={'dataDictionary.resource': 'resource',
                                              'table': 'dataset',
                                              'isRepeatOf.table': 'isRepeatOf.dataset'}, inplace=True)
        df_repeated_variables = float_to_int(df_repeated_variables)  # convert float back to integer
        df_repeated_variables.to_csv(self.path + 'RepeatedVariables.csv', index=False)

    def dataset_mappings(self):
        """Rename columns TableMappings
        """
        df_table_mappings = pd.read_csv(self.path + 'TableMappings.csv', keep_default_na=False)
        df_table_mappings.rename(columns={'fromDataDictionary.resource': 'source',
                                          'fromTable': 'sourceDataset',
                                          'toDataDictionary.resource': 'target',
                                          'toTable': 'targetDataset'}, inplace=True)
        df_table_mappings = float_to_int(df_table_mappings)  # convert float back to integer
        df_table_mappings.to_csv(self.path + 'DatasetMappings.csv', index=False)

    def variable_mappings(self):
        """Rename columns TableMappings
        """
        df_variable_mappings = pd.read_csv(self.path + 'VariableMappings.csv', keep_default_na=False)
        df_variable_mappings.rename(columns={'fromDataDictionary.resource': 'source',
                                             'fromTable': 'sourceDataset',
                                             'fromVariable': 'sourceVariables',
                                             'fromVariablesOtherTables.table': 'sourceVariablesOtherDatasets.dataset',
                                             'fromVariablesOtherTables.name': 'sourceVariablesOtherDatasets.name',
                                             'toDataDictionary.resource': 'target',
                                             'toTable': 'targetDataset',
                                             'toVariable': 'targetVariable'}, inplace=True)
        df_variable_mappings = float_to_int(df_variable_mappings)  # convert float back to integer

        df_variable_mappings.to_csv(self.path + 'VariableMappings.csv', index=False)

        # #Databanks > add to Data sources
    def datasources(self):
        """Add Databanks to Datasources and change column names
        """
        df_databanks = pd.read_csv(self.path + 'Databanks.csv')
        df_datasources = pd.read_csv(self.path + 'Datasources.csv')
        df_datasources_merged = pd.concat([df_databanks, df_datasources])
        df_datasources_merged = float_to_int(df_datasources_merged)
        df_datasources_merged.to_csv(self.path + 'Datasources.csv', index=False)

        #DatasourceDatabanks > LinkedDatasources
            #rename columns:
            #datasource > mainDatasource
            #databank > linkedDatasource
        #DAPs > ResourceOrganisations
            #isDataAccessProvider ontology filled from several data items in DAPs

    def zip_data(self):
        """Zip transformed data to upload.zip
        """
        shutil.make_archive(self.database + '_upload', 'zip', self.path)
