import { describe, it, expect } from "vitest";
import {
  getCatalogueId,
  buildResourceUrl,
  buildCatalogueBreadcrumbs,
} from "./catalogueContextUtils";

describe("getCatalogueId", () => {
  it("should return query param when set", () => {
    expect(getCatalogueId("myNetwork", "someResource", "/someResource")).toBe(
      "myNetwork"
    );
  });

  it("should return resourceId when path has a sub-route", () => {
    expect(getCatalogueId(undefined, "net1", "/net1/collections")).toBe("net1");
  });

  it("should return null at resource root without query param", () => {
    expect(getCatalogueId(undefined, "net1", "/net1")).toBeNull();
  });

  it("should return null when no resourceId and no query param", () => {
    expect(getCatalogueId(undefined, undefined, "/")).toBeNull();
  });

  it("should return null when resourceId is empty string", () => {
    expect(getCatalogueId(undefined, "", "/")).toBeNull();
  });

  it("should prefer query param over path-based detection", () => {
    expect(
      getCatalogueId("fromQuery", "fromPath", "/fromPath/collections")
    ).toBe("fromQuery");
  });

  it("should return null for trailing-slash-only path after resource", () => {
    expect(getCatalogueId(undefined, "net1", "/net1/")).toBeNull();
  });
});

describe("buildResourceUrl", () => {
  it("should return path as-is when no catalogue scope", () => {
    expect(buildResourceUrl("/someResource/collections", null)).toBe(
      "/someResource/collections"
    );
  });

  it("should prepend slash if path does not start with one", () => {
    expect(buildResourceUrl("someResource", null)).toBe("/someResource");
  });

  it("should append catalogue query param when scoped", () => {
    expect(buildResourceUrl("/cohort1/collections", "net1")).toBe(
      "/cohort1/collections?catalogue=net1"
    );
  });

  it("should not append catalogue param when path resourceId matches catalogueId", () => {
    expect(buildResourceUrl("/net1/collections", "net1")).toBe(
      "/net1/collections"
    );
  });

  it("should use & separator when path already contains query params", () => {
    expect(buildResourceUrl("/cohort1?foo=bar", "net1")).toBe(
      "/cohort1?foo=bar&catalogue=net1"
    );
  });

  it("should not duplicate catalogue param for catalogueId path", () => {
    expect(buildResourceUrl("net1", "net1")).toBe("/net1");
  });
});

describe("buildCatalogueBreadcrumbs", () => {
  it("should return items unchanged when not scoped", () => {
    const items = [{ label: "Collections", url: "/collections" }];
    expect(buildCatalogueBreadcrumbs(items, null)).toEqual(items);
  });

  it("should prepend catalogue breadcrumb when scoped", () => {
    const items = [{ label: "Collections", url: "/collections" }];
    const result = buildCatalogueBreadcrumbs(items, "net1");
    expect(result).toHaveLength(2);
    expect(result[0].label).toBe("net1");
    expect(result[0].url).toBe("/net1");
    expect(result[1]).toEqual(items[0]);
  });

  it("should handle empty items array when scoped", () => {
    const result = buildCatalogueBreadcrumbs([], "net1");
    expect(result).toHaveLength(1);
    expect(result[0].label).toBe("net1");
  });
});
