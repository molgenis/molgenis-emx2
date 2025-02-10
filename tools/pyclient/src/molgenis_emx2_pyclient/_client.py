import logging
import os
from typing import Optional

import requests
from requests import Response

from . import graphql_queries as queries
from .exceptions import (ServiceUnavailableError, ServerNotFoundError, PyclientException, GraphQLException,
                         InvalidTokenException,
                         PermissionDeniedException, NonExistentTemplateException,
                         ReferenceException, NoSuchSchemaException, SigninError, NoContextManagerException,
                         TokenSigninException)
from .metadata import Schema

logging.getLogger("requests").setLevel(logging.WARNING)
logging.getLogger("urllib3").setLevel(logging.WARNING)
log = logging.getLogger("Molgenis EMX2 Pyclient")

MOLGENIS_TOKEN = "MOLGENIS_TOKEN"

class _Client(object):
    def __init__(self,
                 url: str,
                 schema: Optional[str] = None,
                 token: Optional[str] = os.getenv(MOLGENIS_TOKEN),
                 job: Optional[str] = None):
        self._as_context_manager = False
        self._token = token
        self._job = job

        self.url: str = url if not url.endswith('/') else url[:-1]
        self.api_graphql = self.url + "/api/graphql"

        self.signin_status: str = 'unknown'
        self.username: str | None = None

        self.session: requests.Session = requests.Session()
        self.session.headers = {'x-molgenis-token': self.token}
        self._validate_url()

        self.schemas: list = self.get_schemas()
        self.default_schema: str = self.set_schema(schema)

    def __repr__(self):
        class_name = type(self).__name__
        _attrs = [['url', self.url]]
        if self.default_schema:
            _attrs.append(['schema', self.default_schema])
        dict_items = [f"{k}={v!r}" for k, v in _attrs]
        return f"{class_name}({', '.join(dict_items)})"

    def __str__(self):
        return self.url

    def __enter__(self):
        self._as_context_manager = True
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.signin_status == 'success':
            self.signout()
        self.session.close()

    async def __aenter__(self):
        self._as_context_manager = True
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        if self.signin_status == 'success':
            self.signout()
        self.session.close()



    def signin(self, username: str, password: str):
        """Signs in to the EMX2 server and retrieves session cookie.

        :param username: the username or email address for an account on this server
        :type username: str
        :param password: the password corresponding to this username.
        :type username: str
        """
        self.username = username

        if self.token is not None:
            raise TokenSigninException("Cannot sign in to client authorized with token.")

        if not self._as_context_manager:
            raise NoContextManagerException("Ensure the Client is called as a context manager,\n"
                                            "e.g. `with Client(url) as client:`")
        query = queries.signin()
        variables = {'email': self.username, 'password': password}

        response = self.session.post(
            url=self.api_graphql,
            json={'query': query, 'variables': variables}
        )
        self._validate_graphql_response(response, mutation='signin')

        response_json: dict = response.json().get('data', {}).get('signin', {})

        if response_json.get('status') == 'SUCCESS':
            self.signin_status = 'success'
            message = f"User {self.username!r} is signed in to {self.url!r}."
            log.info(message)
        elif response_json.get('status') == 'FAILED':
            self.signin_status = 'failed'
            message = f"Error: Unable to sign in to {self.url} as {self.username}." \
                      f"\n{response_json.get('message')}"
            log.error(message)
            raise SigninError(message)
        else:
            self.signin_status = 'failed'
            message = f"Error: Unable to sign in to {self.url} as {self.username}." \
                      f"\n{response_json.get('message')}"
            log.error(message)
            raise SigninError(message)
        self.schemas = self.get_schemas()

    def signout(self):
        """Signs the client out of the EMX2 server."""
        response = self.session.post(
            url=self.api_graphql,
            json={'query': queries.signout()}
        )
        self._validate_graphql_response(response)

        status = response.json().get('data', {}).get('signout', {}).get('status')
        if status == 'SUCCESS':
            log.info(f"User {self.username!r} is signed out of {self.url!r}.")
            self.signin_status = 'signed out'
        else:
            log.error(f"Unable to sign out of {self.url}.")
            message = response.json().get('errors')[0].get('message')
            log.error(message)

    def get_schemas(self) -> list[Schema]:
        """Returns the schemas on the database for this user as a list of dictionaries
        containing for each schema the id, name, label and description.
        """
        query = queries.list_schemas()

        response = self.session.post(
            url=self.api_graphql,
            json={'query': query}
        )
        self._validate_graphql_response(response)

        response_json: dict = response.json()
        schemas = [Schema(**s) for s in response_json['data']['_schemas']]
        return schemas


    def set_schema(self, name: str) -> str:
        """Sets the default schema to the schema supplied as argument.
        Raises NoSuchSchemaException if the schema cannot be found on the server.

        :param name: name of a schema
        :type name: str

        :returns: a schema name
        :rtype: str
        """
        if name not in [*self.schema_names, None]:
            raise NoSuchSchemaException(f"Schema {name!r} not available.")
        self.default_schema = name

        return name

    @property
    def schema_names(self):
        """Returns a list of the names of the schemas."""
        return list(map(str, self.schemas))

    @property
    def token(self):
        """Returns the token by a property to prevent it being modified."""
        return self._token


    def _validate_url(self):
        """
        Checks whether the URL provided is correct and refers to an EMX2 server.
        Raises ServerNotFoundError if not, depending on the error.
        """
        try:
            self.session.head(self.url)
        except requests.exceptions.SSLError:
            raise ServerNotFoundError(f"URL {self.url!r} cannot be found. Ensure the spelling is correct.")
        except requests.exceptions.InvalidSchema:
            if not self.url.startswith('https://'):
                raise ServerNotFoundError(f"No connection adapters were found for {self.url!r}. "
                                          f"Perhaps you meant 'https://{self.url}'?")
            raise ServerNotFoundError(f"No connection adapters were found for {self.url!r}.")
        except requests.exceptions.MissingSchema:
            raise ServerNotFoundError(f"Invalid URL {self.url!r}. "
                                      f"Perhaps you meant 'https://{self.url}'?")



    def _validate_graphql_response(self, response: Response, mutation: str = None, fallback_error_message: str = None):
        """Validates a GraphQL response and prints the appropriate message.

        :param response: a graphql response from the server
        :type response: requests.Response
        :param mutation: the name of the graphql mutation executed, optional
        :type mutation: str
        :param fallback_error_message: a fallback error message, optional
        :type fallback_error_message: str

        :returns: a success or error message
        :rtype: string
        """

        if response.status_code == 503:
            raise ServiceUnavailableError(f"Server with url {self.url!r} (temporarily) unavailable.")
        if response.status_code == 404:
            raise ServerNotFoundError(f"Server with url {self.url!r} not found.")
        if response.status_code == 400:
            if 'Invalid token or token expired' in response.text:
                raise InvalidTokenException("Invalid token or token expired.")
            if 'permission denied' in response.text:
                raise PermissionDeniedException(f"Transaction failed: permission denied.")
            if 'Graphql API error' in response.text:
                msg = response.json().get("errors", [])[0].get('message')
                log.error(msg)
                raise GraphQLException(msg)
            if "violates foreign key constraint" in response.text:
                msg = response.json().get("errors", [])[0].get('message', '')
                log.error(msg)
                raise ReferenceException(msg)

            msg = response.json().get("errors", [])[0].get('message', '')
            log.error(msg)
            raise PyclientException("An unknown error occurred when trying to reach this server.")

        if response.request.method == 'GET':
            return

        if response.status_code == 200:
            return

        response_json = response.json()
        response_keys = response_json.keys()
        if 'errors' not in response_keys and 'data' not in response_keys:
            message = fallback_error_message
            log.error(message)

        elif 'errors' in response_keys:
            message = response_json.get('errors')[0].get('message')
            if 'permission denied' in message:
                log.error("Insufficient permissions for this operations.")
                raise PermissionDeniedException("Insufficient permissions for this operations.")
            if 'AvailableDataModels' in message:
                log.error("Selected template does not exist.")
                raise NonExistentTemplateException("Selected template does not exist.")
            log.error(message)
            raise GraphQLException(message)

        elif mutation is not None:
            if response_json.get('data').get(mutation).get('status') == 'SUCCESS':
                message = response_json.get('data').get(mutation).get('message')
                log.info(message)
            else:
                message = f"Failed to validate response for {mutation!r}"
                log.error(message)