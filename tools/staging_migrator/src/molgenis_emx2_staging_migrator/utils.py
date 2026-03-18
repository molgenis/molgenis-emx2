"""
Utility functions for the StagingMigrator class.
"""
import logging
import zipfile
from io import BytesIO

import numpy as np
import pandas as pd
from molgenis_emx2_pyclient.constants import DATE, DATETIME
from molgenis_emx2_pyclient.exceptions import NoSuchTableException
from molgenis_emx2_pyclient.metadata import Schema, Table
from molgenis_emx2_pyclient.utils import convert_dtypes
from pandas._libs.missing import NAType

from staging_migrator.src.molgenis_emx2_staging_migrator.constants import BASE_DIR
from staging_migrator.src.molgenis_emx2_staging_migrator.exceptions import MissingContactException
from staging_migrator.src.molgenis_emx2_staging_migrator.migrator import SchemaType

log = logging.getLogger(__name__)


def prepare_primary_keys(schema: Schema, table_name: str):
    """
    Finds the primary keys of a table and returns them in a format compatible with CSV output.
    """
    try:
        table_schema: Table = schema.get_table(by='name', value=table_name)
    except ValueError:
        raise NoSuchTableException(f"{table_name!r} not found in schema.")

    return list(map(lambda col: col.name, table_schema.get_columns(by='key', value=1)))


def resource_ref_cols(schema: Schema, table_name: str) -> list[str]:
    """
    Finds the columns in a table definition that reference the 'Resources' table.
    """
    try:
        table_schema: Table = schema.get_table(by='name', value=table_name)
    except ValueError:
        raise NoSuchTableException(f"{table_name!r} not found in schema.")

    if table_name == "Resources":
        return ["id"]
    return list(map(lambda col: col.name, table_schema.get_columns("refTableId", "Resources")))


def process_contacts(contacts: pd.DataFrame, resources: pd.DataFrame) -> pd.DataFrame:
    """Processes any statement of consent by modifying the rows in the table for which no consent is given.
    Checks whether the Resources table does not reference omitted contacts.
    """
    statement = "statement of consent personal data"

    def set_delete(row: pd.Series):
        if statement in row.index and not row[statement]:
            return True
        if row['email'] is None:
            return True
        return False

    # Remove rows without any data consent
    contacts['mg_delete'] = contacts.apply(set_delete, axis=1)
    contacts = contacts.drop(columns=[statement])
    c_cols = ["resource", "first name", "last name"]

    r_cols = [c for c in resources.columns if c.startswith("contact point")]
    r_contacts = resources[r_cols]
    r_contacts = r_contacts.loc[~r_contacts["contact point.resource"].isna()]

    r_contacts["key"] = r_contacts[r_cols].apply(lambda row: '(' + ', '.join(row.values.astype(str)) + ')', axis=1)
    contacts["key"] = contacts[c_cols].apply(lambda row: '(' + ', '.join(row.values.astype(str)) + ')', axis=1)

    r_contacts["mg_delete"] = r_contacts["key"].map(contacts.set_index("key")["mg_delete"].to_dict())
    if any(r_contacts["mg_delete"]):
        missing = r_contacts.loc[r_contacts["mg_delete"]]
        values = ', '.join(missing["key"].values)
        raise MissingContactException(f"Cannot migrate resource due to missing email or consent "
                                      f"for contact (Resource, first name, last name) = {values}.")

    contacts = contacts.drop(columns=["key"])
    return contacts


def load_table(schema_type: SchemaType, table: Table) -> pd.DataFrame:
    """Loads the table from a zip file into a DataFrame.
    Then parses the data by converting the columns' dtypes.
    """
    with zipfile.ZipFile(BASE_DIR / f"{schema_type}.zip", 'r') as archive:
        raw_df = pd.read_csv(BytesIO(archive.read(f"{table.name}.csv")), nrows=1)

    columns = raw_df.columns
    dtypes = {c: convert_dtypes(table).get(c, "string") for c in columns}

    bool_columns = [c for (c, t) in dtypes.items() if t == 'boolean']
    date_columns = [c.name for c in table.columns
                    if c.get('columnType') in (DATE, DATETIME) and c.name in columns]

    with zipfile.ZipFile(BASE_DIR / f"{schema_type}.zip", 'r') as archive:
        df = pd.read_csv(filepath_or_buffer=BytesIO(archive.read(f"{table.name}.csv")),
                         dtype=dtypes,
                         na_values=[""],
                         keep_default_na=False,
                         parse_dates=date_columns)

    df[bool_columns] = df[bool_columns].replace({'true': True, 'false': False})
    df = df.replace({np.nan: None})
    df = df.astype(dtypes)

    return df


def set_all_delete(table: Table) -> pd.DataFrame:
    """
    Adds an `mg_delete` column to the table and sets its values to `true`.
    """
    source_df = load_table('source', table)
    source_df["mg_delete"] = True
    return source_df

def check_hricore(resources: pd.DataFrame):
    """Verifies that the `hricore` column is set to `true` for the Resources listed."""
    def is_missing(val) -> bool:
        return (type(val) == NAType) or val != True
    # missing_hri = resources.loc[resources['hricore'].isin() or resources['hricore'] != True, 'id']
    missing_hri = resources.loc[resources['hricore'].apply(is_missing), 'id']
    if len(missing_hri.index) != 0:
        raise ValueError(f"Value 'hricore' not set to 'true' for resource {', '.join(missing_hri.values)}")
