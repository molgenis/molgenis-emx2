import { describe, it, expect } from "vitest";
import {
  catalogueMetadata,
  catalogueOntologies,
  petStoreMetadata,
} from "./test-resources/metadata";
import { buildRecordDetailsQueryFields } from "../src/tableQuery";
import type { ColumnType, ISchemaMetaData } from "../src/types";

const compositionSchema = (childColumnType: ColumnType): ISchemaMetaData => ({
  id: "compositions",
  label: "compositions",
  tables: [
    {
      id: "Resources",
      name: "Resources",
      label: "Resources",
      tableType: "DATA",
      schemaId: "compositions",
      columns: [
        { id: "id", label: "id", columnType: "STRING", key: 1 },
        {
          id: "tables",
          label: "tables",
          columnType: childColumnType,
          refSchemaId: "compositions",
          refTableId: "ResourceTables",
          refBackId: "resource",
        },
      ],
    },
    {
      id: "ResourceTables",
      name: "ResourceTables",
      label: "ResourceTables",
      tableType: "DATA",
      schemaId: "compositions",
      columns: [
        {
          id: "resource",
          label: "resource",
          columnType: "REF",
          key: 1,
          refSchemaId: "compositions",
          refTableId: "Resources",
        },
        { id: "name", label: "name", columnType: "STRING", key: 1 },
      ],
    },
  ],
});

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
      "id pid acronym name type { order name label  codesystem code ontologyTermURI definition  } typeOther institution institutionAcronym email logo { id, size, filename, extension, url } address expertise country { order name label  codesystem code ontologyTermURI definition  } features { order name label  codesystem code ontologyTermURI definition  } role { order name label  codesystem code ontologyTermURI definition  } leadingResources { id pid acronym name website   description   logo { id, size, filename, extension, url }    fundingStatement acknowledgements  } additionalResources { id pid acronym name website   description   logo { id, size, filename, extension, url }    fundingStatement acknowledgements  } website description contacts {   roleDescription firstName lastName prefix initials   email orcid homepage photo { id, size, filename, extension, url } expertise }";

    expect(
      buildRecordDetailsQueryFields(schemas, "catalogue", "Organisations")
    ).toEqual(expectedFields);
  });

  it("should subselect a PARTS column, exactly as its REFBACK flavor", () => {
    const partsFields = buildRecordDetailsQueryFields(
      { compositions: compositionSchema("PARTS") },
      "compositions",
      "Resources"
    );

    expect(partsFields).toEqual("id tables {  name }");
    expect(partsFields).toEqual(
      buildRecordDetailsQueryFields(
        { compositions: compositionSchema("REFBACK") },
        "compositions",
        "Resources"
      )
    );
  });
});
