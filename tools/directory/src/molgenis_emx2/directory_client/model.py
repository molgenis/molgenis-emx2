import os
import typing
from abc import ABC
from collections import OrderedDict
from copy import deepcopy
from dataclasses import dataclass, field
from enum import Enum
from typing import Dict, List, Set

from .utils import to_ordered_dict


class TableType(Enum):
    """Enum representing the tables each national node has."""

    PERSONS = "persons"
    ALSO_KNOWN = "also_known_in"
    NETWORKS = "networks"
    BIOBANKS = "biobanks"
    SERVICES = "services"
    STUDIES = "studies"
    COLLECTIONS = "collections"
    FACTS = "facts"

    @classmethod
    def get_import_order(cls) -> List["TableType"]:
        return [type_ for type_ in cls]

    @property
    def base_id(self) -> str:
        if self.value == "facts":
            table = "collectionFacts"
        elif self.value == "also_known_in":
            table = "alsoKnownIn"
        else:
            table = self.value

        return table[0].upper() + table[1:]


@dataclass(frozen=True)
class TableMeta:
    """Convenient wrapper for the output of the metadata API."""

    meta: dict
    id_attribute: str = field(init=False)

    def __post_init__(self):
        for attribute in self.meta:
            if attribute.get("key") == 1:
                object.__setattr__(self, "id_attribute", attribute.name)

    @property
    def id(self):
        return self.meta[0].get("table")

    @property
    def attributes(self):
        return [attr.name for attr in self.meta]

    @property
    def one_to_manys(self) -> List[str]:
        one_to_manys = []
        for attribute in self.meta["attributes"]["items"]:
            if attribute["data"]["type"] == "onetomany":
                one_to_manys.append(attribute["data"]["name"])
        return one_to_manys

    @property
    def hyperlinks(self) -> List[str]:
        hyperlinks = []
        for attribute in self.meta:
            if attribute.get("columnType") == "hyperlink":
                hyperlinks.append(attribute.name)
        return hyperlinks


@dataclass(frozen=True)
class BaseTable(ABC):
    """
    Simple representation of a MOLGENIS table. The rows should be in the uploadable
    format. (See utils.py)
    """

    rows_by_id: "typing.OrderedDict[str, dict]"
    meta: TableMeta

    @property
    def rows(self) -> List[dict]:
        return list(self.rows_by_id.values())

    @property
    def full_name(self) -> str:
        return self.meta.id


@dataclass(frozen=True)
class Table(BaseTable):
    """
    Simple representation of a BBMRI ERIC node table.
    """

    type: TableType

    @staticmethod
    def of(table_type: TableType, meta: TableMeta, rows: List[dict]) -> "Table":
        """Factory method that takes a list of rows instead of an OrderedDict of
        ids/rows."""
        return Table(
            rows_by_id=to_ordered_dict(rows, meta.id_attribute),
            meta=meta,
            type=table_type,
        )

    @staticmethod
    def of_empty(table_type: TableType, meta: TableMeta):
        return Table(rows_by_id=OrderedDict(), meta=meta, type=table_type)

    @staticmethod
    def of_placeholder(table_type: TableType):
        meta = {
            "id": table_type.base_id,
            "attributes": {"items": [{"data": {"name": "id", "idAttribute": True}}]},
        }
        return Table.of_empty(
            table_type=table_type,
            meta=TableMeta(meta=meta),
        )


