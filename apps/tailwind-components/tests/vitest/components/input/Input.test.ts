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
  it("preserves the Boolean clear button default", () => {
    const wrapper = mount(Input, {
      props: {
        id: "test-boolean",
        type: "BOOL",
        modelValue: null,
      },
    });

    expect(wrapper.find("#test-boolean-radio-group-clear").exists()).toBe(true);
  });

  it("allows disabling the Boolean clear button", () => {
    const wrapper = mount(Input, {
      props: {
        id: "test-boolean",
        type: "BOOL",
        modelValue: null,
        showClearButton: false,
      },
    });

    expect(wrapper.find("#test-boolean-radio-group-clear").exists()).toBe(false);
  });

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
});
