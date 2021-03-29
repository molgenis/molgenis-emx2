<template>
  <div>
    <h6>Access conditions</h6>
    <OntologyTerms :terms="resource.conditions" color="danger" :inline="true" />
    <p v-if="resource.conditionsDescription">{{resource.conditionsDescription}}</p>
    <h6>Funding statement</h6>
    <p>
      {{ resource.funding ? resource.funding : "N/A" }}
    </p>
    <h6>Acknowledgement statement</h6>
    <p>
      {{ resource.acknowledgements ? resource.acknowledgements : "N/A" }}
    </p>
  </div>
</template>

<script>
import OntologyTerms from "./OntologyTerms";

export default {
  components: { OntologyTerms },
  props: {
    resource: Object,
  },
};
</script>
