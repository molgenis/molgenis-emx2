import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { nextTick } from 'vue';
import OntologyInput from '../../../../app/components/input/Ontology.vue';

// Setup IntersectionObserver mock BEFORE any imports
const createMockObserver = () => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
  takeRecords: vi.fn(() => []),
});

// @ts-ignore
global.IntersectionObserver = vi.fn().mockImplementation(() => createMockObserver());

// Mock the fetchGraphql function BEFORE importing it
vi.mock('../../../../app/composables/fetchGraphql', () => ({
  default: vi.fn(),
}));

// Mock the useClickOutside composable
vi.mock('../../../../app/composables/useClickOutside', () => ({
  useClickOutside: vi.fn((elementRef, callback) => {
    const handler = (event: MouseEvent) => {
      const el = elementRef.value;
      if (!el) return;
      const targetElement = (el as any).$el || el;
      if (targetElement && typeof targetElement.contains === 'function') {
        if (!targetElement.contains(event.target as Node)) {
          callback();
        }
      }
    };
    setTimeout(() => {
      document.addEventListener('mousedown', handler);
    }, 0);
    return () => {
      document.removeEventListener('mousedown', handler);
    };
  }),
}));

// Import the mocked function AFTER setting up the mock
import fetchGraphql from '../../../../app/composables/fetchGraphql';

// Test data helpers
const mockOntologyData = {
  totalCount: { count: 100 },
  rootCount: { count: 20 },
};

const createMockTerms = (offset: number, limit: number, prefix = 'Term') => {
  return Array.from({ length: limit }, (_, i) => ({
    name: `${prefix}${offset + i + 1}`,
    label: `${prefix} ${offset + i + 1}`,
    definition: `Definition for ${prefix} ${offset + i + 1}`,
    code: `CODE${offset + i + 1}`,
    codesystem: 'TEST',
    ontologyTermURI: `http://example.com/${prefix}${offset + i + 1}`,
    children: i % 3 === 0 ? [{ name: `Child${i}` }] : [],
  }));
};

// Helper to create mock response for loadPage
const createMockLoadPageResponse = (terms: any[], count: number, totalCount?: number) => ({
  retrieveTerms: terms,
  count: { count },
  totalCount: { count: totalCount ?? count },
});

