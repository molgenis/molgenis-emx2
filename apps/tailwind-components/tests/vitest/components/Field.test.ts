import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import Field from "../../../app/components/Field.vue";

describe("field", () => {
  it("preserves the Boolean clear button default", () => {
    const wrapper = mount(Field, {
      props: {
        id: "test-boolean",
        type: "BOOL",
        modelValue: null,
      },
    });

    expect(wrapper.find("#test-boolean-input-radio-group-clear").exists()).toBe(
      true,
    );
  });

  it("allows disabling the Boolean clear button", () => {
    const wrapper = mount(Field, {
      props: {
        id: "test-boolean",
        type: "BOOL",
        modelValue: null,
        showClearButton: false,
      },
    });

    expect(wrapper.find("#test-boolean-input-radio-group-clear").exists()).toBe(
      false,
    );
  });
});
