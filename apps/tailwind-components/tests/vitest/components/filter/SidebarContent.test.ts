import { describe, it, expect, vi } from "vitest";
import { mount } from "@vue/test-utils";
import { ref, computed } from "vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";
import type {
  UseFilters,
  IFilterValue,
  NestedColumnMeta,
} from "../../../../types/filters";
import type { CountedOption } from "../../../../app/utils/fetchCounts";
import SidebarContent from "../../../../app/components/filter/SidebarContent.vue";
import FilterPicker from "../../../../app/components/filter/Picker.vue";

vi.mock("../../../../app/components/filter/Column.vue", () => ({
  default: {
    name: "Column",
    props: [
      "column",
      "options",
      "loading",
      "modelValue",
      "saturated",
      "hasCountError",
    ],
    emits: ["update:modelValue"],
    template:
      '<div class="filter-options-stub" :data-column="column.id"></div>',
  },
}));

vi.mock("../../../../app/components/Skeleton.vue", () => ({
  default: {
    name: "Skeleton",
    props: ["lines"],
    template: '<div class="skeleton-stub" />',
  },
}));

vi.mock("../../../../app/components/BaseIcon.vue", () => ({
  default: {
    name: "BaseIcon",
    props: ["name", "width"],
    template: '<span :data-icon="name"></span>',
  },
}));

function synthesizeMockColumn(
  id: string,
  nestedMeta: Map<string, NestedColumnMeta>
): IColumn {
  const nested = nestedMeta.get(id);
  return {
    id,
    label:
      nested?.labelParts?.join(" / ") ??
      (id.includes(".") ? "" : `Label ${id}`),
    columnType: nested?.columnType ?? "ONTOLOGY",
    table: "TestTable",
    position: 0,
  } as IColumn;
}

function makeCountedOptions(names: string[]): CountedOption[] {
  return names.map((name) => ({ name, label: name, count: 5 }));
}

function makeFilters(
  visibleIds: string[],
  filterStatesMap: Map<string, IFilterValue> = new Map(),
  nestedMeta: Map<string, NestedColumnMeta> = new Map(),
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
    visibleColumns: computed(() =>
      visibleIds.map((id) => synthesizeMockColumn(id, nestedMeta))
    ),
    toggleFilter: vi.fn(),
    resetFilters: vi.fn(),
    getCountedOptions: (_columnId: string) =>
      computed(() => makeCountedOptions(["opt1", "opt2"])),
    isCountLoading: (_columnId: string) => computed(() => false),
    isSaturated: (_columnId: string) => computed(() => false),
    hasCountError: (_columnId: string) => computed(() => false),
    nestedColumnMeta,
    registerNestedColumn: vi.fn(),
    schemaId: "test",
    tableId: "TestTable",
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
  };
}

function makeColumns(ids: string[]): IColumn[] {
  return ids.map((id) => ({
    id,
    label: `Label ${id}`,
    columnType: "ONTOLOGY",
  }));
}

function mountSidebarContent(
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
  return mount(SidebarContent, {
    props: {
      filters,
      columns,
      schemaId: "test",
      tableId: "TestTable",
    },
  });
}

