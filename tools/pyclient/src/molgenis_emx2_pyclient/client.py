import json
import logging
import pathlib
import time
from functools import cache
from io import BytesIO
from warnings import warn

import pandas as pd
import requests

from . import graphql_queries as queries
from .constants import HEADING, DATE, DATETIME, SECTION, REF, RADIO, FILE, ONTOLOGY, SELECT
from .exceptions import (NoSuchSchemaException, ServiceUnavailableError, SigninError, SignoutError,
                         ServerNotFoundError, PyclientException, NoSuchTableException,
                         NoContextManagerException, GraphQLException, InvalidTokenException,
                         PermissionDeniedException, TokenSigninException, NonExistentTemplateException,
                         NoSuchColumnException, ReferenceException)
from .metadata import Schema, Table
from .utils import parse_nested_pkeys, convert_dtypes, prepare_filter, format_optional_params, prep_data_or_file, \
    check_schema

logging.getLogger("requests").setLevel(logging.WARNING)
logging.getLogger("urllib3").setLevel(logging.WARNING)
log = logging.getLogger("Molgenis EMX2 Pyclient")


class Client:
    """
    Use the Client object to log in to a Molgenis EMX2 server
    and perform operations on the server.
    """

    def __init__(self, url: str, schema: str = None, token: str = None, job: str = None) -> None:
        """
        Initializes a Client object with a server url.
        """
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
        if self.signin_status != "success":
            raise SignoutError("Could not sign out as user is not signed in.")
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

    def save_schema(self, table: str, name: str = None, file: str | pathlib.Path = None, data: list | pd.DataFrame = None):
        """
        Imports or updates records in a table of a named schema.
        Deprecated and replaced by `save_table`.
        """
        warn("`save_schema` is deprecated. Use `save_table` instead.")
        return self.save_table(table, name, file, data)

    def save_table(self, table: str, schema: str = None, file: str | pathlib.Path = None, data: list | pd.DataFrame = None):
        """Imports or updates records in a table of a named schema.

        :param table: the name of the table
        :type table: str
        :param schema: name of a schema
        :type schema: str
        :param file: location of the file containing records to import or update
        :type file: str
        :param data: a dataset containing records to import or update (list of dictionaries)
        :type data: list

        :returns: status message or response
        :rtype: str
        """
        current_schema = check_schema(schema, self.default_schema, self.schema_names)

        if not self._table_in_schema(table, current_schema):
            raise NoSuchTableException(f"Table {table!r} not found in schema {current_schema!r}.")

        import_data = prep_data_or_file(file_path=file, data=data)

        schema_metadata: Schema = self.get_schema_metadata(current_schema)
        table_id = schema_metadata.get_table(by='name', value=table).id

        response = self.session.post(
            url=f"{self.url}/{current_schema}/api/csv/{table_id}",
            headers={'Content-Type': 'text/csv'},
            data=import_data.encode('utf-8')
        )

        try:
            self._validate_graphql_response(response)
            log.info("Imported data into %s::%s.", current_schema, table)
        except PyclientException:
            errors = '\n'.join([err['message'] for err in response.json().get('errors')])
            log.error("Failed to import data into %s::%s\n%s", current_schema, table, errors)
            raise PyclientException(errors)

    async def upload_file(self, file_path: str | pathlib.Path, schema: str = None):
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

        schema = check_schema(schema, self.default_schema, self.schema_names)

        api_url = f"{self.url}/{schema}/api/"
        if file_path.suffix == '.csv':
            return self._upload_csv(file_path, schema)
        elif file_path.suffix == '.zip':
            api_url += "zip?async=true"
        elif file_path.suffix == '.xlsx':
            api_url += "excel?async=true"
        else:
            raise NotImplementedError(f"Uploading files with extension {file_path.suffix!r} is not supported.")

        if self._job:
            api_url += "&parentJob=" + self._job

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

        # Report on task progress
        await self._report_task_progress(process_id)

    def truncate(self, table: str, schema: str):
        """Truncates the table.

        :param table: the name of the table
        :type table: str
        :param schema: name of a schema
        :type schema: str
        """
        current_schema = check_schema(schema, self.default_schema, self.schema_names)

        if not self._table_in_schema(table, current_schema):
            raise NoSuchTableException(f"Table {table!r} not found in schema {current_schema!r}.")

        query_url = f"{self.url}/{current_schema}/graphql"
        table_id = self.get_schema_metadata(current_schema).get_table(by='name', value=table).id
        query = queries.truncate()

        response = self.session.post(
            url=query_url,
            json={"query": query, "variables": {"table": table_id}}
        )

        self._validate_graphql_response(response, mutation='truncate',
                                        fallback_error_message=f"Failed to truncate table {current_schema}::{table}.")
        log.info(f"Truncated table {table!r}.")



    def _upload_csv(self, file_path: pathlib.Path, schema: str) -> str:
        """Uploads the CSV file from the filename to the schema. Returns the success or error message."""
        file_name = file_path.name
        if not file_name.startswith('molgenis'):
            table = file_name.split(file_path.suffix)[0]
            return self.save_table(table=table, schema=schema, file=str(file_path))
        api_url = f"{self.url}/{schema}/api/csv"
        data = prep_data_or_file(file_path=str(file_path))

        if self._job:
            api_url += "?parentJob=" + self._job

        response = self.session.post(
            url=api_url,
            data=data,
            headers={'Content-Type': 'text/csv',
                     'fileName': file_name}
        )
        if response.status_code == 200:
            msg = response.text
            log.info(f"{response.text}")
        else:
            msg = '\n'.join([err['message'] for err in response.json().get('errors')])
            log.error(msg)
            raise PyclientException(msg)
        return msg

    def delete_records(self, table: str, schema: str = None, file: str | pathlib.Path = None, data: list | pd.DataFrame = None):
        """Deletes records from a table.

        :param table: the name of the table
        :type table: str
        :param schema: name of a schema
        :type schema: str
        :param file: location of the file containing records to import or update
        :type file: str
        :param data: a dataset containing records to delete (list of dictionaries)
        :type data: list

        :returns: status message or response
        :rtype: str
        """
        current_schema = check_schema(schema, self.default_schema, self.schema_names)

        if not self._table_in_schema(table, current_schema):
            raise NoSuchTableException(f"Table {table!r} not found in schema {current_schema!r}.")

        import_data = prep_data_or_file(file_path=file, data=data)

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

    def get(self,
            table: str,
            columns: list[str] = None,
            query_filter: str = None,
            schema: str = None,
            as_df: bool = False) -> list | pd.DataFrame:
        """Retrieves data from a table using the EMX2 CSV API and
        returns as a list of dictionaries or a pandas DataFrame.

        :param table: the name of the table
        :type table: str
        :param columns: list of column names to return, optional, default all columns
        :type columns: list[str]
        :param query_filter: the query to filter the output, optional
        :type query_filter: str
        :param schema: name of a schema, default self.default_schema
        :type schema: str
        :param as_df: if True, the response will be returned as a
                      pandas DataFrame. Otherwise, a recordset will be returned.
        :type as_df: bool

        :returns: list of dictionaries or pandas DataFrame
        :rtype: list | pd.DataFrame
        """
        current_schema = check_schema(schema, self.default_schema, self.schema_names)

        if not self._table_in_schema(table, current_schema):
            raise NoSuchTableException(f"Table {table!r} not found in schema {current_schema!r}.")

        schema_metadata: Schema = self.get_schema_metadata(current_schema)
        table_meta = schema_metadata.get_table(by='name', value=table)
        table_id = table_meta.id

        filter_part = prepare_filter(query_filter, table, schema_metadata)

        if filter_part:
            filter_part = "?filter=" + json.dumps(filter_part)
        else:
            filter_part = ""
        query_url = f"{self.url}/{current_schema}/api/csv/{table_id}{filter_part}"
        response = self.session.get(url=query_url)
        self._validate_graphql_response(response=response,
                                        fallback_error_message=f"Failed to retrieve data from {current_schema}::"
                                                               f"{table!r}.\nStatus code: {response.status_code}.")

        response_columns = pd.read_csv(BytesIO(response.content)).columns
        dtypes = {c: t for (c, t) in convert_dtypes(table_meta).items() if c in response_columns}

        bool_columns = [c for (c, t) in dtypes.items() if t == 'boolean']
        date_columns = [c.name for c in table_meta.columns
                        if c.get('columnType') in (DATE, DATETIME) and c.name in response_columns]
        response_data = pd.read_csv(BytesIO(response.content), keep_default_na=False, na_values=[''], dtype=dtypes, parse_dates=date_columns)

        response_data[bool_columns] = response_data[bool_columns].replace({'true': True, 'false': False})
        response_data = response_data.astype(dtypes)

        if columns:
            try:
                response_data = response_data[columns]
            except KeyError as e:
                if e.args[0].startswith("None of [Index(['"):
                    missing_cols = e.args[0].split("None of [Index([")[1].split("]")[0]
                    msg = f"Columns {missing_cols} not found."
                elif "not in index" in e.args[0]:
                    msg = f"Columns {e.args[0]}"
                else:
                    msg = f"Columns {e.args[0].split('Index(')[1].split(', dtype')} not in index."
                raise NoSuchColumnException(msg)
            response_data = response_data.drop_duplicates(keep='first').reset_index(drop=True)
        if not as_df:
            response_data = response_data.to_dict('records')

        return response_data

    def get_graphql(self,
                    table: str,
                    columns: list[str] = None,
                    query_filter: str = None,
                    schema: str = None):
        """Retrieves data from a schema using the GraphQL API and returns as a list of dictionaries.

        :param table: the name of the table
        :type table: str
        :param columns: list of column ids to return, optional, default all columns
        :type columns: list[str]
        :param query_filter: the query to filter the output, optional
        :type query_filter: str
        :param schema: name of a schema, default self.default_schema
        :type schema: str

        :returns: list of records
        :rtype: list[dict]"""

        current_schema = check_schema(schema, self.default_schema, self.schema_names)

        if not self._table_in_schema(table, current_schema):
            raise NoSuchTableException(f"Table {table!r} not found in schema {current_schema!r}.")

        schema_metadata: Schema = self.get_schema_metadata(current_schema)
        table_meta = schema_metadata.get_table(by='name', value=table)
        table_id = table_meta.id

        filter_part = prepare_filter(query_filter, table, schema_metadata)
        query_url = f"{self.url}/{current_schema}/graphql"

        query = self._parse_get_table_query(table_id, current_schema, columns)
        response = self.session.post(url=query_url,
                                    json={"query": query, "variables": {"filter": filter_part}})
        self._validate_graphql_response(response=response,
                                        fallback_error_message=f"Failed to retrieve data from {current_schema}::"
                                                               f"{table!r}.\nStatus code: {response.status_code}.")
        response_data = response.json().get('data').get(table_id, [])
        response_data = self._parse_ontology(response_data, table_id, schema)

        return response_data

    async def export(self, schema: str = None, table: str = None,
                     filename: str = None, as_excel: bool = False) -> BytesIO:
        """Exports data from a schema to a file in the desired format.

        :param schema: the name of the schema
        :type schema: str
        :param table: the name of the table
        :type table: str
        :param filename: the name of the file to which the data is to be exported, default None
        :type filename: str
        :param as_excel: specifies whether the Excel API is called for the export.
                         Ignored when parameter filename is specified, default False
        :type as_excel: bool
        """
        current_schema = check_schema(schema, self.default_schema, self.schema_names)

        if table is not None and not self._table_in_schema(table, current_schema):
            raise NoSuchTableException(f"Table {table!r} not found in schema {current_schema!r}.")

        schema_metadata: Schema = self.get_schema_metadata(current_schema)

        if filename:
            if filename.endswith('.xlsx'):
                fmt = 'xlsx'
            elif filename.endswith('.csv'):
                fmt = 'csv'
            elif filename.endswith('.zip'):
                fmt = 'csv'
            else:
                raise ValueError(f"File name must end with ('csv', 'xlsx', 'zip')")
        else:
            if as_excel:
                fmt = 'xlsx'
            else:
                fmt = 'csv'

        if fmt == 'xlsx':
            if table is None:
                # Export the whole schema
                url = f"{self.url}/{current_schema}/api/excel?async=true"
                response = self.session.get(url=url)
                self._validate_graphql_response(response)

                if filename:
                    with open(filename, "wb") as file:
                        file.write(response.content)
                    log.info("Exported data from schema %s to '%s'.", current_schema, filename)
                else:
                    log.info("Exported data from schema %s.", current_schema)
            else:
                # Export the single table
                table_id = schema_metadata.get_table(by='name', value=table).id
                url = f"{self.url}/{current_schema}/api/excel/{table_id}?async=true"
                response = self.session.get(url=url)
                self._validate_graphql_response(response)

                if filename:
                    with open(filename, "wb") as file:
                        file.write(response.content)
                    log.info("Exported data from table %s in schema %s to '%s'.", table, current_schema, filename)
                else:
                    log.info("Exported data from table %s in schema %s.", table, current_schema)
        else:
            if table is None:
                url = f"{self.url}/{current_schema}/api/zip?async=true"
                response = self.session.get(url=url)
                self._validate_graphql_response(response)

                if filename:
                    with open(filename, "wb") as file:
                        file.write(response.content)
                    log.info("Exported data from schema %s to '%s'.", current_schema, filename)
                else:
                    log.info("Exported data from schema %s.", current_schema)

            else:
                # Export the single table
                table_id = schema_metadata.get_table(by='name', value=table).id
                url = f"{self.url}/{current_schema}/api/csv/{table_id}?async=true"
                response = self.session.get(url=url)
                self._validate_graphql_response(response)

                if filename:
                    with open(filename, "wb") as file:
                        file.write(response.content)
                    log.info("Exported data from table %s in schema %s to '%s'.", table, current_schema, filename)
                else:
                    log.info("Exported data from table %s in schema %s.", table, current_schema)

        return BytesIO(response.content)

    async def create_schema(self, name: str,
                            description: str = None,
                            template: str = None,
                            include_demo_data: bool = False):
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
        variables = format_optional_params(name=name, description=description,
                                                 template=template, include_demo_data=include_demo_data,
                                                 parent_job=self._job)

        response = self.session.post(
            url=self.api_graphql,
            json={'query': query, 'variables': variables}
        )

        self._validate_graphql_response(
            response=response,
            mutation='createSchema',
            fallback_error_message=f"Failed to create schema {name!r}"
        )
        # Catch process URL
        process_id = response.json().get('data').get('createSchema').get('taskId')

        if process_id:
            # Report on task progress
            await self._report_task_progress(process_id)

        self.schemas = self.get_schemas()
        log.info(f"Created schema {name!r}")

    async def delete_schema(self, name: str = None):
        """Deletes a schema from the EMX2 server.

        :param name: the name of the new schema
        :type name: str

        :returns: a success or error message
        :rtype: string
        """
        current_schema = check_schema(name, self.default_schema, self.schema_names)

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
        current_schema = check_schema(name, self.default_schema, self.schema_names)

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

    async def recreate_schema(self, name: str = None,
                              description: str = None,
                              template: str = None,
                              include_demo_data: bool = False):
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
        current_schema = check_schema(name, self.default_schema, self.schema_names)

        schema_meta = [db for db in self.schemas if db.name == current_schema][0]
        schema_description = description if description else schema_meta.get('description', None)

        try:
            await self.delete_schema(name=current_schema)
            await self.create_schema(
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
        current_schema = check_schema(name, self.default_schema, self.schema_names)

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

    def get_schema_settings(self, name: str = None) -> list[dict]:
        """Retrieves the schema's settings and returns it as list of dictionaries."""
        current_schema = check_schema(name, self.default_schema, self.schema_names)

        query = queries.list_schema_settings()
        response = self.session.post(
            url=f"{self.url}/{current_schema}/api/graphql",
            json={'query': query},
            headers={'x-molgenis-token': self.token}
        )
        self._validate_graphql_response(response)

        response_json = response.json()
        settings = response_json.get('data').get('_schema').get('settings')

        return settings

    def get_schema_members(self, name: str = None) -> list[dict]:
        """Retrieves the schema's settings and returns it as a list of dictionaries."""
        current_schema = check_schema(name, self.default_schema, self.schema_names)

        query = queries.list_schema_members()
        response = self.session.post(
            url=f"{self.url}/{current_schema}/api/graphql",
            json={'query': query},
            headers={'x-molgenis-token': self.token}
        )
        self._validate_graphql_response(response)

        response_json = response.json()
        members = response_json.get('data').get('_schema').get('members')

        return members

    def get_schema_roles(self, name: str = None) -> list[dict]:
        """Retrieves the schema's settings and returns it as a list of dictionaries."""
        current_schema = check_schema(name, self.default_schema, self.schema_names)

        query = queries.list_schema_roles()
        response = self.session.post(
            url=f"{self.url}/{current_schema}/api/graphql",
            json={'query': query},
            headers={'x-molgenis-token': self.token}
        )
        self._validate_graphql_response(response)

        response_json = response.json()
        roles = response_json.get('data').get('_schema').get('roles')

        return roles

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

    async def _report_task_progress(self, process_id: int | str):
        """Reports on the progress of a task and its subtasks."""

        # Report subtask progress
        p_response = self.session.post(
            url=self.api_graphql,
            json={'query': queries.task_status(process_id)}
        )

        reported_tasks = []
        task = p_response.json().get('data').get('_tasks')[0]
        while (status := task.get('status')) != 'COMPLETED':
            if status == 'ERROR':
                raise PyclientException(f"Error uploading file: {task.get('description')}")
            subtasks = task.get('subTasks', [])
            for st in subtasks:
                if st['id'] not in reported_tasks and st['status'] == 'RUNNING':
                    log.info(f"{st['description']}")
                    reported_tasks.append(st['id'])
                if st['id'] not in reported_tasks and st['status'] == 'SKIPPED':
                    log.warning(f"    {st['description']}")
                    reported_tasks.append(st['id'])
                for sst in st.get('subTasks', []):
                    if sst['id'] not in reported_tasks and sst['status'] == 'COMPLETED':
                        log.info(f"    {sst['description']}")
                        reported_tasks.append(sst['id'])
                    if sst['id'] not in reported_tasks and sst['status'] == 'SKIPPED':
                        log.warning(f"    {sst['description']}")
                        reported_tasks.append(sst['id'])
                    for ssst in sst.get('subTasks', []):
                        if ssst['id'] not in reported_tasks and ssst['status'] == 'COMPLETED':
                            log.info(f"        {ssst['description']}")
                            reported_tasks.append(ssst['id'])
                        if ssst['id'] not in reported_tasks and ssst['status'] == 'SKIPPED':
                            log.warning(f"        {ssst['description']}")
                            reported_tasks.append(ssst['id'])
            try:
                p_response = self.session.post(
                    url=self.api_graphql,
                    json={'query': queries.task_status(process_id)}
                )
                task = p_response.json().get('data').get('_tasks')[0]
            except AttributeError as ae:
                log.debug(ae)
                time.sleep(1)
                p_response = self.session.post(
                    url=self.api_graphql,
                    json={'query': queries.task_status(process_id)}
                )
                task = p_response.json().get('data').get('_tasks')[0]
        log.info(f"Completed task: {task.get('description')}")

    def _validate_graphql_response(self, response, mutation: str = None, fallback_error_message: str = None):
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
            if "Cannot create schema from template" in response.text:
                msg = response.json().get("errors", [])[0].get('message', '')
                log.error(msg)
                raise NonExistentTemplateException("Selected template does not exist.")
            if "Field \'members\' in type \'MolgenisSchema\' is undefined" in response.text:
                msg = response.json().get("errors", [])[0].get('message')
                log.error(msg)
                raise PermissionDeniedException("Cannot access members on this schema.")

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

    def _parse_get_table_query(self, table_id: str, schema: str, columns: list = None) -> str:
        """Gathers a table's metadata and parses it to a GraphQL query
        for querying the table's contents.
        """
        schema_metadata: Schema = self.get_schema_metadata(schema)
        table_metadata: Table = schema_metadata.get_table('id', table_id)

        if columns is not None:
            if not all(col in map(lambda c: c.id, table_metadata.columns) for col in columns):
                unknown_cols = "'" + "', '".join([col for col in columns if col not in map(lambda c: c.id, table_metadata.columns)]) + "'"
                raise NoSuchColumnException(f"Columns {unknown_cols} not found.")

        query = (f"query {table_id}($filter: {table_id}Filter) {{\n"
                 f"  {table_id}(filter: $filter) {{\n")

        for col in table_metadata.columns:
            if columns is not None and (col.id not in columns and col.name not in columns):
                continue
            if col.get('columnType') in [HEADING, SECTION]:
                continue
            elif col.get('columnType').startswith(ONTOLOGY):
                query += f"    {col.get('id')} {{name}}\n"
            elif col.get('columnType').startswith(REF) or col.get('columnType') in [RADIO, SELECT]:
                if (ref_schema := col.get('refSchemaName', schema)) == schema:
                    pkeys = schema_metadata.get_pkeys(col.get('refTableId'))
                else:
                    ref_schema_meta = self.get_schema_metadata(ref_schema)
                    pkeys = ref_schema_meta.get_pkeys(col.get('refTableId'))
                query += f"    {col.get('id')} {{"
                query += parse_nested_pkeys(pkeys)
                query += "}\n"
            elif col.get('columnType').startswith(FILE):
                query += f"    {col.get('id')} {{id}}\n"
            else:
                query += f"    {col.get('id')}\n"
        query += "  }\n"
        query += "}"

        return query

    def _parse_ontology(self, data: list, table_id: str, schema: str) -> list:
        """Parses the ontology columns from a GraphQL response."""
        schema_meta = self.get_schema_metadata(schema)
        table_meta = schema_meta.get_table('id', table_id)
        parsed_data = []
        for row in data:
            parsed_row = {}
            for (col, value) in row.items():
                column_meta = table_meta.get_column('id', col)
                match column_meta.get('columnType'):
                    case "ONTOLOGY":
                        parsed_row[col] = value['name']
                    case "ONTOLOGY_ARRAY":
                        parsed_row[col] = [val['name'] for val in value]
                    case "REF", "SELECT", "RADIO":
                        _schema = column_meta.get('refSchemaName', schema)
                        parsed_row[col] = self._parse_ontology([value], column_meta.get('refTableId'), _schema)[0]
                    case "REF_ARRAY", "MULTISELECT", "CHECKBOX":
                        _schema = column_meta.get('refSchemaName', schema)
                        parsed_row[col] = self._parse_ontology(value, column_meta.get('refTableId'), _schema)
                    case "REFBACK":
                        _schema = column_meta.get('refSchemaName', schema)
                        parsed_row[col] = self._parse_ontology(value, column_meta.get('refTableId'), _schema)
                    case _:
                        parsed_row[col] = value
            parsed_data.append(parsed_row)
        return parsed_data

