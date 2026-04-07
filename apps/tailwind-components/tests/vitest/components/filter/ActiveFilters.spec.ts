import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import FloatingVue from "floating-vue";
import "floating-vue/dist/style.css";
import ActiveFilters from "../../../../app/components/filter/ActiveFilters.vue";
import type { ActiveFilter } from "../../../../types/filters";

function mountComponent(filters: ActiveFilter[]) {
  return mount(ActiveFilters, {
    props: { filters },
    global: { plugins: [FloatingVue] },
  });
}

const singleFilter: ActiveFilter = {
  columnId: "status",
  label: "Status",
  displayValue: "active",
  values: ["active"],
};

const multiFilter: ActiveFilter = {
  columnId: "category",
  label: "Category",
  displayValue: "(2)",
  values: ["dogs", "cats"],
};

describe("ActiveFilters", () => {
  it("is hidden when no active filters", () => {
    const wrapper = mountComponent([]);
    expect(wrapper.find("div").exists()).toBe(false);
  });

  it("renders a chip for each active filter", () => {
    const wrapper = mountComponent([singleFilter, multiFilter]);
    const html = wrapper.html();
    expect(html).toContain("Status");
    expect(html).toContain("Category");
  });

  it("shows the display value directly for single-value filters", () => {
    const wrapper = mountComponent([singleFilter]);
    expect(wrapper.html()).toContain("active");
  });

  it("shows the display value for multi-value filters", () => {
    const wrapper = mountComponent([multiFilter]);
    expect(wrapper.html()).toContain("(2)");
  });

  it("provides individual values to tooltip slot for multi-value filters", () => {
    const wrapper = mountComponent([multiFilter]);
    expect(multiFilter.values).toHaveLength(2);
    expect(multiFilter.values).toContain("dogs");
    expect(multiFilter.values).toContain("cats");
    const dropdowns = wrapper.findAllComponents({ name: "VDropdown" });
    expect(dropdowns.length).toBe(1);
  });

  it("emits remove with columnId when chip button is clicked", async () => {
    const wrapper = mountComponent([singleFilter]);
    const button = wrapper.find("button");
    await button.trigger("click");
    expect(wrapper.emitted("remove")).toBeTruthy();
    expect(wrapper.emitted("remove")![0]).toEqual(["status"]);
  });

  it("emits clearAll when clear all button is clicked", async () => {
    const wrapper = mountComponent([singleFilter]);
    const buttons = wrapper.findAll("button");
    const clearAllButton = buttons[buttons.length - 1];
    await clearAllButton.trigger("click");
    expect(wrapper.emitted("clearAll")).toBeTruthy();
  });

  it("shows Clear all button", () => {
    const wrapper = mountComponent([singleFilter]);
    expect(wrapper.html()).toContain("Clear all");
  });
});
