import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import Sidebar from "../../../app/components/Sidebar.vue";

function mountSidebar(slotContent = '<div class="slot-content">content</div>') {
  return mount(Sidebar, {
    slots: { default: slotContent },
  });
}

describe("Sidebar", () => {
  it("renders #filter-sidebar-content container", () => {
    const wrapper = mountSidebar();
    expect(wrapper.find("#filter-sidebar-content").exists()).toBe(true);
  });

  it("renders default slot content", () => {
    const wrapper = mountSidebar();
    expect(wrapper.find(".slot-content").exists()).toBe(true);
  });
});
