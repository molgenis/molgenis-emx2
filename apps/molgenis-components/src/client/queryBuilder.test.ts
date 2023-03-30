import { describe, assert, test } from "vitest";
import { columnNames } from "./queryBuilder";

describe("columnNames", () => {

  const expand = 2
  test("it should return the column names", () => {
    const expectedResult =
      " name category { name } photoUrls status tags{ order name label parent{ name } codesystem code ontologyTermURI definition children { name } } weight orders { orderId pet{ name } quantity price complete status }";
    const result = columnNames("pet store", "Pet", metaData, expand)
    assert.equal(result, expectedResult);
  });
});

// test meta data with mg_columns removed 
const metaData = {
    "name": "pet store",
    "tables": [
        {
            "name": "Category",
            "tableType": "DATA",
            "id": "Category",
            "externalSchema": "pet store",
            "columns": [
                {
                    "name": "name",
                    "id": "name",
                    "columnType": "STRING",
                    "key": 1,
                    "required": true
                },
            ]
        },
        {
            "name": "Order",
            "tableType": "DATA",
            "id": "Order",
            "externalSchema": "pet store",
            "columns": [
                {
                    "name": "orderId",
                    "id": "orderId",
                    "columnType": "STRING",
                    "key": 1,
                    "required": true
                },
                {
                    "name": "pet",
                    "id": "pet",
                    "columnType": "REF",
                    "refTable": "Pet",
                    "refLabelDefault": "${name}",
                    "position": 1
                },
                {
                    "name": "quantity",
                    "id": "quantity",
                    "columnType": "LONG",
                    "position": 2,
                    "validation": "if(quantity < 1) 'quantity should be >= 1'"
                },
                {
                    "name": "price",
                    "id": "price",
                    "columnType": "DECIMAL",
                    "position": 3,
                    "validation": "price >= 1"
                },
                {
                    "name": "complete",
                    "id": "complete",
                    "columnType": "BOOL",
                    "position": 4
                },
                {
                    "name": "status",
                    "id": "status",
                    "columnType": "STRING",
                    "position": 5
                },
            ]
        },
        {
            "name": "Pet",
            "tableType": "DATA",
            "id": "Pet",
            "descriptions": [
                {
                    "locale": "en",
                    "value": "My pet store example table"
                }
            ],
            "externalSchema": "pet store",
            "columns": [
                {
                    "name": "name",
                    "id": "name",
                    "columnType": "STRING",
                    "key": 1,
                    "required": true,
                    "descriptions": [
                        {
                            "locale": "en",
                            "value": "the name"
                        }
                    ]
                },
                {
                    "name": "category",
                    "id": "category",
                    "columnType": "REF",
                    "refTable": "Category",
                    "refLabelDefault": "${name}",
                    "required": true,
                    "position": 1
                },
                {
                    "name": "photoUrls",
                    "id": "photoUrls",
                    "columnType": "STRING_ARRAY",
                    "position": 2
                },
                {
                    "name": "details",
                    "id": "details",
                    "columnType": "HEADING",
                    "descriptions": [
                        {
                            "locale": "en",
                            "value": "Details"
                        }
                    ],
                    "position": 3
                },
                {
                    "name": "status",
                    "id": "status",
                    "columnType": "STRING",
                    "position": 4
                },
                {
                    "name": "tags",
                    "id": "tags",
                    "columnType": "ONTOLOGY_ARRAY",
                    "refTable": "Tag",
                    "refLabelDefault": "${name}",
                    "position": 5
                },
                {
                    "name": "weight",
                    "id": "weight",
                    "columnType": "DECIMAL",
                    "required": true,
                    "position": 6
                },
                {
                    "name": "orders",
                    "id": "orders",
                    "columnType": "REFBACK",
                    "refTable": "Order",
                    "refLabelDefault": "${orderId}",
                    "refBack": "pet",
                    "position": 7
                },
            ]
        },
        {
            "name": "Tag",
            "tableType": "ONTOLOGIES",
            "id": "Tag",
            "externalSchema": "pet store",
            "columns": [
                {
                    "name": "order",
                    "id": "order",
                    "columnType": "INT",
                    "semantics": [
                        "http://purl.obolibrary.org/obo/NCIT_C42680"
                    ],
                    "descriptions": [
                        {
                            "locale": "en",
                            "value": "Order of this term within the code system"
                        }
                    ]
                },
                {
                    "name": "name",
                    "id": "name",
                    "columnType": "STRING",
                    "key": 1,
                    "required": true,
                    "semantics": [
                        "http://purl.obolibrary.org/obo/NCIT_C42614"
                    ],
                    "descriptions": [
                        {
                            "locale": "en",
                            "value": "Unique name of the term within this table"
                        }
                    ],
                    "position": 1
                },
                {
                    "name": "label",
                    "id": "label",
                    "columnType": "STRING",
                    "semantics": [
                        "http://purl.obolibrary.org/obo/NCIT_C45561"
                    ],
                    "descriptions": [
                        {
                            "locale": "en",
                            "value": "User-friendly label for this term. Should be unique in parent"
                        }
                    ],
                    "position": 2
                },
                {
                    "name": "parent",
                    "id": "parent",
                    "columnType": "REF",
                    "refTable": "Tag",
                    "refLabelDefault": "${name}",
                    "semantics": [
                        "http://purl.obolibrary.org/obo/NCIT_C80013"
                    ],
                    "descriptions": [
                        {
                            "locale": "en",
                            "value": "The parent term, in case this code exists in a hierarchy"
                        }
                    ],
                    "position": 3
                },
                {
                    "name": "codesystem",
                    "id": "codesystem",
                    "columnType": "STRING",
                    "semantics": [
                        "http://purl.obolibrary.org/obo/NCIT_C70895"
                    ],
                    "descriptions": [
                        {
                            "locale": "en",
                            "value": "Abbreviation of the code system/ontology this term belongs to"
                        }
                    ],
                    "position": 4
                },
                {
                    "name": "code",
                    "id": "code",
                    "columnType": "STRING",
                    "semantics": [
                        "http://purl.obolibrary.org/obo/NCIT_C25162"
                    ],
                    "descriptions": [
                        {
                            "locale": "en",
                            "value": "Identifier used for this term within this code system/ontology"
                        }
                    ],
                    "position": 5
                },
                {
                    "name": "ontologyTermURI",
                    "id": "ontologyTermURI",
                    "columnType": "HYPERLINK",
                    "semantics": [
                        "http://purl.obolibrary.org/obo/NCIT_C114456"
                    ],
                    "descriptions": [
                        {
                            "locale": "en",
                            "value": "Reference to structured definition of this term"
                        }
                    ],
                    "position": 6
                },
                {
                    "name": "definition",
                    "id": "definition",
                    "columnType": "TEXT",
                    "semantics": [
                        "http://purl.obolibrary.org/obo/NCIT_C42777"
                    ],
                    "descriptions": [
                        {
                            "locale": "en",
                            "value": "A concise explanation of the meaning of this term"
                        }
                    ],
                    "position": 7
                },
                {
                    "name": "children",
                    "id": "children",
                    "columnType": "REFBACK",
                    "refTable": "Tag",
                    "refLabelDefault": "${name}",
                    "refBack": "parent",
                    "semantics": [
                        "http://purl.obolibrary.org/obo/NCIT_C90504"
                    ],
                    "descriptions": [
                        {
                            "locale": "en",
                            "value": "Child terms, in case this term is the parent of other terms"
                        }
                    ],
                    "position": 8
                },
            ]
        },
        {
            "name": "User",
            "tableType": "DATA",
            "id": "User",
            "externalSchema": "pet store",
            "columns": [
                {
                    "name": "username",
                    "id": "username",
                    "columnType": "STRING",
                    "key": 1,
                    "required": true
                },
                {
                    "name": "firstName",
                    "id": "firstName",
                    "columnType": "STRING",
                    "position": 1
                },
                {
                    "name": "lastName",
                    "id": "lastName",
                    "columnType": "STRING",
                    "position": 2
                },
                {
                    "name": "picture",
                    "id": "picture",
                    "columnType": "FILE",
                    "position": 3
                },
                {
                    "name": "email",
                    "id": "email",
                    "columnType": "EMAIL",
                    "position": 4
                },
                {
                    "name": "password",
                    "id": "password",
                    "columnType": "STRING",
                    "position": 5
                },
                {
                    "name": "phone",
                    "id": "phone",
                    "columnType": "STRING",
                    "position": 6
                },
                {
                    "name": "userStatus",
                    "id": "userStatus",
                    "columnType": "INT",
                    "position": 7
                },
                {
                    "name": "pets",
                    "id": "pets",
                    "columnType": "REF_ARRAY",
                    "refTable": "Pet",
                    "refLabelDefault": "${name}",
                    "position": 8
                },
            ]
        }
    ]
}