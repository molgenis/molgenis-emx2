import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
import { ref, computed } from "vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";
import type { UseFilters, IFilterValue } from "../../../../types/filters";
import type { CountedOption } from "../../../../app/utils/fetchCounts";
import Sidebar from "../../../../app/components/filter/Sidebar.vue";
import FilterPicker from "../../../../app/components/filter/Picker.vue";

vi.mock("../../../../app/components/filter/Column.vue", () => ({
  default: {
    name: "Column",
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
  nestedMeta: Map<string, { label: string; columnType: string }> = new Map(),
  collapsedSet: Set<string> = new Set()
): UseFilters {
  const visibleFilterIds = ref(visibleIds);
  const filterStates = ref(filterStatesMap);
  const nestedColumnMeta = ref(nestedMeta);
  const collapsed = ref(collapsedSet);

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
    collapsedIds: computed(() => collapsed.value),
    toggleCollapse: vi.fn((id: string) => {
      const next = new Set(collapsed.value);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      collapsed.value = next;
    }),
    isCollapsed: (id: string) => collapsed.value.has(id),
    hydrateNestedFilters: vi.fn().mockResolvedValue(undefined),
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
  filterStatesMap: Map<string, IFilterValue> = new Map(),
  collapsedSet: Set<string> = new Set()
) {
  const filters = makeFilters(
    visibleIds,
    filterStatesMap,
    new Map(),
    collapsedSet
  );
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

  it("expanded sections show aria-expanded=true", async () => {
    const ids = ["a", "b", "c", "d", "e"];
    const wrapper = mountSidebar(ids);
    await wrapper.vm.$nextTick();

    for (const id of ids) {
      const btn = wrapper.find(`[aria-controls="filter-section-${id}"]`);
      expect(btn.attributes("aria-expanded")).toBe("true");
    }
  });

  it("collapsed sections show aria-expanded=false", async () => {
    const ids = ["a", "b", "c", "d", "e", "f", "g"];
    const wrapper = mountSidebar(ids, new Map(), new Set(["f", "g"]));
    await wrapper.vm.$nextTick();

    const fBtn = wrapper.find('[aria-controls="filter-section-f"]');
    const gBtn = wrapper.find('[aria-controls="filter-section-g"]');
    expect(fBtn.attributes("aria-expanded")).toBe("false");
    expect(gBtn.attributes("aria-expanded")).toBe("false");
  });

  it("calls toggleCollapse when section header is clicked", async () => {
    const ids = ["col1"];
    const filters = makeFilters(ids);
    const columns = makeColumns(ids);
    const wrapper = mount(Sidebar, {
      props: { filters, columns, schemaId: "test", tableId: "TestTable" },
    });
    await wrapper.vm.$nextTick();

    const btn = wrapper.find('[aria-controls="filter-section-col1"]');
    await btn.trigger("click");

    expect(filters.toggleCollapse).toHaveBeenCalledWith("col1");
  });

  it("toggles section collapse on click via mock", async () => {
    const collapsed = new Set<string>();
    const wrapper = mountSidebar(["col1"], new Map(), collapsed);
    await wrapper.vm.$nextTick();

    const btn = wrapper.find('[aria-controls="filter-section-col1"]');
    expect(btn.attributes("aria-expanded")).toBe("true");

    await btn.trigger("click");
    await wrapper.vm.$nextTick();
    expect(btn.attributes("aria-expanded")).toBe("false");

    await btn.trigger("click");
    await wrapper.vm.$nextTick();
    expect(btn.attributes("aria-expanded")).toBe("true");
  });

  it("Column not rendered for collapsed sections (lazy mount)", async () => {
    const ids = ["a", "b", "c", "d", "e", "f"];
    const wrapper = mountSidebar(ids, new Map(), new Set(["f"]));
    await wrapper.vm.$nextTick();

    const filterOptions = wrapper.findAll(".filter-options-stub");
    const expandedCount = filterOptions.length;
    expect(expandedCount).toBe(5);
  });

  it("Column rendered for expanded sections", async () => {
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

  it("calls setFilter when Column emits update", async () => {
    const filters = makeFilters(["col1"]);
    const columns = makeColumns(["col1"]);
    const wrapper = mount(Sidebar, {
      props: { filters, columns, schemaId: "test", tableId: "TestTable" },
    });
    await wrapper.vm.$nextTick();

    const fo = wrapper.findComponent({ name: "Column" });
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

    const fo = wrapper.findComponent({ name: "Column" });
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
