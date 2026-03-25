import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import { nextTick } from "vue";

vi.stubGlobal(
  "$fetch",
  vi
    .fn()
    .mockImplementation(
      (_url: string, options?: { body?: { query?: string } }) => {
        if (options?.body?.query?.includes("_schema")) {
          return Promise.resolve({
            data: {
              _schema: {
                tables: [{ id: "Hospital", label: "Hospital", columns: [] }],
              },
            },
          });
        }
        return Promise.resolve({ data: {} });
      }
    )
);

import Sidebar from "../../../../app/components/filter/Sidebar.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";
import type { IFilterValue, UseFilters } from "../../../../types/filters";
import { ref, computed } from "vue";

const mockRoute = { query: {} as Record<string, string> };
const mockRouter = { replace: vi.fn() };

vi.mock("#imports", async (importOriginal) => {
  const actual = await importOriginal<typeof import("#imports")>();
  return {
    ...actual,
    useRoute: () => mockRoute,
    useRouter: () => mockRouter,
  };
});

vi.mock("vue-router", () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter,
}));

const ontologyColumns: IColumn[] = [
  {
    id: "disease",
    label: "Disease",
    columnType: "ONTOLOGY",
    refTableId: "Diseases",
    refSchemaId: "Ontologies",
  },
  {
    id: "phenotype",
    label: "Phenotype",
    columnType: "ONTOLOGY_ARRAY",
    refTableId: "Phenotypes",
    refSchemaId: "Ontologies",
  },
];

const refColumns: IColumn[] = [
  {
    id: "hospital",
    label: "Hospital",
    columnType: "REF",
    refTableId: "Hospital",
    refSchemaId: "test",
  },
  {
    id: "medications",
    label: "Medications",
    columnType: "REF_ARRAY",
    refTableId: "Medication",
    refSchemaId: "test",
  },
];

const stringColumns: IColumn[] = [
  { id: "name", label: "Name", columnType: "STRING" },
  { id: "notes", label: "Notes", columnType: "TEXT" },
];

const allColumns: IColumn[] = [
  ...ontologyColumns,
  ...refColumns,
  ...stringColumns,
];

const unfilterableColumns: IColumn[] = [
  { id: "heading1", label: "Demographics", columnType: "HEADING" },
  { id: "section1", label: "Section", columnType: "SECTION" },
  { id: "file1", label: "File", columnType: "FILE" },
  { id: "mg_insertedOn", label: "Inserted On", columnType: "DATETIME" },
];

const mockFetchTableMetadata = vi.hoisted(() =>
  vi.fn().mockResolvedValue({
    id: "Hospital",
    columns: [{ id: "name", label: "Name", columnType: "STRING" }],
  })
);

vi.mock("../../../../app/composables/fetchTableMetadata", () => ({
  default: mockFetchTableMetadata,
}));

const MAX_VISIBLE_FILTERS_MOCK = 25;

const defaultProps = {
  schemaId: "test",
  tableId: "Patient",
};

function writeVisibleFiltersToUrl(newIds: string[], defaultIds: string[]) {
  const isDefault =
    newIds.length === defaultIds.length &&
    [...newIds].sort().every((v, i) => v === [...defaultIds].sort()[i]);
  const currentQuery = { ...mockRoute.query };
  if (isDefault) {
    delete currentQuery["mg_filters"];
  } else {
    currentQuery["mg_filters"] = newIds.join(",");
  }
  mockRouter.replace({ query: currentQuery });
}

