<template>
  <div>
    <h6>Access conditions</h6>
    <OntologyTerms :terms="resource.dataAccessConditions" :color="color" />
    <h6>Access conditions description</h6>
    <p v-if="resource.dataAccessConditionsDescription">
      {{ resource.dataAccessConditionsDescription }}
    </p>
    <h6>Funding statement</h6>
    <p>
      {{ resource.fundingStatement ? resource.fundingStatement : "N/A" }}
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
    color: String,
    resource: Object,
  },
};
</script>
