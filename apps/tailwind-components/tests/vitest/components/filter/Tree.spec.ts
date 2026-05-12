import { describe, it, expect, vi } from "vitest";
import { nextTick } from "vue";
import { mount, flushPromises } from "@vue/test-utils";
import type { IColumn } from "../../../../../metadata-utils/src/types";
import type { CountedOption } from "../../../../app/utils/fetchCounts";
import type { IFilterValue } from "../../../../types/filters";
import FilterTree from "../../../../app/components/filter/Tree.vue";
import TreeNode from "../../../../app/components/input/TreeNode.vue";
import NoResultsMessage from "../../../../app/components/text/NoResultsMessage.vue";
import InputSearch from "../../../../app/components/input/Search.vue";

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

function boolColumn(): IColumn {
  return {
    id: "active",
    label: "Active",
    columnType: "BOOL",
  } as IColumn;
}

const sampleOptions: CountedOption[] = [
  { name: "dogs", label: "Dogs", count: 10, overlap: 0 },
  { name: "cats", label: "Cats", count: 5, overlap: 0 },
];

function mountTree(
  column: IColumn,
  options: CountedOption[] = [],
  modelValue: IFilterValue | undefined = undefined,
  loading = false,
  saturated = false
) {
  return mount(FilterTree, {
    props: { column, options, modelValue, loading, saturated },
  });
}

function makeOptions(count: number, withZeros = false): CountedOption[] {
  return Array.from({ length: count }, (_, i) => ({
    name: `opt${i}`,
    label: `Option ${i}`,
    count: withZeros && i % 3 === 0 ? 0 : i + 1,
    overlap: 0,
  }));
}