@dataclass(frozen=True)
class OntologyTable(BaseTable):
    """
    Simple representation of an ontology table where the parent/child relations are
    persisted with self-references.
    """

    parent_attr: str
    matching_attrs: List[str] | None = None

    def get_matching_ontologies(self, ontologies: List[str]) -> Set[str]:
        """
        Will add matching ontologies with the specified level(s) of confidence to the
        list of ontologies

        :param ontologies: a list with the current ontologies
        :return: a list with the current ontologies extended with matching ontologies,
        if available.
        """
        matching_ontologies = []
        for attr in self.matching_attrs:
            for ontology in ontologies:
                try:
                    matching_ontologies.extend(self.rows_by_id[ontology][attr])
                except KeyError:
                    pass

        return set(matching_ontologies)

    def is_descendant_of_any(self, descendant_id: str, ancestor_ids: Set[str]) -> bool:
        """
        Will walk from the descendant up through the parents until it finds one of the
        provided ancestors, or return False if it reaches an element without a parent.
        Will also return True if the descendant_id itself is in de ancestor_ids.

        :param descendant_id: the id of the descendant
        :param ancestor_ids: the ids of the ancestors
        :return: True if the descendant_id is a descendant of any of the ancestor_ids
        """
        current = self.rows_by_id[descendant_id]
        while True and current["codesystem"] != "orphanet":
            if current[self.meta.id_attribute] in ancestor_ids:
                return True
            if not current[self.parent_attr]:
                return False
            if len(current[self.parent_attr]) == 1:
                current = self.rows_by_id[current[self.parent_attr][0]]
            else:
                raise TypeError("More than one ICD-10 parent")

    @staticmethod
    def of(
        meta: TableMeta,
        rows: List[dict],
        parent_attr: str,
        matching_attrs: List[str] | None = None,
    ) -> "OntologyTable":
        """Factory method that takes a list of rows instead of an OrderedDict of
        ids/rows."""
        matching_attrs = matching_attrs if matching_attrs else []
        return OntologyTable(
            rows_by_id=to_ordered_dict(rows, meta.id_attribute),
            meta=meta,
            parent_attr=parent_attr,
            matching_attrs=matching_attrs,
        )


@dataclass(frozen=True)
class Node:
    """Represents a single national node in a BBMRI Biobank Directory."""

    code: str
    description: str | None = None
    date_end: str | None = None

    _classifiers = {
        TableType.PERSONS: "contactID",
        TableType.NETWORKS: "networkID",
        TableType.ALSO_KNOWN: "akiID",
        TableType.BIOBANKS: "ID",
        TableType.SERVICES: "serviceID",
        TableType.STUDIES: "studyID",
        TableType.COLLECTIONS: "ID",
        TableType.FACTS: "factID",
    }

    def get_schema_id(self) -> str:
        return f"{os.getenv('NN_SCHEMA_PREFIX')}-{self.code}"

    @staticmethod
    def get_staging_id(table_type: TableType) -> str:
        """
        Returns the identifier of a node's staging table.

        :param TableType table_type: the table to get the staging id of
        :return: the id of the staging table
        """
        if table_type.value == "facts":
            table = "collectionFacts"
        elif table_type.value == "also_known_in":
            table = "alsoKnownIn"
        else:
            table = table_type.value

        return table[0].upper() + table[1:]

    def get_id_prefix(self, table_type: TableType) -> str:
        """
        Each table has a specific prefix for the identifiers of its rows. This prefix is
        based on the node's code and the classifier of the table.

        :param TableType table_type: the table to get the id prefix for
        :return: the id prefix
        """
        classifier = self._classifiers[table_type]
        return f"bbmri-eric:{classifier}:{self.code}_"

    @classmethod
    def get_eu_id_prefix(cls, table_type: TableType) -> str:
        """
        Some nodes can refer to rows in the EU node. These rows have an EU prefix, and
        it's based on the classifier of the table.

        :param TableType table_type: the table to get the EU id prefix for
        :return: the EU id prefix
        """

        classifier = cls._classifiers[table_type]
        return f"bbmri-eric:{classifier}:EU_"

    @staticmethod
    def of(code: str):
        return Node(code, None, None)

    def __eq__(self, other: object) -> bool:
        if isinstance(other, Node):
            return self.code == other.code
        return False

    def __hash__(self):
        return hash(self.code)


@dataclass(frozen=True)
class ExternalServerNode(Node):
    """Represents a node that has an external server on which its data is hosted."""

    url: str | None = None
    token: str | None = None


class Source(Enum):
    EXTERNAL_SERVER = "external_server"
    STAGING = "staging"
    PUBLISHED = "published"
    TRANSFORMED = "transformed"


@dataclass
class DirectoryData(ABC):
    """Abstract base class for containers storing rows from the Directory tables:
    persons, also_known_in, networks, biobanks, services, studies, collections,
    and facts."""

    source: Source
    persons: Table
    also_known_in: Table
    networks: Table
    biobanks: Table
    services: Table
    studies: Table
    collections: Table
    facts: Table
    table_by_type: Dict[TableType, Table] = field(init=False)

    def __post_init__(self):
        self.table_by_type = {
            TableType.PERSONS: self.persons,
            TableType.NETWORKS: self.networks,
            TableType.ALSO_KNOWN: self.also_known_in,
            TableType.BIOBANKS: self.biobanks,
            TableType.SERVICES: self.services,
            TableType.STUDIES: self.studies,
            TableType.COLLECTIONS: self.collections,
            TableType.FACTS: self.facts,
        }

    @property
    def import_order(self) -> List[Table]:
        return [
            self.persons,
            self.networks,
            self.also_known_in,
            self.biobanks,
            self.services,
            self.studies,
            self.collections,
            self.facts,
        ]


