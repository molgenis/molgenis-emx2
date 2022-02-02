<template>
<div>
  <div class="container pt-3">
    <h1>
      <span v-if="$route.params.schema == 'Minerva'">
        Proof-of-concept catalogue tool for MINERVA metadata pilot</span
      >
      <span v-else>Data catalogue</span>
    </h1>
    <p v-if="$route.params.schema == 'Minerva'" class="text-danger">
      Disclaimer: contents not for public disclosure.
    </p>
    <p>
      Browse and manage metadata for data resources, such as cohorts,
      registries, biobanks, and multi-center collaborations thereof such as
      networks, common data models and studies.
    </p>
    <h2>Metadata on data collections</h2>
    <div class="row justify-content-between">

      <count-card class="btn-dark" :count="counts.institutions" label="Institutions" to="ssr-catalogue/institutions">
        Contributors to the catalogue such as universities, companies, medical
          centres and research institutes
      </count-card>

      <count-card class="btn-secondary" :count="counts.datasources" label="Data sources" to="ssr-catalogue/datasources">
        Collections of data banks covering the same population
      </count-card>

      <count-card class="btn-info" :count="counts.databanks" label="Data banks" to="ssr-catalogue/databanks">
        Data collections such as registries or biobanks
      </count-card>

      <count-card class="btn-primary" :count="counts.cohorts" label="Cohorts" to="ssr-catalogue/cohorts">
         Systematic observations of large groups of individuals over time.
      </count-card>

    </div>
  </div>

</div>
</template>

<style scoped>
.btn .badge {
  position: absolute;
  top: 5px;
  right: 5px;
}

.card-height {
  min-height: 200px;
}
</style>

<script>
import CountCard from '../../../components/CountCard.vue'
export default {
  name: "SSRCatalogue",
  components: { CountCard },
  async fetch ({store}) {
    await store.dispatch('fetchCounts')
    return store.dispatch('fetchSession')
  },
  computed: {
    counts () {
      return this.$store.state.counts
    },
    schema () {
      return this.$store.state.schema
    }
  },
}
</script>
