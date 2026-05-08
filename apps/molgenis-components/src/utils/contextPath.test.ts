import { describe, expect, test, vi, afterEach } from "vitest";
import { getContextPath } from "./contextPath";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("getContextPath", () => {
  test("returns server-injected context path when __molgenisContextPath is set", () => {
    vi.stubGlobal("window", {
      __molgenisContextPath: "/molgenis",
      location: { pathname: "/molgenis/petstore/tables/" },
    });
    expect(getContextPath()).toBe("/molgenis");
  });

  test("returns empty string when __molgenisContextPath is empty string", () => {
    vi.stubGlobal("window", {
      __molgenisContextPath: "",
      location: { pathname: "/molgenis/petstore/tables/" },
    });
    expect(getContextPath()).toBe("");
  });

  test("returns empty string when no context path (/apps/central/)", () => {
    vi.stubGlobal("window", { location: { pathname: "/apps/central/" } });
    expect(getContextPath()).toBe("");
  });

  test("returns context path prefix for /apps/ page", () => {
    vi.stubGlobal("window", { location: { pathname: "/molgenis/apps/central/" } });
    expect(getContextPath()).toBe("/molgenis");
  });

  test("returns multi-segment context path", () => {
    vi.stubGlobal("window", { location: { pathname: "/org/app/apps/central/" } });
    expect(getContextPath()).toBe("/org/app");
  });

  test("returns empty string for schema page without server injection", () => {
    vi.stubGlobal("window", { location: { pathname: "/molgenis/petstore/tables/" } });
    expect(getContextPath()).toBe("");
  });

  test("returns empty string in SSR (no window)", () => {
    vi.stubGlobal("window", undefined);
    expect(getContextPath()).toBe("");
  });
});
