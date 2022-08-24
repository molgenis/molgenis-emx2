<template>
  <tr
    class="hoverContainer"
    :style="column.drop ? 'text-decoration: line-through' : ''"
  >
    <td>
      <span>
        {{ column.name }}
      </span>
      <ColumnEditModal
        v-model="column"
        :schema="schema"
        :subclasses="subclasses"
        @input="$emit('input', column)"
      />
      <IconAction
        class="hoverIcon"
        icon="plus"
        @click="$emit('createColumn', column.position)"
      />
    </td>
    <td>
      <span v-if="column.table != tableName">
        subclass={{ column.table }}
      </span>
      <span v-if="column.refTable">
        {{ column.columnType.toLowerCase() }}({{
          column.refSchema ? column.refSchema + "." : ""
        }}{{ column.refTable
        }}<span v-if="column.refBack">, refBack={{ column.refBack }}</span>
        <span v-if="column.refLink">, refLink={{ column.refLink }}</span
        >)
      </span>
      <span v-else>
        {{ column.columnType.toLowerCase() }}
      </span>
      <span v-if="column.required === true || column.required === 'true'">
        required
      </span>
      <span v-if="column.key">key={{ column.key }}</span>
    </td>
    <td>{{ column.description }}</td>
  </tr>
</template>

<script>
import columnTypes from "../columnTypes.js";
import ColumnEditModal from "./ColumnEditModal.vue";
import { IconAction } from "molgenis-components";

export default {
  components: {
    ColumnEditModal,
    IconAction,
  },
  data() {
    return {
      column: {},
      columnTypes: columnTypes,
      editColumn: false,
    };
  },
  props: {
    value: Object,
    tableName: String,
    subclasses: Array,
    columnIndex: Number,
    schema: Object,
  },
  methods: {
    deleteColumn() {
      if (this.column.drop) {
        delete this.column.drop;
      } else {
        this.column.drop = true;
      }
      this.$emit("input", this.column);
    },
    emitValue() {
      this.$emit("input", this.column);
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
