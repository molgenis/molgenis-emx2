<template>
  <tr
    class="hoverContainer"
    :style="column.drop ? 'text-decoration: line-through' : ''"
  >
    <td>
      <span class="moveHandle">
        {{ column.name }}
        <span v-if="column.semantics">
          (<a
            :href="purl"
            target="_blank"
            v-for="purl in column.semantics"
            :key="purl"
            >{{ purl.substring(purl.lastIndexOf("/") + 1) }}</a
          >)
        </span>
      </span>
      <IconBar class="d-inline-block">
        <ColumnEditModal
          v-if="isManager"
          v-model="column"
          :schema="schema"
          :schemaNames="schemaNames"
          @input="$emit('input', column)"
        />
        <IconDanger
          v-if="isManager"
          class="hoverIcon"
          icon="trash"
          @click="deleteColumn"
        />
        <ColumnEditModal
          v-if="isManager"
          :schema="schema"
          :schemaNames="schemaNames"
          operation="add"
          :tableName="column.table"
          @add="addColumn"
        />
      </IconBar>
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

<style scoped>
.moveHandle:hover {
  cursor: move;
}

span {
  word-break: break-word;
}
</style>

<script>
import columnTypes from "../columnTypes.js";
import ColumnEditModal from "./ColumnEditModal.vue";
import { IconDanger, IconBar } from "molgenis-components";

export default {
  components: {
    ColumnEditModal,
    IconDanger,
    IconBar,
  },
  data() {
    return {
      column: {},
      columnTypes: columnTypes,
      editColumn: false,
    };
  },
  props: {
    value: {
      type: Object,
      required: true,
    },
    schema: {
      type: Object,
      required: true,
    },
    schemaNames: {
      type: Array,
      required: true,
    },
    isManager: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    rootTableName() {
      return this.schema.tables.find(
        (table) =>
          table.name === this.column.table ||
          (table.subclasses !== undefined &&
            table.subclasses
              .map((subclass) => subclass.name)
              .includes(this.column.table))
      ).name;
    },
  },
  methods: {
    addColumn(value) {
      this.$emit("add", value);
    },
    deleteColumn() {
      this.$emit("delete");
    },
  },
  created() {
    this.column = this.value;
  },
};
</script>
