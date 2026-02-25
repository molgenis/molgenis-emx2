import { beforeEach, describe, expect, test, vi } from "vitest";
// Mock the router composables before importing useSession
vi.mock("#app/composables/router", () => ({
  useRoute: vi.fn(),
  useRouter: vi.fn(),
}));
vi.mock("#app/composables/asyncData", () => ({
  useAsyncData: vi.fn(),
}));
// @ts-ignore
import { useRoute } from "#app/composables/router";
//@ts-ignore
import { useAsyncData } from "#app/composables/asyncData";
import { useSession } from "../../../app/composables/useSession";
import { ref } from "vue";

beforeEach(() => {
  vi.clearAllMocks();
});

describe("useSession for non schema path", () => {
  test("should fetch session details if session is empty", async () => {
    useRoute.mockReturnValue({
      params: {},
    });

    useAsyncData.mockResolvedValueOnce({
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
    useRoute.mockReturnValue({
      params: { schema: "abc" },
    });

    useAsyncData
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

    const session = await useSession();

    expect(session.session.value?.roles).toEqual(["Editor"]);
  });
});
