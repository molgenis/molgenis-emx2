import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
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

const defaultProps = {
  allColumns,
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

  describe("smart defaults", () => {
    it("shows ontology columns as default visible filters", () => {
      const wrapper = mountSidebar();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      const columnIds = filterColumns.map((f) => f.props("column")?.id);
      expect(columnIds).toContain("disease");
      expect(columnIds).toContain("phenotype");
    });

    it("fills remaining slots with ref columns when fewer than 5 ontology columns", () => {
      const wrapper = mountSidebar();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      const columnIds = filterColumns.map((f) => f.props("column")?.id);
      expect(columnIds).toContain("hospital");
      expect(columnIds).toContain("medications");
    });

    it("does not show string columns by default", () => {
      const wrapper = mountSidebar();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      const columnIds = filterColumns.map((f) => f.props("column")?.id);
      expect(columnIds).not.toContain("name");
      expect(columnIds).not.toContain("notes");
    });

    it("shows no filters when all columns are unfilterable types", () => {
      const headingOnlyColumns: IColumn[] = [
        { id: "heading1", label: "Demographics", columnType: "HEADING" },
        { id: "section1", label: "Section", columnType: "SECTION" },
      ];
      const wrapper = mountSidebar({ allColumns: headingOnlyColumns });
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      expect(filterColumns.length).toBe(0);
    });

    it("limits defaults to 5 columns maximum", () => {
      const manyOntologyCols: IColumn[] = Array.from({ length: 8 }, (_, i) => ({
        id: `ont${i}`,
        label: `Ont ${i}`,
        columnType: "ONTOLOGY" as const,
        refTableId: "OntTable",
        refSchemaId: "Ont",
      }));
      const wrapper = mountSidebar({ allColumns: manyOntologyCols });
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      expect(filterColumns.length).toBeLessThanOrEqual(5);
    });
  });

  describe("filter toggle", () => {
    it("adds a filter when FilterPicker emits toggle for a hidden column", async () => {
      const wrapper = mountSidebar();
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
    it("reads visible filter IDs from mg_filters URL param on mount", () => {
      mockRoute.query = { mg_filters: "name,notes" };
      const wrapper = mountSidebar();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      const columnIds = filterColumns.map((f) => f.props("column")?.id);
      expect(columnIds).toContain("name");
      expect(columnIds).toContain("notes");
    });

    it("writes mg_filters to URL when visible filters change from defaults", async () => {
      const wrapper = mountSidebar();
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
      const picker = wrapper.findComponent({ name: "FilterPicker" });

      await picker.vm.$emit("toggle", "name");
      await nextTick();
      mockRouter.replace.mockClear();

      await picker.vm.$emit("reset");
      await nextTick();

      if (mockRouter.replace.mock.calls.length > 0) {
        const lastCall =
          mockRouter.replace.mock.calls[
            mockRouter.replace.mock.calls.length - 1
          ];
        const query = lastCall[0]?.query;
        expect(query?.mg_filters).toBeUndefined();
      }
    });
  });

  describe("nested filters", () => {
    it("passes empty labelPrefix for top-level columns", () => {
      const wrapper = mountSidebar();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      const topLevelFilters = filterColumns.filter(
        (f) => !f.props("column")?.id?.includes(".")
      );
      for (const filter of topLevelFilters) {
        expect(filter.props("labelPrefix")).toBe("");
      }
    });

    it("passes schemaId to rendered FilterColumn components", () => {
      const wrapper = mountSidebar();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      expect(filterColumns.length).toBeGreaterThan(0);
      for (const filter of filterColumns) {
        expect(filter.props("schemaId")).toBeDefined();
      }
    });

    it("renders FilterColumn with removable=true", () => {
      const wrapper = mountSidebar();
      const filterColumns = wrapper.findAllComponents({ name: "FilterColumn" });
      for (const filter of filterColumns) {
        expect(filter.props("removable")).toBe(true);
      }
    });
  });

  describe("FilterColumn interaction", () => {
    it("passes filter value to FilterColumn", () => {
      const initialFilterStates = new Map<string, IFilterValue>([
        ["disease", { operator: "in", value: [{ name: "Flu" }] }],
      ]);
      const wrapper = mountSidebar({}, initialFilterStates);
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

  describe("FilterPicker props", () => {
    it("passes allColumns to FilterPicker", () => {
      const wrapper = mountSidebar();
      const picker = wrapper.findComponent({ name: "FilterPicker" });
      expect(picker.props("columns")).toEqual(allColumns);
    });

    it("passes schemaId to FilterPicker", () => {
      const wrapper = mountSidebar();
      const picker = wrapper.findComponent({ name: "FilterPicker" });
      expect(picker.props("schemaId")).toBe("test");
    });

    it("passes visibleFilterIds to FilterPicker", () => {
      const wrapper = mountSidebar();
      const picker = wrapper.findComponent({ name: "FilterPicker" });
      const visibleIds = picker.props("visibleFilterIds") as string[];
      expect(Array.isArray(visibleIds)).toBe(true);
      expect(visibleIds.length).toBeGreaterThan(0);
    });

    it("passes defaultFilterIds to FilterPicker", () => {
      const wrapper = mountSidebar();
      const picker = wrapper.findComponent({ name: "FilterPicker" });
      const defaultIds = picker.props("defaultFilterIds") as string[];
      expect(Array.isArray(defaultIds)).toBe(true);
      expect(defaultIds).toContain("disease");
    });
  });
});
