import { mountSuspended } from "@nuxt/test-utils/runtime";
import { defineComponent } from "vue";
import { describe, expect, it, vi } from "vitest";
import Navigation from "../../../app/components/Navigation.vue";

const VMenuStub = defineComponent({
  template: `
    <div class="vmenu-stub">
      <slot />
      <slot name="popper" />
    </div>
  `,
});

describe("Navigation", () => {
  it("renders menu html with submenu and sub-submenu items", async () => {
    const wrapper = await mountSuspended(Navigation, {
      props: {
        maximumButtonShown: 4,
        navigation: [
          { label: "Home", link: "#" },
          {
            label: "Data",
            link: "#",
            submenu: [
              {
                label: "Catalog",
                link: "#catalog",
                submenu: [{ label: "Entities", link: "#entities" }],
              },
            ],
          },
        ],
      },
      global: {
        stubs: {
          VMenu: VMenuStub,
          BaseIcon: true,
          NuxtLink: {
            props: ["to"],
            template: '<a :href="to"><slot /></a>',
          },
        },
      },
    });

    const html = wrapper.html();
    expect(html).toMatchSnapshot();
  });
});
