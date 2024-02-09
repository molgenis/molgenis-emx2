"""
Script to transform the DataCatalogue model to a flat, 'hierarchy-less' model
in which all staging area models live in harmony.

TODO: delete when pull request is ready for merging
"""

import pandas as pd
import warnings

warnings.filterwarnings("ignore", category=FutureWarning)


class Tables:
    O = 'Organisations'
    R = 'Resources'
    DR = 'Data resources'
    ER = 'Extended resources'
    M = 'Models'
    S = 'Studies'
    N = 'Networks'
    C = 'Cohorts'
    RWE = 'RWE resources'


MODELS_DIR = '../_models/shared'

inherit_tables = [[Tables.O, Tables.R], [Tables.DR, Tables.ER], [Tables.M, Tables.ER],
                  [Tables.N, Tables.ER], [Tables.S, Tables.ER],
                  [Tables.C, Tables.DR], [Tables.RWE, Tables.DR],
                  [Tables.DR, Tables.ER],
                  [Tables.ER, Tables.R],
                  [Tables.S, Tables.R], [Tables.M, Tables.R], [Tables.N, Tables.R]]

table_profiles = {
    'Resources': 'RWEStaging',
    'RWE resources': 'RWEStaging',
}

renames = {
    'Cohorts': 'Resources',
    'RWE resources': 'Resources'
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

        for tables in inherit_tables:
            self._duplicate_columns(*tables)

        for tp in table_profiles.items():
            self._add_profile_tag(*tp)

        for rn in renames.items():
            self._rename_table(*rn)

        self._remove_duplicates()

        # Save result to file
        self._save_df()

        return self.df

    def _duplicate_columns(self, new_tab: str, old_tab: str):
        """Duplicates columns of an inherited table for the inheriting table."""
        inheriting_tables = self.df.loc[self.df['tableName'] == old_tab]

        for idx in reversed(inheriting_tables.index):
            self.df.index = [*self.df.index[:idx + 1], *list(self.df.index[idx + 1:] + 1)]
            self.df.loc[idx + 1] = inheriting_tables.loc[idx]
            self.df.loc[idx + 1, 'tableName'] = new_tab
            self.df.sort_index(inplace=True)
        self.df = self.df.loc[(self.df['tableName'] != new_tab) | (self.df['tableExtends'] != old_tab)]
        self.df.reset_index(inplace=True, drop=True)
        print(f"Duplicated columns in {old_tab} for {new_tab}.")

    def _add_profile_tag(self, table_name: str, tag: str):
        """Adds a tag to all columns for a specified table."""
        self.df['profiles'] = self.df.apply(lambda row:
                                            row['profiles'] if row['tableName'] != table_name
                                            else row['profiles'] + ',' + tag,
                                            axis=1
                                            )
        print(f"Added profile '{tag}' to table {table_name}.")

    def _rename_table(self, old_name: str, new_name: str):
        """Renames a table and references to that table to the new name."""
        self.df['refTable'] = self.df['refTable'].apply(lambda rt: rt if rt != old_name else new_name)
        self.df['tableName'] = self.df['tableName'].replace(old_name, new_name)
        self.df['tableExtends'] = self.df['tableExtends'].replace(old_name, new_name)

        print(f"Renamed tableName for columns of table '{old_name}' to '{new_name}'")

    def _remove_duplicates(self):
        """Removes duplicate rows from the DataFrame."""
        # Drop exact duplicates
        self.df.drop_duplicates(inplace=True)

        # Find pairs of rows (columns) with the same tableName and columnName
        duplicates = self.df[['tableName', 'columnName']].duplicated()
        print(duplicates)

    @staticmethod
    def _load_data() -> pd.DataFrame:
        data = pd.read_csv(f"{MODELS_DIR}/DataCatalogue-TODO.csv")
        data['key'] = data['key'].convert_dtypes()
        return data

    def _save_df(self):
        """Saves the pandas DataFrame of the model to disk."""
        self.df.to_csv(f"{MODELS_DIR}/DataCatalogue-FLAT.csv", index=False)


def main():
    """The main function calling the execution."""
    transformer = Transformer()
    transformer.transform()


if __name__ == '__main__':
    main()
