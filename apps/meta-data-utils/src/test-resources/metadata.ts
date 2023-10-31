import { ISchemaMetaData } from "../types";

export const petStoreMetadata: ISchemaMetaData = {
  name: "Pet",
  tables: [
    {
      name: "Category",
      tableType: "DATA",
      id: "Category",
      externalSchema: "Pet",
      columns: [
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Order",
      tableType: "DATA",
      id: "Order",
      externalSchema: "Pet",
      columns: [
        {
          name: "orderId",
          id: "orderId",
          columnType: "AUTO_ID",
          key: 1,
          required: true,
          computed: "ORDER:${mg_autoid}",
        },
        {
          name: "pet",
          id: "pet",
          columnType: "REF",
          refTable: "Pet",
          refLabelDefault: "${name}",
          position: 1,
        },
        {
          name: "quantity",
          id: "quantity",
          columnType: "LONG",
          position: 2,
          validation: "if(quantity < 1) 'quantity should be >= 1'",
        },
        {
          name: "price",
          id: "price",
          columnType: "DECIMAL",
          position: 3,
          validation: "price >= 1",
        },
        {
          name: "complete",
          id: "complete",
          columnType: "BOOL",
          position: 4,
        },
        {
          name: "status",
          id: "status",
          columnType: "STRING",
          position: 5,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Pet",
      tableType: "DATA",
      id: "Pet",
      descriptions: [
        {
          locale: "en",
          value: "My pet store example table",
        },
      ],
      externalSchema: "Pet",
      columns: [
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "the name",
            },
          ],
        },
        {
          name: "category",
          id: "category",
          columnType: "REF",
          refTable: "Category",
          refLabelDefault: "${name}",
          required: true,
          position: 1,
        },
        {
          name: "photoUrls",
          id: "photoUrls",
          columnType: "STRING_ARRAY",
          position: 2,
        },
        {
          name: "details",
          id: "details",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Details",
            },
          ],
          position: 3,
        },
        {
          name: "status",
          id: "status",
          columnType: "STRING",
          position: 4,
        },
        {
          name: "tags",
          id: "tags",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Tag",
          refLabelDefault: "${name}",
          position: 5,
        },
        {
          name: "weight",
          id: "weight",
          columnType: "DECIMAL",
          required: true,
          position: 6,
        },
        {
          name: "orders",
          id: "orders",
          columnType: "REFBACK",
          refTable: "Order",
          refLabelDefault: "${orderId}",
          refBack: "pet",
          position: 7,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Tag",
      tableType: "ONTOLOGIES",
      id: "Tag",
      externalSchema: "Pet",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Tag",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Tag",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "User",
      tableType: "DATA",
      id: "User",
      externalSchema: "Pet",
      columns: [
        {
          name: "username",
          id: "username",
          columnType: "STRING",
          key: 1,
          required: true,
        },
        {
          name: "firstName",
          id: "firstName",
          columnType: "STRING",
          position: 1,
        },
        {
          name: "lastName",
          id: "lastName",
          columnType: "STRING",
          position: 2,
        },
        {
          name: "picture",
          id: "picture",
          columnType: "FILE",
          position: 3,
        },
        {
          name: "email",
          id: "email",
          columnType: "EMAIL",
          position: 4,
        },
        {
          name: "password",
          id: "password",
          columnType: "STRING",
          position: 5,
        },
        {
          name: "phone",
          id: "phone",
          columnType: "STRING",
          position: 6,
        },
        {
          name: "userStatus",
          id: "userStatus",
          columnType: "INT",
          position: 7,
        },
        {
          name: "pets",
          id: "pets",
          columnType: "REF_ARRAY",
          refTable: "Pet",
          refLabelDefault: "${name}",
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
  ],
};

export const catalogueMetadata: ISchemaMetaData = {
  name: "catalogue",
  tables: [
    {
      name: "ATC",
      tableType: "ONTOLOGIES",
      id: "ATC",
      descriptions: [
        {
          locale: "en",
          value: "ATC codes of medicines studied",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "ATC",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "ATC",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Age groups",
      tableType: "ONTOLOGIES",
      id: "AgeGroups",
      descriptions: [
        {
          locale: "en",
          value:
            "Select the relevant age group for this quantitative information",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Age groups",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Age groups",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "All variables",
      tableType: "DATA",
      id: "AllVariables",
      descriptions: [
        {
          locale: "en",
          value:
            "Generic listing of all source variables. Should not be used directly, please use SourceVariables or RepeatedSourceVariables instead",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "resource",
          id: "resource",
          columnType: "REF",
          key: 1,
          refTable: "Extended resources",
          refLabelDefault: "${id}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Data source that this variable was collected in",
            },
          ],
          position: 321,
        },
        {
          name: "dataset",
          id: "dataset",
          columnType: "REF",
          key: 1,
          refTable: "Datasets",
          refLink: "resource",
          refLabelDefault: "${name}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Dataset this variable is part of",
            },
          ],
          position: 322,
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "name of the variable, unique within a table",
            },
          ],
          position: 323,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Human friendly longer name, if applicable",
            },
          ],
          position: 324,
        },
        {
          name: "collection event",
          id: "collectionEvent",
          columnType: "REF",
          refTable: "Collection events",
          refLabelDefault: "${resource.id}.${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "in case of protocolised data collection this defines the moment in time this variable is collected on",
            },
          ],
          position: 325,
        },
        {
          name: "since version",
          id: "sinceVersion",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "When this variable was introduced",
            },
          ],
          position: 326,
        },
        {
          name: "until version",
          id: "untilVersion",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "When this variable was removed if applicable",
            },
          ],
          position: 327,
        },
        {
          name: "mappings",
          id: "mappings",
          columnType: "REFBACK",
          refTable: "Variable mappings",
          refLabelDefault:
            "${source.id}.${sourceDataset.name}.${target.id}.${targetDataset.name}.${targetVariable.name}",
          refBack: "target variable",
          descriptions: [
            {
              locale: "en",
              value:
                "in case of protocolised data collection this defines the moment in time this variable is collected on",
            },
          ],
          position: 340,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
        {
          name: "mg_tableclass",
          id: "mg_tableclass",
          columnType: "STRING",
          readonly: true,
          position: 10005,
        },
      ],
    },
    {
      name: "Areas of information cohorts",
      tableType: "ONTOLOGIES",
      id: "AreasOfInformationCohorts",
      descriptions: [
        {
          locale: "en",
          value:
            "Areas of information that were extracted in this data collection event",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Areas of information cohorts",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Areas of information cohorts",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Areas of information ds",
      tableType: "ONTOLOGIES",
      id: "AreasOfInformationDs",
      descriptions: [
        {
          locale: "en",
          value: "Areas of information that were collected",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Areas of information ds",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Areas of information ds",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Biospecimens",
      tableType: "ONTOLOGIES",
      id: "Biospecimens",
      descriptions: [
        {
          locale: "en",
          value:
            "If the data bank contains biospecimens, what types of specimen",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Biospecimens",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Biospecimens",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Cohort designs",
      tableType: "ONTOLOGIES",
      id: "CohortDesigns",
      descriptions: [
        {
          locale: "en",
          value:
            "The study design of this cohort, i.e. cross-sectional or longitudinal",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Cohort designs",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Cohort designs",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Cohorts",
      tableType: "DATA",
      id: "Cohorts",
      descriptions: [
        {
          locale: "en",
          value:
            "Group of individuals sharing a defining demographic characteristic",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "overview",
          id: "overview",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "General information",
            },
          ],
        },
        {
          name: "id",
          id: "id",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Internal identifier",
            },
          ],
          position: 1,
        },
        {
          name: "pid",
          id: "pid",
          columnType: "STRING",
          key: 2,
          descriptions: [
            {
              locale: "en",
              value: "Persistent identifier",
            },
          ],
          position: 2,
        },
        {
          name: "acronym",
          id: "acronym",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Acronym if applicable",
            },
          ],
          position: 3,
        },
        {
          name: "name",
          id: "name",
          columnType: "TEXT",
          key: 3,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Name used in European projects",
            },
          ],
          position: 4,
        },
        {
          name: "local name",
          id: "localName",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "If different from above, name in the national language",
            },
          ],
          position: 5,
        },
        {
          name: "keywords",
          id: "keywords",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Keywords to increase findability of this resource. Try to use words that are not used in the description",
            },
          ],
          position: 25,
        },
        {
          name: "website",
          id: "website",
          columnType: "HYPERLINK",
          descriptions: [
            {
              locale: "en",
              value: "Link to the website or homepage",
            },
          ],
          position: 26,
        },
        {
          name: "lead organisation",
          id: "leadOrganisation",
          columnType: "REF_ARRAY",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "lead organisation (e.g. research department or group) for this resource",
            },
          ],
          position: 27,
        },
        {
          name: "additional organisations",
          id: "additionalOrganisations",
          columnType: "REF_ARRAY",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "List the names of any additional organisations that contributed to the resource",
            },
          ],
          position: 28,
        },
        {
          name: "description",
          id: "description",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Short description",
            },
          ],
          position: 31,
        },
        {
          name: "external identifiers",
          id: "externalIdentifiers",
          columnType: "REFBACK",
          refTable: "External identifiers",
          refLabelDefault: "${resource.id}.${identifier}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "External identifier(s) for this resource (e.g. EUPASS number, UMCG register number)",
            },
          ],
          position: 37,
        },
        {
          name: "contact email",
          id: "contactEmail",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Contact e-mail address for this cohort",
            },
          ],
          position: 40,
        },
        {
          name: "contacts",
          id: "contacts",
          columnType: "REFBACK",
          refTable: "Contacts",
          refLabelDefault: "${resource.id}.${firstName}.${lastName}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value: "Contact person(s)",
            },
          ],
          position: 41,
        },
        {
          name: "type",
          id: "type",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Resource types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Type of resource, e.g. registry, cohort, biobank",
            },
          ],
          position: 42,
        },
        {
          name: "type other",
          id: "typeOther",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "If other, describe the type of resource",
            },
          ],
          position: 43,
        },
        {
          name: "design",
          id: "design",
          columnType: "ONTOLOGY",
          refTable: "Cohort designs",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "The study design of this cohort, i.e. cross-sectional or longitudinal",
            },
          ],
          position: 44,
        },
        {
          name: "design description",
          id: "designDescription",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Short description of the study design of this cohort",
            },
          ],
          position: 45,
        },
        {
          name: "design schematic",
          id: "designSchematic",
          columnType: "FILE",
          descriptions: [
            {
              locale: "en",
              value: "A schematic depiction of the study design of this cohort",
            },
          ],
          position: 46,
        },
        {
          name: "collection type",
          id: "collectionType",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Collection types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "The data collection type of this cohort, i.e. retrospective or prospective; if both, select both",
            },
          ],
          position: 47,
        },
        {
          name: "logo",
          id: "logo",
          columnType: "FILE",
          descriptions: [
            {
              locale: "en",
              value: "Logo of the resource, for use on homepages etc.",
            },
          ],
          position: 48,
        },
        {
          name: "population",
          id: "population",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value:
                "Description of the population that can potentially be captured in the resource",
            },
          ],
          position: 49,
        },
        {
          name: "number of participants",
          id: "numberOfParticipants",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "Total number of individuals for which data is collected",
            },
          ],
          position: 50,
        },
        {
          name: "number of participants with samples",
          id: "numberOfParticipantsWithSamples",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "Number of individuals for which samples are collected",
            },
          ],
          position: 51,
        },
        {
          name: "countries",
          id: "countries",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Countries",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Countries where data from this resource largely originate from",
            },
          ],
          position: 53,
        },
        {
          name: "regions",
          id: "regions",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Regions",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Geographical regions where data from this resource largely originate from",
            },
          ],
          position: 54,
        },
        {
          name: "population age groups",
          id: "populationAgeGroups",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Age groups",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Which population age groups are captured in this resource? Select all that are relevant.",
            },
          ],
          position: 55,
        },
        {
          name: "inclusion criteria",
          id: "inclusionCriteria",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Inclusion criteria",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Inclusion criteria applied to the participants of this resource",
            },
          ],
          position: 56,
        },
        {
          name: "other inclusion criteria",
          id: "otherInclusionCriteria",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Other inclusion criteria applied to the participants of this resource",
            },
          ],
          position: 57,
        },
        {
          name: "start year",
          id: "startYear",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "Year when first data was collected",
            },
          ],
          position: 58,
        },
        {
          name: "end year",
          id: "endYear",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value:
                "Year when last data was collected. Leave empty if collection is ongoing",
            },
          ],
          position: 59,
        },
        {
          name: "population disease",
          id: "populationDisease",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Diseases",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Does the resource collect information on a specific disease subpopulation (e.g., as in a disease-specific registry)?",
            },
          ],
          position: 66,
        },
        {
          name: "population oncology topology",
          id: "populationOncologyTopology",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "ICDO topologies",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Does the resource collect information on specific cancer subtype(s)? If yes, select topology specifications.",
            },
          ],
          position: 67,
        },
        {
          name: "population oncology morphology",
          id: "populationOncologyMorphology",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "ICDO morphologies",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Does the resource collect information on specific cancer subtype(s)? If yes, select morphology specifications.",
            },
          ],
          position: 68,
        },
        {
          name: "subcohorts",
          id: "subcohorts",
          columnType: "REFBACK",
          refTable: "Subcohorts",
          refLabelDefault: "${resource.id}.${name}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value: "List of subcohorts or subpopulations for this resource",
            },
          ],
          position: 72,
        },
        {
          name: "contents",
          id: "contents",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Data model and contents",
            },
          ],
          position: 73,
        },
        {
          name: "datasets",
          id: "datasets",
          columnType: "REFBACK",
          refTable: "Datasets",
          refLabelDefault: "${resource.id}.${name}",
          refBack: "resource",
          position: 74,
        },
        {
          name: "collection events",
          id: "collectionEvents",
          columnType: "REFBACK",
          refTable: "Collection events",
          refLabelDefault: "${resource.id}.${name}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value: "List of collection events defined for this resource",
            },
          ],
          position: 94,
        },
        {
          name: "release type",
          id: "releaseType",
          columnType: "ONTOLOGY",
          refTable: "Release types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Select whether this resource is a closed dataset or whether new data is released continuously or at a termly basis",
            },
          ],
          position: 103,
        },
        {
          name: "release description",
          id: "releaseDescription",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Description of the release cycle of this resource",
            },
          ],
          position: 104,
        },
        {
          name: "linkage options",
          id: "linkageOptions",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Linkage options with additional data sources that are available for this resource",
            },
          ],
          position: 105,
        },
        {
          name: "access",
          id: "access",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Access and validation information",
            },
          ],
          position: 106,
        },
        {
          name: "data holder",
          id: "dataHolder",
          columnType: "REF",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "The name of the organisation that is responsible for governance of the data bank",
            },
          ],
          position: 107,
        },
        {
          name: "DAPs",
          id: "dAPs",
          columnType: "REFBACK",
          refTable: "DAPs",
          refLabelDefault: "${organisation.id}.${resource.id}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "List of DAPs that are listed in the catalogue as having (conditional) permission to access (an extract of) the data resource",
            },
          ],
          position: 108,
        },
        {
          name: "data access conditions",
          id: "dataAccessConditions",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Data access conditions",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Codes defining data access terms and conditions",
            },
          ],
          position: 110,
        },
        {
          name: "data use conditions",
          id: "dataUseConditions",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Data use conditions",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Codes defining data use terms and conditions",
            },
          ],
          position: 111,
        },
        {
          name: "data access conditions description",
          id: "dataAccessConditionsDescription",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Description of data access terms and use conditions",
            },
          ],
          position: 112,
        },
        {
          name: "data access fee",
          id: "dataAccessFee",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value: "Does a fee apply to gain access to data of this cohort?",
            },
          ],
          position: 113,
        },
        {
          name: "information",
          id: "information",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Other information",
            },
          ],
          position: 156,
        },
        {
          name: "design paper",
          id: "designPaper",
          columnType: "REF_ARRAY",
          refTable: "Publications",
          refLabelDefault: "${doi}",
          descriptions: [
            {
              locale: "en",
              value:
                "Publication(s) that describe(s) the design of this resource",
            },
          ],
          position: 157,
        },
        {
          name: "publications",
          id: "publications",
          columnType: "REF_ARRAY",
          refTable: "Publications",
          refLabelDefault: "${doi}",
          descriptions: [
            {
              locale: "en",
              value: "Other publication(s) about this resource",
            },
          ],
          position: 158,
        },
        {
          name: "informed consent type",
          id: "informedConsentType",
          columnType: "ONTOLOGY",
          refTable: "Informed consent types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "What type of informed consent was given for data collection?",
            },
          ],
          position: 159,
        },
        {
          name: "funding statement",
          id: "fundingStatement",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Statement listing funding that was obtained for this resource",
            },
          ],
          position: 162,
        },
        {
          name: "acknowledgements",
          id: "acknowledgements",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Acknowledgement statement and citation regulation for this resource",
            },
          ],
          position: 163,
        },
        {
          name: "documentation",
          id: "documentation",
          columnType: "REFBACK",
          refTable: "Documentation",
          refLabelDefault: "${resource.id}.${name}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "Descriptive document(s) available for this resource, e.g. informed consent",
            },
          ],
          position: 164,
        },
        {
          name: "supplementary information",
          id: "supplementaryInformation",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Any other information that needs to be disclosed for this resource",
            },
          ],
          position: 165,
        },
        {
          name: "collaborations",
          id: "collaborations",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "List of relevant collaborations",
            },
          ],
          position: 166,
        },
        {
          name: "studies",
          id: "studies",
          columnType: "REFBACK",
          refTable: "Studies",
          refLabelDefault: "${id}",
          refBack: "cohorts",
          descriptions: [
            {
              locale: "en",
              value: "Listing of studies that used this cohort",
            },
          ],
          position: 167,
        },
        {
          name: "networks",
          id: "networks",
          columnType: "REFBACK",
          refTable: "Networks",
          refLabelDefault: "${id}",
          refBack: "cohorts",
          descriptions: [
            {
              locale: "en",
              value:
                "The consortia or networks that this cohort is involved in",
            },
          ],
          position: 168,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
        {
          name: "mg_tableclass",
          id: "mg_tableclass",
          columnType: "STRING",
          readonly: true,
          position: 10005,
        },
      ],
    },
    {
      name: "Collection events",
      tableType: "DATA",
      id: "CollectionEvents",
      descriptions: [
        {
          locale: "en",
          value: "Definition of a data collection event for a resource",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "resource",
          id: "resource",
          columnType: "REF",
          key: 1,
          refTable: "Extended resources",
          refLabelDefault: "${id}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Resource this collection event is part of",
            },
          ],
          position: 222,
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Name of the collection event",
            },
          ],
          position: 223,
        },
        {
          name: "description",
          id: "description",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Description of the collection event",
            },
          ],
          position: 224,
        },
        {
          name: "subcohorts",
          id: "subcohorts",
          columnType: "REF_ARRAY",
          refTable: "Subcohorts",
          refLink: "resource",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Subcohorts that are targetted by this collection event",
            },
          ],
          position: 225,
        },
        {
          name: "start year",
          id: "startYear",
          columnType: "ONTOLOGY",
          refTable: "Years",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Start year of data collection",
            },
          ],
          position: 226,
        },
        {
          name: "start month",
          id: "startMonth",
          columnType: "ONTOLOGY",
          refTable: "Months",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Start month of data collection",
            },
          ],
          position: 227,
        },
        {
          name: "end year",
          id: "endYear",
          columnType: "ONTOLOGY",
          refTable: "Years",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "End year of data collection. Leave empty if collection is ongoing",
            },
          ],
          position: 228,
        },
        {
          name: "end month",
          id: "endMonth",
          columnType: "ONTOLOGY",
          refTable: "Months",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "End month of data collection. Leave empty if collection is ongoing",
            },
          ],
          position: 229,
        },
        {
          name: "age groups",
          id: "ageGroups",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Age groups",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Age groups included in this data collection event",
            },
          ],
          position: 230,
        },
        {
          name: "number of participants",
          id: "numberOfParticipants",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value:
                "Number of participants sampled in this data collection event",
            },
          ],
          position: 231,
        },
        {
          name: "areas of information",
          id: "areasOfInformation",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Areas of information cohorts",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Areas of information that were extracted in this data collection event",
            },
          ],
          position: 232,
        },
        {
          name: "data categories",
          id: "dataCategories",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Data categories",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Methods of data collection used in this collection event",
            },
          ],
          position: 233,
        },
        {
          name: "sample categories",
          id: "sampleCategories",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Sample categories",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Samples that were collected in this collection event",
            },
          ],
          position: 234,
        },
        {
          name: "standardized tools",
          id: "standardizedTools",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Standardized tools",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Standardized tools, e.g. surveys, questionnaires, instruments used to collect data for this collection event",
            },
          ],
          position: 235,
        },
        {
          name: "standardized tools other",
          id: "standardizedToolsOther",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "If 'other', please specify",
            },
          ],
          position: 236,
        },
        {
          name: "core variables",
          id: "coreVariables",
          columnType: "STRING_ARRAY",
          descriptions: [
            {
              locale: "en",
              value:
                "Name 10-20 relevant variables that were collected in this collection event",
            },
          ],
          position: 237,
        },
        {
          name: "supplementary information",
          id: "supplementaryInformation",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Any other information that needs to be disclosed for this collection event",
            },
          ],
          position: 238,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Collection types",
      tableType: "ONTOLOGIES",
      id: "CollectionTypes",
      descriptions: [
        {
          locale: "en",
          value:
            "The data collection type of this cohort, i.e. retrospective or prospective; if both, select both",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Collection types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Collection types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Contacts",
      tableType: "DATA",
      id: "Contacts",
      externalSchema: "catalogue",
      columns: [
        {
          name: "resource",
          id: "resource",
          columnType: "REF",
          key: 1,
          refTable: "Resources",
          refLabelDefault: "${id}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Resource the contact is affiliated with",
            },
          ],
          position: 185,
        },
        {
          name: "role",
          id: "role",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Contribution types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Type(s) of contribution or role in the resource",
            },
          ],
          position: 186,
        },
        {
          name: "role description",
          id: "roleDescription",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Description of the role",
            },
          ],
          position: 187,
        },
        {
          name: "first name",
          id: "firstName",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "First name of the contact person",
            },
          ],
          position: 188,
        },
        {
          name: "last name",
          id: "lastName",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Last name of the contact person",
            },
          ],
          position: 189,
        },
        {
          name: "prefix",
          id: "prefix",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Surname prefix, if applicable",
            },
          ],
          position: 190,
        },
        {
          name: "initials",
          id: "initials",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Initials of the contact person",
            },
          ],
          position: 191,
        },
        {
          name: "title",
          id: "title",
          columnType: "ONTOLOGY",
          refTable: "Titles",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Title of the contact person",
            },
          ],
          position: 192,
        },
        {
          name: "organisation",
          id: "organisation",
          columnType: "REF",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value: "Affiliated organisation of the contact person",
            },
          ],
          position: 193,
        },
        {
          name: "email",
          id: "email",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Contact's email address",
            },
          ],
          position: 194,
        },
        {
          name: "orcid",
          id: "orcid",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Orcid of the contact person",
            },
          ],
          position: 195,
        },
        {
          name: "homepage",
          id: "homepage",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Link to contact's homepage",
            },
          ],
          position: 196,
        },
        {
          name: "photo",
          id: "photo",
          columnType: "FILE",
          descriptions: [
            {
              locale: "en",
              value: "Contact's photograph",
            },
          ],
          position: 197,
        },
        {
          name: "expertise",
          id: "expertise",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Description of contact's expertise",
            },
          ],
          position: 198,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Contribution types",
      tableType: "ONTOLOGIES",
      id: "ContributionTypes",
      descriptions: [
        {
          locale: "en",
          value: "Type(s) of contribution or role in the resource",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Contribution types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Contribution types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Countries",
      tableType: "ONTOLOGIES",
      id: "Countries",
      descriptions: [
        {
          locale: "en",
          value:
            "Country in which the institution head office or coordinating centre is located",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Countries",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Countries",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "DAP information",
      tableType: "ONTOLOGIES",
      id: "DAPInformation",
      descriptions: [
        {
          locale: "en",
          value:
            "Description of population subset, data access levels, completeness, reason for access",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "DAP information",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "DAP information",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "DAPs",
      tableType: "DATA",
      id: "DAPs",
      descriptions: [
        {
          locale: "en",
          value:
            "Data access provider relationship where an institution can provide access to (parts of) a resource",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "organisation",
          id: "organisation",
          columnType: "REF",
          key: 1,
          refTable: "Organisations",
          refLabelDefault: "${id}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Institution that provides access",
            },
          ],
          position: 398,
        },
        {
          name: "resource",
          id: "resource",
          columnType: "REF",
          key: 1,
          refTable: "Resources",
          refLabelDefault: "${id}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value:
                "Resource that is access or has experience with provided to",
            },
          ],
          position: 399,
        },
        {
          name: "population subset other",
          id: "populationSubsetOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If 'other' is selected above, describe the subset that can be accessed",
            },
          ],
          position: 400,
        },
        {
          name: "is data access provider",
          id: "isDataAccessProvider",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "DAP information",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Description of population subset, data access levels, completeness, reason for access",
            },
          ],
          position: 401,
        },
        {
          name: "reason access other",
          id: "reasonAccessOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If reason access is 'other', reason for being able to access (an extract of) the data source",
            },
          ],
          position: 402,
        },
        {
          name: "process time",
          id: "processTime",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value:
                "On average, how many DAYS does it take for approval/access to be obtained following an application for data access?",
            },
          ],
          position: 403,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Data access conditions",
      tableType: "ONTOLOGIES",
      id: "DataAccessConditions",
      descriptions: [
        {
          locale: "en",
          value: "Codes defining data access terms and conditions",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Data access conditions",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Data access conditions",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Data categories",
      tableType: "ONTOLOGIES",
      id: "DataCategories",
      descriptions: [
        {
          locale: "en",
          value: "Methods of data collection used in this collection event",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Data categories",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Data categories",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Data resources",
      tableType: "DATA",
      id: "DataResources",
      descriptions: [
        {
          locale: "en",
          value: "Resources for data",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "overview",
          id: "overview",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "General information",
            },
          ],
        },
        {
          name: "id",
          id: "id",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Internal identifier",
            },
          ],
          position: 1,
        },
        {
          name: "pid",
          id: "pid",
          columnType: "STRING",
          key: 2,
          descriptions: [
            {
              locale: "en",
              value: "Persistent identifier",
            },
          ],
          position: 2,
        },
        {
          name: "acronym",
          id: "acronym",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Acronym if applicable",
            },
          ],
          position: 3,
        },
        {
          name: "name",
          id: "name",
          columnType: "TEXT",
          key: 3,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Name used in European projects",
            },
          ],
          position: 4,
        },
        {
          name: "local name",
          id: "localName",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "If different from above, name in the national language",
            },
          ],
          position: 5,
        },
        {
          name: "keywords",
          id: "keywords",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Keywords to increase findability of this resource. Try to use words that are not used in the description",
            },
          ],
          position: 25,
        },
        {
          name: "website",
          id: "website",
          columnType: "HYPERLINK",
          descriptions: [
            {
              locale: "en",
              value: "Link to the website or homepage",
            },
          ],
          position: 26,
        },
        {
          name: "lead organisation",
          id: "leadOrganisation",
          columnType: "REF_ARRAY",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "lead organisation (e.g. research department or group) for this resource",
            },
          ],
          position: 27,
        },
        {
          name: "additional organisations",
          id: "additionalOrganisations",
          columnType: "REF_ARRAY",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "List the names of any additional organisations that contributed to the resource",
            },
          ],
          position: 28,
        },
        {
          name: "description",
          id: "description",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Short description",
            },
          ],
          position: 31,
        },
        {
          name: "external identifiers",
          id: "externalIdentifiers",
          columnType: "REFBACK",
          refTable: "External identifiers",
          refLabelDefault: "${resource.id}.${identifier}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "External identifier(s) for this resource (e.g. EUPASS number, UMCG register number)",
            },
          ],
          position: 37,
        },
        {
          name: "contacts",
          id: "contacts",
          columnType: "REFBACK",
          refTable: "Contacts",
          refLabelDefault: "${resource.id}.${firstName}.${lastName}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value: "Contact person(s)",
            },
          ],
          position: 41,
        },
        {
          name: "logo",
          id: "logo",
          columnType: "FILE",
          descriptions: [
            {
              locale: "en",
              value: "Logo of the resource, for use on homepages etc.",
            },
          ],
          position: 48,
        },
        {
          name: "population",
          id: "population",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value:
                "Description of the population that can potentially be captured in the resource",
            },
          ],
          position: 49,
        },
        {
          name: "number of participants",
          id: "numberOfParticipants",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "Total number of individuals for which data is collected",
            },
          ],
          position: 50,
        },
        {
          name: "number of participants with samples",
          id: "numberOfParticipantsWithSamples",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "Number of individuals for which samples are collected",
            },
          ],
          position: 51,
        },
        {
          name: "countries",
          id: "countries",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Countries",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Countries where data from this resource largely originate from",
            },
          ],
          position: 53,
        },
        {
          name: "regions",
          id: "regions",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Regions",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Geographical regions where data from this resource largely originate from",
            },
          ],
          position: 54,
        },
        {
          name: "population age groups",
          id: "populationAgeGroups",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Age groups",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Which population age groups are captured in this resource? Select all that are relevant.",
            },
          ],
          position: 55,
        },
        {
          name: "population disease",
          id: "populationDisease",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Diseases",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Does the resource collect information on a specific disease subpopulation (e.g., as in a disease-specific registry)?",
            },
          ],
          position: 66,
        },
        {
          name: "population oncology topology",
          id: "populationOncologyTopology",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "ICDO topologies",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Does the resource collect information on specific cancer subtype(s)? If yes, select topology specifications.",
            },
          ],
          position: 67,
        },
        {
          name: "population oncology morphology",
          id: "populationOncologyMorphology",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "ICDO morphologies",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Does the resource collect information on specific cancer subtype(s)? If yes, select morphology specifications.",
            },
          ],
          position: 68,
        },
        {
          name: "contents",
          id: "contents",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Data model and contents",
            },
          ],
          position: 73,
        },
        {
          name: "datasets",
          id: "datasets",
          columnType: "REFBACK",
          refTable: "Datasets",
          refLabelDefault: "${resource.id}.${name}",
          refBack: "resource",
          position: 74,
        },
        {
          name: "access",
          id: "access",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Access and validation information",
            },
          ],
          position: 106,
        },
        {
          name: "data holder",
          id: "dataHolder",
          columnType: "REF",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "The name of the organisation that is responsible for governance of the data bank",
            },
          ],
          position: 107,
        },
        {
          name: "DAPs",
          id: "dAPs",
          columnType: "REFBACK",
          refTable: "DAPs",
          refLabelDefault: "${organisation.id}.${resource.id}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "List of DAPs that are listed in the catalogue as having (conditional) permission to access (an extract of) the data resource",
            },
          ],
          position: 108,
        },
        {
          name: "information",
          id: "information",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Other information",
            },
          ],
          position: 156,
        },
        {
          name: "design paper",
          id: "designPaper",
          columnType: "REF_ARRAY",
          refTable: "Publications",
          refLabelDefault: "${doi}",
          descriptions: [
            {
              locale: "en",
              value:
                "Publication(s) that describe(s) the design of this resource",
            },
          ],
          position: 157,
        },
        {
          name: "publications",
          id: "publications",
          columnType: "REF_ARRAY",
          refTable: "Publications",
          refLabelDefault: "${doi}",
          descriptions: [
            {
              locale: "en",
              value: "Other publication(s) about this resource",
            },
          ],
          position: 158,
        },
        {
          name: "informed consent type",
          id: "informedConsentType",
          columnType: "ONTOLOGY",
          refTable: "Informed consent types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "What type of informed consent was given for data collection?",
            },
          ],
          position: 159,
        },
        {
          name: "funding statement",
          id: "fundingStatement",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Statement listing funding that was obtained for this resource",
            },
          ],
          position: 162,
        },
        {
          name: "acknowledgements",
          id: "acknowledgements",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Acknowledgement statement and citation regulation for this resource",
            },
          ],
          position: 163,
        },
        {
          name: "documentation",
          id: "documentation",
          columnType: "REFBACK",
          refTable: "Documentation",
          refLabelDefault: "${resource.id}.${name}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "Descriptive document(s) available for this resource, e.g. informed consent",
            },
          ],
          position: 164,
        },
        {
          name: "supplementary information",
          id: "supplementaryInformation",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Any other information that needs to be disclosed for this resource",
            },
          ],
          position: 165,
        },
        {
          name: "collaborations",
          id: "collaborations",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "List of relevant collaborations",
            },
          ],
          position: 166,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
        {
          name: "mg_tableclass",
          id: "mg_tableclass",
          columnType: "STRING",
          readonly: true,
          position: 10005,
        },
      ],
    },
    {
      name: "Data sources",
      tableType: "DATA",
      id: "DataSources",
      descriptions: [
        {
          locale: "en",
          value:
            "Collections of multiple data banks covering the same population",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "overview",
          id: "overview",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "General information",
            },
          ],
        },
        {
          name: "id",
          id: "id",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Internal identifier",
            },
          ],
          position: 1,
        },
        {
          name: "pid",
          id: "pid",
          columnType: "STRING",
          key: 2,
          descriptions: [
            {
              locale: "en",
              value: "Persistent identifier",
            },
          ],
          position: 2,
        },
        {
          name: "acronym",
          id: "acronym",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Acronym if applicable",
            },
          ],
          position: 3,
        },
        {
          name: "name",
          id: "name",
          columnType: "TEXT",
          key: 3,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Name used in European projects",
            },
          ],
          position: 4,
        },
        {
          name: "local name",
          id: "localName",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "If different from above, name in the national language",
            },
          ],
          position: 5,
        },
        {
          name: "type",
          id: "type",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Datasource types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Which of the following families of databanks best describe this data source",
            },
          ],
          position: 23,
        },
        {
          name: "type other",
          id: "typeOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If other, describe the type of datasource",
            },
          ],
          position: 24,
        },
        {
          name: "keywords",
          id: "keywords",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Keywords to increase findability of this resource. Try to use words that are not used in the description",
            },
          ],
          position: 25,
        },
        {
          name: "website",
          id: "website",
          columnType: "HYPERLINK",
          descriptions: [
            {
              locale: "en",
              value: "Link to the website or homepage",
            },
          ],
          position: 26,
        },
        {
          name: "lead organisation",
          id: "leadOrganisation",
          columnType: "REF_ARRAY",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "lead organisation (e.g. research department or group) for this resource",
            },
          ],
          position: 27,
        },
        {
          name: "additional organisations",
          id: "additionalOrganisations",
          columnType: "REF_ARRAY",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "List the names of any additional organisations that contributed to the resource",
            },
          ],
          position: 28,
        },
        {
          name: "description",
          id: "description",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Short description",
            },
          ],
          position: 31,
        },
        {
          name: "data collection description",
          id: "dataCollectionDescription",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Describe the process of collection and recording of data.",
            },
          ],
          position: 32,
        },
        {
          name: "date established",
          id: "dateEstablished",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value:
                "Date when the data source was first established. If the exact day of the month is not known, please enter 15. If the exact month is not known then please enter 15/06",
            },
          ],
          position: 33,
        },
        {
          name: "start data collection",
          id: "startDataCollection",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value:
                "The date when data started to be collected or extracted. If the exact day of the month is not known, please enter 15. If the exact month is not known then please enter 15/06",
            },
          ],
          position: 34,
        },
        {
          name: "end data collection",
          id: "endDataCollection",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value:
                "If data collection in the data source has ceased, on what date did new records last enter the data source?. If the exact day of the month is not known, please enter 15. If the exact month is not known then please enter 15/06",
            },
          ],
          position: 35,
        },
        {
          name: "time span description",
          id: "timeSpanDescription",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Description of time span",
            },
          ],
          position: 36,
        },
        {
          name: "external identifiers",
          id: "externalIdentifiers",
          columnType: "REFBACK",
          refTable: "External identifiers",
          refLabelDefault: "${resource.id}.${identifier}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "External identifier(s) for this resource (e.g. EUPASS number, UMCG register number)",
            },
          ],
          position: 37,
        },
        {
          name: "contacts",
          id: "contacts",
          columnType: "REFBACK",
          refTable: "Contacts",
          refLabelDefault: "${resource.id}.${firstName}.${lastName}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value: "Contact person(s)",
            },
          ],
          position: 41,
        },
        {
          name: "logo",
          id: "logo",
          columnType: "FILE",
          descriptions: [
            {
              locale: "en",
              value: "Logo of the resource, for use on homepages etc.",
            },
          ],
          position: 48,
        },
        {
          name: "population",
          id: "population",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value:
                "Description of the population that can potentially be captured in the resource",
            },
          ],
          position: 49,
        },
        {
          name: "number of participants",
          id: "numberOfParticipants",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "Total number of individuals for which data is collected",
            },
          ],
          position: 50,
        },
        {
          name: "number of participants with samples",
          id: "numberOfParticipantsWithSamples",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "Number of individuals for which samples are collected",
            },
          ],
          position: 51,
        },
        {
          name: "countries",
          id: "countries",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Countries",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Countries where data from this resource largely originate from",
            },
          ],
          position: 53,
        },
        {
          name: "regions",
          id: "regions",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Regions",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Geographical regions where data from this resource largely originate from",
            },
          ],
          position: 54,
        },
        {
          name: "population age groups",
          id: "populationAgeGroups",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Age groups",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Which population age groups are captured in this resource? Select all that are relevant.",
            },
          ],
          position: 55,
        },
        {
          name: "population entry",
          id: "populationEntry",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Population entry",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Select the possible causes / events that trigger the registration of a person in the data source",
            },
          ],
          position: 62,
        },
        {
          name: "population entry other",
          id: "populationEntryOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If other, specify the causes of entry to the underlying population",
            },
          ],
          position: 63,
        },
        {
          name: "population exit",
          id: "populationExit",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Population exit",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Select the possible causes / events that trigger the de-registration of a person in the data source",
            },
          ],
          position: 64,
        },
        {
          name: "population exit other",
          id: "populationExitOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If other, specify the causes of exit from the underlying population",
            },
          ],
          position: 65,
        },
        {
          name: "population disease",
          id: "populationDisease",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Diseases",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Does the resource collect information on a specific disease subpopulation (e.g., as in a disease-specific registry)?",
            },
          ],
          position: 66,
        },
        {
          name: "population oncology topology",
          id: "populationOncologyTopology",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "ICDO topologies",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Does the resource collect information on specific cancer subtype(s)? If yes, select topology specifications.",
            },
          ],
          position: 67,
        },
        {
          name: "population oncology morphology",
          id: "populationOncologyMorphology",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "ICDO morphologies",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Does the resource collect information on specific cancer subtype(s)? If yes, select morphology specifications.",
            },
          ],
          position: 68,
        },
        {
          name: "population coverage",
          id: "populationCoverage",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Estimated percentage of the population covered by the data source in the catchment area. Please describe the denominator.",
            },
          ],
          position: 69,
        },
        {
          name: "population not covered",
          id: "populationNotCovered",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Description of the population covered by the data source in the catchment area whose data are not collected, where applicable (e.g.: people who are registered only for private care)",
            },
          ],
          position: 70,
        },
        {
          name: "quantantitative information",
          id: "quantantitativeInformation",
          columnType: "REFBACK",
          refTable: "Quantitative information",
          refLabelDefault: "${resource.id}.${ageGroup.name}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value: "Numerical summaries describing data bank population",
            },
          ],
          position: 71,
        },
        {
          name: "contents",
          id: "contents",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Data model and contents",
            },
          ],
          position: 73,
        },
        {
          name: "datasets",
          id: "datasets",
          columnType: "REFBACK",
          refTable: "Datasets",
          refLabelDefault: "${resource.id}.${name}",
          refBack: "resource",
          position: 74,
        },
        {
          name: "mappings to data models",
          id: "mappingsToDataModels",
          columnType: "REFBACK",
          refTable: "Dataset mappings",
          refLabelDefault:
            "${source.id}.${sourceDataset.name}.${target.id}.${targetDataset.name}",
          refBack: "source",
          descriptions: [
            {
              locale: "en",
              value: "overview of dataset mappings available",
            },
          ],
          position: 75,
        },
        {
          name: "areas of information",
          id: "areasOfInformation",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Areas of information ds",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Areas of information that were collected",
            },
          ],
          position: 76,
        },
        {
          name: "quality of life other",
          id: "qualityOfLifeOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If other, specify additional quality of life measures",
            },
          ],
          position: 77,
        },
        {
          name: "cause of death code other",
          id: "causeOfDeathCodeOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what cause of death vocabulary is used?",
            },
          ],
          position: 78,
        },
        {
          name: "indication vocabulary other",
          id: "indicationVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what indication for use vocabulary is used?",
            },
          ],
          position: 79,
        },
        {
          name: "genetic data vocabulary other",
          id: "geneticDataVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what genetic data vocabulary is used?",
            },
          ],
          position: 80,
        },
        {
          name: "care setting other",
          id: "careSettingOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' description of the setting of care",
            },
          ],
          position: 81,
        },
        {
          name: "medicinal product vocabulary other",
          id: "medicinalProductVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If 'other,' description of the medicinal product vocabulary",
            },
          ],
          position: 82,
        },
        {
          name: "prescriptions vocabulary other",
          id: "prescriptionsVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what vocabulary is used?",
            },
          ],
          position: 83,
        },
        {
          name: "dispensings vocabulary other",
          id: "dispensingsVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what vocabulary is used?",
            },
          ],
          position: 84,
        },
        {
          name: "procedures vocabulary other",
          id: "proceduresVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what vocabulary is used?",
            },
          ],
          position: 85,
        },
        {
          name: "biomarker data vocabulary other",
          id: "biomarkerDataVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what vocabulary is used?",
            },
          ],
          position: 86,
        },
        {
          name: "diagnosis medical event vocabulary other",
          id: "diagnosisMedicalEventVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what vocabulary is used?",
            },
          ],
          position: 87,
        },
        {
          name: "disease details",
          id: "diseaseDetails",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "MedDRA",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "If data on a specific disease is collected, which diseases does the data source collect information on",
            },
          ],
          position: 89,
        },
        {
          name: "disease details other",
          id: "diseaseDetailsOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Specify disease details if not present in MedDRA",
            },
          ],
          position: 90,
        },
        {
          name: "biospecimen collected",
          id: "biospecimenCollected",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Biospecimens",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "If the data bank contains biospecimens, what types of specimen",
            },
          ],
          position: 91,
        },
        {
          name: "languages",
          id: "languages",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Languages",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Languages in which that the records are recorded (in ISO 639, https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes)",
            },
          ],
          position: 92,
        },
        {
          name: "record trigger",
          id: "recordTrigger",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "What triggers the creation of a record in the data bank? e.g., hospital discharge, specialist encounter, dispensation of a medicinal product, recording of a congenital anomaly",
            },
          ],
          position: 93,
        },
        {
          name: "linkage",
          id: "linkage",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Data linkage",
            },
          ],
          position: 97,
        },
        {
          name: "linkage description",
          id: "linkageDescription",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Provide a high-level description of the linkages that are either: currently available between data sources in the data source (when pre-linked = yes); linkages that are possible when using the data source",
            },
          ],
          position: 100,
        },
        {
          name: "linkage possibility",
          id: "linkagePossibility",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value: "Can this data source be linked to other data sources?",
            },
          ],
          position: 101,
        },
        {
          name: "linked resources",
          id: "linkedResources",
          columnType: "REFBACK",
          refTable: "Linked resources",
          refLabelDefault: "${mainResource.id}.${linkedResource.id}",
          refBack: "main resource",
          descriptions: [
            {
              locale: "en",
              value:
                "List of resources that are linked into this main resource",
            },
          ],
          position: 102,
        },
        {
          name: "access",
          id: "access",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Access and validation information",
            },
          ],
          position: 106,
        },
        {
          name: "data holder",
          id: "dataHolder",
          columnType: "REF",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "The name of the organisation that is responsible for governance of the data bank",
            },
          ],
          position: 107,
        },
        {
          name: "DAPs",
          id: "dAPs",
          columnType: "REFBACK",
          refTable: "DAPs",
          refLabelDefault: "${organisation.id}.${resource.id}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "List of DAPs that are listed in the catalogue as having (conditional) permission to access (an extract of) the data resource",
            },
          ],
          position: 108,
        },
        {
          name: "informed consent",
          id: "informedConsent",
          columnType: "ONTOLOGY",
          refTable: "Informed consents",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Is informed consent required for use of the data for research purposes?",
            },
          ],
          position: 114,
        },
        {
          name: "informed consent other",
          id: "informedConsentOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If other, describe the conditions when informed consent is required",
            },
          ],
          position: 115,
        },
        {
          name: "access identifiable data",
          id: "accessIdentifiableData",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Can identifiable data be accessed in the data bank (including patient/practitioner name/practice name)?",
            },
          ],
          position: 116,
        },
        {
          name: "access identifiable data route",
          id: "accessIdentifiableDataRoute",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If yes above, what is the route to access or process this information? What permission is required?",
            },
          ],
          position: 117,
        },
        {
          name: "access subject details",
          id: "accessSubjectDetails",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Can individual patients/practitioners/practices be contacted in the data bank?",
            },
          ],
          position: 118,
        },
        {
          name: "access subject details route",
          id: "accessSubjectDetailsRoute",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If yes above, what is the route to access or process this information? What permission is required?",
            },
          ],
          position: 119,
        },
        {
          name: "audit possible",
          id: "auditPossible",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Are external parties allowed to audit the data? For example, is it possible for an external party to audit the quality or validity of the data source?",
            },
          ],
          position: 120,
        },
        {
          name: "standard operating procedures",
          id: "standardOperatingProcedures",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Is there a standard operating procedure document that defines the processes and procedures for data capture and management?",
            },
          ],
          position: 125,
        },
        {
          name: "biospecimen access",
          id: "biospecimenAccess",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "If the data bank contains biospecimens (e.g., tissue samples), can these be retrieved?",
            },
          ],
          position: 126,
        },
        {
          name: "biospecimen access conditions",
          id: "biospecimenAccessConditions",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If yes above, describe the conditions under which permission to retrieve biospecimens may be granted",
            },
          ],
          position: 127,
        },
        {
          name: "governance details",
          id: "governanceDetails",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If available, provide a link to documents or webpages that describe the overall governance of the data source bank (governing data access or utilisation for research purposes by existing DAPs)",
            },
          ],
          position: 128,
        },
        {
          name: "approval for publication",
          id: "approvalForPublication",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Is an approval needed to publish the results of a study using the data",
            },
          ],
          position: 129,
        },
        {
          name: "updates",
          id: "updates",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Information on the regularity of updates and time lags",
            },
          ],
          position: 130,
        },
        {
          name: "preservation",
          id: "preservation",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value: "Are records preserved in the data bank indefinitely?",
            },
          ],
          position: 133,
        },
        {
          name: "preservation duration",
          id: "preservationDuration",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value:
                "If no to the above, for how long (in years) are records preserved in the data bank?",
            },
          ],
          position: 134,
        },
        {
          name: "refresh period",
          id: "refreshPeriod",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Refresh periods",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "If data are refreshed on fixed dates (e.g., every June and December), when are the refreshes scheduled? Select all that apply from the following:",
            },
          ],
          position: 135,
        },
        {
          name: "date last refresh",
          id: "dateLastRefresh",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value: "Date of last update/refresh",
            },
          ],
          position: 136,
        },
        {
          name: "quality",
          id: "quality",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "List of relevant studies conducted using the data bank",
            },
          ],
          position: 137,
        },
        {
          name: "qualification",
          id: "qualification",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Has the data source successfully undergone a formal qualification process (e.g., from the EMA, or ISO or other certifications)?",
            },
          ],
          position: 138,
        },
        {
          name: "qualifications description",
          id: "qualificationsDescription",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Has the resource successfully undergone a qualification process (e.g., from the EMA)? If yes, describe the qualification(s) granted",
            },
          ],
          position: 139,
        },
        {
          name: "access for validation",
          id: "accessForValidation",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Can validity of the data in the data bank be verified, e.g., by review of origin medical charts?",
            },
          ],
          position: 146,
        },
        {
          name: "quality validation frequency",
          id: "qualityValidationFrequency",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "How often are data quality checks and validation steps conducted on the data bank?",
            },
          ],
          position: 147,
        },
        {
          name: "quality validation methods",
          id: "qualityValidationMethods",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "What methods or processes are applied for data quality checks and validation steps conducted on the data bank?",
            },
          ],
          position: 148,
        },
        {
          name: "correction methods",
          id: "correctionMethods",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "What methods or processes are applied to correct illogical values in the data bank?",
            },
          ],
          position: 149,
        },
        {
          name: "quality validation results",
          id: "qualityValidationResults",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value:
                "If available, provide a link to a publication of the data quality check and validation results",
            },
          ],
          position: 150,
        },
        {
          name: "standards",
          id: "standards",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Use of standard data models and ontologies",
            },
          ],
          position: 151,
        },
        {
          name: "cdms",
          id: "cdms",
          columnType: "REFBACK",
          refTable: "Mappings",
          refLabelDefault: "${source.id}.${target.id}",
          refBack: "source",
          descriptions: [
            {
              locale: "en",
              value: "Common data models used or ETL-ed to by this data source",
            },
          ],
          position: 152,
        },
        {
          name: "cdms other",
          id: "cdmsOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If not in list above, give the name of cdm(s) used by this data source",
            },
          ],
          position: 153,
        },
        {
          name: "information",
          id: "information",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Other information",
            },
          ],
          position: 156,
        },
        {
          name: "design paper",
          id: "designPaper",
          columnType: "REF_ARRAY",
          refTable: "Publications",
          refLabelDefault: "${doi}",
          descriptions: [
            {
              locale: "en",
              value:
                "Publication(s) that describe(s) the design of this resource",
            },
          ],
          position: 157,
        },
        {
          name: "publications",
          id: "publications",
          columnType: "REF_ARRAY",
          refTable: "Publications",
          refLabelDefault: "${doi}",
          descriptions: [
            {
              locale: "en",
              value: "Other publication(s) about this resource",
            },
          ],
          position: 158,
        },
        {
          name: "informed consent type",
          id: "informedConsentType",
          columnType: "ONTOLOGY",
          refTable: "Informed consent types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "What type of informed consent was given for data collection?",
            },
          ],
          position: 159,
        },
        {
          name: "funding sources",
          id: "fundingSources",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Funding types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Specify the main financial support sources for the data source in the last 3 years. Select all that apply",
            },
          ],
          position: 160,
        },
        {
          name: "funding statement",
          id: "fundingStatement",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Statement listing funding that was obtained for this resource",
            },
          ],
          position: 162,
        },
        {
          name: "acknowledgements",
          id: "acknowledgements",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Acknowledgement statement and citation regulation for this resource",
            },
          ],
          position: 163,
        },
        {
          name: "documentation",
          id: "documentation",
          columnType: "REFBACK",
          refTable: "Documentation",
          refLabelDefault: "${resource.id}.${name}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "Descriptive document(s) available for this resource, e.g. informed consent",
            },
          ],
          position: 164,
        },
        {
          name: "supplementary information",
          id: "supplementaryInformation",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Any other information that needs to be disclosed for this resource",
            },
          ],
          position: 165,
        },
        {
          name: "collaborations",
          id: "collaborations",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "List of relevant collaborations",
            },
          ],
          position: 166,
        },
        {
          name: "networks",
          id: "networks",
          columnType: "REFBACK",
          refTable: "Networks",
          refLabelDefault: "${id}",
          refBack: "data sources",
          descriptions: [
            {
              locale: "en",
              value: "List of networks that this datasource is associated with",
            },
          ],
          position: 171,
        },
        {
          name: "studies",
          id: "studies",
          columnType: "REFBACK",
          refTable: "Studies",
          refLabelDefault: "${id}",
          refBack: "data sources",
          descriptions: [
            {
              locale: "en",
              value: "List of studies that this datasource is associated with",
            },
          ],
          position: 172,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
        {
          name: "mg_tableclass",
          id: "mg_tableclass",
          columnType: "STRING",
          readonly: true,
          position: 10005,
        },
      ],
    },
    {
      name: "Data use conditions",
      tableType: "ONTOLOGIES",
      id: "DataUseConditions",
      descriptions: [
        {
          locale: "en",
          value: "Codes defining data use terms and conditions",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Data use conditions",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Data use conditions",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Databanks",
      tableType: "DATA",
      id: "Databanks",
      descriptions: [
        {
          locale: "en",
          value:
            "Data collection from real world databases such as health records, registries",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "overview",
          id: "overview",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "General information",
            },
          ],
        },
        {
          name: "id",
          id: "id",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Internal identifier",
            },
          ],
          position: 1,
        },
        {
          name: "pid",
          id: "pid",
          columnType: "STRING",
          key: 2,
          descriptions: [
            {
              locale: "en",
              value: "Persistent identifier",
            },
          ],
          position: 2,
        },
        {
          name: "acronym",
          id: "acronym",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Acronym if applicable",
            },
          ],
          position: 3,
        },
        {
          name: "name",
          id: "name",
          columnType: "TEXT",
          key: 3,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Name used in European projects",
            },
          ],
          position: 4,
        },
        {
          name: "local name",
          id: "localName",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "If different from above, name in the national language",
            },
          ],
          position: 5,
        },
        {
          name: "type",
          id: "type",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Datasource types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Which of the following families of databanks best describe this data source",
            },
          ],
          position: 23,
        },
        {
          name: "type other",
          id: "typeOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If other, describe the type of datasource",
            },
          ],
          position: 24,
        },
        {
          name: "keywords",
          id: "keywords",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Keywords to increase findability of this resource. Try to use words that are not used in the description",
            },
          ],
          position: 25,
        },
        {
          name: "website",
          id: "website",
          columnType: "HYPERLINK",
          descriptions: [
            {
              locale: "en",
              value: "Link to the website or homepage",
            },
          ],
          position: 26,
        },
        {
          name: "lead organisation",
          id: "leadOrganisation",
          columnType: "REF_ARRAY",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "lead organisation (e.g. research department or group) for this resource",
            },
          ],
          position: 27,
        },
        {
          name: "additional organisations",
          id: "additionalOrganisations",
          columnType: "REF_ARRAY",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "List the names of any additional organisations that contributed to the resource",
            },
          ],
          position: 28,
        },
        {
          name: "description",
          id: "description",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Short description",
            },
          ],
          position: 31,
        },
        {
          name: "data collection description",
          id: "dataCollectionDescription",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Describe the process of collection and recording of data.",
            },
          ],
          position: 32,
        },
        {
          name: "date established",
          id: "dateEstablished",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value:
                "Date when the data source was first established. If the exact day of the month is not known, please enter 15. If the exact month is not known then please enter 15/06",
            },
          ],
          position: 33,
        },
        {
          name: "start data collection",
          id: "startDataCollection",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value:
                "The date when data started to be collected or extracted. If the exact day of the month is not known, please enter 15. If the exact month is not known then please enter 15/06",
            },
          ],
          position: 34,
        },
        {
          name: "end data collection",
          id: "endDataCollection",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value:
                "If data collection in the data source has ceased, on what date did new records last enter the data source?. If the exact day of the month is not known, please enter 15. If the exact month is not known then please enter 15/06",
            },
          ],
          position: 35,
        },
        {
          name: "time span description",
          id: "timeSpanDescription",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Description of time span",
            },
          ],
          position: 36,
        },
        {
          name: "external identifiers",
          id: "externalIdentifiers",
          columnType: "REFBACK",
          refTable: "External identifiers",
          refLabelDefault: "${resource.id}.${identifier}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "External identifier(s) for this resource (e.g. EUPASS number, UMCG register number)",
            },
          ],
          position: 37,
        },
        {
          name: "contacts",
          id: "contacts",
          columnType: "REFBACK",
          refTable: "Contacts",
          refLabelDefault: "${resource.id}.${firstName}.${lastName}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value: "Contact person(s)",
            },
          ],
          position: 41,
        },
        {
          name: "logo",
          id: "logo",
          columnType: "FILE",
          descriptions: [
            {
              locale: "en",
              value: "Logo of the resource, for use on homepages etc.",
            },
          ],
          position: 48,
        },
        {
          name: "population",
          id: "population",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value:
                "Description of the population that can potentially be captured in the resource",
            },
          ],
          position: 49,
        },
        {
          name: "number of participants",
          id: "numberOfParticipants",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "Total number of individuals for which data is collected",
            },
          ],
          position: 50,
        },
        {
          name: "number of participants with samples",
          id: "numberOfParticipantsWithSamples",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "Number of individuals for which samples are collected",
            },
          ],
          position: 51,
        },
        {
          name: "underlying population",
          id: "underlyingPopulation",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Provide a summary description of the underlying population (maximum 100 words) or URL to a description",
            },
          ],
          position: 52,
        },
        {
          name: "countries",
          id: "countries",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Countries",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Countries where data from this resource largely originate from",
            },
          ],
          position: 53,
        },
        {
          name: "regions",
          id: "regions",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Regions",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Geographical regions where data from this resource largely originate from",
            },
          ],
          position: 54,
        },
        {
          name: "population age groups",
          id: "populationAgeGroups",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Age groups",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Which population age groups are captured in this resource? Select all that are relevant.",
            },
          ],
          position: 55,
        },
        {
          name: "population entry",
          id: "populationEntry",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Population entry",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Select the possible causes / events that trigger the registration of a person in the data source",
            },
          ],
          position: 62,
        },
        {
          name: "population entry other",
          id: "populationEntryOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If other, specify the causes of entry to the underlying population",
            },
          ],
          position: 63,
        },
        {
          name: "population exit",
          id: "populationExit",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Population exit",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Select the possible causes / events that trigger the de-registration of a person in the data source",
            },
          ],
          position: 64,
        },
        {
          name: "population exit other",
          id: "populationExitOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If other, specify the causes of exit from the underlying population",
            },
          ],
          position: 65,
        },
        {
          name: "population disease",
          id: "populationDisease",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Diseases",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Does the resource collect information on a specific disease subpopulation (e.g., as in a disease-specific registry)?",
            },
          ],
          position: 66,
        },
        {
          name: "population oncology topology",
          id: "populationOncologyTopology",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "ICDO topologies",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Does the resource collect information on specific cancer subtype(s)? If yes, select topology specifications.",
            },
          ],
          position: 67,
        },
        {
          name: "population oncology morphology",
          id: "populationOncologyMorphology",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "ICDO morphologies",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Does the resource collect information on specific cancer subtype(s)? If yes, select morphology specifications.",
            },
          ],
          position: 68,
        },
        {
          name: "population coverage",
          id: "populationCoverage",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Estimated percentage of the population covered by the data source in the catchment area. Please describe the denominator.",
            },
          ],
          position: 69,
        },
        {
          name: "population not covered",
          id: "populationNotCovered",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Description of the population covered by the data source in the catchment area whose data are not collected, where applicable (e.g.: people who are registered only for private care)",
            },
          ],
          position: 70,
        },
        {
          name: "quantantitative information",
          id: "quantantitativeInformation",
          columnType: "REFBACK",
          refTable: "Quantitative information",
          refLabelDefault: "${resource.id}.${ageGroup.name}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value: "Numerical summaries describing data bank population",
            },
          ],
          position: 71,
        },
        {
          name: "contents",
          id: "contents",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Data model and contents",
            },
          ],
          position: 73,
        },
        {
          name: "datasets",
          id: "datasets",
          columnType: "REFBACK",
          refTable: "Datasets",
          refLabelDefault: "${resource.id}.${name}",
          refBack: "resource",
          position: 74,
        },
        {
          name: "mappings to data models",
          id: "mappingsToDataModels",
          columnType: "REFBACK",
          refTable: "Dataset mappings",
          refLabelDefault:
            "${source.id}.${sourceDataset.name}.${target.id}.${targetDataset.name}",
          refBack: "source",
          descriptions: [
            {
              locale: "en",
              value: "overview of dataset mappings available",
            },
          ],
          position: 75,
        },
        {
          name: "areas of information",
          id: "areasOfInformation",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Areas of information ds",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Areas of information that were collected",
            },
          ],
          position: 76,
        },
        {
          name: "quality of life other",
          id: "qualityOfLifeOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If other, specify additional quality of life measures",
            },
          ],
          position: 77,
        },
        {
          name: "cause of death code other",
          id: "causeOfDeathCodeOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what cause of death vocabulary is used?",
            },
          ],
          position: 78,
        },
        {
          name: "indication vocabulary other",
          id: "indicationVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what indication for use vocabulary is used?",
            },
          ],
          position: 79,
        },
        {
          name: "genetic data vocabulary other",
          id: "geneticDataVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what genetic data vocabulary is used?",
            },
          ],
          position: 80,
        },
        {
          name: "care setting other",
          id: "careSettingOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' description of the setting of care",
            },
          ],
          position: 81,
        },
        {
          name: "medicinal product vocabulary other",
          id: "medicinalProductVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If 'other,' description of the medicinal product vocabulary",
            },
          ],
          position: 82,
        },
        {
          name: "prescriptions vocabulary other",
          id: "prescriptionsVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what vocabulary is used?",
            },
          ],
          position: 83,
        },
        {
          name: "dispensings vocabulary other",
          id: "dispensingsVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what vocabulary is used?",
            },
          ],
          position: 84,
        },
        {
          name: "procedures vocabulary other",
          id: "proceduresVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what vocabulary is used?",
            },
          ],
          position: 85,
        },
        {
          name: "biomarker data vocabulary other",
          id: "biomarkerDataVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what vocabulary is used?",
            },
          ],
          position: 86,
        },
        {
          name: "diagnosis medical event vocabulary other",
          id: "diagnosisMedicalEventVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what vocabulary is used?",
            },
          ],
          position: 87,
        },
        {
          name: "data dictionary available",
          id: "dataDictionaryAvailable",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value: "Are a data dictionary and a data model available?",
            },
          ],
          position: 88,
        },
        {
          name: "disease details",
          id: "diseaseDetails",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "MedDRA",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "If data on a specific disease is collected, which diseases does the data source collect information on",
            },
          ],
          position: 89,
        },
        {
          name: "disease details other",
          id: "diseaseDetailsOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Specify disease details if not present in MedDRA",
            },
          ],
          position: 90,
        },
        {
          name: "biospecimen collected",
          id: "biospecimenCollected",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Biospecimens",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "If the data bank contains biospecimens, what types of specimen",
            },
          ],
          position: 91,
        },
        {
          name: "languages",
          id: "languages",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Languages",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Languages in which that the records are recorded (in ISO 639, https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes)",
            },
          ],
          position: 92,
        },
        {
          name: "record trigger",
          id: "recordTrigger",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "What triggers the creation of a record in the data bank? e.g., hospital discharge, specialist encounter, dispensation of a medicinal product, recording of a congenital anomaly",
            },
          ],
          position: 93,
        },
        {
          name: "unit of observation",
          id: "unitOfObservation",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Based on the prompt, what is the unit of observation of a record (e.g., person, prescription)?",
            },
          ],
          position: 95,
        },
        {
          name: "multiple entries",
          id: "multipleEntries",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Can there be multiple entries for a single person in the data bank? For example, may a person contribute multiple records to the data bank?",
            },
          ],
          position: 96,
        },
        {
          name: "linkage",
          id: "linkage",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Data linkage",
            },
          ],
          position: 97,
        },
        {
          name: "has identifier",
          id: "hasIdentifier",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Is there a unique identifier for a person in the data bank?",
            },
          ],
          position: 98,
        },
        {
          name: "identifier description",
          id: "identifierDescription",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Describe the variable that is used as a unique identifier for a person in the data bank? If the unique identifier is not at level of a person (for example hospital encounter), describe how this translated to an individual level",
            },
          ],
          position: 99,
        },
        {
          name: "linkage description",
          id: "linkageDescription",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Provide a high-level description of the linkages that are either: currently available between data sources in the data source (when pre-linked = yes); linkages that are possible when using the data source",
            },
          ],
          position: 100,
        },
        {
          name: "linkage possibility",
          id: "linkagePossibility",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value: "Can this data source be linked to other data sources?",
            },
          ],
          position: 101,
        },
        {
          name: "linked resources",
          id: "linkedResources",
          columnType: "REFBACK",
          refTable: "Linked resources",
          refLabelDefault: "${mainResource.id}.${linkedResource.id}",
          refBack: "main resource",
          descriptions: [
            {
              locale: "en",
              value:
                "List of resources that are linked into this main resource",
            },
          ],
          position: 102,
        },
        {
          name: "access",
          id: "access",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Access and validation information",
            },
          ],
          position: 106,
        },
        {
          name: "data holder",
          id: "dataHolder",
          columnType: "REF",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "The name of the organisation that is responsible for governance of the data bank",
            },
          ],
          position: 107,
        },
        {
          name: "DAPs",
          id: "dAPs",
          columnType: "REFBACK",
          refTable: "DAPs",
          refLabelDefault: "${organisation.id}.${resource.id}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "List of DAPs that are listed in the catalogue as having (conditional) permission to access (an extract of) the data resource",
            },
          ],
          position: 108,
        },
        {
          name: "reason sustained",
          id: "reasonSustained",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Description of the reason why the data bank is sustained by the organisation (e.g., for surveillance, clinical purposes, financial or administrative purposes, research purposes)",
            },
          ],
          position: 109,
        },
        {
          name: "informed consent",
          id: "informedConsent",
          columnType: "ONTOLOGY",
          refTable: "Informed consents",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Is informed consent required for use of the data for research purposes?",
            },
          ],
          position: 114,
        },
        {
          name: "informed consent other",
          id: "informedConsentOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If other, describe the conditions when informed consent is required",
            },
          ],
          position: 115,
        },
        {
          name: "access identifiable data",
          id: "accessIdentifiableData",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Can identifiable data be accessed in the data bank (including patient/practitioner name/practice name)?",
            },
          ],
          position: 116,
        },
        {
          name: "access identifiable data route",
          id: "accessIdentifiableDataRoute",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If yes above, what is the route to access or process this information? What permission is required?",
            },
          ],
          position: 117,
        },
        {
          name: "access subject details",
          id: "accessSubjectDetails",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Can individual patients/practitioners/practices be contacted in the data bank?",
            },
          ],
          position: 118,
        },
        {
          name: "access subject details route",
          id: "accessSubjectDetailsRoute",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If yes above, what is the route to access or process this information? What permission is required?",
            },
          ],
          position: 119,
        },
        {
          name: "audit possible",
          id: "auditPossible",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Are external parties allowed to audit the data? For example, is it possible for an external party to audit the quality or validity of the data source?",
            },
          ],
          position: 120,
        },
        {
          name: "access third party",
          id: "accessThirdParty",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Can (an extract of) the data bank be accessed with permission by a third party?",
            },
          ],
          position: 121,
        },
        {
          name: "access third party conditions",
          id: "accessThirdPartyConditions",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If above is 'yes', describe the conditions under which third-party access may be granted",
            },
          ],
          position: 122,
        },
        {
          name: "access non EU",
          id: "accessNonEU",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Can (an extract of) the data bank be accessed with permission by a non-EU/EEA institution?",
            },
          ],
          position: 123,
        },
        {
          name: "access non EU conditions",
          id: "accessNonEUConditions",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If yes above, describe the conditions under which non-EU/EEA access may be granted",
            },
          ],
          position: 124,
        },
        {
          name: "standard operating procedures",
          id: "standardOperatingProcedures",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Is there a standard operating procedure document that defines the processes and procedures for data capture and management?",
            },
          ],
          position: 125,
        },
        {
          name: "biospecimen access",
          id: "biospecimenAccess",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "If the data bank contains biospecimens (e.g., tissue samples), can these be retrieved?",
            },
          ],
          position: 126,
        },
        {
          name: "biospecimen access conditions",
          id: "biospecimenAccessConditions",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If yes above, describe the conditions under which permission to retrieve biospecimens may be granted",
            },
          ],
          position: 127,
        },
        {
          name: "governance details",
          id: "governanceDetails",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If available, provide a link to documents or webpages that describe the overall governance of the data source bank (governing data access or utilisation for research purposes by existing DAPs)",
            },
          ],
          position: 128,
        },
        {
          name: "approval for publication",
          id: "approvalForPublication",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Is an approval needed to publish the results of a study using the data",
            },
          ],
          position: 129,
        },
        {
          name: "updates",
          id: "updates",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Information on the regularity of updates and time lags",
            },
          ],
          position: 130,
        },
        {
          name: "refresh",
          id: "refresh",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value:
                "Average number of days between refresh of data bank with new records",
            },
          ],
          position: 131,
        },
        {
          name: "lag time",
          id: "lagTime",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value:
                "How many days is the lag time after refresh before a record can be extracted? (e.g., a lag time may occur if the originator conducts quality checks)",
            },
          ],
          position: 132,
        },
        {
          name: "preservation",
          id: "preservation",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value: "Are records preserved in the data bank indefinitely?",
            },
          ],
          position: 133,
        },
        {
          name: "preservation duration",
          id: "preservationDuration",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value:
                "If no to the above, for how long (in years) are records preserved in the data bank?",
            },
          ],
          position: 134,
        },
        {
          name: "refresh period",
          id: "refreshPeriod",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Refresh periods",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "If data are refreshed on fixed dates (e.g., every June and December), when are the refreshes scheduled? Select all that apply from the following:",
            },
          ],
          position: 135,
        },
        {
          name: "date last refresh",
          id: "dateLastRefresh",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value: "Date of last update/refresh",
            },
          ],
          position: 136,
        },
        {
          name: "quality",
          id: "quality",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "List of relevant studies conducted using the data bank",
            },
          ],
          position: 137,
        },
        {
          name: "qualification",
          id: "qualification",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Has the data source successfully undergone a formal qualification process (e.g., from the EMA, or ISO or other certifications)?",
            },
          ],
          position: 138,
        },
        {
          name: "qualifications description",
          id: "qualificationsDescription",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Has the resource successfully undergone a qualification process (e.g., from the EMA)? If yes, describe the qualification(s) granted",
            },
          ],
          position: 139,
        },
        {
          name: "number of records",
          id: "numberOfRecords",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value:
                "Total number of unique records captured in the data bank (most recent count)",
            },
          ],
          position: 140,
        },
        {
          name: "completeness",
          id: "completeness",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Describe the completeness of the data bank (e.g., variables with more or fewer missing values)",
            },
          ],
          position: 141,
        },
        {
          name: "completeness over time",
          id: "completenessOverTime",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Describe any changes in completeness of the data bank (e.g., variables with more or fewer missing values) that have occurred  over time",
            },
          ],
          position: 142,
        },
        {
          name: "completeness results",
          id: "completenessResults",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "What methods or processes are applied to check completeness of the data bank?",
            },
          ],
          position: 143,
        },
        {
          name: "quality description",
          id: "qualityDescription",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Describe the quality of the data bank (e.g., variables with more or fewer missing values)",
            },
          ],
          position: 144,
        },
        {
          name: "quality over time",
          id: "qualityOverTime",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Describe any changes in quality of the data bank that have occurred  over time",
            },
          ],
          position: 145,
        },
        {
          name: "access for validation",
          id: "accessForValidation",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Can validity of the data in the data bank be verified, e.g., by review of origin medical charts?",
            },
          ],
          position: 146,
        },
        {
          name: "quality validation frequency",
          id: "qualityValidationFrequency",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "How often are data quality checks and validation steps conducted on the data bank?",
            },
          ],
          position: 147,
        },
        {
          name: "quality validation methods",
          id: "qualityValidationMethods",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "What methods or processes are applied for data quality checks and validation steps conducted on the data bank?",
            },
          ],
          position: 148,
        },
        {
          name: "correction methods",
          id: "correctionMethods",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "What methods or processes are applied to correct illogical values in the data bank?",
            },
          ],
          position: 149,
        },
        {
          name: "quality validation results",
          id: "qualityValidationResults",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value:
                "If available, provide a link to a publication of the data quality check and validation results",
            },
          ],
          position: 150,
        },
        {
          name: "standards",
          id: "standards",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Use of standard data models and ontologies",
            },
          ],
          position: 151,
        },
        {
          name: "cdms",
          id: "cdms",
          columnType: "REFBACK",
          refTable: "Mappings",
          refLabelDefault: "${source.id}.${target.id}",
          refBack: "source",
          descriptions: [
            {
              locale: "en",
              value: "Common data models used or ETL-ed to by this data source",
            },
          ],
          position: 152,
        },
        {
          name: "cdms other",
          id: "cdmsOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If not in list above, give the name of cdm(s) used by this data source",
            },
          ],
          position: 153,
        },
        {
          name: "ETL standard vocabularies",
          id: "eTLStandardVocabularies",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Vocabularies",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Are data mapped to standardised vocabularies during ETL to the CDM? If yes, what vocabularies are used for events, such as diagnoses?",
            },
          ],
          position: 154,
        },
        {
          name: "ETL standard vocabularies other",
          id: "eTLStandardVocabulariesOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If other, what other vocabularies are used?",
            },
          ],
          position: 155,
        },
        {
          name: "information",
          id: "information",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Other information",
            },
          ],
          position: 156,
        },
        {
          name: "design paper",
          id: "designPaper",
          columnType: "REF_ARRAY",
          refTable: "Publications",
          refLabelDefault: "${doi}",
          descriptions: [
            {
              locale: "en",
              value:
                "Publication(s) that describe(s) the design of this resource",
            },
          ],
          position: 157,
        },
        {
          name: "publications",
          id: "publications",
          columnType: "REF_ARRAY",
          refTable: "Publications",
          refLabelDefault: "${doi}",
          descriptions: [
            {
              locale: "en",
              value: "Other publication(s) about this resource",
            },
          ],
          position: 158,
        },
        {
          name: "informed consent type",
          id: "informedConsentType",
          columnType: "ONTOLOGY",
          refTable: "Informed consent types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "What type of informed consent was given for data collection?",
            },
          ],
          position: 159,
        },
        {
          name: "funding sources",
          id: "fundingSources",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Funding types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Specify the main financial support sources for the data source in the last 3 years. Select all that apply",
            },
          ],
          position: 160,
        },
        {
          name: "funding statement",
          id: "fundingStatement",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Statement listing funding that was obtained for this resource",
            },
          ],
          position: 162,
        },
        {
          name: "acknowledgements",
          id: "acknowledgements",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Acknowledgement statement and citation regulation for this resource",
            },
          ],
          position: 163,
        },
        {
          name: "documentation",
          id: "documentation",
          columnType: "REFBACK",
          refTable: "Documentation",
          refLabelDefault: "${resource.id}.${name}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "Descriptive document(s) available for this resource, e.g. informed consent",
            },
          ],
          position: 164,
        },
        {
          name: "supplementary information",
          id: "supplementaryInformation",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Any other information that needs to be disclosed for this resource",
            },
          ],
          position: 165,
        },
        {
          name: "collaborations",
          id: "collaborations",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "List of relevant collaborations",
            },
          ],
          position: 166,
        },
        {
          name: "networks",
          id: "networks",
          columnType: "REFBACK",
          refTable: "Networks",
          refLabelDefault: "${id}",
          refBack: "data sources",
          descriptions: [
            {
              locale: "en",
              value: "List of networks that this datasource is associated with",
            },
          ],
          position: 171,
        },
        {
          name: "studies",
          id: "studies",
          columnType: "REFBACK",
          refTable: "Studies",
          refLabelDefault: "${id}",
          refBack: "data sources",
          descriptions: [
            {
              locale: "en",
              value: "List of studies that this datasource is associated with",
            },
          ],
          position: 172,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
        {
          name: "mg_tableclass",
          id: "mg_tableclass",
          columnType: "STRING",
          readonly: true,
          position: 10005,
        },
      ],
    },
    {
      name: "Dataset mappings",
      tableType: "DATA",
      id: "DatasetMappings",
      externalSchema: "catalogue",
      columns: [
        {
          name: "source",
          id: "source",
          columnType: "REF",
          key: 1,
          refTable: "Extended resources",
          refLabelDefault: "${id}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "datasource being mapped from",
            },
          ],
          position: 357,
        },
        {
          name: "source dataset",
          id: "sourceDataset",
          columnType: "REF",
          key: 1,
          refTable: "Datasets",
          refLink: "source",
          refLabelDefault: "${name}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "name of the table being mapped from",
            },
          ],
          position: 358,
        },
        {
          name: "target",
          id: "target",
          columnType: "REF",
          key: 1,
          refTable: "Extended resources",
          refLabelDefault: "${id}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value:
                "model being mapped to, i.e. toModel.resource + toModel.version",
            },
          ],
          position: 359,
        },
        {
          name: "target dataset",
          id: "targetDataset",
          columnType: "REF",
          key: 1,
          refTable: "Datasets",
          refLink: "target",
          refLabelDefault: "${name}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "name of the table being mapped to",
            },
          ],
          position: 360,
        },
        {
          name: "order",
          id: "order",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value:
                "Order in which table ETLs should be executed for this source-target combination",
            },
          ],
          position: 361,
        },
        {
          name: "description",
          id: "description",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "human readible description of the mapping",
            },
          ],
          position: 362,
        },
        {
          name: "syntax",
          id: "syntax",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "formal definition of the mapping, ideally executable code",
            },
          ],
          position: 363,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Datasets",
      tableType: "DATA",
      id: "Datasets",
      descriptions: [
        {
          locale: "en",
          value: "Definition of a dataset within a (common) data model",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "resource",
          id: "resource",
          columnType: "REF",
          key: 1,
          refTable: "Extended resources",
          refLabelDefault: "${id}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "resources that these variables are part of",
            },
          ],
          position: 310,
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "unique dataset name in the model",
            },
          ],
          position: 311,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "short human readable description",
            },
          ],
          position: 312,
        },
        {
          name: "unit of observation",
          id: "unitOfObservation",
          columnType: "ONTOLOGY",
          refTable: "Observation targets",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "defines what each record in this table describes",
            },
          ],
          position: 313,
        },
        {
          name: "keywords",
          id: "keywords",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Keywords",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "enables grouping of table list into topic and to display tables in a tree",
            },
          ],
          position: 314,
        },
        {
          name: "description",
          id: "description",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "description of the role/function of this table",
            },
          ],
          position: 315,
        },
        {
          name: "number of rows",
          id: "numberOfRows",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "count of the numbe of records in this table",
            },
          ],
          position: 316,
        },
        {
          name: "mapped to",
          id: "mappedTo",
          columnType: "REFBACK",
          refTable: "Dataset mappings",
          refLabelDefault:
            "${source.id}.${sourceDataset.name}.${target.id}.${targetDataset.name}",
          refBack: "source dataset",
          descriptions: [
            {
              locale: "en",
              value: "common dataset models this dataset has been mapped into",
            },
          ],
          position: 317,
        },
        {
          name: "mapped from",
          id: "mappedFrom",
          columnType: "REFBACK",
          refTable: "Dataset mappings",
          refLabelDefault:
            "${source.id}.${sourceDataset.name}.${target.id}.${targetDataset.name}",
          refBack: "target dataset",
          descriptions: [
            {
              locale: "en",
              value:
                "source datasets that have been mapped to this harmonized dataset",
            },
          ],
          position: 318,
        },
        {
          name: "since version",
          id: "sinceVersion",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "When this dataset was introduced",
            },
          ],
          position: 319,
        },
        {
          name: "until version",
          id: "untilVersion",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "When this dataset was removed if applicable",
            },
          ],
          position: 320,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Datasource types",
      tableType: "ONTOLOGIES",
      id: "DatasourceTypes",
      descriptions: [
        {
          locale: "en",
          value:
            "Which of the following families of databanks best describe this data source",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Datasource types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Datasource types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Diseases",
      tableType: "ONTOLOGIES",
      id: "Diseases",
      descriptions: [
        {
          locale: "en",
          value:
            "Disease groups within this subcohort, based on ICD-10 and ORPHA code classifications",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Diseases",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Diseases",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Document types",
      tableType: "ONTOLOGIES",
      id: "DocumentTypes",
      descriptions: [
        {
          locale: "en",
          value: "Type of documentation",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Document types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Document types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Documentation",
      tableType: "DATA",
      id: "Documentation",
      descriptions: [
        {
          locale: "en",
          value: "Documentation attached to a resource",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "resource",
          id: "resource",
          columnType: "REF",
          key: 1,
          refTable: "Extended resources",
          refLabelDefault: "${id}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "The resource this documentation is for",
            },
          ],
          position: 199,
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Document name",
            },
          ],
          position: 200,
        },
        {
          name: "type",
          id: "type",
          columnType: "ONTOLOGY",
          refTable: "Document types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Type of documentation",
            },
          ],
          position: 201,
        },
        {
          name: "description",
          id: "description",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Description of the document",
            },
          ],
          position: 202,
        },
        {
          name: "url",
          id: "url",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Hyperlink to the source of the documentation",
            },
          ],
          position: 203,
        },
        {
          name: "file",
          id: "file",
          columnType: "FILE",
          descriptions: [
            {
              locale: "en",
              value: "Optional file attachment containing the documentation",
            },
          ],
          position: 204,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Extended resources",
      tableType: "DATA",
      id: "ExtendedResources",
      descriptions: [
        {
          locale: "en",
          value: "Resources that are part of the catalogue",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "overview",
          id: "overview",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "General information",
            },
          ],
        },
        {
          name: "id",
          id: "id",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Internal identifier",
            },
          ],
          position: 1,
        },
        {
          name: "pid",
          id: "pid",
          columnType: "STRING",
          key: 2,
          descriptions: [
            {
              locale: "en",
              value: "Persistent identifier",
            },
          ],
          position: 2,
        },
        {
          name: "acronym",
          id: "acronym",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Acronym if applicable",
            },
          ],
          position: 3,
        },
        {
          name: "name",
          id: "name",
          columnType: "TEXT",
          key: 3,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Name used in European projects",
            },
          ],
          position: 4,
        },
        {
          name: "website",
          id: "website",
          columnType: "HYPERLINK",
          descriptions: [
            {
              locale: "en",
              value: "Link to the website or homepage",
            },
          ],
          position: 26,
        },
        {
          name: "lead organisation",
          id: "leadOrganisation",
          columnType: "REF_ARRAY",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "lead organisation (e.g. research department or group) for this resource",
            },
          ],
          position: 27,
        },
        {
          name: "additional organisations",
          id: "additionalOrganisations",
          columnType: "REF_ARRAY",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "List the names of any additional organisations that contributed to the resource",
            },
          ],
          position: 28,
        },
        {
          name: "description",
          id: "description",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Short description",
            },
          ],
          position: 31,
        },
        {
          name: "external identifiers",
          id: "externalIdentifiers",
          columnType: "REFBACK",
          refTable: "External identifiers",
          refLabelDefault: "${resource.id}.${identifier}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "External identifier(s) for this resource (e.g. EUPASS number, UMCG register number)",
            },
          ],
          position: 37,
        },
        {
          name: "contacts",
          id: "contacts",
          columnType: "REFBACK",
          refTable: "Contacts",
          refLabelDefault: "${resource.id}.${firstName}.${lastName}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value: "Contact person(s)",
            },
          ],
          position: 41,
        },
        {
          name: "logo",
          id: "logo",
          columnType: "FILE",
          descriptions: [
            {
              locale: "en",
              value: "Logo of the resource, for use on homepages etc.",
            },
          ],
          position: 48,
        },
        {
          name: "countries",
          id: "countries",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Countries",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Countries where data from this resource largely originate from",
            },
          ],
          position: 53,
        },
        {
          name: "datasets",
          id: "datasets",
          columnType: "REFBACK",
          refTable: "Datasets",
          refLabelDefault: "${resource.id}.${name}",
          refBack: "resource",
          position: 74,
        },
        {
          name: "publications",
          id: "publications",
          columnType: "REF_ARRAY",
          refTable: "Publications",
          refLabelDefault: "${doi}",
          descriptions: [
            {
              locale: "en",
              value: "Other publication(s) about this resource",
            },
          ],
          position: 158,
        },
        {
          name: "funding statement",
          id: "fundingStatement",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Statement listing funding that was obtained for this resource",
            },
          ],
          position: 162,
        },
        {
          name: "acknowledgements",
          id: "acknowledgements",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Acknowledgement statement and citation regulation for this resource",
            },
          ],
          position: 163,
        },
        {
          name: "documentation",
          id: "documentation",
          columnType: "REFBACK",
          refTable: "Documentation",
          refLabelDefault: "${resource.id}.${name}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "Descriptive document(s) available for this resource, e.g. informed consent",
            },
          ],
          position: 164,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
        {
          name: "mg_tableclass",
          id: "mg_tableclass",
          columnType: "STRING",
          readonly: true,
          position: 10005,
        },
      ],
    },
    {
      name: "External identifier types",
      tableType: "ONTOLOGIES",
      id: "ExternalIdentifierTypes",
      descriptions: [
        {
          locale: "en",
          value: "External identifier type",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "External identifier types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "External identifier types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "External identifiers",
      tableType: "DATA",
      id: "ExternalIdentifiers",
      externalSchema: "catalogue",
      columns: [
        {
          name: "resource",
          id: "resource",
          columnType: "REF",
          key: 1,
          refTable: "Extended resources",
          refLabelDefault: "${id}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Resource that this external identifier belongs to",
            },
          ],
          position: 394,
        },
        {
          name: "identifier",
          id: "identifier",
          columnType: "TEXT",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "External identifier",
            },
          ],
          position: 395,
        },
        {
          name: "external identifier type",
          id: "externalIdentifierType",
          columnType: "ONTOLOGY",
          refTable: "External identifier types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "External identifier type",
            },
          ],
          position: 396,
        },
        {
          name: "external identifier type other",
          id: "externalIdentifierTypeOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If other, enter external identifier type",
            },
          ],
          position: 397,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Formats",
      tableType: "ONTOLOGIES",
      id: "Formats",
      descriptions: [
        {
          locale: "en",
          value: "Data type, e.g. string,int,decimal,date,datetime etc",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Formats",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Formats",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Funding types",
      tableType: "ONTOLOGIES",
      id: "FundingTypes",
      descriptions: [
        {
          locale: "en",
          value:
            "Specify the main financial support sources for the data source in the last 3 years. Select all that apply",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Funding types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Funding types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "ICDO morphologies",
      tableType: "ONTOLOGIES",
      id: "ICDOMorphologies",
      descriptions: [
        {
          locale: "en",
          value:
            "Does the resource collect information on specific cancer subtype(s)? If yes, select morphology specifications.",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "ICDO morphologies",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "ICDO morphologies",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "ICDO topologies",
      tableType: "ONTOLOGIES",
      id: "ICDOTopologies",
      descriptions: [
        {
          locale: "en",
          value:
            "Does the resource collect information on specific cancer subtype(s)? If yes, select topology specifications.",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "ICDO topologies",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "ICDO topologies",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "INN",
      tableType: "ONTOLOGIES",
      id: "INN",
      descriptions: [
        {
          locale: "en",
          value: "INN codes of medicines studied",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "INN",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "INN",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Inclusion criteria",
      tableType: "ONTOLOGIES",
      id: "InclusionCriteria",
      descriptions: [
        {
          locale: "en",
          value:
            "Inclusion criteria applied to the participants of this resource",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Inclusion criteria",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Inclusion criteria",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Informed consent types",
      tableType: "ONTOLOGIES",
      id: "InformedConsentTypes",
      descriptions: [
        {
          locale: "en",
          value: "What type of informed consent was given for data collection?",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Informed consent types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Informed consent types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Informed consents",
      tableType: "ONTOLOGIES",
      id: "InformedConsents",
      descriptions: [
        {
          locale: "en",
          value:
            "Is informed consent required for use of the data for research purposes?",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Informed consents",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Informed consents",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Keywords",
      tableType: "ONTOLOGIES",
      id: "Keywords",
      descriptions: [
        {
          locale: "en",
          value:
            "enables grouping of table list into topic and to display tables in a tree",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Keywords",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Keywords",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Languages",
      tableType: "ONTOLOGIES",
      id: "Languages",
      descriptions: [
        {
          locale: "en",
          value:
            "Languages in which that the records are recorded (in ISO 639, https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes)",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Languages",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Languages",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Linkage strategies",
      tableType: "ONTOLOGIES",
      id: "LinkageStrategies",
      descriptions: [
        {
          locale: "en",
          value:
            "The linkage method that was used to link data banks. One entry per data bank",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Linkage strategies",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Linkage strategies",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Linked resources",
      tableType: "DATA",
      id: "LinkedResources",
      descriptions: [
        {
          locale: "en",
          value: "Links between datasource and databank",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "main resource",
          id: "mainResource",
          columnType: "REF",
          key: 1,
          refTable: "RWE resources",
          refLabelDefault: "${id}",
          required: true,
          position: 205,
        },
        {
          name: "linked resource",
          id: "linkedResource",
          columnType: "REF",
          key: 1,
          refTable: "RWE resources",
          refLabelDefault: "${id}",
          required: true,
          position: 206,
        },
        {
          name: "other linked resource",
          id: "otherLinkedResource",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If other linked data source, enter the name of the data source",
            },
          ],
          position: 207,
        },
        {
          name: "linkage strategy",
          id: "linkageStrategy",
          columnType: "ONTOLOGY",
          refTable: "Linkage strategies",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "The linkage method that was used to link data banks. One entry per data bank",
            },
          ],
          position: 208,
        },
        {
          name: "linkage variable",
          id: "linkageVariable",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If a single variable (or linkage key) is used to link a data bank to others, a name and description of the variable is provided. One entry per data bank",
            },
          ],
          position: 209,
        },
        {
          name: "linkage variable unique",
          id: "linkageVariableUnique",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "If a single variable is used to link a data bank to others, is the variable a unique identifier? One entry per data bank",
            },
          ],
          position: 210,
        },
        {
          name: "linkage completeness",
          id: "linkageCompleteness",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Provide a high-level description of the completeness of linkages that are currently available between data banks in the data source (max 100 words)",
            },
          ],
          position: 211,
        },
        {
          name: "pre linked",
          id: "preLinked",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value: "Does the data source constitute of linked data sources?",
            },
          ],
          position: 212,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Mapping status",
      tableType: "ONTOLOGIES",
      id: "MappingStatus",
      descriptions: [
        {
          locale: "en",
          value:
            "Mapping from collected datasets to standard/harmonized datasets, optionally including ETL syntaxes",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Mapping status",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Mapping status",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Mappings",
      tableType: "DATA",
      id: "Mappings",
      descriptions: [
        {
          locale: "en",
          value:
            "Mapping from collected datasets to standard/harmonized datasets, optionally including ETL syntaxes",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "source",
          id: "source",
          columnType: "REF",
          key: 1,
          refTable: "Extended resources",
          refLabelDefault: "${id}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value:
                "Data source that is being mapped to a (common) data model",
            },
          ],
          position: 351,
        },
        {
          name: "source version",
          id: "sourceVersion",
          columnType: "STRING",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "version of the datasource that is being mapped",
            },
          ],
          position: 352,
        },
        {
          name: "target",
          id: "target",
          columnType: "REF",
          key: 1,
          refTable: "Models",
          refLabelDefault: "${id}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "model being mapped to",
            },
          ],
          position: 353,
        },
        {
          name: "target version",
          id: "targetVersion",
          columnType: "STRING",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "version of the model being mapped to",
            },
          ],
          position: 354,
        },
        {
          name: "mapping status",
          id: "mappingStatus",
          columnType: "ONTOLOGY",
          refTable: "Mapping status",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Mapping from collected datasets to standard/harmonized datasets, optionally including ETL syntaxes",
            },
          ],
          position: 355,
        },
        {
          name: "ETL frequency",
          id: "eTLFrequency",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "The frequency of the ETL process, in months",
            },
          ],
          position: 356,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "MedDRA",
      tableType: "ONTOLOGIES",
      id: "MedDRA",
      descriptions: [
        {
          locale: "en",
          value:
            "If data on a specific disease is collected, which diseases does the data source collect information on",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "MedDRA",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "MedDRA",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Models",
      tableType: "DATA",
      id: "Models",
      descriptions: [
        {
          locale: "en",
          value: "Data models",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "overview",
          id: "overview",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "General information",
            },
          ],
        },
        {
          name: "id",
          id: "id",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Internal identifier",
            },
          ],
          position: 1,
        },
        {
          name: "pid",
          id: "pid",
          columnType: "STRING",
          key: 2,
          descriptions: [
            {
              locale: "en",
              value: "Persistent identifier",
            },
          ],
          position: 2,
        },
        {
          name: "acronym",
          id: "acronym",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Acronym if applicable",
            },
          ],
          position: 3,
        },
        {
          name: "name",
          id: "name",
          columnType: "TEXT",
          key: 3,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Name used in European projects",
            },
          ],
          position: 4,
        },
        {
          name: "website",
          id: "website",
          columnType: "HYPERLINK",
          descriptions: [
            {
              locale: "en",
              value: "Link to the website or homepage",
            },
          ],
          position: 26,
        },
        {
          name: "lead organisation",
          id: "leadOrganisation",
          columnType: "REF_ARRAY",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "lead organisation (e.g. research department or group) for this resource",
            },
          ],
          position: 27,
        },
        {
          name: "additional organisations",
          id: "additionalOrganisations",
          columnType: "REF_ARRAY",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "List the names of any additional organisations that contributed to the resource",
            },
          ],
          position: 28,
        },
        {
          name: "description",
          id: "description",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Short description",
            },
          ],
          position: 31,
        },
        {
          name: "external identifiers",
          id: "externalIdentifiers",
          columnType: "REFBACK",
          refTable: "External identifiers",
          refLabelDefault: "${resource.id}.${identifier}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "External identifier(s) for this resource (e.g. EUPASS number, UMCG register number)",
            },
          ],
          position: 37,
        },
        {
          name: "release frequency",
          id: "releaseFrequency",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "Refreshing rate (in months)",
            },
          ],
          position: 39,
        },
        {
          name: "contacts",
          id: "contacts",
          columnType: "REFBACK",
          refTable: "Contacts",
          refLabelDefault: "${resource.id}.${firstName}.${lastName}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value: "Contact person(s)",
            },
          ],
          position: 41,
        },
        {
          name: "logo",
          id: "logo",
          columnType: "FILE",
          descriptions: [
            {
              locale: "en",
              value: "Logo of the resource, for use on homepages etc.",
            },
          ],
          position: 48,
        },
        {
          name: "countries",
          id: "countries",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Countries",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Countries where data from this resource largely originate from",
            },
          ],
          position: 53,
        },
        {
          name: "datasets",
          id: "datasets",
          columnType: "REFBACK",
          refTable: "Datasets",
          refLabelDefault: "${resource.id}.${name}",
          refBack: "resource",
          position: 74,
        },
        {
          name: "publications",
          id: "publications",
          columnType: "REF_ARRAY",
          refTable: "Publications",
          refLabelDefault: "${doi}",
          descriptions: [
            {
              locale: "en",
              value: "Other publication(s) about this resource",
            },
          ],
          position: 158,
        },
        {
          name: "funding statement",
          id: "fundingStatement",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Statement listing funding that was obtained for this resource",
            },
          ],
          position: 162,
        },
        {
          name: "acknowledgements",
          id: "acknowledgements",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Acknowledgement statement and citation regulation for this resource",
            },
          ],
          position: 163,
        },
        {
          name: "documentation",
          id: "documentation",
          columnType: "REFBACK",
          refTable: "Documentation",
          refLabelDefault: "${resource.id}.${name}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "Descriptive document(s) available for this resource, e.g. informed consent",
            },
          ],
          position: 164,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
        {
          name: "mg_tableclass",
          id: "mg_tableclass",
          columnType: "STRING",
          readonly: true,
          position: 10005,
        },
      ],
    },
    {
      name: "Months",
      tableType: "ONTOLOGIES",
      id: "Months",
      descriptions: [
        {
          locale: "en",
          value: "Start month of data collection",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Months",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Months",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Network features",
      tableType: "ONTOLOGIES",
      id: "NetworkFeatures",
      descriptions: [
        {
          locale: "en",
          value: "Characterizations of the network",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Network features",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Network features",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Network types",
      tableType: "ONTOLOGIES",
      id: "NetworkTypes",
      descriptions: [
        {
          locale: "en",
          value: "Type of network, e.g. h2020 project",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Network types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Network types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Network variables",
      tableType: "DATA",
      id: "NetworkVariables",
      descriptions: [
        {
          locale: "en",
          value: "Listing of the variables used in a network",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "network",
          id: "network",
          columnType: "REF",
          key: 1,
          refTable: "Networks",
          refLabelDefault: "${id}",
          required: true,
          position: 376,
        },
        {
          name: "variable",
          id: "variable",
          columnType: "REF",
          key: 1,
          refTable: "Variables",
          refLabelDefault: "${resource.id}.${dataset.name}.${name}",
          required: true,
          position: 377,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Networks",
      tableType: "DATA",
      id: "Networks",
      descriptions: [
        {
          locale: "en",
          value: "Collaborations of multiple institutions",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "overview",
          id: "overview",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "General information",
            },
          ],
        },
        {
          name: "id",
          id: "id",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Internal identifier",
            },
          ],
          position: 1,
        },
        {
          name: "pid",
          id: "pid",
          columnType: "STRING",
          key: 2,
          descriptions: [
            {
              locale: "en",
              value: "Persistent identifier",
            },
          ],
          position: 2,
        },
        {
          name: "acronym",
          id: "acronym",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Acronym if applicable",
            },
          ],
          position: 3,
        },
        {
          name: "name",
          id: "name",
          columnType: "TEXT",
          key: 3,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Name used in European projects",
            },
          ],
          position: 4,
        },
        {
          name: "type",
          id: "type",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Network types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Type of network, e.g. h2020 project",
            },
          ],
          position: 19,
        },
        {
          name: "features",
          id: "features",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Network features",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Characterizations of the network",
            },
          ],
          position: 20,
        },
        {
          name: "website",
          id: "website",
          columnType: "HYPERLINK",
          descriptions: [
            {
              locale: "en",
              value: "Link to the website or homepage",
            },
          ],
          position: 26,
        },
        {
          name: "lead organisation",
          id: "leadOrganisation",
          columnType: "REF_ARRAY",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "lead organisation (e.g. research department or group) for this resource",
            },
          ],
          position: 27,
        },
        {
          name: "additional organisations",
          id: "additionalOrganisations",
          columnType: "REF_ARRAY",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "List the names of any additional organisations that contributed to the resource",
            },
          ],
          position: 28,
        },
        {
          name: "other organisations",
          id: "otherOrganisations",
          columnType: "TEXT",
          refTable: "Organisations",
          descriptions: [
            {
              locale: "en",
              value:
                "List the names of any other organisations that are not listed and contributed to this resource",
            },
          ],
          position: 30,
        },
        {
          name: "description",
          id: "description",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Short description",
            },
          ],
          position: 31,
        },
        {
          name: "external identifiers",
          id: "externalIdentifiers",
          columnType: "REFBACK",
          refTable: "External identifiers",
          refLabelDefault: "${resource.id}.${identifier}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "External identifier(s) for this resource (e.g. EUPASS number, UMCG register number)",
            },
          ],
          position: 37,
        },
        {
          name: "contacts",
          id: "contacts",
          columnType: "REFBACK",
          refTable: "Contacts",
          refLabelDefault: "${resource.id}.${firstName}.${lastName}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value: "Contact person(s)",
            },
          ],
          position: 41,
        },
        {
          name: "logo",
          id: "logo",
          columnType: "FILE",
          descriptions: [
            {
              locale: "en",
              value: "Logo of the resource, for use on homepages etc.",
            },
          ],
          position: 48,
        },
        {
          name: "countries",
          id: "countries",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Countries",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Countries where data from this resource largely originate from",
            },
          ],
          position: 53,
        },
        {
          name: "start year",
          id: "startYear",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "Year when the network was created",
            },
          ],
          position: 60,
        },
        {
          name: "end year",
          id: "endYear",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "Year when the network ended",
            },
          ],
          position: 61,
        },
        {
          name: "datasets",
          id: "datasets",
          columnType: "REFBACK",
          refTable: "Datasets",
          refLabelDefault: "${resource.id}.${name}",
          refBack: "resource",
          position: 74,
        },
        {
          name: "publications",
          id: "publications",
          columnType: "REF_ARRAY",
          refTable: "Publications",
          refLabelDefault: "${doi}",
          descriptions: [
            {
              locale: "en",
              value: "Other publication(s) about this resource",
            },
          ],
          position: 158,
        },
        {
          name: "funding statement",
          id: "fundingStatement",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Statement listing funding that was obtained for this resource",
            },
          ],
          position: 162,
        },
        {
          name: "acknowledgements",
          id: "acknowledgements",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Acknowledgement statement and citation regulation for this resource",
            },
          ],
          position: 163,
        },
        {
          name: "documentation",
          id: "documentation",
          columnType: "REFBACK",
          refTable: "Documentation",
          refLabelDefault: "${resource.id}.${name}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "Descriptive document(s) available for this resource, e.g. informed consent",
            },
          ],
          position: 164,
        },
        {
          name: "data sources",
          id: "dataSources",
          columnType: "REF_ARRAY",
          refTable: "Data sources",
          refLabelDefault: "${id}",
          position: 253,
        },
        {
          name: "databanks",
          id: "databanks",
          columnType: "REF_ARRAY",
          refTable: "Databanks",
          refLabelDefault: "${id}",
          position: 254,
        },
        {
          name: "cohorts",
          id: "cohorts",
          columnType: "REF_ARRAY",
          refTable: "Cohorts",
          refLabelDefault: "${id}",
          position: 255,
        },
        {
          name: "models",
          id: "models",
          columnType: "REF_ARRAY",
          refTable: "Models",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value: "The common data model(s) used by this network",
            },
          ],
          position: 256,
        },
        {
          name: "studies",
          id: "studies",
          columnType: "REFBACK",
          refTable: "Studies",
          refLabelDefault: "${id}",
          refBack: "networks",
          position: 257,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
        {
          name: "mg_tableclass",
          id: "mg_tableclass",
          columnType: "STRING",
          readonly: true,
          position: 10005,
        },
      ],
    },
    {
      name: "Observation targets",
      tableType: "ONTOLOGIES",
      id: "ObservationTargets",
      descriptions: [
        {
          locale: "en",
          value: "defines what each record in this table describes",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Observation targets",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Observation targets",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Organisation features",
      tableType: "ONTOLOGIES",
      id: "OrganisationFeatures",
      descriptions: [
        {
          locale: "en",
          value: "Features that describe this organisation",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Organisation features",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Organisation features",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Organisation roles",
      tableType: "ONTOLOGIES",
      id: "OrganisationRoles",
      descriptions: [
        {
          locale: "en",
          value:
            "Roles of the institution in connection with data sources in the catalogue. Select one or more of the following:",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Organisation roles",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Organisation roles",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Organisation types",
      tableType: "ONTOLOGIES",
      id: "OrganisationTypes",
      descriptions: [
        {
          locale: "en",
          value:
            "Type of organisation; in which sector is the organisation active?",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Organisation types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Organisation types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Organisations",
      tableType: "DATA",
      id: "Organisations",
      descriptions: [
        {
          locale: "en",
          value: "Research departments and research groups",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "overview",
          id: "overview",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "General information",
            },
          ],
        },
        {
          name: "id",
          id: "id",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Internal identifier",
            },
          ],
          position: 1,
        },
        {
          name: "pid",
          id: "pid",
          columnType: "STRING",
          key: 2,
          descriptions: [
            {
              locale: "en",
              value: "Persistent identifier",
            },
          ],
          position: 2,
        },
        {
          name: "acronym",
          id: "acronym",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Acronym if applicable",
            },
          ],
          position: 3,
        },
        {
          name: "name",
          id: "name",
          columnType: "TEXT",
          key: 3,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Name used in European projects",
            },
          ],
          position: 4,
        },
        {
          name: "type",
          id: "type",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Organisation types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Type of organisation; in which sector is the organisation active?",
            },
          ],
          position: 6,
        },
        {
          name: "type other",
          id: "typeOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If type is 'other', a description of type of organisation",
            },
          ],
          position: 7,
        },
        {
          name: "institution",
          id: "institution",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "University, company, medical centre or research institutes this organisation is part of",
            },
          ],
          position: 8,
        },
        {
          name: "institution acronym",
          id: "institutionAcronym",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Short name of the organisation",
            },
          ],
          position: 9,
        },
        {
          name: "email",
          id: "email",
          columnType: "EMAIL",
          descriptions: [
            {
              locale: "en",
              value:
                "Contact email address for person responsible for organization entry in catalogue",
            },
          ],
          position: 10,
        },
        {
          name: "logo",
          id: "logo",
          columnType: "FILE",
          descriptions: [
            {
              locale: "en",
              value: "Logo of the organisation",
            },
          ],
          position: 11,
        },
        {
          name: "address",
          id: "address",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Address of the organisation",
            },
          ],
          position: 12,
        },
        {
          name: "expertise",
          id: "expertise",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "A short description of the expertise of this institution",
            },
          ],
          position: 13,
        },
        {
          name: "country",
          id: "country",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Countries",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Country in which the institution head office or coordinating centre is located",
            },
          ],
          position: 14,
        },
        {
          name: "features",
          id: "features",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Organisation features",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Features that describe this organisation",
            },
          ],
          position: 15,
        },
        {
          name: "role",
          id: "role",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Organisation roles",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Roles of the institution in connection with data sources in the catalogue. Select one or more of the following:",
            },
          ],
          position: 16,
        },
        {
          name: "leading resources",
          id: "leadingResources",
          columnType: "REFBACK",
          refTable: "Extended resources",
          refLabelDefault: "${id}",
          refBack: "lead organisation",
          descriptions: [
            {
              locale: "en",
              value: "Listing of data sources, cohorts, studies (etc)",
            },
          ],
          position: 17,
        },
        {
          name: "additional resources",
          id: "additionalResources",
          columnType: "REFBACK",
          refTable: "Extended resources",
          refLabelDefault: "${id}",
          refBack: "additional organisations",
          descriptions: [
            {
              locale: "en",
              value: "Listing of data sources, cohorts, studies (etc)",
            },
          ],
          position: 18,
        },
        {
          name: "website",
          id: "website",
          columnType: "HYPERLINK",
          descriptions: [
            {
              locale: "en",
              value: "Link to the website or homepage",
            },
          ],
          position: 26,
        },
        {
          name: "description",
          id: "description",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Short description",
            },
          ],
          position: 31,
        },
        {
          name: "contacts",
          id: "contacts",
          columnType: "REFBACK",
          refTable: "Contacts",
          refLabelDefault: "${resource.id}.${firstName}.${lastName}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value: "Contact person(s)",
            },
          ],
          position: 41,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
        {
          name: "mg_tableclass",
          id: "mg_tableclass",
          columnType: "STRING",
          readonly: true,
          position: 10005,
        },
      ],
    },
    {
      name: "Population entry",
      tableType: "ONTOLOGIES",
      id: "PopulationEntry",
      descriptions: [
        {
          locale: "en",
          value:
            "Select the possible causes / events that trigger the registration of a person in the data source",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Population entry",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Population entry",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Population exit",
      tableType: "ONTOLOGIES",
      id: "PopulationExit",
      descriptions: [
        {
          locale: "en",
          value:
            "Select the possible causes / events that trigger the de-registration of a person in the data source",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Population exit",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Population exit",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Population of interest",
      tableType: "ONTOLOGIES",
      id: "PopulationOfInterest",
      descriptions: [
        {
          locale: "en",
          value:
            "If population of interest is 'Other', please specify which other population has been studied",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Population of interest",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Population of interest",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Publications",
      tableType: "DATA",
      id: "Publications",
      descriptions: [
        {
          locale: "en",
          value: "Publications following bibtex format",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "doi",
          id: "doi",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Digital object identifier",
            },
          ],
          position: 173,
        },
        {
          name: "title",
          id: "title",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Publication title",
            },
          ],
          position: 174,
        },
        {
          name: "authors",
          id: "authors",
          columnType: "STRING_ARRAY",
          descriptions: [
            {
              locale: "en",
              value: "List of authors, one entry per author",
            },
          ],
          position: 175,
        },
        {
          name: "year",
          id: "year",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value:
                "Year of publication (or, if unpublished, year of creation)",
            },
          ],
          position: 176,
        },
        {
          name: "journal",
          id: "journal",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Journal or magazine the work was published in",
            },
          ],
          position: 177,
        },
        {
          name: "volume",
          id: "volume",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "Journal or magazine volume",
            },
          ],
          position: 178,
        },
        {
          name: "number",
          id: "number",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "Journal or maragzine issue number",
            },
          ],
          position: 179,
        },
        {
          name: "pagination",
          id: "pagination",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value:
                "Page numbers, separated either by commas or double-hyphens",
            },
          ],
          position: 180,
        },
        {
          name: "publisher",
          id: "publisher",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Publisher's name",
            },
          ],
          position: 181,
        },
        {
          name: "school",
          id: "school",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "School where the thesis was written (in case of thesis)",
            },
          ],
          position: 182,
        },
        {
          name: "abstract",
          id: "abstract",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Publication abstract",
            },
          ],
          position: 183,
        },
        {
          name: "resources",
          id: "resources",
          columnType: "REFBACK",
          refTable: "Extended resources",
          refLabelDefault: "${id}",
          refBack: "publications",
          descriptions: [
            {
              locale: "en",
              value: "List of resources that refer to this publication",
            },
          ],
          position: 184,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Quantitative information",
      tableType: "DATA",
      id: "QuantitativeInformation",
      descriptions: [
        {
          locale: "en",
          value: "Quantitative information on the resource",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "resource",
          id: "resource",
          columnType: "REF",
          key: 1,
          refTable: "Extended resources",
          refLabelDefault: "${id}",
          required: true,
          position: 213,
        },
        {
          name: "age group",
          id: "ageGroup",
          columnType: "ONTOLOGY",
          key: 1,
          refTable: "Age groups",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value:
                "Select the relevant age group for this quantitative information",
            },
          ],
          position: 214,
        },
        {
          name: "population size",
          id: "populationSize",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value:
                "Total number of unique individuals with records captured in the data source (most recent count). In the catalogue, this will accommodate counts per year",
            },
          ],
          position: 215,
        },
        {
          name: "active size",
          id: "activeSize",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value:
                "Number of unique, active, or currently registered individuals with records captured in the data source (most recent count). In the catalogue, this will accommodate counts per year",
            },
          ],
          position: 216,
        },
        {
          name: "no individuals with samples",
          id: "noIndividualsWithSamples",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value:
                "Number of unique individuals with records of biological samples (e.g., blood, urine) (most recent count). In the catalogue, this will accommodate counts per year",
            },
          ],
          position: 217,
        },
        {
          name: "mean observation years",
          id: "meanObservationYears",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value:
                "Median years for which unique individuals with records captured in the data source are observable (most recent count)",
            },
          ],
          position: 218,
        },
        {
          name: "mean years active",
          id: "meanYearsActive",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value:
                "Median time for which unique individuals with records captured in the data source are observable (most recent count)",
            },
          ],
          position: 219,
        },
        {
          name: "median age",
          id: "medianAge",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "Median age of individuals within data source",
            },
          ],
          position: 220,
        },
        {
          name: "proportion female",
          id: "proportionFemale",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "Proportion of females in the data source",
            },
          ],
          position: 221,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "RWE resources",
      tableType: "DATA",
      id: "RWEResources",
      descriptions: [
        {
          locale: "en",
          value: "Real world data collections",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "overview",
          id: "overview",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "General information",
            },
          ],
        },
        {
          name: "id",
          id: "id",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Internal identifier",
            },
          ],
          position: 1,
        },
        {
          name: "pid",
          id: "pid",
          columnType: "STRING",
          key: 2,
          descriptions: [
            {
              locale: "en",
              value: "Persistent identifier",
            },
          ],
          position: 2,
        },
        {
          name: "acronym",
          id: "acronym",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Acronym if applicable",
            },
          ],
          position: 3,
        },
        {
          name: "name",
          id: "name",
          columnType: "TEXT",
          key: 3,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Name used in European projects",
            },
          ],
          position: 4,
        },
        {
          name: "local name",
          id: "localName",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "If different from above, name in the national language",
            },
          ],
          position: 5,
        },
        {
          name: "type",
          id: "type",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Datasource types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Which of the following families of databanks best describe this data source",
            },
          ],
          position: 23,
        },
        {
          name: "type other",
          id: "typeOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If other, describe the type of datasource",
            },
          ],
          position: 24,
        },
        {
          name: "keywords",
          id: "keywords",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Keywords to increase findability of this resource. Try to use words that are not used in the description",
            },
          ],
          position: 25,
        },
        {
          name: "website",
          id: "website",
          columnType: "HYPERLINK",
          descriptions: [
            {
              locale: "en",
              value: "Link to the website or homepage",
            },
          ],
          position: 26,
        },
        {
          name: "lead organisation",
          id: "leadOrganisation",
          columnType: "REF_ARRAY",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "lead organisation (e.g. research department or group) for this resource",
            },
          ],
          position: 27,
        },
        {
          name: "additional organisations",
          id: "additionalOrganisations",
          columnType: "REF_ARRAY",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "List the names of any additional organisations that contributed to the resource",
            },
          ],
          position: 28,
        },
        {
          name: "description",
          id: "description",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Short description",
            },
          ],
          position: 31,
        },
        {
          name: "data collection description",
          id: "dataCollectionDescription",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Describe the process of collection and recording of data.",
            },
          ],
          position: 32,
        },
        {
          name: "date established",
          id: "dateEstablished",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value:
                "Date when the data source was first established. If the exact day of the month is not known, please enter 15. If the exact month is not known then please enter 15/06",
            },
          ],
          position: 33,
        },
        {
          name: "start data collection",
          id: "startDataCollection",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value:
                "The date when data started to be collected or extracted. If the exact day of the month is not known, please enter 15. If the exact month is not known then please enter 15/06",
            },
          ],
          position: 34,
        },
        {
          name: "end data collection",
          id: "endDataCollection",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value:
                "If data collection in the data source has ceased, on what date did new records last enter the data source?. If the exact day of the month is not known, please enter 15. If the exact month is not known then please enter 15/06",
            },
          ],
          position: 35,
        },
        {
          name: "time span description",
          id: "timeSpanDescription",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Description of time span",
            },
          ],
          position: 36,
        },
        {
          name: "external identifiers",
          id: "externalIdentifiers",
          columnType: "REFBACK",
          refTable: "External identifiers",
          refLabelDefault: "${resource.id}.${identifier}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "External identifier(s) for this resource (e.g. EUPASS number, UMCG register number)",
            },
          ],
          position: 37,
        },
        {
          name: "contacts",
          id: "contacts",
          columnType: "REFBACK",
          refTable: "Contacts",
          refLabelDefault: "${resource.id}.${firstName}.${lastName}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value: "Contact person(s)",
            },
          ],
          position: 41,
        },
        {
          name: "logo",
          id: "logo",
          columnType: "FILE",
          descriptions: [
            {
              locale: "en",
              value: "Logo of the resource, for use on homepages etc.",
            },
          ],
          position: 48,
        },
        {
          name: "population",
          id: "population",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value:
                "Description of the population that can potentially be captured in the resource",
            },
          ],
          position: 49,
        },
        {
          name: "number of participants",
          id: "numberOfParticipants",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "Total number of individuals for which data is collected",
            },
          ],
          position: 50,
        },
        {
          name: "number of participants with samples",
          id: "numberOfParticipantsWithSamples",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "Number of individuals for which samples are collected",
            },
          ],
          position: 51,
        },
        {
          name: "countries",
          id: "countries",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Countries",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Countries where data from this resource largely originate from",
            },
          ],
          position: 53,
        },
        {
          name: "regions",
          id: "regions",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Regions",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Geographical regions where data from this resource largely originate from",
            },
          ],
          position: 54,
        },
        {
          name: "population age groups",
          id: "populationAgeGroups",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Age groups",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Which population age groups are captured in this resource? Select all that are relevant.",
            },
          ],
          position: 55,
        },
        {
          name: "population entry",
          id: "populationEntry",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Population entry",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Select the possible causes / events that trigger the registration of a person in the data source",
            },
          ],
          position: 62,
        },
        {
          name: "population entry other",
          id: "populationEntryOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If other, specify the causes of entry to the underlying population",
            },
          ],
          position: 63,
        },
        {
          name: "population exit",
          id: "populationExit",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Population exit",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Select the possible causes / events that trigger the de-registration of a person in the data source",
            },
          ],
          position: 64,
        },
        {
          name: "population exit other",
          id: "populationExitOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If other, specify the causes of exit from the underlying population",
            },
          ],
          position: 65,
        },
        {
          name: "population disease",
          id: "populationDisease",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Diseases",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Does the resource collect information on a specific disease subpopulation (e.g., as in a disease-specific registry)?",
            },
          ],
          position: 66,
        },
        {
          name: "population oncology topology",
          id: "populationOncologyTopology",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "ICDO topologies",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Does the resource collect information on specific cancer subtype(s)? If yes, select topology specifications.",
            },
          ],
          position: 67,
        },
        {
          name: "population oncology morphology",
          id: "populationOncologyMorphology",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "ICDO morphologies",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Does the resource collect information on specific cancer subtype(s)? If yes, select morphology specifications.",
            },
          ],
          position: 68,
        },
        {
          name: "population coverage",
          id: "populationCoverage",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Estimated percentage of the population covered by the data source in the catchment area. Please describe the denominator.",
            },
          ],
          position: 69,
        },
        {
          name: "population not covered",
          id: "populationNotCovered",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Description of the population covered by the data source in the catchment area whose data are not collected, where applicable (e.g.: people who are registered only for private care)",
            },
          ],
          position: 70,
        },
        {
          name: "quantantitative information",
          id: "quantantitativeInformation",
          columnType: "REFBACK",
          refTable: "Quantitative information",
          refLabelDefault: "${resource.id}.${ageGroup.name}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value: "Numerical summaries describing data bank population",
            },
          ],
          position: 71,
        },
        {
          name: "contents",
          id: "contents",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Data model and contents",
            },
          ],
          position: 73,
        },
        {
          name: "datasets",
          id: "datasets",
          columnType: "REFBACK",
          refTable: "Datasets",
          refLabelDefault: "${resource.id}.${name}",
          refBack: "resource",
          position: 74,
        },
        {
          name: "mappings to data models",
          id: "mappingsToDataModels",
          columnType: "REFBACK",
          refTable: "Dataset mappings",
          refLabelDefault:
            "${source.id}.${sourceDataset.name}.${target.id}.${targetDataset.name}",
          refBack: "source",
          descriptions: [
            {
              locale: "en",
              value: "overview of dataset mappings available",
            },
          ],
          position: 75,
        },
        {
          name: "areas of information",
          id: "areasOfInformation",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Areas of information ds",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Areas of information that were collected",
            },
          ],
          position: 76,
        },
        {
          name: "quality of life other",
          id: "qualityOfLifeOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If other, specify additional quality of life measures",
            },
          ],
          position: 77,
        },
        {
          name: "cause of death code other",
          id: "causeOfDeathCodeOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what cause of death vocabulary is used?",
            },
          ],
          position: 78,
        },
        {
          name: "indication vocabulary other",
          id: "indicationVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what indication for use vocabulary is used?",
            },
          ],
          position: 79,
        },
        {
          name: "genetic data vocabulary other",
          id: "geneticDataVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what genetic data vocabulary is used?",
            },
          ],
          position: 80,
        },
        {
          name: "care setting other",
          id: "careSettingOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' description of the setting of care",
            },
          ],
          position: 81,
        },
        {
          name: "medicinal product vocabulary other",
          id: "medicinalProductVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If 'other,' description of the medicinal product vocabulary",
            },
          ],
          position: 82,
        },
        {
          name: "prescriptions vocabulary other",
          id: "prescriptionsVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what vocabulary is used?",
            },
          ],
          position: 83,
        },
        {
          name: "dispensings vocabulary other",
          id: "dispensingsVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what vocabulary is used?",
            },
          ],
          position: 84,
        },
        {
          name: "procedures vocabulary other",
          id: "proceduresVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what vocabulary is used?",
            },
          ],
          position: 85,
        },
        {
          name: "biomarker data vocabulary other",
          id: "biomarkerDataVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what vocabulary is used?",
            },
          ],
          position: 86,
        },
        {
          name: "diagnosis medical event vocabulary other",
          id: "diagnosisMedicalEventVocabularyOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If 'other,' what vocabulary is used?",
            },
          ],
          position: 87,
        },
        {
          name: "disease details",
          id: "diseaseDetails",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "MedDRA",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "If data on a specific disease is collected, which diseases does the data source collect information on",
            },
          ],
          position: 89,
        },
        {
          name: "disease details other",
          id: "diseaseDetailsOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Specify disease details if not present in MedDRA",
            },
          ],
          position: 90,
        },
        {
          name: "biospecimen collected",
          id: "biospecimenCollected",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Biospecimens",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "If the data bank contains biospecimens, what types of specimen",
            },
          ],
          position: 91,
        },
        {
          name: "languages",
          id: "languages",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Languages",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Languages in which that the records are recorded (in ISO 639, https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes)",
            },
          ],
          position: 92,
        },
        {
          name: "record trigger",
          id: "recordTrigger",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "What triggers the creation of a record in the data bank? e.g., hospital discharge, specialist encounter, dispensation of a medicinal product, recording of a congenital anomaly",
            },
          ],
          position: 93,
        },
        {
          name: "linkage",
          id: "linkage",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Data linkage",
            },
          ],
          position: 97,
        },
        {
          name: "linkage description",
          id: "linkageDescription",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Provide a high-level description of the linkages that are either: currently available between data sources in the data source (when pre-linked = yes); linkages that are possible when using the data source",
            },
          ],
          position: 100,
        },
        {
          name: "linkage possibility",
          id: "linkagePossibility",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value: "Can this data source be linked to other data sources?",
            },
          ],
          position: 101,
        },
        {
          name: "linked resources",
          id: "linkedResources",
          columnType: "REFBACK",
          refTable: "Linked resources",
          refLabelDefault: "${mainResource.id}.${linkedResource.id}",
          refBack: "main resource",
          descriptions: [
            {
              locale: "en",
              value:
                "List of resources that are linked into this main resource",
            },
          ],
          position: 102,
        },
        {
          name: "access",
          id: "access",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Access and validation information",
            },
          ],
          position: 106,
        },
        {
          name: "data holder",
          id: "dataHolder",
          columnType: "REF",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "The name of the organisation that is responsible for governance of the data bank",
            },
          ],
          position: 107,
        },
        {
          name: "DAPs",
          id: "dAPs",
          columnType: "REFBACK",
          refTable: "DAPs",
          refLabelDefault: "${organisation.id}.${resource.id}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "List of DAPs that are listed in the catalogue as having (conditional) permission to access (an extract of) the data resource",
            },
          ],
          position: 108,
        },
        {
          name: "informed consent",
          id: "informedConsent",
          columnType: "ONTOLOGY",
          refTable: "Informed consents",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Is informed consent required for use of the data for research purposes?",
            },
          ],
          position: 114,
        },
        {
          name: "informed consent other",
          id: "informedConsentOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If other, describe the conditions when informed consent is required",
            },
          ],
          position: 115,
        },
        {
          name: "access identifiable data",
          id: "accessIdentifiableData",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Can identifiable data be accessed in the data bank (including patient/practitioner name/practice name)?",
            },
          ],
          position: 116,
        },
        {
          name: "access identifiable data route",
          id: "accessIdentifiableDataRoute",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If yes above, what is the route to access or process this information? What permission is required?",
            },
          ],
          position: 117,
        },
        {
          name: "access subject details",
          id: "accessSubjectDetails",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Can individual patients/practitioners/practices be contacted in the data bank?",
            },
          ],
          position: 118,
        },
        {
          name: "access subject details route",
          id: "accessSubjectDetailsRoute",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If yes above, what is the route to access or process this information? What permission is required?",
            },
          ],
          position: 119,
        },
        {
          name: "audit possible",
          id: "auditPossible",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Are external parties allowed to audit the data? For example, is it possible for an external party to audit the quality or validity of the data source?",
            },
          ],
          position: 120,
        },
        {
          name: "standard operating procedures",
          id: "standardOperatingProcedures",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Is there a standard operating procedure document that defines the processes and procedures for data capture and management?",
            },
          ],
          position: 125,
        },
        {
          name: "biospecimen access",
          id: "biospecimenAccess",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "If the data bank contains biospecimens (e.g., tissue samples), can these be retrieved?",
            },
          ],
          position: 126,
        },
        {
          name: "biospecimen access conditions",
          id: "biospecimenAccessConditions",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If yes above, describe the conditions under which permission to retrieve biospecimens may be granted",
            },
          ],
          position: 127,
        },
        {
          name: "governance details",
          id: "governanceDetails",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If available, provide a link to documents or webpages that describe the overall governance of the data source bank (governing data access or utilisation for research purposes by existing DAPs)",
            },
          ],
          position: 128,
        },
        {
          name: "approval for publication",
          id: "approvalForPublication",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Is an approval needed to publish the results of a study using the data",
            },
          ],
          position: 129,
        },
        {
          name: "updates",
          id: "updates",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Information on the regularity of updates and time lags",
            },
          ],
          position: 130,
        },
        {
          name: "preservation",
          id: "preservation",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value: "Are records preserved in the data bank indefinitely?",
            },
          ],
          position: 133,
        },
        {
          name: "preservation duration",
          id: "preservationDuration",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value:
                "If no to the above, for how long (in years) are records preserved in the data bank?",
            },
          ],
          position: 134,
        },
        {
          name: "refresh period",
          id: "refreshPeriod",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Refresh periods",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "If data are refreshed on fixed dates (e.g., every June and December), when are the refreshes scheduled? Select all that apply from the following:",
            },
          ],
          position: 135,
        },
        {
          name: "date last refresh",
          id: "dateLastRefresh",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value: "Date of last update/refresh",
            },
          ],
          position: 136,
        },
        {
          name: "quality",
          id: "quality",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "List of relevant studies conducted using the data bank",
            },
          ],
          position: 137,
        },
        {
          name: "qualification",
          id: "qualification",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Has the data source successfully undergone a formal qualification process (e.g., from the EMA, or ISO or other certifications)?",
            },
          ],
          position: 138,
        },
        {
          name: "qualifications description",
          id: "qualificationsDescription",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Has the resource successfully undergone a qualification process (e.g., from the EMA)? If yes, describe the qualification(s) granted",
            },
          ],
          position: 139,
        },
        {
          name: "access for validation",
          id: "accessForValidation",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value:
                "Can validity of the data in the data bank be verified, e.g., by review of origin medical charts?",
            },
          ],
          position: 146,
        },
        {
          name: "quality validation frequency",
          id: "qualityValidationFrequency",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "How often are data quality checks and validation steps conducted on the data bank?",
            },
          ],
          position: 147,
        },
        {
          name: "quality validation methods",
          id: "qualityValidationMethods",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "What methods or processes are applied for data quality checks and validation steps conducted on the data bank?",
            },
          ],
          position: 148,
        },
        {
          name: "correction methods",
          id: "correctionMethods",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "What methods or processes are applied to correct illogical values in the data bank?",
            },
          ],
          position: 149,
        },
        {
          name: "quality validation results",
          id: "qualityValidationResults",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value:
                "If available, provide a link to a publication of the data quality check and validation results",
            },
          ],
          position: 150,
        },
        {
          name: "standards",
          id: "standards",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Use of standard data models and ontologies",
            },
          ],
          position: 151,
        },
        {
          name: "cdms",
          id: "cdms",
          columnType: "REFBACK",
          refTable: "Mappings",
          refLabelDefault: "${source.id}.${target.id}",
          refBack: "source",
          descriptions: [
            {
              locale: "en",
              value: "Common data models used or ETL-ed to by this data source",
            },
          ],
          position: 152,
        },
        {
          name: "cdms other",
          id: "cdmsOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If not in list above, give the name of cdm(s) used by this data source",
            },
          ],
          position: 153,
        },
        {
          name: "information",
          id: "information",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Other information",
            },
          ],
          position: 156,
        },
        {
          name: "design paper",
          id: "designPaper",
          columnType: "REF_ARRAY",
          refTable: "Publications",
          refLabelDefault: "${doi}",
          descriptions: [
            {
              locale: "en",
              value:
                "Publication(s) that describe(s) the design of this resource",
            },
          ],
          position: 157,
        },
        {
          name: "publications",
          id: "publications",
          columnType: "REF_ARRAY",
          refTable: "Publications",
          refLabelDefault: "${doi}",
          descriptions: [
            {
              locale: "en",
              value: "Other publication(s) about this resource",
            },
          ],
          position: 158,
        },
        {
          name: "informed consent type",
          id: "informedConsentType",
          columnType: "ONTOLOGY",
          refTable: "Informed consent types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "What type of informed consent was given for data collection?",
            },
          ],
          position: 159,
        },
        {
          name: "funding sources",
          id: "fundingSources",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Funding types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Specify the main financial support sources for the data source in the last 3 years. Select all that apply",
            },
          ],
          position: 160,
        },
        {
          name: "funding statement",
          id: "fundingStatement",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Statement listing funding that was obtained for this resource",
            },
          ],
          position: 162,
        },
        {
          name: "acknowledgements",
          id: "acknowledgements",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Acknowledgement statement and citation regulation for this resource",
            },
          ],
          position: 163,
        },
        {
          name: "documentation",
          id: "documentation",
          columnType: "REFBACK",
          refTable: "Documentation",
          refLabelDefault: "${resource.id}.${name}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "Descriptive document(s) available for this resource, e.g. informed consent",
            },
          ],
          position: 164,
        },
        {
          name: "supplementary information",
          id: "supplementaryInformation",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Any other information that needs to be disclosed for this resource",
            },
          ],
          position: 165,
        },
        {
          name: "collaborations",
          id: "collaborations",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "List of relevant collaborations",
            },
          ],
          position: 166,
        },
        {
          name: "networks",
          id: "networks",
          columnType: "REFBACK",
          refTable: "Networks",
          refLabelDefault: "${id}",
          refBack: "data sources",
          descriptions: [
            {
              locale: "en",
              value: "List of networks that this datasource is associated with",
            },
          ],
          position: 171,
        },
        {
          name: "studies",
          id: "studies",
          columnType: "REFBACK",
          refTable: "Studies",
          refLabelDefault: "${id}",
          refBack: "data sources",
          descriptions: [
            {
              locale: "en",
              value: "List of studies that this datasource is associated with",
            },
          ],
          position: 172,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
        {
          name: "mg_tableclass",
          id: "mg_tableclass",
          columnType: "STRING",
          readonly: true,
          position: 10005,
        },
      ],
    },
    {
      name: "Refresh periods",
      tableType: "ONTOLOGIES",
      id: "RefreshPeriods",
      descriptions: [
        {
          locale: "en",
          value:
            "If data are refreshed on fixed dates (e.g., every June and December), when are the refreshes scheduled? Select all that apply from the following:",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Refresh periods",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Refresh periods",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Regions",
      tableType: "ONTOLOGIES",
      id: "Regions",
      descriptions: [
        {
          locale: "en",
          value:
            "Geographical regions where data from this subcohort largely originate from",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Regions",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Regions",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Release types",
      tableType: "ONTOLOGIES",
      id: "ReleaseTypes",
      descriptions: [
        {
          locale: "en",
          value:
            "Select whether this resource is a closed dataset or whether new data is released continuously or at a termly basis",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Release types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Release types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Repeated variables",
      tableType: "DATA",
      id: "RepeatedVariables",
      descriptions: [
        {
          locale: "en",
          value:
            "Definition of a repeated sourceVariable. Refers to another variable for its definition",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "resource",
          id: "resource",
          columnType: "REF",
          key: 1,
          refTable: "Extended resources",
          refLabelDefault: "${id}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Data source that this variable was collected in",
            },
          ],
          position: 321,
        },
        {
          name: "dataset",
          id: "dataset",
          columnType: "REF",
          key: 1,
          refTable: "Datasets",
          refLink: "resource",
          refLabelDefault: "${name}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Dataset this variable is part of",
            },
          ],
          position: 322,
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "name of the variable, unique within a table",
            },
          ],
          position: 323,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Human friendly longer name, if applicable",
            },
          ],
          position: 324,
        },
        {
          name: "collection event",
          id: "collectionEvent",
          columnType: "REF",
          refTable: "Collection events",
          refLabelDefault: "${resource.id}.${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "in case of protocolised data collection this defines the moment in time this variable is collected on",
            },
          ],
          position: 325,
        },
        {
          name: "since version",
          id: "sinceVersion",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "When this variable was introduced",
            },
          ],
          position: 326,
        },
        {
          name: "until version",
          id: "untilVersion",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "When this variable was removed if applicable",
            },
          ],
          position: 327,
        },
        {
          name: "mappings",
          id: "mappings",
          columnType: "REFBACK",
          refTable: "Variable mappings",
          refLabelDefault:
            "${source.id}.${sourceDataset.name}.${target.id}.${targetDataset.name}.${targetVariable.name}",
          refBack: "target variable",
          descriptions: [
            {
              locale: "en",
              value:
                "in case of protocolised data collection this defines the moment in time this variable is collected on",
            },
          ],
          position: 340,
        },
        {
          name: "is repeat of",
          id: "isRepeatOf",
          columnType: "REF",
          refTable: "Variables",
          refLink: "resource",
          refLabelDefault: "${dataset.name}.${name}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value:
                "reference to the definition of the sourceVariable that is being repeated",
            },
          ],
          position: 350,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
        {
          name: "mg_tableclass",
          id: "mg_tableclass",
          columnType: "STRING",
          readonly: true,
          position: 10005,
        },
      ],
    },
    {
      name: "Resource types",
      tableType: "ONTOLOGIES",
      id: "ResourceTypes",
      descriptions: [
        {
          locale: "en",
          value: "Type of resource, e.g. registry, cohort, biobank",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Resource types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Resource types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Resources",
      tableType: "DATA",
      id: "Resources",
      descriptions: [
        {
          locale: "en",
          value:
            "Generic listing of all resources. Should not be used directly, instead use specific types such as Databanks and Studies",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "overview",
          id: "overview",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "General information",
            },
          ],
        },
        {
          name: "id",
          id: "id",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Internal identifier",
            },
          ],
          position: 1,
        },
        {
          name: "pid",
          id: "pid",
          columnType: "STRING",
          key: 2,
          descriptions: [
            {
              locale: "en",
              value: "Persistent identifier",
            },
          ],
          position: 2,
        },
        {
          name: "acronym",
          id: "acronym",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Acronym if applicable",
            },
          ],
          position: 3,
        },
        {
          name: "name",
          id: "name",
          columnType: "TEXT",
          key: 3,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Name used in European projects",
            },
          ],
          position: 4,
        },
        {
          name: "website",
          id: "website",
          columnType: "HYPERLINK",
          descriptions: [
            {
              locale: "en",
              value: "Link to the website or homepage",
            },
          ],
          position: 26,
        },
        {
          name: "description",
          id: "description",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Short description",
            },
          ],
          position: 31,
        },
        {
          name: "contacts",
          id: "contacts",
          columnType: "REFBACK",
          refTable: "Contacts",
          refLabelDefault: "${resource.id}.${firstName}.${lastName}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value: "Contact person(s)",
            },
          ],
          position: 41,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
        {
          name: "mg_tableclass",
          id: "mg_tableclass",
          columnType: "STRING",
          readonly: true,
          position: 10005,
        },
      ],
    },
    {
      name: "Sample categories",
      tableType: "ONTOLOGIES",
      id: "SampleCategories",
      descriptions: [
        {
          locale: "en",
          value: "Samples that were collected in this collection event",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Sample categories",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Sample categories",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Standardized tools",
      tableType: "ONTOLOGIES",
      id: "StandardizedTools",
      descriptions: [
        {
          locale: "en",
          value:
            "Standardized tools, e.g. surveys, questionnaires, instruments used to collect data for this collection event",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Standardized tools",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Standardized tools",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Status",
      tableType: "ONTOLOGIES",
      id: "Status",
      descriptions: [
        {
          locale: "en",
          value: "whether harmonisation is still draft or final",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Status",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Status",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Status details",
      tableType: "ONTOLOGIES",
      id: "StatusDetails",
      descriptions: [
        {
          locale: "en",
          value: "e.g. 'complete, partial, planned, no-match'",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Status details",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Status details",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Studies",
      tableType: "DATA",
      id: "Studies",
      descriptions: [
        {
          locale: "en",
          value:
            "Collaborations of multiple institutions, addressing research questions using data sources and/or data banks",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "overview",
          id: "overview",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "General information",
            },
          ],
        },
        {
          name: "id",
          id: "id",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Internal identifier",
            },
          ],
          position: 1,
        },
        {
          name: "pid",
          id: "pid",
          columnType: "STRING",
          key: 2,
          descriptions: [
            {
              locale: "en",
              value: "Persistent identifier",
            },
          ],
          position: 2,
        },
        {
          name: "acronym",
          id: "acronym",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Acronym if applicable",
            },
          ],
          position: 3,
        },
        {
          name: "name",
          id: "name",
          columnType: "TEXT",
          key: 3,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Name used in European projects",
            },
          ],
          position: 4,
        },
        {
          name: "type",
          id: "type",
          columnType: "ONTOLOGY",
          refTable: "Study types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Select 1 of the following types of study",
            },
          ],
          position: 21,
        },
        {
          name: "type other",
          id: "typeOther",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "If other, describe the type of study",
            },
          ],
          position: 22,
        },
        {
          name: "website",
          id: "website",
          columnType: "HYPERLINK",
          descriptions: [
            {
              locale: "en",
              value: "Link to the website or homepage",
            },
          ],
          position: 26,
        },
        {
          name: "lead organisation",
          id: "leadOrganisation",
          columnType: "REF_ARRAY",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "lead organisation (e.g. research department or group) for this resource",
            },
          ],
          position: 27,
        },
        {
          name: "additional organisations",
          id: "additionalOrganisations",
          columnType: "REF_ARRAY",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value:
                "List the names of any additional organisations that contributed to the resource",
            },
          ],
          position: 28,
        },
        {
          name: "other organisations",
          id: "otherOrganisations",
          columnType: "TEXT",
          refTable: "Organisations",
          descriptions: [
            {
              locale: "en",
              value:
                "List the names of any other organisations that are not listed and contributed to this resource",
            },
          ],
          position: 29,
        },
        {
          name: "description",
          id: "description",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Short description",
            },
          ],
          position: 31,
        },
        {
          name: "external identifiers",
          id: "externalIdentifiers",
          columnType: "REFBACK",
          refTable: "External identifiers",
          refLabelDefault: "${resource.id}.${identifier}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "External identifier(s) for this resource (e.g. EUPASS number, UMCG register number)",
            },
          ],
          position: 37,
        },
        {
          name: "status",
          id: "status",
          columnType: "ONTOLOGY",
          refTable: "Study status",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Status of the study",
            },
          ],
          position: 38,
        },
        {
          name: "contacts",
          id: "contacts",
          columnType: "REFBACK",
          refTable: "Contacts",
          refLabelDefault: "${resource.id}.${firstName}.${lastName}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value: "Contact person(s)",
            },
          ],
          position: 41,
        },
        {
          name: "logo",
          id: "logo",
          columnType: "FILE",
          descriptions: [
            {
              locale: "en",
              value: "Logo of the resource, for use on homepages etc.",
            },
          ],
          position: 48,
        },
        {
          name: "countries",
          id: "countries",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Countries",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Countries where data from this resource largely originate from",
            },
          ],
          position: 53,
        },
        {
          name: "datasets",
          id: "datasets",
          columnType: "REFBACK",
          refTable: "Datasets",
          refLabelDefault: "${resource.id}.${name}",
          refBack: "resource",
          position: 74,
        },
        {
          name: "publications",
          id: "publications",
          columnType: "REF_ARRAY",
          refTable: "Publications",
          refLabelDefault: "${doi}",
          descriptions: [
            {
              locale: "en",
              value: "Other publication(s) about this resource",
            },
          ],
          position: 158,
        },
        {
          name: "funding scheme",
          id: "fundingScheme",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Study funding",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "The source of funding for the study. Select all that apply",
            },
          ],
          position: 161,
        },
        {
          name: "funding statement",
          id: "fundingStatement",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Statement listing funding that was obtained for this resource",
            },
          ],
          position: 162,
        },
        {
          name: "acknowledgements",
          id: "acknowledgements",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Acknowledgement statement and citation regulation for this resource",
            },
          ],
          position: 163,
        },
        {
          name: "documentation",
          id: "documentation",
          columnType: "REFBACK",
          refTable: "Documentation",
          refLabelDefault: "${resource.id}.${name}",
          refBack: "resource",
          descriptions: [
            {
              locale: "en",
              value:
                "Descriptive document(s) available for this resource, e.g. informed consent",
            },
          ],
          position: 164,
        },
        {
          name: "networks",
          id: "networks",
          columnType: "REF_ARRAY",
          refTable: "Networks",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value: "The consortia or networks that this study is part of",
            },
          ],
          position: 169,
        },
        {
          name: "networks other",
          id: "networksOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "List the names of any other networks that are not listed and this resource is involved in",
            },
          ],
          position: 170,
        },
        {
          name: "study requirements",
          id: "studyRequirements",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Study requirements",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Study requirements",
            },
          ],
          position: 258,
        },
        {
          name: "regulatory procedure number",
          id: "regulatoryProcedureNumber",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value:
                "Regulatory procedure number, for RMP Category 1 and 2 studies only",
            },
          ],
          position: 259,
        },
        {
          name: "date of signing funding contract planned",
          id: "dateOfSigningFundingContractPlanned",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value: "Date when funding contract was signed, planned",
            },
          ],
          position: 260,
        },
        {
          name: "date of signing funding contract actual",
          id: "dateOfSigningFundingContractActual",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value: "Date when funding contract was signed, actual",
            },
          ],
          position: 261,
        },
        {
          name: "collection start planned",
          id: "collectionStartPlanned",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value: "When was the data collection planned to start?",
            },
          ],
          position: 262,
        },
        {
          name: "collection start actual",
          id: "collectionStartActual",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value: "When was the data collection actually start?",
            },
          ],
          position: 263,
        },
        {
          name: "analysis start planned",
          id: "analysisStartPlanned",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value: "When was the data analysis planned to start?",
            },
          ],
          position: 264,
        },
        {
          name: "analysis start actual",
          id: "analysisStartActual",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value: "When did the data analysis actually start?",
            },
          ],
          position: 265,
        },
        {
          name: "interim report planned",
          id: "interimReportPlanned",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value: "Planned date of interim report, if expected",
            },
          ],
          position: 266,
        },
        {
          name: "interim report actual",
          id: "interimReportActual",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value: "Actual date of interim report, if expected",
            },
          ],
          position: 267,
        },
        {
          name: "final report planned",
          id: "finalReportPlanned",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value: "Date of final study report, planned",
            },
          ],
          position: 268,
        },
        {
          name: "final report actual",
          id: "finalReportActual",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value: "Date of final study report, actual",
            },
          ],
          position: 269,
        },
        {
          name: "data",
          id: "data",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Data management",
            },
          ],
          position: 270,
        },
        {
          name: "data sources",
          id: "dataSources",
          columnType: "REF_ARRAY",
          refTable: "Data sources",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value: "Data sources that provided data into this study",
            },
          ],
          position: 271,
        },
        {
          name: "data sources other",
          id: "dataSourcesOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Other not listed data sources that provided data into this study",
            },
          ],
          position: 272,
        },
        {
          name: "databanks",
          id: "databanks",
          columnType: "REF_ARRAY",
          refTable: "Databanks",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value: "Databanks that provided data into this study",
            },
          ],
          position: 273,
        },
        {
          name: "databanks other",
          id: "databanksOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Other not listed databanks that provided data into this study",
            },
          ],
          position: 274,
        },
        {
          name: "cohorts",
          id: "cohorts",
          columnType: "REF_ARRAY",
          refTable: "Cohorts",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value: "Cohorts that provided data into this study",
            },
          ],
          position: 275,
        },
        {
          name: "cdms",
          id: "cdms",
          columnType: "REFBACK",
          refTable: "Mappings",
          refLabelDefault: "${source.id}.${target.id}",
          refBack: "source",
          descriptions: [
            {
              locale: "en",
              value: "Common data model(s) used in this study",
            },
          ],
          position: 276,
        },
        {
          name: "study features",
          id: "studyFeatures",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Study features",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Data features",
            },
          ],
          position: 277,
        },
        {
          name: "data characterisation details",
          id: "dataCharacterisationDetails",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Provide a summary description of the data characterisation or quality check process",
            },
          ],
          position: 278,
        },
        {
          name: "data source types",
          id: "dataSourceTypes",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Study datasource types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Types of data sources used",
            },
          ],
          position: 279,
        },
        {
          name: "data source types other",
          id: "dataSourceTypesOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Sources of data, if other",
            },
          ],
          position: 280,
        },
        {
          name: "quality marks",
          id: "qualityMarks",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Study quality marks",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Quality marks, such as ENCePP seal",
            },
          ],
          position: 281,
        },
        {
          name: "number of data sources",
          id: "numberOfDataSources",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Total number of data sources included in the study",
            },
          ],
          position: 282,
        },
        {
          name: "medicines studied INN codes",
          id: "medicinesStudiedINNCodes",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "INN",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "INN codes of medicines studied",
            },
          ],
          position: 283,
        },
        {
          name: "medicines studied ATC codes",
          id: "medicinesStudiedATCCodes",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "ATC",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "ATC codes of medicines studied",
            },
          ],
          position: 284,
        },
        {
          name: "medicines studied other",
          id: "medicinesStudiedOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If the medicinal product information (e.g. brand name or active substance or ATC code) does not appear in the available look-ups please enter it here",
            },
          ],
          position: 285,
        },
        {
          name: "medical conditions studied",
          id: "medicalConditionsStudied",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "MedDRA",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Codes of the medical conditions studied",
            },
          ],
          position: 286,
        },
        {
          name: "medical conditions studied other",
          id: "medicalConditionsStudiedOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Codes of the medical conditions studied",
            },
          ],
          position: 287,
        },
        {
          name: "data extraction date",
          id: "dataExtractionDate",
          columnType: "DATE",
          descriptions: [
            {
              locale: "en",
              value: "Date on which the study data was extracted",
            },
          ],
          position: 288,
        },
        {
          name: "methods",
          id: "methods",
          columnType: "HEADING",
          descriptions: [
            {
              locale: "en",
              value: "Methodological aspects",
            },
          ],
          position: 289,
        },
        {
          name: "study setting",
          id: "studySetting",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "A short description of the study setting",
            },
          ],
          position: 290,
        },
        {
          name: "analysis plan",
          id: "analysisPlan",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "A brief summary of the analysis method (e.g. risk estimation, measures of risk, internal/external validity)",
            },
          ],
          position: 291,
        },
        {
          name: "population description",
          id: "populationDescription",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "A short description of the study population",
            },
          ],
          position: 292,
        },
        {
          name: "number of subjects",
          id: "numberOfSubjects",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "Estimated number of subjects",
            },
          ],
          position: 293,
        },
        {
          name: "age groups",
          id: "ageGroups",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Age groups",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Which population age groups are studied",
            },
          ],
          position: 294,
        },
        {
          name: "objectives",
          id: "objectives",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "A short description of the study objective",
            },
          ],
          position: 295,
        },
        {
          name: "interventions",
          id: "interventions",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "A short description of the study interventions",
            },
          ],
          position: 296,
        },
        {
          name: "comparators",
          id: "comparators",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "A short description of the study comparators",
            },
          ],
          position: 297,
        },
        {
          name: "outcomes",
          id: "outcomes",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "A short description of the study outcomes",
            },
          ],
          position: 298,
        },
        {
          name: "study design",
          id: "studyDesign",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "A brief summary of the study design",
            },
          ],
          position: 299,
        },
        {
          name: "results",
          id: "results",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "A brief summary of the results of the study on study completion (from abstract)",
            },
          ],
          position: 300,
        },
        {
          name: "topic",
          id: "topic",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Study topics",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "An initial classification of the study purpose",
            },
          ],
          position: 301,
        },
        {
          name: "topic other",
          id: "topicOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If the study is not concerning any of the proposed categories, please specify the details",
            },
          ],
          position: 302,
        },
        {
          name: "trial regulatory scope",
          id: "trialRegulatoryScope",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Study trial regulatory scopes",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Classification of the clinical trial in relation to the medicines authorisation",
            },
          ],
          position: 303,
        },
        {
          name: "study design classification",
          id: "studyDesignClassification",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Study design classification",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Study design classifications",
            },
          ],
          position: 304,
        },
        {
          name: "study design classification other",
          id: "studyDesignClassificationOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Further details on design",
            },
          ],
          position: 305,
        },
        {
          name: "study scope",
          id: "studyScope",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Study scopes",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Scope of the study",
            },
          ],
          position: 306,
        },
        {
          name: "study scope other",
          id: "studyScopeOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "If scope 'other'",
            },
          ],
          position: 307,
        },
        {
          name: "population of interest",
          id: "populationOfInterest",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Population of interest",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "If population of interest is 'Other', please specify which other population has been studied",
            },
          ],
          position: 308,
        },
        {
          name: "population of interest other",
          id: "populationOfInterestOther",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "If population of interest is 'Other', please specify which other population has been studied",
            },
          ],
          position: 309,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
        {
          name: "mg_tableclass",
          id: "mg_tableclass",
          columnType: "STRING",
          readonly: true,
          position: 10005,
        },
      ],
    },
    {
      name: "Study datasource types",
      tableType: "ONTOLOGIES",
      id: "StudyDatasourceTypes",
      descriptions: [
        {
          locale: "en",
          value: "Types of data sources used",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study datasource types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study datasource types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Study design classification",
      tableType: "ONTOLOGIES",
      id: "StudyDesignClassification",
      descriptions: [
        {
          locale: "en",
          value: "Study design classifications",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study design classification",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study design classification",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Study features",
      tableType: "ONTOLOGIES",
      id: "StudyFeatures",
      descriptions: [
        {
          locale: "en",
          value: "Data features",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study features",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study features",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Study funding",
      tableType: "ONTOLOGIES",
      id: "StudyFunding",
      descriptions: [
        {
          locale: "en",
          value: "The source of funding for the study. Select all that apply",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study funding",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study funding",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Study quality marks",
      tableType: "ONTOLOGIES",
      id: "StudyQualityMarks",
      descriptions: [
        {
          locale: "en",
          value: "Quality marks, such as ENCePP seal",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study quality marks",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study quality marks",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Study requirements",
      tableType: "ONTOLOGIES",
      id: "StudyRequirements",
      descriptions: [
        {
          locale: "en",
          value: "Study requirements",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study requirements",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study requirements",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Study scopes",
      tableType: "ONTOLOGIES",
      id: "StudyScopes",
      descriptions: [
        {
          locale: "en",
          value: "Scope of the study",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study scopes",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study scopes",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Study status",
      tableType: "ONTOLOGIES",
      id: "StudyStatus",
      descriptions: [
        {
          locale: "en",
          value: "Status of the study",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study status",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study status",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Study topics",
      tableType: "ONTOLOGIES",
      id: "StudyTopics",
      descriptions: [
        {
          locale: "en",
          value: "An initial classification of the study purpose",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study topics",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study topics",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Study trial regulatory scopes",
      tableType: "ONTOLOGIES",
      id: "StudyTrialRegulatoryScopes",
      descriptions: [
        {
          locale: "en",
          value:
            "Classification of the clinical trial in relation to the medicines authorisation",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study trial regulatory scopes",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study trial regulatory scopes",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Study types",
      tableType: "ONTOLOGIES",
      id: "StudyTypes",
      descriptions: [
        {
          locale: "en",
          value: "Select 1 of the following types of study",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Subcohort counts",
      tableType: "DATA",
      id: "SubcohortCounts",
      externalSchema: "catalogue",
      columns: [
        {
          name: "subcohort",
          id: "subcohort",
          columnType: "REF",
          key: 1,
          refTable: "Subcohorts",
          refLabelDefault: "${resource.id}.${name}",
          required: true,
          position: 378,
        },
        {
          name: "age group",
          id: "ageGroup",
          columnType: "ONTOLOGY",
          key: 1,
          refTable: "Age groups",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          required: true,
          position: 379,
        },
        {
          name: "N total",
          id: "nTotal",
          columnType: "INT",
          position: 380,
        },
        {
          name: "N female",
          id: "nFemale",
          columnType: "INT",
          position: 381,
        },
        {
          name: "N male",
          id: "nMale",
          columnType: "INT",
          position: 382,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Subcohorts",
      tableType: "DATA",
      id: "Subcohorts",
      descriptions: [
        {
          locale: "en",
          value: "Subcohorts defined for this resource",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "resource",
          id: "resource",
          columnType: "REF",
          key: 1,
          refTable: "Extended resources",
          refLabelDefault: "${id}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Resource this subcohort is part of",
            },
          ],
          position: 239,
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value:
                "Subcohort name, e.g. 'mothers in first trimester','newborns'",
            },
          ],
          position: 240,
        },
        {
          name: "description",
          id: "description",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Subcohort description",
            },
          ],
          position: 241,
        },
        {
          name: "number of participants",
          id: "numberOfParticipants",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "Number of participants in this subcohort",
            },
          ],
          position: 242,
        },
        {
          name: "counts",
          id: "counts",
          columnType: "REFBACK",
          refTable: "Subcohort counts",
          refLabelDefault:
            "${subcohort.resource.id}.${subcohort.name}.${ageGroup.name}",
          refBack: "subcohort",
          descriptions: [
            {
              locale: "en",
              value:
                "Total number of unique individuals per age(group), gender and year",
            },
          ],
          position: 243,
        },
        {
          name: "inclusion start",
          id: "inclusionStart",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "Year of first included participant",
            },
          ],
          position: 244,
        },
        {
          name: "inclusion end",
          id: "inclusionEnd",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value:
                "Year of last included participant. Leave empty if collection is ongoing",
            },
          ],
          position: 245,
        },
        {
          name: "age groups",
          id: "ageGroups",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Age groups",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Age groups within this subcohort",
            },
          ],
          position: 246,
        },
        {
          name: "main medical condition",
          id: "mainMedicalCondition",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Diseases",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Disease groups within this subcohort, based on ICD-10 and ORPHA code classifications",
            },
          ],
          position: 247,
        },
        {
          name: "comorbidity",
          id: "comorbidity",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Diseases",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Comorbidity within this subcohort, based on ICD-10 classification",
            },
          ],
          position: 248,
        },
        {
          name: "countries",
          id: "countries",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Countries",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Countries where data from this subcohort largely originate from",
            },
          ],
          position: 249,
        },
        {
          name: "regions",
          id: "regions",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Regions",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Geographical regions where data from this subcohort largely originate from",
            },
          ],
          position: 250,
        },
        {
          name: "inclusion criteria",
          id: "inclusionCriteria",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Inclusion criteria applied to this subcohort",
            },
          ],
          position: 251,
        },
        {
          name: "supplementary information",
          id: "supplementaryInformation",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "Any other information that needs to be disclosed for this subcohort",
            },
          ],
          position: 252,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Submission types",
      tableType: "ONTOLOGIES",
      id: "SubmissionTypes",
      descriptions: [
        {
          locale: "en",
          value: "Type of submission, e.g. 'new entry', 'correction'",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Submission types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Submission types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Submissions",
      tableType: "DATA",
      id: "Submissions",
      descriptions: [
        {
          locale: "en",
          value: "Documentation of data submissions related to a resource",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "submission date",
          id: "submissionDate",
          columnType: "DATETIME",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Date of submission",
            },
          ],
          position: 383,
        },
        {
          name: "submitter name",
          id: "submitterName",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Name of person who submitted",
            },
          ],
          position: 384,
        },
        {
          name: "resources",
          id: "resources",
          columnType: "REF_ARRAY",
          refTable: "Extended resources",
          refLabelDefault: "${id}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Resource(s) that this submission is related to",
            },
          ],
          position: 385,
        },
        {
          name: "submitter email",
          id: "submitterEmail",
          columnType: "STRING",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Email of person who submitted",
            },
          ],
          position: 386,
        },
        {
          name: "submitter organisation",
          id: "submitterOrganisation",
          columnType: "REF",
          refTable: "Organisations",
          refLabelDefault: "${id}",
          descriptions: [
            {
              locale: "en",
              value: "Organisation of submitter",
            },
          ],
          position: 387,
        },
        {
          name: "submitter role",
          id: "submitterRole",
          columnType: "ONTOLOGY",
          refTable: "Submitter roles",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Role of submitter in organisation",
            },
          ],
          position: 388,
        },
        {
          name: "submitter role other",
          id: "submitterRoleOther",
          columnType: "TEXT",
          refTable: "Submitter roles",
          descriptions: [
            {
              locale: "en",
              value: "Role of submitter in organisation, if other than above",
            },
          ],
          position: 389,
        },
        {
          name: "submission type",
          id: "submissionType",
          columnType: "ONTOLOGY",
          refTable: "Submission types",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Type of submission, e.g. 'new entry', 'correction'",
            },
          ],
          position: 390,
        },
        {
          name: "submission description",
          id: "submissionDescription",
          columnType: "TEXT",
          refTable: "Submission types",
          descriptions: [
            {
              locale: "en",
              value: "Description of the submission, if applicable",
            },
          ],
          position: 391,
        },
        {
          name: "responsible persons",
          id: "responsiblePersons",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "List of responsible persons related to this submission",
            },
          ],
          position: 392,
        },
        {
          name: "acceptance date",
          id: "acceptanceDate",
          columnType: "DATETIME",
          descriptions: [
            {
              locale: "en",
              value: "Date submission was accepted",
            },
          ],
          position: 393,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Submitter roles",
      tableType: "ONTOLOGIES",
      id: "SubmitterRoles",
      descriptions: [
        {
          locale: "en",
          value: "Role of submitter in organisation",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Submitter roles",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Submitter roles",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Titles",
      tableType: "ONTOLOGIES",
      id: "Titles",
      descriptions: [
        {
          locale: "en",
          value: "Title of the contact person",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Titles",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Titles",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Units",
      tableType: "ONTOLOGIES",
      id: "Units",
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Units",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Units",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Variable mappings",
      tableType: "DATA",
      id: "VariableMappings",
      descriptions: [
        {
          locale: "en",
          value:
            "Mappings from collected variables to standard/harmonized variables, optionally including ETL syntax",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "source",
          id: "source",
          columnType: "REF",
          key: 1,
          refTable: "Extended resources",
          refLabelDefault: "${id}",
          required: true,
          position: 364,
        },
        {
          name: "source dataset",
          id: "sourceDataset",
          columnType: "REF",
          key: 1,
          refTable: "Datasets",
          refLink: "source",
          refLabelDefault: "${name}",
          required: true,
          position: 365,
        },
        {
          name: "source variables",
          id: "sourceVariables",
          columnType: "REF_ARRAY",
          refTable: "All variables",
          refLink: "source dataset",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "Optional, source variable that was mapped from. You may also indicate that a mapping to a target variable was not done and leave this field empty (match = na)",
            },
          ],
          position: 366,
        },
        {
          name: "source variables other datasets",
          id: "sourceVariablesOtherDatasets",
          columnType: "REF_ARRAY",
          refTable: "All variables",
          refLink: "source",
          refLabelDefault: "${dataset.name}.${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "optional, variable from other source datasets. Initially one may only define mapping between releases",
            },
          ],
          position: 367,
        },
        {
          name: "target",
          id: "target",
          columnType: "REF",
          key: 1,
          refTable: "Extended resources",
          refLabelDefault: "${id}",
          required: true,
          position: 368,
        },
        {
          name: "target dataset",
          id: "targetDataset",
          columnType: "REF",
          key: 1,
          refTable: "Datasets",
          refLink: "target",
          refLabelDefault: "${name}",
          required: true,
          position: 369,
        },
        {
          name: "target variable",
          id: "targetVariable",
          columnType: "REF",
          key: 1,
          refTable: "All variables",
          refLink: "target dataset",
          refLabelDefault: "${name}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value:
                "in UI this is then one lookup field. In Excel it will be two columns. Value of 'targetVariable' is filtered based on selected 'targetCollection' and together be used for fkey(collection,dataset,name) in Variable",
            },
          ],
          position: 370,
        },
        {
          name: "match",
          id: "match",
          columnType: "ONTOLOGY",
          refTable: "Status details",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "e.g. 'complete, partial, planned, no-match'",
            },
          ],
          position: 371,
        },
        {
          name: "status",
          id: "status",
          columnType: "ONTOLOGY",
          refTable: "Status",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "whether harmonisation is still draft or final",
            },
          ],
          position: 372,
        },
        {
          name: "description",
          id: "description",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "human readible description of the mapping",
            },
          ],
          position: 373,
        },
        {
          name: "syntax",
          id: "syntax",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value:
                "formal definition of the mapping, ideally executable code",
            },
          ],
          position: 374,
        },
        {
          name: "comments",
          id: "comments",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "additional notes and comments",
            },
          ],
          position: 375,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Variable values",
      tableType: "DATA",
      id: "VariableValues",
      descriptions: [
        {
          locale: "en",
          value:
            "Listing of categorical value+label definition in case of a categorical variable",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "resource",
          id: "resource",
          columnType: "REF",
          key: 1,
          refTable: "Extended resources",
          refLabelDefault: "${id}",
          required: true,
          position: 341,
        },
        {
          name: "variable",
          id: "variable",
          columnType: "REF",
          key: 1,
          refTable: "Variables",
          refLink: "resource",
          refLabelDefault: "${dataset.name}.${name}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "e.g. PATO",
            },
          ],
          position: 342,
        },
        {
          name: "value",
          id: "value",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "e.g. '1'",
            },
          ],
          position: 343,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          required: true,
          position: 344,
        },
        {
          name: "order",
          id: "order",
          columnType: "INT",
          position: 345,
        },
        {
          name: "is missing",
          id: "isMissing",
          columnType: "BOOL",
          position: 346,
        },
        {
          name: "ontology term URI",
          id: "ontologyTermURI",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value:
                "reference to ontology term that defines this categorical value",
            },
          ],
          position: 347,
        },
        {
          name: "since version",
          id: "sinceVersion",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "When this variable value was introduced if applicable",
            },
          ],
          position: 348,
        },
        {
          name: "until version",
          id: "untilVersion",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "When this variable value was removed if applicable",
            },
          ],
          position: 349,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Variables",
      tableType: "DATA",
      id: "Variables",
      descriptions: [
        {
          locale: "en",
          value:
            "Definition of a non-repeated variable, or of the first variable from a repeated range",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "resource",
          id: "resource",
          columnType: "REF",
          key: 1,
          refTable: "Extended resources",
          refLabelDefault: "${id}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Data source that this variable was collected in",
            },
          ],
          position: 321,
        },
        {
          name: "dataset",
          id: "dataset",
          columnType: "REF",
          key: 1,
          refTable: "Datasets",
          refLink: "resource",
          refLabelDefault: "${name}",
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "Dataset this variable is part of",
            },
          ],
          position: 322,
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          descriptions: [
            {
              locale: "en",
              value: "name of the variable, unique within a table",
            },
          ],
          position: 323,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "Human friendly longer name, if applicable",
            },
          ],
          position: 324,
        },
        {
          name: "collection event",
          id: "collectionEvent",
          columnType: "REF",
          refTable: "Collection events",
          refLabelDefault: "${resource.id}.${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "in case of protocolised data collection this defines the moment in time this variable is collected on",
            },
          ],
          position: 325,
        },
        {
          name: "since version",
          id: "sinceVersion",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "When this variable was introduced",
            },
          ],
          position: 326,
        },
        {
          name: "until version",
          id: "untilVersion",
          columnType: "STRING",
          descriptions: [
            {
              locale: "en",
              value: "When this variable was removed if applicable",
            },
          ],
          position: 327,
        },
        {
          name: "format",
          id: "format",
          columnType: "ONTOLOGY",
          refTable: "Formats",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          descriptions: [
            {
              locale: "en",
              value: "Data type, e.g. string,int,decimal,date,datetime etc",
            },
          ],
          position: 328,
        },
        {
          name: "unit",
          id: "unit",
          columnType: "ONTOLOGY",
          refTable: "Units",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          position: 329,
        },
        {
          name: "references",
          id: "references",
          columnType: "REF",
          refTable: "All variables",
          refLink: "resource",
          refLabelDefault: "${dataset.name}.${name}",
          descriptions: [
            {
              locale: "en",
              value:
                "to define foreign key relationships between variables within or across tables",
            },
          ],
          position: 330,
        },
        {
          name: "mandatory",
          id: "mandatory",
          columnType: "BOOL",
          descriptions: [
            {
              locale: "en",
              value: "whether this variable is required within this collection",
            },
          ],
          position: 331,
        },
        {
          name: "description",
          id: "description",
          columnType: "TEXT",
          position: 332,
        },
        {
          name: "order",
          id: "order",
          columnType: "INT",
          descriptions: [
            {
              locale: "en",
              value: "to sort variables you can optionally add an order value",
            },
          ],
          position: 333,
        },
        {
          name: "example values",
          id: "exampleValues",
          columnType: "STRING_ARRAY",
          position: 334,
        },
        {
          name: "permitted values",
          id: "permittedValues",
          columnType: "REFBACK",
          refTable: "Variable values",
          refLabelDefault:
            "${resource.id}.${variable.dataset.name}.${variable.name}.${value}",
          refBack: "variable",
          position: 335,
        },
        {
          name: "keywords",
          id: "keywords",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Keywords",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          position: 336,
        },
        {
          name: "repeats",
          id: "repeats",
          columnType: "REFBACK",
          refTable: "Repeated variables",
          refLabelDefault: "${resource.id}.${dataset.name}.${name}",
          refBack: "is repeat of",
          descriptions: [
            {
              locale: "en",
              value:
                "listing of all repeated variables defined for this variable",
            },
          ],
          position: 337,
        },
        {
          name: "vocabularies",
          id: "vocabularies",
          columnType: "ONTOLOGY_ARRAY",
          refTable: "Vocabularies",
          refSchema: "CatalogueOntologies",
          refLabelDefault: "${name}",
          position: 338,
        },
        {
          name: "notes",
          id: "notes",
          columnType: "TEXT",
          descriptions: [
            {
              locale: "en",
              value: "Any other information on this variable",
            },
          ],
          position: 339,
        },
        {
          name: "mappings",
          id: "mappings",
          columnType: "REFBACK",
          refTable: "Variable mappings",
          refLabelDefault:
            "${source.id}.${sourceDataset.name}.${target.id}.${targetDataset.name}.${targetVariable.name}",
          refBack: "target variable",
          descriptions: [
            {
              locale: "en",
              value:
                "in case of protocolised data collection this defines the moment in time this variable is collected on",
            },
          ],
          position: 340,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
        {
          name: "mg_tableclass",
          id: "mg_tableclass",
          columnType: "STRING",
          readonly: true,
          position: 10005,
        },
      ],
    },
    {
      name: "Version",
      tableType: "DATA",
      id: "Version",
      descriptions: [
        {
          locale: "en",
          value: "3.7",
        },
      ],
      externalSchema: "catalogue",
      columns: [
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Vocabularies",
      tableType: "ONTOLOGIES",
      id: "Vocabularies",
      descriptions: [
        {
          locale: "en",
          value:
            "Are data mapped to standardised vocabularies during ETL to the CDM? If yes, what vocabularies are used for events, such as diagnoses?",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Vocabularies",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Vocabularies",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Years",
      tableType: "ONTOLOGIES",
      id: "Years",
      descriptions: [
        {
          locale: "en",
          value: "Start year of data collection",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Years",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Years",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
  ],
};

export const catalogueOntologies: ISchemaMetaData = {
  name: "CatalogueOntologies",
  tables: [
    {
      name: "ATC",
      tableType: "ONTOLOGIES",
      id: "ATC",
      descriptions: [
        {
          locale: "en",
          value: "ATC codes of medicines studied",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "ATC",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "ATC",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Age groups",
      tableType: "ONTOLOGIES",
      id: "AgeGroups",
      descriptions: [
        {
          locale: "en",
          value:
            "Select the relevant age group for this quantitative information",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Age groups",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Age groups",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Areas of information cohorts",
      tableType: "ONTOLOGIES",
      id: "AreasOfInformationCohorts",
      descriptions: [
        {
          locale: "en",
          value:
            "Areas of information that were extracted in this data collection event",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Areas of information cohorts",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Areas of information cohorts",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Areas of information ds",
      tableType: "ONTOLOGIES",
      id: "AreasOfInformationDs",
      descriptions: [
        {
          locale: "en",
          value: "Areas of information that were collected",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Areas of information ds",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Areas of information ds",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Biospecimens",
      tableType: "ONTOLOGIES",
      id: "Biospecimens",
      descriptions: [
        {
          locale: "en",
          value:
            "If the data bank contains biospecimens, what types of specimen",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Biospecimens",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Biospecimens",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Cohort designs",
      tableType: "ONTOLOGIES",
      id: "CohortDesigns",
      descriptions: [
        {
          locale: "en",
          value:
            "The study design of this cohort, i.e. cross-sectional or longitudinal",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Cohort designs",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Cohort designs",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Collection types",
      tableType: "ONTOLOGIES",
      id: "CollectionTypes",
      descriptions: [
        {
          locale: "en",
          value:
            "The data collection type of this cohort, i.e. retrospective or prospective; if both, select both",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Collection types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Collection types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Contribution types",
      tableType: "ONTOLOGIES",
      id: "ContributionTypes",
      descriptions: [
        {
          locale: "en",
          value: "Type(s) of contribution or role in the resource",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Contribution types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Contribution types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Countries",
      tableType: "ONTOLOGIES",
      id: "Countries",
      descriptions: [
        {
          locale: "en",
          value:
            "Country in which the institution head office or coordinating centre is located",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Countries",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Countries",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "DAP information",
      tableType: "ONTOLOGIES",
      id: "DAPInformation",
      descriptions: [
        {
          locale: "en",
          value:
            "Description of population subset, data access levels, completeness, reason for access",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "DAP information",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "DAP information",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Data access conditions",
      tableType: "ONTOLOGIES",
      id: "DataAccessConditions",
      descriptions: [
        {
          locale: "en",
          value: "Codes defining data access terms and conditions",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Data access conditions",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Data access conditions",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Data categories",
      tableType: "ONTOLOGIES",
      id: "DataCategories",
      descriptions: [
        {
          locale: "en",
          value: "Methods of data collection used in this collection event",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Data categories",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Data categories",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Data use conditions",
      tableType: "ONTOLOGIES",
      id: "DataUseConditions",
      descriptions: [
        {
          locale: "en",
          value: "Codes defining data use terms and conditions",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Data use conditions",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Data use conditions",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Datasource types",
      tableType: "ONTOLOGIES",
      id: "DatasourceTypes",
      descriptions: [
        {
          locale: "en",
          value:
            "Which of the following families of databanks best describe this data source",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Datasource types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Datasource types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Diseases",
      tableType: "ONTOLOGIES",
      id: "Diseases",
      descriptions: [
        {
          locale: "en",
          value:
            "Disease groups within this subcohort, based on ICD-10 and ORPHA code classifications",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Diseases",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Diseases",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Document types",
      tableType: "ONTOLOGIES",
      id: "DocumentTypes",
      descriptions: [
        {
          locale: "en",
          value: "Type of documentation",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Document types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Document types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "External identifier types",
      tableType: "ONTOLOGIES",
      id: "ExternalIdentifierTypes",
      descriptions: [
        {
          locale: "en",
          value: "External identifier type",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "External identifier types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "External identifier types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Formats",
      tableType: "ONTOLOGIES",
      id: "Formats",
      descriptions: [
        {
          locale: "en",
          value: "Data type, e.g. string,int,decimal,date,datetime etc",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Formats",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Formats",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Funding types",
      tableType: "ONTOLOGIES",
      id: "FundingTypes",
      descriptions: [
        {
          locale: "en",
          value:
            "Specify the main financial support sources for the data source in the last 3 years. Select all that apply",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Funding types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Funding types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "ICDO morphologies",
      tableType: "ONTOLOGIES",
      id: "ICDOMorphologies",
      descriptions: [
        {
          locale: "en",
          value:
            "Does the resource collect information on specific cancer subtype(s)? If yes, select morphology specifications.",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "ICDO morphologies",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "ICDO morphologies",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "ICDO topologies",
      tableType: "ONTOLOGIES",
      id: "ICDOTopologies",
      descriptions: [
        {
          locale: "en",
          value:
            "Does the resource collect information on specific cancer subtype(s)? If yes, select topology specifications.",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "ICDO topologies",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "ICDO topologies",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "INN",
      tableType: "ONTOLOGIES",
      id: "INN",
      descriptions: [
        {
          locale: "en",
          value: "INN codes of medicines studied",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "INN",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "INN",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Inclusion criteria",
      tableType: "ONTOLOGIES",
      id: "InclusionCriteria",
      descriptions: [
        {
          locale: "en",
          value:
            "Inclusion criteria applied to the participants of this resource",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Inclusion criteria",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Inclusion criteria",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Informed consent types",
      tableType: "ONTOLOGIES",
      id: "InformedConsentTypes",
      descriptions: [
        {
          locale: "en",
          value: "What type of informed consent was given for data collection?",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Informed consent types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Informed consent types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Informed consents",
      tableType: "ONTOLOGIES",
      id: "InformedConsents",
      descriptions: [
        {
          locale: "en",
          value:
            "Is informed consent required for use of the data for research purposes?",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Informed consents",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Informed consents",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Keywords",
      tableType: "ONTOLOGIES",
      id: "Keywords",
      descriptions: [
        {
          locale: "en",
          value:
            "enables grouping of table list into topic and to display tables in a tree",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Keywords",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Keywords",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Languages",
      tableType: "ONTOLOGIES",
      id: "Languages",
      descriptions: [
        {
          locale: "en",
          value:
            "Languages in which that the records are recorded (in ISO 639, https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes)",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Languages",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Languages",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Linkage strategies",
      tableType: "ONTOLOGIES",
      id: "LinkageStrategies",
      descriptions: [
        {
          locale: "en",
          value:
            "The linkage method that was used to link data banks. One entry per data bank",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Linkage strategies",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Linkage strategies",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Mapping status",
      tableType: "ONTOLOGIES",
      id: "MappingStatus",
      descriptions: [
        {
          locale: "en",
          value:
            "Mapping from collected datasets to standard/harmonized datasets, optionally including ETL syntaxes",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Mapping status",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Mapping status",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "MedDRA",
      tableType: "ONTOLOGIES",
      id: "MedDRA",
      descriptions: [
        {
          locale: "en",
          value:
            "If data on a specific disease is collected, which diseases does the data source collect information on",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "MedDRA",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "MedDRA",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Months",
      tableType: "ONTOLOGIES",
      id: "Months",
      descriptions: [
        {
          locale: "en",
          value: "Start month of data collection",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Months",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Months",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Network features",
      tableType: "ONTOLOGIES",
      id: "NetworkFeatures",
      descriptions: [
        {
          locale: "en",
          value: "Characterizations of the network",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Network features",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Network features",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Network types",
      tableType: "ONTOLOGIES",
      id: "NetworkTypes",
      descriptions: [
        {
          locale: "en",
          value: "Type of network, e.g. h2020 project",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Network types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Network types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Observation targets",
      tableType: "ONTOLOGIES",
      id: "ObservationTargets",
      descriptions: [
        {
          locale: "en",
          value: "defines what each record in this table describes",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Observation targets",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Observation targets",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Organisation features",
      tableType: "ONTOLOGIES",
      id: "OrganisationFeatures",
      descriptions: [
        {
          locale: "en",
          value: "Features that describe this organisation",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Organisation features",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Organisation features",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Organisation roles",
      tableType: "ONTOLOGIES",
      id: "OrganisationRoles",
      descriptions: [
        {
          locale: "en",
          value:
            "Roles of the institution in connection with data sources in the catalogue. Select one or more of the following:",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Organisation roles",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Organisation roles",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Organisation types",
      tableType: "ONTOLOGIES",
      id: "OrganisationTypes",
      descriptions: [
        {
          locale: "en",
          value:
            "Type of organisation; in which sector is the organisation active?",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Organisation types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Organisation types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Population entry",
      tableType: "ONTOLOGIES",
      id: "PopulationEntry",
      descriptions: [
        {
          locale: "en",
          value:
            "Select the possible causes / events that trigger the registration of a person in the data source",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Population entry",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Population entry",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Population exit",
      tableType: "ONTOLOGIES",
      id: "PopulationExit",
      descriptions: [
        {
          locale: "en",
          value:
            "Select the possible causes / events that trigger the de-registration of a person in the data source",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Population exit",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Population exit",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Population of interest",
      tableType: "ONTOLOGIES",
      id: "PopulationOfInterest",
      descriptions: [
        {
          locale: "en",
          value:
            "If population of interest is 'Other', please specify which other population has been studied",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Population of interest",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Population of interest",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Refresh periods",
      tableType: "ONTOLOGIES",
      id: "RefreshPeriods",
      descriptions: [
        {
          locale: "en",
          value:
            "If data are refreshed on fixed dates (e.g., every June and December), when are the refreshes scheduled? Select all that apply from the following:",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Refresh periods",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Refresh periods",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Regions",
      tableType: "ONTOLOGIES",
      id: "Regions",
      descriptions: [
        {
          locale: "en",
          value:
            "Geographical regions where data from this subcohort largely originate from",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Regions",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Regions",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Release types",
      tableType: "ONTOLOGIES",
      id: "ReleaseTypes",
      descriptions: [
        {
          locale: "en",
          value:
            "Select whether this resource is a closed dataset or whether new data is released continuously or at a termly basis",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Release types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Release types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Resource types",
      tableType: "ONTOLOGIES",
      id: "ResourceTypes",
      descriptions: [
        {
          locale: "en",
          value: "Type of resource, e.g. registry, cohort, biobank",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Resource types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Resource types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Sample categories",
      tableType: "ONTOLOGIES",
      id: "SampleCategories",
      descriptions: [
        {
          locale: "en",
          value: "Samples that were collected in this collection event",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Sample categories",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Sample categories",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Standardized tools",
      tableType: "ONTOLOGIES",
      id: "StandardizedTools",
      descriptions: [
        {
          locale: "en",
          value:
            "Standardized tools, e.g. surveys, questionnaires, instruments used to collect data for this collection event",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Standardized tools",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Standardized tools",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Status",
      tableType: "ONTOLOGIES",
      id: "Status",
      descriptions: [
        {
          locale: "en",
          value: "whether harmonisation is still draft or final",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Status",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Status",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Status details",
      tableType: "ONTOLOGIES",
      id: "StatusDetails",
      descriptions: [
        {
          locale: "en",
          value: "e.g. 'complete, partial, planned, no-match'",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Status details",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Status details",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Study datasource types",
      tableType: "ONTOLOGIES",
      id: "StudyDatasourceTypes",
      descriptions: [
        {
          locale: "en",
          value: "Types of data sources used",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study datasource types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study datasource types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Study design classification",
      tableType: "ONTOLOGIES",
      id: "StudyDesignClassification",
      descriptions: [
        {
          locale: "en",
          value: "Study design classifications",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study design classification",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study design classification",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Study features",
      tableType: "ONTOLOGIES",
      id: "StudyFeatures",
      descriptions: [
        {
          locale: "en",
          value: "Data features",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study features",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study features",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Study funding",
      tableType: "ONTOLOGIES",
      id: "StudyFunding",
      descriptions: [
        {
          locale: "en",
          value: "The source of funding for the study. Select all that apply",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study funding",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study funding",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Study quality marks",
      tableType: "ONTOLOGIES",
      id: "StudyQualityMarks",
      descriptions: [
        {
          locale: "en",
          value: "Quality marks, such as ENCePP seal",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study quality marks",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study quality marks",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Study requirements",
      tableType: "ONTOLOGIES",
      id: "StudyRequirements",
      descriptions: [
        {
          locale: "en",
          value: "Study requirements",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study requirements",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study requirements",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Study scopes",
      tableType: "ONTOLOGIES",
      id: "StudyScopes",
      descriptions: [
        {
          locale: "en",
          value: "Scope of the study",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study scopes",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study scopes",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Study status",
      tableType: "ONTOLOGIES",
      id: "StudyStatus",
      descriptions: [
        {
          locale: "en",
          value: "Status of the study",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study status",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study status",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Study topics",
      tableType: "ONTOLOGIES",
      id: "StudyTopics",
      descriptions: [
        {
          locale: "en",
          value: "An initial classification of the study purpose",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study topics",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study topics",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Study trial regulatory scopes",
      tableType: "ONTOLOGIES",
      id: "StudyTrialRegulatoryScopes",
      descriptions: [
        {
          locale: "en",
          value:
            "Classification of the clinical trial in relation to the medicines authorisation",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study trial regulatory scopes",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study trial regulatory scopes",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Study types",
      tableType: "ONTOLOGIES",
      id: "StudyTypes",
      descriptions: [
        {
          locale: "en",
          value: "Select 1 of the following types of study",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Study types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Study types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Submission types",
      tableType: "ONTOLOGIES",
      id: "SubmissionTypes",
      descriptions: [
        {
          locale: "en",
          value: "Type of submission, e.g. 'new entry', 'correction'",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Submission types",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Submission types",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Submitter roles",
      tableType: "ONTOLOGIES",
      id: "SubmitterRoles",
      descriptions: [
        {
          locale: "en",
          value: "Role of submitter in organisation",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Submitter roles",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Submitter roles",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Titles",
      tableType: "ONTOLOGIES",
      id: "Titles",
      descriptions: [
        {
          locale: "en",
          value: "Title of the contact person",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Titles",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Titles",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Units",
      tableType: "ONTOLOGIES",
      id: "Units",
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Units",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Units",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Vocabularies",
      tableType: "ONTOLOGIES",
      id: "Vocabularies",
      descriptions: [
        {
          locale: "en",
          value:
            "Are data mapped to standardised vocabularies during ETL to the CDM? If yes, what vocabularies are used for events, such as diagnoses?",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Vocabularies",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Vocabularies",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
    {
      name: "Years",
      tableType: "ONTOLOGIES",
      id: "Years",
      descriptions: [
        {
          locale: "en",
          value: "Start year of data collection",
        },
      ],
      externalSchema: "CatalogueOntologies",
      columns: [
        {
          name: "order",
          id: "order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          descriptions: [
            {
              locale: "en",
              value: "Order of this term within the code system",
            },
          ],
        },
        {
          name: "name",
          id: "name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          descriptions: [
            {
              locale: "en",
              value: "Unique name of the term within this table",
            },
          ],
          position: 1,
        },
        {
          name: "label",
          id: "label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          descriptions: [
            {
              locale: "en",
              value:
                "User-friendly label for this term. Should be unique in parent",
            },
          ],
          position: 2,
        },
        {
          name: "parent",
          id: "parent",
          columnType: "REF",
          refTable: "Years",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          descriptions: [
            {
              locale: "en",
              value: "The parent term, in case this code exists in a hierarchy",
            },
          ],
          position: 3,
        },
        {
          name: "codesystem",
          id: "codesystem",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C70895"],
          descriptions: [
            {
              locale: "en",
              value:
                "Abbreviation of the code system/ontology this term belongs to",
            },
          ],
          position: 4,
        },
        {
          name: "code",
          id: "code",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C25162"],
          descriptions: [
            {
              locale: "en",
              value:
                "Identifier used for this term within this code system/ontology",
            },
          ],
          position: 5,
        },
        {
          name: "ontologyTermURI",
          id: "ontologyTermURI",
          columnType: "HYPERLINK",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C114456"],
          descriptions: [
            {
              locale: "en",
              value: "Reference to structured definition of this term",
            },
          ],
          position: 6,
        },
        {
          name: "definition",
          id: "definition",
          columnType: "TEXT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42777"],
          descriptions: [
            {
              locale: "en",
              value: "A concise explanation of the meaning of this term",
            },
          ],
          position: 7,
        },
        {
          name: "children",
          id: "children",
          columnType: "REFBACK",
          refTable: "Years",
          refLabelDefault: "${name}",
          refBack: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          descriptions: [
            {
              locale: "en",
              value:
                "Child terms, in case this term is the parent of other terms",
            },
          ],
          position: 8,
        },
        {
          name: "mg_draft",
          id: "mg_draft",
          columnType: "BOOL",
          position: -5,
        },
        {
          name: "mg_insertedBy",
          id: "mg_insertedBy",
          columnType: "STRING",
          position: -4,
        },
        {
          name: "mg_insertedOn",
          id: "mg_insertedOn",
          columnType: "DATETIME",
          position: -3,
        },
        {
          name: "mg_updatedBy",
          id: "mg_updatedBy",
          columnType: "STRING",
          position: -2,
        },
        {
          name: "mg_updatedOn",
          id: "mg_updatedOn",
          columnType: "DATETIME",
          position: -1,
        },
      ],
    },
  ],
};
