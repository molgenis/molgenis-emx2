import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { mount, flushPromises, VueWrapper } from "@vue/test-utils";
import { nextTick } from "vue";
import OntologyInput from "../../../../app/components/input/Ontology.vue";
import TreeNode from "../../../../app/components/input/TreeNode.vue";

const createMockObserver = () => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
  takeRecords: vi.fn(() => []),
});

global.IntersectionObserver = vi
  .fn()
  .mockImplementation(() => createMockObserver());

vi.mock("../../../../app/composables/fetchGraphql", () => ({
  default: vi.fn(),
}));

vi.mock("../../../../app/composables/useClickOutside", () => ({
  useClickOutside: vi.fn(),
}));

import fetchGraphql from "../../../../app/composables/fetchGraphql";

interface MockTerm {
  name: string;
  label: string;
  definition: string;
  code: string;
  codesystem: string;
  ontologyTermURI: string;
  children: { name: string }[];
}

interface LoadPageResponse {
  retrieveTerms: MockTerm[];
  count: { count: number };
  totalCount: { count: number };
}

const createMockTerms = (
  offset: number,
  limit: number,
  prefix = "Term"
): MockTerm[] => {
  return Array.from({ length: limit }, (_, i) => ({
    name: `${prefix}${offset + i + 1}`,
    label: `${prefix} ${offset + i + 1}`,
    definition: `Definition for ${prefix} ${offset + i + 1}`,
    code: `CODE${offset + i + 1}`,
    codesystem: "TEST",
    ontologyTermURI: `http://example.com/${prefix}${offset + i + 1}`,
    children: i % 3 === 0 ? [{ name: `Child${i}` }] : [],
  }));
};

const createLoadPageResponse = (
  terms: MockTerm[],
  count: number,
  totalCount?: number
): LoadPageResponse => ({
  retrieveTerms: terms,
  count: { count },
  totalCount: { count: totalCount ?? count },
});

const ID = "ontology-input";

const defaultProps = {
  id: ID,
  ontologySchemaId: "test-schema",
  ontologyTableId: "test-table",
  isArray: true,
};

type OntologyVM = {
  searchTerms: string;
  updateSearch: (v: string) => Promise<void>;
  loadMoreTerms: (node: Record<string, unknown>) => Promise<void>;
  rootNode: Record<string, unknown>;
};

function getNodeLabels(wrapper: VueWrapper): string[] {
  return wrapper
    .findAll("span.text-body-sm.leading-normal")
    .map((el) => el.text());
}

function findExpandButton(wrapper: VueWrapper, nodeName: string) {
  return wrapper.find(`button[aria-controls="${nodeName}"]`);
}

function findCheckbox(wrapper: VueWrapper, nodeName: string) {
  return wrapper.find(`#${ID}-${nodeName}-input`);
}

function findButtonByText(wrapper: VueWrapper, text: string) {
  const buttons = wrapper.findAll("button");
  return buttons.find((btn) => btn.text().includes(text)) ?? null;
}

function findMessageSpan(wrapper: VueWrapper) {
  return wrapper.findAll("span.text-body-sm.italic.text-input-description");
}

async function performSearch(wrapper: VueWrapper, value: string) {
  const vm = wrapper.vm as unknown as OntologyVM;
  const searchPromise = vm.updateSearch(value);
  vm.searchTerms = value;
  await searchPromise;
  await flushPromises();
}

async function waitForMacroTask() {
  await new Promise((resolve) => setTimeout(resolve, 10));
  await flushPromises();
  await nextTick();
}

async function openDropdown(wrapper: VueWrapper) {
  const caretDown = wrapper.find('[class*="caret-down"]');
  if (caretDown.exists()) {
    await caretDown.trigger("click");
    await nextTick();
  }
}

