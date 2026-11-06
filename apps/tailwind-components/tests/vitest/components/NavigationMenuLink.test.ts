import { mountSuspended } from "@nuxt/test-utils/runtime";
import { describe, expect, it } from "vitest";
import NavigationMenuLink from "../../../app/components/NavigationMenuLink.vue";
import type { MenuItem } from "../../../types/types";

const NuxtLinkStub = {
  props: ["to"],
  template: '<a data-spa :href="to"><slot /></a>',
};

async function render(item: Omit<MenuItem, "label">, basePath = "/") {
  const wrapper = await mountSuspended(NavigationMenuLink, {
    props: { item: { label: "Link", ...item }, basePath, linkClass: "" },
    global: { stubs: { NuxtLink: NuxtLinkStub } },
  });
  const anchor = wrapper.find("a");
  return {
    href: anchor.attributes("href"),
    isSpa: anchor.attributes("data-spa") !== undefined,
  };
}

describe("NavigationMenuLink", () => {
  it("renders spaLink as a client-side route without base path", async () => {
    expect(
      await render({ link: "/pet store/Pet", linkType: "spaLink" })
    ).toEqual({ href: "/pet store/Pet", isSpa: true });
  });

  it("prefixes internalPageLink with the base path", async () => {
    expect(
      await render({ link: "pet store/schema", linkType: "internalPageLink" })
    ).toEqual({ href: "/pet store/schema", isSpa: false });
    expect(
      await render(
        { link: "apps/central/", linkType: "internalPageLink" },
        "/base/"
      )
    ).toEqual({ href: "/base/apps/central/", isSpa: false });
  });

  it("uses externalLink as is", async () => {
    expect(
      await render({ link: "https://www.google.com", linkType: "externalLink" })
    ).toEqual({ href: "https://www.google.com", isSpa: false });
  });

  it("treats a link without linkType as spaLink", async () => {
    expect(await render({ link: "/pet store/Pet" })).toEqual({
      href: "/pet store/Pet",
      isSpa: true,
    });
  });
});
