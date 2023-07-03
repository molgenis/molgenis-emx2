<template>
  <div class="mt-1">
    <ul v-if="variables.length" class="list-group">
      <variable-list-item
        v-for="(variable, index) in variables"
        :key="index"
        :variable="variable"
      />
    </ul>
  </div>
</template>

<script>
import { Spinner } from "molgenis-components";
import VariableListItem from "../components/VariableListItem.vue";

export default {
  name: "VariableDetailsView",
  components: { Spinner, VariableListItem },
  props: {
    variables: Array,
  },
};
</script>

<style></style>
