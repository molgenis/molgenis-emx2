"""
Script to transform the DataCatalogue model to a flat, 'hierarchy-less' model
in which all staging area models live in harmony.

TODO: delete when pull request is ready for merging
"""

import warnings

import pandas as pd

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

inherit_tables = [[Tables.O, Tables.R], [Tables.M, Tables.ER],
                  [Tables.N, Tables.ER], [Tables.S, Tables.ER],
                  # [Tables.C, Tables.DR], [Tables.RWE, Tables.DR],
                  # [Tables.DR, Tables.ER],
                  # [Tables.ER, Tables.R],
                  [Tables.S, Tables.R], [Tables.M, Tables.R], [Tables.N, Tables.R]]

rename_tables = [[Tables.C, Tables.DR], [Tables.RWE, Tables.DR],
                 [Tables.DR, Tables.ER],
                 [Tables.ER, Tables.R]]

table_profiles = {
    'Resources': 'RWEStaging',
    'RWE resources': 'RWEStaging',
}

renames = {
    # 'Cohorts': 'Resources',
    # 'RWE resources': 'Resources'
}


class Transformer(pd.DataFrame):
    """
    Class to transform the DataCatalogue model to a flat, i.e. without inheritance, model.
    """

    def __init__(self):
        """Initializes the Transformer object by loading the original dataset."""
        super().__init__(data=pd.read_csv(f"{MODELS_DIR}/DataCatalogue-TODO.csv"))
        self._prepare_data()

    def __str__(self):
        return f"{pd.DataFrame(self)}"

    def __repr__(self):
        return pd.DataFrame(self)

    @property
    def view(self):
        return pd.DataFrame(self)

    def do_transform(self) -> pd.DataFrame:
        """Performs the transformation.
        Returns the updated model.
        """

        for tables in inherit_tables:
            self._duplicate_columns(*tables)

        for tp in table_profiles.items():
            self._add_profile_tag(*tp)

        for rn in rename_tables:
            self._rename_table(*rn)

        self._remove_duplicates()

        # Save result to file
        self._save_df()

        return self

    def _duplicate_columns(self, new_tab: str, old_tab: str):
        """Duplicates columns of an inherited table for the inheriting table."""
        inheriting_tables = self.loc[self['tableName'] == old_tab]

        ancestor = inheriting_tables.loc[inheriting_tables['columnName'].isna(), 'tableExtends'].values[0]

        for idx in reversed(inheriting_tables.index):
            self.index = [*self.index[:idx + 1], *self.index[idx + 2:], len(self.index)]
            self.loc[idx + 1] = inheriting_tables.loc[idx]
            self.loc[idx + 1, 'tableName'] = new_tab
            self.sort_index(inplace=True)

        # Keep the column in which the table is defined
        # dc = drop column
        dc = self.loc[(self['tableName'] == new_tab) & self['columnName'].isna() & (self['tableExtends'] != old_tab)]
        self.drop(index=dc.index, axis=1, inplace=True)
        self.loc[(self['tableName'] == new_tab) & self['columnName'].isna(), 'tableExtends'] = ancestor

        self.reset_index(inplace=True, drop=True)
        print(f"Duplicated columns in {old_tab} for {new_tab}.")

    def _add_profile_tag(self, table_name: str, tag: str):
        """Adds a tag to all columns for a specified table."""
        self['profiles'] = self.apply(lambda row:
                                      row['profiles'] if row['tableName'] != table_name
                                      else row['profiles'] + ',' + tag,
                                      axis=1
                                      )
        print(f"Added profile '{tag}' to table {table_name}.")

    def _rename_table(self, old_name: str, new_name: str):
        """Renames a table and references to that table to the new name."""
        # self['refTable'] = self['refTable'].apply(lambda rt: rt if rt != old_name else new_name)
        self['refTable'].replace(old_name, new_name, inplace=True)
        self['tableName'] = self['tableName'].replace(old_name, new_name)
        self['tableExtends'] = self['tableExtends'].replace(old_name, new_name)

        self.drop(index=self.loc[(self['tableName'] == self['tableExtends'])].index, axis=1, inplace=True)

        print(f"Renamed tableName for columns of table '{old_name}' to '{new_name}'")

    def _remove_duplicates(self):
        """Removes duplicate rows from the DataFrame."""
        # Drop exact duplicates
        self.drop_duplicates(inplace=True)

        # Find pairs of rows (columns) with the same tableName and columnName
        duplicates = self[['tableName', 'columnName']].duplicated()
        print(self.loc[duplicates])

    def _prepare_data(self):
        """Prepares the dtype of the 'key' column."""
        self['key'] = self['key'].convert_dtypes()

    def _save_df(self):
        """Saves the pandas DataFrame of the model to disk."""
        self.to_csv(f"{MODELS_DIR}/DataCatalogue-FLAT.csv", index=False)
        profiles = list(set([p for l in list(self['profiles'].str.split(',')) for p in l]))
        for prof in profiles:
            self.loc[self['profiles'].str.contains(prof)].to_csv(f"{MODELS_DIR}/profiles/{prof}.csv", index=False)


def main():
    """The main function calling the execution."""
    transformer = Transformer()
    transformer.do_transform()


if __name__ == '__main__':
    main()
