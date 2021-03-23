<template>
  <div :key="timestamp">
    <a href="#" @click.prevent="createTable"> create table </a>
    <div v-if="schema.tables && schema.tables.length > 0">
      <TableEditor
        v-for="tableIndex in schema.tables.keys()"
        v-model="schema.tables[tableIndex]"
        :schema="schema"
      />
    </div>
    <ShowMore title="debug">
      {{ timestamp }}
      <pre>{{ schema }}</pre>
    </ShowMore>
  </div>
</template>

<script>
import columnTypes from '../columnTypes.vue'
import {ShowMore} from '@/components/ui/index.js'
import TableEditor from './TableEditor.vue'

export default {
  components: {
    ShowMore,
    TableEditor,
  },
  props: {
    value: Object,
  },
  emits: ['input'],
  data() {
    return {
      columnTypes,
      schema: {},
      timestamp: Date.now(), // used for updating when sorting
    }
  },
  watch: {
    schema: {
      deep: true,
      handler() {
        this.$emit('input', this.schema)
      },
    },
    value() {
      this.schema = this.value
    },
  },
  created() {
    this.schema = this.value
  },
  methods: {
    createTable() {
      if (!this.schema.tables) {
        this.schema.tables = []
      }
      let name = 'NewTable'
      this.schema.tables.unshift({
        columns: [],
        name: name,
      })
      this.timestamp = Date.now()
    },
    refbackCandidates(fromTable, toTable) {
      return this.schema.tables
        .filter((t) => t.name === fromTable)
        .map((t) => t.columns)[0]
        .filter((c) => c.refTable === toTable)
        .map((c) => c.name)
    },
  },
}
</script>
