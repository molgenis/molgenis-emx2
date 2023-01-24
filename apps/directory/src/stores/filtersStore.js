import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue';
import { applyFiltersToQuery } from '../functions/applyFiltersToQuery';
import { useBiobanksStore } from './biobanksStore';
import { useSettingsStore } from './settingsStore';

export const useFiltersStore = defineStore('filtersStore', () => {
    const biobankStore = useBiobanksStore();
    const { baseQuery, updateBiobankCards } = biobankStore

    const settingsStore = useSettingsStore();

    let filters = ref({})
    let filterType = ref({})

    function resetFilters () {
        this.baseQuery.resetFilters();
    }

    const hasActiveFilters = computed(() => {
        return Object.keys(filters.value).length > 0
    })

    let queryDelay = undefined

    watch(
        filters,
        (filters) => {
            if (queryDelay) {
                clearTimeout(queryDelay);
            }
            /** reset pagination */
            settingsStore.currentPage = 1

            queryDelay = setTimeout(async () => {
                clearTimeout(queryDelay);

                applyFiltersToQuery(baseQuery, filters)
                await updateBiobankCards()

            }, 750);
        },
        { deep: true }
    )

    function updateFilter (filterName, value) {
        /** filter reset, so delete */
        if (value === "" || value === undefined || value.length === 0) {
            delete filters.value[filterName]
        } else {
            filters.value[filterName] = value
        }
    }

    function getFilterValue (filterName) {
        return filters.value[filterName]
    }

    function updateFilterType (filterName, value) {
        /** filter reset, so delete */
        if (value === "" || value === undefined || value.length === 0) {
            delete filterType.value[filterName]
        } else {
            filterType.value[filterName] = value
        }
    }

    function getFilterType (filterName) {
        return filterType.value[filterName]
    }

    return {
        resetFilters,
        updateFilter,
        getFilterValue,
        updateFilterType,
        getFilterType,
        hasActiveFilters,
        filters
    }
})
