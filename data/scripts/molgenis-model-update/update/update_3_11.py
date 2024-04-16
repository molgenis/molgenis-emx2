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
    x_string = ''

    if not pd.isna(x):
        x_list = x.split(',')
        for item in x_list:
            if not item.startswith('http'):
                item = 'https://doi.org/' + item
            if item == 'https://doi.org/doi: 10.1002/nau.24996':
                item = 'https://doi.org/10.1002/nau.24996'
            x_string += item + ','
        x_string = x_string[:-1]

    return x_string


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
        if self.database_type == 'catalogue':
            self.cohorts()
            self.data_sources()
            self.databanks()
            self.publications()
        if self.database_type in ['cohort', 'cohort_UMCG']:
            self.cohorts()
            self.publications()
        if self.database_type == 'data_sources':
            self.data_sources()
            self.databanks()
            self.publications()

    def cohorts(self):
        """Transform columns in cohorts
        """
        df_cohorts = pd.read_csv(self.path + 'Cohorts.csv')
        df_cohorts['design paper'] = df_cohorts['design paper'].apply(get_hyperlink)
        if self.database_type == 'cohort':
            df_cohorts['publications'] = df_cohorts['publications'].apply(get_hyperlink)
        df_cohorts = float_to_int(df_cohorts)  # convert float back to integer
        df_cohorts.to_csv(self.path + 'Cohorts.csv', index=False)

    def data_sources(self):
        """Transform columns in data sources
        """
        df_data_sources = pd.read_csv(self.path + 'Data sources.csv')
        df_data_sources['design paper'] = df_data_sources['design paper'].apply(get_hyperlink)
        df_data_sources['publications'] = df_data_sources['publications'].apply(get_hyperlink)
        df_data_sources = float_to_int(df_data_sources)  # convert float back to integer
        df_data_sources.to_csv(self.path + 'Data sources.csv', index=False)

    def databanks(self):
        """Transform columns in databanks
        """
        df_databanks = pd.read_csv(self.path + 'Databanks.csv')
        df_databanks['design paper'] = df_databanks['design paper'].apply(get_hyperlink)
        df_databanks['publications'] = df_databanks['publications'].apply(get_hyperlink)
        df_databanks = float_to_int(df_databanks)  # convert float back to integer
        df_databanks.to_csv(self.path + 'Databanks.csv', index=False)

    def publications(self):
        """Transform columns in publications
        """
        df_publications = pd.read_csv(self.path + 'Publications.csv')
        df_publications['doi'] = df_publications['doi'].apply(get_hyperlink)
        df_publications = float_to_int(df_publications)  # convert float back to integer
        df_publications = df_publications.drop_duplicates(subset='doi')
        df_publications.to_csv(self.path + 'Publications.csv', index=False)
