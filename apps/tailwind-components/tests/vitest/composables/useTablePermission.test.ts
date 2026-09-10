import { describe, expect, it } from "vitest";
import { ref, type Ref } from "vue";
import { useTablePermission } from "../../../app/composables/useTablePermission";
import type { ISession, ITablePermission } from "../../../types/types";

function sessionWith(
  permissions: Partial<ITablePermission>[],
  roles: string[] = []
): Ref<ISession | null> {
  return ref({
    email: "user@test.com",
    admin: false,
    roles: { "pet store": roles },
    tablePermissions: { "pet store": permissions },
  } as unknown as ISession);
}

describe("useTablePermission", () => {
  it("should expose the permissions of the requested table", () => {
    const session = sessionWith([
      { id: "Pet", name: "Pet", canView: true, canInsert: true },
      { id: "Order", name: "Order", canView: true, canDelete: true },
    ]);

    const pet = useTablePermission(session, "pet store", "Pet");

    expect(pet.canView.value).toBe(true);
    expect(pet.canInsert.value).toBe(true);
    expect(pet.canUpdate.value).toBe(false);
    expect(pet.canDelete.value).toBe(false);
    expect(pet.canEdit.value).toBe(true);
  });

  it("should deny everything for a table without permissions", () => {
    const session = sessionWith([{ id: "Pet", name: "Pet", canView: true }]);

    const order = useTablePermission(session, "pet store", "Order");

    expect(order.canView.value).toBe(false);
    expect(order.canInsert.value).toBe(false);
    expect(order.canUpdate.value).toBe(false);
    expect(order.canDelete.value).toBe(false);
    expect(order.canEdit.value).toBe(false);
  });

  it("should always allow viewing ontology tables", () => {
    const session = sessionWith([]);

    const tag = useTablePermission(session, "pet store", "Tag", "ONTOLOGIES");

    expect(tag.canView.value).toBe(true);
    expect(tag.canEdit.value).toBe(false);
  });

  it("should report row level security and the roles of the user", () => {
    const session = sessionWith(
      [{ id: "Pet", name: "Pet", canView: true, isRowLevel: true }],
      ["group a"]
    );

    const pet = useTablePermission(session, "pet store", "Pet");

    expect(pet.isRowLevel.value).toBe(true);
    expect(pet.userRoles.value).toEqual(["group a"]);
  });
});
