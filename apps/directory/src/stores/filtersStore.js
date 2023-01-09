import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue';
import { applyFiltersToQuery } from '../functions/applyFiltersToQuery';
import { useBiobanksStore } from './biobanksStore';

export const useFiltersStore = defineStore('filtersStore', () => {
    /** making a hardcopy, just to be sure */
    const { baseQuery } = Object.assign({}, useBiobanksStore());

    const filterResult = ref({})

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

                const queryWithFiltersApplied = applyFiltersToQuery(baseQuery, filters)
                filterResult.value = await queryWithFiltersApplied.execute();

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
        baseQuery,
        filterResult,
        resetFilters,
        updateFilter,
        getFilterValue,
        hasActiveFilters,
        filters
    }
})
