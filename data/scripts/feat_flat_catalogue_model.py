"""
Script to flatten the DataCatalogue model to a flat, 'hierarchy-less' model
in which all resources live in harmony.

"""

import warnings
from pathlib import Path

import pandas as pd
import os

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

inherit_tables = [[Tables.O, Tables.R]]
# [Tables.M, Tables.ER],
# [Tables.N, Tables.ER], [Tables.S, Tables.ER],
# [Tables.S, Tables.R], [Tables.M, Tables.R], [Tables.N, Tables.R]] #,
# [Tables.DS, Tables.RWE], [Tables.DS, Tables.DR],
# [Tables.DS, Tables.ER], [Tables.DS, Tables.R],
# [Tables.DB, Tables.RWE], [Tables.DB, Tables.DR],
# [Tables.DB, Tables.ER], [Tables.DB, Tables.R]]

rename_tables = [[Tables.C, Tables.DR],
                 [Tables.DB, Tables.RWE], [Tables.DS, Tables.RWE], [Tables.M, Tables.ER],
                 [Tables.N, Tables.ER], [Tables.S, Tables.ER],
                 [Tables.RWE, Tables.DR],
                 [Tables.DR, Tables.ER],
                 [Tables.ER, Tables.R],
                 [Tables.R, Tables.COL]]

rename_columns = {
    'Cohorts': [['type', 'cohort type'], ['type other', 'cohort type other']],
    'Networks': [['type', 'network type'], ['type other', 'network type other']],
    'Studies': [['type', 'study type'], ['type other', 'study type other']],
    'RWE resources': [['type', 'datasource type'], ['type other', 'datasource type other']]
}

table_profiles = {
    # 'Databanks': 'DatabanksStaging',
    # 'Datasources': 'DatasourcesStaging',
    # 'Resources': ['DatabanksStaging', 'DatasourcesStaging'],
    # 'Contacts': ['DatabanksStaging', 'DatasourcesStaging']
}

