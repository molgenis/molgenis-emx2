import { mount } from "@vue/test-utils";
import { expect, it } from "vitest";
import InputRadiogroup from "../../../../components/input/RadioGroup.vue";

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
  firstInputElem.trigger("focus");
  expect(wrapper.emitted("focus"));
});

it("to be blurred", async () => {
  const firstInputElem = wrapper.get('input[type="radio"]');
  firstInputElem.trigger("focus");
  firstInputElem.trigger("blur");
  expect(wrapper.emitted("blur"));
});
