import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import ShowMore from "../../../app/components/ShowMore.vue";

describe("ShowMore", () => {
  it("shows button before measurement for SSR safety", () => {
    const wrapper = mount(ShowMore, {
      slots: { default: "Some text content" },
    });
    // Button shows initially (before measurement completes)
    expect(wrapper.find(".button-container").exists()).toBe(true);
  });

  it("renders collapsed class when not expanded", () => {
    const wrapper = mount(ShowMore, {
      props: { lines: 3 },
      slots: { default: "Some text content" },
    });
    expect(wrapper.find(".paragraph").exists()).toBe(true);
    expect(wrapper.find(".paragraph").classes()).toContain("collapsed");
  });

  it("applies --lines CSS variable", () => {
    const wrapper = mount(ShowMore, {
      props: { lines: 5 },
      slots: { default: "Some text content" },
    });
    const style = wrapper.find(".paragraph").attributes("style");
    expect(style).toContain("--lines: 5");
  });
});
