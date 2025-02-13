import json
import logging
import pathlib
from functools import cache
from io import BytesIO

import pandas as pd

from . import graphql_queries as queries
from ._client import _Client
from .constants import DATE, DATETIME
from .exceptions import (NoSuchSchemaException, PyclientException,
                         NoSuchTableException,
                         GraphQLException, TokenSigninException, NoSuchColumnException)
from .metadata import Schema
from .utils import convert_dtypes

logging.getLogger("requests").setLevel(logging.WARNING)
logging.getLogger("urllib3").setLevel(logging.WARNING)
log = logging.getLogger("Molgenis EMX2 Pyclient")


class Client(_Client):
    """
    Use the Client object to log in to a Molgenis EMX2 server
    and perform operations on the server.
    """

    def __init__(self, url: str, schema: str = None, token: str = None, job: str = None) -> None:
        """
        Initializes a Client object.
        """
        super().__init__(
            url=url,
            schema=schema,
            token=token,
            job=job
        )

    def signin(self, username: str, password: str):
        """Signs in to the EMX2 server.

        :param username: the username or email address for an account on this server
        :type username: str
        :param password: the password corresponding to this username.
        :type username: str
        """
        return super().signin(username, password)

    def signout(self):
        """Signs the client out of the EMX2 server."""
        return super().signout()

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
        return super().get_schemas()

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
        return super().save_schema(table, name, file, data)

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
        current_schema = self._set_current_schema(schema)
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

    def delete_records(self, table: str, schema: str = None, file: str = None, data: list | pd.DataFrame = None):
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
        current_schema = self._set_current_schema(schema)
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
        current_schema = self._set_current_schema(schema)
        if not self._table_in_schema(table, current_schema):
            raise NoSuchTableException(f"Table {table!r} not found in schema {current_schema!r}.")

        schema_metadata: Schema = self.get_schema_metadata(current_schema)
        table_meta = schema_metadata.get_table(by='name', value=table)
        table_id = table_meta.id

        filter_part = self._prepare_filter(query_filter, table, schema)

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
        response_data = pd.read_csv(BytesIO(response.content),  keep_default_na=True, dtype=dtypes, parse_dates=date_columns)

        response_data[bool_columns] = response_data[bool_columns].replace({'true': True, 'false': False})
        response_data = response_data.astype(dtypes)

        if columns:
            try:
                response_data = response_data[columns]
            except KeyError as e:
                if "not in index" in e.args[0]:
                    raise NoSuchColumnException(f"Columns {e.args[0]}")
                else:
                    raise NoSuchColumnException(f"Columns {e.args[0].split('Index(')[1].split(', dtype')}"
                                                f" not in index.")
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

        current_schema = self._set_current_schema(schema)
        if not self._table_in_schema(table, current_schema):
            raise NoSuchTableException(f"Table {table!r} not found in schema {current_schema!r}.")

        schema_metadata: Schema = self.get_schema_metadata(current_schema)
        table_meta = schema_metadata.get_table(by='name', value=table)
        table_id = table_meta.id

        filter_part = self._prepare_filter(query_filter, table, schema)
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
        current_schema = self._set_current_schema(schema)
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
                url = f"{self.url}/{current_schema}/api/excel"
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
                url = f"{self.url}/{current_schema}/api/excel/{table_id}"
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
                url = f"{self.url}/{current_schema}/api/zip"
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
                url = f"{self.url}/{current_schema}/api/csv/{table_id}"
                response = self.session.get(url=url)
                self._validate_graphql_response(response)

                if filename:
                    with open(filename, "wb") as file:
                        file.write(response.content)
                    log.info("Exported data from table %s in schema %s to '%s'.", table, current_schema, filename)
                else:
                    log.info("Exported data from table %s in schema %s.", table, current_schema)

        return BytesIO(response.content)

    async def create_schema(self, name: str = None,
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
        variables = self._format_optional_params(name=name, description=description,
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
        current_schema = self._set_current_schema(name)
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
        current_schema = self._set_current_schema(name)
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
        current_schema = self._set_current_schema(name)
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
        return super().get_schema_metadata(name)

    def set_schema(self, name: str) -> str:
        """Sets the default schema to the schema supplied as argument.
        Raises NoSuchSchemaException if the schema cannot be found on the server.

        :param name: name of a schema
        :type name: str

        :returns: a schema name
        :rtype: str
        """

        return super().set_schema(name)
