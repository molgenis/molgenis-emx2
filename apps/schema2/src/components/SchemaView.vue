<template>
  <div>
    <div
      id="molgenis_tables_container"
      v-if="schema.tables && schema.tables.length > 0"
    >
      <span v-for="(table, index) in schema.tables" :key="index">
        <TableView
          v-if="table.inherit == undefined"
          v-model="schema.tables[index]"
          :schema="schema"
          @input="$emit('input', schema)"
      /></span>
    </div>
    <a id="molgenis_bottom_page_anchor"></a>
  </div>
</template>

<script>
import TableView from "./TableView.vue";

export default {
  components: {
    TableView,
  },
  props: {
    value: Object,
  },
  data() {
    return {
      schema: {},
    };
  },
  created() {
    this.schema = this.value;
  },
};
</script>
