<template>
  <div class="list-group mt-1">
    <variable-list-item
      v-for="(variable, index) in variables"
      :key="index"
      :variable="variable"
      :variableDetails="variableDetails[variable.name]"
      @request-variable-detail="fetchVariableDetails(variable.name)"
    >
    </variable-list-item>
  </div>
</template>

<script>
import VariableListItem from "../components/VariableListItem.vue";
import { mapGetters, mapActions } from "vuex";
export default {
  name: "VariableDetailsView",
  components: { VariableListItem },
  computed: {
    ...mapGetters(["variables", "variableCount", "variableDetails"]),
  },
  methods: {
    ...mapActions(["fetchVariableDetails"]),
  },
};
</script>

<style></style>
