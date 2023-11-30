<template>
  <div style="overflow-x: scroll">
    <table class="table table-sm bg-white table-bordered table-hover">
      <thead>
        <th slot="header" scope="col" style="width: 1px" v-if="hasColheader">
          <h6 class="mb-0 mt-2 d-inline">#</h6>
          <span style="text-align: left; font-weight: normal">
            <slot name="rowcolheader" />
          </span>
        </th>
        <th
          v-for="col in dataColumns"
          :key="col.id + col.showColumn"
          scope="col"
          class="column-drag-header"
          :style="col.showColumn ? '' : 'display: none'"
        >
          <h6
            class="mb-0 align-text-bottom text-nowrap"
            @click="onColumnClick(col)"
          >
            {{ col.label }}
            <slot name="colheader" :col="col" />
          </h6>
        </th>
      </thead>

      <tbody>
        <tr v-if="data && !data.length">
          <td :colspan="dataColumns.length + 1" class="alert-warning">
            No results found
          </td>
        </tr>
        <template v-else v-for="row in data">
          <TableRow
            :row="row"
            :columns="dataColumns"
            :tableId="tableId || tableMetadata.id"
            :client="client"
            :showSelect="showSelect"
            :selection="selection"
            @cellClick="$emit('cellClick', $event)"
            @rowClick="$emit('rowClick', $event)"
            @select="onRowSelect"
            @deselect="onRowDeselect"
          >
            <template v-slot:rowheader="slotProps">
              <slot
                name="rowheader"
                :row="slotProps.row"
                :rowKey="slotProps.rowKey"
              />
            </template>
          </TableRow>
        </template>
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
import Client from "../../client/client";
import TableRow from "./TableRow.vue";
import { toRaw } from "vue";

export default {
  components: { TableRow },
  props: {
    /** selection, two-way binded*/
    selection: { type: Array, required: false },
    /** column metadata, two-way binded to allow for reorder */
    columns: { type: Array, default: () => [] },
    /** not two way binded, table metadata */
    tableMetadata: { type: Object, required: false },
    /** if tableMetadata is not supplied table id should be set */
    tableId: { type: String, required: false },
    data: { type: Array, default: () => [] },
    showSelect: { type: Boolean, default: false },
    schemaId: { type: String, required: false },
  },
  data() {
    return {
      client: Client.newClient(this.schemaId),
    };
  },
  computed: {
    dataColumns() {
      const columnsWithoutHeaders = this.columns.filter(
        (column) => column.columnType !== "HEADING"
      );

      return columnsWithoutHeaders.map((column) => {
        if (column.showColumn == undefined) {
          column.showColumn = true;
        }
        return column;
      });
    },
  },
  methods: {
    hasColheader() {
      return this.showSelect || Boolean(this.$slots.colheader);
    },
    onColumnClick(column) {
      this.$emit("column-click", column);
    },
    onRowSelect(rowKey) {
      this.$emit("select", rowKey);
      this.$emit("update:selection", [...toRaw(this.selection), rowKey]);
    },
    onRowDeselect(rowKey) {
      const sortedRowKeyAsString = JSON.stringify(
        rowKey,
        Object.keys(rowKey).sort()
      );
      const deleteIndex = this.selection
        .map((selectionItem) =>
          JSON.stringify(selectionItem, Object.keys(selectionItem).sort())
        )
        .findIndex((selectionItem) => selectionItem === sortedRowKeyAsString);
      this.$emit("deselect", rowKey);
      this.$emit("update:selection", this.selection.toSpliced(deleteIndex, 1));
    },
  },
};
</script>

<docs>
<template>
  <div>
    <DemoItem id="table-molgenis" label="Table Molgenis">
      <label class="font-italic">Remote Pet data</label>
        <div class="border-bottom mb-3 p-2">
      <h5>synced demo props: </h5>
      <div>
        <label for="canSelect" class="pr-1">can select: </label>
        <input type="checkbox" id="canSelect" v-model="canSelect">
      </div>
      <div v-show="canSelect">
      {{ selection }}
      </div>
    </div>
      <table-molgenis 
        v-if="remoteTableData"
        v-model:selection="selection"
        v-model:columns="columns"
        :data="remoteTableData"
        tableId="Pet"
        schemaId="pet store"
        :showSelect="canSelect"
      />
    </DemoItem>
    <DemoItem>
      <label class="font-italic">Example without data</label>
      <table-molgenis
          :data="[]"
          tableId="Pet"
          schemaId="pet store"
          v-model:columns="columns"
      />
    </DemoItem>
    <DemoItem>
      <label class="font-italic">Example without data and columns</label>
      <table-molgenis
          :data="[]"
          tableId="Pet"
          schemaId="pet store"
      />
    </DemoItem>
  </div>
</template>

<script>
import Client from "../../../src/client/client.ts";

export default {
  data() {
    return {
      selection: [],
      columns: [],
      remoteTableData: null,
      canSelect: false,
    };
  },
  async mounted() {
    const client = Client.newClient("pet store");
    const metaData = await client.fetchSchemaMetaData();
    const petColumns = metaData.tables.find(
      (t) => t.id === "Pet"
    ).columns;
    this.columns = petColumns.filter((c) => !c.id.startsWith("mg_"));
    this.remoteTableData = (await client.fetchTableData("Pet")).Pet;
  },
};
</script>
</docs>
