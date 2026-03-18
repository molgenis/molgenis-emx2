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
    id: "Patient",
    columns: [
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
      { id: "name", label: "Name", columnType: "STRING" },
      { id: "notes", label: "Notes", columnType: "TEXT" },
    ],
  })
);

vi.mock("../../../../app/composables/fetchTableMetadata", () => ({
  default: mockFetchTableMetadata,
}));

const defaultProps = {
  schemaId: "test",
  tableId: "Patient",
};

function mountSidebar(props = {}, filterStates?: Map<string, IFilterValue>) {
  return mount(Sidebar, {
    props: {
      ...defaultProps,
      ...props,
      ...(filterStates !== undefined ? { filterStates } : {}),
    },
    global: {
      stubs: {
        FilterColumn: true,
        FilterPicker: true,
        InputSearch: true,
      },
    },
  });
}

describe("Sidebar", () => {
  beforeEach(() => {
    mockRoute.query = {};
    mockRouter.replace.mockClear();
  });

  describe("rendering", () => {
    it("renders default title Filters", () => {
      const wrapper = mountSidebar();
      expect(wrapper.text()).toContain("Filters");
    });

    it("renders custom title when provided", () => {
      const wrapper = mountSidebar({ title: "My Filters" });
      expect(wrapper.text()).toContain("My Filters");
    });

    it("renders search input when showSearch is true", () => {
      const wrapper = mountSidebar({ showSearch: true });
      const searchStub = wrapper.findComponent({ name: "InputSearch" });
      expect(searchStub.exists()).toBe(true);
    });

    it("hides search input when showSearch is false", () => {
      const wrapper = mountSidebar({ showSearch: false });
      const searchStub = wrapper.findComponent({ name: "InputSearch" });
      expect(searchStub.exists()).toBe(false);
    });

    it("applies bg-sidebar-gradient class", () => {
      const wrapper = mountSidebar();
      expect(wrapper.find(".bg-sidebar-gradient").exists()).toBe(true);
    });
  });

  describe("internal metadata fetch", () => {
    it("filters out HEADING, SECTION, FILE, and mg_* columns internally", async () => {
      mockFetchTableMetadata.mockResolvedValueOnce({
        id: "Patient",
        columns: [...allColumns, ...unfilterableColumns],
      });

      const wrapper = mountSidebar();
      await nextTick();
      await nextTick();

      const picker = wrapper.findComponent({ name: "FilterPicker" });
      const columns = picker.props("columns") as IColumn[];
      const columnIds = columns.map((c) => c.id);

      expect(columnIds).not.toContain("heading1");
      expect(columnIds).not.toContain("section1");
      expect(columnIds).not.toContain("file1");
      expect(columnIds).not.toContain("mg_insertedOn");
    });
  });

  describe("smart defaults", () => {
    it("shows ontology columns as default visible filters", async () => {
      const wrapper = mountSidebar();
      await nextTick();
      await nextTick();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      const columnIds = filterColumns.map((f) => f.props("column")?.id);
      expect(columnIds).toContain("disease");
      expect(columnIds).toContain("phenotype");
    });

    it("fills remaining slots with ref columns when fewer than 5 ontology columns", async () => {
      const wrapper = mountSidebar();
      await nextTick();
      await nextTick();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      const columnIds = filterColumns.map((f) => f.props("column")?.id);
      expect(columnIds).toContain("hospital");
      expect(columnIds).toContain("medications");
    });

    it("does not show string columns by default", async () => {
      const wrapper = mountSidebar();
      await nextTick();
      await nextTick();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      const columnIds = filterColumns.map((f) => f.props("column")?.id);
      expect(columnIds).not.toContain("name");
      expect(columnIds).not.toContain("notes");
    });

    it("shows no filters when all columns are unfilterable types", async () => {
      mockFetchTableMetadata.mockResolvedValueOnce({
        id: "Patient",
        columns: [
          { id: "heading1", label: "Demographics", columnType: "HEADING" },
          { id: "section1", label: "Section", columnType: "SECTION" },
        ],
      });

      const wrapper = mountSidebar();
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

      mockFetchTableMetadata.mockResolvedValueOnce({
        id: "Patient",
        columns: manyOntologyCols,
      });

      const wrapper = mountSidebar();
      await nextTick();
      await nextTick();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      expect(filterColumns.length).toBeLessThanOrEqual(5);
    });
  });

  describe("filter toggle", () => {
    it("adds a filter when FilterPicker emits toggle for a hidden column", async () => {
      const wrapper = mountSidebar();
      await nextTick();
      await nextTick();
      const picker = wrapper.findComponent({ name: "FilterPicker" });

      const initialCount = wrapper.findAllComponents({
        name: "FilterColumn",
      }).length;

      await picker.vm.$emit("toggle", "name");
      await nextTick();

      const newCount = wrapper.findAllComponents({
        name: "FilterColumn",
      }).length;
      expect(newCount).toBe(initialCount + 1);
    });

    it("removes a filter when FilterPicker emits toggle for a visible column", async () => {
      const wrapper = mountSidebar();
      await nextTick();
      await nextTick();
      const picker = wrapper.findComponent({ name: "FilterPicker" });

      const initialCount = wrapper.findAllComponents({
        name: "FilterColumn",
      }).length;

      await picker.vm.$emit("toggle", "disease");
      await nextTick();

      const newCount = wrapper.findAllComponents({
        name: "FilterColumn",
      }).length;
      expect(newCount).toBe(initialCount - 1);
    });

    it("removes filter value when toggling off a column that has a value", async () => {
      const initialFilterStates = new Map<string, IFilterValue>([
        ["disease", { operator: "in", value: [{ name: "Flu" }] }],
      ]);
      const wrapper = mountSidebar({}, initialFilterStates);
      await nextTick();
      await nextTick();
      const picker = wrapper.findComponent({ name: "FilterPicker" });

      await picker.vm.$emit("toggle", "disease");
      await nextTick();

      const emittedUpdates = wrapper.emitted("update:filterStates");
      expect(emittedUpdates).toBeTruthy();
      const lastEmitted = emittedUpdates![emittedUpdates!.length - 1][0] as Map<
        string,
        IFilterValue
      >;
      expect(lastEmitted.has("disease")).toBe(false);
    });
  });

  describe("filter reset", () => {
    it("resets to default filters when FilterPicker emits reset", async () => {
      const wrapper = mountSidebar();
      await nextTick();
      await nextTick();
      const picker = wrapper.findComponent({ name: "FilterPicker" });

      await picker.vm.$emit("toggle", "name");
      await nextTick();

      const countAfterToggle = wrapper.findAllComponents({
        name: "FilterColumn",
      }).length;

      await picker.vm.$emit("reset");
      await nextTick();

      const countAfterReset = wrapper.findAllComponents({
        name: "FilterColumn",
      }).length;
      expect(countAfterReset).toBeLessThan(countAfterToggle);
    });

    it("clears filter values for columns removed during reset", async () => {
      const initialFilterStates = new Map<string, IFilterValue>([
        ["name", { operator: "like", value: "test" }],
      ]);
      const wrapper = mountSidebar({}, initialFilterStates);
      await nextTick();
      await nextTick();
      const picker = wrapper.findComponent({ name: "FilterPicker" });

      await picker.vm.$emit("toggle", "name");
      await nextTick();

      await picker.vm.$emit("reset");
      await nextTick();

      const emittedUpdates = wrapper.emitted("update:filterStates");
      expect(emittedUpdates).toBeTruthy();
      const lastEmitted = emittedUpdates![emittedUpdates!.length - 1][0] as Map<
        string,
        IFilterValue
      >;
      expect(lastEmitted.has("name")).toBe(false);
    });
  });

  describe("URL sync", () => {
    it("reads visible filter IDs from mg_filters URL param on mount", async () => {
      mockRoute.query = { mg_filters: "name,notes" };
      const wrapper = mountSidebar();
      await nextTick();
      await nextTick();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      const columnIds = filterColumns.map((f) => f.props("column")?.id);
      expect(columnIds).toContain("name");
      expect(columnIds).toContain("notes");
    });

    it("writes mg_filters to URL when visible filters change from defaults", async () => {
      const wrapper = mountSidebar();
      await nextTick();
      await nextTick();
      const picker = wrapper.findComponent({ name: "FilterPicker" });

      await picker.vm.$emit("toggle", "name");
      await nextTick();

      expect(mockRouter.replace).toHaveBeenCalled();
      const lastCall =
        mockRouter.replace.mock.calls[mockRouter.replace.mock.calls.length - 1];
      const query = lastCall[0]?.query;
      expect(query?.mg_filters).toBeDefined();
      expect(query?.mg_filters).toContain("name");
    });

    it("removes mg_filters from URL when visible filters match defaults", async () => {
      const wrapper = mountSidebar();
      await nextTick();
      await nextTick();
      const picker = wrapper.findComponent({ name: "FilterPicker" });

      await picker.vm.$emit("toggle", "name");
      await nextTick();
      mockRouter.replace.mockClear();

      await picker.vm.$emit("reset");
      await nextTick();

      expect(mockRouter.replace).toHaveBeenCalled();
      const lastCall =
        mockRouter.replace.mock.calls[mockRouter.replace.mock.calls.length - 1];
      const query = lastCall[0]?.query;
      expect(query?.mg_filters).toBeUndefined();
    });
  });

  describe("nested filters", () => {
    it("resolves nested dotted path from URL to correct FilterColumn", async () => {
      mockRoute.query = { mg_filters: "hospital.name" };

      mockFetchTableMetadata
        .mockResolvedValueOnce({
          id: "Patient",
          columns: [
            {
              id: "hospital",
              label: "Hospital",
              columnType: "REF",
              refTableId: "Hospital",
              refSchemaId: "test",
            },
          ],
        })
        .mockResolvedValueOnce({
          id: "Hospital",
          columns: [{ id: "name", label: "Name", columnType: "STRING" }],
        });

      const wrapper = mountSidebar();
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
      mockRoute.query = { mg_filters: "hospital.name" };

      mockFetchTableMetadata
        .mockResolvedValueOnce({
          id: "Patient",
          columns: [
            {
              id: "hospital",
              label: "Hospital",
              columnType: "REF",
              refTableId: "Hospital",
              refSchemaId: "test",
            },
          ],
        })
        .mockResolvedValueOnce({
          id: "Hospital",
          columns: [{ id: "name", label: "Name", columnType: "STRING" }],
        });

      const initialFilterStates = new Map<string, IFilterValue>([
        ["hospital.name", { operator: "like", value: "General" }],
      ]);

      const wrapper = mountSidebar({}, initialFilterStates);
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
      mockRoute.query = { mg_filters: "disease,hospital.name" };

      mockFetchTableMetadata
        .mockResolvedValueOnce({
          id: "Patient",
          columns: [
            {
              id: "disease",
              label: "Disease",
              columnType: "ONTOLOGY",
              refTableId: "Diseases",
              refSchemaId: "Ontologies",
            },
            {
              id: "hospital",
              label: "Hospital",
              columnType: "REF",
              refTableId: "Hospital",
              refSchemaId: "test",
            },
          ],
        })
        .mockRejectedValueOnce(new Error("Network error"));

      const wrapper = mountSidebar();
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
      const wrapper = mountSidebar();
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
        ["disease", { operator: "in", value: [{ name: "Flu" }] }],
      ]);
      const wrapper = mountSidebar({}, initialFilterStates);
      await nextTick();
      await nextTick();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      const diseaseFilter = filterColumns.find(
        (f) => f.props("column")?.id === "disease"
      );
      expect(diseaseFilter).toBeDefined();
      expect(diseaseFilter?.props("modelValue")).toEqual({
        operator: "in",
        value: [{ name: "Flu" }],
      });
    });

    it("removes a filter when FilterColumn emits remove", async () => {
      const wrapper = mountSidebar();
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
    it("refuses to add a filter when already at 25 visible filters", async () => {
      const manyColumns: IColumn[] = Array.from({ length: 26 }, (_, i) => ({
        id: `col${i}`,
        label: `Col ${i}`,
        columnType: "STRING" as const,
      }));

      mockFetchTableMetadata.mockResolvedValueOnce({
        id: "Patient",
        columns: manyColumns,
      });

      mockRoute.query = {
        mg_filters: Array.from({ length: 25 }, (_, i) => `col${i}`).join(","),
      };

      const wrapper = mountSidebar();
      await nextTick();
      await nextTick();

      const filterColumnsBefore = wrapper.findAllComponents({
        name: "FilterColumn",
      }).length;
      expect(filterColumnsBefore).toBe(25);

      const picker = wrapper.findComponent({ name: "FilterPicker" });
      await picker.vm.$emit("toggle", "col25");
      await nextTick();

      const filterColumnsAfter = wrapper.findAllComponents({
        name: "FilterColumn",
      }).length;
      expect(filterColumnsAfter).toBe(25);
    });
  });

  describe("metadata fetch failure", () => {
    it("results in empty filterableColumns when fetchTableMetadata throws on mount", async () => {
      mockFetchTableMetadata.mockRejectedValueOnce(new Error("Network error"));

      const wrapper = mountSidebar();
      await nextTick();
      await nextTick();

      const picker = wrapper.findComponent({ name: "FilterPicker" });
      const columns = picker.props("columns") as IColumn[];
      expect(columns).toHaveLength(0);

      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      expect(filterColumns).toHaveLength(0);
    });
  });

  describe("FilterPicker props", () => {
    it("passes filterable columns, schemaId, and visibleFilterIds to FilterPicker", async () => {
      const wrapper = mountSidebar();
      await nextTick();
      await nextTick();
      const picker = wrapper.findComponent({ name: "FilterPicker" });

      const columns = picker.props("columns") as IColumn[];
      expect(columns.length).toBeGreaterThan(0);
      const columnIds = columns.map((c) => c.id);
      expect(columnIds).toContain("disease");
      expect(columnIds).toContain("hospital");
      expect(columnIds).toContain("name");

      expect(picker.props("schemaId")).toBe("test");

      const visibleIds = picker.props("visibleFilterIds") as string[];
      expect(Array.isArray(visibleIds)).toBe(true);
      expect(visibleIds.length).toBeGreaterThan(0);
    });

    it("passes defaultFilterIds to FilterPicker", async () => {
      const wrapper = mountSidebar();
      await nextTick();
      await nextTick();
      const picker = wrapper.findComponent({ name: "FilterPicker" });
      const defaultIds = picker.props("defaultFilterIds") as string[];
      expect(Array.isArray(defaultIds)).toBe(true);
      expect(defaultIds).toContain("disease");
    });
  });
});
