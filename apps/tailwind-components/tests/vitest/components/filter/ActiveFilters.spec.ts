import { describe, it, expect, vi } from "vitest";
import { mount } from "@vue/test-utils";
import ActiveFilters from "../../../../app/components/filter/ActiveFilters.vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../../types/filters";

const mockColumns: IColumn[] = [
  {
    id: "name",
    label: "Name",
    columnType: "STRING",
    table: "Person",
    key: 1,
    required: false,
  },
  {
    id: "age",
    label: "Age",
    columnType: "INT",
    table: "Person",
    key: 1,
    required: false,
  },
  {
    id: "category",
    label: "Category",
    columnType: "REF_ARRAY",
    table: "Person",
    key: 1,
    required: false,
  },
  {
    id: "email",
    label: "Email",
    columnType: "STRING",
    table: "Person",
    key: 1,
    required: false,
  },
];

describe("ActiveFilters", () => {
  it("renders nothing when no filters active", () => {
    const filters = new Map<string, IFilterValue>();
    const wrapper = mount(ActiveFilters, {
      props: { filters, columns: mockColumns },
    });

    expect(wrapper.find("div").exists()).toBe(false);
  });

  it("renders single filter chip", () => {
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "John" }],
    ]);
    const wrapper = mount(ActiveFilters, {
      props: { filters, columns: mockColumns },
    });

    const chips = wrapper.findAll("button[aria-label^='Remove filter']");
    expect(chips).toHaveLength(1);
    expect(chips[0].text()).toContain("Name");
    expect(chips[0].text()).toContain("John");
  });

  it("renders multiple filter chips", () => {
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "John" }],
      ["age", { operator: "equals", value: 25 }],
    ]);
    const wrapper = mount(ActiveFilters, {
      props: { filters, columns: mockColumns },
    });

    const chips = wrapper.findAll("button[aria-label^='Remove filter']");
    expect(chips).toHaveLength(2);
  });

  it("formats like operator", () => {
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "John" }],
    ]);
    const wrapper = mount(ActiveFilters, {
      props: { filters, columns: mockColumns },
    });

    expect(wrapper.text()).toContain("John");
  });

  it("formats equals operator", () => {
    const filters = new Map<string, IFilterValue>([
      ["age", { operator: "equals", value: 25 }],
    ]);
    const wrapper = mount(ActiveFilters, {
      props: { filters, columns: mockColumns },
    });

    expect(wrapper.text()).toContain("25");
  });

  it("formats in operator with multiple values as count", () => {
    const filters = new Map<string, IFilterValue>([
      ["category", { operator: "in", value: [{ name: "A" }, { name: "B" }] }],
    ]);
    const wrapper = mount(ActiveFilters, {
      props: { filters, columns: mockColumns },
    });

    expect(wrapper.text()).toContain("2");
    expect(wrapper.text()).not.toContain("A, B");
  });

  it("formats between operator with min and max", () => {
    const filters = new Map<string, IFilterValue>([
      ["age", { operator: "between", value: [18, 65] }],
    ]);
    const wrapper = mount(ActiveFilters, {
      props: { filters, columns: mockColumns },
    });

    expect(wrapper.text()).toContain("18 - 65");
  });

  it("formats between operator with min only", () => {
    const filters = new Map<string, IFilterValue>([
      ["age", { operator: "between", value: [18, null] }],
    ]);
    const wrapper = mount(ActiveFilters, {
      props: { filters, columns: mockColumns },
    });

    expect(wrapper.text()).toContain("≥ 18");
  });

  it("formats between operator with max only", () => {
    const filters = new Map<string, IFilterValue>([
      ["age", { operator: "between", value: [null, 65] }],
    ]);
    const wrapper = mount(ActiveFilters, {
      props: { filters, columns: mockColumns },
    });

    expect(wrapper.text()).toContain("≤ 65");
  });

  it("formats isNull operator", () => {
    const filters = new Map<string, IFilterValue>([
      ["email", { operator: "isNull", value: true }],
    ]);
    const wrapper = mount(ActiveFilters, {
      props: { filters, columns: mockColumns },
    });

    expect(wrapper.text()).toContain("is empty");
  });

  it("formats notNull operator", () => {
    const filters = new Map<string, IFilterValue>([
      ["email", { operator: "notNull", value: true }],
    ]);
    const wrapper = mount(ActiveFilters, {
      props: { filters, columns: mockColumns },
    });

    expect(wrapper.text()).toContain("has value");
  });

  it("emits remove event when chip clicked", async () => {
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "John" }],
    ]);
    const wrapper = mount(ActiveFilters, {
      props: { filters, columns: mockColumns },
    });

    const chip = wrapper.find("button[aria-label^='Remove filter']");
    await chip.trigger("click");

    expect(wrapper.emitted("remove")).toBeTruthy();
    expect(wrapper.emitted("remove")?.[0]).toEqual(["name"]);
  });

  it("shows clear all button with 2+ filters", () => {
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "John" }],
      ["age", { operator: "equals", value: 25 }],
    ]);
    const wrapper = mount(ActiveFilters, {
      props: { filters, columns: mockColumns },
    });

    const clearAllButton = wrapper.find("button:not([aria-label])");
    expect(clearAllButton.exists()).toBe(true);
    expect(clearAllButton.text()).toBe("Clear all");
  });

  it("does not show clear all button with 1 filter", () => {
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "John" }],
    ]);
    const wrapper = mount(ActiveFilters, {
      props: { filters, columns: mockColumns },
    });

    const buttons = wrapper.findAll("button");
    expect(buttons).toHaveLength(1);
  });

  it("emits clearAll event when clear all clicked", async () => {
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "John" }],
      ["age", { operator: "equals", value: 25 }],
    ]);
    const wrapper = mount(ActiveFilters, {
      props: { filters, columns: mockColumns },
    });

    const clearAllButton = wrapper.find("button:not([aria-label])");
    await clearAllButton.trigger("click");

    expect(wrapper.emitted("clearAll")).toBeTruthy();
  });

  it("has ARIA labels for accessibility", () => {
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "John" }],
    ]);
    const wrapper = mount(ActiveFilters, {
      props: { filters, columns: mockColumns },
    });

    const chip = wrapper.find("button[aria-label]");
    expect(chip.attributes("aria-label")).toBe("Remove filter: Name");
  });

  it("uses column display config label if available", () => {
    const columnsWithDisplayConfig: IColumn[] = [
      {
        id: "name",
        label: "Name",
        columnType: "STRING",
        table: "Person",
        key: 1,
        required: false,
        displayConfig: { label: "Custom Name" },
      },
    ];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "John" }],
    ]);
    const wrapper = mount(ActiveFilters, {
      props: { filters, columns: columnsWithDisplayConfig },
    });

    expect(wrapper.text()).toContain("Custom Name");
  });

  it("shows single value directly for in operator with one item", () => {
    const filters = new Map<string, IFilterValue>([
      ["category", { operator: "in", value: [{ name: "A" }] }],
    ]);
    const wrapper = mount(ActiveFilters, {
      props: { filters, columns: mockColumns },
    });

    expect(wrapper.text()).toContain("A");
  });

  it("shows count for array values with multiple items", () => {
    const filters = new Map<string, IFilterValue>([
      [
        "category",
        {
          operator: "equals",
          value: [{ name: "X" }, { name: "Y" }, { name: "Z" }],
        },
      ],
    ]);
    const wrapper = mount(ActiveFilters, {
      props: { filters, columns: mockColumns },
    });

    expect(wrapper.text()).toContain("3");
  });

  it("renders tooltip values for multi-value filters", () => {
    const filters = new Map<string, IFilterValue>([
      [
        "category",
        {
          operator: "in",
          value: [{ name: "A" }, { name: "B" }, { name: "C" }],
        },
      ],
    ]);
    const wrapper = mount(ActiveFilters, {
      props: { filters, columns: mockColumns },
      global: {
        stubs: {
          VDropdown: {
            template: `
              <div>
                <slot />
                <div class="popper"><slot name="popper" /></div>
              </div>
            `,
          },
        },
      },
    });

    const popperContent = wrapper.find(".popper");
    expect(popperContent.text()).toContain("A");
    expect(popperContent.text()).toContain("B");
    expect(popperContent.text()).toContain("C");
  });
});
