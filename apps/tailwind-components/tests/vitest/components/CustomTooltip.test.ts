import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import CustomTooltip from "../../../app/components/CustomTooltip.vue";

describe("CustomTooltip.vue", () => {
  it("opens on focus and click, so the content is reachable without a mouse", () => {
    const wrapper = mount(CustomTooltip, {
      props: { label: "Read more", content: "A definition." },
    });

    const triggers = wrapper
      .findComponent({ name: "VTooltip" })
      .props("showTriggers") as string[];

    // 2.1.1 Keyboard: hover and touch alone leave a keyboard user with no way in.
    expect(triggers).toContain("focus");
    expect(triggers).toContain("click");
    expect(triggers).toContain("hover");
    expect(triggers).toContain("touch");
  });

  it("gives the trigger an accessible name", () => {
    const wrapper = mount(CustomTooltip, {
      props: { label: "Read more", content: "A definition." },
    });

    expect(wrapper.find("button").text()).toContain("Read more");
  });
});
