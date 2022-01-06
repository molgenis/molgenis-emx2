<template>
  <div>
    <i v-if="loading">Loading ...</i>
    <i v-else-if="graphqlError">
      <h1>ERROR: {{ graphqlError }}</h1>
      <div>
        Did you provide all required variables? For example:
        /:table/:filter/view<br />
        For example: http://localhost:9091/#/Pet/{"name":"spike"}/json
        <div>Table: {{ table }}</div>
        <div>Filter: {{ filter }}</div>
      </div>
    </i>
    <slot v-else :state="state" />
  </div>
</template>

<script>
import TableMixin from "../copyfromstyleguide/TableMixin";

/* Used to dynamically load state when developing views in dev server mode.
 In ssr, state is normally loaded server side and statically injected into the views */
export default {
  name: "ClientSideStateLoader",
  extends: TableMixin,
  computed: {
    //override
    graphqlFilter() {
      if (this.filter) {
        //simple filter that finds objects that overlap with the filter
        return { equals: this.filter };
      } else return {};
    },
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
