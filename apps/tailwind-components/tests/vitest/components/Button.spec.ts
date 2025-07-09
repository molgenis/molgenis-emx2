import { mount } from "@vue/test-utils";
import { expect, it } from "vitest";
import Button from "../../../components/Button.vue";
import FloatingVue from "floating-vue";
import "floating-vue/dist/style.css"; // Ensure styles are loaded

const wrapper = mount(Button, {
  global: {
    plugins: [FloatingVue], // Register the plugin
  },
});

it("should use the primary text class by default", async () => {
  expect(wrapper.html()).toContain("text-button-primary");
  expect(wrapper.html()).toContain("bg-button-primary");
});
