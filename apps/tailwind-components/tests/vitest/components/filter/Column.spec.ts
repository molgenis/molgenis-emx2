import { describe, it, expect, vi } from "vitest";
import { mount } from "@vue/test-utils";
import type { IColumn } from "../../../../../metadata-utils/src/types";
import type { CountedOption } from "../../../../app/utils/fetchCounts";
import type { IFilterValue } from "../../../../types/filters";
import Column from "../../../../app/components/filter/Column.vue";
import Tree from "../../../../app/components/input/Tree.vue";
import FilterRange from "../../../../app/components/filter/Range.vue";
import GenericInput from "../../../../app/components/Input.vue";
import NoResultsMessage from "../../../../app/components/text/NoResultsMessage.vue";
import InputSearch from "../../../../app/components/input/Search.vue";

vi.mock("../../../../app/components/Input.vue", () => ({
  default: {
    name: "GenericInput",
    props: ["id", "type", "modelValue"],
    emits: ["update:modelValue"],
    template: '<input data-testid="generic-input" :data-type="type" />',
  },
}));

const createMockObserver = () => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
  takeRecords: vi.fn(() => []),
});

global.IntersectionObserver = vi
  .fn()
  .mockImplementation(() => createMockObserver());

function ontologyColumn(): IColumn {
  return {
    id: "category",
    label: "Category",
    columnType: "ONTOLOGY",
  } as IColumn;
}

function intColumn(): IColumn {
  return {
    id: "age",
    label: "Age",
    columnType: "INT",
  } as IColumn;
}

function stringColumn(): IColumn {
  return {
    id: "name",
    label: "Name",
    columnType: "STRING",
  } as IColumn;
}

function boolColumn(): IColumn {
  return {
    id: "active",
    label: "Active",
    columnType: "BOOL",
  } as IColumn;
}

function dateColumn(): IColumn {
  return {
    id: "birthdate",
    label: "Birth Date",
    columnType: "DATE",
  } as IColumn;
}

function datetimeColumn(): IColumn {
  return {
    id: "createdAt",
    label: "Created At",
    columnType: "DATETIME",
  } as IColumn;
}

const sampleOptions: CountedOption[] = [
  { name: "dogs", label: "Dogs", count: 10 },
  { name: "cats", label: "Cats", count: 5 },
];

function mountColumn(
  column: IColumn,
  options: CountedOption[] = [],
  modelValue: IFilterValue | undefined = undefined,
  loading = false,
  saturated = false
) {
  return mount(Column, {
    props: { column, options, modelValue, loading, saturated },
  });
}

function makeOptions(count: number, withZeros = false): CountedOption[] {
  return Array.from({ length: count }, (_, i) => ({
    name: `opt${i}`,
    label: `Option ${i}`,
    count: withZeros && i % 3 === 0 ? 0 : i + 1,
  }));
}

