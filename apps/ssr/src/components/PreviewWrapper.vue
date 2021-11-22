<template>
  <div>
    <Spinner v-if="loading" />
    <MessageError v-else-if="graphqlError">
      {{ graphqlError }}.<br />Table: {{ table }}<br />Filter: {{ filter }}
    </MessageError>
    <slot v-else :state="state" />
  </div>
</template>

<script>
import { TableMixin, MessageError, Spinner } from "@mswertz/emx2-styleguide";

export default {
  extends: TableMixin,
  components: {
    MessageError,
    Spinner,
  },
  computed: {
    state() {
      if (this.data && this.data.length > 0) {
        this.graphqlError = null;
        return { table: this.table, row: this.data[0], schema: this.schema };
      } else if (this.table && this.filter) {
        this.graphqlError = "No records found";
      } else {
        return { error: "you need to provide the #/tableName/{key1:value}" };
      }
    },
  },
};
</script>
