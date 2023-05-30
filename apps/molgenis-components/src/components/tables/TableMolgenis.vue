/* eslint-disable vue/no-unused-components */
<template>
  <div style="max-width: 100%" class="flex-grow-1">
    <table
      class="table table-sm bg-white table-bordered table-hover"
      :class="{ 'table-hover': showSelect }"
    >
      <thead>
        <th slot="header" scope="col" style="width: 1px" v-if="hasColheader">
          <h6 class="mb-0 mt-2 d-inline">#</h6>
          <span style="text-align: left; font-weight: normal">
            <slot name="rowcolheader" />
          </span>
        </th>
        <th
          v-for="col in columnsWithoutMeta"
          :key="col.name + col.showColumn"
          scope="col"
          class="column-drag-header"
          :style="col.showColumn ? '' : 'display: none'"
        >
          <h6
            class="mb-0 align-text-bottom text-nowrap"
            @click="onColumnClick(col)"
          >
            {{ col.name }}
            <slot name="colheader" :col="col" />
          </h6>
        </th>
      </thead>

      <tbody>
        <tr v-if="data && !data.length">
          <td :colspan="columnsWithoutMeta.length + 1" class="alert-warning">
            No results found
          </td>
        </tr>
        <tr
          v-else
          v-for="(row, idx) in data"
          :key="idx + JSON.stringify(row) + isSelected(row)"
          :class="
            (isSelected(row) ? 'table-primary' : 'table-hover') +
            (row['mg_draft'] ? 'alert alert-warning' : '')
          "
        >
          <td v-if="hasColheader">
            <div style="display: flex">
              <div v-if="showSelect" class="form-check form-check-inline mr-1">
                <input
                  type="checkbox"
                  class="form-check-input position-static"
                  :checked="isSelected(row)"
                  @click="onRowClick(row)"
                />
              </div>
              <!--@slot Use this to add values or actions buttons to each row -->
              <slot
                name="rowheader"
                :row="row"
                :metadata="tableMetadata"
                :rowkey="getPrimaryKey(row, tableMetadata)"
              />
            </div>
            <i v-if="row.mg_draft" class="fas fa-user-edit">draft</i>
          </td>

          <td
            v-for="col in columnsWithoutMeta"
            :key="idx + col.name + isSelected(row)"
            style="cursor: pointer"
            :style="col.showColumn ? '' : 'display: none'"
            @click="
              {
                onRowClick(row);
                onCellClick(row, col);
              }
            "
            :class="{ refType: isRefType(col.columnType) }"
          >
            <data-display-cell :data="row[col.id]" :metaData="col" />
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

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

<script>
/**
 * Table that uses MOLGENIS metadata format to configure itself. Provide 'metadata' and 'data' and your table is ready.
 * Can be used without backend to configure a table. Note, columns can be dragged.
 */
import { deepClone, deepEqual, getPrimaryKey, isRefType } from "../utils";
import DataDisplayCell from "./DataDisplayCell.vue";

