import { describe, it, expect, vi, beforeEach } from "vitest";
import { ref, nextTick } from "vue";
import { useFilterCounts } from "../../../app/composables/useFilterCounts";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";

vi.mock("../../../app/composables/fetchGraphql", () => ({
  default: vi.fn(),
}));

vi.mock("@vueuse/core", () => ({
  useDebounceFn: (fn: Function) => fn,
}));

vi.mock("#imports", () => ({
  createError: (err: any) => err,
}));

vi.mock("#app/composables/router", () => ({
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ replace: vi.fn() }),
}));

import fetchGraphql from "../../../app/composables/fetchGraphql";

const mockFetchGraphql = fetchGraphql as ReturnType<typeof vi.fn>;

const flushPromises = () =>
  new Promise<void>((resolve) => setTimeout(resolve, 0));

function createMockColumns(): IColumn[] {
  return [
    { id: "species", columnType: "ONTOLOGY", refTableId: "Species" } as IColumn,
    {
      id: "breed",
      columnType: "ONTOLOGY_ARRAY",
      refTableId: "Breed",
    } as IColumn,
    { id: "name", columnType: "STRING" } as IColumn,
  ];
}

describe("useFilterCounts", () => {
  beforeEach(() => {
    mockFetchGraphql.mockReset();
    vi.clearAllMocks();
  });

  it("fetches leaf counts for visible ontology columns", async () => {
    const schemaId = ref("test");
    const tableId = ref("Dog");
    const filterStates = ref(new Map<string, IFilterValue>());
    const columns = ref(createMockColumns());
    const visibleFilterIds = ref(["species", "breed"]);
    const searchValue = ref("");

    mockFetchGraphql.mockResolvedValueOnce({
      Dog_groupBy: [
        { count: 5, species: { name: "Canine" } },
        { count: 3, species: { name: "Feline" } },
      ],
    });

    mockFetchGraphql.mockResolvedValueOnce({
      Dog_groupBy: [
        { count: 2, breed: { name: "Labrador" } },
        { count: 1, breed: { name: "Poodle" } },
      ],
    });

    const { facetCounts, isLoading } = useFilterCounts({
      schemaId,
      tableId,
      filterStates,
      columns,
      visibleFilterIds,
      searchValue,
    });

    await nextTick();
    await flushPromises();

    expect(isLoading.value).toBe(false);
    expect(facetCounts.value.size).toBe(2);

    const speciesCounts = facetCounts.value.get("species");
    expect(speciesCounts?.get("Canine")).toBe(5);
    expect(speciesCounts?.get("Feline")).toBe(3);

    const breedCounts = facetCounts.value.get("breed");
    expect(breedCounts?.get("Labrador")).toBe(2);
    expect(breedCounts?.get("Poodle")).toBe(1);
  });

  it("excludes current column from cross-filter", async () => {
    const schemaId = ref("test");
    const tableId = ref("Dog");
    const filterStates = ref(
      new Map<string, IFilterValue>([
        ["species", { operator: "equals", value: [{ name: "Canine" }] }],
        ["breed", { operator: "equals", value: [{ name: "Labrador" }] }],
      ])
    );
    const columns = ref(createMockColumns());
    const visibleFilterIds = ref(["species", "breed"]);
    const searchValue = ref("");

    mockFetchGraphql.mockResolvedValue({ Dog_groupBy: [] });

    useFilterCounts({
      schemaId,
      tableId,
      filterStates,
      columns,
      visibleFilterIds,
      searchValue,
    });

    await nextTick();
    await flushPromises();

    expect(mockFetchGraphql).toHaveBeenCalledTimes(2);

    const firstCall = mockFetchGraphql.mock.calls[0];
    const firstVariables = firstCall[2];
    expect(firstVariables.filter.species).toBeUndefined();
    expect(firstVariables.filter.breed).toBeDefined();

    const secondCall = mockFetchGraphql.mock.calls[1];
    const secondVariables = secondCall[2];
    expect(secondVariables.filter.breed).toBeUndefined();
    expect(secondVariables.filter.species).toBeDefined();
  });

  it("does not fetch counts for non-ontology columns", async () => {
    const schemaId = ref("test");
    const tableId = ref("Dog");
    const filterStates = ref(new Map<string, IFilterValue>());
    const columns = ref(createMockColumns());
    const visibleFilterIds = ref(["name"]);
    const searchValue = ref("");

    const { facetCounts } = useFilterCounts({
      schemaId,
      tableId,
      filterStates,
      columns,
      visibleFilterIds,
      searchValue,
    });

    await nextTick();
    await flushPromises();

    expect(mockFetchGraphql).not.toHaveBeenCalled();
    expect(facetCounts.value.size).toBe(0);
  });

  it("handles empty groupBy results", async () => {
    const schemaId = ref("test");
    const tableId = ref("Dog");
    const filterStates = ref(new Map<string, IFilterValue>());
    const columns = ref(createMockColumns());
    const visibleFilterIds = ref(["species"]);
    const searchValue = ref("");

    mockFetchGraphql.mockResolvedValueOnce({ Dog_groupBy: [] });

    const { facetCounts } = useFilterCounts({
      schemaId,
      tableId,
      filterStates,
      columns,
      visibleFilterIds,
      searchValue,
    });

    await nextTick();
    await flushPromises();

    const speciesCounts = facetCounts.value.get("species");
    expect(speciesCounts).toBeDefined();
    expect(speciesCounts?.size).toBe(0);
  });

  it("handles fetch errors gracefully", async () => {
    const schemaId = ref("test");
    const tableId = ref("Dog");
    const filterStates = ref(new Map<string, IFilterValue>());
    const columns = ref(createMockColumns());
    const visibleFilterIds = ref(["species"]);
    const searchValue = ref("");

    const consoleWarnSpy = vi
      .spyOn(console, "warn")
      .mockImplementation(() => {});
    mockFetchGraphql.mockRejectedValueOnce(new Error("Network error"));

    const { facetCounts, isLoading } = useFilterCounts({
      schemaId,
      tableId,
      filterStates,
      columns,
      visibleFilterIds,
      searchValue,
    });

    await nextTick();
    await flushPromises();

    expect(isLoading.value).toBe(false);
    expect(consoleWarnSpy).toHaveBeenCalled();
    const speciesCounts = facetCounts.value.get("species");
    expect(speciesCounts).toBeDefined();
    expect(speciesCounts?.size).toBe(0);

    consoleWarnSpy.mockRestore();
  });

  it("fetches parent counts with _match_any_including_children", async () => {
    const schemaId = ref("test");
    const tableId = ref("Dog");
    const filterStates = ref(new Map<string, IFilterValue>());
    const columns = ref(createMockColumns());
    const visibleFilterIds = ref([]);
    const searchValue = ref("");

    mockFetchGraphql.mockResolvedValueOnce({
      c_Mammal: { count: 10 },
      c_Animal: { count: 15 },
    });

    const { fetchParentCounts } = useFilterCounts({
      schemaId,
      tableId,
      filterStates,
      columns,
      visibleFilterIds,
      searchValue,
    });

    await nextTick();
    await flushPromises();

    const result = await fetchParentCounts("species", ["Mammal", "Animal"]);

    expect(mockFetchGraphql).toHaveBeenCalledTimes(1);
    const call = mockFetchGraphql.mock.calls[0];
    const query = call[1] as string;
    const variables = call[2] as Record<string, any>;

    expect(query).toContain("c_Mammal");
    expect(query).toContain("c_Animal");
    expect(query).toContain("Dog_agg");

    expect(
      variables.filter_c_Mammal.species._match_any_including_children
    ).toBe("Mammal");
    expect(
      variables.filter_c_Animal.species._match_any_including_children
    ).toBe("Animal");

    expect(result.get("Mammal")).toBe(10);
    expect(result.get("Animal")).toBe(15);
  });

  it("caches parent counts", async () => {
    const schemaId = ref("test");
    const tableId = ref("Dog");
    const filterStates = ref(new Map<string, IFilterValue>());
    const columns = ref(createMockColumns());
    const visibleFilterIds = ref([]);
    const searchValue = ref("");

    mockFetchGraphql.mockResolvedValue({
      c_Mammal: { count: 10 },
      c_Animal: { count: 15 },
    });

    const { fetchParentCounts } = useFilterCounts({
      schemaId,
      tableId,
      filterStates,
      columns,
      visibleFilterIds,
      searchValue,
    });

    await nextTick();
    await flushPromises();

    await fetchParentCounts("species", ["Mammal", "Animal"]);
    expect(mockFetchGraphql).toHaveBeenCalledTimes(1);

    const cachedResult = await fetchParentCounts("species", [
      "Mammal",
      "Animal",
    ]);
    expect(mockFetchGraphql).toHaveBeenCalledTimes(1);

    expect(cachedResult.get("Mammal")).toBe(10);
    expect(cachedResult.get("Animal")).toBe(15);
  });

  it("invalidates parent count cache when cross-filter changes", async () => {
    const schemaId = ref("test");
    const tableId = ref("Dog");
    const filterStates = ref(new Map<string, IFilterValue>());
    const columns = ref(createMockColumns());
    const visibleFilterIds = ref([]);
    const searchValue = ref("");

    mockFetchGraphql.mockResolvedValue({
      c_Mammal: { count: 10 },
    });

    const { fetchParentCounts } = useFilterCounts({
      schemaId,
      tableId,
      filterStates,
      columns,
      visibleFilterIds,
      searchValue,
    });

    await nextTick();
    await flushPromises();

    await fetchParentCounts("species", ["Mammal"]);
    expect(mockFetchGraphql).toHaveBeenCalledTimes(1);

    filterStates.value = new Map([
      ["breed", { operator: "equals", value: [{ name: "Labrador" }] }],
    ]);
    await nextTick();
    await flushPromises();

    await fetchParentCounts("species", ["Mammal"]);
    expect(mockFetchGraphql).toHaveBeenCalledTimes(2);
  });

  it("returns empty map for empty parentNames", async () => {
    const schemaId = ref("test");
    const tableId = ref("Dog");
    const filterStates = ref(new Map<string, IFilterValue>());
    const columns = ref(createMockColumns());
    const visibleFilterIds = ref([]);
    const searchValue = ref("");

    const { fetchParentCounts } = useFilterCounts({
      schemaId,
      tableId,
      filterStates,
      columns,
      visibleFilterIds,
      searchValue,
    });

    await nextTick();
    await flushPromises();

    const result = await fetchParentCounts("species", []);

    expect(mockFetchGraphql).not.toHaveBeenCalled();
    expect(result.size).toBe(0);
  });

  it("handles parent count fetch errors gracefully", async () => {
    const schemaId = ref("test");
    const tableId = ref("Dog");
    const filterStates = ref(new Map<string, IFilterValue>());
    const columns = ref(createMockColumns());
    const visibleFilterIds = ref([]);
    const searchValue = ref("");

    const consoleWarnSpy = vi
      .spyOn(console, "warn")
      .mockImplementation(() => {});
    mockFetchGraphql.mockRejectedValueOnce(new Error("Network error"));

    const { fetchParentCounts } = useFilterCounts({
      schemaId,
      tableId,
      filterStates,
      columns,
      visibleFilterIds,
      searchValue,
    });

    await nextTick();
    await flushPromises();

    const result = await fetchParentCounts("species", ["Mammal"]);

    expect(consoleWarnSpy).toHaveBeenCalled();
    expect(result.size).toBe(0);

    consoleWarnSpy.mockRestore();
  });

  it("sanitizes parent names in query aliases", async () => {
    const schemaId = ref("test");
    const tableId = ref("Dog");
    const filterStates = ref(new Map<string, IFilterValue>());
    const columns = ref(createMockColumns());
    const visibleFilterIds = ref([]);
    const searchValue = ref("");

    mockFetchGraphql.mockResolvedValueOnce({
      c_My_Special_Term: { count: 5 },
    });

    const { fetchParentCounts } = useFilterCounts({
      schemaId,
      tableId,
      filterStates,
      columns,
      visibleFilterIds,
      searchValue,
    });

    await nextTick();
    await flushPromises();

    await fetchParentCounts("species", ["My-Special Term!"]);

    const call = mockFetchGraphql.mock.calls[0];
    const query = call[1] as string;

    expect(query).toContain("c_My_Special_Term");
    expect(query).not.toContain("My-Special Term!");
  });

  it("includes searchValue in cross-filter", async () => {
    const schemaId = ref("test");
    const tableId = ref("Dog");
    const filterStates = ref(new Map<string, IFilterValue>());
    const columns = ref(createMockColumns());
    const visibleFilterIds = ref(["species"]);
    const searchValue = ref("labrador");

    mockFetchGraphql.mockResolvedValueOnce({ Dog_groupBy: [] });

    useFilterCounts({
      schemaId,
      tableId,
      filterStates,
      columns,
      visibleFilterIds,
      searchValue,
    });

    await nextTick();
    await flushPromises();

    expect(mockFetchGraphql).toHaveBeenCalledTimes(1);
    const call = mockFetchGraphql.mock.calls[0];
    const variables = call[2] as Record<string, any>;

    expect(variables.filter._search).toBe("labrador");
  });

  it("clears parent count cache when fetchLeafCounts is called", async () => {
    const schemaId = ref("test");
    const tableId = ref("Dog");
    const filterStates = ref(new Map<string, IFilterValue>());
    const columns = ref(createMockColumns());
    const visibleFilterIds = ref(["species"]);
    const searchValue = ref("");

    mockFetchGraphql.mockResolvedValue({
      Dog_groupBy: [],
      c_Mammal: { count: 10 },
    });

    const { fetchParentCounts } = useFilterCounts({
      schemaId,
      tableId,
      filterStates,
      columns,
      visibleFilterIds,
      searchValue,
    });

    await nextTick();
    await flushPromises();

    await fetchParentCounts("species", ["Mammal"]);
    expect(mockFetchGraphql).toHaveBeenCalledTimes(2);

    visibleFilterIds.value = ["species", "breed"];
    await nextTick();
    await flushPromises();

    await fetchParentCounts("species", ["Mammal"]);
    expect(mockFetchGraphql).toHaveBeenCalledTimes(5);
  });

  it("handles groupBy results with missing or null term names", async () => {
    const schemaId = ref("test");
    const tableId = ref("Dog");
    const filterStates = ref(new Map<string, IFilterValue>());
    const columns = ref(createMockColumns());
    const visibleFilterIds = ref(["species"]);
    const searchValue = ref("");

    mockFetchGraphql.mockResolvedValueOnce({
      Dog_groupBy: [
        { count: 5, species: { name: "Canine" } },
        { count: 3, species: null },
        { count: 2, species: {} },
        { count: 1 },
      ],
    });

    const { facetCounts } = useFilterCounts({
      schemaId,
      tableId,
      filterStates,
      columns,
      visibleFilterIds,
      searchValue,
    });

    await nextTick();
    await flushPromises();

    const speciesCounts = facetCounts.value.get("species");
    expect(speciesCounts?.size).toBe(1);
    expect(speciesCounts?.get("Canine")).toBe(5);
  });
});
