import { mountSuspended } from "@nuxt/test-utils/runtime";
import { describe, expect, it } from "vitest";
import { defineComponent, h } from "vue";
import ThemeSwitch from "../../../app/components/ThemeSwitch.vue";

describe("ThemeSwitch", () => {
  it("shows a single button that names the theme it switches to", async () => {
    const wrapper = await mountSuspended(ThemeSwitch, {
      props: { compact: true },
    });
    const button = wrapper.get("button");
    const before = button.attributes("aria-label");

    await button.trigger("click");

    expect(wrapper.findAll("button")).toHaveLength(1);
    expect([before, button.attributes("aria-label")].sort()).toEqual([
      "Switch to dark mode",
      "Switch to light mode",
    ]);
  });

  it("keeps the compact and the large switch in sync", async () => {
    const BothSwitches = defineComponent({
      setup: () => () =>
        h("div", [h(ThemeSwitch), h(ThemeSwitch, { compact: true })]),
    });
    const wrapper = await mountSuspended(BothSwitches);
    const [large, compact] = wrapper.findAll("button");
    const before = compact!.attributes("aria-label");

    await large!.trigger("click");

    expect(compact!.attributes("aria-label")).not.toBe(before);
  });
});
