import { flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import { defineComponent, h, Suspense } from "vue";
import RecordActions from "../../../../app/components/display/RecordActions.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

function catalogueSchema(schemaId: string) {
  const resourceColumns: IColumn[] = [
    { id: "id", label: "Id", columnType: "STRING", key: 1 },
    { id: "name", label: "Name", columnType: "STRING" },
    { id: "mg_tableclass", label: "mg_tableclass", columnType: "STRING" },
  ];
  return {
    id: schemaId,
    label: schemaId,
    tables: [
      {
        id: "Resources",
        name: "Resources",
        label: "Resources",
        schemaId,
        tableType: "DATA",
        columns: resourceColumns,
      },
      {
        id: "Collections",
        name: "Collections",
        label: "Collections",
        schemaId,
        tableType: "DATA",
        inheritId: "Resources",
        columns: [
          ...resourceColumns.map((column) => ({ ...column, inherited: true })),
          {
            id: "collectionType",
            label: "Collection type",
            columnType: "STRING",
          },
        ],
      },
    ],
  };
}

interface IGraphqlStub {
  recordQueries: string[];
}

function stubCatalogueGraphql(
  schemaId: string,
  { roles = ["Editor"], failRecordQueries = false } = {}
): IGraphqlStub {
  const storedCollection: Record<string, unknown> = {
    id: "c1",
    name: "Cohort One",
    mg_tableclass: `${schemaId}.Collections`,
    collectionType: "Population cohort",
  };
  const schema = catalogueSchema(schemaId);
  const recordQueries: string[] = [];

  vi.stubGlobal(
    "$fetch",
    vi.fn(async (url: string, options: { body: any }) => {
      const query =
        typeof options.body === "string"
          ? JSON.parse(options.body).query
          : options.body.query;

      if (query.includes("_session")) {
        return {
          data: {
            _session: {
              email: "editor@molgenis.org",
              admin: false,
              token: "token",
              roles: url === `/${schemaId}/graphql` ? roles : undefined,
            },
          },
        };
      }

      recordQueries.push(query);
      if (failRecordQueries) {
        throw new Error("graphql unavailable");
      }

      if (query.includes("_schema")) return { data: { _schema: schema } };

      const tableId = query.slice(
        query.indexOf("query ") + "query ".length,
        query.indexOf("(")
      );
      const table = schema.tables.find((entry) => entry.id === tableId)!;
      const projected = Object.fromEntries(
        table.columns
          .filter((column) => column.id in storedCollection)
          .map((column) => [column.id, storedCollection[column.id]])
      );
      return {
        data: { [tableId]: [projected], [`${tableId}_agg`]: { count: 1 } },
      };
    })
  );

  return { recordQueries };
}

const mounted: { unmount: () => void }[] = [];

async function mountRecordActions(schemaId: string, tableId: string) {
  const wrapper = mount(
    defineComponent({
      render: () =>
        h(Suspense, null, {
          default: h(RecordActions, { schemaId, tableId, rowId: { id: "c1" } }),
        }),
    }),
    { attachTo: document.body }
  );
  await flushPromises();
  mounted.push(wrapper);
  return wrapper;
}

async function openEditFormOf(schemaId: string, tableId: string) {
  stubCatalogueGraphql(schemaId);
  const wrapper = await mountRecordActions(schemaId, tableId);

  const editButton = wrapper
    .findAll("button")
    .find((button) => button.text().includes("Edit"))!;
  await editButton.trigger("click");
  await flushPromises();
  await flushPromises();
}

function editFormFieldLabels() {
  return Array.from(document.body.querySelectorAll("label")).map((label) =>
    label.textContent?.trim()
  );
}

function editFormFieldValue(
  schemaId: string,
  formTableId: string,
  columnId: string
) {
  return document.body.querySelector<HTMLInputElement>(
    `#${schemaId}-${formTableId}-${columnId}-form-field-input`
  )?.value;
}

describe("display/RecordActions.vue subclass fields", () => {
  afterEach(() => {
    mounted.splice(0).forEach((wrapper) => wrapper.unmount());
    vi.unstubAllGlobals();
    sessionStorage.clear();
  });

  it("offers the fields of the record's own subclass when the URL names the declaring superclass", async () => {
    await openEditFormOf("recordActionsSubclass", "Resources");

    expect(editFormFieldLabels()).toEqual(["Id", "Name", "Collection type"]);
  });

  it("fills the subclass fields with the subclass row, not an empty form", async () => {
    const schemaId = "recordActionsSubclassValues";
    await openEditFormOf(schemaId, "Resources");

    expect(editFormFieldValue(schemaId, "Collections", "collectionType")).toBe(
      "Population cohort"
    );
  });

  it("keeps offering the table's own fields when the row is not of a subclass", async () => {
    await openEditFormOf("recordActionsOwnClass", "Collections");

    expect(editFormFieldLabels()).toEqual(["Id", "Name", "Collection type"]);
  });
});

describe("display/RecordActions.vue without modify rights", () => {
  afterEach(() => {
    mounted.splice(0).forEach((wrapper) => wrapper.unmount());
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    sessionStorage.clear();
  });

  it("fetches no record at all when the viewer may not modify it", async () => {
    const schemaId = "recordActionsViewer";
    const { recordQueries } = stubCatalogueGraphql(schemaId, { roles: [] });

    await mountRecordActions(schemaId, "Resources");

    expect(recordQueries).toEqual([]);
  });

  it("offers no Edit or Delete button when the viewer may not modify it", async () => {
    const schemaId = "recordActionsViewerButtons";
    stubCatalogueGraphql(schemaId, { roles: [] });

    const wrapper = await mountRecordActions(schemaId, "Resources");

    expect(wrapper.findAll("button").map((button) => button.text())).toEqual(
      []
    );
  });
});

describe("display/RecordActions.vue when the record cannot be loaded", () => {
  afterEach(() => {
    mounted.splice(0).forEach((wrapper) => wrapper.unmount());
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    sessionStorage.clear();
  });

  it("reports the failure instead of blanking the render or offering dead buttons", async () => {
    const schemaId = "recordActionsLoadFailure";
    const consoleError = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});
    stubCatalogueGraphql(schemaId, { failRecordQueries: true });

    const wrapper = await mountRecordActions(schemaId, "Resources");

    expect(wrapper.text()).toContain("Cannot load this record");
    expect(wrapper.findAll("button")).toEqual([]);
    expect(consoleError).toHaveBeenCalled();
  });
});
