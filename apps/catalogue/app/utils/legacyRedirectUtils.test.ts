import { describe, it, expect } from "vitest";
import { getLegacyRedirectTarget } from "./legacyRedirectUtils";

describe("getLegacyRedirectTarget", () => {
  it("should redirect /catalogue/collections/cohort1", () => {
    expect(getLegacyRedirectTarget("/catalogue/collections/cohort1")).toBe(
      "/cohort1?catalogue=catalogue"
    );
  });

  it("should redirect /myNet/networks/subNet", () => {
    expect(getLegacyRedirectTarget("/myNet/networks/subNet")).toBe(
      "/subNet?catalogue=myNet"
    );
  });

  it("should return null for paths with fewer than 3 segments", () => {
    expect(getLegacyRedirectTarget("/catalogue/collections")).toBeNull();
  });

  it("should return null for paths with more than 3 segments", () => {
    expect(getLegacyRedirectTarget("/cat/collections/id/extra")).toBeNull();
  });

  it("should return null for single-segment paths", () => {
    expect(getLegacyRedirectTarget("/catalogue")).toBeNull();
  });

  it("should return null for root path", () => {
    expect(getLegacyRedirectTarget("/")).toBeNull();
  });

  it("should return null for reserved route 'all'", () => {
    expect(getLegacyRedirectTarget("/all/collections/cohort1")).toBeNull();
  });

  it("should return null for reserved route 'about'", () => {
    expect(getLegacyRedirectTarget("/about/collections/cohort1")).toBeNull();
  });

  it("should return null for reserved route 'tables'", () => {
    expect(getLegacyRedirectTarget("/tables/collections/cohort1")).toBeNull();
  });

  it("should return null for unsupported resource type 'variables'", () => {
    expect(getLegacyRedirectTarget("/catalogue/variables/var1")).toBeNull();
  });

  it("should return null for unsupported resource type 'about'", () => {
    expect(getLegacyRedirectTarget("/catalogue/about/something")).toBeNull();
  });
});
