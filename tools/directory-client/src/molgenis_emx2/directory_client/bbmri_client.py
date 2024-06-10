import os
from collections import defaultdict
from dataclasses import asdict, dataclass
from enum import Enum
from typing import List

from molgenis.bbmri_eric.model import (
    EricData,
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
from molgenis.client import MolgenisRequestError, Session


@dataclass
class AttributesRequest:
    persons: List[str]
    networks: List[str]
    also_known_in: List[str]
    biobanks: List[str]
    collections: List[str]
    facts: List[str]


class MolgenisImportError(MolgenisRequestError):
    pass


class ImportDataAction(Enum):
    """Enum of MOLGENIS import actions"""

    ADD = "add"
    ADD_UPDATE_EXISTING = "add_update_existing"
    UPDATE = "update"
    ADD_IGNORE_EXISTING = "add_ignore_existing"


class ImportMetadataAction(Enum):
    """Enum of MOLGENIS import metadata actions"""

    ADD = "add"
    UPDATE = "update"
    UPSERT = "upsert"
    IGNORE = "ignore"


class EricSession(Session):
    """
    A session with a BBMRI ERIC directory. Contains methods to get national nodes,
    their (staging) data and quality information.
    """

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

    NODES_TABLE = "eu_bbmri_eric_national_nodes"

    def get_ontology(
        self,
        entity_type_id: str,
        matching_attrs: List[str] | None = None,
        parent_attr: str = "parentId",
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
            entity_type_id,
            batch_size=10000,
            attributes=f"id,{parent_attr},ontology,{','.join(matching_attrs)}",
            uploadable=True,
        )
        meta = TableMeta(meta=self.get_meta(entity_type_id))
        return OntologyTable.of(meta, rows, parent_attr, matching_attrs)

    def get_quality_info(self) -> QualityInfo:
        """
        Retrieves the quality information identifiers for biobanks and collections.
        :return: a QualityInfo object
        """

        biobank_qualities = self.get(
            "eu_bbmri_eric_bio_qual_info",
            batch_size=10000,
            attributes="id,biobank,assess_level_bio",
            uploadable=True,
        )
        collection_qualities = self.get(
            "eu_bbmri_eric_col_qual_info",
            batch_size=10000,
            attributes="id,collection,assess_level_col",
            uploadable=True,
        )

        bb_qual = defaultdict(list)
        bb_level = defaultdict(list)
        coll_qual = defaultdict(list)
        coll_level = defaultdict(list)
        for row in biobank_qualities:
            bb_qual[row["biobank"]].append(row["id"])
            bb_level[row["biobank"]].append(row["assess_level_bio"])
        for row in collection_qualities:
            coll_qual[row["collection"]].append(row["id"])
            coll_level[row["collection"]].append(row["assess_level_col"])

        return QualityInfo(
            biobanks=bb_qual,
            collections=coll_qual,
            biobank_levels=bb_level,
            collection_levels=coll_level,
        )

    def get_node(self, code: str) -> Node:
        """
        Retrieves a single Node object from the national nodes table.
        :param code: node to get by code
        :return: Node object
        """
        nodes = self.get(self.NODES_TABLE, q=f"id=={code}")
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
            nodes = self.get(self.NODES_TABLE, q=f"id=in=({','.join(codes)})")
        else:
            nodes = self.get(self.NODES_TABLE)

        if codes:
            self._validate_codes(codes, nodes)
        return self._to_nodes(nodes)

    def get_external_node(self, code: str) -> ExternalServerNode:
        """
        Retrieves a single ExternalServerNode object from the national nodes table.
        :param code: node to get by code
        :return: ExternalServerNode object
        """
        nodes = self.get(self.NODES_TABLE, q=f"id=={code};dns!=''")
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
            nodes = self.get(self.NODES_TABLE, q=f"id=in=({','.join(codes)});dns!=''")
        else:
            nodes = self.get(self.NODES_TABLE, q="dns!=''")

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

    def get_staging_node_data(self, node: Node) -> NodeData:
        """
        Gets the six tables that belong to a single node's staging area.

        :param Node node: the node to get the staging data for
        :return: a NodeData object
        """
        tables = dict()
        for table_type in TableType.get_import_order():
            id_ = node.get_staging_id(table_type)
            meta = TableMeta(meta=self.get_meta(id_))

            tables[table_type.value] = Table.of(
                table_type=table_type,
                meta=meta,
                rows=self.get(id_, batch_size=10000, uploadable=True),
            )

        return NodeData.from_dict(node=node, source=Source.STAGING, tables=tables)

    def get_published_node_data(self, node: Node) -> NodeData:
        """
        Gets the six tables that belong to a single node from the published tables.
        Filters the rows based on the national_node field.

        :param Node node: the node to get the published data for
        :return: a NodeData object
        """

        tables = dict()
        for table_type in TableType.get_import_order():
            id_ = table_type.base_id
            meta = TableMeta(self.get_meta(id_))

            tables[table_type.value] = Table.of(
                table_type=table_type,
                meta=meta,
                rows=self.get(
                    id_,
                    batch_size=10000,
                    q=f"national_node=={node.code}",
                    uploadable=True,
                ),
            )

        return NodeData.from_dict(node=node, source=Source.PUBLISHED, tables=tables)

    def get_published_data(
        self, nodes: List[Node], attributes: AttributesRequest
    ) -> MixedData:
        """
        Gets the six tables that belong to one or more nodes from the published tables.
        Filters the rows based on the national_node field.

        :param List[Node] nodes: the node(s) to get the published data for
        :param AttributesRequest attributes: the attributes to get for each table
        :return: an EricData object
        """

        if len(nodes) == 0:
            raise ValueError("No nodes provided")

        attributes = asdict(attributes)
        codes = [node.code for node in nodes]
        tables = dict()
        for table_type in TableType.get_import_order():
            id_ = table_type.base_id
            meta = TableMeta(self.get_meta(id_))
            attrs = attributes[table_type.value]

            tables[table_type.value] = Table.of(
                table_type=table_type,
                meta=meta,
                rows=self.get(
                    id_,
                    batch_size=10000,
                    q=f"national_node=in=({','.join(codes)})",
                    attributes=",".join(attrs),
                    uploadable=True,
                ),
            )

        return MixedData.from_mixed_dict(source=Source.PUBLISHED, tables=tables)

    def upload_data(self, data: EricData):
        """
        Converts the six tables of an EricData object to CSV, bundles them in
        a ZIP archive and imports them through the import API.
        :param data: an EricData object
        """

        importable_data = dict()
        for table in data.import_order:
            importable_data[table.full_name] = table.rows

        self.import_data(
            importable_data,
            data_action=ImportDataAction.ADD_UPDATE_EXISTING,
            metadata_action=ImportMetadataAction.IGNORE,
        )


class ExternalServerSession(Session):
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
            if not self.get("sys_md_EntityType", q=f"id=={id_}"):
                tables[table_type.value] = Table.of_placeholder(table_type)
            else:
                meta = TableMeta(self.get_meta(id_))
                tables[table_type.value] = Table.of(
                    table_type=table_type,
                    meta=meta,
                    rows=self.get(id_, batch_size=10000, uploadable=True),
                )

        return NodeData.from_dict(
            node=self.node, source=Source.EXTERNAL_SERVER, tables=tables
        )