visibility = {
    Tables.ER: ['Cohort', 'Databank', 'Data source', 'Model', 'Network', 'Study'],
    Tables.DR: ['Cohort', 'Databank', 'Data source'],
    Tables.RWE: ['Databank', 'Data source'],
    Tables.C: ['Cohort'],
    Tables.DB: ['Databank'],
    Tables.DS: ['Data source'],
    Tables.M: ['Model'],
    Tables.N: ['Network'],
    Tables.S: ['Study']
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

        self._rename_columns()

        self._add_some_columns()

        self._add_visibility()

        for tables in inherit_tables:
            self._duplicate_columns(*tables)

        for tp in table_profiles.items():
            self._add_profile_tag(*tp)

        for rn in rename_tables:
            self._rename_table(*rn)

        self._remove_duplicates()

        self._remove_shared_staging_resources()

        self._remove_refbacks()

        self._remove_some_columns()

        self._rename_profiles()

        self._rename_tables_and_columns()

        # Save result to file
        # self.save_df()

        self.save_split_df()

        self.save_dev_df()

        return self

    def _version_bump(self):
        """Bumps the data model to a new version."""
        self.loc[self['tableName'] == 'Version', 'description'] = VERSION

    def _add_some_columns(self):
        """Adds some columns."""

        resources_name_idx = self.loc[(self['tableName'] == 'Resources')
                                      & (self['columnName'] == 'name')].index[0]
        self.loc[resources_name_idx+0.5, ['tableName', 'columnName', 'columnType',
                                          'refSchema', 'refTable', 'profiles']] = ['Resources', 'collection type',
                                                                                   'ontology', 'CatalogueOntologies',
                                                                                   'Collection types FLAT',
                                                                                   'DataCatalogue,CohortStaging']
        self.sort_index(inplace=True)
        self.reset_index(drop=True, inplace=True)

    def _add_visibility(self):
        """Adds visibility conditions for columns belonging to certain tables."""
        self['visible'] = None
        self.loc[self['tableName'] == 'Cohorts', 'visible'] = f"{['Cohort']}.includes(collectionType?.name)"
        for table, types in visibility.items():
            self.loc[self['tableName'] == table, 'visible'] = f"{types}.includes(collectionType?.name)"

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
            # self.index = [*self.index[:idx + 1], *self.index[idx + 2:], len(self.index)]
            # Copy the values of the ancestor table and rename the tableName to the new table
            self.loc[idx + 0.5] = inherited_columns.loc[idx]
            self.loc[idx + 0.5, 'tableName'] = new_tab
            # Ensure the correct profiles for the duplicated column
            self.loc[idx + 0.5, 'profiles'] = ','.join(p for p in self.profiles[idx + 0.5].split(',')
                                                       if p in profiles.split(','))
            # Sort the index
            self.sort_index(inplace=True)
            self.reset_index(drop=True, inplace=True)

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

    def _rename_columns(self):
        """Renames columns."""
        for table_name, cols in rename_columns.items():
            for old, new in cols:
                self.loc[self['tableName'] == table_name, 'columnName'] \
                    = self.loc[self['tableName'] == table_name, 'columnName'].replace(old, new)

    def _remove_duplicates(self):
        """Removes duplicate rows from the DataFrame."""
        # Drop exact duplicates
        self[['tableName', 'columnName', 'profiles']].drop_duplicates(inplace=True)

        # Remove columns without any profiles
        self.drop(index=self.loc[self['profiles'] == ''].index, axis=1, inplace=True)

        # Find pairs of rows (columns) with the same tableName and columnName
        duplicates = self[['tableName', 'columnName']].duplicated()
        print(self.loc[duplicates, ['tableName', 'columnName', 'profiles']])

    def _remove_refbacks(self):
        """Removes columns with refbacks from the Collections table."""
        self.drop(index=self.loc[(self['tableName'] == 'Collections')
                                 & (self['columnType'] == 'refback')].index, inplace=True)

    def _remove_some_columns(self):
        self.drop(index=self.loc[(self['tableName'] == 'Organisations')
                                 & (self['columnName'] == 'collection type')].index, inplace=True)

    def _prepare_data(self):
        """Prepares the dtype of the 'key' column."""
        self['key'] = self['key'].convert_dtypes()
        self['required'] = self['required'].apply(lambda r: 'true' if r in [True, 'TRUE', 'true', 'True'] else '')

    def _remove_shared_staging_resources(self):
        """Removes the 'SharedStaging' profile from the Resources table."""
        staging_resources = self.loc[(self['tableName'] == 'Resources')
                                     & self['profiles'].str.contains('SharedStaging')]
        self.loc[staging_resources.index, 'profiles'] = staging_resources['profiles'].apply(
            lambda pfs: pfs.replace('SharedStaging', '').replace(',,', ','))

    def _rename_profiles(self):
        """Renames profiles to distinguish between DataCatalogue and DataCatalogueFlat. Deletes rows
        with empty profiles column
        """
        self.loc[:, 'profiles'] = self['profiles'].apply(change_profiles)
        self.drop(index=self.loc[self['profiles'] == ''].index, inplace=True)

        return self

    def _rename_tables_and_columns(self):
        """Renames tables and columns"""
        self.loc[:, 'tableName'] = self['tableName'].apply(change_names)
        self.loc[:, 'columnName'] = self['columnName'].apply(change_names)
        self.loc[:, 'refTable'] = self['refTable'].apply(change_names)
        self.loc[:, 'refLink'] = self['refLink'].apply(change_names)
        self.loc[:, 'refBack'] = self['refBack'].apply(change_names)

        return self

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

    def save_split_df(self):
        """Saves the pandas DataFrame of the model split into tables to disk. Some tables are merged,
        or contain new attributes and are written outside the program.
        """
        for table_name in self['tableName'].unique().tolist():
            if table_name not in ['Datasets', 'All variables', 'Variables', 'Repeated variables',
                                  'Dataset mappings', 'Variable mappings', 'Network variables']:
                df = self.drop(self[self['tableName'] != table_name].index)
                path = SHARED_DIR.joinpath(table_name + '.csv')
                df.to_csv(path, index=False, mode='w')

    def save_dev_df(self):
        """Saves the pandas DataFrame of the flat model to disk."""
        profile = 'DataCatalogueFlat'

        data_model = get_data_model(SHARED_DIR, profile)
        file_dir = str(SPECIFIC_DIR.joinpath('dev'))
        data_model.to_csv(file_dir + '/' + profile + '.csv', index=None)


def get_data_model(path, profile):
    data_model = pd.DataFrame()

    for file_name in os.listdir(path):
        if '.csv' in file_name:
            df = pd.read_csv(path.joinpath(file_name))
            df = df.loc[df['profiles'].str.contains(profile)]
            data_model = pd.concat([data_model, df])

    data_model = float_to_int(data_model)

    return data_model


def float_to_int(df):
    """
    Cast float64 Series to Int64.
    """
    for column in df.columns:
        if df[column].dtype == 'float64':
            df.loc[:, column] = df[column].astype('Int64')

    return df


def change_profiles(profiles):
    """ Changes profile names in pandas series
    """
    profiles = profiles.replace('DataCatalogue', 'DataCatalogueFlat')
    profiles = profiles.replace('CohortStaging', '')
    profiles = profiles.replace('SharedStaging', '')
    # profiles = profiles.replace('EMA', '')
    profiles = profiles.rstrip(',,')
    profiles = profiles.rstrip(',')

    return profiles


def change_names(name):
    """ Changes table and column names in pandas series
    """
    names_to_replace = {'Subcohorts': 'Collection populations',
                        'resource': 'collection',
                        'subcohorts': 'populations',
                        'Linked resources': 'Linked collections',
                        'main resource': 'main collection',
                        'linked resource': 'linked collection',
                        'other linked resource': 'other linked collection',
                        'All variables': 'Variables',
                        'resources': 'collections',
                        'Subcohort counts': 'Population counts',
                        }

    if name in names_to_replace.keys():
        name = names_to_replace[name]

    return name


def main():
    """The main function calling the execution."""
    flattener = Flattener(simple_mode=False)
    # flattener.save_df(old_profiles=True)
    flattener.flatten()
    print(flattener)


if __name__ == '__main__':
    main()
