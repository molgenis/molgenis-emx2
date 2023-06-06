<template>
  <div>
    <div v-if="variable">
      <h5>
        {{ variable.label }}
      </h5>
      <div class="row">
        <variable-details
          class="col"
          :variableDetails="variable"
          :showMappedBy="false" />
      </div>
    </div>
  </div>
</template>

<script>
import { fetchFromVariableDetails } from "../store/repository/variableRepository";
import VariableDetails from "../components/VariableDetails.vue";

export default {
  name: "FromVariableDetails",
  components: { VariableDetails },
  props: {
    sourceCohort: String,
    fromName: String,
  },
  data() {
    return {
      variable: {},
    };
  },
  async created() {
    this.variable = await fetchFromVariableDetails(
      this.fromName,
      this.sourceCohort
    );
  },
};
</script>

<style></style>
