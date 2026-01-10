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

// Mock the useClickOutside composable to actually set up click listeners
vi.mock('../../../../app/composables/useClickOutside', () => ({
  useClickOutside: vi.fn((elementRef, callback) => {
    // Set up actual click outside listener in tests
    const handler = (event: MouseEvent) => {
      const el = elementRef.value;
      if (!el) return;

      // Check if el is a Vue component instance (has $el property)
      const targetElement = (el as any).$el || el;

      if (targetElement && typeof targetElement.contains === 'function') {
        if (!targetElement.contains(event.target as Node)) {
          callback();
        }
      }
    };

    // Add listener on mount
    setTimeout(() => {
      document.addEventListener('mousedown', handler);
    }, 0);

    // Return cleanup function
    return () => {
      document.removeEventListener('mousedown', handler);
    };
  }),
}));

// Import the mocked function AFTER setting up the mock
import fetchGraphql from '../../../../app/composables/fetchGraphql';

// Shared test data and utilities
const mockOntologyData = {
  totalCount: { count: 100 },
  rootCount: { count: 50 },
};

const createMockTerms = (offset: number, limit: number, prefix = 'Term') => {
  return Array.from({ length: limit }, (_, i) => ({
    name: `${prefix}${offset + i + 1}`,
    label: `${prefix} ${offset + i + 1}`,
    definition: `Definition for ${prefix} ${offset + i + 1}`,
    code: `CODE${offset + i + 1}`,
    codesystem: 'TEST',
    ontologyTermURI: `http://example.com/${prefix}${offset + i + 1}`,
    children: [],
  }));
};

