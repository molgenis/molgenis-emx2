<template>
  <ul v-if="models">
    <li v-for="d in models" :key="d.acronym">
      <RouterLink :to="{ name: 'model', params: { acronym: d.acronym } }"
        >{{ d.acronym }} - {{ d.name }}
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
    models: Array,
    color: String,
  },
};
</script>
