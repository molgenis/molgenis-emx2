import { describe, assert, test } from "vitest";
import { getColumnIds } from "./queryBuilder";
import { petStoreMetaMock as metaData } from "./mocks/response/petStoreMetadata";
import { catalogueMetadata } from "./mocks/response/catalogueMetadata";
import { catalogueOntologiesMetadata } from "./mocks/response/catalogueOntologiesMetadata";
import type { ISchemaMetaData } from "meta-data-utils";

describe("getColumnIds", () => {
  const EXPAND_ONE = 1;
  const EXPAND_TWO = 2;

  const metaDataMap: Record<string, ISchemaMetaData> = {
    "pet store": metaData,
  };

  //Pet
  //expand 1
  test("it should return the pet columns expanded, children are not expanded, expect primary keys", () => {
    const result = getColumnIds("pet store", "Pet", metaDataMap, EXPAND_ONE);
    assert.equal(
      result,
      " name category { name } tags {name, label} weight orders { orderId }"
    );
  });
  //expand 2
  test("it should return the pet columns expanded", () => {
    const result = getColumnIds("pet store", "Pet", metaDataMap, EXPAND_TWO);
    assert.equal(
      result,
      " name category { name } tags {name, label} weight orders { orderId pet { name } }"
    );
  });

  //expand 2
  test("it should return the order columns expanded, only arrays are not expanded beyond level 0 (so 'pet.tags' is missing)", () => {
    const expectedResult = " orderId pet { name category { name } weight }";
    const result = getColumnIds("pet store", "Order", metaDataMap, EXPAND_TWO);
    assert.equal(result, expectedResult);
  });

  //ontology
  //expand 1
  test("it should return the ontology columns expanded, only children array are not expanded beyond level 0", () => {
    const expectedResult =
      " order name label parent { name } children { name }";
    const result = getColumnIds("pet store", "Tag", metaDataMap, EXPAND_ONE);
    assert.equal(result, expectedResult);
  });
  //expand 2
  test("it should return the ontology columns expanded, only children array are not expanded beyond level 0", () => {
    const expectedResult =
      " order name label parent { order name label parent { name } } children { order name label parent { name } }";
    const result = getColumnIds("pet store", "Tag", metaDataMap, EXPAND_TWO);
    assert.equal(result, expectedResult);
  });

  test("it should use the passed metaData map to resolve metaData from linked schema's", () => {
    const meta: Record<string, ISchemaMetaData> = {
      "catalogue-demo": catalogueMetadata,
      CatalogueOntologies: catalogueOntologiesMetadata,
    };
    const columnIds = getColumnIds(
      "catalogue-demo",
      "Cohorts",
      meta,
      EXPAND_TWO
    );

    // sample result
    assert.include(columnIds, "contactEmail contacts { resource { id }");
  });
});
