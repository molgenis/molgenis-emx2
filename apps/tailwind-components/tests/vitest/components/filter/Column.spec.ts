import { describe, it, expect, vi } from "vitest";
import { mount } from "@vue/test-utils";
import type { IColumn } from "../../../../../metadata-utils/src/types";
import type { CountedOption } from "../../../../app/utils/fetchCounts";
import type { IFilterValue } from "../../../../types/filters";
import Column from "../../../../app/components/filter/Column.vue";
import Tree from "../../../../app/components/input/Tree.vue";
import FilterRange from "../../../../app/components/filter/Range.vue";
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
  { name: "dogs", label: "Dogs", count: 10 },
  { name: "cats", label: "Cats", count: 5 },
];

function mountColumn(
  column: IColumn,
  options: CountedOption[] = [],
  modelValue: IFilterValue | undefined = undefined,
  loading = false
) {
  return mount(Column, {
    props: { column, options, modelValue, loading },
  });
}

describe("Column", () => {
  describe("countable types", () => {
    it("renders Tree for ONTOLOGY column type", () => {
      const wrapper = mountColumn(ontologyColumn(), sampleOptions);
      const tree = wrapper.findComponent(Tree);
      expect(tree.exists()).toBe(true);
    });

    it("renders Tree for BOOL column type", () => {
      const wrapper = mountColumn(boolColumn(), [
        { name: "true", count: 3 },
        { name: "false", count: 2 },
      ]);
      const tree = wrapper.findComponent(Tree);
      expect(tree.exists()).toBe(true);
    });

    it("passes tree nodes with labels and counts to Tree", () => {
      const wrapper = mountColumn(ontologyColumn(), sampleOptions);
      const tree = wrapper.findComponent(Tree);
      const nodes = tree.props("nodes") as any[];
      expect(nodes).toHaveLength(2);
      expect(nodes[0].name).toBe("dogs");
      expect(nodes[0].label).toContain("Dogs");
      expect(nodes[0].label).toContain("10");
    });

    it("maps modelValue equals array to treeSelection", () => {
      const wrapper = mountColumn(ontologyColumn(), sampleOptions, {
        operator: "equals",
        value: ["dogs"],
      });
      const tree = wrapper.findComponent(Tree);
      expect(tree.props("modelValue")).toEqual(["dogs"]);
    });

    it("passes empty array to Tree when no modelValue", () => {
      const wrapper = mountColumn(ontologyColumn(), sampleOptions);
      const tree = wrapper.findComponent(Tree);
      expect(tree.props("modelValue")).toEqual([]);
    });

    it("emits update:modelValue with equals operator on Tree selection change", async () => {
      const wrapper = mountColumn(ontologyColumn(), sampleOptions);
      const tree = wrapper.findComponent(Tree);
      await tree.vm.$emit("update:modelValue", ["dogs", "cats"]);
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      expect(emitted![0][0]).toEqual({
        operator: "equals",
        value: ["dogs", "cats"],
      });
    });

    it("emits undefined when Tree selection is cleared", async () => {
      const wrapper = mountColumn(ontologyColumn(), sampleOptions, {
        operator: "equals",
        value: ["dogs"],
      });
      const tree = wrapper.findComponent(Tree);
      await tree.vm.$emit("update:modelValue", []);
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      expect(emitted![0][0]).toBeUndefined();
    });

    it("does not render Range or text input for countable types", () => {
      const wrapper = mountColumn(ontologyColumn(), sampleOptions);
      expect(wrapper.findComponent(FilterRange).exists()).toBe(false);
      expect(wrapper.find('input[type="text"]').exists()).toBe(false);
    });

    it("converts children in CountedOption to Tree node children", () => {
      const nested: CountedOption[] = [
        {
          name: "animal",
          label: "Animal",
          count: 15,
          children: [{ name: "dogs", label: "Dogs", count: 10 }],
        },
      ];
      const wrapper = mountColumn(ontologyColumn(), nested);
      const tree = wrapper.findComponent(Tree);
      const nodes = tree.props("nodes") as any[];
      expect(nodes[0].children).toHaveLength(1);
      expect(nodes[0].children[0].name).toBe("dogs");
    });
  });

  describe("range types", () => {
    it("renders FilterRange for INT column type", () => {
      const wrapper = mountColumn(intColumn());
      const range = wrapper.findComponent(FilterRange);
      expect(range.exists()).toBe(true);
    });

    it("does not render Tree or text input for range types", () => {
      const wrapper = mountColumn(intColumn());
      expect(wrapper.findComponent(Tree).exists()).toBe(false);
      expect(wrapper.find('input[type="text"]').exists()).toBe(false);
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
  });

  describe("string-like types", () => {
    it("renders text input for STRING column type", () => {
      const wrapper = mountColumn(stringColumn());
      const input = wrapper.find('input[type="text"]');
      expect(input.exists()).toBe(true);
    });

    it("does not render Tree or FilterRange for string types", () => {
      const wrapper = mountColumn(stringColumn());
      expect(wrapper.findComponent({ name: "Tree" }).exists()).toBe(false);
      expect(wrapper.findComponent({ name: "FilterRange" }).exists()).toBe(
        false
      );
    });

    it("text input shows current like filter value", () => {
      const wrapper = mountColumn(stringColumn(), [], {
        operator: "like",
        value: "fluffy",
      });
      const input = wrapper.find('input[type="text"]');
      expect((input.element as HTMLInputElement).value).toBe("fluffy");
    });

    it("has sr-only label for accessibility", () => {
      const wrapper = mountColumn(stringColumn());
      const label = wrapper.find("label.sr-only");
      expect(label.exists()).toBe(true);
      expect(label.text()).toContain("Name");
    });

    it("emits like filter on text input after debounce", async () => {
      vi.useFakeTimers();
      const wrapper = mountColumn(stringColumn());
      const input = wrapper.find('input[type="text"]');
      const el = input.element as HTMLInputElement;
      el.value = "hello";
      await input.trigger("input");
      vi.advanceTimersByTime(300);
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      expect(emitted![0][0]).toEqual({ operator: "like", value: "hello" });
      vi.useRealTimers();
    });

    it("emits undefined when text input is cleared", async () => {
      vi.useFakeTimers();
      const wrapper = mountColumn(stringColumn(), [], {
        operator: "like",
        value: "hello",
      });
      const input = wrapper.find('input[type="text"]');
      const el = input.element as HTMLInputElement;
      el.value = "";
      await input.trigger("input");
      vi.advanceTimersByTime(300);
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      expect(emitted![0][0]).toBeUndefined();
      vi.useRealTimers();
    });
  });

  describe("composite key support for RADIO", () => {
    it("emits key objects for RADIO with composite key options", async () => {
      const radioCol = {
        id: "status",
        label: "Status",
        columnType: "RADIO",
      } as IColumn;
      const radioOptions: CountedOption[] = [
        { name: "A, 1", count: 5, keyObject: { id: "A", code: "1" } },
        { name: "B, 2", count: 3, keyObject: { id: "B", code: "2" } },
      ];
      const wrapper = mountColumn(radioCol, radioOptions);
      const tree = wrapper.findComponent(Tree);
      await tree.vm.$emit("update:modelValue", ["A, 1"]);
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted![0][0]).toEqual({
        operator: "equals",
        value: [{ id: "A", code: "1" }],
      });
    });

    it("emits plain strings for RADIO with single-key options", async () => {
      const radioCol = {
        id: "status",
        label: "Status",
        columnType: "RADIO",
      } as IColumn;
      const radioOptions: CountedOption[] = [
        { name: "active", count: 5, keyObject: { name: "active" } },
        { name: "inactive", count: 3, keyObject: { name: "inactive" } },
      ];
      const wrapper = mountColumn(radioCol, radioOptions);
      const tree = wrapper.findComponent(Tree);
      await tree.vm.$emit("update:modelValue", ["active"]);
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted![0][0]).toEqual({
        operator: "equals",
        value: ["active"],
      });
    });

    it("extracts display names from composite key objects in modelValue", () => {
      const radioCol = {
        id: "status",
        label: "Status",
        columnType: "RADIO",
      } as IColumn;
      const radioOptions: CountedOption[] = [
        { name: "A, 1", count: 5, keyObject: { id: "A", code: "1" } },
      ];
      const wrapper = mountColumn(radioCol, radioOptions, {
        operator: "equals",
        value: [{ id: "A", code: "1" }],
      });
      const tree = wrapper.findComponent(Tree);
      expect(tree.props("modelValue")).toEqual(["A, 1"]);
    });
  });

  describe("loading state", () => {
    it("shows loading skeleton when loading is true and no options yet", () => {
      const wrapper = mountColumn(ontologyColumn(), [], undefined, true);
      const skeletons = wrapper.findAll('[role="status"]');
      expect(skeletons.length).toBeGreaterThan(0);
    });

    it("does not show loading skeleton when options are present even if loading", () => {
      const wrapper = mountColumn(
        ontologyColumn(),
        sampleOptions,
        undefined,
        true
      );
      const skeletons = wrapper.findAll('[role="status"]');
      expect(skeletons.length).toBe(0);
    });

    it("does not show loading skeleton when not loading", () => {
      const wrapper = mountColumn(ontologyColumn(), [], undefined, false);
      const skeletons = wrapper.findAll('[role="status"]');
      expect(skeletons.length).toBe(0);
    });

    it("renders Tree (not loading skeleton) once options arrive", () => {
      const wrapper = mountColumn(
        ontologyColumn(),
        sampleOptions,
        undefined,
        false
      );
      expect(wrapper.findComponent(Tree).exists()).toBe(true);
      expect(wrapper.findAll('[role="status"]').length).toBe(0);
    });
  });
});
