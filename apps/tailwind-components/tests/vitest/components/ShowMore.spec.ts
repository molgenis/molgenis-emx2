import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import ShowMore from "../../../app/components/ShowMore.vue";

describe("ShowMore", () => {
  it("shows button before hydration for SSR safety", () => {
    const wrapper = mount(ShowMore, {
      slots: { default: "Some text content" },
    });
    // Button shows initially (before hydration completes)
    // This ensures SSR renders the button
    expect(wrapper.find(".controls").exists()).toBe(true);
  });

  it("renders correctly with default props", () => {
    const wrapper = mount(ShowMore, {
      props: { lines: 3 },
      slots: { default: "Some text content" },
    });
    expect(wrapper.find(".paragraph").exists()).toBe(true);
    expect(wrapper.find(".expandable-paragraph").exists()).toBe(true);
  });
});
