<template>
  <StringFilter
    name="Search"
    v-model="search"
    placeholder="Search"></StringFilter>
</template>

<script>
import StringFilter from '../filters/StringFilter.vue'

export default {
  components: {
    StringFilter
  },
  data () {
    return {
      debounce: undefined,
      search: ''
    }
  },
  // computed: {
  //   search: {
  //     get () {
  //       return this.activeFilters.search
  //     },
  //     set (search) {
  //       if (this.debounce) {
  //         clearTimeout(this.debounce)
  //       }

  //       this.debounce = setTimeout(async () => {
  //         clearTimeout(this.debounce)
  //         this.UpdateFilterSelection({
  //           name: 'search',
  //           value: search
  //         })
  //       }, 750)
  //     }
  //   }
  // },
  // methods: {
  //   ...mapMutations(['UpdateFilterSelection'])
  // }
}
</script>
