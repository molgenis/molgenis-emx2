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
            order
            label
            parent {
              name
            }
            codesystem
            code
            ontologyTermURI
            definition
          }
        }
        """
        return query

    @staticmethod
    def list_databases() -> str:
        """
        Query to list the databases on the server.
        """
        query = """
        {
          _schemas {
            name
          }
        }"""

        return query

    @staticmethod
    def database_schema() -> str:
        """
        Query to list the schema of a database.
        """
        query = """
        {
          _schema {
            tables {
              name
              externalSchema
              inherit
              tableType
              columns {
                name 
                required
                key
                columnType 
                refSchema
                refTable
              }      
            }
          }
        }"""
        return query

    @staticmethod
    def column_values(table: str, column: str, pkeys: list):
        """Query to list the values in a column.
        :param table: the name of the table
        :param column: the name of the column
        :param pkeys: the list of columns with primary key in the column's data table
        """
        if column in pkeys:
            pkeys = [pkey + ' {name}' if pkey == column else pkey for pkey in pkeys]
        else:
            pkeys.append(column + ' {name}')
        query = """
        query """ + table + """($filter: """ + table + """Filter) {
          """ + table + """(filter:$filter) {""" + ', '.join(pkeys) + """}
        }
        """

        return query

    @staticmethod
    def upload_mutation(table: str):
        """Query to perform an update in the data in a table column.
        :param table: the name of the table
        """
        query = """
        mutation update($value:[""" + table + """Input]) {
          update(""" + table + """:$value) {message} 
        }
        """
        return query

    @staticmethod
    def search_filter_query(table: str):
        """Query to search for the presence of a term in the table.
        :param table: the name of the table
        """
        query = """
        query """ + table + """($filter: """ + table + """Filter) {
          """ + table + """(filter:$filter) {name}
        }
        """
        return query
