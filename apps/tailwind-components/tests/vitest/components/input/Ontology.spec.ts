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
        });

        // Second batch of search results
        mockFetch.mockResolvedValueOnce({
            retrieveTerms: createMockTerms(20, 20, 'SearchResult'),
            searchMatch: createMockTerms(20, 20, 'SearchResult'),
            count: { count: 45 },
        });

        // Third batch of search results
        mockFetch.mockResolvedValueOnce({
            retrieveTerms: createMockTerms(40, 5, 'SearchResult'),
            searchMatch: createMockTerms(40, 5, 'SearchResult'),
            count: { count: 45 },
        });

        // Trigger search
        await (wrapper.vm as any).updateSearch('search term');
        await flushPromises();

        const rootNode = (wrapper.vm as any).rootNode;

        // Should have loaded all 45 search results
        expect(rootNode.children.length).toBe(45);
        expect(rootNode.loadMoreHasMore).toBe(false); // No pagination during search
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