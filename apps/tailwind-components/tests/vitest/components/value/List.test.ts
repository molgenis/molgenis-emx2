import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import ValueList from "../../../../app/components/value/List.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

const metadata: IColumn = {
  id: "tags",
  label: "Tags",
  columnType: "STRING_ARRAY",
};

const eight = Array.from({ length: 8 }, (_, index) => `Tag ${index + 1}`);

const clampedSpan = (wrapper: ReturnType<typeof mount>) =>
  wrapper.find("span > span");

const isClamped = (wrapper: ReturnType<typeof mount>) =>
  clampedSpan(wrapper).classes().includes("value-list-clamp");

describe("value/List.vue", () => {
  it("renders every item when no cap is given", () => {
    const wrapper = mount(ValueList, { props: { metadata, data: eight } });

    expect(wrapper.text()).toContain("Tag 8");
    expect(wrapper.find("button").exists()).toBe(false);
  });

  it("keeps every value in the DOM while collapsed, so crawlers and Ctrl-F reach them", () => {
    const wrapper = mount(ValueList, {
      props: { metadata, data: eight, maxItems: 5 },
    });

    // Collapsing is visual. Slicing the array would drop these from the markup.
    expect(wrapper.text()).toContain("Tag 6");
    expect(wrapper.text()).toContain("Tag 8");
  });

  it("bounds the collapsed list by lines rather than by item count", () => {
    const wrapper = mount(ValueList, {
      props: { metadata, data: eight, maxItems: 5, maxLines: 2 },
    });

    expect(isClamped(wrapper)).toBe(true);
    expect(clampedSpan(wrapper).attributes("style")).toContain(
      "--value-list-lines: 2"
    );
  });

  it("offers the rest behind a compact control, then collapses again", async () => {
    const wrapper = mount(ValueList, {
      props: { metadata, data: eight, maxItems: 5 },
    });

    const expand = wrapper.find("button");
    expect(expand.text()).toBe("+3 more");
    expect(expand.attributes("aria-expanded")).toBe("false");

    await expand.trigger("click");

    expect(isClamped(wrapper)).toBe(false);
    expect(wrapper.find("button").text()).toBe("less");
    expect(wrapper.find("button").attributes("aria-expanded")).toBe("true");

    await wrapper.find("button").trigger("click");

    expect(isClamped(wrapper)).toBe(true);
    expect(wrapper.find("button").text()).toBe("+3 more");
  });

  it("shows no control, and no clamp, when the data already fits the cap", () => {
    const wrapper = mount(ValueList, {
      props: { metadata, data: eight.slice(0, 3), maxItems: 5 },
    });

    expect(wrapper.find("button").exists()).toBe(false);
    expect(isClamped(wrapper)).toBe(false);
  });

  it("stops a very long list from painting without bound", () => {
    const many = Array.from({ length: 500 }, (_, i) => `Tag ${i + 1}`);
    const wrapper = mount(ValueList, {
      props: { metadata, data: many, maxItems: 5, renderLimit: 100 },
    });

    expect(wrapper.text()).toContain("Tag 100");
    expect(wrapper.text()).not.toContain("Tag 101");
    // The label still counts the whole list, not just what was rendered.
    expect(wrapper.find("button").text()).toBe("+495 more");
  });
});
