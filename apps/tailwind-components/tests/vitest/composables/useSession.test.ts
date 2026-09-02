import { mockNuxtImport } from "@nuxt/test-utils/runtime";
import { beforeEach, describe, expect, it, test, vi } from "vitest";
import { ref } from "vue";

const useRouteMock = vi.fn();
const useStateMock = vi.fn();
const useAsyncDataMock = vi.fn();
// getRowLevelRoles cannot use useAsyncData: it runs after an await, so there is no nuxt context
const fetchMock = vi.fn();

mockNuxtImport("useRoute", () => vi.fn(() => useRouteMock()));
mockNuxtImport("useState", () => vi.fn((key, init) => useStateMock(key, init)));
mockNuxtImport("useAsyncData", () => vi.fn(() => useAsyncDataMock()));
vi.stubGlobal("$fetch", fetchMock);

import { useSession } from "../../../app/composables/useSession";

function schemaRolesResponse(
  roles: {
    name: string;
    permissions: { table: string; isRowLevel: boolean }[];
  }[]
) {
  return { data: { _schema: { roles } } };
}

describe("useSession", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    const store = new Map<string, ReturnType<typeof ref>>();
    useStateMock.mockImplementation((key, init) => {
      if (!store.has(key)) {
        store.set(key, ref(init()));
      }
      return store.get(key);
    });
    fetchMock.mockResolvedValue(schemaRolesResponse([]));
  });

  describe("for non schema path", () => {
    test("should fetch session details if session is empty", async () => {
      useAsyncDataMock.mockResolvedValueOnce({
        data: ref({
          data: {
            _session: {
              email: "test@test.com",
              admin: true,
              token: "abc123",
            },
          },
        }),
        error: ref(null),
        pending: ref(false),
      });

      const session = await useSession();

      expect(session.isAdmin.value).toEqual(true);
      expect(session.session.value).toEqual({
        email: "test@test.com",
        admin: true,
        token: "abc123",
      });
    });
  });

  describe("for schema path", () => {
    test("should fetch session details if session is empty", async () => {
      useRouteMock.mockReturnValue({
        params: { schema: "abc" },
      });

      useAsyncDataMock
        .mockResolvedValueOnce({
          data: ref({
            data: { _session: { roles: ["Editor"] } },
          }),
          error: ref(null),
          pending: ref(false),
        })
        .mockResolvedValueOnce({
          data: ref({
            data: {
              _session: {
                email: "user@test.com",
                admin: true,
                token: "123",
              },
            },
          }),
          error: ref(null),
          pending: ref(false),
        });

      const session = await useSession("abc");

      expect(session.session.value?.roles).toEqual({
        abc: ["Editor"],
      });
    });

    test("should load the row level roles, the session is loaded before they are resolved", async () => {
      useRouteMock.mockReturnValue({
        params: { schema: "abc" },
      });

      useAsyncDataMock
        .mockResolvedValueOnce({
          data: ref({ data: { _session: { roles: ["Manager"] } } }),
          error: ref(null),
          pending: ref(false),
        })
        .mockResolvedValueOnce({
          data: ref({
            data: {
              _session: {
                email: "user@test.com",
                admin: false,
                token: "123",
              },
            },
          }),
          error: ref(null),
          pending: ref(false),
        });

      fetchMock.mockResolvedValue(
        schemaRolesResponse([
          {
            name: "DragonKeeper",
            permissions: [{ table: "Pet", isRowLevel: true }],
          },
          {
            name: "Viewer",
            permissions: [{ table: "Pet", isRowLevel: false }],
          },
        ])
      );

      const session = await useSession("abc");

      expect(session.isManager.value).toBe(true);
      expect(fetchMock).toHaveBeenCalledWith("/abc/graphql", expect.anything());
      expect(session.rowLevelRoles.value).toEqual(["DragonKeeper"]);

      // a second form on the same schema reuses them
      const other = await useSession("abc");

      expect(other.rowLevelRoles.value).toEqual(["DragonKeeper"]);
      expect(fetchMock).toHaveBeenCalledTimes(1);
    });

    test("should not reject when the row level roles cannot be fetched", async () => {
      useRouteMock.mockReturnValue({
        params: { schema: "abc" },
      });

      useAsyncDataMock
        .mockResolvedValueOnce({
          data: ref({ data: { _session: { roles: ["Manager"] } } }),
          error: ref(null),
          pending: ref(false),
        })
        .mockResolvedValueOnce({
          data: ref({
            data: {
              _session: { email: "user@test.com", admin: false, token: "123" },
            },
          }),
          error: ref(null),
          pending: ref(false),
        });

      fetchMock.mockRejectedValue(new Error("403 Forbidden"));
      const consoleError = vi
        .spyOn(console, "error")
        .mockImplementation(() => {});

      const session = await useSession("abc");

      expect(session.rowLevelRoles.value).toEqual([]);
      expect(session.isManager.value).toBe(true);
      expect(consoleError).toHaveBeenCalled();
      consoleError.mockRestore();
    });

    test("should not fetch the row level roles without owner, manager or admin rights", async () => {
      useRouteMock.mockReturnValue({
        params: { schema: "abc" },
      });

      useAsyncDataMock
        .mockResolvedValueOnce({
          data: ref({ data: { _session: { roles: ["Editor"] } } }),
          error: ref(null),
          pending: ref(false),
        })
        .mockResolvedValueOnce({
          data: ref({
            data: {
              _session: {
                email: "user@test.com",
                admin: false,
                token: "123",
              },
            },
          }),
          error: ref(null),
          pending: ref(false),
        });

      const session = await useSession("abc");

      expect(session.rowLevelRoles.value).toEqual([]);
      expect(fetchMock).not.toHaveBeenCalled();
    });
  });

  describe("permissions for a non schema path", () => {
    it("should provide no permissions", async () => {
      useAsyncDataMock.mockResolvedValueOnce({
        data: ref({
          data: {
            _session: {
              email: "anonymous",
              admin: false,
              schemas: ["pet store", "catalogue"],
              settings: [],
            },
          },
        }),
        error: ref(null),
        pending: ref(false),
      });

      const session = await useSession();

      expect(session.session.value?.roles).toBeUndefined();
      expect(session.session.value?.tablePermissions).toBeUndefined();
      expect(session.isAdmin.value).toBe(false);
    });

    describe("permissions for a schema path", () => {
      it("should provide permissions for the specified schema and its tables", async () => {
        const sessionResult = {
          data: ref({
            data: {
              _session: {
                email: "anonymous",
                admin: false,
                schemas: ["pet store", "catalogue"],
                settings: [],
              },
            },
          }),
          error: ref(null),
          pending: ref(false),
        };

        const roles = ["Editor", "Viewer"];
        const tablePermissions = [
          {
            id: "Pet",
            name: "Pet",
            canView: true,
            canInsert: false,
            canUpdate: false,
            canDelete: false,
            isRowLevel: false,
          },
        ];
        const permissionsResult = {
          data: ref({
            data: {
              _session: {
                roles: roles,
                tablePermissions: tablePermissions,
              },
            },
          }),
          error: ref(null),
          pending: ref(false),
        };
        useAsyncDataMock
          .mockResolvedValueOnce(permissionsResult)
          .mockResolvedValueOnce(sessionResult);

        const session = await useSession("pet store");

        expect(session.session.value?.roles).toEqual({ "pet store": roles });
        expect(session.session.value?.tablePermissions).toEqual({
          "pet store": tablePermissions,
        });
        expect(session.isAdmin.value).toBe(false);
      });
    });
  });
});
