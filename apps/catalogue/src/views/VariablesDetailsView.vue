<template>
  <div class="mt-1">
    {{ network }}
    <ul v-if="variables.length" class="list-group">
      <variable-list-item
        v-for="(variable, index) in variables"
        :key="index"
        :variable="variable"
        :network="network"
        @request-variable-detail="handleVariableDetailsRequest(variable)"
      />
      <button
        class="btn btn-link mt-2 mb-3"
        v-if="showMoreVisible"
        @click="fetchAdditionalVariables"
      >
        Show more variables
      </button>
    </ul>
    <p v-else-if="!isLoading" class="text-center font-italic pt-3">
      No variables found matching the given filters
    </p>
    <p v-else class="text-center font-italic pt-3">
      <Spinner />
      Fetching variable data..
    </p>
  </div>
</template>

<script>
import { Spinner } from "@mswertz/emx2-styleguide";
import VariableListItem from "../components/VariableListItem.vue";
import { mapGetters, mapActions, mapState } from "vuex";

export default {
  name: "VariableDetailsView",
  components: { Spinner, VariableListItem },
  props: {
    network: String,
  },
  computed: {
    ...mapState(["isLoading"]),
    ...mapGetters(["variables", "variableCount"]),
    showMoreVisible() {
      return this.variables.length < this.variableCount;
    },
  },
  methods: {
    ...mapActions(["fetchVariableDetails", "fetchAdditionalVariables"]),
    async handleVariableDetailsRequest(variable) {
      const result = await this.fetchVariableDetails(variable);
      this.$set(variable, "variableDetails", result);
    },
  },
};
</script>

<style></style>
