import { mount, flushPromises } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import { defineComponent, h, Suspense } from "vue";
import TableCellDetailRef from "../../../../../app/components/table/cellDetail/TableCellDetailRef.vue";

vi.mock("../../../../../app/composables/fetchRowPrimaryKey", () => {
  return {
    default: vi.fn().mockResolvedValue("bird-1"),
  };
});

vi.mock("../../../../../app/composables/fetchRowData", () => {
  return {
    default: vi.fn().mockResolvedValue({
      name: "Tweety",
      age: 3,
      mg_owner: "admin",
    }),
  };
});

vi.mock("../../../../../app/composables/fetchTableMetadata", () => {
  return {
    default: vi
      .fn()
      .mockImplementation(async (_schema: string, tableId: string) => {
        if (tableId === "birds") {
          return {
            id: "birds",
            columns: [
              {
                id: "details_heading",
                label: "Details",
                columnType: "HEADING",
              },
              {
                id: "name",
                label: "Name",
                columnType: "STRING",
              },
              {
                id: "age",
                label: "Age",
                columnType: "INT",
              },
              {
                id: "mg_owner",
                label: "Data owner",
                columnType: "STRING",
              },
            ],
          };
        }

        return {
          id: tableId,
          columns: [],
        };
      }),
  };
});

vi.mock("../../../../../app/components/value/EMX2.vue", () => {
  return {
    default: defineComponent({
      name: "ValueEMX2Stub",
      props: {
        data: { type: null, required: false },
        metadata: { type: Object, required: true },
      },
      emits: ["valueClick"],
      template:
        "<button class=\"value-emx2\" @click=\"$emit('valueClick', { type: 'ref', data, metadata })\">{{ metadata.label }}: {{ data }}</button>",
    }),
  };
});

function mountWithSuspense(props: Record<string, unknown>) {
  return mount(
    defineComponent({
      render() {
        return h(Suspense, null, {
          // @ts-ignore
          default: h(TableCellDetailRef, props),
        });
      },
    })
  );
}

describe("TableCellDetailRef.vue", () => {
  it("renders the referenced row details and hides mg_ fields by default", async () => {
    const wrapper = mountWithSuspense({
      metadata: {
        id: "birdRef",
        label: "Bird",
        columnType: "REF",
        refTableId: "birds",
      },
      columnValue: { id: "bird-1" },
      schema: "petstore",
    });

    await flushPromises();

    expect(wrapper.text()).toContain("Details");
    expect(wrapper.text()).toContain("Name");
    expect(wrapper.text()).toContain("Tweety");
    expect(wrapper.text()).toContain("Age");
    expect(wrapper.text()).toContain("3");
    expect(wrapper.text()).not.toContain("Data owner");
    expect(wrapper.text()).not.toContain("admin");
  });

  it("shows data owner fields when enabled and forwards nested value clicks", async () => {
    const wrapper = mountWithSuspense({
      metadata: {
        id: "birdRef",
        label: "Bird",
        columnType: "REF",
        refTableId: "birds",
      },
      columnValue: { id: "bird-1" },
      schema: "petstore",
      showDataOwner: true,
    });

    await flushPromises();

    expect(wrapper.text()).toContain("Data owner");
    expect(wrapper.text()).toContain("admin");

    await wrapper.findAll(".value-emx2")[0]?.trigger("click");

    expect(
      wrapper.findComponent(TableCellDetailRef).emitted("onRefClick")
    ).toEqual([
      [
        {
          type: "ref",
          data: "Tweety",
          metadata: {
            id: "name",
            label: "Name",
            columnType: "STRING",
          },
        },
      ],
    ]);
  });
});
