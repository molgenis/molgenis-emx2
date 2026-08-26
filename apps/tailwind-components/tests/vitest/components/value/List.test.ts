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
  it("renders every item, and asks for no clamp by default", () => {
    const wrapper = mount(ValueList, { props: { metadata, data: eight } });

    expect(wrapper.text()).toContain("Tag 8");
    expect(wrapper.findComponent(ContentClamp).props("maxLines")).toBe(
      undefined
    );
  });

  it("passes the line bound it is given straight through", () => {
    const wrapper = mount(ValueList, {
      props: { metadata, data: eight, maxLines: 2 },
    });

    expect(wrapper.findComponent(ContentClamp).props("maxLines")).toBe(2);
  });

  it("leaves every value in the DOM, because collapsing is visual", () => {
    const wrapper = mount(ValueList, {
      props: { metadata, data: eight, maxLines: 3 },
    });

    // Slicing the array would drop these from the markup, losing them to
    // crawlers, in-page search and a screen reader reading the whole page.
    expect(wrapper.text()).toContain("Tag 6");
    expect(wrapper.text()).toContain("Tag 8");
  });

  it("tells the clamp whether values remain unrendered", () => {
    const many = Array.from({ length: 500 }, (_, i) => `Tag ${i + 1}`);
    const withMore = mount(ValueList, {
      props: { metadata, data: many, maxLines: 3, renderLimit: 100 },
    });
    const withoutMore = mount(ValueList, {
      props: { metadata, data: eight, maxLines: 3, renderLimit: 100 },
    });

    // Without this the control disappears once the clamp exhausts the rendered
    // hundred, and the other four hundred become unreachable.
    expect(withMore.findComponent(ContentClamp).props("hasMore")).toBe(true);
    expect(withoutMore.findComponent(ContentClamp).props("hasMore")).toBe(
      false
    );
  });

  it("renders another tranche when the clamp runs out of rendered values", async () => {
    const many = Array.from({ length: 500 }, (_, i) => `Tag ${i + 1}`);
    const wrapper = mount(ValueList, {
      props: { metadata, data: many, maxLines: 3, renderLimit: 100 },
    });

    expect(wrapper.text()).not.toContain("Tag 101");

    await wrapper.findComponent(ContentClamp).vm.$emit("showMore");

    expect(wrapper.text()).toContain("Tag 200");
    expect(wrapper.text()).not.toContain("Tag 201");
  });

  it("stops a very long list from painting without bound", () => {
    const many = Array.from({ length: 500 }, (_, i) => `Tag ${i + 1}`);
    const wrapper = mount(ValueList, {
      props: { metadata, data: many, maxLines: 3, renderLimit: 100 },
    });

    expect(wrapper.text()).toContain("Tag 100");
    expect(wrapper.text()).not.toContain("Tag 101");
  });
});
