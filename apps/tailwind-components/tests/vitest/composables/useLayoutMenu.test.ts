import { describe, expect, it } from "vitest";
import { filterMenuForUser } from "../../../app/composables/useLayoutMenu";
import type { ISession, MenuItem } from "../../../types/types";

const menu: MenuItem[] = [
  { label: "Tables", location: "/pet%20store" },
  { label: "Schema", location: "pet%20store/schema", role: "Viewer" },
  {
    label: "More",
    location: "",
    submenu: [
      { label: "Tasks", location: "pet%20store/tasks", role: "Manager" },
    ],
  },
];

describe("filterMenuForUser", () => {
  it("hides items with a role and drops submenus left empty", () => {
    expect(filterMenuForUser(menu, null).map((item) => item.label)).toEqual([
      "Tables",
    ]);
  });

  it("shows everything to an admin", () => {
    const session: ISession = { email: "admin", admin: true };

    expect(filterMenuForUser(menu, session)).toEqual(menu);
  });
});
