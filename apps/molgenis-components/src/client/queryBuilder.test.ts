import { describe, assert, test } from "vitest";
import { getColumnIds } from "./queryBuilder";
import type { ISchemaMetaData } from "meta-data-utils";

describe("getColumnIds", () => {
  const EXPAND_ONE = 1;
  const EXPAND_TWO = 2;

  //Pet
  //expand 1
  test("it should return the pet columns expanded, children are not expanded, expect primary keys", () => {
    const result = getColumnIds("pet store", "Pet", metaData, EXPAND_ONE);
    assert.equal(
      result,
      " name category { name } tags {name, label} weight orders { orderId }"
    );
  });
  //expand 2
  test("it should return the pet columns expanded", () => {
    const result = getColumnIds("pet store", "Pet", metaData, EXPAND_TWO);
    assert.equal(
      result,
      " name category { name } tags {name, label} weight orders { orderId pet { name } }"
    );
  });

  //expand 2
  test("it should return the order columns expanded, only arrays are not expanded beyond level 0 (so 'pet.tags' is missing)", () => {
    const expectedResult = " orderId pet { name category { name } weight }";
    const result = getColumnIds("pet store", "Order", metaData, EXPAND_TWO);
    assert.equal(result, expectedResult);
  });

  //ontology
  //expand 1
  test("it should return the ontology columns expanded, only children array are not expanded beyond level 0", () => {
    const expectedResult =
      " order name label parent { name } children { name }";
    const result = getColumnIds("pet store", "Tag", metaData, EXPAND_ONE);
    assert.equal(result, expectedResult);
  });
  //expand 2
  test("it should return the ontology columns expanded, only children array are not expanded beyond level 0", () => {
    const expectedResult =
      " order name label parent { order name label parent { name } } children { order name label parent { name } }";
    const result = getColumnIds("pet store", "Tag", metaData, EXPAND_TWO);
    assert.equal(result, expectedResult);
  });
});

// test meta data with mg_columns removed
const metaData: ISchemaMetaData = {
  id: "pet store",
  label: "Pet store",
  tables: [
    {
      label: "Category",
      tableType: "DATA",
      id: "Category",
      schemaId: "pet store",
      columns: [
        {
          id: "name",
          label: "Name",
          columnType: "STRING",
          key: 1,
          required: true,
        },
      ],
    },
    {
      label: "Order",
      tableType: "DATA",
      id: "Order",
      schemaId: "pet store",
      columns: [
        {
          id: "orderId",
          label: "Order id",
          columnType: "STRING",
          key: 1,
          required: true,
        },
        {
          id: "pet",
          label: "Pet",
          columnType: "REF",
          refTableId: "Pet",
          refLabelDefault: "${name}",
          position: 1,
        },
      ],
    },
    {
      id: "Pet",
      tableType: "DATA",
      label: "Pet",
      description: "My pet store example table",
      schemaId: "pet store",
      columns: [
        {
          id: "name",
          label: "Name",
          columnType: "STRING",
          key: 1,
          required: true,
          description: "the name",
        },
        {
          id: "category",
          label: "Category",
          columnType: "REF",
          refTableId: "Category",
          refLabelDefault: "${name}",
          required: true,
          position: 1,
        },

        {
          id: "tags",
          label: "Tags",
          columnType: "ONTOLOGY_ARRAY",
          refTableId: "Tag",
          refLabelDefault: "${name}",
          position: 5,
        },
        {
          id: "weight",
          label: "Weight",
          columnType: "DECIMAL",
          required: true,
          position: 6,
        },
        {
          id: "orders",
          label: "Orders",
          columnType: "REFBACK",
          refTableId: "Order",
          refLabelDefault: "${orderId}",
          refBackId: "pet",
          position: 7,
        },
      ],
    },
    {
      id: "Tag",
      label: "Tag",
      tableType: "ONTOLOGIES",
      schemaId: "pet store",
      columns: [
        {
          id: "order",
          label: "Order",
          columnType: "INT",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42680"],
          description: "Order of this term within the code system",
        },
        {
          id: "name",
          label: "Name",
          columnType: "STRING",
          key: 1,
          required: true,
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C42614"],
          description: "Unique name of the term within this table",
          position: 1,
        },
        {
          id: "label",
          label: "Label",
          columnType: "STRING",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C45561"],
          description:
            "User-friendly label for this term. Should be unique in parent",

          position: 2,
        },
        {
          id: "parent",
          label: "Parent",
          columnType: "REF",
          refTableId: "Tag",
          refLabelDefault: "${name}",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C80013"],
          description:
            "The parent term, in case this code exists in a hierarchy",
          position: 3,
        },

        {
          id: "children",
          label: "Children",
          columnType: "REFBACK",
          refTableId: "Tag",
          refLabelDefault: "${name}",
          refBackId: "parent",
          semantics: ["http://purl.obolibrary.org/obo/NCIT_C90504"],
          description:
            "Child terms, in case this term is the parent of other terms",
          position: 8,
        },
      ],
    },
    {
      id: "User",
      label: "User",
      tableType: "DATA",
      schemaId: "pet store",
      columns: [
        {
          id: "username",
          label: "User name",
          columnType: "STRING",
          key: 1,
          required: true,
        },
        {
          id: "firstName",
          label: "First name",
          columnType: "STRING",
          position: 1,
        },
        {
          id: "lastName",
          label: "Last name",
          columnType: "STRING",
          position: 2,
        },
        {
          id: "picture",
          label: "Picture",
          columnType: "FILE",
          position: 3,
        },
        {
          id: "pets",
          label: "Pets",
          columnType: "REF_ARRAY",
          refTableId: "Pet",
          refLabelDefault: "${name}",
          position: 8,
        },
      ],
    },
  ],
};
