<template>
  <tr :class="{ 'table-danger': column.drop }" :key="timestamp">
    <td>
      <IconAction class="hoverIcon moveHandle" icon="ellipsis-v" />
    </td>
    <td style="width: 10em">
      <InputString
        v-model="column.name"
        :inplace="true"
        :errorMessage="validateName(column.name)"
      />
    </td>
    <td>
      <InputSelect
        v-model="column.columnType"
        :options="columnTypes"
        :inplace="true"
      />
    </td>
    <td>
      <InputSelect
        v-model="column.key"
        :options="[null, 1, 2, 3, 4, 5, 6, 7, 8, 9]"
        :inplace="true"
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
          column.columnType == 'REFBACK' ||
          column.columnType == 'ONTOLOGY' ||
          column.columnType == 'ONTOLOGY_ARRAY'
        "
        v-model="column.refTable"
        :options="tableNames()"
        :errorMessage="column.refTable == null ? 'Required for reference' : ''"
        :inplace="true"
      />
      <span v-else class="text-muted small">n/a</span>
    </td>
    <td>
      <InputSelect
        v-if="
          (column.columnType == 'REF' || column.columnType == 'REF_ARRAY') &&
          refLinkCandidates(tableName, column.name).length > 0
        "
        label="refLink"
        v-model="column.refLink"
        :options="refLinkCandidates(tableName, column.name)"
        :inplace="true"
      />
      <span v-else class="text-muted small">n/a</span>
    </td>
    <td>
      <InputSelect
        v-if="
          column.columnType == 'REFBACK' &&
          refBackCandidates(column.refTable, tableName).length > 1
        "
        label="refBack"
        v-model="column.refBack"
        :options="refBackCandidates(column.refTable, tableName)"
        :inplace="true"
      />
      <span v-else class="text-muted small">n/a</span>
    </td>
    <td>
      <InputString v-model="column.semantics" :list="true" :inplace="true" />
    </td>
    <td>
      <InputText v-model="column.description" :inplace="true" />
    </td>
    <td>
      <IconDanger
        IconDanger
        icon="trash"
        class="hoverIcon"
        @click="deleteColumn"
      />
    </td>
  </tr>
</template>

<style>
.moveHandle:hover {
  cursor: move;
}
</style>

<script>
import {
  InputString,
  InputSelect,
  InputBoolean,
  InputText,
  ButtonAlt,
  IconDanger,
  IconAction,
} from "@mswertz/emx2-styleguide";
import columnTypes from "../columnTypes";

export default {
  components: {
    InputString,
    InputSelect,
    InputBoolean,
    InputText,
    ButtonAlt,
    IconDanger,
    IconAction,
  },
  data() {
    return { column: {}, columnTypes: columnTypes, timestamp: Date.now() };
  },
  props: {
    value: Object,
    tableName: String,
    columnIndex: Number,
    schema: Object,
    needsRefBackColumn: Boolean,
  },
  methods: {
    tableNames() {
      let result = this.schema.tables.map((t) => t.name);
      return result;
    },
    deleteColumn() {
      if (this.column.drop) {
        delete this.column.drop;
      } else {
        this.column.drop = true;
      }
      this.timestamp = Date.now();
    },
    refBackCandidates(fromTable, toTable) {
      return this.schema.tables
        .filter((t) => t.name === fromTable)
        .map((t) => t.columns)[0]
        .filter((c) => c.refTable === toTable)
        .map((c) => c.name);
    },
    refLinkCandidates(tableName, columnName) {
      let result = this.schema.tables
        .filter((t) => t.name === tableName)
        .map((t) => t.columns)[0];
      if (result) {
        result = result
          .filter(
            (c) =>
              c.name != columnName &&
              (c.columnType == "REF" || c.columnType == "REF_ARRAY")
          )
          .map((c) => c.name);
        result.unshift(null);
        return result;
      } else {
        return [];
      }
    },
    validateName(name) {
      // if (this.columns.filter((c) => c.name == name).length != 1) {
      //   return "Name should be unique";
      // }
      if (name == undefined) {
        return "Name is required";
      }
      if (!name.match(/^[a-zA-Z][a-zA-Z0-9_]+$/)) {
        return "Name should start with letter, followed by letter, number or underscore ([a-zA-Z][a-zA-Z0-9_]*)";
      }
    },
  },
  created() {
    this.column = this.value;
    if (this.column) {
      this.column.oldName = this.column.name;
    }
  },
};
</script>
