import { mount } from "@vue/test-utils";
import { describe, expect, test } from "vitest";

import EditableHeading from "../../../../app/components/cms/heading/EditableHeading.vue";

const wrapper = mount(EditableHeading, {
  props: {
    id: "test-heading",
    level: 2,
    headingIsCentered: true,
    text: "Heading 2",
  },
});

describe("Cms:Headings:", () => {
  test("The correct heading level is rendered and the text is centered", async () => {
    expect(wrapper.vm.$el.tagName).toBe("H2");
    expect(wrapper.attributes("class")).toContain(
      "text-title mb-2.5 text-heading-5xl w-full flex justify-center text-center"
    );
  });
});
