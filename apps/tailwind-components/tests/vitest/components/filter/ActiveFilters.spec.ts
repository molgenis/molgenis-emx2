import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import ActiveFilters from "../../../../app/components/filter/ActiveFilters.vue";
import type { ActiveFilter } from "../../../../types/filters";

describe("ActiveFilters", () => {
  it("renders nothing when no filters active", () => {
    const wrapper = mount(ActiveFilters, {
      props: { filters: [] },
    });

    expect(wrapper.find("div").exists()).toBe(false);
  });

  it("renders single filter chip", () => {
    const filters: ActiveFilter[] = [
      { columnId: "name", label: "Name", displayValue: "John", values: [] },
    ];
    const wrapper = mount(ActiveFilters, { props: { filters } });

    const chips = wrapper.findAll("button[aria-label^='Remove filter']");
    expect(chips).toHaveLength(1);
    expect(chips[0].text()).toContain("Name");
    expect(chips[0].text()).toContain("John");
  });

  it("renders multiple filter chips", () => {
    const filters: ActiveFilter[] = [
      { columnId: "name", label: "Name", displayValue: "John", values: [] },
      { columnId: "age", label: "Age", displayValue: "25", values: [] },
    ];
    const wrapper = mount(ActiveFilters, { props: { filters } });

    const chips = wrapper.findAll("button[aria-label^='Remove filter']");
    expect(chips).toHaveLength(2);
  });

  it("shows single value display for non-multi filters", () => {
    const filters: ActiveFilter[] = [
      { columnId: "name", label: "Name", displayValue: "John", values: [] },
    ];
    const wrapper = mount(ActiveFilters, { props: { filters } });

    expect(wrapper.text()).toContain("John");
  });

  it("shows count display for multi-value filters", () => {
    const filters: ActiveFilter[] = [
      {
        columnId: "category",
        label: "Category",
        displayValue: "3",
        values: ["A", "B", "C"],
      },
    ];
    const wrapper = mount(ActiveFilters, { props: { filters } });

    expect(wrapper.text()).toContain("3");
  });

  it("emits remove event when chip clicked", async () => {
    const filters: ActiveFilter[] = [
      { columnId: "name", label: "Name", displayValue: "John", values: [] },
    ];
    const wrapper = mount(ActiveFilters, { props: { filters } });

    const chip = wrapper.find("button[aria-label^='Remove filter']");
    await chip.trigger("click");

    expect(wrapper.emitted("remove")).toBeTruthy();
    expect(wrapper.emitted("remove")?.[0]).toEqual(["name"]);
  });

  it("shows remove all button", () => {
    const filters: ActiveFilter[] = [
      { columnId: "name", label: "Name", displayValue: "John", values: [] },
    ];
    const wrapper = mount(ActiveFilters, { props: { filters } });

    const removeAllButton = wrapper.find("button:not([aria-label])");
    expect(removeAllButton.exists()).toBe(true);
    expect(removeAllButton.text()).toBe("Remove all");
  });

  it("emits clearAll event when clear all clicked", async () => {
    const filters: ActiveFilter[] = [
      { columnId: "name", label: "Name", displayValue: "John", values: [] },
      { columnId: "age", label: "Age", displayValue: "25", values: [] },
    ];
    const wrapper = mount(ActiveFilters, { props: { filters } });

    const clearAllButton = wrapper.find("button:not([aria-label])");
    await clearAllButton.trigger("click");

    expect(wrapper.emitted("clearAll")).toBeTruthy();
  });

  it("has ARIA labels for accessibility", () => {
    const filters: ActiveFilter[] = [
      { columnId: "name", label: "Name", displayValue: "John", values: [] },
    ];
    const wrapper = mount(ActiveFilters, { props: { filters } });

    const chip = wrapper.find("button[aria-label]");
    expect(chip.attributes("aria-label")).toBe("Remove filter: Name");
  });

  it("renders tooltip values for multi-value filters", () => {
    const filters: ActiveFilter[] = [
      {
        columnId: "category",
        label: "Category",
        displayValue: "3",
        values: ["A", "B", "C"],
      },
    ];
    const wrapper = mount(ActiveFilters, {
      props: { filters },
      global: {
        stubs: {
          VDropdown: {
            template: `
              <div>
                <slot />
                <div class="popper"><slot name="popper" /></div>
              </div>
            `,
          },
        },
      },
    });

    const popperContent = wrapper.find(".popper");
    expect(popperContent.text()).toContain("A");
    expect(popperContent.text()).toContain("B");
    expect(popperContent.text()).toContain("C");
  });

  it("renders single value in tooltip for non-multi filters", () => {
    const filters: ActiveFilter[] = [
      { columnId: "name", label: "Name", displayValue: "John", values: [] },
    ];
    const wrapper = mount(ActiveFilters, {
      props: { filters },
      global: {
        stubs: {
          VDropdown: {
            template: `
              <div>
                <slot />
                <div class="popper"><slot name="popper" /></div>
              </div>
            `,
          },
        },
      },
    });

    const popperContent = wrapper.find(".popper");
    expect(popperContent.text()).toContain("John");
  });
});
