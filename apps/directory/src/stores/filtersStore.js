import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue';
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

            queryDelay = setTimeout(() => {
                clearTimeout(queryDelay);
                console.log({filters, active: hasActiveFilters.value})
            }, 750);
            // persist the whole state to the local storage whenever it changes
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
