import { describe, it, expect, vi } from "vitest";
import { fetchCounts } from "../../../app/utils/fetchCounts";

function makeFetcher(rows: any[], tableId: string) {
  return vi.fn().mockResolvedValue({ [`${tableId}_groupBy`]: rows });
}

describe("fetchCounts - nested dotted paths (ONTOLOGY)", () => {
  it("builds nested GraphQL and parses nested response for ontology", async () => {
    const rows = [
      {
        count: 4,
        self: {
          ontologySmallType: { name: "typeA", label: "Type A", parent: null },
        },
      },
      {
        count: 2,
        self: {
          ontologySmallType: {
            name: "typeB",
            label: "Type B",
            parent: { name: "typeA" },
          },
        },
      },
    ];
    const fetcher = vi.fn().mockResolvedValue({ Patient_groupBy: rows });
    const result = await fetchCounts(
      "mySchema",
      "Patient",
      "self.ontologySmallType",
      "ONTOLOGY",
      {},
      fetcher
    );
    expect(fetcher).toHaveBeenCalledOnce();
    const query: string = fetcher.mock.calls[0][1];
    expect(query).toContain("self {");
    expect(query).toContain("ontologySmallType { name label parent { name } }");
    expect(query).not.toContain("self.ontologySmallType");
    expect(result).toHaveLength(1);
    expect(result[0].name).toBe("typeA");
    expect(result[0].children).toHaveLength(1);
    expect(result[0].children![0].name).toBe("typeB");
  });
});

describe("fetchCounts - nested dotted paths (BOOL)", () => {
  it("builds nested GraphQL and parses nested response for bool", async () => {
    const rows = [
      { count: 5, self: { active: true } },
      { count: 3, self: { active: false } },
    ];
    const fetcher = vi.fn().mockResolvedValue({ Patient_groupBy: rows });
    const result = await fetchCounts(
      "mySchema",
      "Patient",
      "self.active",
      "BOOL",
      {},
      fetcher
    );
    const query: string = fetcher.mock.calls[0][1];
    expect(query).toContain("self { active }");
    expect(query).not.toContain("self.active");
    expect(result).toEqual([
      { name: "true", label: "Yes", count: 5 },
      { name: "false", label: "No", count: 3 },
      { name: "_null_", label: "Not set", count: 0 },
    ]);
  });
});

describe("fetchCounts - nested dotted paths (RADIO)", () => {
  it("returns flat options from nested path", async () => {
    const rows = [
      { count: 7, self: { status: "active" } },
      { count: 1, self: { status: "inactive" } },
    ];
    const fetcher = vi.fn().mockResolvedValue({ Patient_groupBy: rows });
    const result = await fetchCounts(
      "mySchema",
      "Patient",
      "self.status",
      "RADIO",
      {},
      fetcher
    );
    const query: string = fetcher.mock.calls[0][1];
    expect(query).toContain("self { status }");
    expect(result).toEqual([
      { name: "active", count: 7 },
      { name: "inactive", count: 1 },
    ]);
  });
});

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

