import { describe, it, expect } from "vitest";

import { buildRecordDetailsQueryFields } from "./tableQuery";
import {
  petStoreMetadata,
  catalogueMetadata,
  catalogueOntologies,
} from "./test-resources/metadata";

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
      "id pid acronym name type { order name label  codesystem code ontologyTermURI definition  } typeOther institution institutionAcronym email logo { id, size, extension, url } address expertise country { order name label  codesystem code ontologyTermURI definition  } features { order name label  codesystem code ontologyTermURI definition  } role { order name label  codesystem code ontologyTermURI definition  } leadingResources { id pid acronym name website   description   logo { id, size, extension, url }    fundingStatement acknowledgements  } additionalResources { id pid acronym name website   description   logo { id, size, extension, url }    fundingStatement acknowledgements  } website description contacts {   roleDescription firstName lastName prefix initials   email orcid homepage photo { id, size, extension, url } expertise }";

    expect(
      buildRecordDetailsQueryFields(schemas, "catalogue", "Organisations")
    ).toEqual(expectedFields);
  });
});
