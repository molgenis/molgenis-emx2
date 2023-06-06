class Queries:
    """Class containing methods that generate valid GraphQL queries based on input."""

    @staticmethod
    def insert(table):
        """
        Query for inserting a term into a table.
        :param table: the name of the table in which the term is inserted.
        """
        query = """
        mutation insert($value:[""" + table + """Input]) {
          insert(""" + table + """:$value){message}
        }"""
        return query
