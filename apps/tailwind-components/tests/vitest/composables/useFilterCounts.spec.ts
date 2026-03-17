import { describe, it, expect, vi, beforeEach } from "vitest";
import { ref } from "vue";
import { useFilterCounts } from "../../../app/composables/useFilterCounts";

vi.mock("../../../app/composables/fetchGraphql", () => ({
  default: vi.fn(),
}));

vi.mock("@vueuse/core", () => ({
  useDebounceFn: (fn: Function) => fn,
}));

import fetchGraphql from "../../../app/composables/fetchGraphql";
const mockFetchGraphql = fetchGraphql as ReturnType<typeof vi.fn>;

describe("useFilterCounts", () => {
  beforeEach(() => {
    mockFetchGraphql.mockReset();
  });

  describe("fetchCounts", () => {
    it("builds correct _groupBy query for simple column path", async () => {
      mockFetchGraphql.mockResolvedValueOnce({
        Patient_groupBy: [
          { count: 5, species: { name: "Dog" } },
          { count: 3, species: { name: "Cat" } },
        ],
      });

      const { facetCounts, fetchCounts } = useFilterCounts({
        crossFilter: ref({ _search: "test" }),
        schemaId: ref("mySchema"),
        tableId: ref("Patient"),
        columnPath: ref("species"),
        keyField: ref("name"),
      });

      await fetchCounts(["Dog", "Cat"]);

      expect(mockFetchGraphql).toHaveBeenCalledTimes(1);
      const [schema, query, variables] = mockFetchGraphql.mock.calls[0];
      expect(schema).toBe("mySchema");
      expect(query).toContain("Patient_groupBy");
      expect(query).toContain("species { name }");
      expect(variables.filter.species.name.equals).toEqual(["Dog", "Cat"]);

      expect(facetCounts.value.get("Dog")).toBe(5);
      expect(facetCounts.value.get("Cat")).toBe(3);
    });

    it("skips fetch for nested dotted column paths", async () => {
      const { facetCounts, fetchCounts } = useFilterCounts({
        crossFilter: ref({}),
        schemaId: ref("mySchema"),
        tableId: ref("Patient"),
        columnPath: ref("hospital.city"),
        keyField: ref("name"),
      });

      await fetchCounts(["Amsterdam", "Rotterdam"]);

      expect(mockFetchGraphql).not.toHaveBeenCalled();
      expect(facetCounts.value.size).toBe(0);
    });

    it("sets count to 0 for names not in response", async () => {
      mockFetchGraphql.mockResolvedValueOnce({
        Patient_groupBy: [
          { count: 5, species: { name: "Dog" } },
        ],
      });

      const { facetCounts, fetchCounts } = useFilterCounts({
        crossFilter: ref({}),
        schemaId: ref("s"),
        tableId: ref("Patient"),
        columnPath: ref("species"),
        keyField: ref("name"),
      });

      await fetchCounts(["Dog", "Cat"]);

      expect(facetCounts.value.get("Dog")).toBe(5);
      expect(facetCounts.value.get("Cat")).toBe(0);
    });

    it("does nothing when crossFilter is undefined", async () => {
      const { fetchCounts } = useFilterCounts({
        crossFilter: ref(undefined),
        schemaId: ref("s"),
        tableId: ref("Patient"),
        columnPath: ref("species"),
        keyField: ref("name"),
      });

      await fetchCounts(["Dog"]);
      expect(mockFetchGraphql).not.toHaveBeenCalled();
    });

    it("handles fetch errors gracefully", async () => {
      const consoleWarnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});
      mockFetchGraphql.mockRejectedValueOnce(new Error("Network error"));

      const { facetCounts, fetchCounts } = useFilterCounts({
        crossFilter: ref({}),
        schemaId: ref("s"),
        tableId: ref("Patient"),
        columnPath: ref("species"),
        keyField: ref("name"),
      });

      await fetchCounts(["Dog"]);
      expect(consoleWarnSpy).toHaveBeenCalled();
      consoleWarnSpy.mockRestore();
    });

    it("uses custom keyField from refLabel", async () => {
      mockFetchGraphql.mockResolvedValueOnce({
        Patient_groupBy: [
          { count: 3, category: { code: "A1" } },
        ],
      });

      const { facetCounts, fetchCounts } = useFilterCounts({
        crossFilter: ref({}),
        schemaId: ref("s"),
        tableId: ref("Patient"),
        columnPath: ref("category"),
        keyField: ref("code"),
      });

      await fetchCounts(["A1"]);

      const [, query, variables] = mockFetchGraphql.mock.calls[0];
      expect(query).toContain("category { code }");
      expect(variables.filter.category.code.equals).toEqual(["A1"]);
      expect(facetCounts.value.get("A1")).toBe(3);
    });
  });

  describe("fetchParentCounts", () => {
    it("builds correct _agg queries with _match_any_including_children", async () => {
      mockFetchGraphql.mockResolvedValueOnce({
        c_Animal: { count: 10 },
        c_Plant: { count: 5 },
      });

      const { facetCounts, fetchParentCounts } = useFilterCounts({
        crossFilter: ref({ _search: "test" }),
        schemaId: ref("mySchema"),
        tableId: ref("Patient"),
        columnPath: ref("species"),
        keyField: ref("name"),
      });

      await fetchParentCounts(["Animal", "Plant"]);

      const [schema, query, variables] = mockFetchGraphql.mock.calls[0];
      expect(schema).toBe("mySchema");
      expect(query).toContain("Patient_agg");
      expect(query).toContain("c_Animal");
      expect(query).toContain("c_Plant");
      expect(variables.filter_c_Animal.species._match_any_including_children).toBe("Animal");
      expect(variables.filter_c_Plant.species._match_any_including_children).toBe("Plant");

      expect(facetCounts.value.get("Animal")).toBe(10);
      expect(facetCounts.value.get("Plant")).toBe(5);
    });

    it("skips fetch for nested dotted column paths", async () => {
      const { facetCounts, fetchParentCounts } = useFilterCounts({
        crossFilter: ref({}),
        schemaId: ref("s"),
        tableId: ref("Patient"),
        columnPath: ref("hospital.species"),
        keyField: ref("name"),
      });

      await fetchParentCounts(["Mammal"]);

      expect(mockFetchGraphql).not.toHaveBeenCalled();
      expect(facetCounts.value.size).toBe(0);
    });

    it("does nothing when crossFilter is undefined", async () => {
      const { fetchParentCounts } = useFilterCounts({
        crossFilter: ref(undefined),
        schemaId: ref("s"),
        tableId: ref("Patient"),
        columnPath: ref("species"),
        keyField: ref("name"),
      });

      await fetchParentCounts(["Animal"]);
      expect(mockFetchGraphql).not.toHaveBeenCalled();
    });
  });
});
