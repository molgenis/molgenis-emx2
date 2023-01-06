import { defineStore } from 'pinia'
import { ref } from 'vue';
import { useBiobanksStore } from './biobanksStore';

export const useFiltersStore = defineStore('filtersStore', () => {
    /** making a hardcopy, just to be sure */
    const { baseQuery } = Object.assign({}, useBiobanksStore());

    const filterResult = ref({})

    const search = ref('')

    function resetFilters () {
        this.baseQuery.resetFilters();
    }


    // this.debounce = setTimeout(async () => {
        //         clearTimeout(this.debounce)
        //         this.UpdateFilterSelection({
        //           name: 'search',
        //           value: search
        //         })
        //       }, 750)

    return { 
        baseQuery, 
        filterResult, 
        resetFilters, 
        search 
    }
})
