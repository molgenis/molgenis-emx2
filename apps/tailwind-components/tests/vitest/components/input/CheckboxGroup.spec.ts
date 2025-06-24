import { mount } from "@vue/test-utils";
import { expect, it } from "vitest";
import InputCheckboxGroup from "../../../../components/input/CheckboxGroup.vue";

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
