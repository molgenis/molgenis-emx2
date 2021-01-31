<template>
  <LayoutForm v-if="value">
    <InputString
      v-model="column.name"
      :error="validateName(column.name)"
      label="Name"
    />
    <InputText v-model="column.description" label="Description" />
    <h4>Constraints</h4>
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
    <InputSelect
      v-if="
        column.columnType == 'REF' ||
        column.columnType == 'REF_ARRAY' ||
        column.columnType == 'MREF' ||
        column.columnType == 'REFBACK'
      "
      v-model="column.refTable"
      :error="
        column.refTable == undefined || column.name == ''
          ? 'Referenced table is required'
          : undefined
      "
      :options="tables"
      label="Referenced table"
    />
    <InputSelect
      v-model="column.key"
      :options="[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]"
      label="Key"
    />
    <InputBoolean v-model="column.nullable" label="Nullable" />
    <h4>Expressions</h4>
    <InputText v-model="column.validationExpression" label="Validation" />
    <InputText v-model="column.visibleExpression" label="Visible" />
    <h4>Settings for semantic web</h4>
    <InputText
      v-model="column.jsonldType"
      label="jsonldType (should be valid json conform jsonld @type spec)"
    />
    <div
      v-if="
        column.columnType == 'REF' ||
        column.columnType == 'REF_ARRAY' ||
        column.columnType == 'MREF' ||
        column.columnType == 'REFBACK'
      "
    >
      <h4>Advanced relationship settings</h4>
      <InputString
        v-model="column.refSchema"
        label="refSchema (only needed if referencing outside schema)"
      />
      <InputString
        v-if="column.columnType == 'REFBACK'"
        v-model="column.mappedBy"
        label="Mapped by"
      />
      <InputString
        v-if="
          column.columnType == 'REF' ||
          column.columnType == 'REF_ARRAY' ||
          column.columnType == 'MREF' ||
          column.columnType == 'REFBACK'
        "
        v-model="column.refFrom"
        :list="true"
        label="refFrom"
      />
      <InputString
        v-if="
          column.columnType == 'REF' ||
          column.columnType == 'REF_ARRAY' ||
          column.columnType == 'MREF' ||
          column.columnType == 'REFBACK'
        "
        v-model="column.refTo"
        :list="true"
        label="refTo"
      />
    </div>
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

const columnTypes = [
  "STRING",
  "INT",
  "BOOL",
  "DECIMAL",
  "DATE",
  "DATETIME",
  "REF",
  "REF_ARRAY",
  //depcrecated "MREF",
  "REFBACK",
  "UUID",
  "TEXT",
  "STRING_ARRAY",
  "INT_ARRAY",
  "BOOL_ARRAY",
  "DECIMAL_ARRAY",
  "DATE_ARRAY",
  "DATETIME_ARRAY",
  "UUID_ARRAY",
  "TEXT_ARRAY",
];

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
    table: Object,
    /** listof tables for references */
    tables: Array,
  },
  methods: {
    validateName(name) {
      if (this.table.columns.filter((c) => c.name == name).length != 1) {
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
  },
};
</script>
