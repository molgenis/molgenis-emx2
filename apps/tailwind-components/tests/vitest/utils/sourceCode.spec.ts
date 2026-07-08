import { describe, it, expect } from "vitest";
import { globKeyToRouteKey } from "../../../app/utils/sourceCode";

describe("globKeyToRouteKey", () => {
  it("strips ../pages prefix from a flat story key", () => {
    expect(globKeyToRouteKey("../pages/Button.story.vue")).toBe(
      "/Button.story.vue"
    );
  });

  it("strips ../pages prefix from a nested story key", () => {
    expect(globKeyToRouteKey("../pages/viz/ProgressMeter.story.vue")).toBe(
      "/viz/ProgressMeter.story.vue"
    );
  });
});
