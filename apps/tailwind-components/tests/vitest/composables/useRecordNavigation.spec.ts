// @vitest-environment happy-dom
import { describe, it, expect, vi, beforeEach } from "vitest";
import { defineComponent, h } from "vue";
import { mount } from "@vue/test-utils";
import { mockNuxtImport } from "@nuxt/test-utils/runtime";

const { navigateToMock } = vi.hoisted(() => ({ navigateToMock: vi.fn() }));
mockNuxtImport("navigateTo", () => navigateToMock);

import {
  useRecordNavigation,
  provideRecordNavigation,
} from "../../../app/composables/useRecordNavigation";

function schemaMetadata(schemaId: string, petKeyColumnId: string = "id") {
  return {
    id: schemaId,
    label: schemaId,
    tables: [
      {
        id: "Pet",
        schemaId,
        name: "Pet",
        label: "Pet",
        tableType: "DATA",
        columns: [
          {
            id: petKeyColumnId,
            label: petKeyColumnId,
            columnType: "STRING",
            key: 1,
          },
        ],
      },
      {
        id: "Resources",
        schemaId,
        name: "Resources",
        label: "Resources",
        tableType: "DATA",
        columns: [
          { id: "id", label: "id", columnType: "STRING", key: 1 },
          {
            id: "tables",
            label: "tables",
            columnType: "PARTS",
            refTableId: "Tables",
            refSchemaId: schemaId,
            refBackId: "resource",
          },
        ],
      },
      {
        id: "Tables",
        schemaId,
        name: "Tables",
        label: "Tables",
        tableType: "DATA",
        columns: [
          {
            id: "resource",
            label: "resource",
            columnType: "REF",
            refTableId: "Resources",
            refSchemaId: schemaId,
            key: 1,
          },
          { id: "name", label: "name", columnType: "STRING", key: 1 },
        ],
      },
    ],
  };
}

function stubMetadataFetch(petKeyColumnBySchema: Record<string, string> = {}) {
  vi.stubGlobal(
    "$fetch",
    vi.fn(async (url: string) => {
      const schemaId = url.split("/")[1] ?? "";
      return {
        data: {
          _schema: schemaMetadata(schemaId, petKeyColumnBySchema[schemaId]),
        },
      };
    })
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  stubMetadataFetch();
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
    const nav = mountedDefaultNav();
    await nav.navigateToRecord("mySchema", "Pet", { id: "fluffy" });
    const href: string = navigateToMock.mock.calls[0][0];
    expect(href).toContain(
      encodeURIComponent(JSON.stringify({ id: "fluffy" }))
    );
  });

  it("uses the key value as the slug in the path", async () => {
    const nav = mountedDefaultNav();
    await nav.navigateToRecord("mySchema", "Pet", { id: "buddy" });
    const href: string = navigateToMock.mock.calls[0][0];
    expect(href).toMatch(/\/mySchema\/Pet\/buddy\?/);
  });

  it("uses refSchemaId as the schema prefix when provided", async () => {
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

  it("resolves the primary key against refSchemaId's metadata, not the home schema's", async () => {
    stubMetadataFetch({ remoteKeySchema: "petCode" });
    const nav = mountedDefaultNav();
    await nav.navigateToRecord(
      "homeKeySchema",
      "Pet",
      { id: "ignored", petCode: "P1" },
      "remoteKeySchema"
    );
    expect(navigateToMock).toHaveBeenCalledWith(
      `/remoteKeySchema/Pet/P1?keys=${encodeURIComponent(
        JSON.stringify({ petCode: "P1" })
      )}`
    );
  });

  it("navigates to the nested canonical path when the row sits in a parts chain", async () => {
    const nav = mountedDefaultNav();
    await nav.navigateToRecord("mySchema", "Tables", {
      resource: { id: "c1" },
      name: "t1",
    });
    expect(navigateToMock).toHaveBeenCalledWith(
      "/mySchema/Resources/c1/tables/t1"
    );
  });

  it("reports the failure and stays put when the metadata fetch fails, rather than rejecting out of a click handler", async () => {
    vi.stubGlobal(
      "$fetch",
      vi.fn(async () => {
        throw new Error("metadata unreachable");
      })
    );
    const consoleError = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});
    const nav = mountedDefaultNav();

    try {
      await expect(
        nav.navigateToRecord("unreachableSchema", "Pet", { id: "test123" })
      ).resolves.toBeUndefined();

      expect(navigateToMock).not.toHaveBeenCalled();
      expect(consoleError).toHaveBeenCalledTimes(4);
      expect(
        consoleError.mock.calls.filter(([message]) =>
          String(message).includes("Could not fetch metadata for schema")
        )
      ).toHaveLength(2);
      expect(consoleError).toHaveBeenCalledWith(
        expect.stringContaining("nested record path"),
        expect.anything()
      );
      expect(consoleError).toHaveBeenCalledWith(
        expect.stringContaining("Could not resolve the primary key"),
        expect.anything()
      );
    } finally {
      consoleError.mockRestore();
    }
  });

  it("resolves the primary key against schemaId's metadata when refSchemaId is not provided", async () => {
    stubMetadataFetch({ ownKeySchema: "petCode" });
    const nav = mountedDefaultNav();
    await nav.navigateToRecord("ownKeySchema", "Pet", {
      id: "ignored",
      petCode: "P2",
    });
    expect(navigateToMock).toHaveBeenCalledWith(
      `/ownKeySchema/Pet/P2?keys=${encodeURIComponent(
        JSON.stringify({ petCode: "P2" })
      )}`
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
    await nav!.navigateToRecord("defaultNavSchema", "Pet", { id: "r" });
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
