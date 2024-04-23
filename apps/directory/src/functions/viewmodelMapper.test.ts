import { describe, expect, test } from "vitest";
import {
  getName,
  mapObjArray,
  urlToString,
  propertyToString,
  rangeToString,
  mapQualityStandards,
  mapAlsoKnownIn,
  mapNetworkInfo,
  collectionReportInformation,
} from "./viewmodelMapper";

describe("getName", () => {
  test("it should create a name in de order: title, first, last, title, role", () => {
    const contact = {
      title_before_name: "Prof. dr.",
      first_name: "Henk",
      last_name: "de Vries",
      title_after_name: "Phd",
      role: "Overseer",
    };
    const result = getName(contact);
    const expectedResult = "Prof. dr. Henk de Vries Phd\nOverseer";
    expect(result).toEqual(expectedResult);
  });

  test("it should trim excess spaces", () => {
    const contact = {
      first_name: "Henk",
      last_name: "de Vries     ",
    };
    const result = getName(contact);
    const expectedResult = "Henk de Vries";
    expect(result).toEqual(expectedResult);
  });
});

describe("propertyToString", () => {
  const property = "property";
  test("it should change the property of an object to a string, adding pre- and suffixes ", () => {
    const object = { property: "some property" };
    const prefix = "pre";
    const suffix = "suf";
    const result = propertyToString(object, property, prefix, suffix);
    const expectedResult = "pre some property suf";
    expect(result).toEqual(expectedResult);
  });

  test("it should return an empty string if the object doesn't exist", () => {
    const object = undefined;
    const result = propertyToString(object, property, undefined, undefined);
    expect(result).toEqual("");
  });

  test("it should return the object if the object is a string", () => {
    const object = "stringObject";
    const result = propertyToString(object, property, undefined, undefined);
    expect(result).toEqual(object);
  });

  test("it should return an empty string if the property is not on the object", () => {
    const object = {};
    const result = propertyToString(object, property, undefined, undefined);
    expect(result).toEqual("");
  });
});

describe("mapObjArray", () => {
  test("it should map the input to {label, uri} objects", () => {
    const objects = [
      { label: "label1", name: "name1", ontologyTermURI: "ontologyTermURI" },
      { name: "name2", url: "url", ontologyTermURI: "not used" },
      { label: "label3", name: "name3", uri: "uri", ontologyTermURI: "used" },
    ];
    const result = mapObjArray(objects);
    const expectedResult = [
      { label: "label1", uri: "ontologyTermURI" },
      { label: "name2", uri: "url" },
      { label: "label3", uri: "uri" },
    ];
    expect(result).toEqual(expectedResult);
  });

  test("it should return just the label if there is no uri", () => {
    const objects = [{ label: "label1", name: "name1" }, { name: "name2" }];
    const result = mapObjArray(objects);
    const expectedResult = ["label1", "name2"];
    expect(result).toEqual(expectedResult);
  });

  test("it should return an empty array if there is no input", () => {
    const result = mapObjArray(undefined);
    expect(result).toEqual([]);
  });
});

describe("urlToString", () => {
  test("return the url if the starts with 'http'", () => {
    const url = "https://molgenis.org";
    const result = urlToString(url);
    expect(result).toEqual(url);
  });

  test("return the input if it is falsy", () => {
    const url = "";
    const result = urlToString(url);
    expect(result).toEqual(url);
  });

  test("return the url with 'https' as prefix if doesn't start with 'http'", () => {
    const url = "molgenis.org";
    const result = urlToString(url);
    const expectedResult = "https://molgenis.org";
    expect(result).toEqual(expectedResult);
  });
});

