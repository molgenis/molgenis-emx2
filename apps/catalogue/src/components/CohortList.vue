<template>
  <ul v-if="cohorts">
    <li v-for="c in cohorts" :key="c.id">
      <RouterLink :to="{ name: 'cohort', params: { id: c.id } }"
        >{{ c.id }} - {{ c.name }}
        <OntologyTerms
          :terms="c.type"
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
    cohorts: Array,
    color: String,
  },
};
</script>
