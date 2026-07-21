// @vitest-environment happy-dom
import { describe, it, expect, vi, beforeEach } from "vitest";
import { defineComponent, h } from "vue";
import { mount } from "@vue/test-utils";
import { mockNuxtImport } from "@nuxt/test-utils/runtime";

const { navigateToMock } = vi.hoisted(() => ({ navigateToMock: vi.fn() }));
mockNuxtImport("navigateTo", () => navigateToMock);

vi.mock("../../../app/utils/getPrimaryKey", () => ({
  getPrimaryKey: vi.fn(),
}));

import { getPrimaryKey } from "../../../app/utils/getPrimaryKey";
import {
  useRecordNavigation,
  provideRecordNavigation,
} from "../../../app/composables/useRecordNavigation";

beforeEach(() => {
  vi.clearAllMocks();
  (getPrimaryKey as ReturnType<typeof vi.fn>).mockResolvedValue({
    id: "test123",
  });
});

function mountedDefaultNav(): ReturnType<typeof useRecordNavigation> {
  let result!: ReturnType<typeof useRecordNavigation>;
  mount(
    defineComponent({
      setup() {
        result = useRecordNavigation();
        return () => h("div");
      },
    })
  );
  return result;
}

describe("useRecordNavigation — default navigation URL building", () => {
  it("calls navigateTo with a path that includes the schemaId and tableId", async () => {
    const nav = mountedDefaultNav();
    await nav.navigateToRecord("mySchema", "Pet", { id: "test123" });
    expect(navigateToMock).toHaveBeenCalledOnce();
    const href: string = navigateToMock.mock.calls[0][0];
    expect(href).toContain("/mySchema/Pet/");
  });

  it("includes ?keys= query param in the URL", async () => {
    const nav = mountedDefaultNav();
    await nav.navigateToRecord("mySchema", "Pet", { id: "test123" });
    const href: string = navigateToMock.mock.calls[0][0];
    expect(href).toContain("?keys=");
  });

  it("encodes the primary key as JSON in the ?keys= param", async () => {
    (getPrimaryKey as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      id: "fluffy",
    });
    const nav = mountedDefaultNav();
    await nav.navigateToRecord("mySchema", "Pet", { id: "fluffy" });
    const href: string = navigateToMock.mock.calls[0][0];
    expect(href).toContain(
      encodeURIComponent(JSON.stringify({ id: "fluffy" }))
    );
  });

  it("uses the key value as the slug in the path", async () => {
    (getPrimaryKey as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      id: "buddy",
    });
    const nav = mountedDefaultNav();
    await nav.navigateToRecord("mySchema", "Pet", { id: "buddy" });
    const href: string = navigateToMock.mock.calls[0][0];
    expect(href).toMatch(/\/mySchema\/Pet\/buddy\?/);
  });

  it("uses refSchemaId as the schema prefix when provided", async () => {
    (getPrimaryKey as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      id: "abc",
    });
    const nav = mountedDefaultNav();
    await nav.navigateToRecord(
      "homeSchema",
      "Pet",
      { id: "abc" },
      "remoteSchema"
    );
    const href: string = navigateToMock.mock.calls[0][0];
    expect(href).toMatch(/^\/remoteSchema\/Pet\//);
  });

  it("passes refSchemaId to getPrimaryKey so it resolves the key in the right schema", async () => {
    const nav = mountedDefaultNav();
    await nav.navigateToRecord(
      "homeSchema",
      "Pet",
      { id: "row" },
      "remoteSchema"
    );
    expect(getPrimaryKey).toHaveBeenCalledWith(
      expect.anything(),
      "Pet",
      "remoteSchema"
    );
  });

  it("falls back to schemaId for getPrimaryKey when refSchemaId is not provided", async () => {
    const nav = mountedDefaultNav();
    await nav.navigateToRecord("mySchema", "Pet", { id: "row" });
    expect(getPrimaryKey).toHaveBeenCalledWith(
      expect.anything(),
      "Pet",
      "mySchema"
    );
  });
});

describe("provideRecordNavigation — override via provide/inject", () => {
  it("a child using useRecordNavigation gets the overridden navigateToRecord", () => {
    const customFn = vi.fn();
    let injectedNav: ReturnType<typeof useRecordNavigation> | undefined;

    mount(
      defineComponent({
        setup() {
          provideRecordNavigation({ navigateToRecord: customFn });
          return () =>
            h(
              defineComponent({
                setup() {
                  injectedNav = useRecordNavigation();
                  return () => h("div");
                },
              })
            );
        },
      })
    );

    expect(injectedNav?.navigateToRecord).toBe(customFn);
  });

  it("useRecordNavigation returns default (navigates) when no ancestor provides", async () => {
    let nav: ReturnType<typeof useRecordNavigation> | undefined;
    mount(
      defineComponent({
        setup() {
          nav = useRecordNavigation();
          return () => h("div");
        },
      })
    );
    await nav!.navigateToRecord("s", "t", { id: "r" });
    expect(navigateToMock).toHaveBeenCalledOnce();
  });

  it("the returned nav from provideRecordNavigation uses the overriding function", () => {
    const customFn = vi.fn();
    let returnedNav: ReturnType<typeof provideRecordNavigation> | undefined;

    mount(
      defineComponent({
        setup() {
          returnedNav = provideRecordNavigation({ navigateToRecord: customFn });
          return () => h("div");
        },
      })
    );

    expect(returnedNav?.navigateToRecord).toBe(customFn);
  });
});