describe("rangeToString", () => {
  test("it should return the min and max with unit", () => {
    const min = -5;
    const max = 5;
    const unit = { label: "kgs" };
    const result = rangeToString(min, max, unit);
    const expectedResult = "-5-5 kgs";
    expect(result).toEqual(expectedResult);
  });

  test("it should return an empty string when the unit is undefined", () => {
    const min = -5;
    const max = 5;
    const unit = undefined;
    const result = rangeToString(min, max, unit);
    const expectedResult = "";
    expect(result).toEqual(expectedResult);
  });

  test("it should return an empty string when min and max are undefined", () => {
    const min = undefined;
    const max = undefined;
    const unit = { label: "kgs" };
    const result = rangeToString(min, max, unit);
    const expectedResult = "";
    expect(result).toEqual(expectedResult);
  });

  test("it should return an empty string when min is undefined and max is 0", () => {
    // this test is sus, is this really how the function should behave?
    const min = undefined;
    const max = 0;
    const unit = { label: "kgs" };
    const result = rangeToString(min, max, unit);
    const expectedResult = "";
    expect(result).toEqual(expectedResult);
  });

  test("it should return the min and max with unit when min is 0", () => {
    const min = 0;
    const max = 5;
    const unit = { label: "kgs" };
    const result = rangeToString(min, max, unit);
    const expectedResult = "0-5 kgs";
    expect(result).toEqual(expectedResult);
  });

  test("it should return the min with unit", () => {
    const min = -5;
    const max = undefined;
    const unit = { label: "kgs" };
    const result = rangeToString(min, max, unit);
    const expectedResult = "> -5 kgs";
    expect(result).toEqual(expectedResult);
  });

  test("it should return the max with unit", () => {
    const min = undefined;
    const max = -5;
    const unit = { label: "kgs" };
    const result = rangeToString(min, max, unit);
    const expectedResult = "< -5 kgs";
    expect(result).toEqual(expectedResult);
  });
});

// describe("getViewmodel", () => {});
// describe("getCollectionDetails", () => {});
// describe("getBiobankDetails", () => {});

describe("collectionReportInformation", () => {
  test("it should return the info for a collection report", () => {
    const collection = {
      head: { title_before_name: "dr.", first_name: "B.", last_name: "Dewitt" },
      contact: {
        first_name: "Henk",
        email: "bla@bla.bla",
        phone: "0123456789",
      },
      also_known: [{ url: "url1", name_system: "molgenis" }],
      biobank: {
        id: "bid",
        name: "bname",
        juridical_person: "jp",

        url: "www.bla.bla",
        contact: { email: "abl@abl.abl" },
      },
      country: { label: "nl" },
      network: [{ name: "nname", id: "nid" }],
      quality: [{ quality_standard: { name: "qname" } }],
      collaboration_commercial: "no idea what the typing is",
      collaboration_non_for_profit: "no idea what the typing is",
      parent_collection: "weak typing sucks",
    };

    const result = collectionReportInformation(collection);

    const expectedResult = {
      head: "dr. B. Dewitt",
      contact: { name: "Henk", email: "bla@bla.bla", phone: "0123456789" },
      also_known: [{ value: "url1", type: "url", label: "molgenis" }],
      biobank: {
        id: "bid",
        name: "bname",
        juridical_person: "jp",
        country: "nl",
        report: "/biobank/bid",
        website: "www.bla.bla",
        email: "abl@abl.abl",
      },
      networks: [{ name: "nname", report: "/network/nid" }],
      certifications: ["qname"],
      collaboration: [
        { name: "Commercial", value: "yes" },
        { name: "Not for profit", value: "yes" },
      ],
      parentCollection: "weak typing sucks", //very consistent with the also_known
    };
    expect(result).toEqual(expectedResult);
  });

  test("it should return a very minimal report if the info is not there", () => {
    const collection = {};
    const result = collectionReportInformation(collection);
    const expectedResult = {
      collaboration: [],
    };
    expect(result).toEqual(expectedResult);
  });
});

describe("mapNetworkInfo", () => {
  test("it should maps network in to a name and a report", () => {
    const data = { network: [{ name: "name1", id: "id1" }] };
    const result = mapNetworkInfo(data);
    const expectedResult = [
      {
        name: { value: "name1", type: "string" },
        report: { value: "/network/id1", type: "report" },
      },
    ];
    expect(result).toEqual(expectedResult);
  });
});

describe("mapAlsoKnownIn", () => {
  test("it should map the AlsoKnowIn to their value, type and label", () => {
    const instance = { also_known: [{ url: "url1", name_system: "molgenis" }] };
    const result = mapAlsoKnownIn(instance);
    const expectedResult = [{ value: "url1", type: "url", label: "molgenis" }];
    expect(result).toEqual(expectedResult);
  });
});

describe("mapQualityStandards", () => {
  test("it should map the qualities into an array of names", () => {
    const instance = [
      { quality_standard: { name: "name1" } },
      { quality_standard: { name: "name2" } },
    ];
    const result = mapQualityStandards(instance);
    const expectedResult = ["name1", "name2"];
    expect(result).toEqual(expectedResult);
  });

  test("it should map an empty array into an empty array", () => {
    const instance: any[] = [];
    const result = mapQualityStandards(instance);
    const expectedResult: string[] = [];
    expect(result).toEqual(expectedResult);
  });
});
