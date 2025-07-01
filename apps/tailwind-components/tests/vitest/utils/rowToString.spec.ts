import { rowToString } from "../../../utils/rowToString";
import { describe, expect, test } from "vitest";

describe("rowToString", () => {
  test("it should return the label according to the template", () => {
    const object = { id: "someid", name: "naam", otherField: "bla" };
    const labelTemplate = "${otherField}";
    const result = rowToString(object, labelTemplate);
    expect(result).toEqual("bla");
  });

  test("it should return the name if the label is empty and there is no primaryKey", () => {
    const object = { id: "someid", primaryKey: { id: "primKey" } };
    const labelTemplate = "${nonExistantField}";
    const result = rowToString(object, labelTemplate);
    expect(result).toEqual(" primKey");
  });

  test("it should return the name if the label is empty and there is no primaryKey", () => {
    const object = { id: "someid", name: "naam" };
    const labelTemplate = "${nonExistantField}";
    const result = rowToString(object, labelTemplate);
    expect(result).toEqual("naam");
  });

  test("it should return the id if the label is empty and there is no primaryKey or name", () => {
    const object = { id: "someid" };
    const labelTemplate = "${nonExistantField}";
    const result = rowToString(object, labelTemplate);
    expect(result).toEqual("someid");
  });
});
