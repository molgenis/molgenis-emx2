import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue';
import { applyFiltersToQuery } from '../functions/applyFiltersToQuery';
import { useBiobanksStore } from './biobanksStore';

export const useFiltersStore = defineStore('filtersStore', () => {
    const biobankStore = useBiobanksStore();
    const { baseQuery, updateBiobankCards } = biobankStore

    let filters = ref({})

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

    return {
        resetFilters,
        updateFilter,
        getFilterValue,
        hasActiveFilters,
        filters
    }
})
