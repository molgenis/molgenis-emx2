<template>
  <ul v-if="databanks">
    <li v-for="d in databanks" :key="d.pid">
      <RouterLink :to="{ name: 'databank', params: { pid: d.pid } }"
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
  components: { OntologyTerms },
  props: {
    databanks: Array,
    color: String,
  },
};
</script>
