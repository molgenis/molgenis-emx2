import { enableAutoUnmount, flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
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
      return {
        rows: [
          { id: "tweety", name: "tweety" },
          { id: "daffy", name: "daffy" },
          { id: "bugs", name: "bugs" },
        ],
        count: 3,
      };
    },
  };
});

enableAutoUnmount(afterEach);

const refProps = {
  modelValue: null,
  refSchemaId: "refSchemaId",
  refTableId: "refTableId",
  refLabel: "${name}",
};

describe("input", () => {
  it("renders SELECT as a single listbox even for a short list", async () => {
    const wrapper = mount(Input, {
      props: { id: "test-select", type: "SELECT", ...refProps },
    });
    await flushPromises();

    expect(wrapper.find('button[role="combobox"]#test-select').exists()).toBe(
      true
    );
    expect(wrapper.findAll('li[role="option"]')).toHaveLength(3);
    expect(
      wrapper.find('ul[role="listbox"]').attributes("aria-multiselectable")
    ).toBeUndefined();
  });

  it("renders MULTISELECT as a multiple listbox", async () => {
    const wrapper = mount(Input, {
      props: { id: "test-multi", type: "MULTISELECT", ...refProps },
    });
    await flushPromises();

    expect(wrapper.find('button[role="combobox"]#test-multi').exists()).toBe(
      true
    );
    expect(
      wrapper.find('ul[role="listbox"]').attributes("aria-multiselectable")
    ).toBe("true");
  });

  it.each([
    ["REF", "#test-ref-radio-group"],
    ["REF_ARRAY", "#test-ref-checkbox-group"],
  ] as const)(
    "renders %s below the limit as a group",
    async (type, selector) => {
      const wrapper = mount(Input, {
        props: { id: "test-ref", type, ...refProps },
      });
      await flushPromises();

      expect(wrapper.find(selector).exists()).toBe(true);
      expect(wrapper.find('button[role="combobox"]').exists()).toBe(false);
    }
  );

  it.each([
    ["ENUM with 2 values", "ENUM", ["a", "b"], 2, undefined],
    ["ENUM_ARRAY with 3 values", "ENUM_ARRAY", ["a", "b", "c"], 3, "true"],
  ] as const)(
    "routes %s to its listbox",
    (_title, type, values, expected, multiselectable) => {
      const wrapper = mount(Input, {
        props: {
          id: "test-enum",
          type,
          modelValue: null,
          allowedValues: [...values],
        },
      });

      expect(wrapper.find('button[role="combobox"]#test-enum').exists()).toBe(
        true
      );
      expect(wrapper.findAll('li[role="option"]')).toHaveLength(expected);
      expect(
        wrapper.find('ul[role="listbox"]').attributes("aria-multiselectable")
      ).toBe(multiselectable);
    }
  );
});
