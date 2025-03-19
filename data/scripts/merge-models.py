"""
Script to merge and update the data models.
"""
import logging
from pathlib import Path, PosixPath

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


def main():
    tables = identify_non_catalogue()
    print(tables)


if __name__ == '__main__':
    main()
