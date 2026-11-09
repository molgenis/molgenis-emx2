import { beforeEach, describe, expect, it, vi } from "vitest";
import { ref } from "vue";
import type { ISession } from "../../../types/types";

vi.mock("../../../app/composables/useMenu", () => ({
  useMenu: vi.fn(),
}));

import { useMenu } from "../../../app/composables/useMenu";
import { useLayoutMenu } from "../../../app/composables/useLayoutMenu";

async function visibleLabels(session: ISession, schema = "pet store") {
  const { userMenuItems } = await useLayoutMenu(ref(schema), ref(session));
  return userMenuItems.value.map((item) => item.label);
}

function sessionWithRoles(roles: string[]): ISession {
  return {
    email: "user",
    admin: false,
    roles: { "pet store": roles },
  };
}

describe("useLayoutMenu", () => {
  beforeEach(() => {
    vi.mocked(useMenu).mockResolvedValue(ref([]) as never);
  });

  it("shows a Manager the items for their role in this schema", async () => {
    const labels = await visibleLabels(
      sessionWithRoles(["Viewer", "Editor", "Manager"])
    );
    expect(labels).toContain("Theme");
    expect(labels).toContain("Settings");
  });

  it("hides admin-only pages from a Manager", async () => {
    const labels = await visibleLabels(sessionWithRoles(["Manager"]));
    expect(labels).not.toContain("Analytics");
  });

  it("hides Manager items from a Viewer", async () => {
    const labels = await visibleLabels(sessionWithRoles(["Viewer"]));
    expect(labels).toContain("Schema");
    expect(labels).not.toContain("Theme");
  });

  it("does not use roles from another schema", async () => {
    const labels = await visibleLabels(
      { email: "user", admin: false, roles: { other: ["Manager"] } },
      "pet store"
    );
    expect(labels).not.toContain("Theme");
  });

  it("shows items without a role to anonymous users", async () => {
    const labels = await visibleLabels({ email: "anonymous", admin: false });
    expect(labels).toContain("Tables");
    expect(labels).not.toContain("Theme");
  });

  it("shows everything to an admin", async () => {
    const labels = await visibleLabels({ email: "admin", admin: true });
    expect(labels).toContain("Analytics");
    expect(labels).toContain("Theme");
  });
});
