import { describe, it, expect, vi } from "vitest";
import { mount } from "@vue/test-utils";
import Sidebar from "../../../app/components/Sidebar.vue";

vi.mock("../../../app/components/BaseIcon.vue", () => ({
  default: {
    name: "BaseIcon",
    props: ["name", "width"],
    template: '<span :data-icon="name"></span>',
  },
}));

function mountSidebar(
  collapsed = false,
  activeFilterCount = 0,
  slotContent = '<div class="slot-content">content</div>'
) {
  return mount(Sidebar, {
    props: { collapsed, activeFilterCount },
    slots: { default: slotContent },
    attachTo: document.body,
  });
}

describe("Sidebar persistent gradient container", () => {
  it("always renders the gradient box when expanded", () => {
    const wrapper = mountSidebar(false, 0);
    expect(wrapper.classes()).toContain("bg-sidebar-gradient");
    wrapper.unmount();
  });

  it("always renders the gradient box when collapsed", () => {
    const wrapper = mountSidebar(true, 0);
    expect(wrapper.classes()).toContain("bg-sidebar-gradient");
    wrapper.unmount();
  });
});

describe("Sidebar expanded", () => {
  it("renders #filter-sidebar-content container", () => {
    const wrapper = mountSidebar(false, 0);
    expect(wrapper.find("#filter-sidebar-content").exists()).toBe(true);
    wrapper.unmount();
  });

  it("renders default slot content", () => {
    const wrapper = mountSidebar(false, 0);
    expect(wrapper.find(".slot-content").exists()).toBe(true);
    wrapper.unmount();
  });

  it("renders the toggle button with aria-label Hide filters when expanded", () => {
    const wrapper = mountSidebar(false, 0);
    const toggleBtn = wrapper.find('[aria-label="Hide filters"]');
    expect(toggleBtn.exists()).toBe(true);
    wrapper.unmount();
  });

  it("renders double-arrow-left icon when expanded", () => {
    const wrapper = mountSidebar(false, 0);
    const icon = wrapper.find('[data-icon="double-arrow-left"]');
    expect(icon.exists()).toBe(true);
    wrapper.unmount();
  });

  it("emits update:collapsed true when toggle button is clicked while expanded", async () => {
    const wrapper = mountSidebar(false, 0);
    const toggleBtn = wrapper.find('[aria-label="Hide filters"]');
    await toggleBtn.trigger("click");
    expect(wrapper.emitted("update:collapsed")).toBeTruthy();
    expect(wrapper.emitted("update:collapsed")![0]).toEqual([true]);
    wrapper.unmount();
  });

  it("shows slot content when expanded", () => {
    const wrapper = mountSidebar(false, 0);
    const content = wrapper.find("#filter-sidebar-content");
    expect(content.isVisible()).toBe(true);
    wrapper.unmount();
  });
});

describe("Sidebar collapsed (rail)", () => {
  it("keeps slot content in the DOM when collapsed (v-show, not v-if)", () => {
    const wrapper = mountSidebar(true, 0);
    expect(wrapper.find(".slot-content").exists()).toBe(true);
    wrapper.unmount();
  });

  it("hides slot content immediately when mounted in collapsed state", () => {
    const wrapper = mountSidebar(true, 0);
    const content = wrapper.find("#filter-sidebar-content");
    expect(content.isVisible()).toBe(false);
    wrapper.unmount();
  });

  it("renders toggle button with aria-label Show filters when collapsed", () => {
    const wrapper = mountSidebar(true, 0);
    const expandBtn = wrapper.find('[aria-label="Show filters"]');
    expect(expandBtn.exists()).toBe(true);
    wrapper.unmount();
  });

  it("renders double-arrow-right icon when collapsed", () => {
    const wrapper = mountSidebar(true, 0);
    const icon = wrapper.find('[data-icon="double-arrow-right"]');
    expect(icon.exists()).toBe(true);
    wrapper.unmount();
  });

  it("emits update:collapsed false when toggle button is clicked while collapsed", async () => {
    const wrapper = mountSidebar(true, 0);
    const expandBtn = wrapper.find('[aria-label="Show filters"]');
    await expandBtn.trigger("click");
    expect(wrapper.emitted("update:collapsed")).toBeTruthy();
    expect(wrapper.emitted("update:collapsed")![0]).toEqual([false]);
    wrapper.unmount();
  });

  it("does not show count badge when activeFilterCount is 0", () => {
    const wrapper = mountSidebar(true, 0);
    const badge = wrapper.find("span.rounded-full.bg-button-primary");
    expect(badge.exists()).toBe(false);
    wrapper.unmount();
  });

  it("shows count badge when activeFilterCount is greater than 0", () => {
    const wrapper = mountSidebar(true, 3);
    const badge = wrapper.find("span.rounded-full.bg-button-primary");
    expect(badge.exists()).toBe(true);
    expect(badge.text()).toBe("3");
    wrapper.unmount();
  });

  it("always renders #filter-sidebar-content in collapsed state (slide is CSS-only)", () => {
    const wrapper = mountSidebar(true, 0);
    expect(wrapper.find("#filter-sidebar-content").exists()).toBe(true);
    wrapper.unmount();
  });

  it("toggle button is always visible regardless of content visibility gating", () => {
    const wrapper = mountSidebar(true, 0);
    const toggleBtn = wrapper.find('[aria-label="Show filters"]');
    expect(toggleBtn.isVisible()).toBe(true);
    wrapper.unmount();
  });
});

