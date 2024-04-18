"""
Script to flatten the DataCatalogue model to a flat, 'hierarchy-less' model
in which all staging area models live in harmony.

TODO: delete when pull request is ready for merging
"""

import warnings
from pathlib import Path

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
    DS = 'Data sources'
    DB = 'Databanks'
    COL = 'Collections'


SHARED_DIR = Path().cwd().joinpath('..', '_models', 'shared')
SPECIFIC_DIR = Path().cwd().joinpath('..', '_models', 'specific')


VERSION = 4.0

inherit_tables = [[Tables.O, Tables.R], [Tables.M, Tables.ER],
                  [Tables.N, Tables.ER], [Tables.S, Tables.ER],
                  [Tables.S, Tables.R], [Tables.M, Tables.R], [Tables.N, Tables.R]] #,
                  # [Tables.DS, Tables.RWE], [Tables.DS, Tables.DR],
                  # [Tables.DS, Tables.ER], [Tables.DS, Tables.R],
                  # [Tables.DB, Tables.RWE], [Tables.DB, Tables.DR],
                  # [Tables.DB, Tables.ER], [Tables.DB, Tables.R]]

rename_tables = [[Tables.C, Tables.DR],
                 # [Tables.DB, Tables.RWE], [Tables.DS, Tables.RWE],
                 [Tables.RWE, Tables.DR],
                 [Tables.DR, Tables.ER],
                 [Tables.ER, Tables.R],
                 [Tables.R, Tables.COL]]

table_profiles = {
    # 'Databanks': 'DatabanksStaging',
    # 'Datasources': 'DatasourcesStaging',
    # 'Resources': ['DatabanksStaging', 'DatasourcesStaging'],
    # 'Contacts': ['DatabanksStaging', 'DatasourcesStaging']
}


