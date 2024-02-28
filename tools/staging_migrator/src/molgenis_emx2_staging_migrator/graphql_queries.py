"""
Class with queries for querying the Molgenis server.
"""


class Queries:
    Cohorts = (
        """{
  Cohorts {
    id
    name
    acronym
  }
}"""
    )
