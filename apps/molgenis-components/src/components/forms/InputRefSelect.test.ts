import axios from "axios";
import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, test, vi } from "vitest";
import InputRefSelect from "./InputRefSelect.vue";
import TableSearch from "../tables/TableSearch.vue";

// resolveTablePermission (the fix under test) runs for real; only the network is
// mocked. TableSearch is stubbed so opening the picker does not load real data.
vi.mock("axios");

function permissionResponse(canInsert: boolean) {
  return {
    data: {
      data: {
        _session: {
          tablePermissions: [{ id: "Pet", name: "Pet", canInsert }],
        },
      },
    },
  };
}

function mountRefSelect(props: Record<string, unknown>) {
  return mount(InputRefSelect, {
    props: { id: "pet", refLabel: "${name}", ...props },
    global: { stubs: { TableSearch: true } },
  });
}

async function openPicker(wrapper: ReturnType<typeof mountRefSelect>) {
  await wrapper.find("input").trigger("click");
  await flushPromises();
}

describe("InputRefSelect add button for cross-schema references", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("passes insert permission of the referenced schema to the picker", async () => {
    (axios.post as any).mockResolvedValue(permissionResponse(true));
    const wrapper = mountRefSelect({
      tableId: "Pet",
      schemaId: "ref-select-grant",
      tablePermissions: [{ id: "Order", name: "Order", canInsert: true }],
    });
    await flushPromises();
    await openPicker(wrapper);

    expect(axios.post).toHaveBeenCalledWith(
      "/ref-select-grant/graphql",
      expect.anything()
    );
    expect(wrapper.getComponent(TableSearch).props("canInsert")).toBe(true);
  });

  test("does not grant insert when the referenced schema denies it", async () => {
    (axios.post as any).mockResolvedValue(permissionResponse(false));
    const wrapper = mountRefSelect({
      tableId: "Pet",
      schemaId: "ref-select-deny",
      tablePermissions: [{ id: "Order", name: "Order", canInsert: true }],
    });
    await flushPromises();
    await openPicker(wrapper);

    expect(wrapper.getComponent(TableSearch).props("canInsert")).toBe(false);
  });

  test("uses the current-schema seed without a permission fetch", async () => {
    const wrapper = mountRefSelect({
      tableId: "Pet",
      schemaId: "pet store",
      tablePermissions: [{ id: "Pet", name: "Pet", canInsert: true }],
    });
    await flushPromises();
    await openPicker(wrapper);

    expect(axios.post).not.toHaveBeenCalled();
    expect(wrapper.getComponent(TableSearch).props("canInsert")).toBe(true);
  });
});
