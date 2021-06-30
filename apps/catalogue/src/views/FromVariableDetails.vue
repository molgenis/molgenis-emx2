<template>
  <div>
    <div v-if="variable">
      <h3>{{ variable.label }}</h3>
      <div class="row">
        <variable-details
          class="col"
          :variableDetails="variable"
          :showMappedBy="false"
        />
      </div>
    </div>
    <variable-details></variable-details>
  </div>
</template>

<script>
import { fetchFromVariableDetails } from "../store/repository/variableRepository";
import VariableDetails from "../components/VariableDetails.vue";
import { mapMutations } from "vuex";

export default {
  name: "FromVariableDetails",
  components: { VariableDetails },
  props: {
    name: String,
    network: String,
    version: String,
    sourceCohort: String,
    fromName: String,
  },
  data() {
    return {
      variable: {},
    };
  },
  methods: {
    ...mapMutations(["setBreadCrumbs"]),
  },
  async created() {
    this.variable = await fetchFromVariableDetails(
      this.fromName,
      this.sourceCohort,
      this.version
    );
    this.setBreadCrumbs([
      { label: "variable explorer", to: { name: "variableDetails" } },
      { label: this.name, to: { name: "singleVariableDetails" } },
      {
        label: this.sourceCohort,
        to: { name: "resourceHarmonizationDetails" },
      },
      { label: this.fromName },
    ]);
  },
};
</script>

<style></style>