describe('OntologyInput - Lazy Loading', () => {

  beforeEach(() => {
    vi.clearAllMocks();

    // Re-setup the IntersectionObserver mock for each test
    // @ts-ignore
    global.IntersectionObserver = vi.fn().mockImplementation(() => createMockObserver());

    // Setup document for click outside tests
    if (!document.body) {
      document.body = document.createElement('body');
    }
  });

  afterEach(() => {
    vi.restoreAllMocks();
    // Clean up any mounted components
    document.body.innerHTML = '';
  });

  it('should load initial batch of terms', async () => {
    const mockFetch = vi.mocked(fetchGraphql);

    // Mock initial load
    mockFetch.mockResolvedValueOnce({
      ...mockOntologyData,
    });

    // Mock first batch of terms
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 20),
      count: { count: 100 },
    });

    const wrapper = mount(OntologyInput, {
      props: {
        id: 'test-ontology',
        ontologySchemaId: 'test-schema',
        ontologyTableId: 'test-table',
        isArray: true,
      },
    });

    await flushPromises();
    await nextTick();

    // Should show initial 20 terms
    expect(mockFetch).toHaveBeenCalled();
  });

  it('should load more terms when loadMore is called', async () => {
    const mockFetch = vi.mocked(fetchGraphql);

    mockFetch.mockResolvedValueOnce({
      ...mockOntologyData,
    });

    const wrapper = mount(OntologyInput, {
      props: {
        id: 'test-ontology',
        ontologySchemaId: 'test-schema',
        ontologyTableId: 'test-table',
        isArray: true,
      },
    });

    await flushPromises();

    // Mock first batch when opening dropdown (displayAsSelect triggers this)
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 20),
      count: { count: 100 },
    });

    // Open the dropdown to load initial terms
    await (wrapper.vm as any).toggleSelect();
    await flushPromises();

    // Second batch
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(20, 20),
      count: { count: 100 },
    });

    // Trigger load more
    const rootNode = (wrapper.vm as any).rootNode;
    await (wrapper.vm as any).loadMoreTerms(rootNode);
    await flushPromises();

    // Should have 40 children now
    expect(rootNode.children.length).toBe(40);
    expect(rootNode.loadMoreOffset).toBe(40);
    expect(rootNode.loadMoreHasMore).toBe(true);
  });

  it('should prevent duplicate loads on same node', async () => {
    const mockFetch = vi.mocked(fetchGraphql);

    mockFetch.mockResolvedValueOnce({
      ...mockOntologyData,
    });

    const wrapper = mount(OntologyInput, {
      props: {
        id: 'test-ontology',
        ontologySchemaId: 'test-schema',
        ontologyTableId: 'test-table',
        isArray: true,
      },
    });

    await flushPromises();

    // Mock first batch when opening dropdown
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 20),
      count: { count: 100 },
    });

    await (wrapper.vm as any).toggleSelect();
    await flushPromises();

    // Mock the second batch only once
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(20, 20),
      count: { count: 100 },
    });

    const rootNode = (wrapper.vm as any).rootNode;

    // Call loadMore twice simultaneously
    const loadPromise1 = (wrapper.vm as any).loadMoreTerms(rootNode);
    const loadPromise2 = (wrapper.vm as any).loadMoreTerms(rootNode);

    await Promise.all([loadPromise1, loadPromise2]);
    await flushPromises();

    // Should only have loaded once (40 items total, not 60)
    expect(rootNode.children.length).toBe(40);

    // fetchGraphql should have been called only once for the second batch
    expect(mockFetch).toHaveBeenCalledTimes(3); // initial + first batch + second batch (not duplicate)
  });

  it('should stop loading when no more terms available', async () => {
    const mockFetch = vi.mocked(fetchGraphql);

    // For small ontologies (< selectCutOff), it loads allTerms immediately
    mockFetch.mockResolvedValueOnce({
      totalCount: { count: 15 },
      rootCount: { count: 15 },
      allTerms: createMockTerms(0, 15).map(term => ({
        ...term,
        parent: null,
      })),
    });

    const wrapper = mount(OntologyInput, {
      props: {
        id: 'test-ontology',
        ontologySchemaId: 'test-schema',
        ontologyTableId: 'test-table',
        isArray: true,
        limit: 20,
      },
    });

    await flushPromises();

    const rootNode = (wrapper.vm as any).rootNode;

    // loadMoreHasMore should be false since we have all items
    expect(rootNode.loadMoreHasMore).toBe(false);
    expect(rootNode.children.length).toBe(15);
  });

  it('should load all search results when searching', async () => {
    const mockFetch = vi.mocked(fetchGraphql);

    mockFetch.mockResolvedValueOnce({
      ...mockOntologyData,
    });

    const wrapper = mount(OntologyInput, {
      props: {
        id: 'test-ontology',
        ontologySchemaId: 'test-schema',
        ontologyTableId: 'test-table',
        isArray: true,
        limit: 20,
      },
    });

    await flushPromises();

    // Mock initial load when opening dropdown
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 20),
      count: { count: 100 },
    });

    await (wrapper.vm as any).toggleSelect();
    await flushPromises();

    // Mock search results (45 total matching terms)
    // First batch
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 20, 'SearchResult'),
      searchMatch: createMockTerms(0, 20, 'SearchResult'),
      count: { count: 45 },
      totalCount: { count: 45 },
    });

    // Trigger search
    await (wrapper.vm as any).updateSearch('search term');
    await flushPromises();

    const rootNode = (wrapper.vm as any).rootNode;

    // Should have loaded only first page (lazy loading)
    expect(rootNode.children.length).toBe(20);
    expect(rootNode.loadMoreHasMore).toBe(true); // Has more to load
    expect(rootNode.loadMoreTotal).toBe(45);
    expect(rootNode.loadMoreOffset).toBe(20);
  });

  it('should restore pagination when clearing search', async () => {
    const mockFetch = vi.mocked(fetchGraphql);

    mockFetch.mockResolvedValueOnce({
      ...mockOntologyData,
    });

    const wrapper = mount(OntologyInput, {
      props: {
        id: 'test-ontology',
        ontologySchemaId: 'test-schema',
        ontologyTableId: 'test-table',
        isArray: true,
        limit: 20,
      },
    });

    await flushPromises();

    // Mock initial load when opening dropdown
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 20),
      count: { count: 100 },
    });

    await (wrapper.vm as any).toggleSelect();
    await flushPromises();

    // Mock search results
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 10, 'SearchResult'),
      searchMatch: createMockTerms(0, 10, 'SearchResult'),
      count: { count: 10 },
    });

    await (wrapper.vm as any).updateSearch('search');
    await flushPromises();

    // Clear search - mock the return to normal pagination
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 20),
      count: { count: 100 },
    });

    await (wrapper.vm as any).updateSearch('');
    await flushPromises();

    const rootNode = (wrapper.vm as any).rootNode;

    // Should restore pagination
    expect(rootNode.children.length).toBe(20);
    expect(rootNode.loadMoreHasMore).toBe(true);
    expect(rootNode.loadMoreOffset).toBe(20);
  });
});