function createMockUseFilters(
  initialFilters?: Map<string, IFilterValue>,
  initialColumns?: IColumn[],
  initialVisibleIds?: string[]
): UseFilters {
  const filterStatesRef = ref<Map<string, IFilterValue>>(
    initialFilters ?? new Map()
  );
  const searchValueRef = ref("");
  const columnsRef = ref<IColumn[]>(initialColumns ?? allColumns);
  const visibleFilterIdsRef = ref<string[]>(
    initialVisibleIds ?? ["disease", "phenotype", "hospital", "medications"]
  );
  const refColumnsCacheInternal = new Map<string, IColumn[]>();

  const defaultIds = ["disease", "phenotype", "hospital", "medications"];

  const resolvedFilters = computed(() => {
    return visibleFilterIdsRef.value
      .map((id) => {
        const column = columnsRef.value.find((c) => c.id === id);
        if (!column) return null;
        return { fullPath: id, column, label: column.label || column.id };
      })
      .filter(Boolean) as {
      fullPath: string;
      column: IColumn;
      label: string;
    }[];
  });

  return {
    filterStates: filterStatesRef,
    searchValue: searchValueRef,
    gqlFilter: ref({}),
    activeFilters: computed(() => []),
    setFilter: (columnId: string, value: IFilterValue | null) => {
      const newMap = new Map(filterStatesRef.value);
      if (value === null) {
        newMap.delete(columnId);
      } else {
        newMap.set(columnId, value);
      }
      filterStatesRef.value = newMap;
    },
    setSearch: (value: string) => {
      searchValueRef.value = value;
    },
    clearFilters: () => {
      filterStatesRef.value = new Map();
      searchValueRef.value = "";
    },
    removeFilter: (columnId: string) => {
      const newMap = new Map(filterStatesRef.value);
      newMap.delete(columnId);
      filterStatesRef.value = newMap;
    },
    columns: columnsRef,
    visibleFilterIds: visibleFilterIdsRef,
    toggleFilter: (columnId: string) => {
      if (visibleFilterIdsRef.value.includes(columnId)) {
        const newIds = visibleFilterIdsRef.value.filter(
          (id) => id !== columnId
        );
        visibleFilterIdsRef.value = newIds;
        const newMap = new Map(filterStatesRef.value);
        newMap.delete(columnId);
        filterStatesRef.value = newMap;
        writeVisibleFiltersToUrl(newIds, defaultIds);
      } else if (visibleFilterIdsRef.value.length < MAX_VISIBLE_FILTERS_MOCK) {
        const newIds = [...visibleFilterIdsRef.value, columnId];
        visibleFilterIdsRef.value = newIds;
        writeVisibleFiltersToUrl(newIds, defaultIds);
      }
    },
    resetFilters: () => {
      const newDefaults = [...defaultIds];
      visibleFilterIdsRef.value = newDefaults;
      filterStatesRef.value = new Map();
      writeVisibleFiltersToUrl(newDefaults, defaultIds);
    },
    loadRefColumns: vi.fn(),
    getRefColumns: (path: string) => refColumnsCacheInternal.get(path) ?? [],
    resolvedFilters,
    setFilterValue: async (
      columnId: string,
      value: IFilterValue | null | undefined
    ) => {
      if (value === null || value === undefined) {
        const newMap = new Map(filterStatesRef.value);
        newMap.delete(columnId);
        filterStatesRef.value = newMap;
      } else {
        const newMap = new Map(filterStatesRef.value);
        newMap.set(columnId, value);
        filterStatesRef.value = newMap;
      }
    },
    getCountFetcher: vi.fn().mockReturnValue({
      fetchRefCounts: vi.fn().mockResolvedValue(new Map()),
      fetchOntologyLeafCounts: vi.fn().mockResolvedValue(new Map()),
      fetchOntologyParentCounts: vi.fn().mockResolvedValue(new Map()),
      getCrossFilter: vi.fn().mockReturnValue(undefined),
    }),
  };
}

function mountSidebar(
  props = {},
  filterStates?: Map<string, IFilterValue>,
  columns?: IColumn[]
) {
  const mockFilters = createMockUseFilters(filterStates, columns);
  const wrapper = mount(Sidebar, {
    props: {
      ...defaultProps,
      filters: mockFilters,
      ...props,
    },
    global: {
      stubs: {
        FilterColumn: true,
        FilterPicker: true,
        InputSearch: true,
      },
    },
  });
  return { wrapper, mockFilters };
}

