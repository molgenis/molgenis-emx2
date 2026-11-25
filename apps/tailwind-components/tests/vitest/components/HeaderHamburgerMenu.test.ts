import { mountSuspended } from "@nuxt/test-utils/runtime";
import { describe, expect, it, vi } from "vitest";
import { defineComponent } from "vue";
import HeaderHamburgerMenu from "../../../app/components/HeaderHamburgerMenu.vue";

const hide = vi.fn();

const VDropdownStub = defineComponent({
  setup: () => ({ hide }),
  template: `
    <div>
      <slot />
      <slot name="popper" :hide="hide" />
    </div>
  `,
});

async function mountMenu() {
  hide.mockClear();
  return mountSuspended(HeaderHamburgerMenu, {
    props: {
      navigation: [
        { label: "Tables", location: "/pet%20store" },
        {
          label: "Help",
          location: "",
          submenu: [
            {
              label: "Docs",
              location: "pet%20store/docs",
              linkType: "internalPageLink",
            },
          ],
        },
      ],
    },
    global: { stubs: { VDropdown: VDropdownStub, BaseIcon: true } },
  });
}

describe("HeaderHamburgerMenu", () => {
  it("closes when a link inside the ui app is clicked", async () => {
    const wrapper = await mountMenu();

    await wrapper.get('a[href="/pet%20store"]').trigger("click");

    expect(hide).toHaveBeenCalledTimes(1);
  });

  it("closes when a submenu link is clicked", async () => {
    const wrapper = await mountMenu();

    await wrapper.findAll("a").at(-1)!.trigger("click");

    expect(hide).toHaveBeenCalledTimes(1);
  });

  it("stays open when a submenu label is clicked", async () => {
    const wrapper = await mountMenu();

    await wrapper
      .findAll("span")
      .find((span) => span.text() === "Help")!
      .trigger("click");

    expect(hide).not.toHaveBeenCalled();
  });
});