export default {
  components: { DataDisplayCell },
  props: {
    /** selection, two-way binded*/
    selection: { type: Array, required: false },
    /** column metadata, two-way binded to allow for reorder */
    columns: { type: Array, default: () => [] },
    /** not two way binded, table metadata */
    tableMetadata: { type: Object, required: false },
    data: { type: Array, default: () => [] },
    showSelect: { type: Boolean, default: false },
  },
  computed: {
    countColumns() {
      return this.columnsWithoutMeta.filter((c) => c.showColumn).length;
    },
    columnsWithoutMeta() {
      return this.columns
        ? this.columns.filter((column) => column.columnType !== "HEADING")
        : [];
    },
  },
  methods: {
    hasColheader() {
      return (
        this.showSelect || !!this.$slots.colheader || !!this.$slots.rowheader
      );
    },
    initShowColumn() {
      if (this.columns) {
        let update = this.columns;
        for (var key in update) {
          if (update[key].showColumn == undefined) {
            update[key].showColumn = true;
            this.$emit("update:columns", update);
          }
        }
      }
    },
    getPrimaryKey(row) {
      return getPrimaryKey(row, this.tableMetadata);
    },
    isSelected(row) {
      let key = this.getPrimaryKey(row);
      let found = false;
      if (Array.isArray(this.selection)) {
        this.selection.forEach((s) => {
          if (s != null && this.deepEqual(key, s)) {
            found = true;
          }
        });
      }
      return found;
    },
    isRefType,
    deepEqual,
    onColumnClick(column) {
      this.$emit("column-click", column);
    },
    onCellClick(row, column) {
      const value = row[column.id];
      if (value) {
        this.$emit("cellClick", {
          cellValue: deepClone(value),
          column: deepClone(column),
        });
      }
    },
    onRowClick(row) {
      const key = this.getPrimaryKey(row);
      if (this.showSelect) {
        //deep copy
        let update = deepClone(this.selection);
        if (this.isSelected(row)) {
          /** when a row is deselected */
          update = update.filter(
            (item) =>
              JSON.stringify(item, Object.keys(item).sort()) !==
              JSON.stringify(key, Object.keys(key).sort())
          );
          this.$emit("deselect", key);
        } else {
          /** when a row is selected */
          update.push(key);
          this.$emit("select", key);
        }
        this.$emit("update:selection", update);
      } else {
        this.$emit("rowClick", key);
      }
    },
  },
  created() {
    this.initShowColumn();
  },
};
</script>

<docs>
<template>
  <div>
    <demo-item id="table-molgenis" label="Table Molgenis">
      <label class="font-italic">Local sample data</label>
      <table-molgenis
          :selection.sync="selected"
          :columns.sync="columns"
          :data="data"
          @select="click"
          @deselect="click"
          @click="click"
      >
        <template v-slot:header>columns</template>
        <template v-slot:rowheader="slotProps"> some row content</template>
      </table-molgenis>
      <label class="font-italic">Remote Pet data</label>
      <table-molgenis v-if="remoteTableData"
                      :selection.sync="remoteSelected"
                      :columns.sync="remoteColumns"
                      :data="remoteTableData"
                      @click="click"
      />
      <table-molgenis id="table-molgenis-empty" label="Empty Table Molgenis"/>
    </demo-item>
    <DemoItem>
      <label class="font-italic">Example without data</label>
      <table-molgenis
          @select="click"
          @deselect="click"
          @click="click"
          :data="[]"
          :columns.sync="columns"
      />
    </DemoItem>
    <DemoItem>
      <label class="font-italic">Example without data and columns</label>
      <table-molgenis
          @select="click"
          @deselect="click"
          @click="click"
          :data="[]"
      />
    </DemoItem>
  </div>
</template>

<script>
import Client from "../../../src/client/client.ts";

export default {
  data() {
    return {
      selected: [],
      columns: [
        { id: "col1", name: "col1", columnType: "STRING", key: 1 },
        {
          id: "ref1",
          name: "ref1",
          columnType: "REF",
          refColumns: ["firstName", "lastName"],
        },
        {
          id: "ref_arr1",
          name: "ref_arr1",
          columnType: "REF_ARRAY",
          refColumns: ["firstName", "lastName"],
        },
      ],
      data: [
        {
          col1: "row1",
          ref1: { firstName: "katrien", lastName: "duck" },
          ref_arr1: [
            { firstName: "kwik", lastName: "duck" },
            {
              firstName: "kwek",
              lastName: "duck",
            },
            { firstName: "kwak", lastName: "duck" },
          ],
        },
        {
          col1: "row2",
        },
      ],
      remoteSelected: [],
      remoteColumns: [],
      remoteTableData: null,
    };
  },
  methods: {
    click(value) {
      alert("click " + JSON.stringify(value));
    },
  },
  async mounted() {
    const client = Client.newClient("pet store", this.$axios);
    const remoteMetaData = await client.fetchSchemaMetaData();
    const petColumns = remoteMetaData.tables.find(
      (t) => t.name === "Pet"
    ).columns;
    this.remoteColumns = petColumns.filter((c) => !c.name.startsWith("mg_"));
    this.remoteTableData = (await client.fetchTableData("Pet")).Pet;
  },
};
</script>
</docs>
