import { mount, flushPromises } from "@vue/test-utils";
import { mockNuxtImport } from "@nuxt/test-utils/runtime";
import { shallowRef } from "vue";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { ITableMetaData } from "../../../../../metadata-utils/src";

const metadata: ITableMetaData = {
  id: "Pet",
  schemaId: "petStore",
  name: "Pet",
  label: "Pet",
  tableType: "DATA",
  columns: [{ id: "name", label: "Name", columnType: "STRING", key: 1 }],
};

function makeRow(name: string) {
  return {
    name,
    _rowId: { name },
    _rowIdString: JSON.stringify({ name }),
  };
}

// the table's own data + refresh, standing in for the real
// fetchTableMetadata/fetchTableData round trip behind useAsyncData
const refresh = vi.fn(async () => {
  data.value = {
    tableMetadata: metadata,
    rows: [makeRow("Tweety (edited)")],
    count: 1,
  };
});

const data = shallowRef<{
  tableMetadata: unknown;
  rows: unknown[];
  count: number;
}>({
  tableMetadata: metadata,
  rows: [makeRow("Tweety")],
  count: 1,
});

mockNuxtImport("useAsyncData", () => {
  return vi.fn(() => ({
    data,
    refresh,
    status: shallowRef("success"),
  }));
});

import TableEMX2 from "../../../../app/components/table/TableEMX2.vue";

describe("TableEMX2.vue", () => {
  beforeEach(() => {
    refresh.mockClear();
    data.value = {
      tableMetadata: metadata,
      rows: [makeRow("Tweety")],
      count: 1,
    };
  });

  function mountTable() {
    return mount(TableEMX2, {
      props: {
        schemaId: "petStore",
        tableId: "Pet",
        canUpdate: true,
        enableFilters: false,
        hideSearch: true,
      },
      global: {
        stubs: {
          teleport: true,
          // isolate the wiring under test from EditModal's own save
          // mechanics, which EditModal.test.ts already covers
          EditModal: {
            props: ["visible"],
            emits: ["update:updated", "update:cancelled"],
            template: `<div v-if="visible" data-testid="edit-modal-stub">
              <button aria-label="fake save" @click="$emit('update:updated', {})">save</button>
            </div>`,
          },
        },
      },
    });
  }

  it("refetches the table after an edit is saved, so the row shown is not stale", async () => {
    const wrapper = mountTable();
    await flushPromises();

    expect(wrapper.text()).toContain("Tweety");
    expect(wrapper.text()).not.toContain("Tweety (edited)");

    await wrapper.find('[id^="edit-button-"]').trigger("click");
    await flushPromises();

    await wrapper
      .find('[data-testid="edit-modal-stub"] button')
      .trigger("click");
    await flushPromises();

    expect(refresh).toHaveBeenCalledTimes(1);
    expect(wrapper.text()).toContain("Tweety (edited)");
  });
});
