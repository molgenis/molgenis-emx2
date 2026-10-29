import { mountSuspended } from "@nuxt/test-utils/runtime";
import { flushPromises } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { clearNuxtData } from "#app";
import type { ISchemaMetaData } from "../../../../../metadata-utils/src/types";
import RecordPage from "../../../../app/pages/display/record/[schemaId]/[tableId]/[recordId].vue";
import { recordConfigs } from "../../../../app/pages/display/record/-recordConfigs";
import { deriveRecordQuery } from "../../../../app/utils/deriveRecordQuery";

const fetchGraphqlMock = vi.fn();
const fetchMetadataMock = vi.fn();

vi.mock("../../../../app/composables/fetchGraphql", () => ({
  default: (...args: unknown[]) => fetchGraphqlMock(...args),
}));
vi.mock("../../../../app/composables/fetchMetadata", () => ({
  default: (...args: unknown[]) => fetchMetadataMock(...args),
}));
vi.mock("../../../../app/composables/fetchTableData", () => ({
  default: () => Promise.resolve({ rows: [], count: 0 }),
}));
vi.mock("../../../../app/composables/fetchTableMetadata", () => ({
  default: () => Promise.resolve({ id: "Subpopulations", columns: [] }),
}));

const schema: ISchemaMetaData = {
  id: "demo",
  label: "demo",
  tables: [
    {
      id: "Collections",
      name: "Collections",
      label: "Collections",
      schemaId: "demo",
      tableType: "DATA",
      columns: [
        { id: "id", label: "id", columnType: "STRING", key: 1 },
        { id: "name", label: "Name", columnType: "STRING" },
        {
          id: "subpopulations",
          label: "Subpopulations",
          columnType: "REFBACK",
          refTableId: "Subpopulations",
          refSchemaId: "demo",
          refBackId: "resource",
        },
      ],
    },
    {
      id: "Subpopulations",
      name: "Subpopulations",
      label: "Subpopulations",
      schemaId: "demo",
      tableType: "DATA",
      columns: [{ id: "name", label: "Name", columnType: "STRING", key: 1 }],
    },
  ],
};

describe("record page", () => {
  beforeEach(() => {
    clearNuxtData();
    fetchGraphqlMock.mockReset();
    fetchMetadataMock.mockReset();
    fetchMetadataMock.mockResolvedValue(schema);
    fetchGraphqlMock.mockResolvedValue({
      Collections: [{ id: "c1", name: "Cohort one" }],
    });
  });

  it("asks the derived query for the record with the route's key, and renders the record", async () => {
    const wrapper = await mountSuspended(RecordPage, {
      route: "/display/record/demo/Collections/c1",
    });
    await flushPromises();

    expect(fetchGraphqlMock).toHaveBeenCalledWith(
      "demo",
      deriveRecordQuery(schema, "Collections", recordConfigs["Collections"]),
      { filter: { id: { equals: "c1" } } }
    );
    expect(wrapper.text()).toContain("Cohort one");
  });
});
