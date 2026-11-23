import { mockNuxtImport } from "@nuxt/test-utils/runtime";
import { describe, it, expect, vi } from "vitest";
import { reactive, ref } from "vue";
import { mount, flushPromises } from "@vue/test-utils";
import type { IColumn } from "../../../../metadata-utils/src/types";

vi.mock("../../../app/composables/fetchGraphql", () => ({
  default: vi.fn(),
}));
vi.mock("../../../app/composables/fetchTableData", () => ({
  getColumnIds: vi.fn().mockResolvedValue(" name"),
}));
vi.mock("../../../app/composables/fetchTableMetadata", () => ({
  default: vi.fn().mockResolvedValue({ columns: [] }),
}));

const mockSetup = vi.hoisted(() => ({
  query: {} as Record<string, string>,
}));

mockNuxtImport("useRoute", () => () => ({
  query: reactive(mockSetup.query),
}));
mockNuxtImport("useRouter", () => () => ({
  replace: vi.fn(),
  beforeEach: vi.fn(),
  beforeResolve: vi.fn(),
  afterEach: vi.fn(),
  options: {},
}));

import fetchGraphql from "../../../app/composables/fetchGraphql";
import { useFilters } from "../../../app/composables/useFilters";
import SidebarContent from "../../../app/components/filter/SidebarContent.vue";

global.IntersectionObserver = vi.fn().mockImplementation(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
  takeRecords: vi.fn(() => []),
}));

describe("sidebar BOOL and BOOL_ARRAY filter when a count request is aborted by a refresh", () => {
  it.each(["BOOL", "BOOL_ARRAY"] as const)(
    "%s still shows Yes (1) and No (1), not an error, after the first request is aborted",
    async (columnType) => {
      mockSetup.query = { mg_filters: "pet,complete" };
      vi.mocked(fetchGraphql).mockImplementation(
        (_s, query, _v, options) =>
          new Promise((resolve, reject) => {
            const timer = setTimeout(
              () =>
                resolve(
                  query.includes("yes:")
                    ? {
                        yes: { count: 1 },
                        no: { count: 1 },
                        empty: { count: 0 },
                      }
                    : { Order_groupBy: [{ count: 2, pet: { name: "spike" } }] }
                ),
              20
            );
            options?.signal?.addEventListener("abort", () => {
              clearTimeout(timer);
              reject(new Error("Could not fetch graphql: aborted"));
            });
          })
      );
      const columns = ref<IColumn[]>([]);
      const filters = useFilters(columns, {
        schemaId: "pet store",
        tableId: "Order",
        urlSync: true,
      });
      const wrapper = mount(SidebarContent, {
        props: {
          filters: filters as any,
          columns: [],
          schemaId: "pet store",
          tableId: "Order",
        },
      });
      await flushPromises();
      columns.value = [
        {
          id: "pet",
          label: "Pet",
          columnType: "REF",
          refTableId: "Pet",
        } as IColumn,
        { id: "complete", label: "Complete", columnType } as IColumn,
      ];
      await new Promise((resolve) => setTimeout(resolve, 5));
      columns.value = [...columns.value];
      await new Promise((resolve) => setTimeout(resolve, 200));
      await flushPromises();
      const labels = wrapper
        .findAll("label")
        .map((label) => label.text().replace(/\s+/g, " "));
      expect(labels).toEqual(["spike (2)", "Yes (1)", "No (1)"]);
    }
  );
});
