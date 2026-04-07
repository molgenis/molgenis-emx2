import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
import { ref, computed } from "vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";
import type { UseFilters, IFilterValue } from "../../../../types/filters";
import type { CountedOption } from "../../../../app/utils/fetchCounts";
import Sidebar from "../../../../app/components/filter/Sidebar.vue";
import FilterPicker from "../../../../app/components/filter/FilterPicker.vue";

vi.mock("../../../../app/components/filter/FilterOptions.vue", () => ({
  default: {
    name: "FilterOptions",
    props: ["column", "options", "loading", "modelValue"],
    emits: ["update:modelValue"],
    template:
      '<div class="filter-options-stub" :data-column="column.id"></div>',
  },
}));

vi.mock("../../../../app/components/BaseIcon.vue", () => ({
  default: {
    name: "BaseIcon",
    props: ["name", "width"],
    template: '<span :data-icon="name"></span>',
  },
}));

vi.mock("../../../../app/components/input/Search.vue", () => ({
  default: {
    name: "InputSearch",
    props: ["modelValue", "placeholder", "aria-label"],
    emits: ["update:modelValue"],
    template: '<input data-testid="search-input" type="search" />',
  },
}));

function makeCountedOptions(names: string[]): CountedOption[] {
  return names.map((name) => ({ name, label: name, count: 5 }));
}

function makeFilters(
  visibleIds: string[],
  filterStatesMap: Map<string, IFilterValue> = new Map(),
  nestedMeta: Map<string, { label: string; columnType: string }> = new Map()
): UseFilters {
  const visibleFilterIds = ref(visibleIds);
  const filterStates = ref(filterStatesMap);
  const nestedColumnMeta = ref(nestedMeta);

  return {
    filterStates,
    searchValue: ref(""),
    gqlFilter: computed(() => ({})),
    activeFilters: computed(() => []),
    setFilter: vi.fn(),
    setSearch: vi.fn(),
    clearFilters: vi.fn(),
    removeFilter: vi.fn(),
    columns: ref([]),
    visibleFilterIds,
    toggleFilter: vi.fn(),
    resetFilters: vi.fn(),
    getCountedOptions: (_columnId: string) =>
      computed(() => makeCountedOptions(["opt1", "opt2"])),
    isCountLoading: (_columnId: string) => computed(() => false),
    nestedColumnMeta,
    registerNestedColumn: vi.fn(),
    schemaId: "test",
    tableId: "TestTable",
  };
}

function makeColumns(ids: string[]): IColumn[] {
  return ids.map((id) => ({
    id,
    label: `Label ${id}`,
    columnType: "ONTOLOGY",
  }));
}

function mountSidebar(
  visibleIds: string[],
  filterStatesMap: Map<string, IFilterValue> = new Map()
) {
  const filters = makeFilters(visibleIds, filterStatesMap);
  const columns = makeColumns(visibleIds);
  return mount(Sidebar, {
    props: {
      filters,
      columns,
      schemaId: "test",
      tableId: "TestTable",
    },
  });
}