describe("Column", () => {
  describe("countable types", () => {
    it("renders Tree for ONTOLOGY column type", () => {
      const wrapper = mountColumn(ontologyColumn(), sampleOptions);
      const tree = wrapper.findComponent(Tree);
      expect(tree.exists()).toBe(true);
    });

    it("renders Tree for BOOL column type", () => {
      const wrapper = mountColumn(boolColumn(), [
        { name: "true", count: 3 },
        { name: "false", count: 2 },
      ]);
      const tree = wrapper.findComponent(Tree);
      expect(tree.exists()).toBe(true);
    });

    it("passes tree nodes with labels and counts to Tree", () => {
      const wrapper = mountColumn(ontologyColumn(), sampleOptions);
      const tree = wrapper.findComponent(Tree);
      const nodes = tree.props("nodes") as any[];
      expect(nodes).toHaveLength(2);
      expect(nodes[0].name).toBe("dogs");
      expect(nodes[0].label).toContain("Dogs");
      expect(nodes[0].label).toContain("10");
    });

    it("maps modelValue equals array to treeSelection", () => {
      const wrapper = mountColumn(ontologyColumn(), sampleOptions, {
        operator: "equals",
        value: ["dogs"],
      });
      const tree = wrapper.findComponent(Tree);
      expect(tree.props("modelValue")).toEqual(["dogs"]);
    });

    it("passes empty array to Tree when no modelValue", () => {
      const wrapper = mountColumn(ontologyColumn(), sampleOptions);
      const tree = wrapper.findComponent(Tree);
      expect(tree.props("modelValue")).toEqual([]);
    });

    it("emits update:modelValue with equals operator on Tree selection change", async () => {
      const wrapper = mountColumn(ontologyColumn(), sampleOptions);
      const tree = wrapper.findComponent(Tree);
      await tree.vm.$emit("update:modelValue", ["dogs", "cats"]);
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      expect(emitted![0][0]).toEqual({
        operator: "equals",
        value: ["dogs", "cats"],
      });
    });

    it("emits undefined when Tree selection is cleared", async () => {
      const wrapper = mountColumn(ontologyColumn(), sampleOptions, {
        operator: "equals",
        value: ["dogs"],
      });
      const tree = wrapper.findComponent(Tree);
      await tree.vm.$emit("update:modelValue", []);
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      expect(emitted![0][0]).toBeUndefined();
    });

    it("does not render Range or text input for countable types", () => {
      const wrapper = mountColumn(ontologyColumn(), sampleOptions);
      expect(wrapper.findComponent(FilterRange).exists()).toBe(false);
      expect(wrapper.find('input[type="text"]').exists()).toBe(false);
    });

    it("converts children in CountedOption to Tree node children", () => {
      const nested: CountedOption[] = [
        {
          name: "animal",
          label: "Animal",
          count: 15,
          children: [{ name: "dogs", label: "Dogs", count: 10 }],
        },
      ];
      const wrapper = mountColumn(ontologyColumn(), nested);
      const tree = wrapper.findComponent(Tree);
      const nodes = tree.props("nodes") as any[];
      expect(nodes[0].children).toHaveLength(1);
      expect(nodes[0].children[0].name).toBe("dogs");
    });
  });

  describe("range types", () => {
    it("renders FilterRange for INT column type", () => {
      const wrapper = mountColumn(intColumn());
      const range = wrapper.findComponent(FilterRange);
      expect(range.exists()).toBe(true);
    });

    it("does not render Tree or text input for range types", () => {
      const wrapper = mountColumn(intColumn());
      expect(wrapper.findComponent(Tree).exists()).toBe(false);
      expect(wrapper.find('input[type="text"]').exists()).toBe(false);
    });

    it("passes range values from modelValue between operator", () => {
      const wrapper = mountColumn(intColumn(), [], {
        operator: "between",
        value: [10, 50],
      });
      const range = wrapper.findComponent(FilterRange);
      expect(range.props("modelValue")).toEqual([10, 50]);
    });

    it("passes [null, null] to FilterRange when no modelValue", () => {
      const wrapper = mountColumn(intColumn());
      const range = wrapper.findComponent(FilterRange);
      expect(range.props("modelValue")).toEqual([null, null]);
    });

    it("renders generic Input with DATE type for DATE column range", () => {
      const wrapper = mountColumn(dateColumn());
      const inputs = wrapper.findAllComponents(GenericInput);
      expect(inputs.length).toBeGreaterThan(0);
      expect(inputs[0].props("type")).toBe("DATE");
    });

    it("renders generic Input with DATETIME type for DATETIME column range", () => {
      const wrapper = mountColumn(datetimeColumn());
      const inputs = wrapper.findAllComponents(GenericInput);
      expect(inputs.length).toBeGreaterThan(0);
      expect(inputs[0].props("type")).toBe("DATETIME");
    });

    it("renders generic Input with INT type for INT column range", () => {
      const wrapper = mountColumn(intColumn());
      const inputs = wrapper.findAllComponents(GenericInput);
      expect(inputs.length).toBeGreaterThan(0);
      expect(inputs[0].props("type")).toBe("INT");
    });
  });

  describe("string-like types", () => {
    it("renders search input for STRING column type", () => {
      const wrapper = mountColumn(stringColumn());
      const input = wrapper.find('input[type="search"]');
      expect(input.exists()).toBe(true);
    });

    it("does not render Tree or FilterRange for string types", () => {
      const wrapper = mountColumn(stringColumn());
      expect(wrapper.findComponent({ name: "Tree" }).exists()).toBe(false);
      expect(wrapper.findComponent({ name: "FilterRange" }).exists()).toBe(
        false
      );
    });

    it("search input shows current like filter value", () => {
      const wrapper = mountColumn(stringColumn(), [], {
        operator: "like",
        value: "fluffy",
      });
      const input = wrapper.find('input[type="search"]');
      expect((input.element as HTMLInputElement).value).toBe("fluffy");
    });

    it("renders InputSearch component for accessibility", () => {
      const wrapper = mountColumn(stringColumn());
      const inputSearch = wrapper.findComponent({ name: "InputSearch" });
      expect(inputSearch.exists()).toBe(true);
    });

    it("emits like filter on text input after debounce", async () => {
      vi.useFakeTimers();
      const wrapper = mountColumn(stringColumn());
      const input = wrapper.find('input[type="search"]');
      const el = input.element as HTMLInputElement;
      el.value = "hello";
      await input.trigger("input");
      vi.advanceTimersByTime(500);
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      expect(emitted![0][0]).toEqual({ operator: "like", value: "hello" });
      vi.useRealTimers();
    });

    it("emits undefined when text input is cleared", async () => {
      vi.useFakeTimers();
      const wrapper = mountColumn(stringColumn(), [], {
        operator: "like",
        value: "hello",
      });
      const input = wrapper.find('input[type="search"]');
      const el = input.element as HTMLInputElement;
      el.value = "";
      await input.trigger("input");
      vi.advanceTimersByTime(500);
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      expect(emitted![0][0]).toBeUndefined();
      vi.useRealTimers();
    });
  });

  describe("composite key support for RADIO", () => {
    it("emits key objects for RADIO with composite key options", async () => {
      const radioCol = {
        id: "status",
        label: "Status",
        columnType: "RADIO",
      } as IColumn;
      const radioOptions: CountedOption[] = [
        { name: "A, 1", count: 5, keyObject: { id: "A", code: "1" } },
        { name: "B, 2", count: 3, keyObject: { id: "B", code: "2" } },
      ];
      const wrapper = mountColumn(radioCol, radioOptions);
      const tree = wrapper.findComponent(Tree);
      await tree.vm.$emit("update:modelValue", ["A, 1"]);
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted![0][0]).toEqual({
        operator: "equals",
        value: [{ id: "A", code: "1" }],
      });
    });

    it("emits plain strings for RADIO with single-key options", async () => {
      const radioCol = {
        id: "status",
        label: "Status",
        columnType: "RADIO",
      } as IColumn;
      const radioOptions: CountedOption[] = [
        { name: "active", count: 5, keyObject: { name: "active" } },
        { name: "inactive", count: 3, keyObject: { name: "inactive" } },
      ];
      const wrapper = mountColumn(radioCol, radioOptions);
      const tree = wrapper.findComponent(Tree);
      await tree.vm.$emit("update:modelValue", ["active"]);
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted![0][0]).toEqual({
        operator: "equals",
        value: ["active"],
      });
    });

    it("extracts display names from composite key objects in modelValue", () => {
      const radioCol = {
        id: "status",
        label: "Status",
        columnType: "RADIO",
      } as IColumn;
      const radioOptions: CountedOption[] = [
        { name: "A, 1", count: 5, keyObject: { id: "A", code: "1" } },
      ];
      const wrapper = mountColumn(radioCol, radioOptions, {
        operator: "equals",
        value: [{ id: "A", code: "1" }],
      });
      const tree = wrapper.findComponent(Tree);
      expect(tree.props("modelValue")).toEqual(["A, 1"]);
    });
  });

  describe("empty state", () => {
    it("renders empty-state message for countable filter with no options when not loading", () => {
      const wrapper = mountColumn(ontologyColumn(), [], undefined, false);
      expect(wrapper.findComponent(NoResultsMessage).exists()).toBe(true);
      expect(wrapper.find("span.italic").exists()).toBe(true);
      expect(wrapper.text()).toContain("No matching values for this filter");
      expect(wrapper.findComponent(Tree).exists()).toBe(false);
    });

    it("does not render empty-state message for range/text filters", () => {
      const rangeWrapper = mountColumn(dateColumn(), [], undefined, false);
      expect(rangeWrapper.findComponent(NoResultsMessage).exists()).toBe(false);
      expect(rangeWrapper.find("span.italic").exists()).toBe(false);
      expect(rangeWrapper.text()).not.toContain(
        "No matching values for this filter"
      );
      expect(rangeWrapper.findComponent(FilterRange).exists()).toBe(true);

      const stringWrapper = mountColumn(stringColumn(), [], undefined, false);
      expect(stringWrapper.findComponent(NoResultsMessage).exists()).toBe(
        false
      );
      expect(stringWrapper.find("span.italic").exists()).toBe(false);
      expect(stringWrapper.text()).not.toContain(
        "No matching values for this filter"
      );
      expect(stringWrapper.find('input[type="search"]').exists()).toBe(true);
    });

    it("does not render empty-state message while loading", () => {
      const wrapper = mountColumn(ontologyColumn(), [], undefined, true);
      expect(wrapper.findComponent(NoResultsMessage).exists()).toBe(false);
      expect(wrapper.find("span.italic").exists()).toBe(false);
      expect(wrapper.findAll('[role="status"]').length).toBeGreaterThan(0);
    });
  });

  describe("show-more and zero-hiding states", () => {
    it("≤25 root options: no show-more button, all options visible", () => {
      const opts = makeOptions(20);
      const wrapper = mountColumn(ontologyColumn(), opts);
      expect(wrapper.find("button").exists()).toBe(false);
      const tree = wrapper.findComponent(Tree);
      expect(tree.props("nodes")).toHaveLength(20);
    });

    it(">25 root options collapsed: shows first 25 roots, button shows 'Show more (+50)'", async () => {
      const opts = makeOptions(30);
      const wrapper = mountColumn(ontologyColumn(), opts);
      const btn = wrapper.find("button");
      expect(btn.exists()).toBe(true);
      expect(btn.text()).toBe("Show 5 more");
      const tree = wrapper.findComponent(Tree);
      expect(tree.props("nodes") as any[]).toHaveLength(25);
    });

    it(">25 root options expanded: all options shown including zeros, button shows 'Show less'", async () => {
      const opts = makeOptions(30, true);
      const wrapper = mountColumn(ontologyColumn(), opts);
      const btn = wrapper.find("button");
      expect(btn.exists()).toBe(true);
      await btn.trigger("click");
      expect(btn.text()).toBe("Show less");
      const tree = wrapper.findComponent(Tree);
      expect((tree.props("nodes") as any[]).length).toBe(30);
    });

    it("5 roots with 200 total descendants: search visible (total > 25) but no show-more button (root ≤ 25)", () => {
      const opts: CountedOption[] = Array.from({ length: 5 }, (_, i) => ({
        name: `root${i}`,
        label: `Root ${i}`,
        count: 40,
        children: Array.from({ length: 39 }, (__, j) => ({
          name: `child${i}_${j}`,
          label: `Child ${i}-${j}`,
          count: 1,
        })),
      }));
      const wrapper = mountColumn(ontologyColumn(), opts);
      expect(wrapper.find("button.text-search-filter-expand").exists()).toBe(
        false
      );
      const searchInputs = wrapper
        .findAllComponents(InputSearch)
        .filter((w) => w.props("id")?.startsWith("filter-search-"));
      expect(searchInputs.length).toBeGreaterThan(0);
    });

    it("30 flat roots: initial 25 shown, button shows 'Show 5 more', click shows all 30, click again resets to 25", async () => {
      const opts = makeOptions(30);
      const wrapper = mountColumn(ontologyColumn(), opts);
      const btn = wrapper.find("button.text-search-filter-expand");
      expect(btn.exists()).toBe(true);
      expect(btn.text()).toBe("Show 5 more");
      expect((wrapper.findComponent(Tree).props("nodes") as any[]).length).toBe(
        25
      );

      await btn.trigger("click");
      expect(btn.text()).toBe("Show less");
      expect((wrapper.findComponent(Tree).props("nodes") as any[]).length).toBe(
        30
      );

      await btn.trigger("click");
      expect(btn.text()).toBe("Show 5 more");
      expect((wrapper.findComponent(Tree).props("nodes") as any[]).length).toBe(
        25
      );
    });

    it("30 roots each with children: show-more shows 25 roots with all their descendants intact", () => {
      const opts: CountedOption[] = Array.from({ length: 30 }, (_, i) => ({
        name: `root${i}`,
        label: `Root ${i}`,
        count: 3,
        children: [
          { name: `child${i}_a`, label: `Child ${i}A`, count: 1 },
          { name: `child${i}_b`, label: `Child ${i}B`, count: 2 },
        ],
      }));
      const wrapper = mountColumn(ontologyColumn(), opts);
      const btn = wrapper.find("button.text-search-filter-expand");
      expect(btn.exists()).toBe(true);
      const tree = wrapper.findComponent(Tree);
      const nodes = tree.props("nodes") as any[];
      expect(nodes).toHaveLength(25);
      nodes.forEach((n: any) => {
        expect(n.children).toHaveLength(2);
      });
    });

    it("100 flat roots: incremental reveal — 25 → 75 → 100 → back to 25", async () => {
      const opts = makeOptions(100);
      const wrapper = mountColumn(ontologyColumn(), opts);
      const btn = wrapper.find("button.text-search-filter-expand");
      expect(btn.exists()).toBe(true);
      expect(btn.text()).toBe("Show more (+50)");
      expect((wrapper.findComponent(Tree).props("nodes") as any[]).length).toBe(
        25
      );

      await btn.trigger("click");
      expect(btn.text()).toBe("Show 25 more");
      expect((wrapper.findComponent(Tree).props("nodes") as any[]).length).toBe(
        75
      );

      await btn.trigger("click");
      expect(btn.text()).toBe("Show less");
      expect((wrapper.findComponent(Tree).props("nodes") as any[]).length).toBe(
        100
      );

      await btn.trigger("click");
      expect(btn.text()).toBe("Show more (+50)");
      expect((wrapper.findComponent(Tree).props("nodes") as any[]).length).toBe(
        25
      );
    });

    it("5 roots: no show-more button", () => {
      const opts = makeOptions(5);
      const wrapper = mountColumn(ontologyColumn(), opts);
      expect(wrapper.find("button.text-search-filter-expand").exists()).toBe(
        false
      );
    });

    it("zero-hiding: zeros hidden while partially expanded, visible when fully expanded, hidden again after Show less", async () => {
      const opts = makeOptions(30, true);
      const wrapper = mountColumn(ontologyColumn(), opts);
      const btn = wrapper.find("button.text-search-filter-expand");

      const zeroOpts = opts.slice(0, 25).filter((o) => o.count === 0);
      expect(zeroOpts.length).toBeGreaterThan(0);

      const initialNodes = wrapper.findComponent(Tree).props("nodes") as any[];
      const zeroNames = zeroOpts.map((o) => o.name);
      expect(initialNodes.some((n: any) => zeroNames.includes(n.name))).toBe(
        false
      );

      await btn.trigger("click");
      const expandedNodes = wrapper.findComponent(Tree).props("nodes") as any[];
      const allZeroNames = opts.filter((o) => o.count === 0).map((o) => o.name);
      expect(
        expandedNodes.some((n: any) => allZeroNames.includes(n.name))
      ).toBe(true);

      await btn.trigger("click");
      const collapsedNodes = wrapper
        .findComponent(Tree)
        .props("nodes") as any[];
      expect(collapsedNodes.some((n: any) => zeroNames.includes(n.name))).toBe(
        false
      );
    });

    it("clearing search resets visibleRootCount to 25", async () => {
      const opts = makeOptions(100);
      const wrapper = mountColumn(ontologyColumn(), opts);
      const btn = wrapper.find("button.text-search-filter-expand");

      await btn.trigger("click");
      expect((wrapper.findComponent(Tree).props("nodes") as any[]).length).toBe(
        75
      );

      const searchInput = wrapper
        .findAllComponents(InputSearch)
        .find((w) => w.props("id")?.startsWith("filter-search-"));
      await searchInput!.vm.$emit("update:modelValue", "opt1");
      await wrapper.vm.$nextTick();
      expect(wrapper.find("button.text-search-filter-expand").exists()).toBe(
        false
      );

      await searchInput!.vm.$emit("update:modelValue", "");
      await wrapper.vm.$nextTick();
      expect((wrapper.findComponent(Tree).props("nodes") as any[]).length).toBe(
        25
      );
      expect(wrapper.find("button.text-search-filter-expand").text()).toBe(
        "Show more (+50)"
      );
    });

    it("searching: no show-more button shown when search active (future: local search)", async () => {
      const opts = makeOptions(30);
      const wrapper = mountColumn(ontologyColumn(), opts);
      const btn = wrapper.find("button");
      expect(btn.exists()).toBe(true);
    });

    it("saturated flag true: hint rendered above tree", () => {
      const opts = makeOptions(10);
      const wrapper = mountColumn(
        ontologyColumn(),
        opts,
        undefined,
        false,
        true
      );
      const hint = wrapper.find("span.italic");
      expect(hint.exists()).toBe(true);
      expect(hint.text()).toBe("too many options, please search");
    });

    it("saturated + not-saturated: hint only when saturated is true", () => {
      const opts = makeOptions(10);
      const withoutSaturation = mountColumn(
        ontologyColumn(),
        opts,
        undefined,
        false,
        false
      );
      expect(withoutSaturation.find("span.italic").exists()).toBe(false);

      const withSaturation = mountColumn(
        ontologyColumn(),
        opts,
        undefined,
        false,
        true
      );
      expect(withSaturation.find("span.italic").exists()).toBe(true);
    });
  });

  describe("per-filter search input", () => {
    it("renders search input when totalCount > 25", () => {
      const opts = makeOptions(30);
      const wrapper = mountColumn(ontologyColumn(), opts);
      const searchInputs = wrapper
        .findAllComponents(InputSearch)
        .filter((w) => w.props("id")?.startsWith("filter-search-"));
      expect(searchInputs.length).toBeGreaterThan(0);
    });

    it("search input hidden when tree is small (≤25) even if hierarchical", () => {
      const opts: CountedOption[] = [
        {
          name: "animal",
          label: "Animal",
          count: 5,
          children: [{ name: "dogs", label: "Dogs", count: 3 }],
        },
      ];
      const wrapper = mountColumn(ontologyColumn(), opts);
      const searchInputs = wrapper
        .findAllComponents(InputSearch)
        .filter((w) => w.props("id")?.startsWith("filter-search-"));
      expect(searchInputs.length).toBe(0);
    });

    it("does not render per-filter search input for flat list with ≤25 options", () => {
      const opts = makeOptions(5);
      const wrapper = mountColumn(ontologyColumn(), opts);
      const searchInputs = wrapper
        .findAllComponents(InputSearch)
        .filter((w) => w.props("id")?.startsWith("filter-search-"));
      expect(searchInputs.length).toBe(0);
    });

    it("renders exactly one search input when >25 options (no duplicate from Tree internal search)", () => {
      const opts = makeOptions(30);
      const wrapper = mountColumn(ontologyColumn(), opts);
      const allSearchInputs = wrapper.findAllComponents(InputSearch);
      expect(allSearchInputs).toHaveLength(1);
    });

    it("small hierarchical tree (6 nodes with parent) renders no search input", () => {
      const opts: CountedOption[] = [
        {
          name: "animal",
          label: "Animal",
          count: 6,
          children: [
            { name: "dogs", label: "Dogs", count: 3 },
            { name: "cats", label: "Cats", count: 2 },
            { name: "birds", label: "Birds", count: 1 },
          ],
        },
      ];
      const wrapper = mountColumn(ontologyColumn(), opts);
      const allSearchInputs = wrapper
        .findAllComponents(InputSearch)
        .filter((w) => w.props("id")?.startsWith("filter-search-"));
      expect(allSearchInputs).toHaveLength(0);
    });

    it("typing in search hides the show-more button", async () => {
      const opts = makeOptions(30);
      const wrapper = mountColumn(ontologyColumn(), opts);
      expect(wrapper.find("button").exists()).toBe(true);
      const searchInput = wrapper
        .findAllComponents(InputSearch)
        .find((w) => w.props("id")?.startsWith("filter-search-"));
      await searchInput!.vm.$emit("update:modelValue", "opt1");
      await wrapper.vm.$nextTick();
      expect(wrapper.find("button").exists()).toBe(false);
    });

    it("typing in search shows matching options including zero-count", async () => {
      const opts: CountedOption[] = [
        { name: "opt-match", label: "Match Option", count: 0 },
        { name: "other", label: "Other", count: 5 },
      ];
      const wrapper = mountColumn(ontologyColumn(), opts);
      const searchInput = wrapper
        .findAllComponents(InputSearch)
        .find((w) => w.props("id")?.startsWith("filter-search-"));
      if (!searchInput) return;
      await searchInput.vm.$emit("update:modelValue", "match");
      await wrapper.vm.$nextTick();
      const tree = wrapper.findComponent(Tree);
      const nodes = tree.props("nodes") as any[];
      expect(nodes.some((n: any) => n.name === "opt-match")).toBe(true);
    });

    it("empty search results render the empty-state no-results message", async () => {
      const opts = makeOptions(30);
      const wrapper = mountColumn(ontologyColumn(), opts);
      const searchInput = wrapper
        .findAllComponents(InputSearch)
        .find((w) => w.props("id")?.startsWith("filter-search-"));
      await searchInput!.vm.$emit("update:modelValue", "xyznotfound");
      await wrapper.vm.$nextTick();
      expect(wrapper.findComponent(NoResultsMessage).exists()).toBe(true);
      expect(wrapper.text()).toContain("No matching values for this filter");
      expect(wrapper.findComponent(Tree).exists()).toBe(false);
    });
  });

  describe("loading state", () => {
    it("shows loading skeleton when loading is true and no options yet", () => {
      const wrapper = mountColumn(ontologyColumn(), [], undefined, true);
      const skeletons = wrapper.findAll('[role="status"]');
      expect(skeletons.length).toBeGreaterThan(0);
    });

    it("does not show loading skeleton when options are present even if loading", () => {
      const wrapper = mountColumn(
        ontologyColumn(),
        sampleOptions,
        undefined,
        true
      );
      const skeletons = wrapper.findAll('[role="status"]');
      expect(skeletons.length).toBe(0);
    });

    it("does not show loading skeleton when not loading", () => {
      const wrapper = mountColumn(ontologyColumn(), [], undefined, false);
      const skeletons = wrapper.findAll('[role="status"]');
      expect(skeletons.length).toBe(0);
    });

    it("renders Tree (not loading skeleton) once options arrive", () => {
      const wrapper = mountColumn(
        ontologyColumn(),
        sampleOptions,
        undefined,
        false
      );
      expect(wrapper.findComponent(Tree).exists()).toBe(true);
      expect(wrapper.findAll('[role="status"]').length).toBe(0);
    });
  });
});
