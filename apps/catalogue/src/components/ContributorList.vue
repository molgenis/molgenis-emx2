<template>
  <div v-if="contributors">
    <div v-for="(c, index) in contributors" :key="index">
      <a v-if="c.contact.email">{{ c.contact.email }}</a>
      <OntologyTerms
        :terms="c.contributionType"
        :inline="true"
        :color="color"
      />
    </div>
  </div>
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
