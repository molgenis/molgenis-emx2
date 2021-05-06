<template>
  <active-filters
      :value="active"
      :filters="filters"
      @input="setFilters"
  />
</template>

<script>
import ActiveFilters from '@/components/lifecycle/ActiveFilters.vue'
import { mapState, mapMutations } from 'vuex'

export default {
  name: "VariablesSelectedFiltersView",
  components: {
    ActiveFilters
  },
  computed: {
    ...mapState({ active: 'filters', keywords: 'keywords' }),
    filters () {
      return [
        {
          name: 'keywords',
          label: 'Keywords',
          options: this.keywordNodes,
          type: 'tree-filter'
        }
      ]
    }
  },
  methods: {
    ...mapMutations(['setFilters']),
    async keywordNodes () {
      return (
          this.keywords &&
          this.keywords.map(keyword => ({
            text: this.pathText(this.keywords, keyword.id),
            value: keyword.id
          }))
      )
    },
    pathText (nodes, id) {
      const keyword = nodes.find(it => it.id === id)
      return keyword.parent
          ? `${this.pathText(nodes, keyword.parent)} / ${keyword.label}`
          : keyword.label
    }
  }
}
</script>
