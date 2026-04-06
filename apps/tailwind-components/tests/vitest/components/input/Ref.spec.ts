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

const ALL_BIRDS = [
  { id: "tweety", name: "tweety" },
  { id: "looney", name: "looney" },
  { id: "daffy", name: "daffy" },
  { id: "sylvester", name: "sylvester" },
  { id: "elmer", name: "elmer" },
  { id: "bugs", name: "bugs" },
];

vi.mock("../../../../app/composables/fetchTableData", () => {
  return {
    default: vi.fn(async (_schema, _table, props) => {
      const filter = props?.filter ?? {};
      const nameFilter = (filter as any)?.name?.equals;
      if (nameFilter) {
        const filtered = ALL_BIRDS.filter((b) => nameFilter.includes(b.name));
        return { rows: filtered, count: filtered.length };
      }
      return { rows: ALL_BIRDS, count: ALL_BIRDS.length };
    }),
  };
});

vi.mock("../../../../app/composables/fetchRowPrimaryKey", () => {
  return {
    default: async (row: any) => {
      return { name: row.name };
    },
  };
});

vi.mock("../../../../app/composables/fetchGraphql", () => ({
  default: vi.fn(),
}));

import fetchGraphql from "../../../../app/composables/fetchGraphql";
import fetchTableData from "../../../../app/composables/fetchTableData";

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

