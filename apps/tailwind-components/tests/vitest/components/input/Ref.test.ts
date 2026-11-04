import { enableAutoUnmount, flushPromises, mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import InputRef from "../../../../app/components/input/Ref.vue";

const fetchTableData = vi.hoisted(() => vi.fn());

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
  return { default: fetchTableData };
});

vi.mock("../../../../app/utils/getPrimaryKey", () => {
  return {
    getPrimaryKey: async (row: { id: string }) => ({ id: row.id }),
  };
});

const ALL_ROWS = Array.from({ length: 45 }, (_, i) => ({
  id: `r${i + 1}`,
  name: `Row ${i + 1}`,
}));
const label = (position: number) => `Row ${position}`;
const labels = (from: number, to: number) =>
  Array.from({ length: to - from + 1 }, (_, i) => label(from + i));

let tableRows = ALL_ROWS;

beforeEach(() => {
  tableRows = ALL_ROWS;
  fetchTableData.mockReset();
  fetchTableData.mockImplementation(async (_schema, _table, query) => {
    const keys = query?.filter?.equals;
    if (keys) {
      const wanted = [keys].flat().map((key: { id: string }) => key.id);
      const rows = tableRows.filter((row) => wanted.includes(row.id));
      return { rows, count: rows.length };
    }
    const term = (query?.searchTerms ?? "").toLowerCase();
    const matching = tableRows.filter((row) =>
      row.name.toLowerCase().includes(term)
    );
    const offset = query?.offset ?? 0;
    const limit = query?.limit ?? 20;
    return {
      rows: matching.slice(offset, offset + limit),
      count: matching.length,
    };
  });
});

enableAutoUnmount(afterEach);

afterEach(() => {
  vi.unstubAllGlobals();
  document.body.innerHTML = "";
});

async function mountRef(props: Record<string, unknown> = {}) {
  const wrapper = mount(InputRef, {
    attachTo: document.body,
    props: {
      id: "r",
      refSchemaId: "test-schema",
      refTableId: "test-table",
      refLabel: "${name}",
      isArray: false,
      limit: 20,
      ...props,
    },
  });
  await flushPromises();
  return wrapper;
}

type Wrapper = Awaited<ReturnType<typeof mountRef>>;

const toggle = (wrapper: Wrapper) => wrapper.find('button[role="combobox"]');
const optionTexts = (wrapper: Wrapper) =>
  wrapper.findAll('ul[role="listbox"] li[role="option"]').map((o) => o.text());
const chipTexts = (wrapper: Wrapper) =>
  wrapper.findAll('button[aria-label^="Remove "]').map((c) => c.text());
const pageQueries = () =>
  fetchTableData.mock.calls
    .map(([, , query]) => query)
    .filter((query) => !query?.filter);

function stubObserver() {
  let callback: IntersectionObserverCallback = () => {};
  class MockObserver {
    constructor(cb: IntersectionObserverCallback) {
      callback = cb;
    }
    observe = () => {};
    unobserve = () => {};
    disconnect = () => {};
  }
  vi.stubGlobal("IntersectionObserver", MockObserver);
  return async () => {
    callback(
      [{ isIntersecting: true } as IntersectionObserverEntry],
      {} as IntersectionObserver
    );
    await flushPromises();
  };
}

