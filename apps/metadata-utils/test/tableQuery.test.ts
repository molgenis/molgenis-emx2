import { describe, it, expect } from "vitest";
import {
  catalogueMetadata,
  catalogueOntologies,
  petStoreMetadata,
} from "./test-resources/metadata";
import {
  buildRecordDetailsQueryFields,
  getRecordTableMetaData,
} from "../src/tableQuery";
import type { ISchemaMetaData } from "../src/types";

describe("buildRecordDetailsQueryFields", () => {
  it("should return the query fields simple schema (pet store)", () => {
    const schemas = { petStore: petStoreMetadata };
    expect(buildRecordDetailsQueryFields(schemas, "petStore", "Pet")).toEqual(
      "name category { name } photoUrls status tags { order name label  codesystem code ontologyTermURI definition  } weight orders { orderId  quantity price complete status }"
    );
    expect(buildRecordDetailsQueryFields(schemas, "petStore", "Order")).toEqual(
      "orderId pet { name  photoUrls status  weight  } quantity price complete status"
    );
  });

  it("should return the query fields for schema with external schema  ", () => {
    const schemas = {
      catalogue: catalogueMetadata,
      CatalogueOntologies: catalogueOntologies,
    };

    const expectedFields =
      "id pid acronym name type { order name label  codesystem code ontologyTermURI definition  } typeOther institution institutionAcronym email logo { id, size, filename, extension, url } address expertise country { order name label  codesystem code ontologyTermURI definition  } features { order name label  codesystem code ontologyTermURI definition  } role { order name label  codesystem code ontologyTermURI definition  } leadingResources { id pid acronym name website   description   logo { id, size, filename, extension, url }    fundingStatement acknowledgements  mg_tableclass } additionalResources { id pid acronym name website   description   logo { id, size, filename, extension, url }    fundingStatement acknowledgements  mg_tableclass } website description contacts {   roleDescription firstName lastName prefix initials   email orcid homepage photo { id, size, filename, extension, url } expertise } mg_tableclass";

    expect(
      buildRecordDetailsQueryFields(schemas, "catalogue", "Organisations")
    ).toEqual(expectedFields);
  });
});

describe("getRecordTableMetaData", () => {
  const schemaMetaData: ISchemaMetaData = {
    id: "catalogue-demo",
    label: "catalogue-demo",
    tables: [
      {
        id: "Resources",
        schemaId: "catalogue-demo",
        name: "Resources",
        label: "Resources",
        tableType: "DATA",
        columns: [],
      },
      {
        id: "Collections",
        schemaId: "catalogue-demo",
        name: "Collections",
        label: "Collections",
        tableType: "DATA",
        columns: [],
      },
    ],
  };

  it("resolves the subclass table when mg_tableclass names a different table in this schema", () => {
    const result = getRecordTableMetaData(schemaMetaData, "Resources", {
      mg_tableclass: "catalogue-demo.Collections",
    });

    expect(result).toBe(schemaMetaData.tables[1]);
  });

  it("falls back to the route table when mg_tableclass names the route table itself (same class)", () => {
    const result = getRecordTableMetaData(schemaMetaData, "Resources", {
      mg_tableclass: "catalogue-demo.Resources",
    });

    expect(result).toBe(schemaMetaData.tables[0]);
  });

  it("falls back to the route table when the record carries no mg_tableclass field (missing field)", () => {
    const result = getRecordTableMetaData(schemaMetaData, "Resources", {
      id: "RAINE",
    });

    expect(result).toBe(schemaMetaData.tables[0]);
  });

  it("falls back to the route table when there is no record", () => {
    expect(getRecordTableMetaData(schemaMetaData, "Resources", null)).toBe(
      schemaMetaData.tables[0]
    );
    expect(getRecordTableMetaData(schemaMetaData, "Resources", undefined)).toBe(
      schemaMetaData.tables[0]
    );
  });

  it("falls back to the route table when mg_tableclass names an unknown table", () => {
    const result = getRecordTableMetaData(schemaMetaData, "Resources", {
      mg_tableclass: "catalogue-demo.NoSuchTable",
    });

    expect(result).toBe(schemaMetaData.tables[0]);
  });

  it("falls back to the route table when mg_tableclass names a table in another schema", () => {
    const result = getRecordTableMetaData(schemaMetaData, "Resources", {
      mg_tableclass: "other-schema.Collections",
    });

    expect(result).toBe(schemaMetaData.tables[0]);
  });

  it("falls back to the route table when mg_tableclass does not parse into schema and table", () => {
    const result = getRecordTableMetaData(schemaMetaData, "Resources", {
      mg_tableclass: "catalogue-demo",
    });

    expect(result).toBe(schemaMetaData.tables[0]);
  });
});
