import { mount, flushPromises } from "@vue/test-utils";
import { expect, it, describe } from "vitest";
import InputCheckboxGroup from "../../../../app/components/input/CheckboxGroup.vue";

const wrapper = mount(InputCheckboxGroup, {
  props: {
    id: "test-input-checkbox-group",
    options: [
      { label: "Option 1", value: "option1" },
      { label: "Option 2", value: "option2" },
      { label: "Option 3", value: "option3" },
    ],
  },
});

it("to be focused", async () => {
  const firstInputElem = wrapper.get('input[type="checkbox"]');
  firstInputElem.trigger("focus");
  expect(wrapper.emitted("focus"));
});

it("to be blurred", async () => {
  const firstInputElem = wrapper.get('input[type="checkbox"]');
  firstInputElem.trigger("focus");
  firstInputElem.trigger("blur");
  expect(wrapper.emitted("blur"));
});

describe("CheckboxGroup with undefined initial modelValue (ENUM_ARRAY / MODULE_ARRAY bug)", () => {
  it("does not throw when modelValue is undefined and a checkbox is changed", async () => {
    const w = mount(InputCheckboxGroup, {
      props: {
        id: "test-undefined-model",
        options: [{ value: "research" }, { value: "clinical" }],
      },
    });

    const firstCheckbox = w.get('input[value="research"]');
    await expect(firstCheckbox.trigger("change")).resolves.not.toThrow();
    await flushPromises();
  });

  it("emits an ARRAY (not boolean true) on first check from undefined modelValue", async () => {
    const w = mount(InputCheckboxGroup, {
      props: {
        id: "test-array-emit",
        options: [{ value: "research" }, { value: "clinical" }],
      },
    });

    const firstCheckbox = w.get('input[value="research"]');
    firstCheckbox.element.checked = true;
    await firstCheckbox.trigger("change");
    await flushPromises();

    const emitted = w.emitted("update:modelValue");
    expect(emitted).toBeTruthy();
    const lastEmit = emitted![emitted!.length - 1][0];
    expect(Array.isArray(lastEmit)).toBe(true);
    expect((lastEmit as string[]).includes("research")).toBe(true);
  });

  it("adds second value to array when a second checkbox is checked", async () => {
    const w = mount(InputCheckboxGroup, {
      props: {
        id: "test-two-checks",
        options: [{ value: "research" }, { value: "clinical" }],
        modelValue: ["research"],
      },
    });

    const secondCheckbox = w.get('input[value="clinical"]');
    secondCheckbox.element.checked = true;
    await secondCheckbox.trigger("change");
    await flushPromises();

    const emitted = w.emitted("update:modelValue");
    expect(emitted).toBeTruthy();
    const lastEmit = emitted![emitted!.length - 1][0] as string[];
    expect(Array.isArray(lastEmit)).toBe(true);
    expect(lastEmit.includes("research")).toBe(true);
    expect(lastEmit.includes("clinical")).toBe(true);
  });

  it("removes value from array when checkbox is unchecked", async () => {
    const w = mount(InputCheckboxGroup, {
      props: {
        id: "test-uncheck",
        options: [{ value: "research" }, { value: "clinical" }],
        modelValue: ["research", "clinical"],
      },
    });

    const firstCheckbox = w.get('input[value="research"]');
    firstCheckbox.element.checked = false;
    await firstCheckbox.trigger("change");
    await flushPromises();

    const emitted = w.emitted("update:modelValue");
    expect(emitted).toBeTruthy();
    const lastEmit = emitted![emitted!.length - 1][0] as string[];
    expect(Array.isArray(lastEmit)).toBe(true);
    expect(lastEmit.includes("research")).toBe(false);
    expect(lastEmit.includes("clinical")).toBe(true);
  });
});
