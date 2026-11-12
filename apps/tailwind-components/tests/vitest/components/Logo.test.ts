import { mountSuspended } from "@nuxt/test-utils/runtime";
import { describe, expect, test } from "vitest";
import Logo from "../../../app/components/Logo.vue";

const routeWithLogoFile = { query: { logo: "uncan-white.png" } };

describe("Logo alt text", () => {
  test("names the mark with the supplied alt when a logo file is resolved", async () => {
    const logo = await mountSuspended(Logo, {
      props: { alt: "LifeCycle" },
      route: routeWithLogoFile,
    });

    expect(logo.get("img").attributes("alt")).toBe("LifeCycle");
  });

  test("falls back to the generic alt when none is supplied", async () => {
    const logo = await mountSuspended(Logo, {
      route: routeWithLogoFile,
    });

    expect(logo.get("img").attributes("alt")).toBe("logo");
  });

  test("names the mark with the supplied alt on an image url", async () => {
    const logo = await mountSuspended(Logo, {
      props: { image: "https://example.org/mark.png", alt: "LifeCycle" },
    });

    expect(logo.get("img").attributes("alt")).toBe("LifeCycle");
  });

  test("leaves an image url unnamed when no alt is supplied", async () => {
    const logo = await mountSuspended(Logo, {
      props: { image: "https://example.org/mark.png" },
    });

    expect(logo.get("img").attributes("alt")).toBeUndefined();
  });
});
