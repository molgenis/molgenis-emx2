"""
Functions to generate GraphQL queries to be deployed
on an EMX2 server by the Molgenis EMX2 Python client.
"""


def signin():
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


def signout():
    """GraphQL mutation to sign out and close the session."""
    return """
        mutation {
            signout {
                status
                message
            }
        }
    """


def create_schema():
    """GraphQL query to create a new schema. Function must contain name in the
    posted data.
    
    @example
    ```py
    import requests
    requests.post(...,
        json={
            'query': graphql.create_schema(),
            'variables': {'name': ...}
        }
    )
    ```
    """
    return """
        mutation(
            $name: String,
            $description: String,
            $template: String,
            $includeDemoData: Boolean
        ) {
            createSchema(
                name: $name,
                description: $description,
                template: $template,
                includeDemoData: $includeDemoData
              ) {
                  status
                  message
                  taskId
            }
        }
    """


def delete_schema():
    """GraphQL query for deleting a schema. Function must include the name of
    the schema
    
    @example
    ```py
    import requests
    requests.post(...,
        json={
            'query': graphql.delete_schema(),
            'variables': {'id': ...}
        }
    )
    ```
    """
    return """
        mutation($id:String) {
            deleteSchema(id: $id) {
                status
                message
                taskId
            }
        }
    """


def update_schema():
    """GraphQL query to update a schema description. Function must include
    the name of the schema and description in the posted data.
    
    @example
    ```py
    import requests
    requests.post(...,
        json={
            'query': graphql.update_schema(),
            'variables': {'name': ..., 'description', ...}
        }
    )
    ```
    """
    return """
        mutation($name: String, $description: String) {
            updateSchema(name: $name, description: $description) {
                status
                message
                taskId
            }
        }
    """


def list_schemas():
    """GraphQL query to view all available schemas."""
    return """
        {
            _schemas {
                id
                name
                label
                description
            }
        }
    """


def list_schema_meta():
    """GraphQL query to view metadata about a schema including
    the definition of tables and columns, as well as schema settings and members.
    """
    return """
      { 
        _schema {
            id
            name
            label
            tables {
                name
                label
                description
                labels {
                    locale
                    value
                }
                id
                schemaName
                schemaId
                inheritName
                inheritId
                descriptions {
                    locale
                    value
                }
                columns {
                    table
                    name
                    description
                    labels {
                        locale
                        value
                    }
                    id
                    descriptions {
                        locale
                        value
                    }
                    position
                    columnType
                    inherited
                    key
                    required
                    defaultValue
                    refSchemaId
                    refSchemaName
                    refTableName
                    refTableId
                    refLinkName
                    refBackId
                    refLabel
                    validation
                    visible
                    readonly
                    computed
                    semantics
                }
                settings {
                    key
                    value
                }
                semantics
                tableType
            }
            members {
                email
                role
            }
            settings {
                key
                value
            }
            roles {
                name
            }
        }
      }
    """


def version_number():
    """GraphQL query to retrieve the server's version number."""
    return (
        """
        {
          _manifest {
            ImplementationVersion
            SpecificationVersion
            DatabaseVersion
          }
        }
        """
    )
