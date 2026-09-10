import { describe, expect, it } from "vitest";
import { rowMatchesUserRole } from "../../../app/utils/rowMatchesUserRole";

describe("rowMatchesUserRole", () => {
  it("should match when one of the user roles owns the row", () => {
    expect(rowMatchesUserRole({ mg_roles: ["group a"] }, ["group a"])).toBe(
      true
    );
    expect(
      rowMatchesUserRole({ mg_roles: ["group a"] }, ["group b", "group a"])
    ).toBe(true);
  });

  it("should not match when the row is owned by another role", () => {
    expect(rowMatchesUserRole({ mg_roles: ["group a"] }, ["group b"])).toBe(
      false
    );
  });

  it("should not match when the row has no owner or the user has no roles", () => {
    expect(rowMatchesUserRole({ mg_roles: [] }, ["group a"])).toBe(false);
    expect(rowMatchesUserRole({ name: "pooky" }, ["group a"])).toBe(false);
    expect(rowMatchesUserRole({ mg_roles: ["group a"] }, [])).toBe(false);
  });
});
