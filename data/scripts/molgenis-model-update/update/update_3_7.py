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
        if self.database_type == 'catalogue':
            data_model = os.path.abspath('../../../data/datacatalogue/molgenis.csv')
        elif self.database_type == 'network':
            data_model = os.path.abspath('../../../data/datacatalogue/stagingNetworks/molgenis.csv')
        elif self.database_type == 'cohort':
            data_model = os.path.abspath('../../../data/datacatalogue/stagingCohorts/molgenis.csv')
        elif self.database_type == 'data_source':
            data_model = os.path.abspath('../../../data/datacatalogue/stagingRWE/molgenis.csv')
        elif self.database_type == 'cohort_UMCG':
            data_model = os.path.abspath('../../../data/datacatalogue/stagingCohortsUMCG/molgenis.csv')
        elif self.database_type == 'shared':
            data_model = os.path.abspath('../../../data/datacatalogue/stagingShared/molgenis.csv')

        # copy molgenis.csv to appropriate folder
        if self.database_type == 'catalogue':
            path = './files/' + 'catalogue_data_model'
            os.mkdir(path)
            shutil.copyfile(data_model, os.path.abspath(os.path.join(path, 'molgenis.csv')))
            shutil.make_archive('./files/' + 'catalogue_data_model_upload', 'zip', path)
        else:
            shutil.copyfile(data_model, os.path.abspath(os.path.join(self.path, 'molgenis.csv')))


class TransformDataCatalogue(Transform):
    """Functions to update catalogue data from data model version 3.3 to 3.7.
    """

    def __init__(self, database_name, database_type):
        Transform.__init__(self, database_name, database_type)

    def transform_data(self):
        """Make changes per table
        """
        # transformations for catalogue and cohorts
        self.cohorts()
        self.resource_organisations()

    def cohorts(self):
        """Rename columns in cohorts
        """
        df_cohorts = pd.read_csv(self.path + 'Cohorts.csv')
        df_cohorts.rename(columns={'inclusion criteria': 'inclusion criteria other'}, inplace=True)
        df_cohorts = float_to_int(df_cohorts)  # convert float back to integer
        df_cohorts.to_csv(self.path + 'Cohorts.csv', index=False)

    def resource_organisations(self):
        """Rename table
        """
        df_resource_organisations = pd.read_csv(self.path + 'Resource organisations.csv')
        df_resource_organisations = float_to_int(df_resource_organisations)  # convert float back to integer
        df_resource_organisations.to_csv(self.path + 'DAPs.csv', index=False)


class TransformDataStaging(Transform):
    """Functions to update catalogue data model for cohort staging areas from 3.3 to 3.7.
    """

    def __init__(self, database_name, database_type):
        Transform.__init__(self, database_name, database_type)

    def transform_data(self):
        """Make changes per table
        """
        # transformations for staging cohorts UMCG and data catalogue staging cohorts
        if self.database_type in ['cohort', 'cohort_UMCG']:
            self.cohorts()

    def cohort(self):
        """Rename columns in cohorts
        """
        df_cohorts = pd.read_csv(self.path + 'Cohorts.csv')
        df_cohorts.rename(columns={'inclusion criteria': 'inclusion criteria other'}, inplace=True)
        df_cohorts = float_to_int(df_cohorts)  # convert float back to integer
        df_cohorts.to_csv(self.path + 'Cohorts.csv', index=False)
