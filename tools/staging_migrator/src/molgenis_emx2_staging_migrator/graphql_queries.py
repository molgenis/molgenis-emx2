"""
Class with queries for querying the Molgenis server.
"""


class Queries:
    Cohorts = (
        """query Cohorts($filter: CohortsFilter) {
  Cohorts(filter: $filter) {
    id
    name
    acronym
  }
}"""
    )
