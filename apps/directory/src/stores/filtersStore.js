import { defineStore } from 'pinia'
import { ref } from 'vue';
import { useBiobanksStore } from './biobanksStore';

export const useFiltersStore = defineStore('filtersStore', () => {
    /** making a hardcopy, just to be sure */
    const { baseQuery } = Object.assign({}, useBiobanksStore());

    const filterResult = ref({})

    function resetFilters () {
        this.baseQuery.resetFilters();
    }

    return { baseQuery, filterResult, resetFilters }
})
