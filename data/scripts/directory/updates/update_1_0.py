import os


class Transform:
    """General functions to update Directory or staging area data models."""

    def __init__(self, database_name, database_type):
        self.database_name = database_name
        self.database_type = database_type
        self.path = self.database_name + '_data/'

    def delete_data_model_file(self):
        """Delete molgenis.csv"""
        os.remove(self.path + 'molgenis.csv')

    def transform_data(self):
        """Make changes per table"""
        # transformations for catalogue and cohorts
        if self.database_type == 'BIOBANK_DIRECTORY':
            print("  No transformations necessary this time")

        if self.database_type == "BIOBANK_DIRECTORY_STAGING":
            print("  No transformations necessary this time")
