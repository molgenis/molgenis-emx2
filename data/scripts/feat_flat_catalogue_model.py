"""
Script to transform the DataCatalogue model to a flat, 'hierarchy-less' model
in which all staging area models live in harmony.

TODO: delete when pull request is ready for merging
"""

import pandas as pd


MODELS_DIR = '../_models/shared'

inherit_tables = {'Organisations': 'Resources', 'Data resources': 'Extended resources', 'Models': 'Extended resources',
                  'Networks': 'Extended resources', 'Studies': 'Extended resources', 'Extended resources': 'Resources'
                  }

class Transformer:
    """
    Class to transform the DataCatalogue model to a flat, i.e. without inheritance, model.
    """
    df: pd.DataFrame

    def __init__(self):
        """Initializes the Transformer object by loading the original dataset."""
        self.df = self._load_data()

    def transform(self) -> pd.DataFrame:
        """Performs the transformation.
        Returns the updated model.
        """
        self.df = self._load_data()

        for tables in inherit_tables.items():
            self._duplicate_columns(*tables)


        # Save result to file
        self._save_df()

        return self.df

    def _duplicate_columns(self, new_tab: str, old_tab: str):
        """Duplicates columns of an inherited table for the inheriting table."""
        inheriting_tables = self.df.loc[self.df['tableName'] == old_tab]

        for idx in reversed(inheriting_tables.index):
            self.df.index = [*self.df.index[:idx+1], *list(self.df.index[idx+1:] + 1)]
            self.df.loc[idx+1] = inheriting_tables.loc[idx]
            self.df.loc[idx+1, 'tableName'] = new_tab
            self.df.sort_index(inplace=True)
        self.df = self.df.loc[(self.df['tableName'] != old_tab) | (self.df['tableExtends'] != new_tab)]

    @staticmethod
    def _load_data() -> pd.DataFrame:
        return pd.read_csv(f"{MODELS_DIR}/DataCatalogue-TODO.csv")

    def _save_df(self):
        """Saves the pandas DataFrame of the model to disk."""
        self.df.to_csv(f"{MODELS_DIR}/DataCatalogue-FLAT.csv", index=False)


def transform_data_model():

    # Load DataCatalogue
    data_catalogue = pd.read_csv(f"{MODELS_DIR}/DataCatalogue-TODO.csv")
    print(data_catalogue.head())

    # Make a copy
    df = data_catalogue.copy()

    # Transform Organisations, Models, Networks and Studies into standalone tables that do not inherit from *Resources
    # Copy Organisations columns from Resources columns
    resources = df.loc[df['tableName'] == 'Resources']
    for idx in reversed(resources.index):
        df.index = [*df.index[:idx+1], *list(df.index[idx+1:] + 1)]
        df.loc[idx+1] = resources.loc[idx]
        df.loc[idx+1, 'tableName'] = 'Organisations'
        df.sort_index(inplace=True)

    # Change tableName from 'Cohorts' to 'Resources' and add 'Cohorts' as profile

    df.to_csv(f"{MODELS_DIR}/DataCatalogue-FLAT.csv", index=False)


def replace_table_name(old_tab: str, new_tab: str):
    """
    Duplicates rows of a table that are inherited by another table.
    """


if __name__ == '__main__':
    # transform_data_model()
    transformer = Transformer()
    transformer.transform()
