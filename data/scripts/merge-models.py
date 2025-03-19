"""
Script to merge and update the data models.
"""
import logging
from pathlib import Path, PosixPath

import pandas as pd

DATA_DIR = Path(__file__).parent.parent.absolute()

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger("merge")

def move_fdp_tables():
    """Moves the tables in the _models/shared directory that are not part of the data catalogue model."""

    logger.info("Moving tables.")
    # Create the directory
    if not (fdp_dir := (DATA_DIR / "_models" / "specific" / "fdp")).exists():
        fdp_dir.mkdir()

    # Iterate over the tables in _models/shared and check if any of the columns have the 'DataCatalogueFlat' profile.
    # If not, move to the new directory
    shared_dir = DATA_DIR / "_models" / "shared"

    table_path: PosixPath
    for table_path in shared_dir.iterdir():
        df = pd.read_csv(table_path)
        if df["profiles"].str.contains("DataCatalogueFlat").any():
            continue
        # Move to new directory
        logger.info(f"Moving file 'shared/{table_path.name}' to 'specific/fdp{table_path.name}'.")
        table_path.rename(Path(str(table_path).replace("shared", "specific/fdp")))

def main():
    move_fdp_tables()


if __name__ == '__main__':
    main()
