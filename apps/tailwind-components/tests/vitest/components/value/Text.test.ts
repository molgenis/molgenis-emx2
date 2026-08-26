import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import ContentClamp from "../../../../app/components/ContentClamp.vue";
import ValueText from "../../../../app/components/value/Text.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

const metadata: IColumn = {
  id: "description",
  label: "Description",
  columnType: "TEXT",
};

const long = "word ".repeat(120).trim();

const mountText = (data: string | null, maxLines?: number) =>
  mount(ValueText, {
    props: { metadata, data, maxLines },
    global: { components: { ContentClamp } },
  });

describe("value/Text.vue", () => {
  it("takes the bound from its caller, rather than keeping one of its own", () => {
    expect(
      mountText(long, 3).findComponent(ContentClamp).props("maxLines")
    ).toBe(3);
    expect(mountText(long).findComponent(ContentClamp).props("maxLines")).toBe(
      undefined
    );
  });

  it("keeps the whole text in the DOM, bounding only its height", () => {
    const wrapper = mountText(long, 3);

    expect(wrapper.text()).toContain(long);
  });

  it("renders nothing when there is no text", () => {
    const wrapper = mountText(null);

    expect(wrapper.text().trim()).toBe("");
  });
});
