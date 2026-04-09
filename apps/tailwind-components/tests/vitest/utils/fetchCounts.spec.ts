import { describe, it, expect, vi, beforeEach } from "vitest";
import { fetchCounts, ontologyTreeCache } from "../../../app/utils/fetchCounts";

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
  beforeEach(() => {
    ontologyTreeCache.clear();
  });

  function makeHierarchicalFetcher(
    ontologyTerms: Array<{
      name: string;
      label?: string;
      parent?: { name: string } | null;
    }>,
    groupByRows: Array<{ count: number; [key: string]: any }>,
    aggCountsByParent: Record<string, number>
  ) {
    return vi.fn().mockImplementation((_schemaId: string, query: string) => {
      if (query.includes("OntologyTable {")) {
        return Promise.resolve({ OntologyTable: ontologyTerms });
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

  it("fetches full ontology tree and applies leaf counts", async () => {
    const ontologyTerms = [
      { name: "root", label: "Root", parent: null },
      { name: "child1", label: "Child 1", parent: { name: "root" } },
      { name: "child2", label: "Child 2", parent: { name: "root" } },
    ];
    const groupByRows = [{ count: 3, disease: { name: "child1" } }];
    const fetcher = makeHierarchicalFetcher(ontologyTerms, groupByRows, {
      root: 3,
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

    expect(result).toHaveLength(1);
    expect(result[0].name).toBe("root");
    expect(result[0].count).toBe(3);
    expect(result[0].children).toHaveLength(1);
    expect(result[0].children![0].name).toBe("child1");
    expect(result[0].children![0].count).toBe(3);
  });

  it("prunes branches with no counts", async () => {
    const ontologyTerms = [
      { name: "root", label: "Root", parent: null },
      { name: "branchA", label: "Branch A", parent: { name: "root" } },
      { name: "branchB", label: "Branch B", parent: { name: "root" } },
      { name: "leafA1", label: "Leaf A1", parent: { name: "branchA" } },
    ];
    const groupByRows = [{ count: 5, disease: { name: "leafA1" } }];
    const fetcher = makeHierarchicalFetcher(ontologyTerms, groupByRows, {
      root: 5,
      branchA: 5,
      branchB: 0,
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

    expect(result).toHaveLength(1);
    expect(result[0].name).toBe("root");
    const children = result[0].children!;
    expect(children).toHaveLength(1);
    expect(children[0].name).toBe("branchA");
    expect(children[0].children![0].name).toBe("leafA1");
  });

  it("uses cache on second call — fetcher only called once for ontology tree", async () => {
    const ontologyTerms = [{ name: "termA", label: "Term A", parent: null }];
    const groupByRows = [{ count: 2, disease: { name: "termA" } }];
    const fetcher = makeHierarchicalFetcher(ontologyTerms, groupByRows, {});

    await fetchCounts(
      "mySchema",
      "Patient",
      "disease",
      "ONTOLOGY",
      {},
      fetcher,
      "OntologyTable",
      "mySchema"
    );
    await fetchCounts(
      "mySchema",
      "Patient",
      "disease",
      "ONTOLOGY",
      {},
      fetcher,
      "OntologyTable",
      "mySchema"
    );

    const ontologyFetchCalls = fetcher.mock.calls.filter(
      ([_schema, query]: [string, string]) => query.includes("OntologyTable {")
    );
    expect(ontologyFetchCalls).toHaveLength(1);
  });

  it("uses refSchemaId for ontology tree fetch", async () => {
    const ontologyTerms = [{ name: "termX", label: "Term X", parent: null }];
    const groupByRows = [{ count: 1, disease: { name: "termX" } }];
    const fetcher = makeHierarchicalFetcher(ontologyTerms, groupByRows, {});

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

    const ontologyFetchCall = fetcher.mock.calls.find(
      ([_schema, query]: [string, string]) => query.includes("OntologyTable {")
    );
    expect(ontologyFetchCall).toBeDefined();
    expect(ontologyFetchCall![0]).toBe("sharedSchema");
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

  it("applies cross-filter to _agg parent count queries", async () => {
    const ontologyTerms = [
      { name: "parent1", label: "Parent 1", parent: null },
      { name: "child1", label: "Child 1", parent: { name: "parent1" } },
    ];
    const groupByRows = [{ count: 2, disease: { name: "child1" } }];
    const crossFilter = { status: { equals: "active" } };
    const capturedAggQueries: string[] = [];

    const fetcher = vi
      .fn()
      .mockImplementation((_schemaId: string, query: string) => {
        if (query.includes("OntologyTable {")) {
          return Promise.resolve({ OntologyTable: ontologyTerms });
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
      "ONTOLOGY",
      crossFilter,
      fetcher,
      "OntologyTable",
      null
    );

    expect(capturedAggQueries).toHaveLength(1);
    expect(capturedAggQueries[0]).toContain("status");
    expect(capturedAggQueries[0]).toContain("_match_any_including_children");
  });

  it("returns empty array when ontology tree fetch fails", async () => {
    const fetcher = vi.fn().mockImplementation((_s: string, query: string) => {
      if (query.includes("OntologyTable {")) {
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
});