describe('OntologyInput - Selection', () => {
  it('should handle single selection', async () => {
    const mockFetch = vi.mocked(fetchGraphql);

    mockFetch.mockResolvedValue({
      totalCount: { count: 5 },
      rootCount: { count: 5 },
      allTerms: [
        { name: 'term1', label: 'Term 1', parent: null },
        { name: 'term2', label: 'Term 2', parent: null },
      ],
    });

    const wrapper = mount(OntologyInput, {
      props: {
        id: 'test-ontology',
        ontologySchemaId: 'test-schema',
        ontologyTableId: 'test-table',
        isArray: false, // Single selection
        modelValue: undefined,
      },
    });

    await flushPromises();

    const term = (wrapper.vm as any).rootNode.children[0];
    await (wrapper.vm as any).toggleTermSelect(term);

    expect(wrapper.emitted('update:modelValue')?.[0]?.[0]).toBe('term1');
  });

  it('should handle multiple selection', async () => {
    const mockFetch = vi.mocked(fetchGraphql);

    mockFetch.mockResolvedValue({
      totalCount: { count: 5 },
      rootCount: { count: 5 },
      allTerms: [
        { name: 'term1', label: 'Term 1', parent: null },
        { name: 'term2', label: 'Term 2', parent: null },
      ],
    });

    const wrapper = mount(OntologyInput, {
      props: {
        id: 'test-ontology',
        ontologySchemaId: 'test-schema',
        ontologyTableId: 'test-table',
        isArray: true,
        modelValue: [],
      },
    });

    await flushPromises();

    const term1 = (wrapper.vm as any).rootNode.children[0];
    const term2 = (wrapper.vm as any).rootNode.children[1];

    await (wrapper.vm as any).toggleTermSelect(term1);
    await (wrapper.vm as any).toggleTermSelect(term2);

    const emitted = wrapper.emitted('update:modelValue');
    expect(emitted?.[1]?.[0]).toContain('term1');
    expect(emitted?.[1]?.[0]).toContain('term2');
  });

  it('should close dropdown when clicking outside', async () => {
    const mockFetch = vi.mocked(fetchGraphql);

    mockFetch.mockResolvedValueOnce({
      ...mockOntologyData,
    });

    const wrapper = mount(OntologyInput, {
      props: {
        id: 'test-ontology',
        ontologySchemaId: 'test-schema',
        ontologyTableId: 'test-table',
        isArray: true,
      },
      attachTo: document.body,
    });

    await flushPromises();
    await nextTick();

    // Mock first batch when opening dropdown
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 20),
      count: { count: 100 },
    });

    // Open the dropdown
    await (wrapper.vm as any).toggleSelect();
    await flushPromises();
    await nextTick();

    // Verify dropdown is open
    expect((wrapper.vm as any).showSelect).toBe(true);

    // Wait a bit for all watchers and observers to be set up
    await new Promise(resolve => setTimeout(resolve, 100));

    // Simulate click outside
    const clickEvent = new MouseEvent('mousedown', {
      bubbles: true,
      cancelable: true,
      view: window,
    });
    document.body.dispatchEvent(clickEvent);

    await nextTick();
    await flushPromises();

    // Verify dropdown is closed
    expect((wrapper.vm as any).showSelect).toBe(false);

    // Cleanup - wrap in try-catch to avoid errors during teardown
    try {
      wrapper.unmount();
    } catch (e) {
      // Ignore cleanup errors
      console.warn('Cleanup warning:', e);
    }
  });
});