class Flattener(pd.DataFrame):
    """
    Class to flatten the DataCatalogue model to a flat, i.e. without inheritance, model.
    """

    def __init__(self, simple_mode: bool = False):
        """Initializes the Flattener object by loading the original dataset."""
        super().__init__(data=pd.read_csv(SHARED_DIR.joinpath('DataCatalogue-TODO.csv')))
        self._prepare_data()
        if simple_mode:
            self.drop(columns=['key', 'required', 'validation', 'semantics', 'description'], inplace=True)

    def __str__(self):
        return f"{pd.DataFrame(self)!s}"

    def __repr__(self):
        return pd.DataFrame(self)

    @property
    def view(self):
        return pd.DataFrame(self)

    def flatten(self) -> pd.DataFrame:
        """Performs the flattening.
        Returns the updated model.
        """

        self._version_bump()

        for tables in inherit_tables:
            self._duplicate_columns(*tables)

        for tp in table_profiles.items():
            self._add_profile_tag(*tp)

        for rn in rename_tables:
            self._rename_table(*rn)

        self._remove_duplicates()

        # self._add_table_label()

        self._remove_shared_staging_resources()

        # Save result to file
        self.save_df()

        self.save_dev_df()

        return self

    def _version_bump(self):
        """Bumps the data model to a new version."""
        self.loc[self['tableName'] == 'Version', 'description'] = VERSION

    def _duplicate_columns(self, new_tab: str, old_tab: str):
        """Duplicates columns of an inherited table for the inheriting table."""

        # Find the columns that are inherited by the new table
        inherited_columns = self.loc[self['tableName'] == old_tab]
        # Find the ancestor of the table from which the columns are inherited
        ancestor = inherited_columns.loc[inherited_columns['columnName'].isna(), 'tableExtends'].values[0]

        # Find the profiles declared in the definition of the inheriting table
        profiles = self.loc[(self['tableName'] == new_tab) & (self['tableExtends'] == old_tab), 'profiles'].values[0]

        # Iterate backwards over the rows of the inherited_columns DataFrame
        # to correctly place the duplicated columns
        for idx in reversed(inherited_columns.index):
            # Increase the index from the duplicated column's index onwards
            self.index = [*self.index[:idx + 1], *self.index[idx + 2:], len(self.index)]
            # Copy the values of the ancestor table and rename the tableName to the new table
            self.loc[idx + 1] = inherited_columns.loc[idx]
            self.loc[idx + 1, 'tableName'] = new_tab
            # Ensure the correct profiles for the duplicated column
            self.loc[idx + 1, 'profiles'] = ','.join(p for p in self.profiles[idx+1].split(',')
                                                     if p in profiles.split(','))
            # Sort the index
            self.sort_index(inplace=True)

        # Keep the column in which the table is defined and replace the old table's name by the ancestor's
        # dc = drop column
        dc = self.loc[(self['tableName'] == new_tab) & self['columnName'].isna() & (self['tableExtends'] != old_tab)]
        self.drop(index=dc.index, axis=1, inplace=True)
        self.loc[(self['tableName'] == new_tab) & self['columnName'].isna(), 'tableExtends'] = ancestor

        self.reset_index(inplace=True, drop=True)
        print(f"Duplicated columns in {old_tab} for {new_tab}.")

    def _add_profile_tag(self, table_name: str, tag: str | list):
        """Adds a tag to all columns for a specified table."""
        if isinstance(tag, list):
            tag = ','.join(tag)
        self['profiles'] = self.apply(lambda row:
                                      row['profiles'] if row['tableName'] != table_name
                                      else row['profiles'] + ',' + tag,
                                      axis=1
                                      )
        print(f"Added profile '{tag}' to table {table_name}.")

    def _rename_table(self, old_name: str, new_name: str):
        """Renames a table and references to that table to the new name."""

        self['refTable'].replace(old_name, new_name, inplace=True)
        self['tableName'].replace(old_name, new_name, inplace=True)
        self['tableExtends'].replace(old_name, new_name, inplace=True)

        self.drop(index=self.loc[(self['tableName'] == self['tableExtends'])].index, axis=1, inplace=True)

        print(f"Renamed tableName for columns of table '{old_name}' to '{new_name}'")

    def _remove_duplicates(self):
        """Removes duplicate rows from the DataFrame."""
        # Drop exact duplicates
        self.drop_duplicates(inplace=True)

        # Remove columns without any profiles
        self.drop(index=self.loc[self['profiles'] == ''].index, axis=1, inplace=True)

        # Find pairs of rows (columns) with the same tableName and columnName
        duplicates = self[['tableName', 'columnName']].duplicated()
        print(self.loc[duplicates, ['tableName', 'columnName', 'profiles']])

    def _prepare_data(self):
        """Prepares the dtype of the 'key' column."""
        self['key'] = self['key'].convert_dtypes()
        self['required'] = self['required'].apply(lambda r: 'true' if r in [True, 'TRUE', 'true', 'True'] else '')

    def _add_table_label(self):
        """Adds the label 'Cohorts' to the 'Resources' table in the CohortsStaging schema.
        And replaces its description by that of the original Cohorts table.
        """
        descriptions = {
            'Cohorts': "Group of individuals sharing a defining demographic characteristic",
            # 'Databanks': "Data collection from real world databases such as health records, registries",
            # 'Data sources': "Collections of multiple data banks covering the same population"
        }
        stagings = {
            'Cohorts': "CohortStaging",
            # 'Databanks': "DatabanksStaging",
            # 'Data sources': "DatasourcesStaging"
        }

        description = "Group of individuals sharing a defining demographic characteristic"
        idx = self.loc[(self['tableName'] == 'Collections') & (self['columnName'].isna())].index[0]
        self['label'] = None
        self.loc[idx, 'profiles'] = ','.join(p for p in self.loc[idx, 'profiles'].split(',')
                                             if p not in ['CohortStaging', 'DatabanksStaging', 'DatasourcesStaging'])
        for inc, table in enumerate(descriptions.keys()):
            self.loc[idx+0.1*(inc+1)] = self.loc[idx]
            self.loc[idx+0.1*(inc+1), 'description'] = descriptions[table]
            self.loc[idx+0.1*(inc+1), 'profiles'] = stagings[table]
            self.loc[idx+0.1*(inc+1), 'label'] = table
        self.sort_index(inplace=True)
        self.reset_index(drop=True, inplace=True)

    def _remove_shared_staging_resources(self):
        """Removes the 'SharedStaging' profile from the Resources table."""
        staging_resources = self.loc[(self['tableName'] == 'Resources')
                                     & self['profiles'].str.contains('SharedStaging')]
        self.loc[staging_resources.index, 'profiles'] = staging_resources['profiles'].apply(
            lambda pfs: pfs.replace('SharedStaging', '').replace(',,', ','))

    def save_df(self, old_profiles: bool = False):
        """Saves the pandas DataFrame of the model to disk."""
        if not old_profiles:
            self.to_csv(SHARED_DIR.joinpath('DataCatalogue-FLAT.csv'), index=False)
        profiles = list(set([p for _l in list(self['profiles'].str.split(',')) for p in _l]))

        file_dir = SPECIFIC_DIR
        if old_profiles:
            file_dir = file_dir.joinpath('old_profiles')
        for prof in profiles:
            self.loc[self['profiles'].str.contains(prof)].drop(
                columns=['profiles']).to_csv(file_dir.joinpath(f"{prof}.csv"), index=False)

    def save_dev_df(self):
        """Saves the pandas DataFrame of the test model to disk."""

        self.replace('catalogue', 'catalogueFLAT', inplace=True)
        file_dir = SPECIFIC_DIR.joinpath('dev')

        profiles = list(set([p for _l in list(self['profiles'].str.split(',')) for p in _l]))
        for prof in profiles:
            self.loc[self['profiles'].str.contains(prof)].drop(
                columns=['profiles']).to_csv(file_dir.joinpath(f"{prof}.csv"), index=False)


def main():
    """The main function calling the execution."""
    flattener = Flattener(simple_mode=False)
    flattener.save_df(old_profiles=True)
    flattener.flatten()
    print(flattener)


if __name__ == '__main__':
    main()
