"""
Development script for fetching data from a schema using the GraphQL API.
"""

from tools.pyclient.src.molgenis_emx2_pyclient import Client
from tools.pyclient.src.molgenis_emx2_pyclient.metadata import Schema, Table, Column

URL = "https://emx2.dev.molgenis.org"
SCHEMA = "catalogue"

HEADING = "HEADING"
LOGO = "LOGO"
STRING = "STRING"
TEXT = "TEXT"
INT = "INT"
FLOAT = "FLOAT"
BOOL = "BOOL"
HYPERLINK = "HYPERLINK"
DATE = "DATE"
DATETIME = "DATETIME"

NONREFS = [STRING, TEXT, INT, FLOAT, BOOL, DATE, DATETIME]



def get_data() -> list:
    """Fetches data."""

    with Client(url=URL, schema=SCHEMA) as client:
        resources = client.get(table='Resources')


    return resources

def parse_query(raw_query: dict, metadata: Schema) -> str:
    accolade_count = 0
    table_id = raw_query.get('id')
    columns = raw_query.get('columns')
    meta_columns = ["id", "columnType", "refSchemaId", "refTableId"]
    columns = [{key: val for (key, val) in col.items() if key in meta_columns} for col in columns]

    query = f"{{\n  {table_id} {{\n"
    for col in columns:
        if col['columnType'] in [HEADING, LOGO]:
            continue
        if col['columnType'] in NONREFS:
            query += f"    {col['id']}\n"
        if col['columnType'].startswith('ONTOLOGY'):
            query += f"    {col['id']} {{name}}\n"
        if col['columnType'].startswith('REF'):
            pkeys = metadata.get_pkeys(table_id)
    query += "  }\n"
    query += "}"

    return query



if __name__ == '__main__':
    data = get_data()
    print(data)
