import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import FilterRange from "../../../../app/components/filter/Range.vue";

describe("FilterRange", () => {
  it("renders with default min/max labels", () => {
    const wrapper = mount(FilterRange, {
      props: { id: "test-range" },
    });
    expect(wrapper.text()).toContain("Min");
    expect(wrapper.text()).toContain("Max");
  });

  it("renders custom min/max labels", () => {
    const wrapper = mount(FilterRange, {
      props: {
        id: "test-range",
        minLabel: "From",
        maxLabel: "To",
      },
    });
    expect(wrapper.text()).toContain("From");
    expect(wrapper.text()).toContain("To");
    expect(wrapper.text()).not.toContain("Min");
    expect(wrapper.text()).not.toContain("Max");
  });

  it("emits tuple on min change via slot", async () => {
    const wrapper = mount(FilterRange, {
      props: {
        id: "test-range",
        modelValue: [null, null],
        "onUpdate:modelValue": (e: any) => wrapper.setProps({ modelValue: e }),
      },
      slots: {
        min: `<template #min="{ update }">
          <button data-testid="min-btn" @click="update(10)">Set Min</button>
        </template>`,
      },
    });

    await wrapper.find('[data-testid="min-btn"]').trigger("click");
    expect(wrapper.props("modelValue")).toEqual([10, null]);
  });

  it("emits tuple on max change via slot", async () => {
    const wrapper = mount(FilterRange, {
      props: {
        id: "test-range",
        modelValue: [null, null],
        "onUpdate:modelValue": (e: any) => wrapper.setProps({ modelValue: e }),
      },
      slots: {
        max: `<template #max="{ update }">
          <button data-testid="max-btn" @click="update(100)">Set Max</button>
        </template>`,
      },
    });

    await wrapper.find('[data-testid="max-btn"]').trigger("click");
    expect(wrapper.props("modelValue")).toEqual([null, 100]);
  });

  it("preserves other value when updating min", async () => {
    const wrapper = mount(FilterRange, {
      props: {
        id: "test-range",
        modelValue: [null, 50],
        "onUpdate:modelValue": (e: any) => wrapper.setProps({ modelValue: e }),
      },
      slots: {
        min: `<template #min="{ update }">
          <button data-testid="min-btn" @click="update(25)">Set Min</button>
        </template>`,
      },
    });

    await wrapper.find('[data-testid="min-btn"]').trigger("click");
    expect(wrapper.props("modelValue")).toEqual([25, 50]);
  });

  it("preserves other value when updating max", async () => {
    const wrapper = mount(FilterRange, {
      props: {
        id: "test-range",
        modelValue: [10, null],
        "onUpdate:modelValue": (e: any) => wrapper.setProps({ modelValue: e }),
      },
      slots: {
        max: `<template #max="{ update }">
          <button data-testid="max-btn" @click="update(99)">Set Max</button>
        </template>`,
      },
    });

    await wrapper.find('[data-testid="max-btn"]').trigger("click");
    expect(wrapper.props("modelValue")).toEqual([10, 99]);
  });

  it("passes correct ids to slots", () => {
    const wrapper = mount(FilterRange, {
      props: { id: "age-filter" },
      slots: {
        min: `<template #min="{ id }"><input :id="id" data-testid="min-input" /></template>`,
        max: `<template #max="{ id }"><input :id="id" data-testid="max-input" /></template>`,
      },
    });

    expect(wrapper.find('[data-testid="min-input"]').attributes("id")).toBe(
      "age-filter-min"
    );
    expect(wrapper.find('[data-testid="max-input"]').attributes("id")).toBe(
      "age-filter-max"
    );
  });

  it("converts undefined to null when updating", async () => {
    const wrapper = mount(FilterRange, {
      props: {
        id: "test-range",
        modelValue: [10, 20],
        "onUpdate:modelValue": (e: any) => wrapper.setProps({ modelValue: e }),
      },
      slots: {
        min: `<template #min="{ update }">
          <button data-testid="clear-btn" @click="update(undefined)">Clear</button>
        </template>`,
      },
    });

    await wrapper.find('[data-testid="clear-btn"]').trigger("click");
    expect(wrapper.props("modelValue")).toEqual([null, 20]);
  });

  it("uses fieldset for accessibility grouping", () => {
    const wrapper = mount(FilterRange, {
      props: { id: "test-range" },
    });
    expect(wrapper.find("fieldset.space-y-2").exists()).toBe(true);
  });

  it("renders labels with for attributes matching slot ids", () => {
    const wrapper = mount(FilterRange, {
      props: { id: "price-range" },
    });
    const labels = wrapper.findAll("label");
    expect(labels[0].attributes("for")).toBe("price-range-min");
    expect(labels[1].attributes("for")).toBe("price-range-max");
  });

  it("renders sr-only legend when legend prop provided", () => {
    const wrapper = mount(FilterRange, {
      props: { id: "test-range", legend: "Price range filter" },
    });
    const legend = wrapper.find("legend");
    expect(legend.exists()).toBe(true);
    expect(legend.text()).toBe("Price range filter");
    expect(legend.classes()).toContain("sr-only");
  });

  it("does not render legend when legend prop empty", () => {
    const wrapper = mount(FilterRange, {
      props: { id: "test-range" },
    });
    expect(wrapper.find("legend").exists()).toBe(false);
  });
});
