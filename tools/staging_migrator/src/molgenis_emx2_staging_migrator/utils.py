"""
Utility functions for the StagingMigrator class.
"""
import logging

from tools.staging_migrator.src.molgenis_emx2_staging_migrator.graphql_queries import Queries

log = logging.getLogger(__name__)


def get_staging_cohort_id(url, session, staging_area) -> str | None:
    """Fetches the id associated with the staging area's cohort."""

    # Query server for cohort id
    query = Queries.Cohorts
    staging_url = f"{url}/{staging_area}/graphql"
    response = session.post(url=staging_url, json={"query": query}).json().get('data')
    
    # Return only if there is exactly one id/cohort in the Cohorts table
    if "Cohorts" in response.keys():
        if len(response['Cohorts']) != 1:
            log.warning(
                f'Expected a single cohort in staging area "{staging_area}"'
                f' but found {len(response["Cohorts"])}')
            return None
    else:
        log.warning(
            f'Expected a single cohort in staging area "{staging_area}"'
            f' but found none.')
        return None

    return response['Cohorts'][0]['id']


def table_to_pascal(_table: str) -> str:
    """Converts a table name to Pascal case format for use in
    GraphQL queries, e.g. 'Variable mappings' -> 'VariableMappings'.
    """
    return "".join(word.capitalize() for word in _table.split(' '))


def prepare_pkey(schema: dict, table_name: str, col_name: str | list = None) -> str | list | dict:
    """Prepares a primary key by adding a reference to a table if necessary."""
    if not col_name:
        # Return the primary keys of a table if no column name is specified
        return [col['name'] for col in schema[table_name]['columns'] if col.get('key') == 1]
    col_data, = [col for col in schema[table_name]['columns'] if col['name'] == col_name]
    if not col_data.get('columnType') in ['REF', 'REF_ARRAY']:
        return col_name
    if col_data.get('columnType') == 'REF':
        ref_keys = prepare_pkey(schema, col_data.get('refTable'))
        ref_cols = [prepare_pkey(schema, col_data['refTable'], rk) for rk in ref_keys]
        return {col_name: ref_cols}


def query_columns_string(column: str | list | dict, indent: int) -> str:
    """Constructs a query string based on the level of indentation requested."""
    if isinstance(column, str):
        return_val = f"{indent*' '}{column}"
        return return_val
    if isinstance(column, list):
        return_val = "\n".join(query_columns_string(item, indent=indent) for item in column)
        return return_val
    if isinstance(column, dict):
        col = list(column.keys())[0]
        vals = list(column.values())[0]
        return_val = (f"{indent*' '}{col} {{\n"
                      f"{query_columns_string(vals, indent=indent+2)}\n"
                      f"{indent*' '}}}")
        return return_val
