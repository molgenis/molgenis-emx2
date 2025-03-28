<template>
  <tr
    class="hoverContainer"
    :style="column.drop ? 'text-decoration: line-through' : ''"
  >
    <td class="bg-white">
      <IconAction
        v-if="isManager"
        class="moveHandle mr-1 align-middle"
        icon="ellipsis-v"
      />
      <span>
        {{ column.name }}
        <span v-if="column.semantics">
          (<template v-for="(semantics, index) in column.semantics"
            ><template v-if="index > 0">,</template>{{ semantics }}</template
          >)
        </span>
      </span>
      <IconBar class="d-inline-block">
        <ColumnEditModal
          v-if="isManager"
          v-model="column"
          :schema="schema"
          :schemaNames="schemaNames"
          @update:modelValue="$emit('update:modelValue', column)"
          :locales="locales"
          :columnIndex="columnIndex"
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
          tooltip="Add column at this position"
          :locales="locales"
          :columnIndex="columnIndex"
        />
      </IconBar>
    </td>
    <td class="bg-white" v-if="table.subclasses?.length > 0">
      {{ column.table }}
    </td>
    <td class="bg-white">
      <ColumnDefinition :column="column" />
    </td>
    <td class="bg-white">
      <table v-if="column.labels" class="table-borderless">
        <tr v-for="el in column.labels.filter((el) => el.value)">
          <td>{{ el.locale }}:</td>
          <td>{{ el.value }}</td>
        </tr>
      </table>
    </td>
    <td class="bg-white">
      <table v-if="column.descriptions" class="table-borderless">
        <tr v-for="el in column.descriptions.filter((el) => el.value)">
          <td>{{ el.locale }}:</td>
          <td>{{ el.value }}</td>
        </tr>
      </table>
    </td>
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
import ColumnDefinition from "./ColumnDefinition.vue";
import { IconDanger, IconBar, IconAction } from "molgenis-components";

export default {
  components: {
    ColumnEditModal,
    ColumnDefinition,
    IconDanger,
    IconBar,
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
    modelValue: {
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
    locales: {
      type: Array,
    },
    columnIndex: {
      type: Number,
      required: true,
    },
  },
  computed: {
    table() {
      return this.schema.tables.find(
        (table) =>
          //use oldName because otheriwse error on renaming
          //must make sure new tables/subtables also have oldName set!
          table.oldName === this.column.table ||
          table.name === this.column.table ||
          (table.subclasses !== undefined &&
            (table.subclasses
              .map((subclass) => subclass.oldName)
              .includes(this.column.table) ||
              table.subclasses
                .map((subclass) => subclass.name)
                .includes(this.column.table)))
      );
    },
    rootTableName() {
      return this.table.name;
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
    this.column = this.modelValue;
  },
  emits: ["update:modelValue", "add", "delete"],
};
</script>