describe("SidebarContent", () => {
  it("renders collapsible sections for visible filters", () => {
    const wrapper = mountSidebarContent(["col1", "col2", "col3"]);
    const sections = wrapper.findAll('[aria-controls^="filter-section-"]');
    expect(sections.length).toBe(3);
  });

  it("renders column labels in section headers", () => {
    const wrapper = mountSidebarContent(["col1", "col2"]);
    expect(wrapper.html()).toContain("Label col1");
    expect(wrapper.html()).toContain("Label col2");
  });

  it("expanded sections show aria-expanded=true", async () => {
    const ids = ["a", "b", "c", "d", "e"];
    const wrapper = mountSidebarContent(ids);
    await wrapper.vm.$nextTick();

    for (const id of ids) {
      const btn = wrapper.find(`[aria-controls="filter-section-${id}"]`);
      expect(btn.attributes("aria-expanded")).toBe("true");
    }
  });

  it("collapsed sections show aria-expanded=false", async () => {
    const ids = ["a", "b", "c", "d", "e", "f", "g"];
    const wrapper = mountSidebarContent(ids, new Map(), new Set(["f", "g"]));
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
    const wrapper = mount(SidebarContent, {
      props: { filters, columns, schemaId: "test", tableId: "TestTable" },
    });
    await wrapper.vm.$nextTick();

    const btn = wrapper.find('[aria-controls="filter-section-col1"]');
    await btn.trigger("click");

    expect(filters.toggleCollapse).toHaveBeenCalledWith("col1");
  });

  it("toggles section collapse on click via mock", async () => {
    const collapsed = new Set<string>();
    const wrapper = mountSidebarContent(["col1"], new Map(), collapsed);
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
    const wrapper = mountSidebarContent(ids, new Map(), new Set(["f"]));
    await wrapper.vm.$nextTick();

    const filterOptions = wrapper.findAll(".filter-options-stub");
    const expandedCount = filterOptions.length;
    expect(expandedCount).toBe(5);
  });

  it("Column rendered for expanded sections", async () => {
    const wrapper = mountSidebarContent(["col1"]);
    await wrapper.vm.$nextTick();

    expect(wrapper.find('[data-column="col1"]').exists()).toBe(true);
  });

  it("Customize button is present", () => {
    const wrapper = mountSidebarContent(["col1"]);
    const buttons = wrapper.findAll("button");
    const customizeBtn = buttons.find((b) =>
      b.text().toLowerCase().includes("customize")
    );
    expect(customizeBtn).toBeDefined();
  });

  it("renders hr dividers between sections", () => {
    const wrapper = mountSidebarContent(["col1", "col2", "col3"]);
    const hrs = wrapper.findAll("hr");
    expect(hrs.length).toBe(3);
  });

  it("calls setFilter when Column emits update", async () => {
    const filters = makeFilters(["col1"]);
    const columns = makeColumns(["col1"]);
    const wrapper = mount(SidebarContent, {
      props: { filters, columns, schemaId: "test", tableId: "TestTable" },
    });
    await wrapper.vm.$nextTick();

    const fo = wrapper.findComponent({ name: "Column" });
    const newValue: IFilterValue = { operator: "equals", value: "test" };
    await fo.vm.$emit("update:modelValue", newValue);

    expect(filters.setFilter).toHaveBeenCalledWith("col1", newValue);
  });

  it("nested column shows dotted id as label when nestedColumnMeta is not yet resolved", async () => {
    const filters = makeFilters(["publisher.country"]);
    const columns = makeColumns(["publisher"]);
    const wrapper = mount(SidebarContent, {
      props: { filters, columns, schemaId: "test", tableId: "TestTable" },
    });
    await wrapper.vm.$nextTick();

    expect(wrapper.html()).toContain("publisher.country");
  });

  it("nested column columnType comes from nestedColumnMeta when available", async () => {
    const nestedMeta = new Map<string, NestedColumnMeta>([
      [
        "publisher.country",
        {
          labelParts: ["Publisher", "Country"],
          columnType: "ONTOLOGY",
        },
      ],
    ]);
    const filters = makeFilters(["publisher.country"], new Map(), nestedMeta);
    const columns = makeColumns(["publisher"]);
    const wrapper = mount(SidebarContent, {
      props: { filters, columns, schemaId: "test", tableId: "TestTable" },
    });
    await wrapper.vm.$nextTick();

    const fo = wrapper.findComponent({ name: "Column" });
    expect(fo.props("column").columnType).toBe("ONTOLOGY");
  });

  it("handlePickerApply with empty set calls toggleFilter for all currently visible filters", async () => {
    const filters = makeFilters(["tags", "status"]);
    const columns = makeColumns(["tags", "status"]);
    const wrapper = mount(SidebarContent, {
      props: { filters, columns, schemaId: "test", tableId: "TestTable" },
    });
    await wrapper.vm.$nextTick();

    const pickerComponent = wrapper.findComponent(FilterPicker);
    await pickerComponent.vm.$emit(
      "apply",
      new Set<string>(),
      new Map<string, NestedColumnMeta>()
    );
    await wrapper.vm.$nextTick();

    expect(filters.toggleFilter).toHaveBeenCalledWith("tags");
    expect(filters.toggleFilter).toHaveBeenCalledWith("status");
  });

  it("renders skeleton for dotted id without nestedColumnMeta entry (pending)", async () => {
    const filters = makeFilters(["publisher.country"]);
    const columns = makeColumns(["publisher"]);
    const wrapper = mount(SidebarContent, {
      props: { filters, columns, schemaId: "test", tableId: "TestTable" },
    });
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".skeleton-stub").exists()).toBe(true);
    expect(wrapper.find('[data-column="publisher.country"]').exists()).toBe(
      false
    );
  });

  it("renders Column (not skeleton) for dotted id once nestedColumnMeta resolves", async () => {
    const nestedMeta = ref(
      new Map<string, NestedColumnMeta>([
        [
          "publisher.country",
          {
            labelParts: ["Publisher", "Country"],
            columnType: "ONTOLOGY",
          },
        ],
      ])
    );
    const filtersWithMeta = makeFilters(
      ["publisher.country"],
      new Map(),
      nestedMeta.value
    );
    const columns = makeColumns(["publisher"]);
    const wrapper = mount(SidebarContent, {
      props: {
        filters: filtersWithMeta,
        columns,
        schemaId: "test",
        tableId: "TestTable",
      },
    });
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".skeleton-stub").exists()).toBe(false);
    expect(wrapper.find('[data-column="publisher.country"]').exists()).toBe(
      true
    );
  });

  describe("Clear / Remove N selected label", () => {
    it("remove/clear action is a <button> element (keyboard-reachable)", async () => {
      const filterStates = new Map<string, IFilterValue>([
        ["col1", { operator: "equals", value: ["termA"] }],
      ]);
      const wrapper = mountSidebarContent(["col1"], filterStates);
      await wrapper.vm.$nextTick();
      const removeBtn = wrapper
        .findAll("button")
        .find((b) => b.text().includes("selected") || b.text() === "Clear");
      expect(removeBtn).toBeDefined();
      expect(removeBtn!.element.tagName.toLowerCase()).toBe("button");
    });

    it("single selection (array of 1) renders 'Remove 1 selected'", async () => {
      const filterStates = new Map<string, IFilterValue>([
        ["col1", { operator: "equals", value: ["termA"] }],
      ]);
      const wrapper = mountSidebarContent(["col1"], filterStates);
      await wrapper.vm.$nextTick();
      expect(wrapper.html()).toContain("Remove 1 selected");
    });

    it("two selections (array of 2) renders 'Remove 2 selected'", async () => {
      const filterStates = new Map<string, IFilterValue>([
        ["col1", { operator: "equals", value: ["termA", "termB"] }],
      ]);
      const wrapper = mountSidebarContent(["col1"], filterStates);
      await wrapper.vm.$nextTick();
      expect(wrapper.html()).toContain("Remove 2 selected");
    });

    it("text filter (string value) renders 'Clear', not 'Remove 1 selected'", async () => {
      const filterStates = new Map<string, IFilterValue>([
        ["col1", { operator: "like", value: "hello" }],
      ]);
      const wrapper = mountSidebarContent(["col1"], filterStates);
      await wrapper.vm.$nextTick();
      expect(wrapper.html()).toContain("Clear");
      expect(wrapper.html()).not.toContain("Remove 1 selected");
    });

    it("range filter ({between, [min,max]}) renders 'Clear', not 'Remove N selected'", async () => {
      const filterStates = new Map<string, IFilterValue>([
        ["col1", { operator: "between", value: [1, 100] }],
      ]);
      const wrapper = mountSidebarContent(["col1"], filterStates);
      await wrapper.vm.$nextTick();
      expect(wrapper.html()).toContain("Clear");
      expect(wrapper.html()).not.toContain("Remove");
    });
  });

  it("nested column with labelParts renders breadcrumb with separator and aria-label", async () => {
    const nestedMeta = new Map<string, NestedColumnMeta>([
      [
        "publisher.country",
        {
          labelParts: ["Publisher", "Country"],
          columnType: "ONTOLOGY",
        },
      ],
    ]);
    const filters = makeFilters(["publisher.country"], new Map(), nestedMeta);
    const columns = makeColumns(["publisher"]);
    const wrapper = mount(SidebarContent, {
      props: { filters, columns, schemaId: "test", tableId: "TestTable" },
    });
    await wrapper.vm.$nextTick();

    const html = wrapper.html();
    expect(html).toContain("Publisher");
    expect(html).toContain("Country");

    const separators = wrapper.findAll('span[aria-hidden="true"]');
    expect(separators.length).toBeGreaterThan(0);
    expect(separators[0]!.text()).toContain("→");

    const labelHost = wrapper.find('[aria-label="Publisher / Country"]');
    expect(labelHost.exists()).toBe(true);
  });

  it("Customize button is clustered left with Filters title (not pushed right)", () => {
    const wrapper = mountSidebarContent(["col1"]);
    const header = wrapper.find(".flex.items-center.justify-between");
    expect(header.exists()).toBe(true);
    const leftCluster = header.find(".flex.items-center.gap-3");
    expect(leftCluster.exists()).toBe(true);
    const customizeBtn = leftCluster
      .findAll("button")
      .find((b) => b.text().toLowerCase().includes("customize"));
    expect(customizeBtn).toBeDefined();
  });
});
