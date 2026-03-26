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
import type { IFilterValue } from "../../../../types/filters";
import { computed } from "vue";
import { createMockUseFilters } from "../../fixtures/mockFilters";

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

const defaultProps = {
  schemaId: "test",
  tableId: "Patient",
};

const defaultVisibleIds = ["disease", "phenotype", "hospital", "medications"];

function makeMockFilters(
  initialFilters?: Map<string, IFilterValue>,
  initialColumns?: IColumn[],
  initialVisibleIds?: string[]
) {
  return createMockUseFilters({
    initialFilters,
    initialColumns: initialColumns ?? allColumns,
    initialVisibleIds: initialVisibleIds ?? defaultVisibleIds,
    defaultVisibleIds,
    mockRoute,
    mockRouter,
  });
}

function mountSidebar(
  props = {},
  filterStates?: Map<string, IFilterValue>,
  columns?: IColumn[]
) {
  const mockFilters = makeMockFilters(filterStates, columns);
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

      const mockFilters = makeMockFilters(
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

      const mockFilters = makeMockFilters(
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

      const mockFilters = makeMockFilters(
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

      const mockFilters = makeMockFilters(undefined, manyColumns, initial25Ids);

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
      const mockFilters = makeMockFilters(undefined, [], []);

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
});
