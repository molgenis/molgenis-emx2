<template>
  <LayoutForm v-if="value">
    <div
      v-if="!hideNameDescription"
      class="d-flex align-content-around flex-wrap"
    >
      <div class="col-4">
        <InputString v-model="column.name" label="columnName" />
      </div>
      <div class="col-8">
        <InputText v-model="column.description" label="description" />
      </div>
    </div>
    <div class="d-flex align-content-around flex-wrap">
      <div class="col-4">
        <InputSelect
          v-model="column.columnType"
          :options="columnTypes"
          label="columnType"
        />
      </div>
      <div class="col-4" v-if="column.columnType != 'CONSTANT'">
        <InputBoolean
          v-model="column.required"
          :label="column.visibleIf ? 'required (if visible)' : 'required'"
        />
      </div>
      <div class="col-4" v-if="column.columnType != 'CONSTANT'">
        <InputSelect
          v-model="column.key"
          :options="[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]"
          label="key"
        />
      </div>
      <div
        class="col-4"
        v-if="
          column.columnType == 'REF' ||
          column.columnType == 'REF_ARRAY' ||
          column.columnType == 'REFBACK' ||
          column.columnType == 'ONTOLOGY' ||
          column.columnType == 'ONTOLOGY_ARRAY'
        "
      >
        <InputSelect
          v-model="column.refTable"
          :errorMessage="
            column.refTable == undefined || column.name == ''
              ? 'Referenced table is required'
              : undefined
          "
          :options="tableNames"
          label="refTable"
        />
      </div>
      <div
        class="col-4"
        v-if="
          column.columnType == 'REFBACK' &&
          refBackCandidates(column.refTable, table.name).length > 1
        "
      >
        <InputSelect
          label="refBack"
          v-model="column.refBack"
          :options="refBackCandidates(column.refTable, table.name)"
        />
      </div>
      <div
        class="col-4"
        v-if="
          column.refTable &&
          (column.columnType == 'REF' || column.columnType == 'REF_ARRAY')
        "
      >
        <InputString v-model="column.refLink" label="refLink" />
      </div>
    </div>
    <div class="d-flex align-content-around flex-wrap">
      <div class="col-4" v-if="column.columnType != 'CONSTANT'">
        <InputText
          v-model="column.validationExpression"
          label="validationExpression (NOT FUNCTIONAL YET!)"
          description="Example: name == 'John'"
        />
      </div>
      <div class="col-4" v-if="column.columnType != 'CONSTANT'">
        <InputText
          v-model="column.validationMessage"
          label="validationMessage  (NOT FUNCTIONAL YET!)"
          description="Example: name must be John'"
        />
      </div>
      <div class="col-4">
        <InputText
          v-model="column.visibleIf"
          label="visibleIf (NOT FUNCTIONAL YET!)"
          description="other > ''"
        />
      </div>
      <div class="col-4">
        <InputString
          v-model="column.semantics"
          :list="true"
          label="semantics"
        />
      </div>
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
    /** schema */
    schema: Object,
    /** hide name and description */
    hideNameDescription: Boolean,
  },
  computed: {
    tableNames() {
      let result = this.schema.tables.map((t) => t.name);
      return result;
    },
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
    refBackCandidates(fromTable, toTable) {
      return this.schema.tables
        .filter((t) => t.name === fromTable)
        .map((t) => t.columns)[0]
        .filter((c) => c.refTable === toTable)
        .map((c) => c.name);
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
