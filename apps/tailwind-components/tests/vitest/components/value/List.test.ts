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

describe("value/List.vue", () => {
  it("renders every item when no cap is given", () => {
    const wrapper = mount(ValueList, { props: { metadata, data: eight } });

    expect(wrapper.text()).toContain("Tag 8");
    expect(wrapper.find("button").exists()).toBe(false);
  });

  it("caps at maxItems and offers the rest behind a compact +N control", () => {
    const wrapper = mount(ValueList, {
      props: { metadata, data: eight, maxItems: 5 },
    });

    expect(wrapper.text()).toContain("Tag 5");
    expect(wrapper.text()).not.toContain("Tag 6");
    expect(wrapper.find("button").text()).toBe("+3 more");
    expect(wrapper.find("button").attributes("aria-expanded")).toBe("false");
  });

  it("expands to every item, then offers to collapse again", async () => {
    const wrapper = mount(ValueList, {
      props: { metadata, data: eight, maxItems: 5 },
    });

    await wrapper.find("button").trigger("click");

    expect(wrapper.text()).toContain("Tag 8");
    expect(wrapper.find("button").text()).toBe("less");
    expect(wrapper.find("button").attributes("aria-expanded")).toBe("true");
  });

  it("reveals a long list in tranches rather than painting it all at once", async () => {
    const many = Array.from({ length: 500 }, (_, i) => `Tag ${i + 1}`);
    const wrapper = mount(ValueList, {
      props: { metadata, data: many, maxItems: 5, step: 20 },
    });

    // The label states what the click does, not how many are hidden overall.
    expect(wrapper.find("button").text()).toBe("+20 more");
    expect(wrapper.text()).toContain("Tag 5");
    expect(wrapper.text()).not.toContain("Tag 6");

    await wrapper.find("button").trigger("click");

    expect(wrapper.text()).toContain("Tag 25");
    expect(wrapper.text()).not.toContain("Tag 26");
    expect(wrapper.find("button").text()).toBe("+20 more");
  });

  it("shrinks the last tranche to what is left", async () => {
    const wrapper = mount(ValueList, {
      props: { metadata, data: eight, maxItems: 5, step: 20 },
    });

    expect(wrapper.find("button").text()).toBe("+3 more");

    await wrapper.find("button").trigger("click");

    expect(wrapper.text()).toContain("Tag 8");
    expect(wrapper.findAll("button").map((b) => b.text())).toEqual(["less"]);
  });

  it("collapses back to the cap from any tranche", async () => {
    const many = Array.from({ length: 500 }, (_, i) => `Tag ${i + 1}`);
    const wrapper = mount(ValueList, {
      props: { metadata, data: many, maxItems: 5, step: 20 },
    });

    await wrapper.find("button").trigger("click");
    await wrapper.find("button").trigger("click");
    expect(wrapper.text()).toContain("Tag 45");

    const collapse = wrapper.findAll("button").find((b) => b.text() === "less");
    await collapse!.trigger("click");

    expect(wrapper.text()).toContain("Tag 5");
    expect(wrapper.text()).not.toContain("Tag 6");
  });

  it("shows no control when the data already fits the cap", () => {
    const wrapper = mount(ValueList, {
      props: { metadata, data: eight.slice(0, 3), maxItems: 5 },
    });

    expect(wrapper.find("button").exists()).toBe(false);
  });
});
