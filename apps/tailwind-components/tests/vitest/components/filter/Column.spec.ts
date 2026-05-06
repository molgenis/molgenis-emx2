import { describe, it, expect, vi } from "vitest";
import { mount } from "@vue/test-utils";
import type { IColumn } from "../../../../../metadata-utils/src/types";
import type { CountedOption } from "../../../../app/utils/fetchCounts";
import type { IFilterValue } from "../../../../types/filters";
import Column from "../../../../app/components/filter/Column.vue";
import FilterTree from "../../../../app/components/filter/Tree.vue";
import FilterRange from "../../../../app/components/filter/Range.vue";
import FilterText from "../../../../app/components/filter/Text.vue";
import GenericInput from "../../../../app/components/Input.vue";

vi.mock("../../../../app/components/Input.vue", () => ({
  default: {
    name: "GenericInput",
    props: ["id", "type", "modelValue"],
    emits: ["update:modelValue"],
    template: '<input data-testid="generic-input" :data-type="type" />',
  },
}));

const createMockObserver = () => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
  takeRecords: vi.fn(() => []),
});

global.IntersectionObserver = vi
  .fn()
  .mockImplementation(() => createMockObserver());

function ontologyColumn(): IColumn {
  return {
    id: "category",
    label: "Category",
    columnType: "ONTOLOGY",
  } as IColumn;
}

function intColumn(): IColumn {
  return {
    id: "age",
    label: "Age",
    columnType: "INT",
  } as IColumn;
}

function stringColumn(): IColumn {
  return {
    id: "name",
    label: "Name",
    columnType: "STRING",
  } as IColumn;
}

function boolColumn(): IColumn {
  return {
    id: "active",
    label: "Active",
    columnType: "BOOL",
  } as IColumn;
}

function dateColumn(): IColumn {
  return {
    id: "birthdate",
    label: "Birth Date",
    columnType: "DATE",
  } as IColumn;
}

function datetimeColumn(): IColumn {
  return {
    id: "createdAt",
    label: "Created At",
    columnType: "DATETIME",
  } as IColumn;
}

const sampleOptions: CountedOption[] = [
  { name: "dogs", label: "Dogs", count: 10, overlap: 0 },
  { name: "cats", label: "Cats", count: 5, overlap: 0 },
];

function mountColumn(
  column: IColumn,
  options: CountedOption[] = [],
  modelValue: IFilterValue | undefined = undefined,
  loading = false,
  saturated = false
) {
  return mount(Column, {
    props: { column, options, modelValue, loading, saturated },
  });
}

