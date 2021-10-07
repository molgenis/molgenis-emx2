<template>
  <ul v-if="contributors">
    <li v-for="c in contributors">
      {{ c.contact.name }}
      <OntologyTerms
        :terms="c.contributionType"
        :inline="true"
        :color="color"
      />
    </li>
  </ul>
  <p v-else>N/A</p>
</template>
<script>
import OntologyTerms from "./OntologyTerms";

export default {
  components: { OntologyTerms },
  props: {
    contributors: Array,
    color: {
      type: String,
      default: () => "primary",
    },
  },
};
</script>
