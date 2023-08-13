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
        self.path = './files/' + self.database + '_data/'
        self.logger = logging.getLogger(' data update and transform')

    # def transform_gdpr(self):
    #     df_contacts = pd.read_csv(self.path + 'Contacts.csv')
    #     df_contacts['statementOfConsentPersonalData'] = False
    #     df_contacts['statementOfConsentEmail'] = False
    #     df_contacts.to_csv(self.path + 'Contacts.csv', index=False, mode='w+')


class TransformGeneral:
    """General functions to update catalogue data model.
    """

    def __init__(self, database):
        self.database = database
        self.path = './files/' + self.database + '_data/'
        self.logger = logging.getLogger(' data update and transform')

    def delete_molgenis_files(self):
        """Delete molgenis.csv
        """
        os.remove(self.path + 'molgenis.csv')
        if 'molgenis_members.csv' in os.listdir(self.path):
            os.remove(self.path + 'molgenis_members.csv')
        if 'molgenis_settings.csv' in os.listdir(self.path):
            os.remove(self.path + 'molgenis_settings.csv')


class CohortsETL:
    """Copy tables from SharedStaging to other schemas to get rid of references from
    SharedStaging inside catalogue"""

    def __init__(self, source_database, target_database):
        self.source_database = source_database
        self.target_database = target_database
        self.path = './files/' + self.source_database + '_data/'

    def cohorts(self):
        """Rename column in cohorts
        """
        df_cohorts = pd.read_csv(self.path + 'Cohorts.csv')
        df_cohorts = float_to_int(df_cohorts)  # convert float back to integer
        new_mg_tableclass = self.target_database + '.Cohorts'
        df_cohorts.mg_tableclass = new_mg_tableclass
        df_cohorts.to_csv(self.path + 'Cohorts.csv', index=False, mode='w+')
