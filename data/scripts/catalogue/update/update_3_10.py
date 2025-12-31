import shutil
import os
import pandas as pd


class Transform:
    """General functions to update catalogue data model.
    """

    def __init__(self, database_type):
        self.database_type = database_type
        self.path = self.database_type + '/'

    def update_data_model_file(self):
        """Get path to data model file and copy molgenis.csv to appropriate folder if it does not exist
        """
        os.mkdir(self.path)

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
        shutil.copyfile(data_model, os.path.abspath(os.path.join(self.path, 'molgenis.csv')))
