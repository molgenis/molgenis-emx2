import { expect, test, describe } from "vitest";
import { asDataObject } from "../../../app/utils/viz";

const data = [
  { name: "Group A", value: 42 },
  { name: "Group B", value: 31 },
  { name: "Group C", value: 82 },
];

describe("asDataObject (viz):", () => {
  test("Data is transformed into key-value pairs", () => {
    const result = asDataObject(data, "name", "value");
    expect(result).toEqual({ "Group A": 42, "Group B": 31, "Group C": 82 });
  });

  test("Data is transformed into key-value pairs and sorted", () => {
    const result = asDataObject(data, "name", "value");
    expect(result).toEqual({ "Group C": 82, "Group A": 42, "Group B": 31 });
  });
});
