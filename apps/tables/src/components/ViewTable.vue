<template>
  <div>
    <router-link v-if="schema" to="/">< Back to {{ schema.name }}</router-link>
    <TableExplorer :table="table" />
  </div>
</template>

<script>
import { TableExplorer } from "@mswertz/emx2-styleguide";

export default {
  props: {
    table: String,
    schema: Object,
  },
  components: {
    TableExplorer,
  },
};
</script>
