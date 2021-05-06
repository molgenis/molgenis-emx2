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
    ...mapState({ active: 'filters', keywords: 'keywords', databanks: 'databanks', networks: 'networks' }),
    filters () {
      return [
          {
          name: 'networks',
          label: 'Networks',
          options: this.networkOptions || [],
          type: 'checkbox-filter'
        },
        {
          name: 'keywords',
          label: 'Keywords',
          options: this.keywordNodes || [],
          type: 'tree-filter'
        },
        {
          name: 'databanks',
          label: 'Databanks',
          options: this.databankOptions || [],
          type: 'checkbox-filter'
        },
      ]
    }
  },
  methods: {
    ...mapMutations(['setFilters']),
    async keywordOptions () {
      return (
          this.keywords &&
          this.keywords.map(keyword => ({
            text: this.pathText(this.keywords, keyword.id),
            value: keyword.id
          }))
      )
    },
    async networkOptions () {
      return (
          this.networks &&
          this.networks.map(network => ({
            value: network.acronym,
            text: network.name
          }))
      )
    },
    async databankOptions () {
      return (
          this.databanks &&
          this.databanks.map(databank => ({
            value: databank.acronym,
            text: databank.name
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