describe('OntologyInput - Search Functionality', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    // Re-setup the IntersectionObserver mock for each test
    // @ts-ignore
    global.IntersectionObserver = vi.fn().mockImplementation(() => createMockObserver());

    // Setup document for click outside tests
    if (!document.body) {
      document.body = document.createElement('body');
    }
  });

  afterEach(() => {
    vi.restoreAllMocks();
    document.body.innerHTML = '';
  });

  it('should debounce search input', async () => {
    vi.useFakeTimers();
    const mockFetch = vi.mocked(fetchGraphql);

    mockFetch.mockResolvedValueOnce({
      ...mockOntologyData,
    });

    const wrapper = mount(OntologyInput, {
      props: {
        id: 'test-ontology',
        ontologySchemaId: 'test-schema',
        ontologyTableId: 'test-table',
        isArray: true,
      },
    });

    await flushPromises();

    // Mock initial dropdown load
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 20),
      count: { count: 100 },
    });

    await (wrapper.vm as any).toggleSelect();
    await flushPromises();

    const initialCallCount = mockFetch.mock.calls.length;

    // Find the search input
    const searchInput = wrapper.find('input[type="text"]');

    // Type multiple times quickly (this sets searchTerms via v-model)
    // Each setValue updates the v-model which triggers the watcher
    await searchInput.setValue('c');
    await searchInput.setValue('ca');
    await searchInput.setValue('can');
    await searchInput.setValue('canc');
    await searchInput.setValue('cance');
    await searchInput.setValue('cancer');

    // Should not have triggered search yet (debounced)
    expect(mockFetch.mock.calls.length).toBe(initialCallCount);

    // Mock search results for when timer fires
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 10, 'Cancer'),
      searchMatch: createMockTerms(0, 10, 'Cancer'),
      count: { count: 10 },
      totalCount: { count: 10 },
    });

    // Fast forward past debounce time (500ms)
    vi.advanceTimersByTime(550);
    await flushPromises();

    // Now search should have been called
    expect(mockFetch.mock.calls.length).toBeGreaterThan(initialCallCount);

    vi.useRealTimers();
  });

  it('should load all search results when count exceeds limit', async () => {
    const mockFetch = vi.mocked(fetchGraphql);

    mockFetch.mockResolvedValueOnce({
      ...mockOntologyData,
    });

    const wrapper = mount(OntologyInput, {
      props: {
        id: 'test-ontology',
        ontologySchemaId: 'test-schema',
        ontologyTableId: 'test-table',
        isArray: true,
        limit: 20,
      },
    });

    await flushPromises();

    // Mock initial load
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 20),
      count: { count: 100 },
    });

    await (wrapper.vm as any).toggleSelect();
    await flushPromises();

    // Mock search results - 45 total results
    // First batch (offset 0)
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 20, 'SearchResult'),
      searchMatch: createMockTerms(0, 20, 'SearchResult'),
      count: { count: 45 },
      totalCount: { count: 45 },
    });

    // Trigger search directly (bypass debounce)
    await (wrapper.vm as any).updateSearch('search term');
    await flushPromises();

    const rootNode = (wrapper.vm as any).rootNode;

    // Should have loaded only first page (lazy loading)
    expect(rootNode.children.length).toBe(20);
    expect(rootNode.loadMoreHasMore).toBe(true);
    expect(rootNode.loadMoreTotal).toBe(45);

    // Now test loading more
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(20, 20, 'SearchResult'),
      searchMatch: createMockTerms(20, 20, 'SearchResult'),
      count: { count: 45 },
      totalCount: { count: 45 },
    });

    await (wrapper.vm as any).loadMoreTerms(rootNode);
    await flushPromises();

    expect(rootNode.children.length).toBe(40);
    expect(rootNode.loadMoreHasMore).toBe(true);

    // Load final batch
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(40, 5, 'SearchResult'),
      searchMatch: createMockTerms(40, 5, 'SearchResult'),
      count: { count: 45 },
      totalCount: { count: 45 },
    });

    await (wrapper.vm as any).loadMoreTerms(rootNode);
    await flushPromises();

    expect(rootNode.children.length).toBe(45);
    expect(rootNode.loadMoreHasMore).toBe(false); // Partial batch, done loading
  });

  it('should restore pagination when clearing search', async () => {
    const mockFetch = vi.mocked(fetchGraphql);

    mockFetch.mockResolvedValueOnce({
      ...mockOntologyData,
    });

    const wrapper = mount(OntologyInput, {
      props: {
        id: 'test-ontology',
        ontologySchemaId: 'test-schema',
        ontologyTableId: 'test-table',
        isArray: true,
        limit: 20,
      },
    });

    await flushPromises();

    // Mock initial load
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 20),
      count: { count: 100 },
    });

    await (wrapper.vm as any).toggleSelect();
    await flushPromises();

    // Perform search
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 10, 'SearchResult'),
      searchMatch: createMockTerms(0, 10, 'SearchResult'),
      count: { count: 10 },
    });

    await (wrapper.vm as any).updateSearch('search');
    await flushPromises();

    // Verify search state
    let rootNode = (wrapper.vm as any).rootNode;
    expect(rootNode.children.length).toBe(10);
    expect(rootNode.loadMoreHasMore).toBe(false);

    // Clear search
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 20),
      count: { count: 100 },
    });

    await (wrapper.vm as any).updateSearch('');
    await flushPromises();

    rootNode = (wrapper.vm as any).rootNode;

    // Should restore pagination
    expect(rootNode.children.length).toBe(20);
    expect(rootNode.loadMoreHasMore).toBe(true);
    expect(rootNode.loadMoreOffset).toBe(20);
  });

  it('should handle search with no results', async () => {
    const mockFetch = vi.mocked(fetchGraphql);

    mockFetch.mockResolvedValueOnce({
      ...mockOntologyData,
    });

    const wrapper = mount(OntologyInput, {
      props: {
        id: 'test-ontology',
        ontologySchemaId: 'test-schema',
        ontologyTableId: 'test-table',
        isArray: true,
      },
    });

    await flushPromises();

    // Mock initial load
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 20),
      count: { count: 100 },
    });

    await (wrapper.vm as any).toggleSelect();
    await flushPromises();

    // Mock search with no results
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: [],
      searchMatch: [],
      count: { count: 0 },
    });

    await (wrapper.vm as any).updateSearch('nonexistent');
    await flushPromises();

    const rootNode = (wrapper.vm as any).rootNode;

    expect(rootNode.children.length).toBe(0);
    expect(rootNode.loadMoreHasMore).toBe(false);
  });

  it('should update searchTerms value on input', async () => {
    const mockFetch = vi.mocked(fetchGraphql);

    mockFetch.mockResolvedValueOnce({
      ...mockOntologyData,
    });

    const wrapper = mount(OntologyInput, {
      props: {
        id: 'test-ontology',
        ontologySchemaId: 'test-schema',
        ontologyTableId: 'test-table',
        isArray: true,
      },
    });

    await flushPromises();

    // Find the search input
    const searchInput = wrapper.find('input[type="text"]');

    // Simulate typing
    await searchInput.setValue('test search');

    // searchTerms should be updated immediately
    expect((wrapper.vm as any).searchTerms).toBe('test search');
  });
});

