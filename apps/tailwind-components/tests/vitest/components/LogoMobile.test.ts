import { mountSuspended } from "@nuxt/test-utils/runtime";
import { describe, expect, test } from "vitest";
import LogoMobile from "../../../app/components/LogoMobile.vue";

const routeWithLogoFile = { query: { logo: "uncan-white.png" } };

describe("LogoMobile alt text", () => {
  test("names the mark with the supplied alt when a logo file is resolved", async () => {
    const logo = await mountSuspended(LogoMobile, {
      props: { alt: "LifeCycle" },
      route: routeWithLogoFile,
    });

    expect(logo.get("img").attributes("alt")).toBe("LifeCycle");
  });

  test("falls back to the generic alt when none is supplied", async () => {
    const logo = await mountSuspended(LogoMobile, {
      route: routeWithLogoFile,
    });

    expect(logo.get("img").attributes("alt")).toBe("logo");
  });

  test("names the mark with the supplied alt on an image url", async () => {
    const logo = await mountSuspended(LogoMobile, {
      props: { image: "https://example.org/mark.png", alt: "LifeCycle" },
    });

    expect(logo.get("img").attributes("alt")).toBe("LifeCycle");
  });

  test("leaves an image url unnamed when no alt is supplied", async () => {
    const logo = await mountSuspended(LogoMobile, {
      props: { image: "https://example.org/mark.png" },
    });

    expect(logo.get("img").attributes("src")).toBe(
      "https://example.org/mark.png"
    );
    expect(logo.get("img").attributes("alt")).toBeUndefined();
  });
});
