
class graphql:
    """GraphQL
    Methods for interacting with the GraphQL API
    """

    @staticmethod
    def signin():
        """signin
        Mutation to sign into an EMX2 molgenis instance. Function must include
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
            mutation($email:String, $password: String) {
                signin(email: $email, password: $password) {
                    status
                    message
                    token
                }
            }
        """

    @staticmethod
    def signout():
        """Signout
        Sign out and close the session
        """
        return """
            mutation {
                signout {
                    status
                    message
                }
            }
        """

    @staticmethod
    def listSchemas():
        """List Schemas
        View all available schemas
        """
        return """
            {
                _schemas {
                    name
                    description
                }
            }
        """