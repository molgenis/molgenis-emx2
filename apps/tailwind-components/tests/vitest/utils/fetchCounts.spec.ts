import { describe, it, expect, vi } from "vitest";
import { fetchCounts } from "../../../app/utils/fetchCounts";

function makeFetcher(rows: any[], tableId: string) {
  return vi.fn().mockResolvedValue({ [`${tableId}_groupBy`]: rows });
}

describe("fetchCounts - BOOL", () => {
  it("returns all three options when all are present", async () => {
    const fetcher = makeFetcher(
      [
        { count: 5, active: true },
        { count: 3, active: false },
        { count: 2, active: null },
      ],
      "Patient"
    );
    const result = await fetchCounts(
      "mySchema",
      "Patient",
      "active",
      "BOOL",
      {},
      fetcher
    );
    expect(result).toEqual([
      { name: "true", label: "Yes", count: 5 },
      { name: "false", label: "No", count: 3 },
      { name: "_null_", label: "Not set", count: 2 },
    ]);
  });

  it("fills missing options with count 0", async () => {
    const fetcher = makeFetcher([{ count: 5, active: true }], "Patient");
    const result = await fetchCounts(
      "mySchema",
      "Patient",
      "active",
      "BOOL",
      {},
      fetcher
    );
    expect(result).toEqual([
      { name: "true", label: "Yes", count: 5 },
      { name: "false", label: "No", count: 0 },
      { name: "_null_", label: "Not set", count: 0 },
    ]);
  });

  it("returns all three with count 0 when no rows", async () => {
    const fetcher = makeFetcher([], "Patient");
    const result = await fetchCounts(
      "mySchema",
      "Patient",
      "active",
      "BOOL",
      {},
      fetcher
    );
    expect(result).toEqual([
      { name: "true", label: "Yes", count: 0 },
      { name: "false", label: "No", count: 0 },
      { name: "_null_", label: "Not set", count: 0 },
    ]);
  });

  it("always returns options in true/false/null order", async () => {
    const fetcher = makeFetcher(
      [
        { count: 2, active: null },
        { count: 3, active: false },
        { count: 5, active: true },
      ],
      "Patient"
    );
    const result = await fetchCounts(
      "mySchema",
      "Patient",
      "active",
      "BOOL",
      {},
      fetcher
    );
    expect(result.map((o) => o.name)).toEqual(["true", "false", "_null_"]);
  });
});
