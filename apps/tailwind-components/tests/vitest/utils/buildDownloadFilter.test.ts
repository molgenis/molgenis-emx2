import { describe, expect, it } from "vitest";
import { buildDownloadFilter } from "../../../app/utils/buildDownloadFilter";

const nameFilter = { name: { like: ["pooky"] } };
const pooky = { name: "pooky" };
const spike = { name: "spike" };

describe("buildDownloadFilter", () => {
  it("should return undefined when there is nothing to filter on", () => {
    expect(buildDownloadFilter()).toBeUndefined();
    expect(buildDownloadFilter({}, [])).toBeUndefined();
  });

  it("should return the filter when no rows are selected", () => {
    expect(buildDownloadFilter(nameFilter, [])).toEqual(nameFilter);
  });

  it("should match the selected rows on their keys", () => {
    expect(buildDownloadFilter(undefined, [pooky, spike])).toEqual({
      equals: [pooky, spike],
    });
  });

  it("should pass composite ref keys as nested objects", () => {
    const key = { owner: { firstName: "Jane", lastName: "Doe" }, name: "x" };
    expect(buildDownloadFilter(undefined, [key])).toEqual({ equals: [key] });
  });

  it("should require both the filter and the row selection to match", () => {
    expect(buildDownloadFilter(nameFilter, [pooky])).toEqual({
      _and: [nameFilter, { equals: [pooky] }],
    });
  });
});
