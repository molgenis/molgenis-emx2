"""
Utility functions for the StagingMigrator class.
"""
import logging

import requests


from tools.staging_migrator.src.molgenis_emx2_staging_migrator.graphql_queries import Queries


log = logging.getLogger(__name__)


def delete_staging_from_catalogue(url, session: requests.Session,
                                  staging_area: str,
                                  catalogue: str,
                                  tables_to_delete: dict):
    """
    Prepares the staging area by deleting data from tables
    that are later synchronized from the staging area.
    """
    source_cohort_id = get_staging_cohort_id(url, session, staging_area)
    
    for t_name, t_type in tables_to_delete.items():
        if t_type == 'resource':
            variables = {"filter": {"resource": {"equals": [{"id": source_cohort_id}]}}}
        elif t_type == 'mappings':
            variables = {"filter": {"source": {"equals": [{"id": source_cohort_id}]}}}
        elif t_type == 'variables':
            variables = {"filter": {"resource": {"equals": [{"id": source_cohort_id}]}}}
        elif t_type == 'id':
            variables = {"filter": {"equals": [{"id": source_cohort_id}]}}
        elif t_type == 'subcohort':
            variables = {"filter": {"subcohort": {"resource": {"equals": [{"id": source_cohort_id}]}}}}
        else:
            continue

        table_query = Queries
        # TODO move function back to class
        # TODO Generate queries based on table schema with primary keys


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
