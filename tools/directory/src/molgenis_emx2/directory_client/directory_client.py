import csv
import io
import json
import os
import tempfile
from collections import defaultdict
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import List
from zipfile import ZipFile

import pandas as pd
from molgenis_emx2_pyclient import Client as Session
from molgenis_emx2_pyclient.exceptions import NoSuchSchemaException
from molgenis_emx2_pyclient.metadata import NoSuchTableException, Schema
from molgenis_emx2_pyclient.metadata import Table as MetaTable

from .errors import MolgenisRequestError
from .model import (
    DirectoryData,
    ExternalServerNode,
    MixedData,
    Node,
    NodeData,
    OntologyTable,
    QualityInfo,
    Source,
    Table,
    TableMeta,
    TableType,
)
from .utils import create_csv

# Increase max. field size to accommodate e.g. long lists of refbacks
# Value is 1/4th of max. CSV line size in Molgenis
csv.field_size_limit(2097152)

@dataclass
class AttributesRequest:
    persons: List[str]
    networks: List[str]
    also_known_in: List[str]
    biobanks: List[str]
    services: List[str]
    studies: List[str]
    collections: List[str]
    facts: List[str]


class MolgenisImportError(MolgenisRequestError):
    pass


class DirectorySession(Session):
    """
    A session with a BBMRI Biobank Directory. Contains methods to get national nodes,
    their (staging) data and quality information.
    """

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.directory_schema = self.default_schema

    NODES_TABLE = "NationalNodes"
    ONTOLOGY_SCHEMA = "DirectoryOntologies"

    def get(
        self,
        table: str,
        query_filter: str = None,
        schema: str = None,
        as_df: bool = False,
    ) -> list | pd.DataFrame:
        """
        Copy of the EMX2 pyclient get, with change in how list of dicts is returned
        Retrieves data from a schema and returns as a list of dictionaries or as
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
            raise NoSuchTableException(
                f"Table {table!r} not found " f"in schema {current_schema!r}."
            )

        schema_metadata: Schema = self.get_schema_metadata(current_schema)
        table_id = schema_metadata.get_table(by="name", value=table).id

        filter_part = self._prepare_filter(query_filter, table, schema)
        if filter_part:
            filter_part = "?filter=" + json.dumps(filter_part)
        else:
            filter_part = ""
        query_url = f"{self.url}/{current_schema}/api/csv/{table_id}{filter_part}"
        response = self.session.get(url=query_url)

        self._validate_graphql_response(
            response=response,
            fallback_error_message=f"Failed to retrieve "
            f"data from {current_schema}::{table!r}.\n"
            f"Status code: {response.status_code}.",
        )

        response_data = pd.read_csv(io.BytesIO(response.content), keep_default_na=False)

        if not as_df:
            response_data = []
            data = response.content.decode("utf-8").strip()
            with io.StringIO(data) as infile:
                reader = csv.DictReader(infile, quotechar='"')
                for rec in reader:
                    response_data.append(rec)

            response_data = self._reset_data_types(
                response_data, schema_metadata.get_table(by="name", value=table)
            )
        return response_data

    def get_table_meta(
        self,
        table_name: str,
        schema: str = None,
    ):
        schema_meta = self.get_schema_metadata(schema)
        for table in schema_meta.tables:
            if table.name == table_name:
                return table.columns
        raise MolgenisRequestError(f"Unknown table: {table_name}")

    def get_ontology(
        self,
        entity_type_id: str,
        matching_attrs: List[str] | None = None,
        parent_attr: str = "parent",
    ) -> OntologyTable:
        """
        Retrieves an ontology table.
        :param entity_type_id: the identifier of the table
        :param parent_attr: the name of the attribute that contains the parent relation
        :param matching_attrs: a list with the relevant level or levels of the
                               "matching" ontology code columns
        :return: an OntologyTable
        """
        matching_attrs = matching_attrs if matching_attrs else []

        rows = self.get(
            schema=self.ONTOLOGY_SCHEMA,
            table=entity_type_id,
        )

        meta = TableMeta(
            meta=self.get_table_meta(
                schema=self.ONTOLOGY_SCHEMA, table_name=entity_type_id
            )
        )
        return OntologyTable.of(meta, rows, parent_attr, matching_attrs)

    def get_quality_info(self) -> QualityInfo:
        """
        Retrieves the quality information identifiers for biobanks and collections.
        :return: a QualityInfo object
        """

        biobank_qualities = self.get(
            table="QualityInfoBiobanks",
        )
        collection_qualities = self.get(
            table="QualityInfoCollections",
        )

        service_qualities = self.get(
            table="QualityInfoServices",
        )

        bb_qual = defaultdict(list)
        bb_level = defaultdict(list)
        coll_qual = defaultdict(list)
        coll_level = defaultdict(list)
        service_qual = defaultdict(list)
        for row in biobank_qualities:
            bb_qual[row["biobank"]].append(row["id"])
            bb_level[row["biobank"]].append(row["assess_level_bio"])
        for row in collection_qualities:
            coll_qual[row["collection"]].append(row["id"])
            coll_level[row["collection"]].append(row["assess_level_col"])

        for row in service_qualities:
            service_qual[row["service"]].append(row["id"])

        return QualityInfo(
            biobanks=bb_qual,
            collections=coll_qual,
            biobank_levels=bb_level,
            collection_levels=coll_level,
            services=service_qual,
        )

    def get_node(self, code: str) -> Node:
        """
        Retrieves a single Node object from the national nodes table.
        :param code: node to get by code
        :return: Node object
        """
        nodes = self.get(table=self.NODES_TABLE, query_filter=f"id == {code}")
        self._validate_codes([code], nodes)
        return self._to_nodes(nodes)[0]

    def get_nodes(self, codes: List[str] = None) -> List[Node]:
        """
        Retrieves a list of Node objects from the national nodes table. Will return
        all nodes or some nodes if 'codes' is specified.
        :param codes: nodes to get by code
        :return: list of Node objects
        """
        if codes:
            nodes = self.get(table=self.NODES_TABLE, query_filter=f"id == {codes}")
        else:
            nodes = self.get(table=self.NODES_TABLE)

        if codes:
            self._validate_codes(codes, nodes)
        return self._to_nodes(nodes)

    def get_external_node(self, code: str) -> ExternalServerNode:
        """
        Retrieves a single ExternalServerNode object from the national nodes table.
        :param code: node to get by code
        :return: ExternalServerNode object
        """
        df_nodes = self.get(
            table=self.NODES_TABLE, query_filter=f"id == {code}", as_df=True
        )
        df_nodes = df_nodes.loc[df_nodes["dns"] != ""]
        nodes = df_nodes.to_dict("records")

        self._validate_codes([code], nodes)
        return self._to_nodes(nodes)[0]

    def get_external_nodes(self, codes: List[str] = None) -> List[ExternalServerNode]:
        """
        Retrieves a list of ExternalServerNode objects from the national nodes table.
        Will return all nodes or some nodes if 'codes' is specified.
        :param codes: nodes to get by code
        :return: list of ExternalServerNode objects
        """
        if codes:
            df_nodes = self.get(
                table=self.NODES_TABLE, query_filter=f"id == {codes}", as_df=True
            )
        else:
            df_nodes = self.get(table=self.NODES_TABLE, as_df=True)

        df_nodes = df_nodes.loc[df_nodes["dns"] != ""]
        nodes = df_nodes.to_dict("records")

        if codes:
            self._validate_codes(codes, nodes)
        return self._to_nodes(nodes)

    @staticmethod
    def _validate_codes(codes: List[str], nodes: List[dict]):
        """Raises a KeyError if a requested node code was not found."""
        retrieved_codes = {node["id"] for node in nodes}
        for code in codes:
            if code not in retrieved_codes:
                raise KeyError(f"Unknown code: {code}")

    @staticmethod
    def _to_nodes(nodes: List[dict]):
        """Maps rows to Node or ExternalServerNode objects."""
        result = list()
        for node in nodes:
            if "dns" not in node:
                result.append(
                    Node(
                        code=node["id"],
                        description=node["description"],
                        date_end=node.get("date_end"),
                    )
                )
            else:
                result.append(
                    ExternalServerNode(
                        code=node["id"],
                        description=node["description"],
                        date_end=node.get("date_end"),
                        url=node["dns"],
                        token=os.getenv(f"{node['id']}_user"),
                    )
                )

        return result

    @staticmethod
    def _reset_data_types(data: List, meta: MetaTable):
        for row in data:
            bools = meta.get_columns(by="columnType", value="BOOL")
            bools = [i.name for i in bools]
            ints = meta.get_columns(by="columnType", value="INT")
            ints = [column.name for column in ints]
            arrays = meta.get_columns(by="columnType", value="REF_ARRAY")
            arrays.extend(meta.get_columns(by="columnType", value="ONTOLOGY_ARRAY"))
            arrays = [column.name for column in arrays]
            for column in [column.name for column in meta.columns]:
                if column not in row:
                    continue
                if column in arrays:
                    if not row[column]:
                        row[column] = []
                    else:
                        row[column] = row[column].split(",")
                if not row[column] and type(row[column]) is not list:
                    del row[column]
                    continue
                if column in ints:
                    row[column] = int(row[column])
                if column in bools and type(row[column]) is str:
                    row[column] = eval(row[column].capitalize())
        return data

    def get_staging_node_data(self, node: Node) -> NodeData:
        """
        Gets the tables that belong to a single node's staging area.

        :param Node node: the node to get the staging data for
        :return: a NodeData object
        """
        tables = dict()
        for table_type in TableType.get_import_order():
            id_ = node.get_staging_id(table_type)
            meta = TableMeta(
                meta=self.get_table_meta(schema=node.get_schema_id(), table_name=id_)
            )

            tables[table_type.value] = Table.of(
                table_type=table_type,
                meta=meta,
                rows=self.get(schema=node.get_schema_id(), table=id_),
            )

        return NodeData.from_dict(node=node, source=Source.STAGING, tables=tables)

    def get_published_node_data(self, node: Node) -> NodeData:
        """
        Gets the tables that belong to a single node from the published tables.
        Filters the rows based on the national_node field.

        :param Node node: the node to get the published data for
        :return: a NodeData object
        """
        tables = dict()
        for table_type in TableType.get_import_order():
            id_ = table_type.base_id
            meta = TableMeta(
                self.get_table_meta(schema=self.directory_schema, table_name=id_)
            )

            tables[table_type.value] = Table.of(
                table_type=table_type,
                meta=meta,
                rows=self.get(
                    table=id_, query_filter=f"national_node.id == {node.code}"
                ),
            )

        return NodeData.from_dict(node=node, source=Source.PUBLISHED, tables=tables)

    def get_published_data(
        self, nodes: List[Node], attributes: AttributesRequest
    ) -> MixedData:
        """
        Gets the tables that belong to one or more nodes from the Directory tables.
        Filters the rows based on the national_node field.

        :param List[Node] nodes: the node(s) to get the Directory data for
        :param AttributesRequest attributes: the attributes to get for each table
        :return: a DirectoryData object
        """

        if len(nodes) == 0:
            raise ValueError("No nodes provided")

        attributes = asdict(attributes)
        codes = [node.code for node in nodes]
        tables = dict()
        for table_type in TableType.get_import_order():
            id_ = table_type.base_id
            meta = TableMeta(
                self.get_table_meta(schema=self.directory_schema, table_name=id_)
            )
            attrs = attributes[table_type.value]
            data = []
            for code in codes:
                p_code = self.get(table=id_, query_filter=f"national_node.id == {code}")
                data.extend(p_code)
            rows = []
            for row in data:
                rows.append(dict(filter(lambda x: x[0] in attrs, row.items())))

            tables[table_type.value] = Table.of(
                table_type=table_type, meta=meta, rows=rows
            )

        return MixedData.from_mixed_dict(source=Source.PUBLISHED, tables=tables)

    async def upload_data(self, schema: str, data: DirectoryData):
        """
        Converts the tables of a DirectoryData object to CSV, bundles them in
        a ZIP archive and imports them through the import API.
        :param schema: database where data should be uploaded into
        :param data: a DirectoryData object
        """
        with tempfile.TemporaryDirectory() as tmpdir:
            archive_name = f"{tmpdir}/directory_data.zip"
            with ZipFile(archive_name, "w") as archive:
                for table in data.import_order:
                    file_name = f"{table.full_name}.csv"
                    file_path = f"{tmpdir}/{file_name}"
                    create_csv(table.rows, file_path, table.meta.attributes)
                    archive.write(file_path, file_name)

            await self.upload_file(schema=schema, file_path=Path(archive_name))


class ExternalServerSession(DirectorySession):
    """
    A session with a national node's external server (for example BBMRI-NL).
    """

    def __init__(self, node: ExternalServerNode):
        super().__init__(url=node.url, token=node.token)
        self.node = node

    def get_node_data(self) -> NodeData:
        """
        Gets the six tables of this node's external server.

        :return: a NodeData object
        """

        tables = dict()
        for table_type in TableType.get_import_order():
            id_ = self.node.get_staging_id(table_type)
            schema_meta = self.get_schema_metadata(self.node.get_schema_id())
            try:
                schema_meta.get_table(by="name", value=id_)
                meta = TableMeta(
                    self.get_table_meta(
                        schema=self.node.get_schema_id(), table_name=id_
                    )
                )
                tables[table_type.value] = Table.of(
                    table_type=table_type,
                    meta=meta,
                    rows=self.get(schema=self.node.get_schema_id(), table=id_),
                )
            except NoSuchTableException:
                tables[table_type.value] = Table.of_placeholder(table_type)

        return NodeData.from_dict(
            node=self.node, source=Source.EXTERNAL_SERVER, tables=tables
        )
