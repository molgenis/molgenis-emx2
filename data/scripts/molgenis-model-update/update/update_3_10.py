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
        self.path = self.database_name + '_data/'

    def delete_data_model_file(self):
        """Delete molgenis.csv
        """
        os.remove(self.path + 'molgenis.csv')

    def update_data_model_file(self):
        """Get path to data model file
        """
        # get molgenis.csv location
        if self.database_type == 'catalogue':
            data_model = os.path.abspath('../../../datacatalogue/molgenis.csv')
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
            path = './files/' + 'catalogue_data_model'
            if not os.path.isdir(path):
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
        self.variables()
        self.rep_variables()

    def variables(self):
        """Rename columns in variables
        """
        df_variables = pd.read_csv(self.path + 'Variables.csv')
        df_variables.rename(columns={'collection event.name': 'collection event'}, inplace=True)
        df_variables = float_to_int(df_variables)  # convert float back to integer
        df_variables.to_csv(self.path + 'Variables.csv', index=False)

    def rep_variables(self):
        """Rename columns in repeated variables
        """
        df_rep_variables = pd.read_csv(self.path + 'Repeated variables.csv')
        df_rep_variables.rename(columns={'collection event.name': 'collection event'}, inplace=True)
        df_rep_variables = float_to_int(df_rep_variables)  # convert float back to integer
        df_rep_variables.to_csv(self.path + 'Repeated variables.csv', index=False)
