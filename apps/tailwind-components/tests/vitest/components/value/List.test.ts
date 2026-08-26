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

  it("shows no control when the data already fits the cap", () => {
    const wrapper = mount(ValueList, {
      props: { metadata, data: eight.slice(0, 3), maxItems: 5 },
    });

    expect(wrapper.find("button").exists()).toBe(false);
  });
});
