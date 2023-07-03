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
          :showMappedBy="false"
        />
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
    variable: Object,
  },
};
</script>

<style></style>
