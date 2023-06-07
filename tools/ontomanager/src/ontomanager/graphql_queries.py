# graphql_queries.py

class Queries:
    """Class containing methods that generate valid GraphQL queries based on input."""

    @staticmethod
    def insert(table: str) -> str:
        """
        Query for inserting a term into a table.
        :param table: the name of the table in which the term is inserted.
        """
        query = """
        mutation insert($value:[""" + table + """Input]) {
          insert(""" + table + """:$value){message}
        }"""
        return query

    @staticmethod
    def delete(table: str) -> str:
        """
        Query for deleting a term from a table.
        :param table: the name of the table from which the term is deleted.
        """
        query = """
        mutation delete($pkey:[""" + table + """Input]) {
          delete(""" + table + """:$pkey) {message}
        }
        """
        return query

    @staticmethod
    def list_ontology_tables() -> str:
        """
        Query to list the ontology tables in the CatalogueOntologies database.
        """
        query = """
        {
          _schema {
            tables {
              name
            }
          }
        }"""
        return query

    @staticmethod
    def list_ontology_terms(table: str) -> str:
        """
        Query to list the terms in an ontology.
        :param table: the name of the table from which the list is requested.
        """
        query = """
        {
          """ + table + """ {
            name
          }
        }
        """
        return query
