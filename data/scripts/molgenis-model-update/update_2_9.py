import shutil
import os
import pandas as pd
import logging
import itertools


def float_to_int(df):
    """
    Cast float64 Series to Int64.
    """
    for column in df.columns:
        if df[column].dtype == 'float64':
            df.loc[:, column] = df[column].astype('Int64')

    return df


class TransformShared:
    def __init__(self, database):
        self.database = database
        self.path = './downloads/' + self.database + '_data/'
        self.logger = logging.getLogger(' data update and transform')

    def transform_gdpr(self):
        df_contacts = pd.read_csv(self.path + 'Contacts.csv')
        df_contacts['statementOfConsentPersonalData'] = False
        df_contacts['statementOfConsentEmail'] = False
        df_contacts.to_csv(self.path + 'Contacts.csv', index=False, mode='w+')


class TransformGeneral:
    """General functions to update catalogue data model.
    """

    def __init__(self, database, database_type):
        self.database = database
        self.database_type = database_type
        self.path = './downloads/' + self.database + '_data/'
        self.logger = logging.getLogger(' data update and transform')

    def delete_data_model_file(self):
        """Delete molgenis.csv
        """
        os.remove(self.path + 'molgenis.csv')

    def update_data_model_file(self):
        """Get path to data model file
        """
        # get molgenis.csv location
        if self.database_type == 'catalogue_2.9':
            data_model = os.path.abspath('datamodels/molgenis_2.9.csv')
        elif self.database_type == 'cohort_UMCG_2.9':
            data_model = os.path.abspath('datamodels/molgenis_stagingCohortsUMCG_2.9.csv')  #'../../datacatalogue3/stagingCohortsUMCG/molgenis.csv')
        elif self.database_type == 'SharedStagingUMCG':
            data_model = os.path.abspath('datamodels/molgenis_stagingSharedUMCG_2.9.csv')

        # copy molgenis.csv to appropriate folder
        shutil.copyfile(data_model, os.path.abspath(os.path.join(self.path, 'molgenis.csv')))
