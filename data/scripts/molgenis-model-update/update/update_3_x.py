import shutil
import os
import pandas as pd


def float_to_int(df):
    """
    Cast float64 Series to Int64.
    """
    for column in df.columns:
        if df[column].dtype == 'float64':
            df.loc[:, column] = df[column].astype('Int64')

    return df


def get_hyperlink(x):
    """
    Return hyperlink for websites if not filled out correctly.
    """
    if not pd.isna(x):
        if not x.startswith('http'):
            x = 'https://' + x

    return x


class Transform:
    """General functions to update catalogue data model.
    """

    def __init__(self, database_name, database_type):
        self.database_name = database_name
        self.database_type = database_type
        self.path = './files/' + self.database_name + '_data/'

    def delete_data_model_file(self):
        """Delete molgenis.csv
        """
        os.remove(self.path + 'molgenis.csv')

    def update_data_model_file(self):
        """Get path to data model file
        """
        # get molgenis.csv location
        if self.database_type in ['catalogue_staging', 'catalogue']:
            data_model = os.path.abspath('../../../data/datacatalogue/molgenis.csv')
        elif self.database_type == 'network':
            data_model = os.path.abspath('../../../data/datacatalogue/stagingNetworks/molgenis.csv')
        elif self.database_type == 'cohort':
            data_model = os.path.abspath('../../../data/datacatalogue/stagingCohorts/molgenis.csv')
        elif self.database_type == 'cohort_UMCG':
            data_model = os.path.abspath('../../../data/datacatalogue/stagingCohortsUMCG/molgenis.csv')
        elif self.database_type == 'shared':
            data_model = os.path.abspath('../../../data/datacatalogue/stagingShared/molgenis.csv')

        # copy molgenis.csv to appropriate folder
        if self.database_type in ['catalogue_staging', 'catalogue']:
            path = './files/' + 'catalogue_data_model'
            os.mkdir(path)
            shutil.copyfile(data_model, os.path.abspath(os.path.join(path, 'molgenis.csv')))
            shutil.make_archive('./files/' + 'catalogue_data_model_upload', 'zip', path)
        else:
            shutil.copyfile(data_model, os.path.abspath(os.path.join(self.path, 'molgenis.csv')))


class TransformShared(Transform):
    def __init__(self, database_name, database_type):
        Transform.__init__(self, database_name, database_type)

    def organisations(self):
        """Rename Institutions and rename columns
        """
        df_organisations = pd.read_csv(self.path + 'Institutions.csv')
        df_organisations['website'] = df_organisations['website'].apply(get_hyperlink)
        df_organisations.rename(columns={'pid': 'id',
                                         'roles': 'role'}, inplace=True)
        df_organisations["name"].fillna(inplace=True, value=df_organisations["id"])  # fill na in column 'name'
        df_organisations = float_to_int(df_organisations)  # convert float back to integer
        df_organisations.to_csv(self.path + 'Organisations.csv', index=False)


