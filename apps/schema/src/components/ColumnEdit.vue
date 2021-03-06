<template>
  <LayoutForm v-if="value">
    <InputString
      v-model="column.name"
      :errorMessage="validateName(column.name)"
      label="Name"
    />
    <InputText v-model="column.description" label="Description" />
    <InputSelect
      v-model="column.columnType"
      :options="columnTypes"
      label="Column type"
    />
    <InputSelect
      v-if="column.columnType == 'STRING'"
      v-model="column.columnFormat"
      :options="['', 'HYPERLINK']"
      label="Column format"
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
        :errorMessage="
          column.refTable == undefined || column.name == ''
            ? 'Referenced table is required'
            : undefined
        "
        :options="tables"
        label="Referenced table"
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
      :options="[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]"
      label="Key"
    />
    <InputBoolean v-model="column.required" label="required" />

    <InputText
      v-model="column.validationExpression"
      label="validationExpression"
    />
    <InputText v-model="column.visibleExpression" label="visibleExpression" />
    <InputText
      v-model="column.jsonldType"
      label="jsonldType (should be valid json conform jsonld @type spec)"
    />
  </LayoutForm>
</template>

<script>
import {
  LayoutForm,
  InputText,
  InputString,
  InputBoolean,
  InputSelect,
} from "@mswertz/emx2-styleguide";
import columnTypes from "../columnTypes";

export default {
  components: {
    LayoutForm,
    InputText,
    InputString,
    InputBoolean,
    InputSelect,
  },
  props: {
    /** Column metadata object entered as v-model */
    value: Object,
    /** table column is part of */
    table: Object,
    /** listof tables for references */
    tables: Array,
  },
  methods: {
    validateName(name) {
      if (
        Array.isArray(this.table.columns) &&
        this.table.columns.filter((c) => c.name == name).length != 1
      ) {
        return "Name should be unique";
      }
      if (name == undefined) {
        return "Name is required";
      }
      if (!name.match(/^[a-zA-Z][a-zA-Z0-9_]+$/)) {
        return "Name should start with letter, followed by letter, number or underscore ([a-zA-Z][a-zA-Z0-9_]*)";
      }
    },
  },
  data() {
    return {
      // of type 'column metadata'
      column: null,
      //the options
      columnTypes,
    };
  },
  created() {
    this.column = this.value;
  },
  watch: {
    column() {
      if (this.column != null) {
        this.$emit("input", this.column);
      }
    },
    value() {
      this.column = this.value;
    },
  },
};
</script>
