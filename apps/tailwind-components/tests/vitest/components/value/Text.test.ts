import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import ContentReadMore from "../../../../app/components/ContentReadMore.vue";
import ValueText from "../../../../app/components/value/Text.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

const metadata: IColumn = {
  id: "description",
  label: "Description",
  columnType: "TEXT",
};

const long = "word ".repeat(120).trim();

const mountText = (data: string | null) =>
  mount(ValueText, {
    props: { metadata, data },
    global: { components: { ContentReadMore } },
  });

describe("value/Text.vue", () => {
  it("renders long text through ContentReadMore so it can collapse", () => {
    const wrapper = mountText(long);

    const readMore = wrapper.findComponent(ContentReadMore);
    expect(readMore.exists()).toBe(true);
    expect(readMore.props("text")).toBe(long);
  });

  it("truncates past the cutoff and offers a control to expand", () => {
    const wrapper = mountText(long);

    expect(wrapper.text()).not.toContain(long);
    expect(wrapper.find("button").exists()).toBe(true);
  });

  it("renders nothing when there is no text", () => {
    const wrapper = mountText(null);

    expect(wrapper.text().trim()).toBe("");
  });
});
