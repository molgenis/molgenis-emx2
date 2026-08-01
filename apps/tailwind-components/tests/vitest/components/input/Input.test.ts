import { mount } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import Input from "../../../../app/components/Input.vue";

vi.mock("../../../../app/composables/fetchTableMetadata", () => {
  return {
    default: async () => {
      return {
        id: "MockId",
        label: "Mock label",
      };
    },
  };
});

vi.mock("../../../../app/composables/fetchTableData", () => {
  return {
    default: async () => {
      return {};
    },
  };
});

describe("input", () => {
  it("should render a select with select with radio component(single select)", () => {
    const wrapper = mount(Input, {
      props: {
        id: "test-select",
        type: "SELECT",
        modelValue: null,
        limit: 0,
        refSchemaId: "refSchemaId",
        refTableId: "refTableId",
        refLabel: "refLabel",
      },
    });
    expect(wrapper.html()).toContain("select");
    expect(wrapper.html()).toContain("test-select-radio-group");
  });

  it("renders a single-select listbox for MODULE type when values are provided", () => {
    const wrapper = mount(Input, {
      props: {
        id: "test-module",
        type: "MODULE",
        modelValue: null,
        values: ["ModuleA", "ModuleB", "ModuleC"],
      },
    });
    expect(wrapper.find('[role="listbox"]').exists()).toBe(true);
  });
});
