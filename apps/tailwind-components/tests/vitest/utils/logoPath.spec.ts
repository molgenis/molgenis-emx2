import { describe, expect, test } from "vitest";
import { toLogoPath } from "../../../app/utils/logoPath";

describe("toLogoPath", () => {
  test("appends .svg to a bare logo name", () => {
    expect(toLogoPath("HDSU-logo")).toBe("/_nuxt-styles/logos/HDSU-logo.svg");
  });

  test("appends .svg to a dotted name that is not an image extension", () => {
    expect(toLogoPath("UMCGkort.woordbeeld")).toBe(
      "/_nuxt-styles/logos/UMCGkort.woordbeeld.svg"
    );
  });

  test("uses a name ending in an image extension verbatim", () => {
    expect(toLogoPath("uncan-purple.png")).toBe(
      "/_nuxt-styles/logos/uncan-purple.png"
    );
  });

  test("uses an explicit .svg name verbatim", () => {
    expect(toLogoPath("HDSU-logo.svg")).toBe(
      "/_nuxt-styles/logos/HDSU-logo.svg"
    );
  });
});
