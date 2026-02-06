import { mount } from "@vue/test-utils";
import { describe, expect, test } from "vitest";

import Paragraph from "../../../../app/components/text/Paragraph.vue";

const wrapper = mount(Paragraph, {
  props: {
    id: "test-paragraph",
    paragraphIsCentered: true,
  },
  slots: {
    default:
      "Aute occaecat irure proident esse veniam tempor fugiat Lorem proident minim.",
  },
});

describe("Custom pages: paragraphs", () => {
  test("Paragraph is rendered and text is centered", async () => {
    expect(wrapper.vm.$el.tagName).toBe("P");
    expect(wrapper.attributes("class")).toBe("text-title-contrast text-center");
  });
});
