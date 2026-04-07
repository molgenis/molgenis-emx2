import { describe, it, expect, vi, beforeEach } from "vitest";
import type { ICountFetcher } from "../../../app/utils/createCountFetcher";

const mockFetchGraphql = vi.hoisted(() => vi.fn());
vi.mock("../../../app/composables/fetchGraphql", () => ({
  default: mockFetchGraphql,
}));

import { probeFilterColumns } from "../../../app/utils/probeFilterColumns";

function makeCountFetcher(counts: Map<string, number>): {
  fetcher: ICountFetcher;
  getCountFetcher: ReturnType<typeof vi.fn>;
} {
  const fetcher = {
    fetchAllOntologyBaseCounts: vi.fn().mockResolvedValue(counts),
    fetchRefCounts: vi.fn(),
    fetchRefBaseCounts: vi.fn(),
    fetchOntologyLeafCounts: vi.fn(),
    fetchOntologyLeafBaseCounts: vi.fn(),
    fetchOntologyParentCounts: vi.fn(),
    fetchOntologyParentBaseCounts: vi.fn(),
  } as unknown as ICountFetcher;
  const getCountFetcher = vi.fn().mockReturnValue(fetcher);
  return { fetcher, getCountFetcher };
}

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

  describe("ontology column probing via count fetcher", () => {
    it("uses getCountFetcher for ONTOLOGY columns and includes paths with results", async () => {
      const columnTypes = new Map([
        ["sampleType", "ONTOLOGY"],
        ["tissueType", "ONTOLOGY"],
      ]);
      const { fetcher: sampleFetcher, getCountFetcher } = makeCountFetcher(
        new Map([["DNA", 3]])
      );
      const emptyFetcher = {
        ...sampleFetcher,
        fetchAllOntologyBaseCounts: vi.fn().mockResolvedValue(new Map()),
      } as unknown as ICountFetcher;
      getCountFetcher
        .mockReturnValueOnce(sampleFetcher)
        .mockReturnValueOnce(emptyFetcher);

      const result = await probeFilterColumns(
        "mySchema",
        "Experiments",
        ["sampleType", "tissueType"],
        columnTypes,
        getCountFetcher
      );

      expect(result).toEqual(new Set(["sampleType"]));
      expect(getCountFetcher).toHaveBeenCalledWith("sampleType");
      expect(getCountFetcher).toHaveBeenCalledWith("tissueType");
    });

    it("uses getCountFetcher for ONTOLOGY_ARRAY columns", async () => {
      const columnTypes = new Map([["tags", "ONTOLOGY_ARRAY"]]);
      const { fetcher, getCountFetcher } = makeCountFetcher(
        new Map([["rare", 2]])
      );

      const result = await probeFilterColumns(
        "mySchema",
        "Samples",
        ["tags"],
        columnTypes,
        getCountFetcher
      );

      expect(result).toEqual(new Set(["tags"]));
      expect(fetcher.fetchAllOntologyBaseCounts).toHaveBeenCalled();
    });

    it("does not call fetchGraphql for ontology columns", async () => {
      const columnTypes = new Map([["sampleType", "ONTOLOGY"]]);
      const { getCountFetcher } = makeCountFetcher(new Map([["RNA", 1]]));

      await probeFilterColumns(
        "mySchema",
        "Experiments",
        ["sampleType"],
        columnTypes,
        getCountFetcher
      );

      expect(mockFetchGraphql).not.toHaveBeenCalled();
    });

    it("mixes _notNull for regular columns and count fetcher for ontology columns", async () => {
      mockFetchGraphql.mockResolvedValueOnce({
        probe_name: { count: 5 },
      });

      const columnTypes = new Map([
        ["name", "STRING"],
        ["diagnosis", "ONTOLOGY"],
      ]);
      const { getCountFetcher } = makeCountFetcher(new Map([["T1D", 2]]));

      const result = await probeFilterColumns(
        "mySchema",
        "Patient",
        ["name", "diagnosis"],
        columnTypes,
        getCountFetcher
      );

      expect(result).toEqual(new Set(["name", "diagnosis"]));
      expect(mockFetchGraphql).toHaveBeenCalledTimes(1);
      expect(getCountFetcher).toHaveBeenCalledWith("diagnosis");
    });

    it("returns fail-open on ontology count fetcher error", async () => {
      const columnTypes = new Map([["sampleType", "ONTOLOGY"]]);
      const failingFetcher = {
        fetchAllOntologyBaseCounts: vi
          .fn()
          .mockRejectedValue(new Error("fetch error")),
      } as unknown as ICountFetcher;
      const getCountFetcher = vi.fn().mockReturnValue(failingFetcher);

      const result = await probeFilterColumns(
        "mySchema",
        "Experiments",
        ["sampleType"],
        columnTypes,
        getCountFetcher
      );

      expect(result).toEqual(new Set(["sampleType"]));
    });

    it("excludes ontology columns with empty count results", async () => {
      const columnTypes = new Map([["emptyOntology", "ONTOLOGY"]]);
      const { getCountFetcher } = makeCountFetcher(new Map());

      const result = await probeFilterColumns(
        "mySchema",
        "Table",
        ["emptyOntology"],
        columnTypes,
        getCountFetcher
      );

      expect(result).toEqual(new Set());
    });

    it("calls getCountFetcher with the correct path for nested ontology columns", async () => {
      const columnTypes = new Map([["samples.type", "ONTOLOGY"]]);
      const { getCountFetcher } = makeCountFetcher(new Map([["blood", 1]]));

      await probeFilterColumns(
        "mySchema",
        "Experiments",
        ["samples.type"],
        columnTypes,
        getCountFetcher
      );

      expect(getCountFetcher).toHaveBeenCalledWith("samples.type");
    });
  });
});
