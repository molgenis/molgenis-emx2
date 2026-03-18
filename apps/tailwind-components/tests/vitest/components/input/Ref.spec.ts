import { mount, flushPromises } from "@vue/test-utils";
import { nextTick } from "vue";
import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import InputRef from "../../../../app/components/input/Ref.vue";

vi.mock("../../../../app/composables/fetchTableMetadata", () => {
  return {
    default: async () => {
      return {
        id: "MockId",
        label: "Mock label",
      };
    },
  };
});

vi.mock("../../../../app/composables/fetchTableData", () => {
  return {
    default: async () => {
      return {
        rows: [
          { id: "tweety", name: "tweety" },
          { id: "looney", name: "looney" },
          { id: "daffy", name: "daffy" },
          { id: "sylvester", name: "sylvester" },
          { id: "elmer", name: "elmer" },
          { id: "bugs", name: "bugs" },
        ],
        count: 6,
      };
    },
  };
});

vi.mock("../../../../app/composables/fetchRowPrimaryKey", () => {
  return {
    default: async (row: any) => {
      return row.id;
    },
  };
});

vi.mock("../../../../app/composables/fetchGraphql", () => ({
  default: vi.fn(),
}));

import fetchGraphql from "../../../../app/composables/fetchGraphql";

const wrapper = mount(InputRef, {
  props: {
    id: "test-ref",
    refTableId: "test-table",
    refSchemaId: "test-schema",
    refLabel: "${name}",
    isArray: false,
    limit: 5,
    modelValue: { id: "tweety", name: "tweety" },
  },
});

describe("input ref", () => {
  it("deselect on non-array version should emit null", () => {
    expect(wrapper.exists()).toBe(true);
    wrapper.find("button").trigger("click");
    expect(wrapper.emitted("update:modelValue")).toEqual([[null]]);
  });
});

describe("facet count fetching", () => {
  let mockFetchGraphql: ReturnType<typeof vi.mocked<typeof fetchGraphql>>;

  beforeEach(() => {
    vi.useFakeTimers();
    vi.clearAllMocks();
    mockFetchGraphql = vi.mocked(fetchGraphql);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("fetches counts via _groupBy after loading options", async () => {
    mockFetchGraphql.mockResolvedValue({
      Pet_groupBy: [
        { count: 3, bird: { name: "tweety" } },
        { count: 5, bird: { name: "daffy" } },
      ],
    });

    const { createCountFetcher } = await import(
      "../../../../app/utils/createCountFetcher"
    );
    const countFetcher = createCountFetcher({
      schemaId: "test-schema",
      tableId: "Pet",
      columnPath: "bird",
      getCrossFilter: () => ({ age: { between: [1, 10] } }),
    });

    const wrapper = mount(InputRef, {
      props: {
        id: "test-ref-counts",
        refTableId: "bird",
        refSchemaId: "test-schema",
        refLabel: "${name}",
        isArray: true,
        limit: 20,
        countFetcher,
      },
    });

    await flushPromises();

    expect(mockFetchGraphql).toHaveBeenCalledWith(
      "test-schema",
      expect.stringContaining("Pet_groupBy"),
      expect.objectContaining({
        filter: expect.objectContaining({
          age: { between: [1, 10] },
        }),
      })
    );
  });

  it("re-fetches counts when crossFilter changes", async () => {
    mockFetchGraphql.mockResolvedValue({
      Pet_groupBy: [],
    });

    const { ref } = await import("vue");
    const { createCountFetcher } = await import(
      "../../../../app/utils/createCountFetcher"
    );
    const crossFilterRef = ref<any>({ age: { between: [1, 10] } });
    const countFetcher = createCountFetcher({
      schemaId: "test-schema",
      tableId: "Pet",
      columnPath: "bird",
      getCrossFilter: () => crossFilterRef.value,
    });

    const wrapper = mount(InputRef, {
      props: {
        id: "test-ref-recount",
        refTableId: "bird",
        refSchemaId: "test-schema",
        refLabel: "${name}",
        isArray: true,
        limit: 20,
        countFetcher,
      },
    });

    await flushPromises();
    const callsBefore = mockFetchGraphql.mock.calls.length;

    crossFilterRef.value = { age: { between: [5, 20] } };
    await nextTick();
    vi.advanceTimersByTime(300);
    await flushPromises();

    expect(mockFetchGraphql.mock.calls.length).toBeGreaterThan(callsBefore);
    const lastCall =
      mockFetchGraphql.mock.calls[mockFetchGraphql.mock.calls.length - 1];
    expect(lastCall[2]).toEqual(
      expect.objectContaining({
        filter: expect.objectContaining({
          age: { between: [5, 20] },
        }),
      })
    );
  });
});
