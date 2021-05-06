<template>
  <filter-container
      v-if="keywords && keywords.length && networks && networks.length && databanks && databanks.length"
      :value="selection"
      :filters="filters"
      :filters-shown="['networks', 'keywords', 'databanks']"
      @input="setFilters"
  />
</template>

<script>
import { FilterContainer } from '@molgenis-ui/components-library'
import { mapState, mapMutations, mapActions } from 'vuex'

export default {
  name: 'VariableFiltersView',
  components: {
    FilterContainer
  },
  computed: {
    ...mapState({ selection: 'filters', keywords: 'keywords', networks: 'networks', databanks: 'databanks' }),
    filters () {
      return [
        {
          name: 'networks',
          label: 'Networks',
          collapsed: false,
          options: this.networkOptions,
          initialDisplayItems: 2,
          type: 'checkbox-filter',
          bulkOperation: true
        },
        {
          name: 'keywords',
          label: 'Keywords',
          collapsed: false,
          type: 'tree-filter',
          options: this.keywordOptions
        },
        {
          name: 'databanks',
          label: 'Databanks',
          collapsed: true,
          options: this.databankOptions,
          initialDisplayItems: 2,
          type: 'checkbox-filter',
          bulkOperation: true
        },
      ]
    }
  },
  methods: {
    ...mapMutations(['setFilters']),
    ...mapActions(['fetchKeywords', 'fetchDatabanks', 'fetchNetworks']),
    async keywordOptions () {
      return this.keywords || []
    },
    async networkOptions () {
      return this.networks.map(network => ({ value: network.acronym, text: network.acronym})) || []
    },
    async databankOptions () {
      return this.databanks.map(databank => ({ value: databank.acronym, text: databank.acronym})) || []
    }
  },
  async created() {
    await this.fetchKeywords()
    await this.fetchDatabanks()
    await this.fetchNetworks()
  }
}
</script>