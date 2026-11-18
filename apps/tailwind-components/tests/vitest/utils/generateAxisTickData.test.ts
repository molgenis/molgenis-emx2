import { expect, test, describe } from "vitest";
import { generateAxisTickData } from "../../../app/utils/viz";

const data = [
  { name: "Group A", value: 42 },
  { name: "Group B", value: 31 },
  { name: "Group C", value: 82 },
  { name: "Group D", value: 3 },
];

const data2 = [
  { name: "Group A", value: 10 },
  { name: "Group B", value: 5 },
  { name: "Group C", value: -10 },
  { name: "Group D", value: -8 },
];

const tickData = generateAxisTickData(data, "value");
const tickData2 = generateAxisTickData(data2, "value");

describe("generateAxisTickData (viz):", () => {
  test("tick limit is nicely rounded", () => {
    expect(tickData.limit).toEqual(90);
    expect(tickData2.limit).toEqual(10);
  });

  test("ticks intervals are cleanly and evenly spaced", () => {
    expect(tickData.ticks).toEqual([0, 10, 20, 30, 40, 50, 60, 70, 80, 90]);
    expect(tickData2.ticks).toEqual([-10, -8, -6, -4, -2, 0, 2, 4, 6, 8, 10]);
  });

  test("min is always 0", () => {
    expect(tickData.min).toBe(0);
  });

  test("tick 0 is always included if data has negative numbers", () => {
    expect(tickData2.min).toBe(-10);
    expect(tickData.ticks).toContain(0);
  });

  test("max is the highest value in the dataset", () => {
    expect(tickData.max).toBe(82);
    expect(tickData2.max).toBe(10);
  });
});
