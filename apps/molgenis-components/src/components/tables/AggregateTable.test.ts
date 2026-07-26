import { flushPromises, mount } from "@vue/test-utils";
import { describe, expect, test, vi } from "vitest";
import AggregateTable from "./AggregateTable.vue";

// stub the data-loading client so the group-by picker can be inspected without a schema behind it
vi.mock("../../client/client", async (importActual) => {
  const actual = await importActual<typeof import("../../client/client")>();
  return {
    ...actual,
    default: {
      newClient: () => ({
        getPrimaryKeyFields: vi.fn().mockResolvedValue(["name"]),
        fetchAggregateData: vi.fn().mockResolvedValue({}),
      }),
    },
  };
});

function mountAggregateTable(refbackFlavor: string) {
  return mount(AggregateTable, {
    props: {
      canView: true,
      schemaId: "pet store",
      tableId: "Shelter",
      allColumns: [
        { id: "name", columnType: "STRING" },
        {
          id: "kennels",
          columnType: refbackFlavor,
          refTableId: "Kennel",
          refBackId: "shelter",
        },
      ],
    },
  });
}

describe("AggregateTable group-by picker", () => {
  test.each(["REFBACK", "PARTS"])(
    "it should offer a %s column to group by",
    async (refbackFlavor: string) => {
      const wrapper = mountAggregateTable(refbackFlavor);
      await flushPromises();

      const options = wrapper
        .findAll("#aggregate-column-select option")
        .map((option) => option.text());
      expect(options).toContain("kennels");
    }
  );

  test.each(["REF", "SELECT", "RADIO", "REF_ARRAY", "CHECKBOX", "MULTISELECT"])(
    "it should offer a %s column to group by, since every ref widget aggregates alike",
    async (refWidget: string) => {
      const wrapper = mountAggregateTable(refWidget);
      await flushPromises();

      const options = wrapper
        .findAll("#aggregate-column-select option")
        .map((option) => option.text());
      expect(options).toContain("kennels");
    }
  );
});
