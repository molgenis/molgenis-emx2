<template>
  <div>
    <variable-details v-if="details" :variableDetails="details" />
  </div>
</template>

<script>
import VariableDetails from "../components/VariableDetails.vue";
import { mapGetters } from "vuex";
export default {
  name: "SingleVarDetailsView",
  components: { VariableDetails },
  props: {
    name: String,
  },
  computed: {
    ...mapGetters(["variableDetailsByName"]),
    details() {
      return this.variableDetailsByName(this.name);
    },
  },
};
</script>

<style></style>