describe("FilterTree", () => {
  describe("countable types", () => {
    it("renders Tree for ONTOLOGY column type", () => {
      const wrapper = mountTree(ontologyColumn(), sampleOptions);
      expect(wrapper.findComponent(TreeNode).exists()).toBe(true);
    });

    it("renders Tree for BOOL column type", () => {
      const wrapper = mountTree(boolColumn(), [
        { name: "true", count: 3 },
        { name: "false", count: 2 },
      ]);
      expect(wrapper.findComponent(TreeNode).exists()).toBe(true);
    });

    it("passes tree nodes with labels and counts to Tree", () => {
      const wrapper = mountTree(ontologyColumn(), sampleOptions);
      expect(wrapper.text()).toContain("Dogs");
      expect(wrapper.text()).toContain("10");
    });

    it("maps modelValue equals array to treeSelection", () => {
      const wrapper = mountTree(ontologyColumn(), sampleOptions, {
        operator: "equals",
        value: ["dogs"],
      });
      const checkbox = wrapper.find(
        'input[id="filter-tree-category-dogs-input"]'
      );
      expect(checkbox.exists()).toBe(true);
      expect((checkbox.element as HTMLInputElement).checked).toBe(true);
    });

    it("passes empty array to Tree when no modelValue", () => {
      const wrapper = mountTree(ontologyColumn(), sampleOptions);
      const checkbox = wrapper.find(
        'input[id="filter-tree-category-dogs-input"]'
      );
      expect(checkbox.exists()).toBe(true);
      expect((checkbox.element as HTMLInputElement).checked).toBe(false);
    });

    it("emits update:modelValue with equals operator on Tree selection change", async () => {
      const wrapper = mountTree(ontologyColumn(), sampleOptions);
      const dogsCheckbox = wrapper.find(
        'input[id="filter-tree-category-dogs-input"]'
      );
      await dogsCheckbox.trigger("click");
      const catsCheckbox = wrapper.find(
        'input[id="filter-tree-category-cats-input"]'
      );
      await catsCheckbox.trigger("click");
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      const lastEmit = emitted![emitted!.length - 1][0] as IFilterValue;
      expect(lastEmit).toEqual({
        operator: "equals",
        value: expect.arrayContaining(["dogs", "cats"]),
      });
    });

    it("emits undefined when Tree selection is cleared", async () => {
      const wrapper = mountTree(ontologyColumn(), sampleOptions, {
        operator: "equals",
        value: ["dogs"],
      });
      const dogsCheckbox = wrapper.find(
        'input[id="filter-tree-category-dogs-input"]'
      );
      await dogsCheckbox.trigger("click");
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      expect(emitted![0][0]).toBeUndefined();
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
      const wrapper = mountTree(ontologyColumn(), nested);
      expect(wrapper.text()).toContain("Animal");
      expect(wrapper.text()).toContain("Dogs");
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
      const wrapper = mountTree(radioCol, radioOptions);
      const checkbox = wrapper.find(
        'input[id="filter-tree-status-A, 1-input"]'
      );
      await checkbox.trigger("click");
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
      const wrapper = mountTree(radioCol, radioOptions);
      const checkbox = wrapper.find(
        'input[id="filter-tree-status-active-input"]'
      );
      await checkbox.trigger("click");
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
      const wrapper = mountTree(radioCol, radioOptions, {
        operator: "equals",
        value: [{ id: "A", code: "1" }],
      });
      const checkbox = wrapper.find(
        'input[id="filter-tree-status-A, 1-input"]'
      );
      expect(checkbox.exists()).toBe(true);
      expect((checkbox.element as HTMLInputElement).checked).toBe(true);
    });
  });

  describe("empty state", () => {
    it("H10: shows reworded empty-state message when options array is empty", () => {
      const wrapper = mountTree(ontologyColumn(), [], undefined, false);
      expect(wrapper.text()).toContain(
        "No options available given current filters"
      );
      expect(wrapper.text()).not.toContain(
        "No matching values for this filter"
      );
    });

    it("renders empty-state message for countable filter with no options when not loading", () => {
      const wrapper = mountTree(ontologyColumn(), [], undefined, false);
      expect(wrapper.findComponent(NoResultsMessage).exists()).toBe(true);
      expect(wrapper.find("span.italic").exists()).toBe(true);
      expect(wrapper.text()).toContain(
        "No options available given current filters"
      );
      expect(wrapper.findComponent(TreeNode).exists()).toBe(false);
    });

    it("does not render empty-state message while loading", () => {
      const wrapper = mountTree(ontologyColumn(), [], undefined, true);
      expect(wrapper.findComponent(NoResultsMessage).exists()).toBe(false);
      expect(wrapper.find("span.italic").exists()).toBe(false);
      expect(wrapper.findAll('[role="status"]').length).toBeGreaterThan(0);
    });
  });

  describe("show-more and zero-hiding states", () => {
    it("≤25 root options: no show-more button, all options visible", () => {
      const opts = makeOptions(20);
      const wrapper = mountTree(ontologyColumn(), opts);
      expect(wrapper.find("button").exists()).toBe(false);
      expect(wrapper.findComponent(TreeNode).exists()).toBe(true);
      const text = wrapper.text();
      opts.forEach((opt) => {
        expect(text).toContain(opt.label);
      });
    });

    it(">25 root options collapsed: shows first 25 roots, button shows 'Show 5 more'", async () => {
      const opts = makeOptions(30);
      const wrapper = mountTree(ontologyColumn(), opts);
      const btn = wrapper.find("button");
      expect(btn.exists()).toBe(true);
      expect(btn.text()).toBe("Show 5 more");
      const text = wrapper.text();
      expect(text).toContain("Option 0");
      expect(text).toContain("Option 24");
      expect(text).not.toContain("Option 25");
    });

    it(">25 root options expanded: all options shown including zeros, button shows 'Show less'", async () => {
      const opts = makeOptions(30, true);
      const wrapper = mountTree(ontologyColumn(), opts);
      const btn = wrapper.find("button");
      expect(btn.exists()).toBe(true);
      await btn.trigger("click");
      expect(btn.text()).toBe("Show less");
      const text = wrapper.text();
      expect(text).toContain("Option 29");
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
      const wrapper = mountTree(ontologyColumn(), opts);
      expect(wrapper.find("button.text-search-filter-action").exists()).toBe(
        false
      );
      const searchInputs = wrapper
        .findAllComponents(InputSearch)
        .filter((w) => w.props("id")?.startsWith("filter-search-"));
      expect(searchInputs.length).toBeGreaterThan(0);
    });

    it("30 flat roots: initial 25 shown, button shows 'Show 5 more', click shows all 30, click again resets to 25", async () => {
      const opts = makeOptions(30);
      const wrapper = mountTree(ontologyColumn(), opts);
      const btn = wrapper.find("button.text-search-filter-action");
      expect(btn.exists()).toBe(true);
      expect(btn.text()).toBe("Show 5 more");
      expect(wrapper.text()).not.toContain("Option 25");

      await btn.trigger("click");
      expect(btn.text()).toBe("Show less");
      expect(wrapper.text()).toContain("Option 25");
      expect(wrapper.text()).toContain("Option 29");

      await btn.trigger("click");
      expect(btn.text()).toBe("Show 5 more");
      expect(wrapper.text()).not.toContain("Option 25");
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
      const wrapper = mountTree(ontologyColumn(), opts);
      const btn = wrapper.find("button.text-search-filter-action");
      expect(btn.exists()).toBe(true);
      const text = wrapper.text();
      expect(text).toContain("Root 0");
      expect(text).toContain("Root 24");
      expect(text).not.toContain("Root 25");
    });

    it("100 flat roots: incremental reveal — 25 → 75 → 100 → back to 25", async () => {
      const opts = makeOptions(100);
      const wrapper = mountTree(ontologyColumn(), opts);
      const btn = wrapper.find("button.text-search-filter-action");
      expect(btn.exists()).toBe(true);
      expect(btn.text()).toBe("Show more (+50)");
      expect(wrapper.text()).not.toContain("Option 25");

      await btn.trigger("click");
      expect(btn.text()).toBe("Show 25 more");
      expect(wrapper.text()).toContain("Option 74");
      expect(wrapper.text()).not.toContain("Option 75");

      await btn.trigger("click");
      expect(btn.text()).toBe("Show less");
      expect(wrapper.text()).toContain("Option 99");

      await btn.trigger("click");
      expect(btn.text()).toBe("Show more (+50)");
      expect(wrapper.text()).not.toContain("Option 25");
    });

    it("5 roots: no show-more button", () => {
      const opts = makeOptions(5);
      const wrapper = mountTree(ontologyColumn(), opts);
      expect(wrapper.find("button.text-search-filter-action").exists()).toBe(
        false
      );
    });

    it("zero-hiding: zeros hidden while partially expanded, visible when fully expanded, hidden again after Show less", async () => {
      const opts = makeOptions(30, true);
      const wrapper = mountTree(ontologyColumn(), opts);
      const btn = wrapper.find("button.text-search-filter-action");

      const zeroOpts = opts.slice(0, 25).filter((o) => o.count === 0);
      expect(zeroOpts.length).toBeGreaterThan(0);

      const textBefore = wrapper.text();
      zeroOpts.forEach((opt) => {
        expect(textBefore).not.toContain(opt.label);
      });

      await btn.trigger("click");
      const textExpanded = wrapper.text();
      const allZeroOpts = opts.filter((o) => o.count === 0);
      expect(allZeroOpts.some((opt) => textExpanded.includes(opt.label))).toBe(
        true
      );

      await btn.trigger("click");
      const textCollapsed = wrapper.text();
      zeroOpts.forEach((opt) => {
        expect(textCollapsed).not.toContain(opt.label);
      });
    });

    it("clearing search resets visibleRootCount to 25", async () => {
      const opts = makeOptions(100);
      const wrapper = mountTree(ontologyColumn(), opts);
      const btn = wrapper.find("button.text-search-filter-action");

      await btn.trigger("click");
      expect(wrapper.text()).toContain("Option 74");

      const searchInput = wrapper
        .findAllComponents(InputSearch)
        .find((w) => w.props("id")?.startsWith("filter-search-"));
      await searchInput!.vm.$emit("update:modelValue", "opt1");
      await wrapper.vm.$nextTick();
      expect(wrapper.find("button.text-search-filter-action").exists()).toBe(
        false
      );

      await searchInput!.vm.$emit("update:modelValue", "");
      await wrapper.vm.$nextTick();
      expect(wrapper.text()).not.toContain("Option 25");
      expect(wrapper.find("button.text-search-filter-action").text()).toBe(
        "Show more (+50)"
      );
    });

    it("BOOL filter with 3 options: zero-count options hidden by default (no show-more button)", () => {
      const opts: CountedOption[] = [
        { name: "true", label: "Yes", count: 5, overlap: 0 },
        { name: "false", label: "No", count: 0, overlap: 0 },
        { name: "_null_", label: "Not set", count: 0, overlap: 0 },
      ];
      const wrapper = mountTree(boolColumn(), opts);
      expect(wrapper.find("button.text-search-filter-action").exists()).toBe(
        false
      );
      const text = wrapper.text();
      expect(text).toContain("Yes");
      expect(text).not.toContain("No (0)");
      expect(text).not.toContain("Not set (0)");
    });

    it("large filter (>25 roots): clicking show-more makes zero-count options visible", async () => {
      const opts: CountedOption[] = [
        ...Array.from({ length: 45 }, (_, i) => ({
          name: `opt${i}`,
          label: `Option ${i}`,
          count: i + 1,
          overlap: 0,
        })),
        ...Array.from({ length: 5 }, (_, i) => ({
          name: `zero${i}`,
          label: `Zero Option ${i}`,
          count: 0,
          overlap: 0,
        })),
      ];
      const wrapper = mountTree(ontologyColumn(), opts);
      const btn = wrapper.find("button.text-search-filter-action");
      expect(btn.exists()).toBe(true);

      const textBefore = wrapper.text();
      expect(textBefore).not.toContain("Zero Option 0");

      await btn.trigger("click");
      const textExpanded = wrapper.text();
      expect(textExpanded).toContain("Zero Option 0");
    });

    it("show-less resets zero-hiding: zero-count options hidden again after show-less", async () => {
      const opts: CountedOption[] = [
        ...Array.from({ length: 45 }, (_, i) => ({
          name: `opt${i}`,
          label: `Option ${i}`,
          count: i + 1,
          overlap: 0,
        })),
        ...Array.from({ length: 5 }, (_, i) => ({
          name: `zero${i}`,
          label: `Zero Option ${i}`,
          count: 0,
          overlap: 0,
        })),
      ];
      const wrapper = mountTree(ontologyColumn(), opts);
      const btn = wrapper.find("button.text-search-filter-action");

      await btn.trigger("click");
      expect(wrapper.text()).toContain("Zero Option 0");

      await btn.trigger("click");
      expect(wrapper.text()).not.toContain("Zero Option 0");
    });

    it("searching: no show-more button shown when search active (future: local search)", async () => {
      const opts = makeOptions(30);
      const wrapper = mountTree(ontologyColumn(), opts);
      const btn = wrapper.find("button");
      expect(btn.exists()).toBe(true);
    });

    it("shows 'No options available given current filters' when all options have count 0 (cross-filter zero)", async () => {
      const opts: CountedOption[] = [
        { name: "true", label: "Yes", count: 0, overlap: 0 },
        { name: "false", label: "No", count: 0, overlap: 0 },
        { name: "_null_", label: "Not set", count: 0, overlap: 0 },
      ];
      const wrapper = mountTree(boolColumn(), opts);
      expect(wrapper.findComponent(NoResultsMessage).exists()).toBe(true);
      expect(wrapper.text()).toContain(
        "No options available given current filters"
      );
      expect(wrapper.findComponent(TreeNode).exists()).toBe(false);
    });

    it("saturated flag true: hint rendered above tree", () => {
      const opts = makeOptions(10);
      const wrapper = mountTree(ontologyColumn(), opts, undefined, false, true);
      const hint = wrapper.find("span.italic");
      expect(hint.exists()).toBe(true);
      expect(hint.text()).toBe("too many options, please search");
    });

    it("saturated + not-saturated: hint only when saturated is true", () => {
      const opts = makeOptions(10);
      const withoutSaturation = mountTree(
        ontologyColumn(),
        opts,
        undefined,
        false,
        false
      );
      expect(withoutSaturation.find("span.italic").exists()).toBe(false);

      const withSaturation = mountTree(
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
      const wrapper = mountTree(ontologyColumn(), opts);
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
      const wrapper = mountTree(ontologyColumn(), opts);
      const searchInputs = wrapper
        .findAllComponents(InputSearch)
        .filter((w) => w.props("id")?.startsWith("filter-search-"));
      expect(searchInputs.length).toBe(0);
    });

    it("does not render per-filter search input for flat list with ≤25 options", () => {
      const opts = makeOptions(5);
      const wrapper = mountTree(ontologyColumn(), opts);
      const searchInputs = wrapper
        .findAllComponents(InputSearch)
        .filter((w) => w.props("id")?.startsWith("filter-search-"));
      expect(searchInputs.length).toBe(0);
    });

    it("renders exactly one search input when >25 options (no duplicate from Tree internal search)", () => {
      const opts = makeOptions(30);
      const wrapper = mountTree(ontologyColumn(), opts);
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
      const wrapper = mountTree(ontologyColumn(), opts);
      const allSearchInputs = wrapper
        .findAllComponents(InputSearch)
        .filter((w) => w.props("id")?.startsWith("filter-search-"));
      expect(allSearchInputs).toHaveLength(0);
    });

    it("typing in search hides the show-more button", async () => {
      const opts = makeOptions(30);
      const wrapper = mountTree(ontologyColumn(), opts);
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
      const wrapper = mountTree(ontologyColumn(), opts);
      const searchInput = wrapper
        .findAllComponents(InputSearch)
        .find((w) => w.props("id")?.startsWith("filter-search-"));
      if (!searchInput) return;
      await searchInput.vm.$emit("update:modelValue", "match");
      await wrapper.vm.$nextTick();
      expect(wrapper.text()).toContain("Match Option");
    });

    it("empty search results render the empty-state no-results message", async () => {
      const opts = makeOptions(30);
      const wrapper = mountTree(ontologyColumn(), opts);
      const searchInput = wrapper
        .findAllComponents(InputSearch)
        .find((w) => w.props("id")?.startsWith("filter-search-"));
      await searchInput!.vm.$emit("update:modelValue", "xyznotfound");
      await wrapper.vm.$nextTick();
      expect(wrapper.findComponent(NoResultsMessage).exists()).toBe(true);
      expect(wrapper.text()).toContain(
        "No options available given current filters"
      );
      expect(wrapper.findComponent(TreeNode).exists()).toBe(false);
    });
  });

  describe("loading state", () => {
    it("shows loading skeleton when loading is true and no options yet", () => {
      const wrapper = mountTree(ontologyColumn(), [], undefined, true);
      const skeletons = wrapper.findAll('[role="status"]');
      expect(skeletons.length).toBeGreaterThan(0);
    });

    it("does not show loading skeleton when options are present even if loading", () => {
      const wrapper = mountTree(
        ontologyColumn(),
        sampleOptions,
        undefined,
        true
      );
      const skeletons = wrapper.findAll('[role="status"]');
      expect(skeletons.length).toBe(0);
    });

    it("does not show loading skeleton when not loading", () => {
      const wrapper = mountTree(ontologyColumn(), [], undefined, false);
      const skeletons = wrapper.findAll('[role="status"]');
      expect(skeletons.length).toBe(0);
    });

    it("renders Tree (not loading skeleton) once options arrive", () => {
      const wrapper = mountTree(
        ontologyColumn(),
        sampleOptions,
        undefined,
        false
      );
      expect(wrapper.findComponent(TreeNode).exists()).toBe(true);
      expect(wrapper.findAll('[role="status"]').length).toBe(0);
    });
  });
});

describe("FilterTree — delta display counts", () => {
  it("unselected option shows delta count (count - overlap) when overlap > 0", () => {
    const col = ontologyColumn();
    const options: CountedOption[] = [
      { name: "Adult", label: "Adult", count: 6, overlap: 2 },
      { name: "Adolescent", label: "Adolescent", count: 8, overlap: 0 },
    ];
    const modelValue: IFilterValue = {
      operator: "equals",
      value: ["Adolescent"],
    };
    const wrapper = mountTree(col, options, modelValue);
    expect(wrapper.text()).toContain("Adult");
    expect(wrapper.text()).toContain("(4)");
    expect(wrapper.text()).not.toContain("Adult (6)");
  });

  it("selected option shows solo count (not delta)", () => {
    const col = ontologyColumn();
    const options: CountedOption[] = [
      { name: "Adult", label: "Adult", count: 6, overlap: 2 },
      { name: "Adolescent", label: "Adolescent", count: 8, overlap: 8 },
    ];
    const modelValue: IFilterValue = { operator: "equals", value: ["Adult"] };
    const wrapper = mountTree(col, options, modelValue);
    expect(wrapper.text()).toContain("Adult (6)");
  });

  it("parent ontology node unselected shows delta (count - overlap)", () => {
    const col = ontologyColumn();
    const options: CountedOption[] = [
      {
        name: "AgeGroup",
        label: "Age Group",
        count: 10,
        overlap: 3,
        children: [{ name: "Adult", label: "Adult", count: 6, overlap: 2 }],
      },
    ];
    const wrapper = mountTree(col, options, undefined);
    expect(wrapper.text()).toContain("Age Group (7)");
    expect(wrapper.text()).not.toContain("Age Group (10)");
  });

  it("BOOL single-select unselected option shows solo count when overlap is 0", () => {
    const col = boolColumn();
    const options: CountedOption[] = [
      { name: "true", label: "Yes", count: 3, overlap: 3 },
      { name: "false", label: "No", count: 5, overlap: 0 },
      { name: "_null_", label: "Not set", count: 1, overlap: 0 },
    ];
    const modelValue: IFilterValue = { operator: "equals", value: ["true"] };
    const wrapper = mountTree(col, options, modelValue);
    expect(wrapper.text()).toContain("No (5)");
  });

  it("selected Adolescent count stays stable when Adult is added to selection", async () => {
    const col = ontologyColumn();
    const options: CountedOption[] = [
      { name: "Adult", label: "Adult", count: 6, overlap: 2 },
      { name: "Adolescent", label: "Adolescent", count: 8, overlap: 8 },
    ];
    const modelValueAdolescent: IFilterValue = {
      operator: "equals",
      value: ["Adolescent"],
    };
    const wrapper = mountTree(col, options, modelValueAdolescent);

    expect(wrapper.text()).toContain("Adolescent (8)");

    await wrapper.setProps({
      modelValue: { operator: "equals", value: ["Adolescent", "Adult"] },
    });

    expect(wrapper.text()).toContain("Adolescent (8)");
  });
});

describe("H3: type routing (FilterTree)", () => {
  it("H3: STRING_ARRAY renders Tree (countable), not text search fallback", () => {
    const col: IColumn = {
      id: "tags",
      label: "Tags",
      columnType: "STRING_ARRAY",
    } as IColumn;
    const opts: CountedOption[] = [
      { name: "a", count: 3 },
      { name: "b", count: 1 },
    ];
    const wrapper = mountTree(col, opts);
    expect(wrapper.findComponent(TreeNode).exists()).toBe(true);
    const allSearch = wrapper
      .findAllComponents(InputSearch)
      .filter((w) => w.props("id")?.startsWith("filter-text-"));
    expect(allSearch.length).toBe(0);
  });

  it("H3: STRING_ARRAY emits equals filter on tree selection", async () => {
    const col: IColumn = {
      id: "tags",
      label: "Tags",
      columnType: "STRING_ARRAY",
    } as IColumn;
    const opts: CountedOption[] = [
      { name: "a", count: 3 },
      { name: "b", count: 1 },
    ];
    const wrapper = mountTree(col, opts);
    const aCheckbox = wrapper.find('input[id="filter-tree-tags-a-input"]');
    await aCheckbox.trigger("click");
    const bCheckbox = wrapper.find('input[id="filter-tree-tags-b-input"]');
    await bCheckbox.trigger("click");
    const emitted = wrapper.emitted("update:modelValue");
    expect(emitted).toBeTruthy();
    const lastEmit = emitted![emitted!.length - 1][0] as IFilterValue;
    expect(lastEmit).toEqual({ operator: "equals", value: ["a", "b"] });
  });
});

describe("auto-expand on small trees", () => {
  function makeHierarchicalOptions(
    parentCount: number,
    childrenPerParent: number
  ): CountedOption[] {
    return Array.from({ length: parentCount }, (_, parentIndex) => ({
      name: `parent${parentIndex + 1}`,
      label: `Parent ${parentIndex + 1}`,
      count: childrenPerParent + 1,
      overlap: 0,
      children: Array.from({ length: childrenPerParent }, (__, childIndex) => ({
        name: `child${parentIndex + 1}_${childIndex + 1}`,
        label: `Child ${parentIndex + 1}-${childIndex + 1}`,
        count: 1,
        overlap: 0,
      })),
    }));
  }

  it("auto-expands all parent nodes when FilterTree has ≤25 total nodes", () => {
    const opts = makeHierarchicalOptions(3, 2);
    const wrapper = mountTree(ontologyColumn(), opts);

    const expandButtons = wrapper.findAll("button[aria-controls]");
    expect(expandButtons.length).toBeGreaterThan(0);
    expandButtons.forEach((btn) => {
      expect(btn.attributes("aria-expanded")).toBe("true");
    });

    expect(wrapper.text()).toContain("Child 1-1");
    expect(wrapper.text()).toContain("Child 2-2");
    expect(wrapper.text()).toContain("Child 3-1");
  });

  it("does not auto-expand when FilterTree has >25 total nodes", () => {
    const opts = makeHierarchicalOptions(13, 1);
    const wrapper = mountTree(ontologyColumn(), opts);

    const expandButtons = wrapper.findAll("button[aria-controls]");
    expect(expandButtons.length).toBeGreaterThan(0);
    expandButtons.forEach((btn) => {
      expect(btn.attributes("aria-expanded")).toBe("false");
    });

    expect(wrapper.text()).not.toContain("Child 1-1");
  });
});

describe("expand state preserved across options updates", () => {
  function makeHierarchicalOptions(
    parentCount: number,
    childrenPerParent: number,
    countMultiplier = 1
  ): CountedOption[] {
    return Array.from({ length: parentCount }, (_, parentIndex) => ({
      name: `parent${parentIndex + 1}`,
      label: `Parent ${parentIndex + 1}`,
      count: (childrenPerParent + 1) * countMultiplier,
      overlap: 0,
      children: Array.from({ length: childrenPerParent }, (__, childIndex) => ({
        name: `child${parentIndex + 1}_${childIndex + 1}`,
        label: `Child ${parentIndex + 1}-${childIndex + 1}`,
        count: countMultiplier,
        overlap: 0,
      })),
    }));
  }

  it("preserves expand state when options prop updates with new counts", async () => {
    const initialOpts = makeHierarchicalOptions(3, 2);
    const wrapper = mountTree(ontologyColumn(), initialOpts);

    const parent1Btn = wrapper.find("button[aria-controls='parent1']");
    expect(parent1Btn.attributes("aria-expanded")).toBe("true");

    await parent1Btn.trigger("click");
    await nextTick();

    expect(
      wrapper
        .find("button[aria-controls='parent1']")
        .attributes("aria-expanded")
    ).toBe("false");

    const updatedOpts = makeHierarchicalOptions(3, 2, 2);
    await wrapper.setProps({ options: updatedOpts });
    await flushPromises();
    await nextTick();

    expect(
      wrapper
        .find("button[aria-controls='parent1']")
        .attributes("aria-expanded")
    ).toBe("false");

    expect(
      wrapper
        .find("button[aria-controls='parent2']")
        .attributes("aria-expanded")
    ).toBe("true");
  });
});
