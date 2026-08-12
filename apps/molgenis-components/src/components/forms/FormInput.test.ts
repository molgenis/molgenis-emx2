import { flushPromises, mount } from "@vue/test-utils";
import { describe, expect, test, vi } from "vitest";
import FormInput from "./FormInput.vue";
import InputRefBack from "./InputRefBack.vue";

// stub the data-loading client so mounting the ref inputs does not need a real schema behind it
vi.mock("../../client/client", async (importActual) => {
  const actual = await importActual<typeof import("../../client/client")>();
  return {
    ...actual,
    default: {
      newClient: () => ({
        fetchTableMetaData: vi
          .fn()
          .mockResolvedValue({ id: "Kennel", label: "Kennel", columns: [] }),
        fetchTableDataValues: vi.fn().mockResolvedValue([]),
      }),
    },
  };
});

async function mountFormInput(columnType: string) {
  const wrapper = mount(FormInput, {
    props: {
      id: "kennels",
      label: "Kennels",
      columnType,
      schemaId: "pet store",
      tableId: "Kennel",
      refBackId: "shelter",
      refLabel: "${name}",
    },
  });
  await flushPromises();
  return wrapper;
}

describe("FormInput type dispatch", () => {
  test("it should render the refback input for a PARTS column, like it does for REFBACK", async () => {
    const refBackWrapper = await mountFormInput("REFBACK");
    const partsWrapper = await mountFormInput("PARTS");

    expect(refBackWrapper.findComponent(InputRefBack).exists()).toBe(true);
    expect(partsWrapper.findComponent(InputRefBack).exists()).toBe(true);
    expect(partsWrapper.text()).not.toContain("UNSUPPORTED TYPE");
  });
});
