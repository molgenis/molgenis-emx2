import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import Field from "../../../../app/components/Field.vue";
import Input from "../../../../app/components/Input.vue";

const clearButton = "#test-bool-input-radio-group-clear";

describe("BOOL clear button through the wrappers", () => {
  it("should keep the Boolean default (shown) when Field omits showClearButton", () => {
    const wrapper = mount(Field, {
      props: { id: "test-bool", type: "BOOL", modelValue: true },
    });
    expect(wrapper.find(clearButton).exists()).toBe(true);
  });

  it("should hide it when Field passes showClearButton false", () => {
    const wrapper = mount(Field, {
      props: {
        id: "test-bool",
        type: "BOOL",
        modelValue: true,
        showClearButton: false,
      },
    });
    expect(wrapper.find(clearButton).exists()).toBe(false);
  });

  it("should keep the Boolean default (shown) when Input omits showClearButton", () => {
    const wrapper = mount(Input, {
      props: { id: "test-bool-input", type: "BOOL", modelValue: true },
    });
    expect(wrapper.find(clearButton).exists()).toBe(true);
  });
});
