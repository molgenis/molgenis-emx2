<template>
  <ul v-if="datasources">
    <li v-for="d in datasources" :key="d.id">
      <RouterLink
        :to="{
          name: 'Datasources-details',
          params: { id: d.id },
        }"
        >{{ d.id }} - {{ d.name }}
        <OntologyTerms
          :terms="d.type"
          :inline="true"
          :color="color"
        ></OntologyTerms>
      </RouterLink>
    </li>
  </ul>
  <p v-else>N/A</p>
</template>

<script>
import OntologyTerms from "./OntologyTerms.vue";

export default {
  components: {
    OntologyTerms,
  },
  props: {
    color: String,
    datasources: Array,
  },
};
</script>