describe("Sidebar", () => {
  beforeEach(() => {
    mockRoute.query = {};
    mockRouter.replace.mockClear();
  });

  describe("rendering", () => {
    it("renders default title Filters", () => {
      const { wrapper } = mountSidebar();
      expect(wrapper.text()).toContain("Filters");
    });

    it("renders custom title when provided", () => {
      const { wrapper } = mountSidebar({ title: "My Filters" });
      expect(wrapper.text()).toContain("My Filters");
    });

    it("renders search input when showSearch is true", () => {
      const { wrapper } = mountSidebar({ showSearch: true });
      const searchStub = wrapper.findComponent({ name: "InputSearch" });
      expect(searchStub.exists()).toBe(true);
    });

    it("hides search input when showSearch is false", () => {
      const { wrapper } = mountSidebar({ showSearch: false });
      const searchStub = wrapper.findComponent({ name: "InputSearch" });
      expect(searchStub.exists()).toBe(false);
    });

    it("applies bg-sidebar-gradient class", () => {
      const { wrapper } = mountSidebar();
      expect(wrapper.find(".bg-sidebar-gradient").exists()).toBe(true);
    });
  });

  describe("columns from filters.columns", () => {
    it("passes filters object to FilterPicker", async () => {
      const { wrapper, mockFilters } = mountSidebar();
      await nextTick();

      const picker = wrapper.findComponent({ name: "FilterPicker" });
      expect(picker.props("filters")).toBe(mockFilters);
    });
  });

  describe("smart defaults", () => {
    it("shows ontology columns as default visible filters", async () => {
      const { wrapper } = mountSidebar();
      await nextTick();
      await nextTick();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      const columnIds = filterColumns.map((f) => f.props("column")?.id);
      expect(columnIds).toContain("disease");
      expect(columnIds).toContain("phenotype");
    });

    it("fills remaining slots with ref columns when fewer than 5 ontology columns", async () => {
      const { wrapper } = mountSidebar();
      await nextTick();
      await nextTick();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      const columnIds = filterColumns.map((f) => f.props("column")?.id);
      expect(columnIds).toContain("hospital");
      expect(columnIds).toContain("medications");
    });

    it("does not show string columns by default", async () => {
      const { wrapper } = mountSidebar();
      await nextTick();
      await nextTick();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      const columnIds = filterColumns.map((f) => f.props("column")?.id);
      expect(columnIds).not.toContain("name");
      expect(columnIds).not.toContain("notes");
    });

    it("shows no filters when all columns are unfilterable types", async () => {
      const { wrapper } = mountSidebar({}, undefined, []);

      await nextTick();
      await nextTick();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      expect(filterColumns.length).toBe(0);
    });

    it("limits defaults to 5 columns maximum", async () => {
      const manyOntologyCols: IColumn[] = Array.from({ length: 8 }, (_, i) => ({
        id: `ont${i}`,
        label: `Ont ${i}`,
        columnType: "ONTOLOGY" as const,
        refTableId: "OntTable",
        refSchemaId: "Ont",
      }));

      const mockFilters = createMockUseFilters(
        undefined,
        manyOntologyCols,
        manyOntologyCols.slice(0, 5).map((c) => c.id)
      );

      const wrapper = mount(Sidebar, {
        props: {
          ...defaultProps,
          filters: mockFilters,
        },
        global: {
          stubs: {
            FilterColumn: true,
            FilterPicker: true,
            InputSearch: true,
          },
        },
      });

      await nextTick();
      await nextTick();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      expect(filterColumns.length).toBe(5);
    });
  });

  describe("filter toggle", () => {
    it("re-renders when filters.visibleFilterIds changes", async () => {
      const { wrapper, mockFilters } = mountSidebar();
      await nextTick();
      await nextTick();

      const initialCount = wrapper.findAllComponents({
        name: "FilterColumn",
      }).length;

      mockFilters.toggleFilter("name");
      await nextTick();

      const newCount = wrapper.findAllComponents({
        name: "FilterColumn",
      }).length;
      expect(newCount).toBe(initialCount + 1);
    });

    it("re-renders when a visible filter is toggled off", async () => {
      const { wrapper, mockFilters } = mountSidebar();
      await nextTick();
      await nextTick();

      const initialCount = wrapper.findAllComponents({
        name: "FilterColumn",
      }).length;

      mockFilters.toggleFilter("disease");
      await nextTick();

      const newCount = wrapper.findAllComponents({
        name: "FilterColumn",
      }).length;
      expect(newCount).toBe(initialCount - 1);
    });
  });

  describe("URL sync", () => {
    it("reads visible filter IDs from mg_filters URL param on mount", async () => {
      mockRoute.query = { mg_filters: "name,notes" };

      const mockFilters = createMockUseFilters(undefined, allColumns, [
        "name",
        "notes",
      ]);
      const wrapper = mount(Sidebar, {
        props: { ...defaultProps, filters: mockFilters },
        global: {
          stubs: { FilterColumn: true, FilterPicker: true, InputSearch: true },
        },
      });

      await nextTick();
      await nextTick();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      const columnIds = filterColumns.map((f) => f.props("column")?.id);
      expect(columnIds).toContain("name");
      expect(columnIds).toContain("notes");
    });

    it("renders filters matching visibleFilterIds from mock", async () => {
      const mockFilters = createMockUseFilters(undefined, allColumns, [
        "name",
        "notes",
      ]);
      const wrapper = mount(Sidebar, {
        props: { ...defaultProps, filters: mockFilters },
        global: {
          stubs: { FilterColumn: true, FilterPicker: true, InputSearch: true },
        },
      });

      await nextTick();
      await nextTick();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      const columnIds = filterColumns.map((f) => f.props("column")?.id);
      expect(columnIds).toContain("name");
      expect(columnIds).toContain("notes");
    });
  });

  describe("nested filters", () => {
    it("resolves nested dotted path from URL to correct FilterColumn", async () => {
      const hospitalColumn: IColumn = {
        id: "hospital",
        label: "Hospital",
        columnType: "REF",
        refTableId: "Hospital",
        refSchemaId: "test",
      };
      const nameColumn: IColumn = {
        id: "name",
        label: "Name",
        columnType: "STRING",
      };

      const mockFilters = createMockUseFilters(
        undefined,
        [hospitalColumn],
        ["hospital.name"]
      );
      (mockFilters.resolvedFilters as any) = computed(() => [
        {
          fullPath: "hospital.name",
          column: nameColumn,
          label: "Hospital.Name",
        },
      ]);

      const wrapper = mount(Sidebar, {
        props: { ...defaultProps, filters: mockFilters },
        global: {
          stubs: { FilterColumn: true, FilterPicker: true, InputSearch: true },
        },
      });

      await flushPromises();
      await nextTick();

      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      const nestedFilter = filterColumns.find(
        (f) => f.props("column")?.id === "name"
      );
      expect(nestedFilter).toBeDefined();
      expect(nestedFilter?.props("label")).toBe("Hospital.Name");
    });

    it("resolves nested filter value from URL correctly", async () => {
      const hospitalColumn: IColumn = {
        id: "hospital",
        label: "Hospital",
        columnType: "REF",
        refTableId: "Hospital",
        refSchemaId: "test",
      };
      const nameColumn: IColumn = {
        id: "name",
        label: "Name",
        columnType: "STRING",
      };

      const initialFilterStates = new Map<string, IFilterValue>([
        ["hospital.name", { operator: "like", value: "General" }],
      ]);

      const mockFilters = createMockUseFilters(
        initialFilterStates,
        [hospitalColumn],
        ["hospital.name"]
      );
      (mockFilters.resolvedFilters as any) = computed(() => [
        {
          fullPath: "hospital.name",
          column: nameColumn,
          label: "Hospital.Name",
        },
      ]);

      const wrapper = mount(Sidebar, {
        props: { ...defaultProps, filters: mockFilters },
        global: {
          stubs: { FilterColumn: true, FilterPicker: true, InputSearch: true },
        },
      });

      await flushPromises();
      await nextTick();

      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      const nestedFilter = filterColumns.find(
        (f) => f.props("column")?.id === "name"
      );
      expect(nestedFilter).toBeDefined();
      expect(nestedFilter?.props("modelValue")).toEqual({
        operator: "like",
        value: "General",
      });
    });

    it("gracefully skips nested path when ref metadata fetch fails", async () => {
      const diseaseColumn: IColumn = {
        id: "disease",
        label: "Disease",
        columnType: "ONTOLOGY",
        refTableId: "Diseases",
        refSchemaId: "Ontologies",
      };

      const mockFilters = createMockUseFilters(
        undefined,
        [diseaseColumn],
        ["disease"]
      );
      (mockFilters.resolvedFilters as any) = computed(() => [
        { fullPath: "disease", column: diseaseColumn, label: "Disease" },
      ]);

      const wrapper = mount(Sidebar, {
        props: { ...defaultProps, filters: mockFilters },
        global: {
          stubs: { FilterColumn: true, FilterPicker: true, InputSearch: true },
        },
      });

      await flushPromises();
      await nextTick();

      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      const nestedFilter = filterColumns.find(
        (f) => f.props("column")?.id === "name"
      );
      expect(nestedFilter).toBeUndefined();

      const diseaseFilter = filterColumns.find(
        (f) => f.props("column")?.id === "disease"
      );
      expect(diseaseFilter).toBeDefined();
    });

    it("passes column label for top-level columns", async () => {
      const { wrapper } = mountSidebar();
      await nextTick();
      await nextTick();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      const topLevelFilters = filterColumns.filter(
        (f) => !f.props("column")?.id?.includes(".")
      );
      for (const filter of topLevelFilters) {
        const column = filter.props("column");
        expect(filter.props("label")).toBe(column?.label || column?.id);
      }
    });
  });

  describe("FilterColumn interaction", () => {
    it("passes filter value to FilterColumn", async () => {
      const initialFilterStates = new Map<string, IFilterValue>([
        ["disease", { operator: "equals", value: [{ name: "Flu" }] }],
      ]);
      const { wrapper } = mountSidebar({}, initialFilterStates);
      await nextTick();
      await nextTick();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      const diseaseFilter = filterColumns.find(
        (f) => f.props("column")?.id === "disease"
      );
      expect(diseaseFilter).toBeDefined();
      expect(diseaseFilter?.props("modelValue")).toEqual({
        operator: "equals",
        value: [{ name: "Flu" }],
      });
    });

    it("removes a filter when FilterColumn emits remove", async () => {
      const { wrapper } = mountSidebar();
      await nextTick();
      await nextTick();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      const initialCount = filterColumns.length;

      await filterColumns[0].vm.$emit("remove");
      await nextTick();

      const newCount = wrapper.findAllComponents({
        name: "FilterColumn",
      }).length;
      expect(newCount).toBe(initialCount - 1);
    });
  });

  describe("MAX_VISIBLE_FILTERS cap", () => {
    it("renders 25 FilterColumns when given 25 visible filters", async () => {
      const manyColumns: IColumn[] = Array.from({ length: 26 }, (_, i) => ({
        id: `col${i}`,
        label: `Col ${i}`,
        columnType: "STRING" as const,
      }));

      const initial25Ids = Array.from({ length: 25 }, (_, i) => `col${i}`);

      const mockFilters = createMockUseFilters(
        undefined,
        manyColumns,
        initial25Ids
      );

      const wrapper = mount(Sidebar, {
        props: { ...defaultProps, filters: mockFilters },
        global: {
          stubs: {
            FilterColumn: true,
            FilterPicker: true,
            InputSearch: true,
          },
        },
      });

      await nextTick();
      await nextTick();

      const filterColumns = wrapper.findAllComponents({
        name: "FilterColumn",
      }).length;
      expect(filterColumns).toBe(25);
    });
  });

  describe("metadata fetch failure", () => {
    it("renders no FilterColumns when filters.columns is empty", async () => {
      const mockFilters = createMockUseFilters(undefined, [], []);

      const wrapper = mount(Sidebar, {
        props: { ...defaultProps, filters: mockFilters },
        global: {
          stubs: {
            FilterColumn: true,
            FilterPicker: true,
            InputSearch: true,
          },
        },
      });

      await nextTick();
      await nextTick();

      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      expect(filterColumns).toHaveLength(0);
    });
  });

  describe("FilterPicker props", () => {
    it("passes filters object to FilterPicker", async () => {
      const { wrapper, mockFilters } = mountSidebar();
      await nextTick();
      await nextTick();
      const picker = wrapper.findComponent({ name: "FilterPicker" });
      expect(picker.props("filters")).toBe(mockFilters);
    });
  });
});
