<template>
  <tr :style="column.drop ? 'text-decoration: line-through' : ''">
    <td>
      <div class="moveHandle">{{ column.position }}</div>
    </td>
    <td style="width: 10em">
      <InputString
        v-model="column.name"
        :inplace="true"
        :error="validateName(column.name)"
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
      <InputBoolean v-model="column.nullable" :inplace="true" />
    </td>
    <td>
      <InputSelect
        v-if="
          column.columnType == 'REF' ||
          column.columnType == 'REF_ARRAY' ||
          column.columnType == 'REFBACK'
        "
        v-model="column.refTable"
        :options="tableNames()"
        :error="column.refTable == null ? 'Required for reference' : ''"
        :inplace="true"
      />
      <span v-else class="text-muted small">n/a</span>
    </td>
    <td>
      <InputSelect
        v-if="
          (column.columnType == 'REF' || column.columnType == 'REF_ARRAY') &&
          reflinkCandidates(tableName, column.name).length > 0
        "
        label="refLink"
        v-model="column.refLink"
        :options="reflinkCandidates(tableName, column.name)"
        :inplace="true"
      />
      <span v-else class="text-muted small">n/a</span>
    </td>
    <td v-if="needsMappedByColumn">
      <InputSelect
        v-if="
          column.columnType == 'REFBACK' &&
          refbackCandidates(column.refTable, tableName).length > 1
        "
        label="mappedBy"
        v-model="column.mappedBy"
        :options="refbackCandidates(column.refTable, tableName)"
        :inplace="true"
      />
      <span v-else class="text-muted small">n/a</span>
    </td>
    <td>
      <InputString v-model="column.jsonldType" :inplace="true" />
    </td>
    <td>
      <InputText v-model="column.description" :inplace="true" />
    </td>
    <td>
      <IconDanger
        v-if="column.drop"
        IconDanger
        icon="trash"
        class="hoverIcon"
        @click="deleteColumn"
      />
    </td>
  </tr>
</template>

<style scoped>
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
  },
  data() {
    return { column: {}, columnTypes: columnTypes };
  },
  props: {
    value: Object,
    tableName: String,
    columnIndex: Number,
    schema: Object,
    needsMappedByColumn: Boolean,
  },
  methods: {
    tableNames() {
      let result = this.schema.tables.map((t) => t.name);
      console.log("result: " + JSON.stringify(result));
      return result;
    },
    deleteColumn() {
      this.column.drop = true;
    },
    refbackCandidates(fromTable, toTable) {
      return this.schema.tables
        .filter((t) => t.name === fromTable)
        .map((t) => t.columns)[0]
        .filter((c) => c.refTable === toTable)
        .map((c) => c.name);
    },
    reflinkCandidates(tableName, columnName) {
      let result = this.schema.tables
        .filter((t) => t.name === tableName)
        .map((t) => t.columns)[0]
        .filter(
          (c) =>
            c.name != columnName &&
            (c.columnType == "REF" || c.columnType == "REF_ARRAY")
        )
        .map((c) => c.name);
      result.unshift(null);
      console.log(columnName + "=" + result);
      return result;
    },
    validateName(name) {
      console.log("validate");
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
