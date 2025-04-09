"""
Utility functions for the StagingMigrator class.
"""
import logging
from io import BytesIO

import pandas as pd
from molgenis_emx2_pyclient.exceptions import NoSuchTableException
from molgenis_emx2_pyclient.metadata import Schema, Table

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


def has_statement_of_consent(table_id: str, schema: Schema) -> int:
    """Checks whether this table has a column that asks for a statement of consent."""
    consent_cols = ['statement of consent personal data',
                    'statement of consent email']
    try:
        table_schema = schema.get_table(by='id', value=table_id)
    except ValueError:
        raise NoSuchTableException(f"Table with id {table_id!r} not in schema.")
    col_names = map(str, table_schema.columns)

    return 1 * (consent_cols[0] in col_names) + 2 * (consent_cols[1] in col_names)


def process_statement(table: bytes, consent_val: int) -> bytes:
    """Processes any statement of consent by modifying the rows in the table for which no consent is given."""
    df = pd.read_csv(BytesIO(table))

    # Remove rows without any data consent
    if consent_val % 2 == 1:
        df = df.loc[df['statement of consent personal data']]
    # Replace email values for rows without email consent
    if consent_val > 1:
        df.loc[~df['statement of consent email'], 'email'] = None

    _table = df.to_csv(index=False).encode()

    log.info("Implemented statement of consent in Contacts table.")

    return _table
