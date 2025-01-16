import { describe, expect, test } from "vitest";
import {
  asKeyValuePairs,
  sum,
  sumObjectValues,
  uniqueValues,
} from "../src/utils/";

const data = [
  { group: "A", value: 32, count: 4 },
  { group: "B", value: 22, count: 10 },
  { group: "C", value: 19, count: 2 },
];

const dataObject = asKeyValuePairs(data, "group", "value");

describe("asKeyValuePairs", () => {
  test("data is transformed into an object of key-value pairs", () => {
    expect(dataObject).toStrictEqual({ A: 32, B: 22, C: 19 });
  });
});

describe("sum", () => {
  test("Column in a dataset is summed correctly", () => {
    expect(sum(data, "value")).toBe(73);
    expect(sum(data, "count")).toBe(16);
  });
});


describe("sumObjectValues", () => {
  test("Sum on objects are calculated correctly", () => {
    expect(sumObjectValues(dataObject)).toBe(73);
  });
});

describe("uniqueValues", ()=> {
  test("Unique values are extracted from a column in a dataset", () => {
    const data = [
      { value: "cat" },
      { value: "mouse" },
      { value: "cat" },
      { value: "dog" },
    ];
    expect(uniqueValues(data, "value")).toStrictEqual(["cat", "dog", "mouse"]);
  });
});

