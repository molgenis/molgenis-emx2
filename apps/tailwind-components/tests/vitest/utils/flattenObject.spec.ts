import { expect, test } from "vitest";
import { flattenObject } from "../../../utils/flattenObject";

test("flattens the object", () => {
  expect(flattenObject({ a: 1, b: 2 })).toBe(" 1 2");
});
