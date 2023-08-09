"""
Functions to generate GraphQL queries to be deployed
on an EMX2 server by the Molgenis EMX2 Python client.
"""


def sign_in():
    """
    GraphQL mutation to sign in to an EMX2 molgenis instance. Function must include
    username and password in the posted data.

    @example
    ```py
    import requests
    requests.post(...,
        json={
            'query': graphql.query,
            'variables': {'username': ..., 'password': ...}
        }
    )

    ```
    """
    return """
        mutation($email: String, $password: String) {
            signin(email: $email, password: $password) {
                status
                message
                token
            }
        }
    """


def sign_out():
    """GraphQL mutation to sign out and close the session."""
    return """
        mutation {
            signout {
                status
                message
            }
        }
    """


def list_schemas():
    """GraphQL query to view all available schemas."""
    return """
        {
            _schemas {
                name
                description
            }
        }
    """


def list_tables():
    """GraphQL query to list the tables in a schema."""
    return """
        {
          _schema {
            tables {
              name
            }
          }
        }
    """
