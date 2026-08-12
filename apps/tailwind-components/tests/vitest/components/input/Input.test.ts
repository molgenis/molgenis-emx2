import { flushPromises, mount } from "@vue/test-utils";
import { defineComponent, h, Suspense } from "vue";
import { describe, expect, it, vi } from "vitest";
import Input from "../../../../app/components/Input.vue";
import type { CellValueType } from "../../../../../metadata-utils/src/types";

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

function mountInputWithinSuspense(inputProps: Record<string, unknown>) {
  return mount(
    defineComponent({
      setup() {
        return () =>
          h(Suspense, null, { default: () => h(Input, inputProps as never) });
      },
    })
  );
}

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

  it("should render a PARTS column with the same refback input as a REFBACK column", async () => {
    const sharedProps = {
      id: "test-refback",
      modelValue: null,
      refSchemaId: "refSchemaId",
      refTableId: "refTableId",
      refLabel: "${name}",
      refBackColumn: "refBackColumn",
    };
    const refbackWrapper = mountInputWithinSuspense({
      ...sharedProps,
      type: "REFBACK",
    });
    const partsWrapper = mountInputWithinSuspense({
      ...sharedProps,
      type: "PARTS",
    });
    await flushPromises();

    expect(refbackWrapper.text()).toContain(
      "can only be filled in after you have saved"
    );
    expect(partsWrapper.html()).toEqual(refbackWrapper.html());
  });

  it("should render the column type itself when no input component matches the type", () => {
    const wrapper = mount(Input, {
      props: {
        id: "test-unmapped",
        type: "NOT_A_KNOWN_COLUMN_TYPE" as CellValueType,
        modelValue: null,
      },
    });
    expect(wrapper.text()).toContain("NOT_A_KNOWN_COLUMN_TYPE");
  });
});
