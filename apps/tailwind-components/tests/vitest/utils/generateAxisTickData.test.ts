import { expect, test, describe } from "vitest";
import { generateAxisTickData } from "../../../app/utils/viz";

const data = [
  { name: "Group A", value: 42 },
  { name: "Group B", value: 31 },
  { name: "Group C", value: 82 },
  { name: "Group D", value: 3 },
];

const tickData = generateAxisTickData(data, "value");

describe("generatAxisTickData (viz):", () => {
  test("tick limit is nicely rounded", () => {
    expect(tickData.limit).toEqual(100);
  });

  test("ticks intervals are cleanly and evenly spaced (25)", () => {
    expect(tickData.ticks).toEqual([0, 25, 50, 75, 100]);
  });

  test("min is always the lowest value in the dataset", () => {
    expect(tickData.min).toBe(3);
  });

  test("max is the highest value in the dataset", () => {
    expect(tickData.max).toBe(82);
  });
});
