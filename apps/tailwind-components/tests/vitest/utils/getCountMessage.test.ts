import { expect, it, describe } from "vitest";
import { getCountMessage } from "../../../app/utils/getCountMessage";

describe("getCountMessage", () => {
  it("should return correct message for page 1", () => {
    expect(getCountMessage(1, 10, 50)).toBe("Showing 1 to 10 of 50 items");
  });

  it("should return correct message for page 2", () => {
    expect(getCountMessage(2, 10, 50)).toBe("Showing 11 to 20 of 50 items");
  });

  it("should return correct message for last page with fewer items", () => {
    expect(getCountMessage(5, 10, 45)).toBe("Showing 41 to 45 of 45 items");
  });

  it("should return correct message when totalCount is less than pageSize", () => {
    expect(getCountMessage(1, 10, 5)).toBe("Showing 1 to 5 of 5 items");
  });

  it("should return correct message when totalCount is zero", () => {
    expect(getCountMessage(1, 10, 0)).toBe("");
  });
});
