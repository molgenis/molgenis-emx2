<template>
  <tr
    :class="
      (isSelected ? 'table-primary' : 'table-hover') +
      (row['mg_draft'] ? 'alert alert-warning' : '')
    "
  >
    <td v-if="hasRowheader">
      <div style="display: flex">
        <div v-if="showSelect" class="form-check form-check-inline mr-1">
          <input
            type="checkbox"
            class="form-check-input position-static"
            :checked="isSelected"
            @click="onRowClick"
          />
        </div>
        <!--@slot Use this to add values or actions buttons to each row -->
        <slot name="rowheader" :row="row" :rowKey="rowKey" />
      </div>
      <i v-if="row.mg_draft" class="fas fa-user-edit">draft</i>
    </td>

    <td
      v-for="col in columns"
      :key="col.name + isSelected"
      style="cursor: pointer"
      :style="col.showColumn ? '' : 'display: none'"
      @click="
        {
          onRowClick();
          onCellClick(col);
        }
      "
      :class="{ refType: isRefType(col.columnType) }"
    >
      <data-display-cell :data="row[col.id]" :metaData="col" />
    </td>
  </tr>
</template>

<script>
import { deepClone, deepEqual, isRefType } from "../utils";
import DataDisplayCell from "./DataDisplayCell.vue";
import { toRaw } from "vue";

export default {
  name: "TableRow",
  components: { DataDisplayCell },
  props: {
    row: {
      type: Object,
      required: true,
    },
    columns: { type: Array, required: true },
    tableName: { type: String, required: true },
    client: { type: Object, required: true },
    showSelect: { type: Boolean, required: false },
    selection: { type: Array, required: false },
  },
  data() {
    return {
      rowKey: null,
    };
  },
  computed: {
    isSelected() {
      return Boolean(
        this.selection?.find((selectionItem) => {
          return (
            selectionItem &&
            this.rowKey &&
            deepEqual(selectionItem, this.rowKey)
          );
        })
      );
    },
  },
  methods: {
    isRefType,
    async convertRowToPrimaryKey(row) {
      return await this.client.convertRowToPrimaryKey(row, this.tableName);
    },
    onColumnClick(column) {
      this.$emit("column-click", column);
    },
    onCellClick(column) {
      const value = this.row[column.id];
      if (value) {
        this.$emit("cellClick", {
          cellValue: deepClone(value),
          column: deepClone(column),
        });
      }
    },
    onRowClick() {
      if (this.showSelect) {
        const selectionEvent = this.isSelected ? "deselect" : "select";
        this.$emit(selectionEvent, toRaw(this.rowKey));
      } else {
        this.$emit("rowClick", toRaw(this.rowKey));
      }
    },
    hasRowheader() {
      return this.showSelect || Boolean(this.$slots.rowheader);
    },
  },
  mounted() {
    this.client
      .convertRowToPrimaryKey(this.row, this.tableName)
      .then((result) => {
        this.rowKey = result;
      })
      .catch((err) => {
        console.log(err);
      });
  },
};
</script>

<style scoped>
th {
  background-color: white;
}

.column-drag-header:hover {
  cursor: grab;
}

.table .refType {
  color: var(--primary);
}
.table .refType:hover {
  background-color: rgba(0, 0, 0, 0.05);
  text-decoration: underline;
}
</style>
