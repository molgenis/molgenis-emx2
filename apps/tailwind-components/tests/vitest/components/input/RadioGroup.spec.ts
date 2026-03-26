import { mount } from "@vue/test-utils";
import { nextTick } from "vue";
import { expect, it } from "vitest";
import InputRadiogroup from "../../../../app/components/input/RadioGroup.vue";

const wrapper = mount(InputRadiogroup, {
  props: {
    id: "test",
    options: [{ label: "Option 1", value: "option1" }],
  },
});

it("the parent of the hidden input should have a class relative to keep the position of the sr-only element within its bounding box ", async () => {
  const radioItemLabel = wrapper.get('label[for="test-radio-group-option1"]');
  expect(radioItemLabel.html()).toContain("relative");
  expect(
    radioItemLabel.get('[id="test-radio-group-option1"]').html()
  ).toContain("sr-only");
});

it("to be focused", async () => {
  const firstInputElem = wrapper.get('input[type="radio"]');
  await firstInputElem.trigger("focusin");
  await nextTick();
  expect(wrapper.emitted("focus")).toBeTruthy();
});

it("to be blurred", async () => {
  const firstInputElem = wrapper.get('input[type="radio"]');
  await firstInputElem.trigger("focusin");
  await firstInputElem.trigger("focusout");
  await nextTick();
  expect(wrapper.emitted("blur")).toBeTruthy();
});