describe('OntologyInput - Unified loadPage Architecture', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // @ts-ignore
    global.IntersectionObserver = vi.fn().mockImplementation(() => createMockObserver());
    if (!document.body) {
      document.body = document.createElement('body');
    }
  });

  afterEach(() => {
    vi.restoreAllMocks();
    document.body.innerHTML = '';
  });

  describe('Small Ontology (< selectCutOff)', () => {
    it('should load entire small ontology and auto-expand', async () => {
      const mockFetch = vi.mocked(fetchGraphql);

      // Initial counts (total < 25 = small ontology)
      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 15 },
        rootCount: { count: 5 },
      });

      // Load all terms at once for small ontology
      mockFetch.mockResolvedValueOnce({
        allTerms: [
          { name: 'Parent1', parent: null, label: 'Parent 1' },
          { name: 'Child1', parent: { name: 'Parent1' }, label: 'Child 1' },
          { name: 'Parent2', parent: null, label: 'Parent 2' },
        ],
      });

      const wrapper = mount(OntologyInput, {
        props: {
          ontologySchemaId: 'test-schema',
          ontologyTableId: 'test-table',
          isArray: true,
        },
      });

      await flushPromises();

      const rootNode = (wrapper.vm as any).rootNode;
      expect(rootNode.children.length).toBe(2); // 2 root parents
      expect(rootNode.children[0].children.length).toBe(1); // Child1 under Parent1
      expect(rootNode.children[0].expanded).toBe(true); // Auto-expanded
    });

    it('should use paginated loading when forceList is true even for small ontology', async () => {
      const mockFetch = vi.mocked(fetchGraphql);

      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 15 },
        rootCount: { count: 15 },
      });

      // First page only
      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 5), 15)
      );

      const wrapper = mount(OntologyInput, {
        props: {
          ontologySchemaId: 'test-schema',
          ontologyTableId: 'test-table',
          isArray: true,
          forceList: true,
          limit: 5,
        },
      });

      await flushPromises();

      const rootNode = (wrapper.vm as any).rootNode;
      expect(rootNode.children.length).toBe(5); // First page only
      expect(rootNode.loadMoreHasMore).toBe(true); // Has more to load
    });
  });

  describe('Large Ontology (>= selectCutOff)', () => {
    it('should load first page only for large ontology', async () => {
      const mockFetch = vi.mocked(fetchGraphql);

      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 500 },
        rootCount: { count: 100 },
      });

      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, {
        props: {
          ontologySchemaId: 'test-schema',
          ontologyTableId: 'test-table',
          isArray: true,
        },
      });

      await flushPromises();

      const rootNode = (wrapper.vm as any).rootNode;
      expect(rootNode.children.length).toBe(20);
      expect(rootNode.loadMoreOffset).toBe(20);
      expect(rootNode.loadMoreTotal).toBe(100);
      expect(rootNode.loadMoreHasMore).toBe(true);
    });

    it('should load more terms when clicking load more', async () => {
      const mockFetch = vi.mocked(fetchGraphql);

      mockFetch.mockResolvedValueOnce({
        ...mockOntologyData,
      });

      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, {
        props: {
          ontologySchemaId: 'test-schema',
          ontologyTableId: 'test-table',
          isArray: true,
        },
      });

      await flushPromises();

      // Load more
      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(20, 20), 100)
      );

      const rootNode = (wrapper.vm as any).rootNode;
      await (wrapper.vm as any).loadMoreTerms(rootNode);
      await flushPromises();

      expect(rootNode.children.length).toBe(40);
      expect(rootNode.loadMoreOffset).toBe(40);
    });

    it('should stop loading when reaching end', async () => {
      const mockFetch = vi.mocked(fetchGraphql);

      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 25 },
        rootCount: { count: 25 },
      });

      // First page
      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 20), 25)
      );

      const wrapper = mount(OntologyInput, {
        props: {
          ontologySchemaId: 'test-schema',
          ontologyTableId: 'test-table',
          isArray: true,
          limit: 20,
        },
      });

      await flushPromises();

      // Load remaining 5 items (partial batch)
      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(20, 5), 25)
      );

      const rootNode = (wrapper.vm as any).rootNode;
      await (wrapper.vm as any).loadMoreTerms(rootNode);
      await flushPromises();

      expect(rootNode.children.length).toBe(25);
      expect(rootNode.loadMoreHasMore).toBe(false); // No more to load
    });
  });

  describe('Search Functionality', () => {
    it('should search and load first page of results', async () => {
      const mockFetch = vi.mocked(fetchGraphql);

      mockFetch.mockResolvedValueOnce({
        ...mockOntologyData,
      });

      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, {
        props: {
          ontologySchemaId: 'test-schema',
          ontologyTableId: 'test-table',
          isArray: true,
        },
      });

      await flushPromises();

      // Search returns filtered results (20 out of 45 total)
      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 20, 'Match'), 45, 100)
      );

      await (wrapper.vm as any).updateSearch('search term');
      await flushPromises();

      const rootNode = (wrapper.vm as any).rootNode;
      expect(rootNode.children.length).toBe(20);
      expect(rootNode.loadMoreTotal).toBe(45); // Filtered count
      expect(rootNode.loadMoreHasMore).toBe(true); // 20 < 45, so has more
    });

    it('should clear search and reload first page', async () => {
      const mockFetch = vi.mocked(fetchGraphql);

      mockFetch.mockResolvedValueOnce({
        ...mockOntologyData,
      });

      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, {
        props: {
          ontologySchemaId: 'test-schema',
          ontologyTableId: 'test-table',
          isArray: true,
        },
      });

      await flushPromises();

      // Search
      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 5, 'Match'), 5, 100)
      );
      await (wrapper.vm as any).updateSearch('test');
      await flushPromises();

      // Clear search
      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 20), 100)
      );
      await (wrapper.vm as any).updateSearch('');
      await flushPromises();

      const rootNode = (wrapper.vm as any).rootNode;
      expect(rootNode.children.length).toBe(20);
      expect(rootNode.loadMoreTotal).toBe(100);
    });

    it('should debounce search input and only search once', async () => {
      vi.useFakeTimers();
      const mockFetch = vi.mocked(fetchGraphql);

      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 100 },
      });

      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, {
        props: {
          ontologySchemaId: 'test-schema',
          ontologyTableId: 'test-table',
          isArray: true,
        },
      });

      await flushPromises();

      // Open dropdown so search watcher isn't blocked
      (wrapper.vm as any).showSelect = true;
      await nextTick();

      const callCountBefore = mockFetch.mock.calls.length;

      // Simulate rapid typing
      (wrapper.vm as any).searchTerms = 'h';
      await nextTick();
      (wrapper.vm as any).searchTerms = 'he';
      await nextTick();
      (wrapper.vm as any).searchTerms = 'hea';
      await nextTick();
      (wrapper.vm as any).searchTerms = 'heal';
      await nextTick();
      (wrapper.vm as any).searchTerms = 'health';
      await nextTick();

      // Mock search response
      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 10, 'Health'), 10, 100)
      );

      // Fast-forward past debounce delay
      vi.advanceTimersByTime(500);
      await flushPromises();

      // Should only have made ONE search call (debounced)
      const callCountAfter = mockFetch.mock.calls.length;
      expect(callCountAfter - callCountBefore).toBe(1);

      vi.useRealTimers();
    });

    it('should maintain search context when loading more results', async () => {
      const mockFetch = vi.mocked(fetchGraphql);

      mockFetch.mockResolvedValueOnce({
        ...mockOntologyData,
      });

      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, {
        props: {
          ontologySchemaId: 'test-schema',
          ontologyTableId: 'test-table',
          isArray: true,
          limit: 10,
        },
      });

      await flushPromises();

      // Search returns first page
      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 10, 'Match'), 45, 100)
      );

      (wrapper.vm as any).searchTerms = 'health';
      await (wrapper.vm as any).updateSearch('health');
      await flushPromises();

      const rootNode = (wrapper.vm as any).rootNode;
      expect(rootNode.children.length).toBe(10);

      // Load more should maintain search context
      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(10, 10, 'Match'), 45, 100)
      );

      await (wrapper.vm as any).loadMoreTerms(rootNode);
      await flushPromises();

      expect(rootNode.children.length).toBe(20);
      expect(rootNode.loadMoreTotal).toBe(45); // Still filtered count
    });

    it('should work in forceList mode with toggle search', async () => {
      const mockFetch = vi.mocked(fetchGraphql);

      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 100 },
      });

      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, {
        props: {
          ontologySchemaId: 'test-schema',
          ontologyTableId: 'test-table',
          isArray: true,
          forceList: true, // Force list mode
        },
      });

      await flushPromises();

      // Initially showSearch should be false
      expect((wrapper.vm as any).showSearch).toBe(false);

      // Toggle search on
      await (wrapper.vm as any).toggleSearch();
      expect((wrapper.vm as any).showSearch).toBe(true);

      // Perform search
      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 15, 'Match'), 15, 100)
      );

      (wrapper.vm as any).searchTerms = 'test';
      await (wrapper.vm as any).updateSearch('test');
      await flushPromises();

      const rootNode = (wrapper.vm as any).rootNode;
      expect(rootNode.children.length).toBe(15);
      expect(rootNode.loadMoreTotal).toBe(15);

      // Toggle search off should clear search
      await (wrapper.vm as any).toggleSearch();
      expect((wrapper.vm as any).showSearch).toBe(false);
      expect((wrapper.vm as any).searchTerms).toBe('');
    });

    it('should show search results summary', async () => {
      const mockFetch = vi.mocked(fetchGraphql);

      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 100 },
      });

      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, {
        props: {
          ontologySchemaId: 'test-schema',
          ontologyTableId: 'test-table',
          isArray: true,
          limit: 10, // Use limit of 10 so hasMore will be true
        },
      });

      await flushPromises();

      // No summary when not searching
      expect((wrapper.vm as any).searchResultsSummary).toBeNull();

      // Set searchTerms first (required for summary to show)
      (wrapper.vm as any).searchTerms = 'health';
      await nextTick();

      // Perform search - return exactly 10 items (full batch) with 45 total
      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 10, 'Match'), 45, 100)
      );

      await (wrapper.vm as any).updateSearch('health');
      await flushPromises();

      // Should show summary
      const summary = (wrapper.vm as any).searchResultsSummary;
      expect(summary).not.toBeNull();
      expect(summary.loaded).toBe(10);
      expect(summary.total).toBe(45);
      expect(summary.hasMore).toBe(true); // 10 items loaded, 45 total, limit 10
      expect(summary.showingAll).toBe(false);
    });
  });

  describe('Child Node Expansion', () => {
    it('should load children when expanding node', async () => {
      const mockFetch = vi.mocked(fetchGraphql);

      mockFetch.mockResolvedValueOnce({
        ...mockOntologyData,
      });

      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 5), 20)
      );

      const wrapper = mount(OntologyInput, {
        props: {
          ontologySchemaId: 'test-schema',
          ontologyTableId: 'test-table',
          isArray: true,
          limit: 10,
        },
      });

      await flushPromises();

      const nodeToExpand = (wrapper.vm as any).rootNode.children[0];

      // Load children
      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(100, 10, 'Child'), 50)
      );

      await (wrapper.vm as any).toggleTermExpand(nodeToExpand);
      await flushPromises();

      expect(nodeToExpand.expanded).toBe(true);
      expect(nodeToExpand.children.length).toBe(10);
      expect(nodeToExpand.loadMoreTotal).toBe(50);
      expect(nodeToExpand.loadMoreHasMore).toBe(true);
    });

    it('should show exact hidden count when expanding during search', async () => {
      const mockFetch = vi.mocked(fetchGraphql);

      mockFetch.mockResolvedValueOnce({
        ...mockOntologyData,
      });

      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 5), 20)
      );

      const wrapper = mount(OntologyInput, {
        props: {
          ontologySchemaId: 'test-schema',
          ontologyTableId: 'test-table',
          isArray: true,
        },
      });

      await flushPromises();

      // Search
      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 5, 'Match'), 10, 20)
      );
      await (wrapper.vm as any).updateSearch('health');
      await flushPromises();

      const nodeToExpand = (wrapper.vm as any).rootNode.children[0];

      // Expand node - 5 matching out of 50 total children
      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(100, 5, 'ChildMatch'), 5, 50)
      );

      await (wrapper.vm as any).toggleTermExpand(nodeToExpand);
      await flushPromises();

      expect(nodeToExpand.loadMoreTotal).toBe(5); // Filtered count
      expect((nodeToExpand as any).unfilteredTotal).toBe(50); // Total count
      // Hidden count = 50 - 5 = 45
    });

    it('should support "show all" to bypass search filter', async () => {
      const mockFetch = vi.mocked(fetchGraphql);

      mockFetch.mockResolvedValueOnce({
        ...mockOntologyData,
      });

      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 5), 20)
      );

      const wrapper = mount(OntologyInput, {
        props: {
          ontologySchemaId: 'test-schema',
          ontologyTableId: 'test-table',
          isArray: true,
        },
      });

      await flushPromises();

      // Search
      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 5, 'Match'), 10, 20)
      );
      await (wrapper.vm as any).updateSearch('health');
      await flushPromises();

      const nodeToExpand = (wrapper.vm as any).rootNode.children[0];

      // Show all (bypass filter) - loads all 50 children
      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(100, 10, 'Child'), 50)
      );

      await (wrapper.vm as any).toggleTermExpand(nodeToExpand, true); // showAll = true
      await flushPromises();

      expect((nodeToExpand as any).showingAll).toBe(true);
      expect(nodeToExpand.children.length).toBe(10);
      expect(nodeToExpand.loadMoreTotal).toBe(50); // All children, not filtered
    });
  });

  describe('Selection Behavior', () => {
    it('should apply selection states when loading more items', async () => {
      const mockFetch = vi.mocked(fetchGraphql);

      mockFetch.mockResolvedValueOnce({
        totalCount: { count: 100 },
        rootCount: { count: 50 },
      });

      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 20), 50)
      );

      const wrapper = mount(OntologyInput, {
        props: {
          ontologySchemaId: 'test-schema',
          ontologyTableId: 'test-table',
          isArray: true,
          modelValue: [],
        },
      });

      await flushPromises();
      await nextTick();

      const rootNode = (wrapper.vm as any).rootNode;

      // Select Term1 manually
      await (wrapper.vm as any).toggleTermSelect(rootNode.children[0]);
      await flushPromises();

      // Verify Term1 is selected
      expect(rootNode.children[0].selected).toBe('selected');

      // Load more
      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(20, 20), 50)
      );

      await (wrapper.vm as any).loadMoreTerms(rootNode);
      await flushPromises();
      await nextTick();

      // Term1 should still be selected after loading more
      expect(rootNode.children[0].selected).toBe('selected');

      // Now select Term21 (first item in second batch)
      await (wrapper.vm as any).toggleTermSelect(rootNode.children[20]);
      await flushPromises();

      // Both should be selected
      expect(rootNode.children[0].selected).toBe('selected');
      expect(rootNode.children[20].selected).toBe('selected');
    });

    it('should not auto-select parent when children not fully loaded', async () => {
      const mockFetch = vi.mocked(fetchGraphql);

      mockFetch.mockResolvedValueOnce({
        ...mockOntologyData,
      });

      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 5), 20)
      );

      const wrapper = mount(OntologyInput, {
        props: {
          ontologySchemaId: 'test-schema',
          ontologyTableId: 'test-table',
          isArray: true,
          modelValue: [],
          limit: 5,
        },
      });

      await flushPromises();

      const parentNode = (wrapper.vm as any).rootNode.children[0];

      // Expand parent - has 20 children total, only loads 5
      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(100, 5, 'Child'), 20)
      );

      await (wrapper.vm as any).toggleTermExpand(parentNode);
      await flushPromises();

      // Select all 5 visible children
      for (const child of parentNode.children) {
        await (wrapper.vm as any).toggleTermSelect(child);
      }

      // Parent should NOT be selected because loadMoreHasMore = true (15 more children not loaded)
      expect(parentNode.selected).not.toBe('selected');
      expect(parentNode.loadMoreHasMore).toBe(true);
    });

    it('should keep all children selected when loadMoreHasMore is false', async () => {
      const mockFetch = vi.mocked(fetchGraphql);

      mockFetch.mockResolvedValueOnce({
        ...mockOntologyData,
      });

      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 5), 20)
      );

      const wrapper = mount(OntologyInput, {
        props: {
          ontologySchemaId: 'test-schema',
          ontologyTableId: 'test-table',
          isArray: true,
          modelValue: [],
          limit: 5,
        },
      });

      await flushPromises();

      const parentNode = (wrapper.vm as any).rootNode.children[0];

      // Expand parent - only 3 children total (all loaded in one page)
      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(100, 3, 'Child'), 3)
      );

      await (wrapper.vm as any).toggleTermExpand(parentNode);
      await flushPromises();

      expect(parentNode.loadMoreHasMore).toBe(false); // All children loaded
      expect(parentNode.children.length).toBe(3);

      // Select all 3 children one by one
      await (wrapper.vm as any).toggleTermSelect(parentNode.children[0]);
      await (wrapper.vm as any).toggleTermSelect(parentNode.children[1]);
      await (wrapper.vm as any).toggleTermSelect(parentNode.children[2]);
      await flushPromises();

      // Check the final modelValue
      const emissions = wrapper.emitted('update:modelValue');
      const lastValue = emissions?.[emissions.length - 1]?.[0] as string[];

      // All children should be selected
      expect(lastValue).toContain('Child101');
      expect(lastValue).toContain('Child102');
      expect(lastValue).toContain('Child103');
      expect(lastValue.length).toBe(3);

      // Verify loadMoreHasMore is false (all children are loaded)
      expect(parentNode.loadMoreHasMore).toBe(false);
    });
  });

  describe('IntersectionObserver and forceList', () => {
    it('should NOT enable auto-load when forceList is true', async () => {
      const mockFetch = vi.mocked(fetchGraphql);

      mockFetch.mockResolvedValueOnce({
        ...mockOntologyData,
      });

      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 5), 20)
      );

      const wrapper = mount(OntologyInput, {
        props: {
          ontologySchemaId: 'test-schema',
          ontologyTableId: 'test-table',
          isArray: true,
          forceList: true,
        },
      });

      await flushPromises();

      // enableAutoLoad should be false
      expect((wrapper.vm as any).enableAutoLoad).toBe(false);
    });
  });

  describe('Prevent Duplicate Loads', () => {
    it('should prevent simultaneous load more calls on same node', async () => {
      const mockFetch = vi.mocked(fetchGraphql);

      mockFetch.mockResolvedValueOnce({
        ...mockOntologyData,
      });

      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(0, 20), 100)
      );

      const wrapper = mount(OntologyInput, {
        props: {
          ontologySchemaId: 'test-schema',
          ontologyTableId: 'test-table',
          isArray: true,
        },
      });

      await flushPromises();

      // Mock second page (only once)
      mockFetch.mockResolvedValueOnce(
          createMockLoadPageResponse(createMockTerms(20, 20), 100)
      );

      const rootNode = (wrapper.vm as any).rootNode;

      // Call loadMore twice simultaneously
      const promise1 = (wrapper.vm as any).loadMoreTerms(rootNode);
      const promise2 = (wrapper.vm as any).loadMoreTerms(rootNode);

      await Promise.all([promise1, promise2]);
      await flushPromises();

      // Should only load once (40 items, not 60)
      expect(rootNode.children.length).toBe(40);
      // mockFetch called 3 times: counts + first page + second page (NOT duplicate)
      expect(mockFetch).toHaveBeenCalledTimes(3);
    });
  });
});