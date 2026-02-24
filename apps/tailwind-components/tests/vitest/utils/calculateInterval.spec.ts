import { expect, test, describe } from "vitest";
import { calculateInterval } from "../../../app/utils/viz";

describe("calculateInterval (viz):", () => {
  test("value greater than 5000 returns 1000", () => {
    expect(calculateInterval(6000)).toBe(1000);
  });

  test("value between 1500 and 5000 returns 500", () => {
    expect(calculateInterval(2500)).toBe(500);
  });

  test("value between 500 and 1500 returns 250", () => {
    expect(calculateInterval(1000)).toBe(250);
  });

  test("value between 100 and 500 returns 100", () => {
    expect(calculateInterval(250)).toBe(100);
  });

  test("value between 50 and 100 returns 25", () => {
    expect(calculateInterval(100)).toBe(25);
  });

  test("value between 10 and 50 returns 10", () => {
    expect(calculateInterval(45)).toBe(10);
  });

  test("value between 5 and 10 returns 2", () => {
    expect(calculateInterval(8)).toBe(2);
  });
});
