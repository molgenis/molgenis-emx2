import { expect, test, describe } from "vitest";
import { seqAlongBy } from "../../../app/utils/viz";

describe("seqAlongBy (viz):", () => {
  test("default sequence produces intervals of 1", () => {
    const result = JSON.stringify(seqAlongBy(1, 5));
    expect(result).toEqual("[1,2,3,4,5]");
  });

  test("defined intervals produce correct sequence", () => {
    const result = JSON.stringify(seqAlongBy(0, 25, 5));
    expect(result).toEqual("[0,5,10,15,20,25]");
  });
});
