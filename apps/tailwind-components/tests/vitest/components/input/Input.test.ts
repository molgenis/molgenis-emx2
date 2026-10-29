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

  it.each([
    ["ENUM with 2 values", "ENUM", ["a", "b"], 'input[type="radio"]', 2],
    [
      "ENUM_ARRAY with 3 values",
      "ENUM_ARRAY",
      ["a", "b", "c"],
      'input[type="checkbox"]',
      3,
    ],
  ] as const)(
    "routes %s to its widget",
    (_title, type, values, selector, expected) => {
      const wrapper = mount(Input, {
        props: {
          id: "test-enum",
          type,
          modelValue: null,
          allowedValues: [...values],
        },
      });
      expect(wrapper.findAll(selector).length).toEqual(expected);
    }
  );
});
