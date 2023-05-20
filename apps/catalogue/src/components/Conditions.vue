<template>
  <div>
    <h6>Data access conditions</h6>
    <OntologyTerms :terms="resource.dataAccessConditions" :color="color" />
    <h6>Data use conditions</h6>
    <OntologyTerms :terms="resource.dataUseConditions" :color="color" />
    <h6>Data access conditions description</h6>
    <p>
      {{
        resource.dataAccessConditionsDescription
          ? resource.dataAccessConditionsDescription
          : "N/A"
      }}
    </p>
    <h6>Fee required?</h6>
    <p>
      {{ resource.dataAccessFee }}
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
import OntologyTerms from "./OntologyTerms.vue";

export default {
  components: { OntologyTerms },
  props: {
    color: String,
    resource: Object,
  },
};
</script>