describe("OntologyInput", () => {
  let mockFetch: ReturnType<typeof vi.mocked<typeof fetchGraphql>>;

  beforeEach(() => {
    vi.clearAllMocks();
    global.IntersectionObserver = vi
      .fn()
      .mockImplementation(() => createMockObserver());
    mockFetch = vi.mocked(fetchGraphql);
  });

  afterEach(() => {
    vi.clearAllTimers();
    vi.restoreAllMocks();
    document.body.innerHTML = "";
  });

  describe("Small Ontology (< selectCutOff)", () => {
    it("should render all terms and auto-expand parents", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 15 },
        rootCount: { count: 5 },
      });

      mockFetch.mockResolvedValueOnce({
        allTerms: [
          { name: "Parent1", parent: null, label: "Parent 1" },
          { name: "Child1", parent: { name: "Parent1" }, label: "Child 1" },
          { name: "Parent2", parent: null, label: "Parent 2" },
        ],
      });

      const wrapper = mount(OntologyInput, { props: defaultProps });
      await flushPromises();

      const labels = getNodeLabels(wrapper);
      expect(labels).toContain("Parent 1");
      expect(labels).toContain("Child 1");
      expect(labels).toContain("Parent 2");

      const expandBtn = findExpandButton(wrapper, "Parent1");
      expect(expandBtn.exists()).toBe(true);
      expect(expandBtn.attributes("aria-expanded")).toBe("true");
    });

    it("should use paginated loading when forceList is true", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 15 },
        rootCount: { count: 15 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 5), 15)
      );

      const wrapper = mount(OntologyInput, {
        props: { ...defaultProps, forceList: true, limit: 5 },
      });
      await flushPromises();

      const labels = getNodeLabels(wrapper);
      expect(labels.length).toBe(5);

      const loadMoreBtn = findButtonByText(wrapper, "(load more)");
      expect(loadMoreBtn).not.toBeNull();
    });
  });

  describe("Large Ontology (>= selectCutOff)", () => {
    it("should render first page of terms", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 500 },
        rootCount: { count: 100 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, { props: defaultProps });
      await flushPromises();

      await openDropdown(wrapper);
      await flushPromises();

      const labels = getNodeLabels(wrapper);
      expect(labels.length).toBe(20);

      const messages = findMessageSpan(wrapper);
      const loadMsg = messages.find((m) => m.text().includes("more term"));
      expect(loadMsg).toBeDefined();
    });

    it("should load more terms when clicking load more", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 20 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, {
        props: { ...defaultProps, forceList: true },
      });
      await flushPromises();

      expect(getNodeLabels(wrapper).length).toBe(20);

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(20, 20), 100)
      );

      const loadMoreBtn = findButtonByText(wrapper, "(load more)");
      expect(loadMoreBtn).not.toBeNull();
      await loadMoreBtn!.trigger("click");
      await flushPromises();

      expect(getNodeLabels(wrapper).length).toBe(40);
    });

    it("should hide load more when all items loaded", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 25 },
        rootCount: { count: 25 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 20), 25)
      );

      const wrapper = mount(OntologyInput, {
        props: { ...defaultProps, forceList: true, limit: 20 },
      });
      await flushPromises();

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(20, 5), 25)
      );

      const loadMoreBtn = findButtonByText(wrapper, "(load more)");
      await loadMoreBtn!.trigger("click");
      await flushPromises();

      expect(getNodeLabels(wrapper).length).toBe(25);
      expect(findButtonByText(wrapper, "(load more)")).toBeNull();
    });
  });

  describe("Search Functionality", () => {
    it("should filter results after searching", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 100 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, {
        props: { ...defaultProps, forceList: true },
      });
      await flushPromises();

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 20, "Match"), 45, 100)
      );

      await performSearch(wrapper, "search term");

      const labels = getNodeLabels(wrapper);
      expect(labels.length).toBe(20);
      expect(labels[0]).toContain("Match");

      const loadMoreBtn = findButtonByText(wrapper, "(load more)");
      expect(loadMoreBtn).not.toBeNull();
    });

    it("should debounce search input and only search once", async () => {
      vi.useFakeTimers();

      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 100 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, {
        props: { ...defaultProps, forceList: true },
      });
      await flushPromises();

      const showBtn = findButtonByText(wrapper, "Show search");
      await showBtn!.trigger("click");
      await nextTick();

      const callCountBefore = mockFetch.mock.calls.length;

      const vm = wrapper.vm as unknown as OntologyVM;
      vm.searchTerms = "h";
      await nextTick();
      vm.searchTerms = "he";
      await nextTick();
      vm.searchTerms = "hea";
      await nextTick();
      vm.searchTerms = "heal";
      await nextTick();
      vm.searchTerms = "health";
      await nextTick();

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 10, "Health"), 10, 100)
      );

      vi.advanceTimersByTime(500);
      await flushPromises();

      const callCountAfter = mockFetch.mock.calls.length;
      expect(callCountAfter - callCountBefore).toBe(1);

      vi.useRealTimers();
    });

    it("should work in forceList mode with toggle search", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 100 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, {
        props: { ...defaultProps, forceList: true },
      });
      await flushPromises();

      expect(wrapper.find(`#${ID}-search-list`).exists()).toBe(false);

      const showBtn = findButtonByText(wrapper, "Show search");
      expect(showBtn).not.toBeNull();
      await showBtn!.trigger("click");
      await nextTick();

      expect(wrapper.find(`#${ID}-search-list`).exists()).toBe(true);

      const hideBtn = findButtonByText(wrapper, "Hide search");
      expect(hideBtn).not.toBeNull();
      await hideBtn!.trigger("click");
      await nextTick();

      expect(wrapper.find(`#${ID}-search-list`).exists()).toBe(false);
    });

    it("should show (show filtered) when expanding node with hidden children", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 100 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, {
        props: { ...defaultProps, forceList: true },
      });
      await flushPromises();

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 5, "Match"), 5, 100)
      );
      await performSearch(wrapper, "health");

      const expandBtn = findExpandButton(wrapper, "Match1");
      expect(expandBtn.exists()).toBe(true);

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 5, "ChildMatch"), 5, 15)
      );

      await expandBtn.trigger("click");
      await flushPromises();

      expect(expandBtn.attributes("aria-expanded")).toBe("true");

      const messages = findMessageSpan(wrapper);
      const filterMsg = messages.find((m) =>
        m.text().includes("hidden by filter")
      );
      expect(filterMsg).toBeDefined();

      const showFilteredBtn = findButtonByText(wrapper, "(show filtered)");
      expect(showFilteredBtn).not.toBeNull();
    });

    it("should show message when all children hidden by filter", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 100 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, {
        props: { ...defaultProps, forceList: true },
      });
      await flushPromises();

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 5, "Match"), 5, 100)
      );
      await performSearch(wrapper, "health");

      mockFetch.mockResolvedValueOnce(createLoadPageResponse([], 0, 10));

      const expandBtn = findExpandButton(wrapper, "Match1");
      expect(expandBtn.exists()).toBe(true);
      await expandBtn.trigger("click");
      await flushPromises();

      const messages = findMessageSpan(wrapper);
      const hiddenMsg = messages.find(
        (m) =>
          m.text().includes("hidden by filter") ||
          m.text().includes("All children hidden")
      );
      expect(hiddenMsg).toBeDefined();
    });

    it("should bypass filter when clicking (show filtered)", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 100 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, {
        props: { ...defaultProps, forceList: true },
      });
      await flushPromises();

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 5, "Match"), 5, 100)
      );
      await performSearch(wrapper, "health");

      mockFetch.mockResolvedValueOnce(createLoadPageResponse([], 0, 10));

      const expandBtn = findExpandButton(wrapper, "Match1");
      expect(expandBtn.exists()).toBe(true);
      await expandBtn.trigger("click");
      await flushPromises();

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 10, "AllChild"), 10, 10)
      );

      const showFilteredBtn = findButtonByText(wrapper, "(show filtered)");
      expect(showFilteredBtn).not.toBeNull();
      await showFilteredBtn!.trigger("click");
      await waitForMacroTask();

      const childLabels = getNodeLabels(wrapper);
      expect(childLabels.some((l) => l.includes("AllChild"))).toBe(true);

      const applyFilterBtn = findButtonByText(wrapper, "(apply filter)");
      expect(applyFilterBtn).not.toBeNull();
    });

    it("should reapply filter when clicking (apply filter)", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 100 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, {
        props: { ...defaultProps, forceList: true },
      });
      await flushPromises();

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 5, "Match"), 5, 100)
      );
      await performSearch(wrapper, "health");

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 5, "ChildMatch"), 5, 25)
      );

      const expandBtn = findExpandButton(wrapper, "Match1");
      expect(expandBtn.exists()).toBe(true);
      await expandBtn.trigger("click");
      await flushPromises();

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 25, "AllChild"), 25, 25)
      );

      const showFilteredBtn = findButtonByText(wrapper, "(show filtered)");
      expect(showFilteredBtn).not.toBeNull();
      await showFilteredBtn!.trigger("click");
      await waitForMacroTask();

      const allLabels = getNodeLabels(wrapper);
      expect(allLabels.some((l) => l.includes("AllChild"))).toBe(true);

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 5, "ChildMatch"), 5, 25)
      );

      const applyFilterBtn = findButtonByText(wrapper, "(apply filter)");
      expect(applyFilterBtn).not.toBeNull();
      await applyFilterBtn!.trigger("click");
      await waitForMacroTask();

      expect(findButtonByText(wrapper, "(apply filter)")).toBeNull();
      expect(findButtonByText(wrapper, "(show filtered)")).not.toBeNull();
    });

    it("should not add (show filtered) when all children match filter", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 100 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, {
        props: { ...defaultProps, forceList: true },
      });
      await flushPromises();

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 5, "Match"), 5, 100)
      );
      await performSearch(wrapper, "health");

      const showFilteredCountBefore = wrapper
        .findAll("button")
        .filter((b) => b.text().includes("(show filtered)")).length;

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 15, "ChildMatch"), 15, 15)
      );

      const expandBtn = findExpandButton(wrapper, "Match1");
      expect(expandBtn.exists()).toBe(true);
      await expandBtn.trigger("click");
      await flushPromises();

      const showFilteredCountAfter = wrapper
        .findAll("button")
        .filter((b) => b.text().includes("(show filtered)")).length;

      expect(showFilteredCountAfter).toBe(showFilteredCountBefore);
    });

    it("should show combined load more and hidden message", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 100 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, {
        props: { ...defaultProps, forceList: true, limit: 5 },
      });
      await flushPromises();

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 5, "Match"), 5, 100)
      );
      await performSearch(wrapper, "health");

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 5, "ChildMatch"), 15, 25)
      );

      const expandBtn = findExpandButton(wrapper, "Match1");
      expect(expandBtn.exists()).toBe(true);
      await expandBtn.trigger("click");
      await flushPromises();

      const messages = findMessageSpan(wrapper);
      const combinedMsg = messages.find(
        (m) =>
          m.text().includes("more") && m.text().includes("hidden by filter")
      );
      expect(combinedMsg).toBeDefined();

      expect(findButtonByText(wrapper, "(load more)")).not.toBeNull();
      expect(findButtonByText(wrapper, "(show filtered)")).not.toBeNull();
    });

    it("should show apply filter message with correct filtered count", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 100 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, {
        props: { ...defaultProps, forceList: true },
      });
      await flushPromises();

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 5, "Match"), 5, 100)
      );
      await performSearch(wrapper, "health");

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 7, "ChildMatch"), 7, 20)
      );

      const expandBtn = findExpandButton(wrapper, "Match1");
      expect(expandBtn.exists()).toBe(true);
      await expandBtn.trigger("click");
      await flushPromises();

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 20, "AllChild"), 20, 20)
      );

      const showFilteredBtn = findButtonByText(wrapper, "(show filtered)");
      expect(showFilteredBtn).not.toBeNull();
      await showFilteredBtn!.trigger("click");
      await waitForMacroTask();

      const messages = findMessageSpan(wrapper);
      const applyMsg = messages.find((m) => m.text().includes("match filter"));
      expect(applyMsg).toBeDefined();
      expect(applyMsg!.text()).toContain("7");
    });
  });

  describe("Child Node Expansion", () => {
    it("should render children when expanding a node", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 20 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 5), 20)
      );

      const wrapper = mount(OntologyInput, {
        props: { ...defaultProps, forceList: true, limit: 10 },
      });
      await flushPromises();

      const expandBtn = findExpandButton(wrapper, "Term1");
      expect(expandBtn.exists()).toBe(true);

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(100, 10, "Child"), 50)
      );

      await expandBtn.trigger("click");
      await flushPromises();

      expect(expandBtn.attributes("aria-expanded")).toBe("true");

      const labels = getNodeLabels(wrapper);
      expect(labels.some((l) => l.includes("Child"))).toBe(true);

      const messages = findMessageSpan(wrapper);
      const moreMsg = messages.find((m) => m.text().includes("more"));
      expect(moreMsg).toBeDefined();
    });

    it("should show hidden count when expanding during search", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 20 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 5), 20)
      );

      const wrapper = mount(OntologyInput, {
        props: { ...defaultProps, forceList: true },
      });
      await flushPromises();

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 5, "Match"), 10, 20)
      );
      await performSearch(wrapper, "health");

      const expandBtn = findExpandButton(wrapper, "Match1");
      expect(expandBtn.exists()).toBe(true);

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(100, 5, "ChildMatch"), 5, 50)
      );

      await expandBtn.trigger("click");
      await flushPromises();

      const messages = findMessageSpan(wrapper);
      const filterMsg = messages.find((m) =>
        m.text().includes("hidden by filter")
      );
      expect(filterMsg).toBeDefined();
    });
  });

  describe("Selection Behavior", () => {
    it("should emit selected values when clicking checkboxes", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 50 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 20), 50)
      );

      const wrapper = mount(OntologyInput, {
        props: { ...defaultProps, forceList: true, modelValue: [] },
      });
      await flushPromises();

      const checkbox = findCheckbox(wrapper, "Term1");
      expect(checkbox.exists()).toBe(true);

      await checkbox.trigger("click");
      await flushPromises();

      const emissions = wrapper.emitted("update:modelValue");
      expect(emissions).toBeTruthy();
      const lastValue = emissions![emissions!.length - 1][0] as string[];
      expect(lastValue).toContain("Term1");
    });

    it("should maintain selection after loading more items", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 50 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 20), 50)
      );

      const wrapper = mount(OntologyInput, {
        props: { ...defaultProps, forceList: true, modelValue: [] },
      });
      await flushPromises();

      const checkbox1 = findCheckbox(wrapper, "Term1");
      await checkbox1.trigger("click");
      await flushPromises();

      expect((checkbox1.element as HTMLInputElement).checked).toBe(true);

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(20, 20), 50)
      );

      const loadMoreBtn = findButtonByText(wrapper, "(load more)");
      await loadMoreBtn!.trigger("click");
      await flushPromises();

      const checkbox1After = findCheckbox(wrapper, "Term1");
      expect((checkbox1After.element as HTMLInputElement).checked).toBe(true);

      const checkbox21 = findCheckbox(wrapper, "Term21");
      await checkbox21.trigger("click");
      await flushPromises();

      const emissions = wrapper.emitted("update:modelValue");
      const lastValue = emissions![emissions!.length - 1][0] as string[];
      expect(lastValue).toContain("Term1");
      expect(lastValue).toContain("Term21");
    });

    it("should not auto-select parent when children not fully loaded", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 20 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 5), 20)
      );

      const wrapper = mount(OntologyInput, {
        props: { ...defaultProps, forceList: true, modelValue: [], limit: 5 },
      });
      await flushPromises();

      const expandBtn = findExpandButton(wrapper, "Term1");
      expect(expandBtn.exists()).toBe(true);

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(100, 5, "Child"), 20)
      );

      await expandBtn.trigger("click");
      await flushPromises();

      for (let idx = 1; idx <= 5; idx++) {
        const childCheckbox = findCheckbox(wrapper, `Child${100 + idx}`);
        if (childCheckbox.exists()) {
          await childCheckbox.trigger("click");
          await flushPromises();
        }
      }

      const parentCheckbox = findCheckbox(wrapper, "Term1");
      expect((parentCheckbox.element as HTMLInputElement).checked).toBe(false);
    });

    it("should not auto-select parent when search filter hides children", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 20 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 5), 20)
      );

      const wrapper = mount(OntologyInput, {
        props: {
          ...defaultProps,
          forceList: true,
          modelValue: [],
          limit: 20,
        },
      });
      await flushPromises();

      const vm = wrapper.vm as unknown as OntologyVM;
      vm.searchTerms = "active-search";
      await nextTick();

      const expandBtn = findExpandButton(wrapper, "Term1");
      expect(expandBtn.exists()).toBe(true);

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(100, 3, "Child"), 3)
      );

      await expandBtn.trigger("click");
      await flushPromises();

      for (let idx = 1; idx <= 3; idx++) {
        const childCheckbox = findCheckbox(wrapper, `Child${100 + idx}`);
        if (childCheckbox.exists()) {
          await childCheckbox.trigger("click");
          await flushPromises();
        }
      }

      const parentCheckbox = findCheckbox(wrapper, "Term1");
      expect((parentCheckbox.element as HTMLInputElement).checked).toBe(false);

      const emissions = wrapper.emitted("update:modelValue");
      const lastValue = emissions![emissions!.length - 1][0] as string[];
      expect(lastValue).toContain("Child101");
      expect(lastValue).toContain("Child102");
      expect(lastValue).toContain("Child103");
      expect(lastValue).not.toContain("Term1");
    });

    it("should select all children when all are loaded", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 20 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 5), 20)
      );

      const wrapper = mount(OntologyInput, {
        props: { ...defaultProps, forceList: true, modelValue: [], limit: 5 },
      });
      await flushPromises();

      const expandBtn = findExpandButton(wrapper, "Term1");
      expect(expandBtn.exists()).toBe(true);

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(100, 3, "Child"), 3)
      );

      await expandBtn.trigger("click");
      await flushPromises();

      for (let idx = 1; idx <= 3; idx++) {
        const childCheckbox = findCheckbox(wrapper, `Child${100 + idx}`);
        if (childCheckbox.exists()) {
          await childCheckbox.trigger("click");
          await flushPromises();
        }
      }

      const emissions = wrapper.emitted("update:modelValue");
      const lastValue = emissions![emissions!.length - 1][0] as string[];
      expect(lastValue).toContain("Child101");
      expect(lastValue).toContain("Child102");
      expect(lastValue).toContain("Child103");
    });
  });

  describe("IntersectionObserver and forceList", () => {
    it("should not enable auto-load when forceList is true", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 20 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 5), 20)
      );

      const wrapper = mount(OntologyInput, {
        props: { ...defaultProps, forceList: true },
      });
      await flushPromises();

      const treeNode = wrapper.findComponent(TreeNode);
      expect(treeNode.exists()).toBe(true);
      expect(treeNode.props("enableAutoLoad")).toBe(false);
    });
  });

  describe("Prevent Duplicate Loads", () => {
    it("should only fetch once for simultaneous load more calls", async () => {
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 100 },
      });

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, {
        props: { ...defaultProps, forceList: true },
      });
      await flushPromises();

      mockFetch.mockResolvedValueOnce(
        createLoadPageResponse(createMockTerms(20, 20), 100)
      );

      const callCountBefore = mockFetch.mock.calls.length;
      const vm = wrapper.vm as unknown as OntologyVM;

      const promise1 = vm.loadMoreTerms(vm.rootNode);
      const promise2 = vm.loadMoreTerms(vm.rootNode);

      await Promise.all([promise1, promise2]);
      await flushPromises();

      expect(getNodeLabels(wrapper).length).toBe(40);
      expect(mockFetch.mock.calls.length - callCountBefore).toBe(1);
    });
  });
});
