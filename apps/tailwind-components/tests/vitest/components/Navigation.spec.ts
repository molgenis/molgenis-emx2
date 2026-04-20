import { mount } from "@vue/test-utils";
import { defineComponent } from "vue";
import { describe, expect, it, vi } from "vitest";
import Navigation from "../../../app/components/Navigation.vue";

vi.mock("#app", () => ({
  useAppConfig: () => ({
    basePath: "/",
  }),
}));

const VMenuStub = defineComponent({
  template: `
    <div class="vmenu-stub">
      <slot />
      <slot name="popper" />
    </div>
  `,
});

describe("Navigation", () => {
  it("renders menu html with submenu and sub-submenu items", () => {
    const wrapper = mount(Navigation, {
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
