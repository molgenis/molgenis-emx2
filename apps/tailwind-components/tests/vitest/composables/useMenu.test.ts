import { describe, expect, it } from "vitest";
import { parseMobileMenuItems } from "../../../app/composables/useMenu";

describe("parseMobileMenuItems", () => {
  it("returns up to three menu items", () => {
    const menu = [
      { label: "Tables", location: "/pet%20store" },
      { label: "Pets", location: "/pet%20store/Pet" },
      {
        label: "Help",
        location: "pet%20store/docs",
        linkType: "internalPageLink",
      },
    ];

    expect(parseMobileMenuItems(JSON.stringify(menu))).toEqual(menu);
  });

  it("rejects a menu with more than three items", () => {
    const menu = ["A", "B", "C", "D"].map((label) => ({
      label,
      location: `/${label}`,
    }));

    expect(parseMobileMenuItems(JSON.stringify(menu))).toEqual([]);
  });

  it("rejects a menu with a submenu", () => {
    const menu = [
      {
        label: "Data",
        location: "",
        submenu: [{ label: "Pets", location: "/pet%20store/Pet" }],
      },
    ];

    expect(parseMobileMenuItems(JSON.stringify(menu))).toEqual([]);
  });

  it("accepts an item with an empty submenu", () => {
    const menu = [{ label: "Tables", location: "/pet%20store", submenu: [] }];

    expect(parseMobileMenuItems(JSON.stringify(menu))).toEqual(menu);
  });

  it("rejects invalid json without throwing", () => {
    expect(parseMobileMenuItems("[{ not json")).toEqual([]);
  });

  it("rejects json that is not a menu", () => {
    expect(parseMobileMenuItems(JSON.stringify([{ label: 1 }]))).toEqual([]);
    expect(parseMobileMenuItems(JSON.stringify({ label: "A" }))).toEqual([]);
  });

  it("returns an empty menu when the setting is missing", () => {
    expect(parseMobileMenuItems(undefined)).toEqual([]);
  });
});
