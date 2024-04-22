import { describe, expect, test } from "vitest";
import {
  getName,
  mapObjArray,
  urlToString,
  propertyToString,
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

describe("mapRange", () => {
  test("", () => {});
});
