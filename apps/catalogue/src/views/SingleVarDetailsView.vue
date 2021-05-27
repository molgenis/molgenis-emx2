<template>
  <div>
    <variable-details v-if="variable" :variableDetails="variable" />
  </div>
</template>

<script>
import { request } from "graphql-request";
import VariableDetails from "../components/VariableDetails.vue";
import variableDetails from "../store/query/variableDetails.gql";
export default {
  name: "SingleVarDetailsView",
  components: { VariableDetails },
  props: {
    name: String,
  },
  data() {
    return {
      variable: null,
    };
  },
  methods: {
    async fetch(name) {
      const params = { filter: { name: { equals: name } } };
      const resp = await request("graphql", variableDetails, params).catch(
        (e) => console.error(e)
      );
      this.variable = resp.Variables[0];
    },
  },
  created() {
    this.fetch(this.name);
  },
};
</script>

<style></style>