describe("REF URL-hydrated modelValue (partial key object)", () => {
  it("selects only the matching option when modelValue is a partial ref object from URL parsing", async () => {
    const wrapper = mount(InputRef, {
      props: {
        id: "test-ref-url-hydrated",
        refTableId: "test-table",
        refSchemaId: "test-schema",
        refLabel: "${name}",
        isArray: true,
        limit: 20,
        modelValue: [{ name: "tweety" }],
      },
    });

    await flushPromises();

    const selectionMap = (wrapper.vm as any).selectionMap;
    expect(Object.keys(selectionMap)).toHaveLength(1);
    expect(Object.keys(selectionMap)).toContain("tweety");
  });

  it("selects only matching option for single-select partial ref object from URL parsing", async () => {
    const wrapper = mount(InputRef, {
      props: {
        id: "test-ref-url-hydrated-single",
        refTableId: "test-table",
        refSchemaId: "test-schema",
        refLabel: "${name}",
        isArray: false,
        limit: 20,
        modelValue: [{ name: "tweety" }],
      },
    });

    await flushPromises();

    const selectionMap = (wrapper.vm as any).selectionMap;
    expect(Object.keys(selectionMap)).toHaveLength(1);
    expect(Object.keys(selectionMap)).toContain("tweety");
  });

  it("renders the checkbox as checked for URL-hydrated partial ref object", async () => {
    const wrapper = mount(InputRef, {
      props: {
        id: "test-ref-checkbox-checked",
        refTableId: "test-table",
        refSchemaId: "test-schema",
        refLabel: "${name}",
        isArray: true,
        limit: 20,
        modelValue: [{ name: "tweety" }],
      },
    });

    await flushPromises();
    await nextTick();

    const checkbox = wrapper.find('input[type="checkbox"][value="tweety"]');
    expect(checkbox.exists()).toBe(true);
    expect((checkbox.element as HTMLInputElement).checked).toBe(true);
  });

  it("emits full object shape when user selects an additional option after URL-hydration", async () => {
    const wrapper = mount(InputRef, {
      props: {
        id: "test-ref-add-selection",
        refTableId: "test-table",
        refSchemaId: "test-schema",
        refLabel: "${name}",
        isArray: true,
        limit: 20,
        modelValue: [{ name: "tweety" }],
      },
    });

    await flushPromises();
    await nextTick();

    await wrapper.vm.select("daffy");

    const emitted = wrapper.emitted("update:modelValue") as any[][];
    expect(emitted).toBeTruthy();
    const lastEmit = emitted[emitted.length - 1][0];
    expect(Array.isArray(lastEmit)).toBe(true);
    expect(lastEmit).toContainEqual(
      expect.objectContaining({ name: "tweety" })
    );
    expect(lastEmit).toContainEqual(expect.objectContaining({ name: "daffy" }));
    expect(lastEmit.every((v: unknown) => typeof v === "object")).toBe(true);
  });

  it("emits empty array when user removes the URL-hydrated selection", async () => {
    const wrapper = mount(InputRef, {
      props: {
        id: "test-ref-remove-selection",
        refTableId: "test-table",
        refSchemaId: "test-schema",
        refLabel: "${name}",
        isArray: true,
        limit: 20,
        modelValue: [{ name: "tweety" }],
      },
    });

    await flushPromises();
    await nextTick();

    wrapper.vm.deselect("tweety");

    const emitted = wrapper.emitted("update:modelValue") as any[][];
    expect(emitted).toBeTruthy();
    const lastEmit = emitted[emitted.length - 1][0];
    expect(lastEmit).toEqual([]);
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

  it("shows all options when no countFetcher provided", async () => {
    const wrapper = mount(InputRef, {
      props: {
        id: "test-ref-no-fetcher",
        refTableId: "bird",
        refSchemaId: "test-schema",
        refLabel: "${name}",
        isArray: true,
        limit: 20,
      },
    });

    await flushPromises();

    const html = wrapper.html();
    expect(html).toContain("tweety");
    expect(html).toContain("looney");
    expect(html).toContain("daffy");
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

describe("REF URL round-trip: modelValue change does not flash empty state", () => {
  let mockFetchTableData: ReturnType<typeof vi.mocked<typeof fetchTableData>>;

  beforeEach(() => {
    vi.clearAllMocks();
    mockFetchTableData = vi.mocked(fetchTableData);
  });

  it("checkbox stays checked and selection well is never empty during URL round-trip", async () => {
    const wrapper = mount(InputRef, {
      props: {
        id: "test-ref-roundtrip",
        refTableId: "test-table",
        refSchemaId: "test-schema",
        refLabel: "${name}",
        isArray: true,
        limit: 20,
        modelValue: [{ name: "tweety" }],
      },
    });

    await flushPromises();

    const selectionMapBefore = { ...(wrapper.vm as any).selectionMap };
    expect(Object.keys(selectionMapBefore)).toContain("tweety");

    const emptyStates: boolean[] = [];
    const stopWatch = (wrapper.vm as any).$watch ? null : null;

    await wrapper.setProps({ modelValue: [{ name: "tweety" }] });
    await nextTick();

    const selectionMapAfter = (wrapper.vm as any).selectionMap;
    expect(Object.keys(selectionMapAfter)).toContain("tweety");
    expect(Object.keys(selectionMapAfter)).toHaveLength(1);
  });

  it("selectionMap is never transiently empty when modelValue round-trips the same value", async () => {
    const selectionSnapshots: number[] = [];

    const wrapper = mount(InputRef, {
      props: {
        id: "test-ref-no-empty-flash",
        refTableId: "test-table",
        refSchemaId: "test-schema",
        refLabel: "${name}",
        isArray: true,
        limit: 20,
        modelValue: [{ name: "tweety" }],
      },
    });

    await flushPromises();

    const vm = wrapper.vm as any;
    expect(Object.keys(vm.selectionMap)).toHaveLength(1);

    const fetchCallsBefore = mockFetchTableData.mock.calls.length;

    await wrapper.setProps({ modelValue: [{ name: "tweety" }] });
    await nextTick();

    expect(Object.keys(vm.selectionMap)).toHaveLength(1);
    expect(Object.keys(vm.selectionMap)).toContain("tweety");
    expect(mockFetchTableData.mock.calls.length).toBe(fetchCallsBefore);
  });

  it("concurrent modelValue updates are not dropped — last value wins", async () => {
    const wrapper = mount(InputRef, {
      props: {
        id: "test-ref-concurrent",
        refTableId: "test-table",
        refSchemaId: "test-schema",
        refLabel: "${name}",
        isArray: true,
        limit: 20,
        modelValue: [{ name: "tweety" }],
      },
    });

    await flushPromises();

    await wrapper.setProps({ modelValue: [{ name: "daffy" }] });
    await wrapper.setProps({ modelValue: [{ name: "looney" }] });
    await flushPromises();

    const vm = wrapper.vm as any;
    expect(Object.keys(vm.selectionMap)).toContain("looney");
    expect(Object.keys(vm.selectionMap)).not.toContain("tweety");
  });
});