describe("fetchCounts - hierarchical ontology (refTableId provided)", () => {
  function makeHierarchicalFetcher(
    ancestorTerms: Array<{
      name: string;
      label?: string;
      parent?: { name: string } | null;
    }>,
    groupByRows: Array<{ count: number; [key: string]: any }>,
    aggCountsByParent: Record<string, number>
  ) {
    return vi.fn().mockImplementation((_schemaId: string, query: string) => {
      if (query.includes("_match_any_including_parents")) {
        return Promise.resolve({ OntologyTable: ancestorTerms });
      }
      if (query.includes("_groupBy")) {
        return Promise.resolve({ Patient_groupBy: groupByRows });
      }
      if (query.includes("_agg")) {
        const match = query.match(
          /_match_any_including_children:\s*\["([^"]+)"\]/
        );
        const parentName = match?.[1];
        const count = parentName ? aggCountsByParent[parentName] ?? 0 : 0;
        return Promise.resolve({ Patient_agg: { count } });
      }
      return Promise.resolve({});
    });
  }

  it("uses two queries: _groupBy then _match_any_including_parents", async () => {
    const groupByRows = [
      {
        count: 3,
        disease: { name: "child1", label: "Child 1", parent: { name: "root" } },
      },
    ];
    const ancestorTerms = [
      { name: "root", label: "Root", parent: null },
      { name: "child1", label: "Child 1", parent: { name: "root" } },
    ];
    const fetcher = makeHierarchicalFetcher(ancestorTerms, groupByRows, {});

    await fetchCounts(
      "mySchema",
      "Patient",
      "disease",
      "ONTOLOGY",
      {},
      fetcher,
      "OntologyTable",
      null
    );

    const queries: string[] = fetcher.mock.calls.map(
      ([_s, q]: [string, string]) => q
    );
    expect(queries.some((q) => q.includes("_groupBy"))).toBe(true);
    expect(
      queries.some((q) => q.includes("_match_any_including_parents"))
    ).toBe(true);
    expect(queries.some((q) => q.includes("_agg"))).toBe(false);
  });

  it("fetches only counted terms and their ancestors, applies leaf counts", async () => {
    const groupByRows = [
      {
        count: 3,
        disease: { name: "child1", label: "Child 1", parent: { name: "root" } },
      },
    ];
    const ancestorTerms = [
      { name: "root", label: "Root", parent: null },
      { name: "child1", label: "Child 1", parent: { name: "root" } },
    ];
    const fetcher = makeHierarchicalFetcher(ancestorTerms, groupByRows, {});

    const result = await fetchCounts(
      "mySchema",
      "Patient",
      "disease",
      "ONTOLOGY",
      {},
      fetcher,
      "OntologyTable",
      null
    );

    expect(result).toHaveLength(1);
    expect(result[0].name).toBe("root");
    expect(result[0].count).toBe(3);
    expect(result[0].children).toHaveLength(1);
    expect(result[0].children![0].name).toBe("child1");
    expect(result[0].children![0].count).toBe(3);
  });

  it("rolls up counts from children for ONTOLOGY (non-array) — no _agg needed", async () => {
    const groupByRows = [
      {
        count: 4,
        disease: {
          name: "leafA",
          label: "Leaf A",
          parent: { name: "branch" },
        },
      },
      {
        count: 2,
        disease: {
          name: "leafB",
          label: "Leaf B",
          parent: { name: "branch" },
        },
      },
    ];
    const ancestorTerms = [
      { name: "branch", label: "Branch", parent: { name: "root" } },
      { name: "root", label: "Root", parent: null },
      { name: "leafA", label: "Leaf A", parent: { name: "branch" } },
      { name: "leafB", label: "Leaf B", parent: { name: "branch" } },
    ];
    const fetcher = makeHierarchicalFetcher(ancestorTerms, groupByRows, {});

    const result = await fetchCounts(
      "mySchema",
      "Patient",
      "disease",
      "ONTOLOGY",
      {},
      fetcher,
      "OntologyTable",
      null
    );

    expect(result).toHaveLength(1);
    expect(result[0].name).toBe("root");
    expect(result[0].count).toBe(6);
    expect(result[0].children![0].name).toBe("branch");
    expect(result[0].children![0].count).toBe(6);

    const aggCalls = fetcher.mock.calls.filter(([_s, q]: [string, string]) =>
      q.includes("_agg")
    );
    expect(aggCalls).toHaveLength(0);
  });

  it("uses _agg queries for parent counts with ONTOLOGY_ARRAY", async () => {
    const groupByRows = [
      {
        count: 3,
        disease: {
          name: "child1",
          label: "Child 1",
          parent: { name: "root" },
        },
      },
    ];
    const ancestorTerms = [
      { name: "root", label: "Root", parent: null },
      { name: "child1", label: "Child 1", parent: { name: "root" } },
    ];
    const fetcher = makeHierarchicalFetcher(ancestorTerms, groupByRows, {
      root: 3,
    });

    const result = await fetchCounts(
      "mySchema",
      "Patient",
      "disease",
      "ONTOLOGY_ARRAY",
      {},
      fetcher,
      "OntologyTable",
      null
    );

    expect(result).toHaveLength(1);
    expect(result[0].name).toBe("root");
    expect(result[0].count).toBe(3);

    const aggCalls = fetcher.mock.calls.filter(([_s, q]: [string, string]) =>
      q.includes("_agg")
    );
    expect(aggCalls.length).toBeGreaterThan(0);
  });

  it("handles multiple ancestor levels returned in one query", async () => {
    const groupByRows = [
      {
        count: 2,
        disease: {
          name: "leaf",
          label: "Leaf",
          parent: { name: "mid" },
        },
      },
    ];
    const ancestorTerms = [
      { name: "root", label: "Root", parent: null },
      { name: "mid", label: "Mid", parent: { name: "root" } },
      { name: "leaf", label: "Leaf", parent: { name: "mid" } },
    ];
    const fetcher = makeHierarchicalFetcher(ancestorTerms, groupByRows, {});

    const result = await fetchCounts(
      "mySchema",
      "Patient",
      "disease",
      "ONTOLOGY",
      {},
      fetcher,
      "OntologyTable",
      null
    );

    expect(result).toHaveLength(1);
    expect(result[0].name).toBe("root");
    expect(result[0].children![0].name).toBe("mid");
    expect(result[0].children![0].children![0].name).toBe("leaf");

    const ancestorQueryCalls = fetcher.mock.calls.filter(
      ([_s, q]: [string, string]) => q.includes("_match_any_including_parents")
    );
    expect(ancestorQueryCalls).toHaveLength(1);
  });

  it("does NOT fetch entire ontology table — uses filtered ancestor queries", async () => {
    const groupByRows = [
      {
        count: 1,
        disease: { name: "leaf", label: "Leaf", parent: { name: "root" } },
      },
    ];
    const ancestorTerms = [
      { name: "root", label: "Root", parent: null },
      { name: "leaf", label: "Leaf", parent: { name: "root" } },
    ];
    const fetcher = makeHierarchicalFetcher(ancestorTerms, groupByRows, {});

    await fetchCounts(
      "mySchema",
      "Patient",
      "disease",
      "ONTOLOGY",
      {},
      fetcher,
      "OntologyTable",
      null
    );

    const unfilteredTreeFetch = fetcher.mock.calls.find(
      ([_schema, query]: [string, string]) =>
        query.includes("OntologyTable {") && !query.includes("filter:")
    );
    expect(unfilteredTreeFetch).toBeUndefined();
  });

  it("uses refSchemaId for ancestor fetch queries", async () => {
    const groupByRows = [
      { count: 1, disease: { name: "termX", label: "Term X", parent: null } },
    ];
    const ancestorTerms = [{ name: "termX", label: "Term X", parent: null }];
    const fetcher = makeHierarchicalFetcher(ancestorTerms, groupByRows, {});

    await fetchCounts(
      "mainSchema",
      "Patient",
      "disease",
      "ONTOLOGY",
      {},
      fetcher,
      "OntologyTable",
      "sharedSchema"
    );

    const ancestorFetchCall = fetcher.mock.calls.find(
      ([_schema, query]: [string, string]) =>
        query.includes("_match_any_including_parents")
    );
    expect(ancestorFetchCall).toBeDefined();
    expect(ancestorFetchCall![0]).toBe("sharedSchema");
  });

  it("falls back to groupBy when refTableId is not provided", async () => {
    const rows = [
      { count: 5, disease: { name: "termA", label: "Term A", parent: null } },
    ];
    const fetcher = vi.fn().mockResolvedValue({ Patient_groupBy: rows });

    const result = await fetchCounts(
      "mySchema",
      "Patient",
      "disease",
      "ONTOLOGY",
      {},
      fetcher
    );

    expect(fetcher).toHaveBeenCalledOnce();
    expect(result).toHaveLength(1);
    expect(result[0].name).toBe("termA");
    expect(result[0].count).toBe(5);
  });

  it("applies cross-filter to _agg parent count queries for ONTOLOGY_ARRAY", async () => {
    const groupByRows = [
      {
        count: 2,
        disease: {
          name: "child1",
          label: "Child 1",
          parent: { name: "parent1" },
        },
      },
    ];
    const ancestorTerms = [
      { name: "parent1", label: "Parent 1", parent: null },
      { name: "child1", label: "Child 1", parent: { name: "parent1" } },
    ];
    const crossFilter = { status: { equals: "active" } };
    const capturedAggQueries: string[] = [];

    const fetcher = vi
      .fn()
      .mockImplementation((_schemaId: string, query: string) => {
        if (query.includes("_match_any_including_parents")) {
          return Promise.resolve({ OntologyTable: ancestorTerms });
        }
        if (query.includes("_groupBy")) {
          return Promise.resolve({ Patient_groupBy: groupByRows });
        }
        if (query.includes("_agg")) {
          capturedAggQueries.push(query);
          return Promise.resolve({ Patient_agg: { count: 2 } });
        }
        return Promise.resolve({});
      });

    await fetchCounts(
      "mySchema",
      "Patient",
      "disease",
      "ONTOLOGY_ARRAY",
      crossFilter,
      fetcher,
      "OntologyTable",
      null
    );

    expect(capturedAggQueries).toHaveLength(1);
    expect(capturedAggQueries[0]).toContain("status");
    expect(capturedAggQueries[0]).toContain("_match_any_including_children");
  });

  it("returns empty array when groupBy fetch fails", async () => {
    const fetcher = vi.fn().mockImplementation((_s: string, query: string) => {
      if (query.includes("_groupBy")) {
        return Promise.reject(new Error("network error"));
      }
      return Promise.resolve({});
    });

    const result = await fetchCounts(
      "mySchema",
      "Patient",
      "disease",
      "ONTOLOGY",
      {},
      fetcher,
      "OntologyTable",
      null
    );

    expect(result).toEqual([]);
  });

  it("returns empty array when groupBy returns no rows", async () => {
    const fetcher = vi.fn().mockImplementation((_s: string, query: string) => {
      if (query.includes("_groupBy")) {
        return Promise.resolve({ Patient_groupBy: [] });
      }
      return Promise.resolve({});
    });

    const result = await fetchCounts(
      "mySchema",
      "Patient",
      "disease",
      "ONTOLOGY",
      {},
      fetcher,
      "OntologyTable",
      null
    );

    expect(result).toEqual([]);
  });
});
