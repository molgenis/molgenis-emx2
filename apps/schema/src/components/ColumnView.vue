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
        @input="$emit('input', column)"
      />
      <IconDanger class="hoverIcon" icon="trash" @click="deleteColumn" />
      <IconAction
        class="hoverIcon"
        icon="plus"
        @click="$emit('createColumn', column.position)"
      />
    </td>
    <td>
      <span v-if="column.table !== rootTableName">
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
import { IconAction, IconDanger } from "molgenis-components";

export default {
  components: {
    ColumnEditModal,
    IconAction,
    IconDanger,
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
    schema: Object,
  },
  computed: {
    rootTableName() {
      return this.schema.tables.filter(
        (table) =>
          table.name === this.column.table ||
          (table.subclasses !== undefined &&
            table.subclasses
              .map((subclass) => subclass.name)
              .includes(this.column.table))
      )[0].name;
    },
  },
  methods: {
    deleteColumn() {
      if (this.column.drop) {
        delete this.column.drop;
      } else {
        this.column.drop = true;
      }
      this.emitValue();
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
