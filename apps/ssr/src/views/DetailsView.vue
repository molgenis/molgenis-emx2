<template>
  <div>
    <RowCard :columns="columns" :row="state.row" :tableName="state.table" />
  </div>
</template>

<script>
import { RowCard } from "@mswertz/emx2-styleguide";

export default {
  props: {
    state: Object,
  },
  components: {
    RowCard,
  },
  computed: {
    columns() {
      return this.state.schema.tables
        .filter((t) => t.name == this.state.table)[0]
        .columns.map((c) => {
          //TODO: huge pitfall that we have to set showColumn
          c.showColumn = true;
          return c;
        });
    },
  },
};
</script>
