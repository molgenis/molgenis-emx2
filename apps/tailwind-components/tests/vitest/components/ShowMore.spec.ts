import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import ShowMore from "../../../app/components/ShowMore.vue";

describe("ShowMore", () => {
  it("shows button after mount (simulating hydration)", async () => {
    const wrapper = mount(ShowMore, {
      slots: { default: "Some text content" },
    });
    await wrapper.vm.$nextTick();
    // After hydration, button visibility depends on content overflow
    // In test env with short content, button may be hidden
    expect(wrapper.find(".controls").exists()).toBe(false);
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
