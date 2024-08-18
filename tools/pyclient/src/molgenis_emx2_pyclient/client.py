import csv
import io
import json
import logging
import pathlib
import time
from functools import cache
from typing import TypeAlias, Literal

import pandas as pd
import requests
from requests import Response

from . import graphql_queries as queries
from . import utils
from .exceptions import (NoSuchSchemaException, ServiceUnavailableError, SigninError,
                         ServerNotFoundError, PyclientException, NoSuchTableException,
                         NoContextManagerException, GraphQLException, InvalidTokenException,
                         PermissionDeniedException, TokenSigninException, NonExistentTemplateException)
from .metadata import Schema

log = logging.getLogger("Molgenis EMX2 Pyclient")

OutputFormat: TypeAlias = Literal['csv', 'xlsx']


class Client:
    """
    Use the Client object to log in to a Molgenis EMX2 server
    and perform operations on the server.
    """

    def __init__(self, url: str, schema: str = None, token: str = None) -> None:
        """
        Initializes a Client object with a server url.
        """
        self._as_context_manager = False
        self._token = token

        self.url: str = url
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

    @property
    def status(self):
        """Shows the sign-in status of the user, the server version
        and the schemas that the user can interact with.
        """
        schemas = '\n\t'.join(self.schema_names)
        host = self.url
        user = self.username if self.username else ('token' if self.token else 'anonymous')
        status = 'signed in' if self.signin_status == 'success' else ('session-less' if self.token else 'signed out')
        version = self.version
        message = (
            f"Host: {host}\n"
            f"User: {user}\n"
            f"Status: {status}\n"
            f"Schemas: \n\t{schemas}\n"
            f"Version: {version}\n"
        )
        return message

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

    @property
    def schema_names(self):
        """Returns a list of the names of the schemas."""
        return list(map(str, self.schemas))

    @property
    def token(self):
        """Returns the token by a property to prevent it being modified."""
        return self._token

    def set_token(self, token: str):
        """Sets the token supplied as the argument as the client's token."""
        if self.signin_status == 'success':
            raise TokenSigninException("Cannot set a token on a client authorized with sign in.")
        self._token = token

    @property
    def version(self):
        """Lists the current EMX2 version on the server"""
        query = queries.version_number()
        response = self.session.post(
            url=self.api_graphql,
            json={'query': query}
        )
        self._validate_graphql_response(response)
        return response.json().get('data').get('_manifest').get('SpecificationVersion')

    def save_schema(self, table: str, name: str = None, file: str = None, data: list | pd.DataFrame = None):
        """Imports or updates records in a table of a named schema.

        :param name: name of a schema
        :type name: str
        :param table: the name of the table
        :type table: str
        :param file: location of the file containing records to import or update
        :type file: str
        :param data: a dataset containing records to import or update (list of dictionaries)
        :type data: list

        :returns: status message or response
        :rtype: str
        """
        current_schema = name
        if current_schema is None:
            current_schema = self.default_schema

        if current_schema not in self.schema_names:
            raise NoSuchSchemaException(f"Schema {current_schema!r} not available.")

        if not self._table_in_schema(table, current_schema):
            raise NoSuchTableException(f"Table {table!r} not found in schema {current_schema!r}.")

        import_data = self._prep_data_or_file(file_path=file, data=data)

        schema_metadata: Schema = self.get_schema_metadata(current_schema)
        table_id = schema_metadata.get_table(by='name', value=table).id

        response = self.session.post(
            url=f"{self.url}/{current_schema}/api/csv/{table_id}",
            headers={'Content-Type': 'text/csv'},
            data=import_data
        )

        try:
            self._validate_graphql_response(response)
            log.info("Imported data into %s::%s.", current_schema, table)
        except PyclientException:
            errors = '\n'.join([err['message'] for err in response.json().get('errors')])
            log.error("Failed to import data into %s::%s\n%s", current_schema, table, errors)
            raise PyclientException(errors)

    def upload_file(self, file_path: str | pathlib.Path, schema: str = None):
        """Uploads a file to a database on the EMX2 server.

        :param file_path: the path where the file is located.
        :type file_path: str or pathlib.Path object
        :param schema: the name of the schema where the file should be uploaded
        :type schema: str, default None

        :returns: status message or response
        :rtype: str
        """
        if not isinstance(file_path, pathlib.Path):
            file_path = pathlib.Path(file_path)
        if not file_path.exists():
            raise FileNotFoundError(f"No file found at {file_path!r}.")

        schema = schema if schema else self.default_schema
        if not schema:
            raise NoSuchSchemaException(f"Specify the schema where the file should be uploaded.")

        api_url = f"{self.url}/{schema}/api/"
        if file_path.suffix == '.csv':
            return self._upload_csv(file_path, schema)
        elif file_path.suffix == '.zip':
            api_url += "zip?async=true"
        elif file_path.suffix == '.xlsx':
            api_url += "excel?async=true"
        else:
            raise NotImplementedError(f"Uploading files with extension {file_path.suffix!r} is not supported.")

        with open(file_path, 'rb') as file:
            response = self.session.post(
                url=api_url,
                files={'file': file}
            )

        # Check if status is OK
        log.info(response.status_code)

        if response.status_code != 200:
            msg = '\n'.join([err['message'] for err in response.json().get('errors')])
            log.error(msg)
            raise PyclientException(msg)

        # Catch process URL
        process_id = response.json().get('id')

        # Report subtask progress
        p_response = self.session.post(
            url=self.api_graphql,
            json={'query': queries.task_status(process_id)}
        )
        if p_response.status_code != 200:
            raise PyclientException("Error uploading file")

        reported_tasks = []
        task = p_response.json().get('data').get('_tasks')[0]
        while (status := task.get('status')) != 'COMPLETED':
            if status == 'ERROR':
                # TODO improve error handling
                raise PyclientException(f"Error uploading file: {task.get('description')}")
            subtasks = task.get('subTasks', [])
            for st in subtasks:
                if st['id'] not in reported_tasks and st['status'] == 'RUNNING':
                    log.info(f"Subtask: {st['description']}")
                    reported_tasks.append(st['id'])
                if st['id'] not in reported_tasks and st['status'] == 'SKIPPED':
                    log.warning(f"    Subtask: {st['description']}")
                    reported_tasks.append(st['id'])
                for sst in st.get('subTasks', []):
                    if sst['id'] not in reported_tasks and sst['status'] == 'COMPLETED':
                        log.info(f"    Subsubtask: {sst['description']}")
                        reported_tasks.append(sst['id'])
                    if sst['id'] not in reported_tasks and sst['status'] == 'SKIPPED':
                        log.warning(f"    Subsubtask: {sst['description']}")
                        reported_tasks.append(sst['id'])
            try:
                p_response = self.session.post(
                    url=self.api_graphql,
                    json={'query': queries.task_status(process_id)}
                )
                task = p_response.json().get('data').get('_tasks')[0]
            except AttributeError:
                time.sleep(1)
                p_response = self.session.post(
                    url=self.api_graphql,
                    json={'query': queries.task_status(process_id)}
                )
                task = p_response.json().get('data').get('_tasks')[0]
        log.info(f"Completed task: {task.get('description')}")

    def _upload_csv(self, file_path: pathlib.Path, schema: str) -> str:
        """Uploads the CSV file from the filename to the schema. Returns the success or error message."""
        file_name = file_path.name
        if not file_name.startswith('molgenis'):
            table = file_name.split(file_path.suffix)[0]
            return self.save_schema(table=table, name=schema, file=str(file_path))
        api_url = f"{self.url}/{schema}/api/csv"
        data = self._prep_data_or_file(file_path=str(file_path))

        response = self.session.post(
            url=api_url,
            data=data,
            headers={'Content-Type': 'text/csv'}
        )
        if response.status_code == 200:
            msg = response.text
            log.info(f"{response.text}")
        else:
            msg = '\n'.join([err['message'] for err in response.json().get('errors')])
            log.error(msg)
            raise PyclientException(msg)
        return msg

    def delete_records(self, table: str, schema: str = None, file: str = None, data: list | pd.DataFrame = None):
        """Deletes records from a table.

        :param schema: name of a schema
        :type schema: str
        :param table: the name of the table
        :type table: str
        :param file: location of the file containing records to import or update
        :type file: str
        :param data: a dataset containing records to delete (list of dictionaries)
        :type data: list

        :returns: status message or response
        :rtype: str
        """
        current_schema = schema
        if current_schema is None:
            current_schema = self.default_schema

        if current_schema not in self.schema_names:
            raise NoSuchSchemaException(f"Schema {current_schema!r} not available.")

        if not self._table_in_schema(table, current_schema):
            raise NoSuchTableException(f"Table {table!r} not found in schema {current_schema!r}.")

        import_data = self._prep_data_or_file(file_path=file, data=data)

        schema_metadata: Schema = self.get_schema_metadata(current_schema)
        table_id = schema_metadata.get_table(by='name', value=table).id

        response = self.session.delete(
            url=f"{self.url}/{current_schema}/api/csv/{table_id}",
            headers={'Content-Type': 'text/csv'},
            data=import_data
        )

        self._validate_graphql_response(response, mutation='delete',
                                        fallback_error_message=f"Failed to delete data from {current_schema}::{table}.")

        if response.status_code == 200:
            log.info("Deleted data from %s::%s.", current_schema, table)
        else:
            errors = '\n'.join([err['message'] for err in response.json().get('errors')])
            log.error("Failed to delete data from %s::%s\n%s.", current_schema, table, errors)

    def get(self, table: str, query_filter: str = None, schema: str = None, as_df: bool = False) -> list | pd.DataFrame:
        """Retrieves data from a schema and returns as a list of dictionaries or as
        a pandas DataFrame (as pandas is used to parse the response).

        :param schema: name of a schema
        :type schema: str
        :param query_filter: the query to filter the output
        :type query_filter: str
        :param table: the name of the table
        :type table: str
        :param as_df: if True, the response will be returned as a
                      pandas DataFrame. Otherwise, a recordset will be returned.
        :type as_df: bool

        :returns: list of dictionaries, status message or data frame
        :rtype: list | pd.DataFrame
        """
        current_schema = schema
        if current_schema is None:
            current_schema = self.default_schema

        if current_schema not in self.schema_names:
            raise NoSuchSchemaException(f"Schema {current_schema!r} not available.")

        if not self._table_in_schema(table, current_schema):
            raise NoSuchTableException(f"Table {table!r} not found in schema {current_schema!r}.")

        schema_metadata: Schema = self.get_schema_metadata(current_schema)
        table_id = schema_metadata.get_table(by='name', value=table).id

        filter_part = self._prepare_filter(query_filter, table, schema)
        query_url = f"{self.url}/{current_schema}/api/csv/{table_id}{filter_part}"
        response = self.session.get(url=query_url)

        self._validate_graphql_response(response=response,
                                        fallback_error_message=f"Failed to retrieve data from {current_schema}::"
                                                               f"{table!r}.\nStatus code: {response.status_code}.")

        response_data = pd.read_csv(io.BytesIO(response.content), keep_default_na=False)

        if not as_df:
            return response_data.to_dict('records')
        return response_data

    def export(self, schema: str = None, table: str = None, fmt: OutputFormat = 'csv'):
        """Exports data from a schema to a file in the desired format.

        :param schema: the name of the schema
        :type schema: str
        :param table: the name of the table
        :type table: str
        :param fmt: the export format of the schema and or table (csv or xlsx)
        :type fmt: str

        """
        current_schema = schema if schema is not None else self.default_schema
        if current_schema not in self.schema_names:
            raise NoSuchSchemaException(f"Schema {current_schema!r} not available.")

        if table is not None and not self._table_in_schema(table, current_schema):
            raise NoSuchTableException(f"Table {table!r} not found in schema {current_schema!r}.")

        schema_metadata: Schema = self.get_schema_metadata(current_schema)

        if fmt == 'xlsx':
            if table is None:
                # Export the whole schema
                url = f"{self.url}/{current_schema}/api/excel"
                response = self.session.get(url=url)
                self._validate_graphql_response(response)

                filename = f"{current_schema}.xlsx"
                with open(filename, "wb") as file:
                    file.write(response.content)
                log.info("Exported data from schema %s to '%s'.", current_schema, filename)
            else:
                # Export the single table
                table_id = schema_metadata.get_table(by='name', value=table).id
                url = f"{self.url}/{current_schema}/api/excel/{table_id}"
                response = self.session.get(url=url)
                self._validate_graphql_response(response)

                filename = f"{table}.xlsx"
                with open(filename, "wb") as file:
                    file.write(response.content)
                log.info("Exported data from table %s in schema %s to '%s'.", table, current_schema, filename)

        if fmt == 'csv':
            if table is None:
                url = f"{self.url}/{current_schema}/api/zip"
                response = self.session.get(url=url)
                self._validate_graphql_response(response)

                filename = f"{current_schema}.zip"
                with open(filename, "wb") as file:
                    file.write(response.content)
                log.info("Exported data from schema %s to '%s'.", current_schema, filename)
            else:
                # Export the single table
                table_id = schema_metadata.get_table(by='name', value=table).id
                url = f"{self.url}/{current_schema}/api/csv/{table_id}"
                response = self.session.get(url=url)
                self._validate_graphql_response(response)

                filename = f"{table}.csv"
                with open(filename, "wb") as file:
                    file.write(response.content)
                log.info("Exported data from table %s in schema %s to '%s'.", table, current_schema, filename)

    def create_schema(self, name: str = None,
                      description: str = None,
                      template: str = None,
                      include_demo_data: bool = None):
        """Creates a new schema on the EMX2 server.

        :param name: the name of the new schema
        :type name: str
        :param description: additional text that provides context for a schema
        :type description: str
        :param template: (optional) the name of a template to set as the schema
        :type template: str
        :param include_demo_data: If true and a template schema is selected,
                                any example data will be loaded into the schema
        :type include_demo_data: bool

        :returns: a success or error message
        :rtype: string
        """
        if name in self.schema_names:
            raise PyclientException(f"Schema with name {name!r} already exists.")
        query = queries.create_schema()
        variables = self._format_optional_params(name=name, description=description,
                                                 template=template, include_demo_data=include_demo_data)

        response = self.session.post(
            url=self.api_graphql,
            json={'query': query, 'variables': variables}
        )

        self._validate_graphql_response(
            response=response,
            mutation='createSchema',
            fallback_error_message=f"Failed to create schema {name!r}"
        )
        self.schemas = self.get_schemas()
        log.info(f"Created schema {name!r}")

    def delete_schema(self, name: str = None):
        """Deletes a schema from the EMX2 server.

        :param name: the name of the new schema
        :type name: str

        :returns: a success or error message
        :rtype: string
        """
        current_schema = name if name is not None else self.default_schema
        if current_schema not in self.schema_names:
            raise NoSuchSchemaException(f"Schema {current_schema!r} not available.")

        query = queries.delete_schema()
        variables = {'id': current_schema}

        response = self.session.post(
            url=self.api_graphql,
            json={'query': query, 'variables': variables}
        )

        self._validate_graphql_response(
            response=response,
            mutation='deleteSchema',
            fallback_error_message=f"Failed to delete schema {current_schema!r}"
        )
        self.schemas = self.get_schemas()
        log.info(f"Deleted schema {current_schema!r}")

    def update_schema(self, name: str = None, description: str = None):
        """Updates a schema's description.

        :param name: the name of the new schema
        :type name: str
        :param description: additional text that provides context for a schema
        :type description: str

        :returns: a success or error message
        :rtype: string
        """
        current_schema = name if name is not None else self.default_schema
        if current_schema not in self.schema_names:
            raise NoSuchSchemaException(f"Schema {current_schema!r} not available.")

        query = queries.update_schema()
        variables = {'name': current_schema, 'description': description}

        response = self.session.post(
            url=self.api_graphql,
            json={'query': query, 'variables': variables}
        )

        self._validate_graphql_response(
            response=response,
            mutation='updateSchema',
            fallback_error_message=f"Failed to update schema {current_schema!r}"
        )
        self.schemas = self.get_schemas()

    def recreate_schema(self, name: str = None,
                        description: str = None,
                        template: str = None,
                        include_demo_data: bool = None):
        """Recreates a schema on the EMX2 server by deleting and subsequently
        creating it without data on the EMX2 server.

        :param name: the name of the new schema
        :type name: str
        :param description: additional text that provides context for a schema
        :type description: str
        :param template: (optional) the name of a template to set as the schema
        :type template: str
        :param include_demo_data: If true and a template schema is selected,
                                any example data will be loaded into the schema
        :type include_demo_data: bool

        :returns: a success or error message
        :rtype: string
        """
        current_schema = name if name is not None else self.default_schema
        if current_schema not in self.schema_names:
            raise NoSuchSchemaException(f"Schema {current_schema!r} not available.")

        schema_meta = [db for db in self.schemas if db.name == current_schema][0]
        schema_description = description if description else schema_meta.get('description', None)

        try:
            self.delete_schema(name=current_schema)
            self.create_schema(
                name=current_schema,
                description=schema_description,
                template=template,
                include_demo_data=include_demo_data
            )

        except GraphQLException:
            message = f"Failed to recreate {current_schema!r}"
            log.error(message)

        self.schemas = self.get_schemas()

    @cache
    def get_schema_metadata(self, name: str = None) -> Schema:
        """Retrieves a schema's metadata and returns it in a metadata.Schema object.

        :param name: the name of the schema
        :type name: str

        :returns: metadata of the schema
        :rtype: metadata.Schema
        """
        current_schema = name if name is not None else self.default_schema
        if current_schema not in self.schema_names:
            raise NoSuchSchemaException(f"Schema {current_schema!r} not available.")

        query = queries.list_schema_meta()
        response = self.session.post(
            url=f"{self.url}/{current_schema}/api/graphql",
            json={'query': query},
            headers={'x-molgenis-token': self.token}
        )
        self._validate_graphql_response(response)

        response_json = response.json()

        if 'id' not in response_json.get('data').get('_schema'):
            message = f"Unable to retrieve metadata for schema {current_schema!r}"
            log.error(message)
            raise GraphQLException(message)

        metadata = Schema(**response_json.get('data').get('_schema'))
        return metadata

    def _prepare_filter(self, expr: str, _table: str, _schema: str) -> str:
        """Prepares a GraphQL filter based on the expression passed into `get`."""
        if expr in [None, ""]:
            return ""
        statements = expr.split(' and ')
        _filter = dict()
        for stmt in statements:
            if '==' in stmt:
                _filter.update(**self.__prepare_equals_filter(stmt, _table, _schema))
            elif '>' in stmt:
                _filter.update(**self.__prepare_greater_filter(stmt, _table, _schema))
            elif '<' in stmt:
                _filter.update(**self.__prepare_smaller_filter(stmt, _table, _schema))
            elif '!=' in stmt:
                _filter.update(**self.__prepare_unequal_filter(stmt, _table, _schema))
            elif 'between' in stmt:
                _filter.update(**self.__prepare_between_filter(stmt, _table, _schema))
            else:
                raise ValueError(f"Cannot process statement {stmt!r}, "
                                 f"ensure specifying one of the operators '==', '>', '<', '!=', 'between' "
                                 f"in your statement.")
        return "?filter=" + json.dumps(_filter)

    def __prepare_equals_filter(self, stmt: str, _table: str, _schema: str) -> dict:
        """Prepares the filter part if the statement filters on equality."""
        _col = stmt.split('==')[0].strip()
        _val = stmt.split('==')[1].strip()

        col_id = ''.join(_col.split('`'))

        if '.' in col_id:
            return self.__prepare_nested_filter(col_id, _val, "equals")

        schema = self.get_schema_metadata(_schema)
        col = schema.get_table(by='name', value=_table).get_column(by='id', value=col_id)
        match col.get('columnType'):
            case 'BOOL':
                val = False
                if str(_val).lower() == 'true':
                    val = True
            case _:
                try:
                    val = json.loads(''.join(_val.split('`')).replace("'", '"'))
                except json.decoder.JSONDecodeError:
                    val = ''.join(_val.split('`'))

        return {col.id: {'equals': val}}

    def __prepare_greater_filter(self, stmt: str, _table: str, _schema: str) -> dict:
        """Prepares the filter part if the statement filters on greater than."""
        exclusive = '=' not in stmt
        stmt = stmt.replace('=', '')

        _col = stmt.split('>')[0].strip()
        _val = stmt.split('>')[1].strip()

        col_id = ''.join(_col.split('`'))

        schema = self.get_schema_metadata(_schema)
        col = schema.get_table(by='name', value=_table).get_column(by='id', value=col_id)

        match col.get('columnType'):
            case 'INT':
                val = int(_val) + 1 * exclusive
            case 'DECIMAL':
                val = float(_val) + 0.0000001 * exclusive
            case _:
                raise NotImplementedError(f"Cannot perform filter '>' on column with type {col.get('columnType')}.")

        return {col.id: {"between": [val, None]}}

    def __prepare_smaller_filter(self, stmt: str, _table: str, _schema: str) -> dict:
        """Prepares the filter part if the statement filters on greater than."""
        exclusive = '=' not in stmt
        stmt = stmt.replace('=', '')

        _col = stmt.split('<')[0].strip()
        _val = stmt.split('<')[1].strip()

        col_id = ''.join(_col.split('`'))

        schema = self.get_schema_metadata(_schema)
        col = schema.get_table(by='name', value=_table).get_column(by='id', value=col_id)

        match col.get('columnType'):
            case 'INT':
                val = int(_val) - 1 * exclusive
            case 'DECIMAL':
                val = float(_val) - 0.0000001 * exclusive
            case _:
                raise NotImplementedError(f"Cannot perform filter '<' on column with type {col.get('columnType')}.")

        return {col.id: {"between": [None, val]}}

    def __prepare_unequal_filter(self, stmt: str, _table: str, _schema: str) -> dict:
        """Prepares the filter part if the statement filters on greater than."""
        _col = stmt.split('!=')[0].strip()
        _val = stmt.split('!=')[1].strip()

        col_id = ''.join(_col.split('`'))

        if '.' in col_id:
            return self.__prepare_nested_filter(col_id, _val, "not_equals")

        schema = self.get_schema_metadata(_schema)
        col = schema.get_table(by='name', value=_table).get_column(by='id', value=col_id)

        match col.get('columnType'):
            case _:
                try:
                    val = json.loads(''.join(_val.split('`')).replace("'", '"'))
                except json.decoder.JSONDecodeError:
                    val = ''.join(_val.split('`'))

        return {col.id: {"not_equals": val}}

    def __prepare_between_filter(self, stmt: str, _table: str, _schema: str) -> dict:
        """Prepares the filter part if values between a certain range are requested."""
        stmt.replace('=', '')
        _col = stmt.split('between')[0].strip()
        _val = stmt.split('between')[1].strip()

        try:
            val = json.loads(_val)
        except json.decoder.JSONDecodeError as e:
            msg = ("To filter on values between a and b, supply them as a list, [a, b]. "
                   "Ensure the values for a and b are numeric.")
            raise ValueError(msg)
        col_id = ''.join(_col.split('`'))

        schema = self.get_schema_metadata(_schema)
        col = schema.get_table(by='name', value=_table).get_column(by='id', value=col_id)
        if (col_type := col.get('columnType')) not in ['INT', 'DECIMAL']:
            raise NotImplementedError(f"The filter 'between' is not implemented for columns of type {col_type!r}.")

        return {col.id: {'between': val}}

    @staticmethod
    def __prepare_nested_filter(columns: str, value: str | int | float | list, comparison: str):
        _filter = {}
        current = _filter
        for (i, segment) in enumerate(columns.split('.')[:-1]):
            current[segment] = {}
            current = current[segment]
        last_segment = columns.split('.')[-1]
        current[last_segment] = {comparison: value}
        return _filter

    @staticmethod
    def _prep_data_or_file(file_path: str = None, data: list | pd.DataFrame = None) -> str | None:
        """Prepares the data from memory or loaded from disk for addition or deletion action.

        :param file_path: path to the file to be prepared
        :type file_path: str
        :type file_path: str
        :param data: data to be prepared
        :type data: list

        :returns: prepared data in dataframe format
        :rtype: pd.DataFrame
        """

        if file_path is not None:
            return utils.read_file(file_path=file_path)

        if data is not None:
            if isinstance(data, pd.DataFrame):
                return data.to_csv(index=False, quoting=csv.QUOTE_NONNUMERIC, encoding='UTF-8')
            else:
                return pd.DataFrame(data, dtype=str).to_csv(index=False, quoting=csv.QUOTE_NONNUMERIC, encoding='UTF-8')

        message = "No data to import. Specify a file location or a dataset."
        log.error(message)
        raise FileNotFoundError(message)

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
                raise GraphQLException(msg)
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

    @staticmethod
    def _format_optional_params(**kwargs):
        """Parses optional keyword arguments to a format suitable for GraphQL queries."""
        keys = kwargs.keys()
        args = {key: kwargs[key] for key in keys if (key != 'self') and (key is not None)}
        if 'name' in args.keys():
            args['name'] = args.pop('name')
        if 'include_demo_data' in args.keys():
            args['includeDemoData'] = args.pop('include_demo_data')
        return args

    def _table_in_schema(self, table_name: str, schema_name: str) -> bool:
        """Checks whether the requested table is present in the schema.

        :param table_name: the name of the table
        :type table_name: str
        :param schema_name: the name of the schema
        :type schema_name: str
        :returns: boolean indicating whether table is present
        :rtype: bool
        """
        schema_data = self.get_schema_metadata(schema_name)
        if not hasattr(schema_data, 'tables'):
            return False
        if table_name in map(str, schema_data.tables):
            return True
        return False

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

