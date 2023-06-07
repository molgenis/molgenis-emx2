<template>
  <ul v-if="databanks">
    <li v-for="d in databanks" :key="d.id">
      <RouterLink :to="{ name: 'databank', params: { id: d.id } }"
        >{{ d.id }} - {{ d.name }}
        <OntologyTerms
          :terms="d.type"
          :inline="true"
          :color="color"></OntologyTerms>
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
    databanks: Array,
    color: String,
  },
};
</script>
