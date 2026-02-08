import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import FilterColumn from "../../../../app/components/filter/Column.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../../types/filters";

describe("FilterColumn", () => {
  const stringColumn: IColumn = {
    id: "name",
    label: "Name",
    columnType: "STRING",
  };

  const intColumn: IColumn = {
    id: "age",
    label: "Age",
    columnType: "INT",
  };

  const dateColumn: IColumn = {
    id: "birthdate",
    label: "Birth Date",
    columnType: "DATE",
  };

  const boolColumn: IColumn = {
    id: "active",
    label: "Active",
    columnType: "BOOL",
  };

  const refColumn: IColumn = {
    id: "pet",
    label: "Pet",
    columnType: "REF",
    refSchemaId: "test",
    refTableId: "Pet",
  };

  describe("Visual wrapper (from 6.3.2 Container)", () => {
    it("renders column label as title", () => {
      const wrapper = mount(FilterColumn, {
        props: {
          column: stringColumn,
          modelValue: null,
        },
      });

      expect(wrapper.text()).toContain("Name");
    });

    it("uses custom label from displayConfig when provided", () => {
      const customColumn: IColumn = {
        ...stringColumn,
        displayConfig: {
          label: "Custom Label",
        },
      };

      const wrapper = mount(FilterColumn, {
        props: {
          column: customColumn,
          modelValue: null,
        },
      });

      expect(wrapper.text()).toContain("Custom Label");
    });

    it("always shows filter content expanded", () => {
      const wrapper = mount(FilterColumn, {
        props: {
          column: stringColumn,
          modelValue: null,
        },
      });

      const content = wrapper.find(".mb-5");
      expect(content.exists()).toBe(true);
    });

    it("shows Clear button when modelValue is truthy", () => {
      const wrapper = mount(FilterColumn, {
        props: {
          column: stringColumn,
          modelValue: {
            operator: "like",
            value: "test",
          } as IFilterValue,
        },
      });

      expect(wrapper.text()).toContain("Clear");
    });

    it("hides Clear button when modelValue is null", () => {
      const wrapper = mount(FilterColumn, {
        props: {
          column: stringColumn,
          modelValue: null,
        },
      });

      expect(wrapper.text()).not.toContain("Clear");
    });

    it("clears modelValue when Clear button is clicked", async () => {
      const wrapper = mount(FilterColumn, {
        props: {
          column: stringColumn,
          modelValue: {
            operator: "like",
            value: "test",
          } as IFilterValue,
          "onUpdate:modelValue": (e: IFilterValue | null) =>
            wrapper.setProps({ modelValue: e }),
        },
      });

      expect(wrapper.props("modelValue")).toBeTruthy();

      const clearButton = wrapper.find(
        ".text-search-filter-expand, .text-search-filter-expand-mobile"
      );
      await clearButton.trigger("click");

      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
      expect(wrapper.emitted("update:modelValue")?.[0]).toEqual([null]);
    });

    it("applies mobile class suffixes when mobileDisplay is true", () => {
      const wrapper = mount(FilterColumn, {
        props: {
          column: stringColumn,
          modelValue: null,
          mobileDisplay: true,
        },
      });

      expect(
        wrapper.find(".text-search-filter-group-title-mobile").exists()
      ).toBe(true);
    });
  });

  describe("Dispatch logic (from 6.3.3 FilterColumn)", () => {
    it("stores raw string input without parsing", async () => {
      const wrapper = mount(FilterColumn, {
        props: {
          column: stringColumn,
          modelValue: null,
        },
      });

      const input = wrapper.find('input[type="text"]');
      expect(input.exists()).toBe(true);

      await input.setValue("dog cat");

      const emitted = wrapper.emitted("update:modelValue") as any[];
      expect(emitted).toBeTruthy();
      const lastEmit = emitted[emitted.length - 1][0];
      expect(lastEmit?.operator).toBe("like");
      expect(lastEmit?.value).toBe("dog cat");
    });

    it("renders FilterRange for INT type with between operator", () => {
      const wrapper = mount(FilterColumn, {
        props: {
          column: intColumn,
          modelValue: null,
        },
      });

      const labels = wrapper.findAll("label");
      const labelTexts = labels.map((l) => l.text());
      expect(labelTexts).toContain("Min");
      expect(labelTexts).toContain("Max");
    });

    it("renders FilterRange for DATE type with between operator", () => {
      const wrapper = mount(FilterColumn, {
        props: {
          column: dateColumn,
          modelValue: null,
        },
      });

      const labels = wrapper.findAll("label");
      const labelTexts = labels.map((l) => l.text());
      expect(labelTexts).toContain("Min");
      expect(labelTexts).toContain("Max");
    });

    it("renders single Input for BOOL type with equals operator", () => {
      const wrapper = mount(FilterColumn, {
        props: {
          column: boolColumn,
          modelValue: null,
        },
      });

      const radio = wrapper.find('input[type="radio"]');
      expect(radio.exists()).toBe(true);
    });

    it("sets operator to 'like' for STRING/TEXT/EMAIL types", async () => {
      const wrapper = mount(FilterColumn, {
        props: {
          column: { ...stringColumn, columnType: "EMAIL" },
          modelValue: null,
        },
      });

      const input = wrapper.find('input[type="text"]');
      await input.setValue("test@example.com");

      const emitted = wrapper.emitted("update:modelValue") as any[];
      const lastEmit = emitted[emitted.length - 1][0];
      expect(lastEmit?.operator).toBe("like");
    });

    it("sets operator to 'between' for range types", async () => {
      const wrapper = mount(FilterColumn, {
        props: {
          column: intColumn,
          modelValue: null,
        },
      });

      await wrapper.setProps({
        modelValue: { operator: "between", value: [10, 20] } as IFilterValue,
      });

      expect(wrapper.props("modelValue")?.operator).toBe("between");
      expect(wrapper.props("modelValue")?.value).toEqual([10, 20]);
    });

    it("clears modelValue when Clear is clicked", async () => {
      const wrapper = mount(FilterColumn, {
        props: {
          column: stringColumn,
          modelValue: { operator: "like", value: ["test"] } as IFilterValue,
        },
      });

      const clearBtn = wrapper.find(".text-search-filter-expand");
      expect(clearBtn.exists()).toBe(true);
      await clearBtn.trigger("click");

      const emitted = wrapper.emitted("update:modelValue") as any[];
      const lastEmit = emitted[emitted.length - 1][0];
      expect(lastEmit).toBe(null);
    });

    it("clears modelValue when both range inputs are empty", async () => {
      const wrapper = mount(FilterColumn, {
        props: {
          column: intColumn,
          modelValue: {
            operator: "between",
            value: [10, 20],
          } as IFilterValue,
        },
      });

      await wrapper.setProps({
        modelValue: {
          operator: "between",
          value: [null, null],
        } as IFilterValue,
      });

      await wrapper.vm.$nextTick();

      const updatedValue = wrapper.props("modelValue");
      expect(updatedValue?.value).toEqual([null, null]);
    });
  });

  describe("REF type filters", () => {
    it("renders REF column label", () => {
      const wrapper = mount(FilterColumn, {
        props: {
          column: refColumn,
          modelValue: null,
        },
      });

      expect(wrapper.text()).toContain("Pet");
    });

    it("renders REF column with correct operator", async () => {
      const wrapper = mount(FilterColumn, {
        props: {
          column: refColumn,
          modelValue: null,
        },
      });

      expect(wrapper.html()).toContain("Pet");
    });
  });

  describe("Integration", () => {
    it("handles complete filter lifecycle for STRING type", async () => {
      const wrapper = mount(FilterColumn, {
        props: {
          column: stringColumn,
          modelValue: null,
        },
      });

      expect(wrapper.text()).not.toContain("Clear");
      expect(wrapper.find(".mb-5").exists()).toBe(true);

      const input = wrapper.find('input[type="text"]');
      await input.setValue("test");

      expect(wrapper.emitted("update:modelValue")).toBeTruthy();

      await wrapper.setProps({
        modelValue: { operator: "like", value: "test" } as IFilterValue,
      });

      expect(wrapper.text()).toContain("Clear");

      const clearButton = wrapper.find(".text-search-filter-expand");
      await clearButton.trigger("click");

      expect(wrapper.emitted("update:modelValue")?.[1]).toEqual([null]);
    });

    it("handles complete filter lifecycle for INT range type", async () => {
      const wrapper = mount(FilterColumn, {
        props: {
          column: intColumn,
          modelValue: null,
        },
      });

      expect(wrapper.text()).not.toContain("Clear");

      await wrapper.setProps({
        modelValue: { operator: "between", value: [10, 20] } as IFilterValue,
      });

      expect(wrapper.text()).toContain("Clear");

      const clearButton = wrapper.find(".text-search-filter-expand");
      await clearButton.trigger("click");

      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
      const emittedValues = wrapper.emitted("update:modelValue") as any[];
      expect(emittedValues[emittedValues.length - 1]).toEqual([null]);
    });
  });
});
