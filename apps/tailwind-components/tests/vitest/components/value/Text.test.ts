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

const mountText = (data: string | null) =>
  mount(ValueText, {
    props: { metadata, data },
    global: { components: { ContentClamp } },
  });

describe("value/Text.vue", () => {
  it("renders long text through the clamp so it can collapse", () => {
    const wrapper = mountText(long);

    const clamp = wrapper.findComponent(ContentClamp);
    expect(clamp.exists()).toBe(true);
    expect(clamp.text()).toContain(long);
  });

  it("keeps the whole text in the DOM, bounding only its height", () => {
    const wrapper = mountText(long);

    // The old character cutoff built a shorter string, so the tail was not in the
    // markup at all. Clamping hides it visually and leaves it findable.
    expect(wrapper.text()).toContain(long);
  });

  it("renders nothing when there is no text", () => {
    const wrapper = mountText(null);

    expect(wrapper.text().trim()).toBe("");
  });
});
