import { mount } from "@vue/test-utils";
import { nextTick } from "vue";
import { expect, it } from "vitest";
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
  await firstInputElem.trigger("focusin");
  await nextTick();
  expect(wrapper.emitted("focus")).toBeTruthy();
});

it("to be blurred", async () => {
  const firstInputElem = wrapper.get('input[type="checkbox"]');
  await firstInputElem.trigger("focusin");
  await firstInputElem.trigger("focusout");
  await nextTick();
  expect(wrapper.emitted("blur")).toBeTruthy();
});
