"""
Script to merge and update the data models.
"""
import logging
from pathlib import Path, PosixPath

import numpy as np
import pandas as pd

DATA_DIR = Path(__file__).parent.parent.absolute()

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger("merge")

def identify_non_catalogue() -> list:
    """Identifies the tables in the _models/shared directory that are not part of the data catalogue model."""


    # Iterate over the tables in _models/shared and check if any of the columns have the 'DataCatalogueFlat' profile.
    # If not, move to the new directory
    shared_dir = DATA_DIR / "_models" / "shared"

    non_catalogue = list()
    table_path: PosixPath
    for table_path in shared_dir.glob("*.csv"):
        df = pd.read_csv(table_path)
        if df["profiles"].str.contains("DataCatalogueFlat").any():
            continue
        non_catalogue.append(table_path.name)

    return non_catalogue

def merge_models():
    """
    Merges the portal and data catalogue data models.
    Iterates over the tables in the portals directory.
    If the table is not present in _models/shared the table is moved there and the profile 'portal' is added
    to each of its columns' metadata.
    Else iterates of the columns in that table.
    If a portal column's metadata does not fully match the corresponding catalogue column's metadata,
    or if the column does not exist in the catalogue model the column's metadata is added as a new row
    and given the 'portal' profile.
    Else the profile 'portal' is appended to the list of profiles of the corresponding column.
    """
    portal_dir = DATA_DIR / "portal"
    shared_dir = DATA_DIR / "_models" / "shared"

    for table_file in portal_dir.glob("*.csv"):
        if (shared_dir / table_file.name).exists():
            logger.info(f"Merging table {table_file.name!r}.")

            portal_df = pd.read_csv(table_file).replace({np.nan: None}).set_index('columnName')
            cat_df = pd.read_csv(shared_dir / table_file.name).replace({np.nan: None}).set_index('columnName')

            print(portal_df.head(5).to_string())
            print(cat_df.head(5).to_string())

            cols = portal_df.columns.drop("profiles")

            # Matching columns
            portal_match = portal_df.loc[portal_df.index.isin(cat_df.index)]
            portal_add = portal_df.loc[~portal_df.index.isin(cat_df.index)]
        else:
            logger.info(f"Moving table {table_file.name!r} to folder 'shared'.")

def main():
    tables = identify_non_catalogue()
    print(tables)
    merge_models()


if __name__ == '__main__':
    main()
