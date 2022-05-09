<template>
  <div v-if="contributors">
    <div v-for="(contributor, index) in contributors" :key="index">
      <a v-if="contributor.contact.email">{{ contributor.contact.email }}</a>
      <OntologyTerms
        :terms="contributor.contributionType"
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
