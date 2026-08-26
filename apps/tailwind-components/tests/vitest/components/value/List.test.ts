import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import ContentClamp from "../../../../app/components/ContentClamp.vue";
import ValueList from "../../../../app/components/value/List.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

const metadata: IColumn = {
  id: "tags",
  label: "Tags",
  columnType: "STRING_ARRAY",
};

const eight = Array.from({ length: 8 }, (_, index) => `Tag ${index + 1}`);

describe("value/List.vue", () => {
  it("renders every item, and asks for no clamp when no cap is given", () => {
    const wrapper = mount(ValueList, { props: { metadata, data: eight } });

    expect(wrapper.text()).toContain("Tag 8");
    expect(wrapper.findComponent(ContentClamp).props("maxLines")).toBe(
      undefined
    );
  });

  it("asks for a line clamp once the list is longer than the cap", () => {
    const wrapper = mount(ValueList, {
      props: { metadata, data: eight, maxItems: 5, maxLines: 2 },
    });

    expect(wrapper.findComponent(ContentClamp).props("maxLines")).toBe(2);
  });

  it("leaves every value in the DOM, because collapsing is visual", () => {
    const wrapper = mount(ValueList, {
      props: { metadata, data: eight, maxItems: 5 },
    });

    // Slicing the array would drop these from the markup, losing them to
    // crawlers, in-page search and a screen reader reading the whole page.
    expect(wrapper.text()).toContain("Tag 6");
    expect(wrapper.text()).toContain("Tag 8");
  });

  it("asks for no clamp when the list already fits the cap", () => {
    const wrapper = mount(ValueList, {
      props: { metadata, data: eight.slice(0, 3), maxItems: 5 },
    });

    expect(wrapper.findComponent(ContentClamp).props("maxLines")).toBe(
      undefined
    );
  });

  it("stops a very long list from painting without bound", () => {
    const many = Array.from({ length: 500 }, (_, i) => `Tag ${i + 1}`);
    const wrapper = mount(ValueList, {
      props: { metadata, data: many, maxItems: 5, renderLimit: 100 },
    });

    expect(wrapper.text()).toContain("Tag 100");
    expect(wrapper.text()).not.toContain("Tag 101");
  });
});