describe("Sidebar", () => {
  it("renders search input", () => {
    const wrapper = mountSidebar(["col1"]);
    expect(wrapper.find('[data-testid="search-input"]').exists()).toBe(true);
  });

  it("renders collapsible sections for visible filters", () => {
    const wrapper = mountSidebar(["col1", "col2", "col3"]);
    const sections = wrapper.findAll('[aria-controls^="filter-section-"]');
    expect(sections.length).toBe(3);
  });

  it("renders column labels in section headers", () => {
    const wrapper = mountSidebar(["col1", "col2"]);
    expect(wrapper.html()).toContain("Label col1");
    expect(wrapper.html()).toContain("Label col2");
  });

  it("first 5 sections are expanded on mount", async () => {
    const ids = ["a", "b", "c", "d", "e"];
    const wrapper = mountSidebar(ids);
    await wrapper.vm.$nextTick();

    for (const id of ids) {
      const btn = wrapper.find(`[aria-controls="filter-section-${id}"]`);
      expect(btn.attributes("aria-expanded")).toBe("true");
    }
  });

  it("sections beyond index 5 are collapsed on mount", async () => {
    const ids = ["a", "b", "c", "d", "e", "f", "g"];
    const wrapper = mountSidebar(ids);
    await wrapper.vm.$nextTick();

    const fBtn = wrapper.find('[aria-controls="filter-section-f"]');
    const gBtn = wrapper.find('[aria-controls="filter-section-g"]');
    expect(fBtn.attributes("aria-expanded")).toBe("false");
    expect(gBtn.attributes("aria-expanded")).toBe("false");
  });

  it("sections with active filter states start expanded even beyond index 5", async () => {
    const ids = ["a", "b", "c", "d", "e", "f"];
    const filterStates = new Map<string, IFilterValue>([
      ["f", { operator: "equals", value: "something" }],
    ]);
    const wrapper = mountSidebar(ids, filterStates);
    await wrapper.vm.$nextTick();

    const fBtn = wrapper.find('[aria-controls="filter-section-f"]');
    expect(fBtn.attributes("aria-expanded")).toBe("true");
  });

  it("toggles section collapse on click", async () => {
    const wrapper = mountSidebar(["col1"]);
    await wrapper.vm.$nextTick();

    const btn = wrapper.find('[aria-controls="filter-section-col1"]');
    expect(btn.attributes("aria-expanded")).toBe("true");

    await btn.trigger("click");
    expect(btn.attributes("aria-expanded")).toBe("false");

    await btn.trigger("click");
    expect(btn.attributes("aria-expanded")).toBe("true");
  });

  it("FilterOptions not rendered for collapsed sections (lazy mount)", async () => {
    const ids = ["a", "b", "c", "d", "e", "f"];
    const wrapper = mountSidebar(ids);
    await wrapper.vm.$nextTick();

    const filterOptions = wrapper.findAll(".filter-options-stub");
    const expandedCount = filterOptions.length;
    expect(expandedCount).toBe(5);
  });

  it("FilterOptions rendered for expanded sections", async () => {
    const wrapper = mountSidebar(["col1"]);
    await wrapper.vm.$nextTick();

    expect(wrapper.find('[data-column="col1"]').exists()).toBe(true);
  });

  it("sidebar content is always rendered (visibility controlled by parent)", async () => {
    const wrapper = mountSidebar(["col1"]);
    await wrapper.vm.$nextTick();

    expect(wrapper.find("#filter-sidebar-content").exists()).toBe(true);
  });

  it("Customize button is present", () => {
    const wrapper = mountSidebar(["col1"]);
    const buttons = wrapper.findAll("button");
    const customizeBtn = buttons.find((b) =>
      b.text().toLowerCase().includes("customize")
    );
    expect(customizeBtn).toBeDefined();
  });

  it("renders hr dividers between sections", () => {
    const wrapper = mountSidebar(["col1", "col2", "col3"]);
    const hrs = wrapper.findAll("hr");
    expect(hrs.length).toBe(3);
  });

  it("calls setSearch when search value changes", async () => {
    const filters = makeFilters(["col1"]);
    const columns = makeColumns(["col1"]);
    const wrapper = mount(Sidebar, {
      props: { filters, columns, schemaId: "test", tableId: "TestTable" },
    });
    await wrapper.vm.$nextTick();

    const searchInput = wrapper.findComponent({ name: "InputSearch" });
    await searchInput.vm.$emit("update:modelValue", "hello");

    expect(filters.setSearch).toHaveBeenCalledWith("hello");
  });

  it("calls setFilter when FilterOptions emits update", async () => {
    const filters = makeFilters(["col1"]);
    const columns = makeColumns(["col1"]);
    const wrapper = mount(Sidebar, {
      props: { filters, columns, schemaId: "test", tableId: "TestTable" },
    });
    await wrapper.vm.$nextTick();

    const fo = wrapper.findComponent({ name: "FilterOptions" });
    const newValue: IFilterValue = { operator: "equals", value: "test" };
    await fo.vm.$emit("update:modelValue", newValue);

    expect(filters.setFilter).toHaveBeenCalledWith("col1", newValue);
  });

  it("nested column shows path label as 'root → child' in sidebar", async () => {
    const filters = makeFilters(["publisher.country"]);
    const columns = makeColumns(["publisher"]);
    const wrapper = mount(Sidebar, {
      props: { filters, columns, schemaId: "test", tableId: "TestTable" },
    });
    await wrapper.vm.$nextTick();

    expect(wrapper.html()).toContain("publisher → country");
  });

  it("nested column columnType comes from nestedColumnMeta when available", async () => {
    const nestedMeta = new Map([
      [
        "publisher.country",
        { label: "publisher → country", columnType: "ONTOLOGY" },
      ],
    ]);
    const filters = makeFilters(["publisher.country"], new Map(), nestedMeta);
    const columns = makeColumns(["publisher"]);
    const wrapper = mount(Sidebar, {
      props: { filters, columns, schemaId: "test", tableId: "TestTable" },
    });
    await wrapper.vm.$nextTick();

    const fo = wrapper.findComponent({ name: "FilterOptions" });
    expect(fo.props("column").columnType).toBe("ONTOLOGY");
  });

  it("handlePickerApply with empty set calls toggleFilter for all currently visible filters", async () => {
    const filters = makeFilters(["tags", "status"]);
    const columns = makeColumns(["tags", "status"]);
    const wrapper = mount(Sidebar, {
      props: { filters, columns, schemaId: "test", tableId: "TestTable" },
    });
    await wrapper.vm.$nextTick();

    const pickerComponent = wrapper.findComponent(FilterPicker);
    await pickerComponent.vm.$emit(
      "apply",
      new Set<string>(),
      new Map<string, { label: string; columnType: string }>()
    );
    await wrapper.vm.$nextTick();

    expect(filters.toggleFilter).toHaveBeenCalledWith("tags");
    expect(filters.toggleFilter).toHaveBeenCalledWith("status");
  });
});
