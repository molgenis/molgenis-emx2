"""
Utility functions for the StagingMigrator class.
"""
import logging
import zipfile
from io import BytesIO

import pandas as pd
from molgenis_emx2_pyclient.constants import DATE, DATETIME
from molgenis_emx2_pyclient.exceptions import NoSuchTableException
from molgenis_emx2_pyclient.metadata import Schema, Table
from molgenis_emx2_pyclient.utils import convert_dtypes
from numpy import nan

from staging_migrator.src.molgenis_emx2_staging_migrator.constants import BASE_DIR
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


def has_statement_of_consent(table: Table) -> int:
    """Checks whether this table has a column that asks for a statement of consent."""
    consent_cols = ['statement of consent personal data',
                    'statement of consent email']

    col_names = map(str, table.columns)

    return 1 * (consent_cols[0] in col_names) + 2 * (consent_cols[1] in col_names)


def process_statement(df: pd.DataFrame, consent_val: int) -> pd.DataFrame:
    """Processes any statement of consent by modifying the rows in the table for which no consent is given."""

    # Remove rows without any data consent
    if consent_val % 2 == 1:
        df['mg_delete'] = ~df['statement of consent personal data'].replace({nan: False})
    # Replace email values for rows without email consent
    if consent_val > 1:
        df.loc[~df['statement of consent email'], 'email'] = None

    log.info("Implemented statement of consent in Contacts table.")

    return df


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
    df = df.astype(dtypes)

    return df


def set_all_delete(table: Table) -> pd.DataFrame:
    """
    Adds an `mg_delete` column to the table and sets its values to `true`.
    """
    source_df = load_table('source', table)
    source_df["mg_delete"] = True
    return source_df
