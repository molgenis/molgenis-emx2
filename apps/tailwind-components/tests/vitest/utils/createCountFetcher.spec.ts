import { describe, it, expect, vi, beforeEach } from "vitest";
import { createCountFetcher } from "../../../app/utils/createCountFetcher";

vi.mock("../../../app/composables/fetchGraphql", () => ({
  default: vi.fn(),
}));

import fetchGraphql from "../../../app/composables/fetchGraphql";
const mockFetchGraphql = fetchGraphql as ReturnType<typeof vi.fn>;

describe("createCountFetcher", () => {
  beforeEach(() => {
    mockFetchGraphql.mockReset();
  });

  describe("fetchRefCounts", () => {
    it("calls _groupBy with cross-filter and returns counts by label", async () => {
      mockFetchGraphql.mockResolvedValue({
        Pet_groupBy: [
          { count: 3, bird: { name: "tweety" } },
          { count: 5, bird: { name: "daffy" } },
        ],
      });

      const fetcher = createCountFetcher({
        schemaId: "test-schema",
        tableId: "Pet",
        columnPath: "bird",
        getCrossFilter: () => ({ age: { between: [1, 10] } }),
      });

      const options = new Map<string, Record<string, unknown>>([
        ["tweety", { name: "tweety" }],
        ["daffy", { name: "daffy" }],
      ]);

      const counts = await fetcher.fetchRefCounts(options);

      expect(mockFetchGraphql).toHaveBeenCalledWith(
        "test-schema",
        expect.stringContaining("Pet_groupBy"),
        expect.objectContaining({
          filter: expect.objectContaining({
            age: { between: [1, 10] },
          }),
        })
      );
      expect(counts.get("tweety")).toBe(3);
      expect(counts.get("daffy")).toBe(5);
    });

    it("returns counts mapped by label using key from first option", async () => {
      mockFetchGraphql.mockResolvedValueOnce({
        Patient_groupBy: [
          { count: 5, species: { name: "Dog" } },
          { count: 3, species: { name: "Cat" } },
        ],
      });

      const fetcher = createCountFetcher({
        schemaId: "mySchema",
        tableId: "Patient",
        columnPath: "species",
        getCrossFilter: () => ({ _search: "test" }),
      });

      const options = new Map<string, Record<string, unknown>>([
        ["Dog", { name: "Dog" }],
        ["Cat", { name: "Cat" }],
      ]);

      const counts = await fetcher.fetchRefCounts(options);

      expect(mockFetchGraphql).toHaveBeenCalledTimes(1);
      const [schema, query, variables] = mockFetchGraphql.mock.calls[0];
      expect(schema).toBe("mySchema");
      expect(query).toContain("Patient_groupBy");
      expect(query).toContain("species { name }");
      expect(variables.filter.species.name.equals).toEqual(["Dog", "Cat"]);

      expect(counts.get("Dog")).toBe(5);
      expect(counts.get("Cat")).toBe(3);
    });

    it("returns empty map for nested dotted column path", async () => {
      const fetcher = createCountFetcher({
        schemaId: "s",
        tableId: "Patient",
        columnPath: "hospital.city",
        getCrossFilter: () => ({}),
      });

      const counts = await fetcher.fetchRefCounts(
        new Map([["Amsterdam", { name: "Amsterdam" }]])
      );

      expect(mockFetchGraphql).not.toHaveBeenCalled();
      expect(counts.size).toBe(0);
    });

    it("sets count to 0 for labels not in response", async () => {
      mockFetchGraphql.mockResolvedValueOnce({
        Patient_groupBy: [{ count: 5, species: { name: "Dog" } }],
      });

      const fetcher = createCountFetcher({
        schemaId: "s",
        tableId: "Patient",
        columnPath: "species",
        getCrossFilter: () => ({}),
      });

      const counts = await fetcher.fetchRefCounts(
        new Map([
          ["Dog", { name: "Dog" }],
          ["Cat", { name: "Cat" }],
        ])
      );

      expect(counts.get("Dog")).toBe(5);
      expect(counts.get("Cat")).toBe(0);
    });

    it("calls getCrossFilter at invocation time", async () => {
      let currentFilter = { _search: "first" };
      mockFetchGraphql.mockResolvedValue({ Patient_groupBy: [] });

      const fetcher = createCountFetcher({
        schemaId: "s",
        tableId: "Patient",
        columnPath: "species",
        getCrossFilter: () => currentFilter,
      });

      await fetcher.fetchRefCounts(new Map([["Dog", { name: "Dog" }]]));
      expect(mockFetchGraphql.mock.calls[0][2].filter._search).toBe("first");

      currentFilter = { _search: "second" };
      mockFetchGraphql.mockReset();
      mockFetchGraphql.mockResolvedValue({ Patient_groupBy: [] });

      await fetcher.fetchRefCounts(new Map([["Dog", { name: "Dog" }]]));
      expect(mockFetchGraphql.mock.calls[0][2].filter._search).toBe("second");
    });

    it("returns empty map on fetch error", async () => {
      const consoleWarnSpy = vi
        .spyOn(console, "warn")
        .mockImplementation(() => {});
      mockFetchGraphql.mockRejectedValueOnce(new Error("Network error"));

      const fetcher = createCountFetcher({
        schemaId: "s",
        tableId: "Patient",
        columnPath: "species",
        getCrossFilter: () => ({}),
      });

      const counts = await fetcher.fetchRefCounts(
        new Map([["Dog", { name: "Dog" }]])
      );

      expect(counts.size).toBe(0);
      expect(consoleWarnSpy).toHaveBeenCalled();
      consoleWarnSpy.mockRestore();
    });
  });

  describe("fetchOntologyLeafCounts", () => {
    it("returns counts by name using _groupBy query", async () => {
      mockFetchGraphql.mockResolvedValueOnce({
        Patient_groupBy: [
          { count: 7, species: { name: "Dog" } },
          { count: 2, species: { name: "Cat" } },
        ],
      });

      const fetcher = createCountFetcher({
        schemaId: "mySchema",
        tableId: "Patient",
        columnPath: "species",
        getCrossFilter: () => ({}),
      });

      const counts = await fetcher.fetchOntologyLeafCounts(["Dog", "Cat"]);

      const [, query, variables] = mockFetchGraphql.mock.calls[0];
      expect(query).toContain("Patient_groupBy");
      expect(query).toContain("species { name }");
      expect(variables.filter.species.name.equals).toEqual(["Dog", "Cat"]);

      expect(counts.get("Dog")).toBe(7);
      expect(counts.get("Cat")).toBe(2);
    });

    it("returns empty map for nested dotted column path", async () => {
      const fetcher = createCountFetcher({
        schemaId: "s",
        tableId: "Patient",
        columnPath: "hospital.species",
        getCrossFilter: () => ({}),
      });

      const counts = await fetcher.fetchOntologyLeafCounts(["Dog"]);

      expect(mockFetchGraphql).not.toHaveBeenCalled();
      expect(counts.size).toBe(0);
    });

    it("returns empty map for empty names array", async () => {
      const fetcher = createCountFetcher({
        schemaId: "s",
        tableId: "Patient",
        columnPath: "species",
        getCrossFilter: () => ({}),
      });

      const counts = await fetcher.fetchOntologyLeafCounts([]);
      expect(mockFetchGraphql).not.toHaveBeenCalled();
      expect(counts.size).toBe(0);
    });

    it("sets count to 0 for names not in response", async () => {
      mockFetchGraphql.mockResolvedValueOnce({
        Patient_groupBy: [{ count: 7, species: { name: "Dog" } }],
      });

      const fetcher = createCountFetcher({
        schemaId: "s",
        tableId: "Patient",
        columnPath: "species",
        getCrossFilter: () => ({}),
      });

      const counts = await fetcher.fetchOntologyLeafCounts(["Dog", "Cat"]);

      expect(counts.get("Dog")).toBe(7);
      expect(counts.get("Cat")).toBe(0);
    });

    it("returns empty map on fetch error", async () => {
      const consoleWarnSpy = vi
        .spyOn(console, "warn")
        .mockImplementation(() => {});
      mockFetchGraphql.mockRejectedValueOnce(new Error("Network error"));

      const fetcher = createCountFetcher({
        schemaId: "s",
        tableId: "Patient",
        columnPath: "species",
        getCrossFilter: () => ({}),
      });

      const counts = await fetcher.fetchOntologyLeafCounts(["Dog", "Cat"]);

      expect(counts.size).toBe(0);
      expect(consoleWarnSpy).toHaveBeenCalled();
      consoleWarnSpy.mockRestore();
    });
  });

  describe("fetchRefBaseCounts", () => {
    it("builds query without cross-filter in variables", async () => {
      mockFetchGraphql.mockResolvedValueOnce({
        Patient_groupBy: [
          { count: 5, species: { name: "Dog" } },
          { count: 3, species: { name: "Cat" } },
        ],
      });

      const fetcher = createCountFetcher({
        schemaId: "mySchema",
        tableId: "Patient",
        columnPath: "species",
        getCrossFilter: () => ({ _search: "cross-filter-value" }),
      });

      const options = new Map<string, Record<string, unknown>>([
        ["Dog", { name: "Dog" }],
        ["Cat", { name: "Cat" }],
      ]);

      await fetcher.fetchRefBaseCounts(options);

      expect(mockFetchGraphql).toHaveBeenCalledTimes(1);
      const [schema, query, variables] = mockFetchGraphql.mock.calls[0];
      expect(schema).toBe("mySchema");
      expect(query).toContain("Patient_groupBy");
      expect(variables.filter).not.toHaveProperty("_search");
    });

    it("returns counts map", async () => {
      mockFetchGraphql.mockResolvedValueOnce({
        Patient_groupBy: [
          { count: 5, species: { name: "Dog" } },
          { count: 3, species: { name: "Cat" } },
        ],
      });

      const fetcher = createCountFetcher({
        schemaId: "s",
        tableId: "Patient",
        columnPath: "species",
        getCrossFilter: () => ({ _search: "test" }),
      });

      const options = new Map<string, Record<string, unknown>>([
        ["Dog", { name: "Dog" }],
        ["Cat", { name: "Cat" }],
      ]);

      const counts = await fetcher.fetchRefBaseCounts(options);

      expect(counts.get("Dog")).toBe(5);
      expect(counts.get("Cat")).toBe(3);
    });

    it("returns empty map on fetch error", async () => {
      const consoleWarnSpy = vi
        .spyOn(console, "warn")
        .mockImplementation(() => {});
      mockFetchGraphql.mockRejectedValueOnce(new Error("Network error"));

      const fetcher = createCountFetcher({
        schemaId: "s",
        tableId: "Patient",
        columnPath: "species",
        getCrossFilter: () => ({}),
      });

      const counts = await fetcher.fetchRefBaseCounts(
        new Map([["Dog", { name: "Dog" }]])
      );

      expect(counts.size).toBe(0);
      expect(consoleWarnSpy).toHaveBeenCalled();
      consoleWarnSpy.mockRestore();
    });

    it("returns empty map for nested dotted column path", async () => {
      const fetcher = createCountFetcher({
        schemaId: "s",
        tableId: "Patient",
        columnPath: "hospital.species",
        getCrossFilter: () => ({}),
      });

      const counts = await fetcher.fetchRefBaseCounts(
        new Map([["Amsterdam", { name: "Amsterdam" }]])
      );

      expect(mockFetchGraphql).not.toHaveBeenCalled();
      expect(counts.size).toBe(0);
    });
  });

  describe("fetchOntologyLeafBaseCounts", () => {
    it("builds query without cross-filter in variables", async () => {
      mockFetchGraphql.mockResolvedValueOnce({
        Patient_groupBy: [
          { count: 7, species: { name: "Dog" } },
          { count: 2, species: { name: "Cat" } },
        ],
      });

      const fetcher = createCountFetcher({
        schemaId: "mySchema",
        tableId: "Patient",
        columnPath: "species",
        getCrossFilter: () => ({ _search: "cross-filter-value" }),
      });

      await fetcher.fetchOntologyLeafBaseCounts(["Dog", "Cat"]);

      expect(mockFetchGraphql).toHaveBeenCalledTimes(1);
      const [schema, , variables] = mockFetchGraphql.mock.calls[0];
      expect(schema).toBe("mySchema");
      expect(variables.filter).not.toHaveProperty("_search");
    });

    it("returns counts map", async () => {
      mockFetchGraphql.mockResolvedValueOnce({
        Patient_groupBy: [
          { count: 7, species: { name: "Dog" } },
          { count: 2, species: { name: "Cat" } },
        ],
      });

      const fetcher = createCountFetcher({
        schemaId: "s",
        tableId: "Patient",
        columnPath: "species",
        getCrossFilter: () => ({ _search: "test" }),
      });

      const counts = await fetcher.fetchOntologyLeafBaseCounts(["Dog", "Cat"]);

      expect(counts.get("Dog")).toBe(7);
      expect(counts.get("Cat")).toBe(2);
    });

    it("returns empty map for empty names array", async () => {
      const fetcher = createCountFetcher({
        schemaId: "s",
        tableId: "Patient",
        columnPath: "species",
        getCrossFilter: () => ({}),
      });

      const counts = await fetcher.fetchOntologyLeafBaseCounts([]);
      expect(mockFetchGraphql).not.toHaveBeenCalled();
      expect(counts.size).toBe(0);
    });

    it("returns empty map on fetch error", async () => {
      const consoleWarnSpy = vi
        .spyOn(console, "warn")
        .mockImplementation(() => {});
      mockFetchGraphql.mockRejectedValueOnce(new Error("Network error"));

      const fetcher = createCountFetcher({
        schemaId: "s",
        tableId: "Patient",
        columnPath: "species",
        getCrossFilter: () => ({}),
      });

      const counts = await fetcher.fetchOntologyLeafBaseCounts(["Dog", "Cat"]);

      expect(counts.size).toBe(0);
      expect(consoleWarnSpy).toHaveBeenCalled();
      consoleWarnSpy.mockRestore();
    });
  });

  describe("fetchOntologyParentBaseCounts", () => {
    it("returns counts by name using aliased _agg queries without cross-filter", async () => {
      mockFetchGraphql.mockResolvedValueOnce({
        c_Animal: { count: 10 },
        c_Plant: { count: 5 },
      });

      const fetcher = createCountFetcher({
        schemaId: "mySchema",
        tableId: "Patient",
        columnPath: "species",
        getCrossFilter: () => ({ _search: "cross-filter-value" }),
      });

      const counts = await fetcher.fetchOntologyParentBaseCounts([
        "Animal",
        "Plant",
      ]);

      const [schema, query, variables] = mockFetchGraphql.mock.calls[0];
      expect(schema).toBe("mySchema");
      expect(query).toContain("Patient_agg");
      expect(query).toContain("c_Animal");
      expect(query).toContain("c_Plant");
      expect(
        variables.filter_c_Animal.species._match_any_including_children
      ).toBe("Animal");
      expect(
        variables.filter_c_Plant.species._match_any_including_children
      ).toBe("Plant");
      expect(variables.filter_c_Animal).not.toHaveProperty("_search");

      expect(counts.get("Animal")).toBe(10);
      expect(counts.get("Plant")).toBe(5);
    });

    it("returns empty map for empty names array", async () => {
      const fetcher = createCountFetcher({
        schemaId: "s",
        tableId: "Patient",
        columnPath: "species",
        getCrossFilter: () => ({}),
      });

      const counts = await fetcher.fetchOntologyParentBaseCounts([]);
      expect(mockFetchGraphql).not.toHaveBeenCalled();
      expect(counts.size).toBe(0);
    });

    it("returns empty map on fetch error", async () => {
      const consoleWarnSpy = vi
        .spyOn(console, "warn")
        .mockImplementation(() => {});
      mockFetchGraphql.mockRejectedValueOnce(new Error("Network error"));

      const fetcher = createCountFetcher({
        schemaId: "s",
        tableId: "Patient",
        columnPath: "species",
        getCrossFilter: () => ({}),
      });

      const counts = await fetcher.fetchOntologyParentBaseCounts(["Animal"]);

      expect(counts.size).toBe(0);
      expect(consoleWarnSpy).toHaveBeenCalled();
      consoleWarnSpy.mockRestore();
    });
  });

  describe("fetchOntologyParentCounts", () => {
    it("returns counts by name using aliased _agg queries with _match_any_including_children", async () => {
      mockFetchGraphql.mockResolvedValueOnce({
        c_Animal: { count: 10 },
        c_Plant: { count: 5 },
      });

      const fetcher = createCountFetcher({
        schemaId: "mySchema",
        tableId: "Patient",
        columnPath: "species",
        getCrossFilter: () => ({ _search: "test" }),
      });

      const counts = await fetcher.fetchOntologyParentCounts([
        "Animal",
        "Plant",
      ]);

      const [schema, query, variables] = mockFetchGraphql.mock.calls[0];
      expect(schema).toBe("mySchema");
      expect(query).toContain("Patient_agg");
      expect(query).toContain("c_Animal");
      expect(query).toContain("c_Plant");
      expect(
        variables.filter_c_Animal.species._match_any_including_children
      ).toBe("Animal");
      expect(
        variables.filter_c_Plant.species._match_any_including_children
      ).toBe("Plant");

      expect(counts.get("Animal")).toBe(10);
      expect(counts.get("Plant")).toBe(5);
    });

    it("returns empty map on fetch error", async () => {
      const consoleWarnSpy = vi
        .spyOn(console, "warn")
        .mockImplementation(() => {});
      mockFetchGraphql.mockRejectedValueOnce(new Error("Network error"));

      const fetcher = createCountFetcher({
        schemaId: "s",
        tableId: "Patient",
        columnPath: "species",
        getCrossFilter: () => ({}),
      });

      const counts = await fetcher.fetchOntologyParentCounts(["Animal"]);

      expect(counts.size).toBe(0);
      expect(consoleWarnSpy).toHaveBeenCalled();
      consoleWarnSpy.mockRestore();
    });
  });
});
