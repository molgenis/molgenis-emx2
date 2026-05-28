import { describe, it, expect, vi } from "vitest";
import { mount } from "@vue/test-utils";
import Sidebar from "../../../app/components/Sidebar.vue";

vi.mock("../../../app/components/Button.vue", () => ({
  default: {
    name: "Button",
    props: ["type", "iconOnly", "icon", "label", "size"],
    emits: ["click"],
    template:
      '<button :aria-label="label" @click="$emit(\'click\', $event)">{{ label }}</button>',
  },
}));

function mountSidebar(collapsed: boolean, collapsedLabel = "Show filters") {
  return mount(Sidebar, {
    props: { collapsed, collapsedLabel },
    slots: { default: '<div class="slot-content">content</div>' },
  });
}

describe("Sidebar", () => {
  it("renders collapsed rail when collapsed=true", () => {
    const wrapper = mountSidebar(true);
    expect(wrapper.find('[role="button"]').exists()).toBe(true);
    expect(wrapper.find("#filter-sidebar-content").exists()).toBe(false);
  });

  it("renders expanded panel when collapsed=false", () => {
    const wrapper = mountSidebar(false);
    expect(wrapper.find("#filter-sidebar-content").exists()).toBe(true);
    expect(wrapper.find('[role="button"]').exists()).toBe(false);
  });

  it("shows collapsedLabel text in collapsed rail", () => {
    const wrapper = mountSidebar(true, "Show filters");
    expect(wrapper.html()).toContain("Show filters");
  });

  it("emits update:collapsed=false when collapsed rail is clicked", async () => {
    const wrapper = mountSidebar(true);
    await wrapper.find('[role="button"]').trigger("click");
    expect(wrapper.emitted("update:collapsed")).toBeDefined();
    expect(wrapper.emitted("update:collapsed")![0]).toEqual([false]);
  });

  it("emits update:collapsed=true when Hide-filters button is clicked", async () => {
    const wrapper = mountSidebar(false);
    const hideBtn = wrapper
      .findAll("button")
      .find((b) => b.attributes("aria-label") === "Hide filters");
    expect(hideBtn).toBeDefined();
    await hideBtn!.trigger("click");
    expect(wrapper.emitted("update:collapsed")).toBeDefined();
    expect(wrapper.emitted("update:collapsed")![0]).toEqual([true]);
  });

  it("renders default slot content when expanded", () => {
    const wrapper = mountSidebar(false);
    expect(wrapper.find(".slot-content").exists()).toBe(true);
  });

  it("does not render default slot content when collapsed", () => {
    const wrapper = mountSidebar(true);
    expect(wrapper.find(".slot-content").exists()).toBe(false);
  });
});
