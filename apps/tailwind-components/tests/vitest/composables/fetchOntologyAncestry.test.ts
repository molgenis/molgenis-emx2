import { describe, it, expect, vi, beforeEach } from "vitest";
import type { IColumn } from "../../../../metadata-utils/src/types";

vi.mock("../../../app/composables/fetchGraphql", () => ({
  default: vi.fn(),
}));

beforeEach(() => {
  vi.resetModules();
  vi.clearAllMocks();
});

async function loadModule() {
  const { default: fetchOntologyAncestry, withOntologyAncestry } = await import(
    "../../../app/composables/fetchOntologyAncestry"
  );
  const { default: fetchGraphql } = await import(
    "../../../app/composables/fetchGraphql"
  );
  return {
    fetchOntologyAncestry,
    withOntologyAncestry,
    fetchGraphql: vi.mocked(fetchGraphql),
  };
}

describe("withOntologyAncestry", () => {
  it("gives each ontology term on the row its ancestors, keeping single and array shapes", async () => {
    const { withOntologyAncestry, fetchGraphql } = await loadModule();
    fetchGraphql.mockResolvedValueOnce({
      Diagnoses: [
        { name: "Cardiology", parent: { name: "Medicine" } },
        { name: "Medicine", parent: null },
      ],
    });
    const columns: IColumn[] = [
      {
        id: "diagnosis",
        label: "Diagnosis",
        columnType: "ONTOLOGY",
        refSchemaId: "OntologySchema",
        refTableId: "Diagnoses",
      },
      {
        id: "diagnoses",
        label: "Diagnoses",
        columnType: "ONTOLOGY_ARRAY",
        refSchemaId: "OntologySchema",
        refTableId: "Diagnoses",
      },
    ];

    const row = await withOntologyAncestry(
      {
        diagnosis: { name: "Cardiology" },
        diagnoses: [{ name: "Cardiology" }],
      },
      columns
    );

    expect(row.diagnosis).toMatchObject({
      name: "Cardiology",
      parent: { name: "Medicine" },
    });
    expect(row.diagnoses).toMatchObject([
      { name: "Cardiology", parent: { name: "Medicine" } },
    ]);
  });

  it("leaves a column with no ref table as it is, and fetches nothing", async () => {
    const { withOntologyAncestry, fetchGraphql } = await loadModule();
    const columns: IColumn[] = [
      { id: "diagnosis", label: "Diagnosis", columnType: "ONTOLOGY" },
    ];

    const row = await withOntologyAncestry(
      { diagnosis: { name: "Cardiology" } },
      columns
    );

    expect(row.diagnosis).toEqual({ name: "Cardiology" });
    expect(fetchGraphql).not.toHaveBeenCalled();
  });
});

describe("fetchOntologyAncestry", () => {
  it("requests only names its map does not already hold", async () => {
    const { fetchOntologyAncestry, fetchGraphql } = await loadModule();
    fetchGraphql.mockResolvedValueOnce({
      OntologyTable: [{ name: "A" }, { name: "B" }],
    });
    await fetchOntologyAncestry("schema", "OntologyTable", ["A", "B"]);

    fetchGraphql.mockResolvedValueOnce({ OntologyTable: [{ name: "C" }] });
    await fetchOntologyAncestry("schema", "OntologyTable", ["A", "C"]);

    expect(fetchGraphql).toHaveBeenCalledTimes(2);
    const secondCallVariables = fetchGraphql.mock.calls[1][2];
    expect(secondCallVariables.filter._match_any_including_parents).toEqual([
      "C",
    ]);
  });

  it("produces one request for two overlapping calls for the same names", async () => {
    const { fetchOntologyAncestry, fetchGraphql } = await loadModule();
    let resolveFetch: (value: any) => void;
    fetchGraphql.mockReturnValueOnce(
      new Promise((resolve) => {
        resolveFetch = resolve;
      })
    );

    const first = fetchOntologyAncestry("schema", "OntologyTable", ["A", "B"]);
    const second = fetchOntologyAncestry("schema", "OntologyTable", ["A", "B"]);

    resolveFetch!({ OntologyTable: [{ name: "A" }, { name: "B" }] });
    await Promise.all([first, second]);

    expect(fetchGraphql).toHaveBeenCalledTimes(1);
  });

  it("keys two tables separately", async () => {
    const { fetchOntologyAncestry, fetchGraphql } = await loadModule();
    fetchGraphql.mockResolvedValue({ TableA: [{ name: "A" }] });

    await fetchOntologyAncestry("schema", "TableA", ["A"]);
    await fetchOntologyAncestry("schema", "TableB", ["A"]);

    expect(fetchGraphql).toHaveBeenCalledTimes(2);
  });
});