describe("Ref, select above the limit", () => {
  it.each([
    ["REF above limit", { isArray: false, limit: 20 }],
    ["REF_ARRAY above limit", { isArray: true, limit: 20 }],
    ["SELECT, limit 0", { isArray: false, limit: 0 }],
    ["MULTISELECT, limit 0", { isArray: true, limit: 0 }],
  ])("%s renders the listbox", async (_title, props) => {
    tableRows = props.limit === 0 ? ALL_ROWS.slice(0, 6) : ALL_ROWS;
    const wrapper = await mountRef(props);

    expect(toggle(wrapper).attributes("id")).toBe("r");
    expect(wrapper.find('input[type="radio"]').exists()).toBe(false);
    expect(wrapper.find('input[type="checkbox"]').exists()).toBe(false);
    expect(
      wrapper.find('ul[role="listbox"]').attributes("aria-multiselectable")
    ).toBe(props.isArray ? "true" : undefined);
  });

  it("renders radio and checkbox groups at or below the limit", async () => {
    tableRows = ALL_ROWS.slice(0, 6);
    const single = await mountRef({ isArray: false, limit: 20 });
    const multiple = await mountRef({ isArray: true, limit: 20 });

    expect(single.find("#r-radio-group").exists()).toBe(true);
    expect(single.find('button[role="combobox"]').exists()).toBe(false);
    expect(multiple.find("#r-checkbox-group").exists()).toBe(true);
    expect(multiple.find('button[role="combobox"]').exists()).toBe(false);
  });

  it("a limit above 20 still renders every option as a group", async () => {
    tableRows = ALL_ROWS.slice(0, 25);
    const wrapper = await mountRef({ isArray: true, limit: 30 });

    expect(wrapper.findAll('input[type="checkbox"]')).toHaveLength(25);
    expect(wrapper.find('button[role="combobox"]').exists()).toBe(false);
  });

  it("a limit above 20 with more rows than the limit pages the listbox by 20", async () => {
    const reachEnd = stubObserver();
    const wrapper = await mountRef({ limit: 30 });

    expect(optionTexts(wrapper)).toEqual(labels(1, 20));
    await reachEnd();
    expect(pageQueries().at(-1)).toMatchObject({ limit: 20, offset: 20 });
  });

  it("loads a first page of 20 rows", async () => {
    const wrapper = await mountRef({ limit: 5 });

    expect(pageQueries()).toEqual([
      expect.objectContaining({ limit: 20, offset: 0 }),
    ]);
    expect(optionTexts(wrapper)).toEqual(labels(1, 20));
  });

  it("each next page starts after the last row loaded, with no row skipped", async () => {
    const reachEnd = stubObserver();
    const wrapper = await mountRef({ limit: 5 });

    await reachEnd();
    expect(pageQueries().at(-1)).toMatchObject({ limit: 20, offset: 20 });
    expect(optionTexts(wrapper)).toEqual(labels(1, 40));

    await reachEnd();
    expect(pageQueries().at(-1)).toMatchObject({ limit: 20, offset: 40 });
    expect(optionTexts(wrapper)).toEqual(labels(1, 45));
  });

  it("fetches nothing once every row is loaded", async () => {
    const reachEnd = stubObserver();
    tableRows = ALL_ROWS.slice(0, 25);
    await mountRef({ limit: 5 });
    await reachEnd();
    const callsAfterLastPage = fetchTableData.mock.calls.length;

    await reachEnd();

    expect(fetchTableData.mock.calls).toHaveLength(callsAfterLastPage);
  });

  it("search refetches from offset 0 with the search terms", async () => {
    const wrapper = await mountRef({ limit: 5 });
    await toggle(wrapper).trigger("click");
    await wrapper.find('input[type="search"]').setValue("Row 4");
    await flushPromises();

    expect(pageQueries().at(-1)).toMatchObject({
      limit: 20,
      offset: 0,
      searchTerms: "Row 4",
    });
    expect(optionTexts(wrapper)).toEqual(["Row 4", ...labels(40, 45)]);
  });

  it("a response that arrives after a newer one is discarded", async () => {
    const wrapper = await mountRef({ limit: 5 });
    await toggle(wrapper).trigger("click");
    let resolveOlder: (value: unknown) => void = () => {};
    fetchTableData.mockImplementationOnce(
      () => new Promise((resolve) => (resolveOlder = resolve))
    );
    await wrapper.find('input[type="search"]').setValue("Row 4");
    await wrapper.find('input[type="search"]').setValue("Row 41");
    await flushPromises();

    resolveOlder({ rows: ALL_ROWS.slice(3, 4), count: 1 });
    await flushPromises();

    expect(optionTexts(wrapper)).toEqual(["Row 41"]);
  });

  it("load-more after a search keeps the search terms", async () => {
    const reachEnd = stubObserver();
    const wrapper = await mountRef({ limit: 5 });
    await toggle(wrapper).trigger("click");
    await wrapper.find('input[type="search"]').setValue("Row");
    await flushPromises();

    await reachEnd();

    expect(pageQueries().at(-1)).toMatchObject({
      offset: 20,
      searchTerms: "Row",
    });
    expect(optionTexts(wrapper)).toEqual(labels(1, 40));
  });

  it("emits the primary key of the picked row", async () => {
    const wrapper = await mountRef({ limit: 5 });
    await toggle(wrapper).trigger("click");
    await wrapper.findAll('li[role="option"]')[2]!.trigger("click");
    await flushPromises();

    expect(wrapper.emitted("update:modelValue")).toEqual([[{ id: "r3" }]]);
    expect(toggle(wrapper).text()).toBe("Row 3");
  });

  it("emits the primary keys of all picked rows, in pick order", async () => {
    const wrapper = await mountRef({ isArray: true, limit: 5 });
    await toggle(wrapper).trigger("click");
    await wrapper.findAll('li[role="option"]')[4]!.trigger("click");
    await flushPromises();
    await wrapper.findAll('li[role="option"]')[1]!.trigger("click");
    await flushPromises();

    expect(wrapper.emitted("update:modelValue")).toEqual([
      [[{ id: "r5" }]],
      [[{ id: "r5" }, { id: "r2" }]],
    ]);
    expect(chipTexts(wrapper)).toEqual(["Row 5", "Row 2"]);
  });

  it("shows a chip with its label for a selected row outside the loaded page", async () => {
    const wrapper = await mountRef({
      isArray: true,
      limit: 5,
      modelValue: [{ id: "r40" }],
    });

    expect(chipTexts(wrapper)).toEqual(["Row 40"]);
    expect(optionTexts(wrapper)).not.toContain("Row 40");
  });

  it("a cross removes the row and emits the remaining keys", async () => {
    const wrapper = await mountRef({
      isArray: true,
      limit: 5,
      modelValue: [{ id: "r2" }, { id: "r40" }],
    });
    await wrapper.find('button[aria-label="Remove Row 2"]').trigger("click");
    await flushPromises();

    expect(wrapper.emitted("update:modelValue")).toEqual([
      [[expect.objectContaining({ id: "r40" })]],
    ]);
  });

  it("Clear empties a single select to null and a multiple select to []", async () => {
    const single = await mountRef({ limit: 5, modelValue: { id: "r2" } });
    const multiple = await mountRef({
      id: "m",
      isArray: true,
      limit: 5,
      modelValue: [{ id: "r2" }],
    });
    await single.find("#r-listbox-clear").trigger("click");
    await multiple.find("#m-listbox-clear").trigger("click");
    await flushPromises();

    expect(single.emitted("update:modelValue")).toEqual([[null]]);
    expect(multiple.emitted("update:modelValue")).toEqual([[[]]]);
  });
});
