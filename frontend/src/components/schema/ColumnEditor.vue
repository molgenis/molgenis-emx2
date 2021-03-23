<template>
  <tr :key="timestamp" :class="{ 'table-danger': column.drop }">
    <td>
      <IconAction class="hoverIcon moveHandle" icon="ellipsis-v" />
    </td>
    <td style="width: 10em;">
      <InputString
        v-model="column.name"
        :error-message="validateName(column.name)"
        :inplace="true"
      />
    </td>
    <td>
      <InputSelect
        v-model="column.columnType"
        :inplace="true"
        :options="columnTypes"
      />
    </td>
    <td>
      <InputSelect
        v-model="column.key"
        :inplace="true"
        :options="[null, 1, 2, 3, 4, 5, 6, 7, 8, 9]"
      />
    </td>
    <td>
      <InputBoolean v-model="column.required" :inplace="true" />
    </td>
    <td>
      <InputSelect
        v-if="
          column.columnType == 'REF' ||
            column.columnType == 'REF_ARRAY' ||
            column.columnType == 'REFBACK'
        "
        v-model="column.refTable"
        :error-message="column.refTable == null ? 'Required for reference' : ''"
        :inplace="true"
        :options="tableNames()"
      />
      <span v-else class="text-muted small">n/a</span>
    </td>
    <td>
      <InputSelect
        v-if="
          (column.columnType == 'REF' || column.columnType == 'REF_ARRAY') &&
            reflinkCandidates(tableName, column.name).length > 0
        "
        v-model="column.refLink"
        :inplace="true"
        label="refLink"
        :options="reflinkCandidates(tableName, column.name)"
      />
      <span v-else class="text-muted small">n/a</span>
    </td>
    <td v-if="needsMappedByColumn">
      <InputSelect
        v-if="
          column.columnType == 'REFBACK' &&
            refbackCandidates(column.refTable, tableName).length > 1
        "
        v-model="column.mappedBy"
        :inplace="true"
        label="mappedBy"
        :options="refbackCandidates(column.refTable, tableName)"
      />
      <span v-else class="text-muted small">n/a</span>
    </td>
    <td>
      <InputString v-model="column.semantics" :inplace="true" :list="true" />
    </td>
    <td>
      <InputText v-model="column.description" :inplace="true" />
    </td>
    <td>
      <IconDanger
        class="hoverIcon"
        icon="trash"
        icon-danger
        @click="deleteColumn"
      />
    </td>
  </tr>
</template>

<script>
import columnTypes from '../columnTypes'
import {IconAction, IconDanger, InputBoolean, InputSelect, InputString, InputText} from '@/components/ui/index.js'

export default {
  components: {
    IconAction,
    IconDanger,
    InputBoolean,
    InputSelect,
    InputString,
    InputText,
  },
  props: {
    columnIndex: Number,
    needsMappedByColumn: Boolean,
    schema: Object,
    tableName: String,
    value: Object,
  },
  data() {
    return {column: {}, columnTypes: columnTypes, timestamp: Date.now()}
  },
  created() {
    this.column = this.value
    if (this.column) {
      this.column.oldName = this.column.name
    }
  },
  methods: {
    deleteColumn() {
      if (this.column.drop) {
        delete this.column.drop
      } else {
        this.column.drop = true
      }
      this.timestamp = Date.now()
    },
    refbackCandidates(fromTable, toTable) {
      return this.schema.tables
        .filter((t) => t.name === fromTable)
        .map((t) => t.columns)[0]
        .filter((c) => c.refTable === toTable)
        .map((c) => c.name)
    },
    reflinkCandidates(tableName, columnName) {
      let result = this.schema.tables
        .filter((t) => t.name === tableName)
        .map((t) => t.columns)[0]
      if (result) {
        result = result
          .filter(
            (c) =>
              c.name != columnName &&
              (c.columnType == 'REF' || c.columnType == 'REF_ARRAY'),
          )
          .map((c) => c.name)
        result.unshift(null)
        return result
      } else {
        return []
      }
    },
    tableNames() {
      let result = this.schema.tables.map((t) => t.name)
      return result
    },
    validateName(name) {
      if (name == undefined) {
        return 'Name is required'
      }
      if (!name.match(/^[a-zA-Z][a-zA-Z0-9_]+$/)) {
        return 'Name should start with letter, followed by letter, number or underscore ([a-zA-Z][a-zA-Z0-9_]*)'
      }
    },
  },
}
</script>

<style>
.moveHandle:hover {
  cursor: move;
}
</style>