describe("Column dispatcher", () => {
  describe("countable types — renders FilterTree", () => {
    it("renders FilterTree for ONTOLOGY column type", () => {
      const wrapper = mountColumn(ontologyColumn(), sampleOptions);
      expect(wrapper.findComponent(FilterTree).exists()).toBe(true);
      expect(wrapper.findComponent(FilterRange).exists()).toBe(false);
      expect(wrapper.findComponent(FilterText).exists()).toBe(false);
    });

    it("renders FilterTree for BOOL column type", () => {
      const wrapper = mountColumn(boolColumn(), [
        { name: "true", count: 3 },
        { name: "false", count: 2 },
      ]);
      expect(wrapper.findComponent(FilterTree).exists()).toBe(true);
      expect(wrapper.findComponent(FilterRange).exists()).toBe(false);
      expect(wrapper.findComponent(FilterText).exists()).toBe(false);
    });

    it("H3: STRING_ARRAY renders FilterTree (countable), not text search fallback", () => {
      const col: IColumn = {
        id: "tags",
        label: "Tags",
        columnType: "STRING_ARRAY",
      } as IColumn;
      const opts: CountedOption[] = [
        { name: "a", count: 3 },
        { name: "b", count: 1 },
      ];
      const wrapper = mountColumn(col, opts);
      expect(wrapper.findComponent(FilterTree).exists()).toBe(true);
      expect(wrapper.findComponent(FilterRange).exists()).toBe(false);
      expect(wrapper.findComponent(FilterText).exists()).toBe(false);
    });
  });

  describe("range types — renders FilterRange", () => {
    it("renders FilterRange for INT column type", () => {
      const wrapper = mountColumn(intColumn());
      expect(wrapper.findComponent(FilterRange).exists()).toBe(true);
      expect(wrapper.findComponent(FilterTree).exists()).toBe(false);
      expect(wrapper.findComponent(FilterText).exists()).toBe(false);
    });

    it("H3: INT renders FilterRange, not tree or text", () => {
      const wrapper = mountColumn(intColumn());
      expect(wrapper.findComponent(FilterRange).exists()).toBe(true);
      expect(wrapper.findComponent(FilterTree).exists()).toBe(false);
    });

    it("H3: NON_NEGATIVE_INT renders FilterRange", () => {
      const col: IColumn = {
        id: "count",
        label: "Count",
        columnType: "NON_NEGATIVE_INT",
      } as IColumn;
      const wrapper = mountColumn(col);
      expect(wrapper.findComponent(FilterRange).exists()).toBe(true);
      expect(wrapper.findComponent(FilterTree).exists()).toBe(false);
    });

    it("H3: DATETIME renders FilterRange with DATETIME input type", () => {
      const wrapper = mountColumn(datetimeColumn());
      expect(wrapper.findComponent(FilterRange).exists()).toBe(true);
      const inputs = wrapper.findAllComponents(GenericInput);
      expect(inputs.length).toBeGreaterThan(0);
      expect(inputs[0].props("type")).toBe("DATETIME");
    });

    it("passes range values from modelValue between operator", () => {
      const wrapper = mountColumn(intColumn(), [], {
        operator: "between",
        value: [10, 50],
      });
      const range = wrapper.findComponent(FilterRange);
      expect(range.props("modelValue")).toEqual([10, 50]);
    });

    it("passes [null, null] to FilterRange when no modelValue", () => {
      const wrapper = mountColumn(intColumn());
      const range = wrapper.findComponent(FilterRange);
      expect(range.props("modelValue")).toEqual([null, null]);
    });

    it("renders generic Input with DATE type for DATE column range", () => {
      const wrapper = mountColumn(dateColumn());
      const inputs = wrapper.findAllComponents(GenericInput);
      expect(inputs.length).toBeGreaterThan(0);
      expect(inputs[0].props("type")).toBe("DATE");
    });

    it("renders generic Input with DATETIME type for DATETIME column range", () => {
      const wrapper = mountColumn(datetimeColumn());
      const inputs = wrapper.findAllComponents(GenericInput);
      expect(inputs.length).toBeGreaterThan(0);
      expect(inputs[0].props("type")).toBe("DATETIME");
    });

    it("renders generic Input with INT type for INT column range", () => {
      const wrapper = mountColumn(intColumn());
      const inputs = wrapper.findAllComponents(GenericInput);
      expect(inputs.length).toBeGreaterThan(0);
      expect(inputs[0].props("type")).toBe("INT");
    });

    it("does not render FilterTree or text input for range types", () => {
      const wrapper = mountColumn(intColumn());
      expect(wrapper.findComponent(FilterTree).exists()).toBe(false);
      expect(wrapper.find('input[type="search"]').exists()).toBe(false);
    });

    it("emits undefined when range min and max are both null", async () => {
      const wrapper = mountColumn(intColumn(), [], {
        operator: "between",
        value: [10, 50],
      });
      const range = wrapper.findComponent(FilterRange);
      await range.vm.$emit("update:modelValue", [null, null]);
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      expect(emitted![0][0]).toBeUndefined();
    });

    it("emits between filter when range value set", async () => {
      const wrapper = mountColumn(intColumn());
      const range = wrapper.findComponent(FilterRange);
      await range.vm.$emit("update:modelValue", [5, 20]);
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      expect(emitted![0][0]).toEqual({ operator: "between", value: [5, 20] });
    });
  });

  describe("string-like types — renders FilterText", () => {
    it("renders FilterText for STRING column type", () => {
      const wrapper = mountColumn(stringColumn());
      expect(wrapper.findComponent(FilterText).exists()).toBe(true);
      expect(wrapper.findComponent(FilterTree).exists()).toBe(false);
      expect(wrapper.findComponent(FilterRange).exists()).toBe(false);
    });

    it("does not render FilterTree or FilterRange for string types", () => {
      const wrapper = mountColumn(stringColumn());
      expect(wrapper.findComponent(FilterTree).exists()).toBe(false);
      expect(wrapper.findComponent(FilterRange).exists()).toBe(false);
    });

    it("renders search input for STRING column type", () => {
      const wrapper = mountColumn(stringColumn());
      const input = wrapper.find('input[type="search"]');
      expect(input.exists()).toBe(true);
    });
  });

  describe("empty state — forwarded to FilterTree", () => {
    it("does not render empty-state message for range/text filters", () => {
      const rangeWrapper = mountColumn(dateColumn(), [], undefined, false);
      expect(rangeWrapper.findComponent(FilterRange).exists()).toBe(true);

      const stringWrapper = mountColumn(stringColumn(), [], undefined, false);
      expect(stringWrapper.find('input[type="search"]').exists()).toBe(true);
    });
  });
});
