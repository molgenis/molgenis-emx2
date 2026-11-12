import axios from "axios";
import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, test, vi } from "vitest";
import InputRefList from "./InputRefList.vue";
import RowButtonAdd from "../tables/RowButtonAdd.vue";

// Keep the real permission logic (resolveTablePermission / fetchTablePermissions,
// which is the fix under test) but stub the data-loading client so the mount does
// not need a real schema behind it.
vi.mock("../../client/client", async (importActual) => {
  const actual = await importActual<typeof import("../../client/client")>();
  return {
    ...actual,
    default: {
      newClient: () => ({
        fetchTableMetaData: vi
          .fn()
          .mockResolvedValue({ id: "Pet", label: "Pet", columns: [] }),
        fetchTableData: vi
          .fn()
          .mockResolvedValue({ Pet: [], Pet_agg: { count: 0 } }),
      }),
    },
  };
});

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

function mountRefList(props: Record<string, unknown>) {
  return mount(InputRefList, {
    props: { id: "pets", label: "Pet", refLabel: "${name}", ...props },
  });
}

describe("InputRefList add button for cross-schema references", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // distinct schema ids per test: the client caches permissions per schema
  test("shows the add button when the referenced (other) schema grants insert", async () => {
    (axios.post as any).mockResolvedValue(permissionResponse(true));
    const wrapper = mountRefList({
      tableId: "Pet",
      schemaId: "ref-grant",
      tablePermissions: [{ id: "Order", name: "Order", canInsert: true }],
    });
    await flushPromises();

    expect(axios.post).toHaveBeenCalledWith(
      "/ref-grant/graphql",
      expect.anything()
    );
    expect(wrapper.findComponent(RowButtonAdd).exists()).toBe(true);
    expect(wrapper.getComponent(RowButtonAdd).props("schemaId")).toBe(
      "ref-grant"
    );
  });

  test("hides the add button when the referenced schema denies insert", async () => {
    (axios.post as any).mockResolvedValue(permissionResponse(false));
    const wrapper = mountRefList({
      tableId: "Pet",
      schemaId: "ref-deny",
      tablePermissions: [{ id: "Order", name: "Order", canInsert: true }],
    });
    await flushPromises();

    expect(wrapper.findComponent(RowButtonAdd).exists()).toBe(false);
  });

  test("uses the current-schema seed without a permission fetch", async () => {
    const wrapper = mountRefList({
      tableId: "Pet",
      schemaId: "pet store",
      tablePermissions: [{ id: "Pet", name: "Pet", canInsert: true }],
    });
    await flushPromises();

    expect(axios.post).not.toHaveBeenCalled();
    expect(wrapper.findComponent(RowButtonAdd).exists()).toBe(true);
  });
});