describe('OntologyInput - Advanced Search Features', () => {
  it('should show exact hidden count when expanding nodes during search', async () => {
    const mockFetch = vi.mocked(fetchGraphql);

    mockFetch.mockResolvedValueOnce({
      ...mockOntologyData,
    });

    const wrapper = mount(OntologyInput, {
      props: {
        id: 'test-ontology',
        ontologySchemaId: 'test-schema',
        ontologyTableId: 'test-table',
        isArray: true,
      },
    });

    await flushPromises();

    // Open select
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 5),
      count: { count: 5 },
    });

    await (wrapper.vm as any).toggleSelect();
    await flushPromises();

    // Search returns filtered results
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 5),
      count: { count: 5 },
      totalCount: { count: 5 }, // Same as count (all match)
    });

    await (wrapper.vm as any).updateSearch('term');
    await flushPromises();

    // Expand a node during search - returns filtered and total counts
    const nodeToExpand = (wrapper.vm as any).rootNode.children[0];
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(100, 3), // 3 matching children
      searchMatch: createMockTerms(100, 3),
      count: { count: 3 }, // Filtered count
      totalCount: { count: 50 }, // Total children
    });

    await (wrapper.vm as any).toggleTermExpand(nodeToExpand);
    await flushPromises();

    // Node should have unfilteredTotal stored
    expect((nodeToExpand as any).unfilteredTotal).toBe(50);
    expect(nodeToExpand.loadMoreTotal).toBe(3);
    expect(nodeToExpand.children.length).toBe(3);
  });

  it('should support "show all" to bypass search filter on individual nodes', async () => {
    const mockFetch = vi.mocked(fetchGraphql);

    mockFetch.mockResolvedValueOnce({
      ...mockOntologyData,
    });

    const wrapper = mount(OntologyInput, {
      props: {
        id: 'test-ontology',
        ontologySchemaId: 'test-schema',
        ontologyTableId: 'test-table',
        isArray: true,
      },
    });

    await flushPromises();

    // Open and search
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 5),
      count: { count: 5 },
    });
    await (wrapper.vm as any).toggleSelect();
    await flushPromises();

    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 5),
      count: { count: 5 },
      totalCount: { count: 5 },
    });
    await (wrapper.vm as any).updateSearch('term');
    await flushPromises();

    const nodeToExpand = (wrapper.vm as any).rootNode.children[0];

    // First expand with search filter - 3 matching children
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(100, 3),
      searchMatch: createMockTerms(100, 3),
      count: { count: 3 },
      totalCount: { count: 50 },
    });
    await (wrapper.vm as any).toggleTermExpand(nodeToExpand);
    await flushPromises();

    expect(nodeToExpand.children.length).toBe(3);
    expect((nodeToExpand as any).showingAll).toBeFalsy();

    // Now "show all" - should load all 50 children
    nodeToExpand.expanded = false; // Collapse first
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(100, 20), // First page of all children
      count: { count: 50 },
      totalCount: { count: 50 }, // Same when not filtering
    });

    await (wrapper.vm as any).toggleTermExpand(nodeToExpand, true); // showAll = true
    await flushPromises();

    expect(nodeToExpand.children.length).toBe(20);
    expect((nodeToExpand as any).showingAll).toBe(true);
    expect(nodeToExpand.loadMoreTotal).toBe(50);
  });

  it('should handle lazy loading during search with correct offset', async () => {
    const mockFetch = vi.mocked(fetchGraphql);

    mockFetch.mockResolvedValueOnce({
      ...mockOntologyData,
    });

    const wrapper = mount(OntologyInput, {
      props: {
        id: 'test-ontology',
        ontologySchemaId: 'test-schema',
        ontologyTableId: 'test-table',
        isArray: true,
        limit: 5, // Small limit for testing
      },
    });

    await flushPromises();

    // Open select
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 5),
      count: { count: 78 },
    });
    await (wrapper.vm as any).toggleSelect();
    await flushPromises();

    // Search returns first page
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 5),
      count: { count: 78 }, // 78 total matching results
      totalCount: { count: 78 },
    });

    await (wrapper.vm as any).updateSearch('health');
    await flushPromises();

    const rootNode = (wrapper.vm as any).rootNode;
    expect(rootNode.children.length).toBe(5);
    expect(rootNode.loadMoreTotal).toBe(78);
    expect(rootNode.loadMoreHasMore).toBe(true);
    expect(rootNode.loadMoreOffset).toBe(5);

    // Load more should request with offset 5
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(5, 5),
      count: { count: 78 },
      totalCount: { count: 78 },
    });

    await (wrapper.vm as any).loadMoreTerms(rootNode);
    await flushPromises();

    expect(rootNode.children.length).toBe(10);
    expect(rootNode.loadMoreOffset).toBe(10);
    expect(rootNode.loadMoreHasMore).toBe(true);
  });

  it('should stop loading when receiving partial batch', async () => {
    const mockFetch = vi.mocked(fetchGraphql);

    mockFetch.mockResolvedValueOnce({
      ...mockOntologyData,
    });

    const wrapper = mount(OntologyInput, {
      props: {
        id: 'test-ontology',
        ontologySchemaId: 'test-schema',
        ontologyTableId: 'test-table',
        isArray: true,
        limit: 20,
      },
    });

    await flushPromises();

    // Open and search
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 20),
      count: { count: 23 },
    });
    await (wrapper.vm as any).toggleSelect();
    await flushPromises();

    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 20),
      count: { count: 23 },
      totalCount: { count: 23 },
    });
    await (wrapper.vm as any).updateSearch('test');
    await flushPromises();

    const rootNode = (wrapper.vm as any).rootNode;
    expect(rootNode.loadMoreHasMore).toBe(true);

    // Load more returns only 3 items (partial batch)
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(20, 3), // Only 3 items
      count: { count: 23 },
      totalCount: { count: 23 },
    });

    await (wrapper.vm as any).loadMoreTerms(rootNode);
    await flushPromises();

    expect(rootNode.children.length).toBe(23);
    expect(rootNode.loadMoreOffset).toBe(23);
    expect(rootNode.loadMoreHasMore).toBe(false); // Should stop (partial batch)
  });

  it('should maintain search context when loading more on child nodes', async () => {
    const mockFetch = vi.mocked(fetchGraphql);

    mockFetch.mockResolvedValueOnce({
      ...mockOntologyData,
    });

    const wrapper = mount(OntologyInput, {
      props: {
        id: 'test-ontology',
        ontologySchemaId: 'test-schema',
        ontologyTableId: 'test-table',
        isArray: true,
        limit: 5,
      },
    });

    await flushPromises();

    // Search at root
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 5),
      count: { count: 5 },
    });
    await (wrapper.vm as any).toggleSelect();
    await flushPromises();

    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 5),
      count: { count: 5 },
      totalCount: { count: 5 },
    });
    await (wrapper.vm as any).updateSearch('health');
    await flushPromises();

    // Expand node - gets filtered children
    const nodeToExpand = (wrapper.vm as any).rootNode.children[0];
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(100, 5),
      searchMatch: createMockTerms(100, 5),
      count: { count: 15 }, // 15 matching children
      totalCount: { count: 50 },
    });

    await (wrapper.vm as any).toggleTermExpand(nodeToExpand);
    await flushPromises();

    expect(nodeToExpand.children.length).toBe(5);
    expect(nodeToExpand.loadMoreHasMore).toBe(true);

    // Load more on this node should maintain search filter
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(105, 5),
      searchMatch: createMockTerms(105, 5),
      count: { count: 15 },
      totalCount: { count: 50 },
    });

    await (wrapper.vm as any).loadMoreTerms(nodeToExpand);
    await flushPromises();

    expect(nodeToExpand.children.length).toBe(10);
    expect((nodeToExpand as any).showingAll).toBeFalsy(); // Still filtered
  });

  it('should always include totalCount in queries', async () => {
    const mockFetch = vi.mocked(fetchGraphql);

    mockFetch.mockResolvedValueOnce({
      ...mockOntologyData,
    });

    const wrapper = mount(OntologyInput, {
      props: {
        id: 'test-ontology',
        ontologySchemaId: 'test-schema',
        ontologyTableId: 'test-table',
        isArray: true,
      },
    });

    await flushPromises();

    // Open select - even in normal mode should get totalCount
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 20),
      count: { count: 100 },
      totalCount: { count: 100 }, // Should be present
    });

    await (wrapper.vm as any).toggleSelect();
    await flushPromises();

    // Expand a node without search
    const nodeToExpand = (wrapper.vm as any).rootNode.children[0];
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(100, 20),
      count: { count: 50 },
      totalCount: { count: 50 }, // Should be present even without search
    });

    await (wrapper.vm as any).toggleTermExpand(nodeToExpand);
    await flushPromises();

    expect((nodeToExpand as any).unfilteredTotal).toBe(50);
    expect(nodeToExpand.loadMoreTotal).toBe(50);
  });

  it('should clear search and reload first page on search clear', async () => {
    const mockFetch = vi.mocked(fetchGraphql);

    mockFetch.mockResolvedValueOnce({
      ...mockOntologyData,
    });

    const wrapper = mount(OntologyInput, {
      props: {
        id: 'test-ontology',
        ontologySchemaId: 'test-schema',
        ontologyTableId: 'test-table',
        isArray: true,
      },
    });

    await flushPromises();

    // Search
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 5),
      count: { count: 5 },
    });
    await (wrapper.vm as any).toggleSelect();
    await flushPromises();

    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 5),
      count: { count: 5 },
      totalCount: { count: 5 },
    });
    await (wrapper.vm as any).updateSearch('health');
    await flushPromises();

    const rootNode = (wrapper.vm as any).rootNode;
    expect(rootNode.children.length).toBe(5);

    // Clear search
    mockFetch.mockResolvedValueOnce({
      retrieveTerms: createMockTerms(0, 20),
      count: { count: 100 },
      totalCount: { count: 100 },
    });

    await (wrapper.vm as any).updateSearch('');
    await flushPromises();

    expect(rootNode.children.length).toBe(20);
    expect(rootNode.loadMoreTotal).toBe(100);
    expect(rootNode.loadMoreHasMore).toBe(true);
  });
});