class TransformDataCatalogue(Transform):
    """Functions to update catalogue data model from 2.8 to 3.0.
    """

    def __init__(self, database_name, database_type):
        Transform.__init__(self, database_name, database_type)
        self.shared_staging = 'SharedStaging'
        self.path_shared_staging = './files/' + self.shared_staging + '_data/'

    def transform_data(self):
        """Make changes per table
        """
        # transformations for UMCG and data catalogue
        self.cohorts()
        self.contacts()
        self.organisations()
        self.copy_files()
        self.identifiers()

        # transformations for data catalogue
        if self.database_type in ['catalogue', 'catalogue_staging']:
            self.tables()
            self.variables()
            self.variable_values()
            self.repeated_variables()
            self.dataset_mappings()
            self.variable_mappings()
            self.copy_files()

    def cohorts(self):
        """Rename columns in cohorts
        """
        df_cohorts = pd.read_csv(self.path + 'Cohorts.csv')

        # add partners to column 'additional organisations'
        df_partners = pd.read_csv(self.path + 'Partners.csv')
        df_partners.rename(columns={'institution': 'additional organisations'}, inplace=True)
        grouped_partners = df_partners.groupby('resource')['additional organisations'].agg(','.join)
        df_cohorts['resource'] = df_cohorts['pid']
        df_cohorts_merged = pd.merge(df_cohorts, grouped_partners, on='resource', how='outer')
        df_cohorts_merged['website'] = df_cohorts_merged['website'].apply(get_hyperlink)
        df_cohorts_merged.rename(columns={'pid': 'id',
                                          'institution': 'lead organisation'}, inplace=True)
        df_cohorts_merged = float_to_int(df_cohorts_merged)  # convert float back to integer
        df_cohorts_merged.to_csv(self.path + 'Cohorts.csv', index=False)

    def contacts(self):
        """Merge Contributions & Contacts on firstName and surname and rename columns
        """
        df_contributions = pd.read_csv(self.path + 'Contributions.csv')
        df_contributions.rename(columns={'contributionType': 'role',
                                         'contact.firstName': 'firstName',
                                         'contact.surname': 'surname',
                                         'institution': 'organisation'}, inplace=True)
        if self.database_type == 'catalogue':
            df_contacts = pd.read_csv(self.path + 'Contacts.csv')
        if self.database_type == 'catalogue_staging':
            df_contacts = pd.read_csv(self.path_shared_staging + 'Contacts.csv')
        if self.database_type == 'UMCG':
            df_contacts = pd.read_csv(self.path_shared_staging + 'Contacts.csv')

        df_contacts_merged = pd.merge(df_contributions, df_contacts, on=['firstName', 'surname'])
        df_contacts_merged.rename(columns={'surname': 'lastName'}, inplace=True)
        df_contacts_merged = float_to_int(df_contacts_merged)  # convert float back to integer
        df_contacts_merged.to_csv(self.path + 'Contacts.csv', index=False)

    def organisations(self):
        """Move Institutions to Organisations
        """
        df_organisations = pd.read_csv(self.path_shared_staging + 'Institutions.csv')
        df_organisations['website'] = df_organisations['website'].apply(get_hyperlink)
        df_organisations.rename(columns={'pid': 'id',
                                         'roles': 'role',
                                         'providerOf': 'leading resources',
                                         'partnerIn': 'additional resources'}, inplace=True)
        df_organisations["name"].fillna(inplace=True, value=df_organisations["id"])  # fill na in column 'name'
        df_organisations.to_csv(self.path + 'Organisations.csv', index=False)

    def identifiers(self):
        """Move external identifiers to a csv file
        """
        # get numbers from entry
        df_identifiers = pd.read_csv(self.path + 'Cohorts.csv')
        df_identifiers = df_identifiers[['id', 'externalIdentifiers']]
        df_identifiers.to_csv('External identifiers.csv', index=False)

    def tables(self):
        """Merge TargetTables and SourceTables and rename columns
        """
        df_target_tables = pd.read_csv(self.path + 'TargetTables.csv')
        df_source_tables = pd.read_csv(self.path + 'SourceTables.csv')
        df_datasets = pd.concat([df_source_tables, df_target_tables], ignore_index=True)
        df_datasets.drop(df_datasets[(df_datasets['dataDictionary.resource'] == 'IPEC_CDM') &
                                     (df_datasets['dataDictionary.version'] == '1.0.0')].index, inplace=True)
        df_datasets.rename(columns={'dataDictionary.resource': 'resource',
                                    'dataDictionary.version': 'since version'}, inplace=True)
        df_datasets.loc[df_datasets['resource'] == 'IPEC_CDM', 'since version'] = '1.0.0'
        df_datasets = float_to_int(df_datasets)  # convert float back to integer
        df_datasets.to_csv(self.path + 'Datasets.csv', index=False)

    def variables(self):
        """Merge TargetVariables and SourceVariables and rename columns
        """
        df_target_vars = pd.read_csv(self.path + 'TargetVariables.csv', keep_default_na=False)
        df_source_vars = pd.read_csv(self.path + 'SourceVariables.csv', keep_default_na=False)
        df_variables = pd.concat([df_source_vars, df_target_vars], ignore_index=True)
        df_variables = float_to_int(df_variables)  # convert float back to integer
        df_variables.drop(df_variables[(df_variables['dataDictionary.resource'] == 'IPEC_CDM') &
                                       (df_variables['dataDictionary.version'] == '1.0.0')].index, inplace=True)
        df_variables.rename(columns={'dataDictionary.resource': 'resource',
                                     'table': 'dataset',
                                     'dataDictionary.version': 'since version'}, inplace=True)
        df_variables.loc[df_variables['resource'] == 'IPEC_CDM', 'since version'] = '1.0.0'
        df_variables.to_csv(self.path + 'Variables.csv', index=False)

    def variable_values(self):
        """Merge TargetVariableValues and SourceVariableValues and rename columns
        """
        df_target_vars_values = pd.read_csv(self.path + 'TargetVariableValues.csv', keep_default_na=False)
        df_source_vars_values = pd.read_csv(self.path + 'SourceVariableValues.csv', keep_default_na=False)
        df_variable_values = pd.concat([df_source_vars_values, df_target_vars_values], ignore_index=True)
        df_variable_values.drop(df_variable_values[(df_variable_values['dataDictionary.resource'] == 'IPEC_CDM') &
                                                   (df_variable_values['dataDictionary.version'] == '1.0.0')].index,
                                inplace=True)
        df_variable_values.rename(columns={'dataDictionary.resource': 'resource',
                                           'dataDictionary.version': 'since version',
                                           'variable.table': 'variable.dataset',
                                           'ontologyTermIRI': 'ontologyTermURI'}, inplace=True)
        df_variable_values = float_to_int(df_variable_values)  # convert float back to integer
        df_variable_values.loc[df_variable_values['resource'] == 'IPEC_CDM', 'since version'] = '1.0.0'
        df_variable_values.to_csv(self.path + 'VariableValues.csv', index=False)

    def repeated_variables(self):
        """Merge RepeatedTargetVariables and RepeatedSourceVariables and rename columns
        """
        df_repeated_target_vars = pd.read_csv(self.path + 'RepeatedTargetVariables.csv', keep_default_na=False)
        df_repeated_source_vars = pd.read_csv(self.path + 'RepeatedSourceVariables.csv', keep_default_na=False)
        df_repeated_variables = pd.concat([df_repeated_source_vars, df_repeated_target_vars], ignore_index=True)
        df_repeated_variables.rename(columns={'dataDictionary.resource': 'resource',
                                              'dataDictionary.version': 'since version',
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
        df_table_mappings.drop(df_table_mappings[(df_table_mappings['target'] == 'IPEC_CDM') &
                                                 df_table_mappings['toDataDictionary.version'] == '1.0.0'].index,
                               inplace=True)
        df_table_mappings = float_to_int(df_table_mappings)  # convert float back to integer
        df_table_mappings.to_csv(self.path + 'DatasetMappings.csv', index=False)

    def variable_mappings(self):
        """Rename columns VariableMappings
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
        df_variable_mappings.drop(df_variable_mappings[(df_variable_mappings['target'] == 'IPEC_CDM') &
                                                       (df_variable_mappings['toDataDictionary.version'] == '1.0.0')].index, inplace=True)
        df_variable_mappings = float_to_int(df_variable_mappings)  # convert float back to integer
        df_variable_mappings.to_csv(self.path + 'VariableMappings.csv', index=False)

    def copy_files(self):
        """Copy files from SharedStaging to catalogue
        """
        path_shared_staging_files = os.path.abspath('./files/SharedStaging_data/_files/')
        path_catalogue_files = os.path.abspath(os.path.join(self.path, '_files/'))
        for file in os.listdir(path_shared_staging_files):
            shutil.copyfile(os.path.join(path_shared_staging_files, file),
                            os.path.join(path_catalogue_files, file))


class TransformDataStaging(Transform):
    """Functions to update catalogue data model for cohort staging areas from 2.8 to 3.0.
    """

    def __init__(self, database_name, database_type):
        Transform.__init__(self, database_name, database_type)
        self.shared_staging = 'SharedStaging'
        self.path_shared_staging = './files/' + self.shared_staging + '_data/'

    def transform_data(self):
        """Make changes per table
        """
        # transformations for staging UMCG and data catalogue staging cohorts
        if self.database_type in ['cohort', 'cohort_UMCG']:
            self.cohorts()
            self.contacts()
            self.organisations()
            self.identifiers()
            self.copy_files()

        # transformations for data catalogue staging networks
        if self.database_type == 'network':
            self.networks()
            self.models()

        # transformations for data catalogue staging
        if self.database_type in ['cohort', 'network']:
            self.tables()
            self.variables()
            self.variable_values()
            self.repeated_variables()

        # transformations for data catalogue staging cohorts
        if self.database_type in ['cohort']:
            self.dataset_mappings()
            self.variable_mappings()

    def cohorts(self):
        """Rename column in cohorts
        """
        df_cohorts = pd.read_csv(self.path + 'Cohorts.csv')
        df_cohorts = float_to_int(df_cohorts)  # convert float back to integer

        # add partners to column 'additional organisations'
        df_partners = pd.read_csv(self.path + 'Partners.csv')
        list_partners = df_partners['institution'].unique().tolist()
        string_partners = ','.join(list_partners)
        df_cohorts['additional organisations'] = string_partners

        df_cohorts.rename(columns={'pid': 'id',
                                   'institution': 'lead organisation'}, inplace=True)
        df_cohorts['website'] = df_cohorts['website'].apply(get_hyperlink)
        if self.database_type == 'cohort_UMCG':
            new_mg_tableclass = df_cohorts.mg_tableclass[0][5:]
            df_cohorts.mg_tableclass = new_mg_tableclass
        df_cohorts.to_csv(self.path + 'Cohorts.csv', index=False, mode='w+')

    def contacts(self):
        """Merge Contributions & Contacts on firstName and surname and rename columns
        Only keep resource == database_name
        """
        df_contributions = pd.read_csv(self.path + 'Contributions.csv')
        df_contributions.rename(columns={'contributionType': 'role',
                                         'contact.firstName': 'firstName',
                                         'contact.surname': 'surname'}, inplace=True)
        df_contacts = pd.read_csv(self.path_shared_staging + 'Contacts.csv')
        df_contacts_merged = pd.merge(df_contributions, df_contacts, on=['firstName', 'surname'])
        df_contacts_merged.rename(columns={'surname': 'lastName',
                                           'institution': 'organisation'}, inplace=True)
        df_contacts_merged = float_to_int(df_contacts_merged)  # convert float back to integer
        df_contacts_merged.to_csv(self.path + 'Contacts.csv', index=False)

    def organisations(self):
        """Move Institutions to Organisations
        """
        df_organisations = pd.read_csv(self.path_shared_staging + 'Institutions.csv')

        # only keep institutions that are referred to from other tables
        df_contacts = pd.read_csv(self.path + 'Contacts.csv')
        institutions_from_contacts = df_contacts['organisation'].tolist()
        df_cohorts = pd.read_csv(self.path + 'Cohorts.csv')
        if not pd.isna:
            institutions_from_cohorts = df_cohorts['lead organisation'][0].split(",")
        else:
            institutions_from_cohorts = []
        df_partners = pd.read_csv(self.path + 'Partners.csv')
        institutions_from_partners = df_partners['institution'].tolist()
        combined_institutions = institutions_from_partners + institutions_from_contacts + institutions_from_cohorts
        df_organisations = df_organisations.query('pid in @combined_institutions')
        df_organisations['website'] = df_organisations['website'].apply(get_hyperlink)
        df_organisations.rename(columns={'pid': 'id',
                                         'institution': 'organisation',
                                         'roles': 'role'}, inplace=True)
        df_organisations["name"].fillna(inplace=True, value=df_organisations["id"])  # fill na in column 'name'
        df_organisations.to_csv(self.path + 'Organisations.csv', index=False)

    def identifiers(self):
        """Move external identifiers to separate table
        """
        # get numbers from entry
        df_identifiers = pd.read_csv(self.path + 'Cohorts.csv')
        df_identifiers = df_identifiers[['id', 'externalIdentifiers']]
        df_identifiers.to_csv('External identifiers.csv', mode='a', index=False, header=False)

    def networks(self):
        """ Rename column
        """
        df_networks = pd.read_csv(self.path + 'Networks.csv')
        df_networks.rename(columns={'pid': 'id'}, inplace=True)
        df_networks.to_csv(self.path + 'Networks.csv', index=False, mode='w+')

    def models(self):
        """ Rename column
        """
        df_models = pd.read_csv(self.path + 'Models.csv')
        df_models.rename(columns={'pid': 'id'}, inplace=True)
        df_models.to_csv(self.path + 'Models.csv', index=False, mode='w+')

    def tables(self):
        """Rename SourceTables and TargetTables and rename columns
        """
        if self.database_type == 'cohort':
            df_datasets = pd.read_csv(self.path + 'SourceTables.csv')
        elif self.database_type == 'network':
            df_datasets = pd.read_csv(self.path + 'TargetTables.csv')
        df_datasets.drop(df_datasets[(df_datasets['dataDictionary.resource'] == 'IPEC_CDM') &
                                     (df_datasets['dataDictionary.version'] == '1.0.0')].index, inplace=True)
        df_datasets.rename(columns={'dataDictionary.resource': 'resource',
                                    'dataDictionary.version': 'since version'}, inplace=True)
        df_datasets.loc[df_datasets['resource'] == 'IPEC_CDM', 'since version'] = '1.0.0'
        df_datasets = float_to_int(df_datasets)  # convert float back to integer
        df_datasets.to_csv(self.path + 'Datasets.csv', index=False)

    def variables(self):
        """Rename SourceVariables and TargetVariables and rename columns
        """
        if self.database_type == 'cohort':
            df_variables = pd.read_csv(self.path + 'SourceVariables.csv', keep_default_na=False)
        elif self.database_type == 'network':
            df_variables = pd.read_csv(self.path + 'TargetVariables.csv', keep_default_na=False)
        df_variables.drop(df_variables[(df_variables['dataDictionary.resource'] == 'IPEC_CDM') &
                                       (df_variables['dataDictionary.version'] == '1.0.0')].index, inplace=True)
        df_variables.rename(columns={'dataDictionary.resource': 'resource',
                                     'dataDictionary.version': 'since version',
                                     'table': 'dataset',}, inplace=True)
        df_variables.loc[df_variables['resource'] == 'IPEC_CDM', 'since version'] = '1.0.0'
        df_variables = float_to_int(df_variables)  # convert float back to integer
        df_variables.to_csv(self.path + 'Variables.csv', index=False)

    def variable_values(self):
        """Rename SourceVariableValues and TargetVariableValues and rename columns
        """
        if self.database_type == 'cohort':
            df_variable_values = pd.read_csv(self.path + 'SourceVariableValues.csv', keep_default_na=False)
        elif self.database_type == 'network':
            df_variable_values = pd.read_csv(self.path + 'TargetVariableValues.csv', keep_default_na=False)
        df_variable_values.drop(df_variable_values[(df_variable_values['dataDictionary.resource'] == 'IPEC_CDM') &
                                                   (df_variable_values['dataDictionary.version'] == '1.0.0')].index,
                                inplace=True)
        df_variable_values.rename(columns={'dataDictionary.resource': 'resource',
                                           'dataDictionary.version': 'since version',
                                           'variable.table': 'variable.dataset',
                                           'ontologyTermIRI': 'ontologyTermURI'}, inplace=True)
        df_variable_values.loc[df_variable_values['resource'] == 'IPEC_CDM', 'since version'] = '1.0.0'
        df_variable_values = float_to_int(df_variable_values)  # convert float back to integer
        df_variable_values.to_csv(self.path + 'VariableValues.csv', index=False)

    def repeated_variables(self):
        """Rename RepeatedSourceVariables and RepeatedTargetVariables and rename columns
        """
        if self.database_type == 'cohort':
            df_repeated_variables = pd.read_csv(self.path + 'RepeatedSourceVariables.csv', keep_default_na=False)
        elif self.database_type == 'network':
            df_repeated_variables = pd.read_csv(self.path + 'RepeatedTargetVariables.csv', keep_default_na=False)

        df_repeated_variables.rename(columns={'dataDictionary.resource': 'resource',
                                              'dataDictionary.version': 'since version',
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
        df_table_mappings.drop(df_table_mappings[(df_table_mappings['target'] == 'IPEC_CDM') &
                                                 (df_table_mappings['toDataDictionary.version'] == '1.0.0')].index,
                               inplace=True)
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
        df_variable_mappings.drop(df_variable_mappings[(df_variable_mappings['target'] == 'IPEC_CDM') &
                                                       (df_variable_mappings['toDataDictionary.version'] == '1.0.0')]
                                  .index, inplace=True)
        df_variable_mappings = float_to_int(df_variable_mappings)  # convert float back to integer

        df_variable_mappings.to_csv(self.path + 'VariableMappings.csv', index=False)

    def copy_files(self):
        """Copy files from SharedStaging to cohort staging data
        """
        path_shared_staging_files = os.path.abspath('./files/SharedStaging_data/_files')
        path_cohort_files = os.path.abspath(os.path.join(self.path, '_files'))
        if not os.path.exists(path_cohort_files):
            os.mkdir(path_cohort_files)

        for item in os.listdir(path_shared_staging_files):
            if os.path.isfile(os.path.join(path_shared_staging_files, item)):
                shutil.copyfile(os.path.join(path_shared_staging_files, item),
                                os.path.join(path_cohort_files, item))
