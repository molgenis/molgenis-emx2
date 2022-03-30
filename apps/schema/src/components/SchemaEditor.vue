<template>
  <div :key="timestamp">
    <div v-if="schema.tables && schema.tables.length > 0">
      <TableEditor
        v-for="(tableIndex, index) in schema.tables.keys()"
        :key="index"
        v-model="schema.tables[tableIndex]"
        :schema="schema"
      />
    </div>
  </div>
</template>

<script>
import columnTypes from '../columnTypes';
import TableEditor from './TableEditor';

export default {
  components: {
    TableEditor
  },
  props: {
    value: Object
  },
  data() {
    return {
      schema: {},
      columnTypes,
      timestamp: Date.now() //used for updating when sorting
    };
  },
  methods: {
    refBackCandidates(fromTable, toTable) {
      return this.schema.tables
        .filter((t) => t.name === fromTable)
        .map((t) => t.columns)[0]
        .filter((c) => c.refTable === toTable)
        .map((c) => c.name);
    }
  },
  watch: {
    schema: {
      deep: true,
      handler() {
        this.$emit('input', this.schema);
      }
    },
    value() {
      this.schema = this.value;
    }
  },
  created() {
    this.schema = this.value;
  }
};
</script>
