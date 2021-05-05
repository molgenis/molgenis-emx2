<template>
  <FilterContainer
      v-if="keywords && keywords.length"
      :value="selection"
      :filters="filters"
      :filters-shown="['keywords']"
      @input="setFilters"
  />
</template>

<script>
import { FilterContainer } from '@molgenis-ui/components-library'
import { mapState, mapMutations, mapActions } from 'vuex'

export default {
  components: {
    FilterContainer
  },
  computed: {
    ...mapState({ selection: 'filters', keywords: 'keywords' }),
    filters () {
      return [
        {
          name: 'keywords',
          label: 'Keywords',
          collapsed: false,
          type: 'tree-filter',
          options: this.keywordNodes
        }
      ]
    }
  },
  methods: {
    ...mapMutations(['setFilters']),
    ...mapActions(['fetchKeywords']),
    keywordNodes () {
      return new Promise((resolve) => resolve(this.keywords))
    }
  },
  async created() {
    await this.fetchKeywords()
  }
}
</script>