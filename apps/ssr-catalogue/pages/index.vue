<template>
<div>
  <Menu :menu="menu"/>
  <div class="container pt-3">
    <h1>Data Catalogue</h1>
        <h2>Collected data</h2>
    <div class="row justify-content-between">

      <count-card class="btn-dark" :count="counts.institutions" label="Institutions" to="institutions">
        Contributors to the catalogue such as universities, companies, medical
          centres and research institutes
      </count-card>

      <count-card class="btn-secondary" :count="counts.datasources" label="Data sources" to="datasources">
        Collections of data banks covering the same population
      </count-card>

      <count-card class="btn-info" :count="counts.databanks" label="Data banks" to="databanks">
        Data collections such as registries or biobanks
      </count-card>

      <count-card class="btn-primary" :count="counts.cohorts" label="Cohorts" to="cohorts">
         Systematic observations of large groups of individuals over time.
      </count-card>

    </div>
  </div>
</div>
</template>

<script>
import CountCard from '../components/CountCard.vue'
import Menu from '../components/Menu.vue'
export default {
  components: { Menu, CountCard },
  async fetch ({store}) {
    await store.dispatch('fetchCounts')
    return store.dispatch('fetchSession')
  },
  computed: {
    menu () {
      return this.$store.state.menu
    },
    counts () {
      return this.$store.state.counts
    }
  },
}
</script>
