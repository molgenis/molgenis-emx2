import { describe, it, expect, vi } from "vitest";
import { mount } from "@vue/test-utils";
import type { IColumn } from "../../../../../metadata-utils/src/types";
import type { CountedOption } from "../../../../app/utils/fetchCounts";
import type { IFilterValue } from "../../../../types/filters";
import Column from "../../../../app/components/filter/Column.vue";
import FilterTree from "../../../../app/components/filter/Tree.vue";
import FilterRange from "../../../../app/components/filter/Range.vue";
import FilterText from "../../../../app/components/filter/Text.vue";
import FilterBool from "../../../../app/components/filter/Bool.vue";
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
    it.each(["ENUM", "ENUM_ARRAY"] as const)(
      "renders FilterTree for %s column type",
      (columnType) => {
        const col = { id: "status", label: "Status", columnType } as IColumn;
        const wrapper = mountColumn(col, [{ name: "open", count: 0 }]);
        expect(wrapper.findComponent(FilterTree).exists()).toBe(true);
        expect(wrapper.findComponent(FilterText).exists()).toBe(false);
      }
    );

    it("renders FilterTree for ONTOLOGY column type", () => {
      const wrapper = mountColumn(ontologyColumn(), sampleOptions);
      expect(wrapper.findComponent(FilterTree).exists()).toBe(true);
      expect(wrapper.findComponent(FilterRange).exists()).toBe(false);
      expect(wrapper.findComponent(FilterText).exists()).toBe(false);
    });

    it("renders FilterTree for REF column type", () => {
      const col: IColumn = {
        id: "category",
        label: "Category",
        columnType: "REF",
        refTableId: "Category",
      } as IColumn;
      const wrapper = mountColumn(col, sampleOptions);
      expect(wrapper.findComponent(FilterTree).exists()).toBe(true);
      expect(wrapper.findComponent(FilterRange).exists()).toBe(false);
      expect(wrapper.findComponent(FilterText).exists()).toBe(false);
    });

    it("renders FilterTree for REF_ARRAY column type", () => {
      const col: IColumn = {
        id: "tags",
        label: "Tags",
        columnType: "REF_ARRAY",
        refTableId: "Tag",
      } as IColumn;
      const wrapper = mountColumn(col, sampleOptions);
      expect(wrapper.findComponent(FilterTree).exists()).toBe(true);
      expect(wrapper.findComponent(FilterRange).exists()).toBe(false);
      expect(wrapper.findComponent(FilterText).exists()).toBe(false);
    });

    it("renders FilterTree for REFBACK column type", () => {
      const col: IColumn = {
        id: "events",
        label: "Events",
        columnType: "REFBACK",
        refTableId: "Event",
      } as IColumn;
      const wrapper = mountColumn(col, sampleOptions);
      expect(wrapper.findComponent(FilterTree).exists()).toBe(true);
      expect(wrapper.findComponent(FilterRange).exists()).toBe(false);
      expect(wrapper.findComponent(FilterText).exists()).toBe(false);
    });

    it("REF single-key emits plain string array wrapped in equals operator", async () => {
      const col: IColumn = {
        id: "category",
        label: "Category",
        columnType: "REF",
        refTableId: "Category",
      } as IColumn;
      const singleKeyOptions: CountedOption[] = [
        { name: "dogs", keyObject: { name: "dogs" }, count: 5, overlap: 0 },
        { name: "cats", keyObject: { name: "cats" }, count: 3, overlap: 0 },
      ];
      const wrapper = mountColumn(col, singleKeyOptions);
      const tree = wrapper.findComponent(FilterTree);
      await tree.vm.$emit("update:modelValue", ["dogs"]);
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      expect(emitted![0][0]).toEqual({ operator: "equals", value: ["dogs"] });
    });

    it("REF composite-key emits keyObjects in equals operator", async () => {
      const col: IColumn = {
        id: "category",
        label: "Category",
        columnType: "REF",
        refTableId: "Category",
      } as IColumn;
      const compositeKeyOptions: CountedOption[] = [
        {
          name: "A, 1",
          keyObject: { id: "A", code: "1" },
          count: 5,
          overlap: 0,
        },
        {
          name: "B, 2",
          keyObject: { id: "B", code: "2" },
          count: 3,
          overlap: 0,
        },
      ];
      const wrapper = mountColumn(col, compositeKeyOptions);
      const tree = wrapper.findComponent(FilterTree);
      await tree.vm.$emit("update:modelValue", ["A, 1"]);
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      expect(emitted![0][0]).toEqual({
        operator: "equals",
        value: [{ id: "A", code: "1" }],
      });
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

    it("renders FilterTree for SELECT column type", () => {
      const col: IColumn = {
        id: "contactPoint",
        label: "Contact Point",
        columnType: "SELECT",
        refTableId: "Persons",
      } as IColumn;
      const wrapper = mountColumn(col, sampleOptions);
      expect(wrapper.findComponent(FilterTree).exists()).toBe(true);
      expect(wrapper.findComponent(FilterRange).exists()).toBe(false);
      expect(wrapper.findComponent(FilterText).exists()).toBe(false);
    });

    it("renders FilterTree for MULTISELECT column type", () => {
      const col: IColumn = {
        id: "tags",
        label: "Tags",
        columnType: "MULTISELECT",
        refTableId: "Tag",
      } as IColumn;
      const wrapper = mountColumn(col, sampleOptions);
      expect(wrapper.findComponent(FilterTree).exists()).toBe(true);
      expect(wrapper.findComponent(FilterRange).exists()).toBe(false);
      expect(wrapper.findComponent(FilterText).exists()).toBe(false);
    });
  });

  describe("BOOL and BOOL_ARRAY — three-choice boolean input", () => {
    it.each(["BOOL", "BOOL_ARRAY"] as const)(
      "%s renders Yes, No and Not set checkboxes, not the counted tree",
      (columnType) => {
        const wrapper = mountColumn({ ...boolColumn(), columnType });
        expect(wrapper.findComponent(FilterBool).exists()).toBe(true);
        expect(wrapper.findComponent(FilterTree).exists()).toBe(false);
        expect(wrapper.findComponent(FilterText).exists()).toBe(false);
        expect(wrapper.findAll('input[type="checkbox"]')).toHaveLength(3);
        expect(wrapper.text()).toContain("Yes");
        expect(wrapper.text()).toContain("No");
        expect(wrapper.text()).toContain("Not set");
      }
    );

    it("checks the choices held by the modelValue", () => {
      const wrapper = mountColumn(boolColumn(), [], {
        operator: "equals",
        value: ["false", "_null_"],
      });
      const checked = wrapper
        .findAll('input[type="checkbox"]')
        .map((box) => (box.element as HTMLInputElement).checked);
      expect(checked).toEqual([false, true, true]);
    });

    it("emits equals with the chosen values in Yes, No, Not set order", async () => {
      const wrapper = mountColumn(boolColumn(), [], {
        operator: "equals",
        value: ["_null_"],
      });
      await wrapper.findAll('input[type="checkbox"]')[1]!.setValue(true);
      expect(wrapper.emitted("update:modelValue")!.at(-1)![0]).toEqual({
        operator: "equals",
        value: ["false", "_null_"],
      });
    });

    it("emits undefined when nothing is chosen", async () => {
      const wrapper = mountColumn(boolColumn(), [], {
        operator: "equals",
        value: ["true"],
      });
      await wrapper.findAll('input[type="checkbox"]')[0]!.setValue(false);
      expect(wrapper.emitted("update:modelValue")!.at(-1)![0]).toBeUndefined();
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
        value: [[10, 50]],
      });
      const range = wrapper.findComponent(FilterRange);
      expect(range.props("modelValue")).toEqual([[10, 50]]);
    });

    it("passes no ranges to FilterRange when no modelValue", () => {
      const wrapper = mountColumn(intColumn());
      const range = wrapper.findComponent(FilterRange);
      expect(range.props("modelValue")).toEqual([]);
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
        value: [[10, 50]],
      });
      const range = wrapper.findComponent(FilterRange);
      await range.vm.$emit("update:modelValue", [[null, null]]);
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      expect(emitted![0][0]).toBeUndefined();
    });

    it("emits between filter when range value set", async () => {
      const wrapper = mountColumn(intColumn());
      const range = wrapper.findComponent(FilterRange);
      await range.vm.$emit("update:modelValue", [[5, 20]]);
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      expect(emitted![0][0]).toEqual({
        operator: "between",
        value: [[5, 20]],
      });
    });

    it("emits only the ranges that have a side, dropping empty rows", async () => {
      const wrapper = mountColumn(intColumn());
      await wrapper.findComponent(FilterRange).vm.$emit("update:modelValue", [
        [5, 20],
        [null, null],
        [null, 3],
      ]);
      expect(wrapper.emitted("update:modelValue")![0]![0]).toEqual({
        operator: "between",
        value: [
          [5, 20],
          [null, 3],
        ],
      });
    });

    it("shows one min and max pair per range, with add and remove from two rows", async () => {
      const wrapper = mountColumn(intColumn(), [], {
        operator: "between",
        value: [[10, 20]],
      });
      const named = (name: string) =>
        wrapper.findAll("button").filter((button) => button.text() === name);
      expect(wrapper.findAllComponents(GenericInput)).toHaveLength(2);
      expect(named("Remove item")).toHaveLength(0);

      await named("Add an additional item")[0]!.trigger("click");
      expect(wrapper.findAllComponents(GenericInput)).toHaveLength(4);
      expect(named("Remove item")).toHaveLength(2);

      await named("Remove item")[1]!.trigger("click");
      expect(wrapper.findAllComponents(GenericInput)).toHaveLength(2);
      expect(named("Remove item")).toHaveLength(0);
    });

    it("empties the rows when the filter is removed from outside", async () => {
      const wrapper = mountColumn(intColumn(), [], {
        operator: "between",
        value: [
          [10, 20],
          [30, 40],
        ],
      });
      expect(wrapper.findAllComponents(GenericInput)).toHaveLength(4);
      await wrapper.setProps({ modelValue: undefined });
      expect(wrapper.findAllComponents(GenericInput)).toHaveLength(2);
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

    it("renders a text row for STRING column type", () => {
      const wrapper = mountColumn(stringColumn());
      expect(wrapper.findAllComponents(GenericInput)).toHaveLength(1);
      expect(wrapper.findAllComponents(GenericInput)[0]!.props("type")).toBe(
        "STRING"
      );
    });
  });

  describe("empty state — forwarded to FilterTree", () => {
    it("does not render empty-state message for range/text filters", () => {
      const rangeWrapper = mountColumn(dateColumn(), [], undefined, false);
      expect(rangeWrapper.findComponent(FilterRange).exists()).toBe(true);

      const stringWrapper = mountColumn(stringColumn(), [], undefined, false);
      expect(stringWrapper.findComponent(FilterText).exists()).toBe(true);
    });
  });
});
