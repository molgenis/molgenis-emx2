import { mount, flushPromises } from "@vue/test-utils";
import { describe, expect, it, vi, beforeEach } from "vitest";
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
  it("deselect on non-array version should yield empty array ", () => {
    expect(wrapper.exists()).toBe(true);
    wrapper.find("button").trigger("click");
    expect(wrapper.emitted("update:modelValue")).toEqual([[null]]);
    setTimeout(() => {
      //timeout because of debounce
      expect(wrapper.emitted("blur")).toBeDefined();
    }, 100);
  });
});

describe("facet count fetching", () => {
  let mockFetchGraphql: ReturnType<typeof vi.mocked<typeof fetchGraphql>>;

  beforeEach(() => {
    vi.clearAllMocks();
    mockFetchGraphql = vi.mocked(fetchGraphql);
  });

  it("fetches counts via _groupBy after loading options", async () => {
    mockFetchGraphql.mockResolvedValue({
      Pet_groupBy: [
        { count: 3, bird: { name: "tweety" } },
        { count: 5, bird: { name: "daffy" } },
      ],
    });

    const wrapper = mount(InputRef, {
      props: {
        id: "test-ref-counts",
        refTableId: "bird",
        refSchemaId: "test-schema",
        refLabel: "${name}",
        isArray: true,
        limit: 20,
        crossFilter: { age: { between: [1, 10] } },
        schemaId: "test-schema",
        tableId: "Pet",
        columnPath: "bird",
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
    vi.useFakeTimers();
    mockFetchGraphql.mockResolvedValue({
      Pet_groupBy: [],
    });

    const wrapper = mount(InputRef, {
      props: {
        id: "test-ref-recount",
        refTableId: "bird",
        refSchemaId: "test-schema",
        refLabel: "${name}",
        isArray: true,
        limit: 20,
        crossFilter: { age: { between: [1, 10] } },
        schemaId: "test-schema",
        tableId: "Pet",
        columnPath: "bird",
      },
    });

    await flushPromises();
    const callsBefore = mockFetchGraphql.mock.calls.length;

    await wrapper.setProps({ crossFilter: { age: { between: [5, 20] } } });
    vi.advanceTimersByTime(300);
    await flushPromises();

    vi.useRealTimers();
    expect(mockFetchGraphql.mock.calls.length).toBeGreaterThan(callsBefore);
  });
});
