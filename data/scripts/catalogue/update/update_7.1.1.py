import shutil
import os
from pathlib import Path
import pandas as pd
from decouple import config

CATALOGUE_SCHEMA_NAME = config('MG_CATALOGUE_SCHEMA_NAME')

class Transform:
    """General functions to update catalogue data model.
    """

    def __init__(self, schema_name, profile):
        self.schema_name = schema_name
        self.path = self.schema_name + '_data/'

    def delete_data_model_file(self):
        """Delete molgenis.csv
        """
        os.remove(self.path + 'molgenis.csv')

    def transform_data(self):
        """Make changes per table
        """
        # transformations per table
        self.resources()

    def resources(self):
        """ Transform data in Resources
        """
        df_resources = pd.read_csv(self.path + 'Resources.csv', dtype='object')

        # update resource types
        df_resources['type'] = df_resources['type'].apply(update_types)

        # write table to file
        df_resources.to_csv(self.path + 'Resources.csv', index=False)

def update_types(types):
    updated_types = ''
    types = types.split(',')

    for t in types:
        if t in ['Disease specific', 'Other type']:
            


    return updates_types
