import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import ShowMore from "../../../../app/components/ShowMore.vue";
import ValueList from "../../../../app/components/value/List.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

const metadata: IColumn = {
  id: "tags",
  label: "Tags",
  columnType: "STRING_ARRAY",
};

const eight = Array.from({ length: 8 }, (_, index) => `Tag ${index + 1}`);

describe("value/List.vue", () => {
  it("hands the clamp its caller's bound, and says when not to collapse", () => {
    const bounded = mount(ValueList, {
      props: { metadata, data: eight, maxLines: 2 },
    });
    const uncollapsed = mount(ValueList, {
      props: { metadata, data: eight, collapsible: false },
    });

    expect(bounded.findComponent(ShowMore).props("maxLines")).toBe(2);
    expect(uncollapsed.findComponent(ShowMore).props("collapsible")).toBe(false);
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
    expect(withMore.findComponent(ShowMore).props("hasMore")).toBe(true);
    expect(withoutMore.findComponent(ShowMore).props("hasMore")).toBe(false);
  });

  it("puts a realistic list wholly in the DOM, where a crawler reads it", () => {
    const many = Array.from({ length: 500 }, (_, i) => `Tag ${i + 1}`);
    const wrapper = mount(ValueList, {
      props: { metadata, data: many, maxLines: 3 },
    });

    expect(wrapper.text()).toContain("Tag 500");
  });

  it("guards against a pathological list, and reaches the rest by asking", async () => {
    const many = Array.from({ length: 500 }, (_, i) => `Tag ${i + 1}`);
    const wrapper = mount(ValueList, {
      props: { metadata, data: many, maxLines: 3, renderLimit: 100 },
    });

    expect(wrapper.text()).not.toContain("Tag 101");

    await wrapper.findComponent(ShowMore).vm.$emit("showMore");

    expect(wrapper.text()).toContain("Tag 200");
    expect(wrapper.text()).not.toContain("Tag 201");
  });
});
