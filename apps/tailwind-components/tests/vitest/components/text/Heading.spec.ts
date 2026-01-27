import { mount } from "@vue/test-utils";
import { describe, expect, test } from "vitest";

import TextHeading from "../../../../app/components/text/Heading.vue";

const wrapper = mount(TextHeading, {
  props: {
    id: "test-heading",
    headingLevel: "H2",
    isCentered: true,
  },
  slots: {
    default: "Heading 2",
  },
});

describe("Text Headings", () => {
  test("Heading is rendered and text is centered", async () => {
    expect(wrapper.vm.$el.tagName).toBe("H2");
    expect(wrapper.attributes("class")).toContain(
      "text-title text-heading-5xl text-center"
    );
  });
});
