import { mount } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import Field from "../../../../app/components/Field.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

vi.mock("../../../../app/composables/fetchTableMetadata", () => ({
  default: async () => ({ id: "MockId", label: "Mock label" }),
}));

vi.mock("../../../../app/composables/fetchTableData", () => ({
  default: async () => ({}),
}));

describe("Field.vue values threading", () => {
  it("renders a listbox select widget for ENUM column when values are provided", () => {
    const wrapper = mount(Field, {
      props: {
        id: "test-enum-field",
        type: "ENUM" as IColumn["columnType"],
        modelValue: null,
        values: ["MALE", "FEMALE", "UNKNOWN"],
        label: "Sex",
      },
    });
    expect(wrapper.find('[role="listbox"]').exists()).toBe(true);
  });

  it("renders checkboxes for ENUM_ARRAY column when values are provided", () => {
    const wrapper = mount(Field, {
      props: {
        id: "test-enum-array-field",
        type: "ENUM_ARRAY" as IColumn["columnType"],
        modelValue: null,
        values: ["TAG_A", "TAG_B", "TAG_C"],
        label: "Tags",
      },
    });
    expect(wrapper.findAll('input[type="checkbox"]').length).toBeGreaterThan(0);
  });

  it("renders a listbox select widget for MODULE column when values are provided", () => {
    const wrapper = mount(Field, {
      props: {
        id: "test-module-field",
        type: "MODULE" as IColumn["columnType"],
        modelValue: null,
        values: ["ModuleA", "ModuleB"],
        label: "Module",
      },
    });
    expect(wrapper.find('[role="listbox"]').exists()).toBe(true);
  });

  it("renders checkboxes for MODULE_ARRAY column when values are provided", () => {
    const wrapper = mount(Field, {
      props: {
        id: "test-module-array-field",
        type: "MODULE_ARRAY" as IColumn["columnType"],
        modelValue: null,
        values: ["ModuleX", "ModuleY", "ModuleZ"],
        label: "Subgroups",
      },
    });
    expect(wrapper.findAll('input[type="checkbox"]').length).toBeGreaterThan(0);
  });
});
