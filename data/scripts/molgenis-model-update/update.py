import shutil
import os
import pandas as pd
import logging


def float_to_int(df):
    """
    Cast float64 Series to Int64.
    """
    for column in df.columns:
        if df[column].dtype == 'float64':
            df.loc[:, column] = df[column].astype('Int64')

    return df


class TransformGeneral:
    """General functions to update catalogue data model.
    """

    def __init__(self, database, database_type):
        self.database = database
        self.database_type = database_type
        self.path = './' + self.database + '_data/'
        self.logger = logging.getLogger(' data update and transform')

    def delete_data_model_file(self):
        """Delete molgenis.csv
        """
        os.remove(self.path + 'molgenis.csv')

    def data_model_file(self):
        """Get path to data model file
        """
        # copy updated molgenis.csv to os.listdir(self.path)
        if self.database_type == 'catalogue':
            template = os.path.abspath('../../datacatalogue3/molgenis.csv')
        elif self.database_type == 'network':
            template = os.path.abspath('../../datacatalogue3/stagingNetworks/molgenis.csv')
        elif self.database_type == 'cohort':
            template = os.path.abspath('../../datacatalogue3/stagingCohorts/molgenis.csv')
        elif self.database_type == 'cohort_UMCG':
            template = os.path.abspath('../../datacatalogue3/stagingCohortsUMCG/molgenis.csv')

        return template


class TransformDataCatalogue:
    """Functions to update catalogue data model from 2.8 to 3.0.
    """

    def __init__(self, database, database_type):
        self.database = database
        self.path = './' + self.database + '_data/'
        self.shared_staging = 'SharedStaging'
        self.path_shared_staging = self.shared_staging + '_data/'
        self.database_type = database_type
        self.logger = logging.getLogger(' data update and transform')

    def transform_data(self):
        """Make changes per table
        """
        if self.database_type == 'catalogue':
            self.contacts()
            self.identifiers()
            self.tables()
            self.variables()
            self.variable_values()
            self.repeated_variables()
            self.dataset_mappings()
            self.variable_mappings()
            self.datasources()
        if self.database_type == 'UMCG':
            self.contacts()

    def contacts(self):
        """Merge Contributions & Contacts on firstName and surname and rename columns
        """
        df_contributions = pd.read_csv(self.path + 'Contributions.csv')
        df_contributions.rename(columns={'contributionType': 'role',
                                         'contact.firstName': 'firstName',
                                         'contact.surname': 'surname'}, inplace=True)

        if self.database_type == 'catalogue':
            df_contacts = pd.read_csv(self.path + 'Contacts.csv')
        if self.database_type == 'UMCG':
            df_contacts = pd.read_csv(self.path_shared_staging + 'Contacts.csv')

        df_contacts_merged = pd.merge(df_contributions, df_contacts, on=['firstName', 'surname'])
        df_contacts_merged.rename(columns={'surname': 'lastName'}, inplace=True)
        df_contacts_merged = float_to_int(df_contacts_merged)  # convert float back to integer
        df_contacts_merged.to_csv(self.path + 'Contacts.csv', index=False)

    def identifiers(self):
        """Move external identifiers to separate table
        """
        # get numbers from entry
        # move to External identifiers.identifier
        # External identifiers.resource = cohort[5:]

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


class TransformDataStagingCohorts:
    """Functions to update catalogue data model for cohort staging areas from 2.8 to 3.0.
    """

    def __init__(self, database, database_type):
        self.cohort_name = database
        self.path = './' + self.cohort_name + '_data/'
        self.shared_staging = 'SharedStaging'
        self.path_shared_staging = self.shared_staging + '_data/'
        self.database_type = database_type
        self.logger = logging.getLogger(' data update and transform')

    def transform_data(self):
        """Make changes per table
        """
        if self.database_type == 'cohort':
            self.contacts()
            self.tables()
            self.variables()
            self.variable_values()
            self.repeated_variables()
            self.dataset_mappings()
            self.variable_mappings()
        if self.database_type == 'cohort_UMCG':
            self.contacts()

    def contacts(self):
        """Merge Contributions & Contacts on firstName and surname and rename columns
        Only keep resource == cohort_name
        """
        df_contributions = pd.read_csv(self.path + 'Contributions.csv')
        df_contributions.rename(columns={'contributionType': 'role',
                                         'contact.firstName': 'firstName',
                                         'contact.surname': 'surname'}, inplace=True)
        df_contacts = pd.read_csv(self.path_shared_staging + 'Contacts.csv')
        df_contacts_merged = pd.merge(df_contributions, df_contacts, on=['firstName', 'surname'])
        df_contacts_merged.rename(columns={'surname': 'lastName'}, inplace=True)
        df_contacts_merged = float_to_int(df_contacts_merged)  # convert float back to integer
        df_contacts_merged.to_csv(self.path + 'Contacts.csv', index=False)

    def tables(self):
        """Rename SourceTables and rename columns
        """
        df_datasets = pd.read_csv(self.path + 'SourceTables.csv')
        df_datasets.rename(columns={'dataDictionary.resource': 'resource'}, inplace=True)
        df_datasets = float_to_int(df_datasets)  # convert float back to integer
        df_datasets.to_csv(self.path + 'Datasets.csv', index=False)

    def variables(self):
        """Rename SourceVariables and rename columns
        """
        df_variables = pd.read_csv(self.path + 'SourceVariables.csv', keep_default_na=False)
        df_variables = float_to_int(df_variables)  # convert float back to integer
        df_variables.rename(columns={'dataDictionary.resource': 'resource',
                                     'table': 'dataset'}, inplace=True)
        df_variables.to_csv(self.path + 'Variables.csv', index=False)

    def variable_values(self):
        """Rename SourceVariableValues and rename columns
        """
        df_variable_values = pd.read_csv(self.path + 'SourceVariableValues.csv', keep_default_na=False)
        df_variable_values.rename(columns={'dataDictionary.resource': 'resource',
                                           'variable.table': 'variable.dataset',
                                           'ontologyTermIRI': 'ontologyTermURI'}, inplace=True)
        df_variable_values = float_to_int(df_variable_values)  # convert float back to integer
        df_variable_values.to_csv(self.path + 'VariableValues.csv', index=False)

    def repeated_variables(self):
        """Rename RepeatedSourceVariables and rename columns
        """
        df_repeated_variables = pd.read_csv(self.path + 'RepeatedSourceVariables.csv', keep_default_na=False)
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

