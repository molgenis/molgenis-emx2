<template>
  <ul v-if="datasources">
    <li v-for="d in datasources" :key="d.pid">
      <RouterLink
        :to="{
          name: 'datasource',
          params: { pid: d.pid },
        }"
        >{{ d.pid }} - {{ d.name }}
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
import OntologyTerms from "./OntologyTerms";

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
