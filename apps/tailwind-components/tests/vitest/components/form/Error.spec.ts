import { mount } from "@vue/test-utils";
import { expect, it } from "vitest";
import Error from "~/components/form/Error.vue";
import FloatingVue from "floating-vue";
import "floating-vue/dist/style.css"; // Ensure styles are loaded

const wrapper = mount(Error, {
  props: {
    message: "this is an error message",
  },
  global: {
    plugins: [FloatingVue], // Register the plugin
  },
});

it("should show the message", async () => {
  expect(wrapper.html()).toContain("this is an error message");
  expect(wrapper.findAll("button")[0].text()).toContain("Go to previous error");
  expect(wrapper.findAll("button")[1].text()).toContain("Go to next error");
});
