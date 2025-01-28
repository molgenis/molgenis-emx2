import { describe, expect, test } from "vitest";
import { generateAxisTickData } from "../src/utils/generateAxisTicks";

describe("generateAxisTicks", () => {
  const data = [
    { value: 12 },
    { value: 44 },
    { value: 6 },
    { value: 20 },
    { value: 16 },
    { value: 87 },
    { value: 92 },
    { value: 77 },
  ];

  const axisData = generateAxisTickData(data, "value");

  test("axis tick interval of 25 is returned", () => {
    expect(axisData.limit).toBe(100);
  });

  test("axis ticks are generated between 0 and 100 by 25", () => {
    expect(axisData.ticks).toStrictEqual([0, 25, 50, 75, 100]);
  });
});
