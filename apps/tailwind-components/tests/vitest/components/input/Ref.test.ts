import { mount } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import InputRef from "../../../../app/components/input/Ref.vue";

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
      return {
        rows: [
          { id: "tweety", name: "tweety" },
          { id: "looney", name: "looney" },
          { id: "daffy", name: "daffy" },
          { id: "sylvester", name: "sylvester" },
          { id: "elmer", name: "elmer" },
          { id: "bugs", name: "bugs" },
        ],
        count: 6,
      };
    },
  };
});

vi.mock("../../../../app/composables/fetchRowPrimaryKey", () => {
  return {
    default: async (row: any) => {
      return row.id;
    },
  };
});

const wrapper = mount(InputRef, {
  props: {
    id: "test-ref",
    refTableId: "test-table",
    refSchemaId: "test-schema",
    refLabel: "${name}",
    isArray: false,
    limit: 5,
    modelValue: { id: "tweety", name: "tweety" },
  },
});

describe("input ref", () => {
  it("deselect on non-array version should yield empty array ", () => {
    expect(wrapper.exists()).toBe(true);
    wrapper.find("button").trigger("click");
    expect(wrapper.emitted("update:modelValue")).toEqual([[null]]);
    setTimeout(() => {
      //timeout because of debounce
      expect(wrapper.emitted("blur")).toBeDefined();
    }, 100);
  });
});
