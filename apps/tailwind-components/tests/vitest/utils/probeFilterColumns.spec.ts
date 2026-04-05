import { describe, it, expect, vi, beforeEach } from "vitest";

const mockFetchGraphql = vi.hoisted(() => vi.fn());
vi.mock("../../../app/composables/fetchGraphql", () => ({
  default: mockFetchGraphql,
}));

import { probeFilterColumns } from "../../../app/utils/probeFilterColumns";

describe("probeFilterColumns", () => {
  beforeEach(() => {
    mockFetchGraphql.mockReset();
  });

  it("returns set of column paths that have data", async () => {
    mockFetchGraphql.mockResolvedValue({
      probe_disease: { count: 42 },
      probe_hospital: { count: 0 },
      probe_phenotype: { count: 7 },
    });

    const result = await probeFilterColumns("mySchema", "Patient", [
      "disease",
      "hospital",
      "phenotype",
    ]);

    expect(result).toEqual(new Set(["disease", "phenotype"]));
  });

  it("returns empty set when no columns have data", async () => {
    mockFetchGraphql.mockResolvedValue({
      probe_disease: { count: 0 },
      probe_hospital: { count: 0 },
    });

    const result = await probeFilterColumns("mySchema", "Patient", [
      "disease",
      "hospital",
    ]);

    expect(result).toEqual(new Set());
  });

  it("returns all columns on GraphQL error (fail-open)", async () => {
    mockFetchGraphql.mockRejectedValue(new Error("Network error"));

    const result = await probeFilterColumns("mySchema", "Patient", [
      "disease",
      "hospital",
    ]);

    expect(result).toEqual(new Set(["disease", "hospital"]));
  });

  it("returns empty set for empty input", async () => {
    const result = await probeFilterColumns("mySchema", "Patient", []);
    expect(result).toEqual(new Set());
    expect(mockFetchGraphql).not.toHaveBeenCalled();
  });

  it("builds correct notNull filter for nested paths", async () => {
    mockFetchGraphql.mockResolvedValue({
      probe_collectionEvents_ageGroups: { count: 5 },
    });

    await probeFilterColumns("mySchema", "Patient", [
      "collectionEvents.ageGroups",
    ]);

    expect(mockFetchGraphql).toHaveBeenCalledWith(
      "mySchema",
      expect.any(String),
      {
        filter_probe_collectionEvents_ageGroups: {
          collectionEvents: { ageGroups: { _notNull: true } },
        },
      }
    );
  });

  it("builds correct query with aliases", async () => {
    mockFetchGraphql.mockResolvedValue({
      probe_disease: { count: 10 },
    });

    await probeFilterColumns("mySchema", "Patient", ["disease"]);

    const query = mockFetchGraphql.mock.calls[0][1];
    expect(query).toContain("probe_disease: Patient_agg");
    expect(query).toContain("$filter_probe_disease: PatientFilter");
  });
});
