import { beforeEach, describe, expect, test, vi } from "vitest";
import { mockNuxtImport } from "@nuxt/test-utils/runtime";
import { ref } from "vue";

const useRouteMock = vi.fn();
const useAsyncDataMock = vi.fn();

mockNuxtImport("useRoute", () => vi.fn(() => useRouteMock()));
mockNuxtImport("useAsyncData", () => vi.fn(() => useAsyncDataMock()));

import { useSession } from "../../../app/composables/useSession";

beforeEach(() => {
  vi.clearAllMocks();
});

describe("useSession for non schema path", () => {
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

describe("useSession for schema path", () => {
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
});