@dataclass
class NodeData(DirectoryData):
    """Container object storing the tables of a single node."""

    node: Node

    @staticmethod
    def from_dict(node: Node, source: Source, tables: Dict[str, Table]) -> "NodeData":
        return NodeData(node=node, source=source, **tables)

    def convert_to_staging(self) -> "NodeData":
        """
        The metadata of an external node is the same as the metadata of its staging
        area. This method copies an external server's NodeData and changes only the
        table identifiers to point to the staging area's identifiers.
        """
        if self.source != Source.EXTERNAL_SERVER:
            raise ValueError("data isn't from an external server")

        tables = dict()
        for table in self.import_order:
            metadata = deepcopy(table.meta.meta)
            # metadata["id"] = self.node.get_staging_id(table.type)
            tables[table.type.value] = Table(
                table.rows_by_id, TableMeta(metadata), table.type
            )

        return NodeData(node=self.node, source=Source.STAGING, **tables)


class MixedData(DirectoryData):
    """Container object storing the tables with mixed origins, for example from
    the combined tables or from multiple staging areas."""

    @staticmethod
    def from_mixed_dict(source: Source, tables: Dict[str, Table]) -> "MixedData":
        return MixedData(source=source, **tables)

    def merge(self, other_data: DirectoryData):
        self.persons.rows_by_id.update(other_data.persons.rows_by_id)
        self.networks.rows_by_id.update(other_data.networks.rows_by_id)
        self.also_known_in.rows_by_id.update(other_data.also_known_in.rows_by_id)
        self.biobanks.rows_by_id.update(other_data.biobanks.rows_by_id)
        self.services.rows_by_id.update(other_data.services.rows_by_id)
        self.studies.rows_by_id.update(other_data.studies.rows_by_id)
        self.collections.rows_by_id.update(other_data.collections.rows_by_id)
        self.facts.rows_by_id.update(other_data.facts.rows_by_id)

    def remove_node_rows(self, node: Node):
        for table in self.import_order:
            ids_to_remove = [
                row["id"] for row in table.rows if row["national_node"] == node.code
            ]
            all(table.rows_by_id.pop(id_) for id_ in ids_to_remove)

    def copy_empty(self) -> "MixedData":
        return MixedData(
            source=self.source,
            persons=Table.of_empty(TableType.PERSONS, self.persons.meta),
            networks=Table.of_empty(TableType.NETWORKS, self.networks.meta),
            also_known_in=Table.of_empty(TableType.ALSO_KNOWN, self.also_known_in.meta),
            biobanks=Table.of_empty(TableType.BIOBANKS, self.biobanks.meta),
            services=Table.of_empty(TableType.SERVICES, self.services.meta),
            studies=Table.of_empty(TableType.STUDIES, self.studies.meta),
            collections=Table.of_empty(TableType.COLLECTIONS, self.collections.meta),
            facts=Table.of_empty(TableType.FACTS, self.facts.meta),
        )


@dataclass(frozen=True)
class QualityInfo:
    """
    Stores the quality information for biobanks, collections and services
    """

    biobanks: Dict[str, List[str]]
    """Dictionary of biobank ids and their quality ids"""

    biobank_levels: Dict[str, List[str]]
    """Dictionary of biobank ids and their assessment levels"""

    collections: Dict[str, List[str]]
    """Dictionary of collection ids and their quality ids"""

    collection_levels: Dict[str, List[str]]
    """Dictionary of collection ids and their assessment levels"""

    services: Dict[str, List[str]]
    """Dictionary of service ids and their quality ids"""

    def get_qualities(self, table_type: TableType) -> Dict[str, List[str]]:
        if table_type == TableType.BIOBANKS:
            return self.biobanks
        elif table_type == TableType.COLLECTIONS:
            return self.collections
        elif table_type == TableType.SERVICES:
            return self.services
        else:
            return dict()

    def get_levels(self, table_type: TableType) -> Dict[str, List[str]]:
        if table_type == TableType.BIOBANKS:
            return self.biobank_levels
        elif table_type == TableType.COLLECTIONS:
            return self.collection_levels
        else:
            return dict()
