<template>
  <LayoutForm v-if="value">
    <InputString
      v-model="column.name"
      :error-message="validateName(column.name)"
      label="Name"
    />
    <InputText v-model="column.description" label="Description" />
    <InputSelect
      v-model="column.columnType"
      label="Column type"
      :options="columnTypes"
    />
    <InputSelect
      v-if="column.columnType == 'STRING'"
      v-model="column.columnFormat"
      label="Column format"
      :options="['', 'HYPERLINK']"
    />
    <div
      v-if="
        column.columnType == 'REF' ||
          column.columnType == 'REF_ARRAY' ||
          column.columnType == 'MREF' ||
          column.columnType == 'REFBACK'
      "
    >
      <InputString
        v-model="column.refSchema"
        label="refSchema (only needed if referencing outside schema)"
      />
      <InputSelect
        v-model="column.refTable"
        :error-message="
          column.refTable == undefined || column.name == ''
            ? 'Referenced table is required'
            : undefined
        "
        label="Referenced table"
        :options="tables"
      />
      <InputString
        v-if="column.columnType == 'REFBACK'"
        v-model="column.mappedBy"
        label="Mapped by"
      />
      <InputString
        v-if="column.columnType == 'REF' || column.columnType == 'REF_ARRAY'"
        v-model="column.refLink"
        label="refLink"
      />
    </div>
    <InputSelect
      v-model="column.key"
      label="Key"
      :options="[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]"
    />
    <InputBoolean v-model="column.required" label="required" />

    <InputText
      v-model="column.validationExpression"
      label="validationExpression"
    />
    <InputText v-model="column.visibleExpression" label="visibleExpression" />
    <InputString
      v-model="column.semantics"
      label="semantics (should be command separated list of IRI, or keyword 'id')"
      :list="true"
    />
  </LayoutForm>
</template>

<script>
import columnTypes from '@/lib/columnTypes'
import {InputBoolean, InputSelect, InputString, InputText,  LayoutForm} from '@/components/ui/index.js'

export default {
  components: {
    InputBoolean,
    InputSelect,
    InputString,
    InputText,
    LayoutForm,
  },
  props: {
    /** table column is part of */
    table: Object,
    /** listof tables for references */
    tables: Array,
    /** Column metadata object entered as v-model */
    value: Object,
  },
  emits: ['input'],
  data() {
    return {
      // of type 'column metadata'
      column: null,
      // the options
      columnTypes,
    }
  },
  watch: {
    column() {
      if (this.column != null) {
        this.$emit('input', this.column)
      }
    },
    value() {
      this.column = this.value
    },
  },
  created() {
    this.column = this.value
  },
  methods: {
    validateName(name) {
      if (
        Array.isArray(this.table.columns) &&
        this.table.columns.filter((c) => c.name == name).length != 1
      ) {
        return 'Name should be unique'
      }
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
