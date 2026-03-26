import { ref, computed } from "vue";
import { vi } from "vitest";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue, UseFilters } from "../../../types/filters";

const MAX_VISIBLE_FILTERS = 25;

export interface MockUseFiltersOptions {
  reactive?: boolean;
  initialFilters?: Map<string, IFilterValue>;
  initialColumns?: IColumn[];
  initialVisibleIds?: string[];
  defaultVisibleIds?: string[];
  mockRoute?: { query: Record<string, string> };
  mockRouter?: { replace: ReturnType<typeof vi.fn> };
}

function writeVisibleFiltersToUrl(
  newIds: string[],
  defaultIds: string[],
  mockRoute: { query: Record<string, string> },
  mockRouter: { replace: ReturnType<typeof vi.fn> }
) {
  const isDefault =
    newIds.length === defaultIds.length &&
    [...newIds].sort().every((v, i) => v === [...defaultIds].sort()[i]);
  const currentQuery = { ...mockRoute.query };
  if (isDefault) {
    delete currentQuery["mg_filters"];
  } else {
    currentQuery["mg_filters"] = newIds.join(",");
  }
  mockRouter.replace({ query: currentQuery });
}

export function createMockUseFilters(
  options: MockUseFiltersOptions = {}
): UseFilters {
  const {
    reactive: isReactive = true,
    initialFilters,
    initialColumns = [],
    initialVisibleIds,
    defaultVisibleIds = [],
    mockRoute,
    mockRouter,
  } = options;

  if (!isReactive) {
    return {
      columns: ref(initialColumns),
      visibleFilterIds: ref(initialVisibleIds ?? []),
      toggleFilter: vi.fn(),
      resetFilters: vi.fn(),
      loadRefColumns: vi.fn(),
      getRefColumns: vi.fn().mockReturnValue([]),
      filterStates: ref(new Map()),
      searchValue: ref(""),
      gqlFilter: ref({}),
      activeFilters: computed(() => []),
      setFilter: vi.fn(),
      setSearch: vi.fn(),
      clearFilters: vi.fn(),
      removeFilter: vi.fn(),
      resolvedFilters: computed(() => []),
      setFilterValue: vi.fn(),
      getCountFetcher: vi.fn(),
    } as unknown as UseFilters;
  }

  const filterStatesRef = ref<Map<string, IFilterValue>>(
    initialFilters ?? new Map()
  );
  const searchValueRef = ref("");
  const columnsRef = ref<IColumn[]>(initialColumns);
  const visibleFilterIdsRef = ref<string[]>(initialVisibleIds ?? []);
  const refColumnsCache = new Map<string, IColumn[]>();
  const effectiveDefaultIds = defaultVisibleIds;

  const resolvedFilters = computed(() => {
    return visibleFilterIdsRef.value
      .map((id) => {
        const column = columnsRef.value.find((c) => c.id === id);
        if (!column) return null;
        return { fullPath: id, column, label: column.label || column.id };
      })
      .filter(Boolean) as {
      fullPath: string;
      column: IColumn;
      label: string;
    }[];
  });

  return {
    filterStates: filterStatesRef,
    searchValue: searchValueRef,
    gqlFilter: ref({}),
    activeFilters: computed(() => []),
    setFilter: (columnId: string, value: IFilterValue | null) => {
      const newMap = new Map(filterStatesRef.value);
      if (value === null) {
        newMap.delete(columnId);
      } else {
        newMap.set(columnId, value);
      }
      filterStatesRef.value = newMap;
    },
    setSearch: (value: string) => {
      searchValueRef.value = value;
    },
    clearFilters: () => {
      filterStatesRef.value = new Map();
      searchValueRef.value = "";
    },
    removeFilter: (columnId: string) => {
      const newMap = new Map(filterStatesRef.value);
      newMap.delete(columnId);
      filterStatesRef.value = newMap;
    },
    columns: columnsRef,
    visibleFilterIds: visibleFilterIdsRef,
    toggleFilter: (columnId: string) => {
      if (visibleFilterIdsRef.value.includes(columnId)) {
        const newIds = visibleFilterIdsRef.value.filter(
          (id) => id !== columnId
        );
        visibleFilterIdsRef.value = newIds;
        const newMap = new Map(filterStatesRef.value);
        newMap.delete(columnId);
        filterStatesRef.value = newMap;
        if (mockRoute && mockRouter) {
          writeVisibleFiltersToUrl(
            newIds,
            effectiveDefaultIds,
            mockRoute,
            mockRouter
          );
        }
      } else if (visibleFilterIdsRef.value.length < MAX_VISIBLE_FILTERS) {
        const newIds = [columnId, ...visibleFilterIdsRef.value];
        visibleFilterIdsRef.value = newIds;
        if (mockRoute && mockRouter) {
          writeVisibleFiltersToUrl(
            newIds,
            effectiveDefaultIds,
            mockRoute,
            mockRouter
          );
        }
      }
    },
    resetFilters: () => {
      const newDefaults = [...effectiveDefaultIds];
      visibleFilterIdsRef.value = newDefaults;
      filterStatesRef.value = new Map();
      if (mockRoute && mockRouter) {
        writeVisibleFiltersToUrl(
          newDefaults,
          effectiveDefaultIds,
          mockRoute,
          mockRouter
        );
      }
    },
    loadRefColumns: vi.fn(),
    getRefColumns: (path: string) => refColumnsCache.get(path) ?? [],
    resolvedFilters,
    setFilterValue: async (
      columnId: string,
      value: IFilterValue | null | undefined
    ) => {
      if (value === null || value === undefined) {
        const newMap = new Map(filterStatesRef.value);
        newMap.delete(columnId);
        filterStatesRef.value = newMap;
      } else {
        const newMap = new Map(filterStatesRef.value);
        newMap.set(columnId, value);
        filterStatesRef.value = newMap;
      }
    },
    getCountFetcher: vi.fn().mockReturnValue({
      fetchRefCounts: vi.fn().mockResolvedValue(new Map()),
      fetchOntologyLeafCounts: vi.fn().mockResolvedValue(new Map()),
      fetchOntologyParentCounts: vi.fn().mockResolvedValue(new Map()),
      getCrossFilter: vi.fn().mockReturnValue(undefined),
    }),
  };
}
