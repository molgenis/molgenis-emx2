import { enableAutoUnmount, flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it } from "vitest";
import type {
  IColumn,
  ISchemaMetaData,
} from "../../../../../metadata-utils/src/types";
import DisplayRecord from "../../../../app/components/display/Record.vue";

enableAutoUnmount(afterEach);

const visitColumns: IColumn[] = [
  { id: "pet", label: "Pet", columnType: "REF", refTableId: "Pet", key: 1 },
  { id: "date", label: "Date", columnType: "DATE", key: 1 },
  { id: "ward", label: "Ward", columnType: "STRING", key: 1, role: "TITLE" },
];

const petColumns: IColumn[] = [
  { id: "name", label: "Name", columnType: "STRING", key: 1 },
  {
    id: "visits",
    label: "Visits",
    columnType: "REFBACK",
    refTableId: "Visit",
    refSchemaId: "demo",
    refBackId: "pet",
    display: "LIST",
  },
];

const schema: ISchemaMetaData = {
  id: "demo",
  label: "demo",
  tables: [
    {
      id: "Pet",
      label: "Pet",
      tableType: "DATA",
      schemaId: "demo",
      columns: petColumns,
    },
    {
      id: "Visit",
      label: "Visit",
      tableType: "DATA",
      schemaId: "demo",
      columns: visitColumns,
    },
  ],
};

describe("a row in a nested list", () => {
  it("links to the parent path, the column's route and the composite key, minus the column the parent fills", async () => {
    const wrapper = mount(DisplayRecord, {
      props: {
        columns: petColumns,
        row: {
          name: "rex",
          visits: [{ pet: { name: "rex" }, date: "2020-01-02", ward: "A,1" }],
          visits_agg: { count: 1 },
        },
        schema,
        recordPath: "/display/record/demo/Pet/rex",
        showTableOfContents: false,
        config: {
          columns: {
            visits: { route: "visits" },
          },
        },
      },
      global: {
        stubs: {
          NuxtLink: { props: ["to"], template: '<a :href="to"><slot /></a>' },
        },
      },
    });
    await flushPromises();

    expect(wrapper.find("a").attributes("href")).toEqual(
      "/display/record/demo/Pet/rex/visits/2020-01-02,A!c1"
    );
  });
});
