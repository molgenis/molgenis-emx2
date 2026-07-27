import { flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import DetailView from "../../../../app/components/display/DetailView.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

function birdsColumns(): IColumn[] {
  return [
    {
      id: "overview",
      label: "Overview",
      columnType: "SECTION",
      section: "overview",
    },
    { id: "name", label: "Name", columnType: "STRING", section: "overview" },
    {
      id: "details",
      label: "Details",
      columnType: "HEADING",
      section: "overview",
    },
    { id: "age", label: "Age", columnType: "INT", heading: "details" },
    { id: "sightings", label: "Sightings", columnType: "HEADING" },
    {
      id: "region",
      label: "Region",
      columnType: "STRING",
      heading: "sightings",
    },
    { id: "ring", label: "Ring", columnType: "STRING" },
  ];
}

const birdData = {
  name: "Tweety",
  age: 3,
  region: "Groningen",
  ring: "NL-1234",
};

function mountDetailView() {
  return mount(DetailView, {
    props: { columns: birdsColumns(), data: birdData, showSideNav: false },
  });
}

describe("display/DetailView.vue section grouping", () => {
  it("groups the columns under their own SECTION and HEADING, with the columns belonging to neither first", () => {
    const wrapper = mountDetailView();

    const groups = wrapper.findAll("section").map((section) => ({
      heading: section.find("h2").exists() ? section.find("h2").text() : null,
      terms: section.findAll("dt").map((term) => term.text()),
    }));

    expect(groups).toEqual([
      { heading: null, terms: ["Ring"] },
      { heading: "Overview", terms: ["Name"] },
      { heading: "Details", terms: ["Age"] },
      { heading: "Sightings", terms: ["Region"] },
    ]);
  });
});

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
            id: "numberOfParticipants",
            label: "Number of participants",
            columnType: "INT",
          },
        ],
      },
    ],
  };
}

function stubCatalogueGraphql(schemaId: string) {
  const storedCollection: Record<string, unknown> = {
    id: "c1",
    name: "Cohort One",
    mg_tableclass: `${schemaId}.Collections`,
    numberOfParticipants: 42,
  };
  const schema = catalogueSchema(schemaId);

  vi.stubGlobal(
    "$fetch",
    vi.fn(async (_url: string, options: { body: { query: string } }) => {
      const query = options.body.query;
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
}

async function mountRecordOf(schemaId: string, tableId: string) {
  stubCatalogueGraphql(schemaId);
  const wrapper = mount(DetailView, {
    props: { schemaId, tableId, rowId: { id: "c1" }, showSideNav: false },
  });
  await flushPromises();
  await flushPromises();
  return wrapper;
}

describe("display/DetailView.vue subclass columns", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    sessionStorage.clear();
  });

  it("shows the columns of the row's own subclass when the URL names the declaring superclass", async () => {
    const wrapper = await mountRecordOf("detailViewSubclassRoot", "Resources");

    const definitions = Object.fromEntries(
      wrapper
        .findAll("dt")
        .map((term, index) => [
          term.text(),
          wrapper.findAll("dd")[index]!.text(),
        ])
    );

    expect(definitions).toEqual({
      Id: "c1",
      Name: "Cohort One",
      "Number of participants": "42",
    });
  });

  it("labels the record with its own subclass table, not the declaring superclass", async () => {
    const wrapper = await mountRecordOf("detailViewSubclassLabel", "Resources");

    expect(wrapper.text()).toContain("Collections");
  });

  it("keeps showing the table's own columns when the row is not of a subclass", async () => {
    const wrapper = await mountRecordOf("detailViewSubclassOwn", "Collections");

    expect(wrapper.findAll("dt").map((term) => term.text())).toEqual([
      "Id",
      "Name",
      "Number of participants",
    ]);
  });
});

describe("display/DetailView.vue title override", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    sessionStorage.clear();
  });

  it("headlines the record with the title its caller supplies instead of the whole primary key", async () => {
    stubCatalogueGraphql("detailViewTitleOverride");
    const wrapper = mount(DetailView, {
      props: {
        schemaId: "detailViewTitleOverride",
        tableId: "Collections",
        rowId: { id: "c1" },
        showSideNav: false,
        title: "Cohort One",
      },
    });
    await flushPromises();
    await flushPromises();

    expect(wrapper.find("h1").text()).toBe("Cohort One");
    expect(document.title).toBe("Cohort One");
  });

  it("keeps deriving the headline from the record when the caller supplies no title", async () => {
    const wrapper = await mountRecordOf(
      "detailViewTitleDerived",
      "Collections"
    );

    expect(wrapper.find("h1").text()).toBe("c1");
  });
});
