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


def get_data_model(profile_path, path_to_write, profile):
    # get data model from profile and write to file
    data_model = pd.DataFrame()

    for file_name in os.listdir(profile_path):
        if '.csv' in file_name:
            df = pd.read_csv(profile_path + file_name)
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
            profile_path = os.path.abspath('../../../_models/shared/')
            profile = 'DataCatalogue'
            get_data_model(profile_path, data_model, profile)
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
        # transformations for catalogue and cohorts
        self.collections()
        self.subcohorts()
        self.collection_events()
        self.datasets()
        self.variables()
        self.repeated_variables()
        self.variable_values()
        self.dataset_mappings()
        self.variable_mappings()
        # remove selected tables
        os.remove(self.path + 'Network variables.csv')
        # os.remove(self.path + 'Models.csv')

    def collections(self):
        """Transform columns in Cohorts, Networks, Studies, Data sources, Databanks
        """
        # Cohorts to Collections
        df_cohorts = pd.read_csv(self.path + 'Cohorts.csv')
        df_cohorts.rename(columns={'type': 'cohort type',
                                   'type other': 'cohort type other',
                                   'collection type': 'cohort collection type'}, inplace=True)
        df_cohorts['collection type'] = 'Cohort'

        # Networks to Collections
        df_networks = pd.read_csv(self.path + 'Networks.csv')
        df_networks.rename(columns={'type': 'network type'}, inplace=True)
        df_networks['collection type'] = 'Network'
        df_networks['models'] = ''

        # Studies to Collections
        df_studies = pd.read_csv(self.path + 'Studies.csv')
        df_studies.rename(columns={'type': 'study type',
                                   'type other': 'study type other'}, inplace=True)
        df_studies['collection type'] = 'Study'

        # Data sources to Collections
        df_data_sources = pd.read_csv(self.path + 'Data sources.csv')
        df_data_sources.rename(columns={'type': 'datasource type',
                                        'type other': 'datasource type other'}, inplace=True)
        df_data_sources['collection type'] = 'Data source'

        # Databanks to Collections
        df_databanks = pd.read_csv(self.path + 'Databanks.csv')
        df_databanks.rename(columns={'type': 'datasource type',
                                     'type other': 'datasource type other'}, inplace=True)
        df_databanks['collection type'] = 'Databank'

        # handles exception CRC Screening CDM
        df_models = pd.read_csv(self.path + 'Models.csv', keep_default_na=False)
        df_models = df_models[df_models['id'] == 'CRC Screening CDM']
        df_models['collection type'] = ''  # TODO: add term here

        df_models = float_to_int(df_models)  # convert float back to integer

        # concatenate all to Collections
        df_collections = pd.concat([df_cohorts, df_networks, df_studies, df_databanks,
                                    df_data_sources, df_models])

        df_collections = float_to_int(df_collections)  # convert float back to integer
        df_collections.to_csv(self.path + 'Collections.csv', index=False)

    def subcohorts(self):
        df = pd.read_csv(self.path + 'Subcohorts.csv')
        df.loc[:, 'resource'] = df['resource'].apply(strip_resource)

        df = float_to_int(df)  # convert float back to integer
        df.to_csv(self.path + 'Subcohorts.csv', index=False)

    def collection_events(self):
        df = pd.read_csv(self.path + 'Collection events.csv')
        df.loc[:, 'resource'] = df['resource'].apply(strip_resource)
        df.loc[:, 'subcohorts'] = df['subcohorts'].apply(strip_resource)

        df = float_to_int(df)  # convert float back to integer
        df.to_csv(self.path + 'Collection events.csv', index=False)

    def datasets(self):
        df = pd.read_csv(self.path + 'Datasets.csv', keep_default_na=False)
        df.loc[:, 'resource'] = df['resource'].apply(strip_resource)

        df = float_to_int(df)  # convert float back to integer
        df.to_csv(self.path + 'Datasets.csv', index=False)

    def variables(self):
        df = pd.read_csv(self.path + 'Variables.csv', keep_default_na=False)
        df.loc[:, 'resource'] = df['resource'].apply(strip_resource)
        df.loc[:, 'collection event.resource'] = df['collection event.resource'].apply(strip_resource)

        df = float_to_int(df)  # convert float back to integer
        df.to_csv(self.path + 'Variables.csv', index=False)

    def repeated_variables(self):
        df = pd.read_csv(self.path + 'Repeated variables.csv', keep_default_na=False)
        df.loc[:, 'resource'] = df['resource'].apply(strip_resource)
        df.loc[:, 'collection event.resource'] = df['collection event.resource'].apply(strip_resource)

        df = float_to_int(df)  # convert float back to integer
        df.to_csv(self.path + 'Repeated variables.csv', index=False)

    def variable_values(self):
        df = pd.read_csv(self.path + 'Variable values.csv', keep_default_na=False)
        df.loc[:, 'resource'] = df['resource'].apply(strip_resource)

        df = float_to_int(df)  # convert float back to integer
        df.to_csv(self.path + 'Variable values.csv', index=False)

    def dataset_mappings(self):
        df = pd.read_csv(self.path + 'Dataset mappings.csv', keep_default_na=False)
        df.loc[:, 'target'] = df['target'].apply(strip_resource)

        df = float_to_int(df)  # convert float back to integer
        df.to_csv(self.path + 'Dataset mappings.csv', index=False)

    def variable_mappings(self):
        df = pd.read_csv(self.path + 'Variable mappings.csv', keep_default_na=False)
        df.loc[:, 'target'] = df['target'].apply(strip_resource)

        df = float_to_int(df)  # convert float back to integer
        df.to_csv(self.path + 'Variable mappings.csv', index=False)


def strip_resource(resource_name):
    if not pd.isna(resource_name):
        if '_CDM' in resource_name:  # TODO: handle exception CRC screening CDM under EOSC4Cancer
            resource_name = resource_name[:-4]

    return resource_name
