import { mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it } from "vitest";
import CellEMX2 from "../../../../app/components/table/CellEMX2.vue";
import ShowMore from "../../../../app/components/ShowMore.vue";
import ValueEMX2 from "../../../../app/components/value/EMX2.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

const arrayColumn: IColumn = {
  id: "tags",
  label: "Tags",
  columnType: "STRING_ARRAY",
};

const eight = Array.from({ length: 8 }, (_, index) => `Tag ${index + 1}`);

beforeEach(() => {
  (globalThis as any).ResizeObserver = class {
    observe() {}
    disconnect() {}
  };
});

describe("table/CellEMX2.vue", () => {
  it("routes its value through the same component the detail view uses", () => {
    const wrapper = mount(CellEMX2, {
      props: { metadata: arrayColumn, data: eight },
    });

    expect(wrapper.findComponent(ValueEMX2).exists()).toBe(true);
    expect(wrapper.text()).toContain("Tag 1");
    expect(wrapper.text()).toContain("Tag 8");
  });

  it("does not collapse the value, because the cell bounds it itself", () => {
    const wrapper = mount(CellEMX2, {
      props: { metadata: arrayColumn, data: eight },
    });

    expect(wrapper.findComponent(ValueEMX2).props("collapsible")).toBe(false);
    expect(wrapper.findComponent(ShowMore).props("collapsible")).toBe(false);
  });

  it("keeps far fewer values in the DOM than a record page does", () => {
    const many = Array.from({ length: 500 }, (_, i) => `Tag ${i + 1}`);
    const wrapper = mount(CellEMX2, {
      props: { metadata: arrayColumn, data: many },
    });

    expect(wrapper.text()).toContain("Tag 10");
    expect(wrapper.text()).not.toContain("Tag 11");
  });

  it("renders an empty cell for a missing value", () => {
    const wrapper = mount(CellEMX2, {
      props: { metadata: arrayColumn, data: null },
    });

    expect(wrapper.findComponent(ValueEMX2).exists()).toBe(false);
    expect(wrapper.text().trim()).toBe("");
  });
});