describe("Sidebar contentVisible gating", () => {
  it("shows content immediately when transitioning from collapsed to expanded", async () => {
    const wrapper = mountSidebar(true, 0);
    const content = wrapper.find("#filter-sidebar-content");
    expect(content.isVisible()).toBe(false);

    await wrapper.setProps({ collapsed: false });

    expect(content.isVisible()).toBe(true);
    wrapper.unmount();
  });

  it("keeps content visible during collapse slide (before transitionend)", async () => {
    const wrapper = mountSidebar(false, 0);
    const content = wrapper.find("#filter-sidebar-content");
    expect(content.isVisible()).toBe(true);

    await wrapper.setProps({ collapsed: true });

    expect(content.isVisible()).toBe(true);
    wrapper.unmount();
  });

  it("hides content after transitionend fires on margin-left while collapsed", async () => {
    const wrapper = mountSidebar(false, 0);
    const content = wrapper.find("#filter-sidebar-content");

    await wrapper.setProps({ collapsed: true });
    expect(content.isVisible()).toBe(true);

    (wrapper.vm as any).onTransitionEnd({
      propertyName: "margin-left",
    } as TransitionEvent);
    await wrapper.vm.$nextTick();

    expect(content.isVisible()).toBe(false);
    wrapper.unmount();
  });

  it("does not hide content if re-expanded before transitionend fires", async () => {
    const wrapper = mountSidebar(false, 0);
    const content = wrapper.find("#filter-sidebar-content");

    await wrapper.setProps({ collapsed: true });
    await wrapper.setProps({ collapsed: false });

    (wrapper.vm as any).onTransitionEnd({
      propertyName: "margin-left",
    } as TransitionEvent);
    await wrapper.vm.$nextTick();

    expect(content.isVisible()).toBe(true);
    wrapper.unmount();
  });

  it("ignores transitionend events for other CSS properties", async () => {
    const wrapper = mountSidebar(false, 0);
    const content = wrapper.find("#filter-sidebar-content");

    await wrapper.setProps({ collapsed: true });

    (wrapper.vm as any).onTransitionEnd({
      propertyName: "opacity",
    } as TransitionEvent);
    await wrapper.vm.$nextTick();

    expect(content.isVisible()).toBe(true);
    wrapper.unmount();
  });
});

describe("Sidebar click-to-expand on collapsed rail", () => {
  it("emits update:collapsed false when clicking the collapsed panel root", async () => {
    const wrapper = mountSidebar(true, 0);
    await wrapper.trigger("click");
    expect(wrapper.emitted("update:collapsed")).toBeTruthy();
    expect(wrapper.emitted("update:collapsed")![0]).toEqual([false]);
    wrapper.unmount();
  });

  it("does not emit when clicking the expanded panel root", async () => {
    const wrapper = mountSidebar(false, 0);
    await wrapper.trigger("click");
    expect(wrapper.emitted("update:collapsed")).toBeFalsy();
    wrapper.unmount();
  });

  it("adds cursor-pointer class when collapsed", () => {
    const wrapper = mountSidebar(true, 0);
    expect(wrapper.classes()).toContain("cursor-pointer");
    wrapper.unmount();
  });

  it("does not add cursor-pointer class when expanded", () => {
    const wrapper = mountSidebar(false, 0);
    expect(wrapper.classes()).not.toContain("cursor-pointer");
    wrapper.unmount();
  });
});

describe("Sidebar toggle button is always present (single unified control)", () => {
  it("toggle button exists in both expanded and collapsed state", () => {
    const expandedWrapper = mountSidebar(false, 0);
    const collapsedWrapper = mountSidebar(true, 0);
    expect(expandedWrapper.find('[aria-label="Hide filters"]').exists()).toBe(
      true
    );
    expect(collapsedWrapper.find('[aria-label="Show filters"]').exists()).toBe(
      true
    );
    expandedWrapper.unmount();
    collapsedWrapper.unmount();
  });

  it("count badge shows under toggle when activeFilterCount > 0 in expanded state", () => {
    const wrapper = mountSidebar(false, 5);
    const badge = wrapper.find("span.rounded-full.bg-button-primary");
    expect(badge.exists()).toBe(true);
    expect(badge.text()).toBe("5");
    wrapper.unmount();
  });
});
