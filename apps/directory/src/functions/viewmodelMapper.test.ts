import { describe, assert, test, expect, vi, it } from "vitest";
import { getName } from "./viewmodelMapper";

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
