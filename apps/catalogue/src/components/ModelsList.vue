<template>
  <ul v-if="models">
    <li v-for="d in models" :key="d.id">
      <RouterLink :to="{ name: 'model', params: { id: d.id } }"
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
  components: { OntologyTerms },
  props: {
    models: Array,
    color: String,
  },
};
</script>
