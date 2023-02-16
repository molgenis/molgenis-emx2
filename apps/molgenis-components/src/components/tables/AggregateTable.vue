<template>
  <div>
    <Spinner v-if="loading" class="m-3" />
    <div v-else-if="!refColumns.length" class="alert alert-warning">
      Not enough input to create an aggregate table
    </div>

    <div>
      <div class="aggregate-options container">
        <div class="row">
          <div class="col-1">
            <label class="mr-2 col-form-label" for="aggregate-column-select">
              Column:
            </label>
          </div>
          <div class="col-3">
            <InputSelect
              id="aggregate-column-select"
              v-model="selectedColumn"
              @update:modelValue="fetchData"
              :options="refColumns"
              required
            />
          </div>
        </div>
        <div class="row">
          <div class="col-1">
            <label class="mr-2 col-form-label" for="aggregate-row-select">
              Row:
            </label>
          </div>
          <div class="col-3">
            <InputSelect
              id="aggregate-row-select"
              v-model="selectedRow"
              @update:modelValue="fetchData"
              :options="refColumns"
              required
            />
          </div>
        </div>
      </div>

      <div v-if="noResults" class="alert alert-warning">No results found</div>

      <TableStickyHeaders
        v-else
        :columns="columns"
        :rows="rows"
        :data="aggregateData"
      >
        <template #column="columnProps">
          {{ columnProps.value }}
        </template>
        <template #row="rowProps">
          {{ rowProps.value }}
        </template>
        <template #cell="cell">
          <div v-if="!cell.value" class="text-center text-black-50">-</div>
          <div v-else-if="cell.value < minimumValue">﹤{{ minimumValue }}</div>
          <div v-else>{{ cell.value }}</div>
        </template>
      </TableStickyHeaders>
    </div>
  </div>
</template>

<style>
.aggregate-options .float-right {
  display: none;
}
</style>

<script lang="ts">
import { defineComponent } from "vue";
import TableStickyHeaders from "./TableStickyHeaders.vue";
import IAggregateData from "./IAggregateData";
import Client from "../../client/client";
import InputSelect from "../forms/InputSelect.vue";
import { IColumn } from "../../Interfaces/IColumn";

export default defineComponent({
  name: "AggregateTable",
  components: { TableStickyHeaders },
  props: {
    schemaName: {
      type: String,
      required: true,
    },
    allColumns: {
      type: Array,
      required: true,
    },
    tableName: {
      type: String,
      required: true,
    },
    minimumValue: {
      type: Number,
      default: 1,
    },
    graphqlFilter: {
      type: Object,
      default: {},
    },
  },
  data: function () {
    return {
      selectedColumn: "",
      selectedRow: "",
      refColumns: [] as string[],
      loading: true,
      rows: [] as string[],
      columns: [] as string[],
      aggregateData: {} as IAggregateData,
      noResults: false,
    };
  },
  methods: {
    async fetchData() {
      this.loading = true;
      this.rows = [];
      this.columns = [];
      this.aggregateData = {};
      const client = Client.newClient(this.schemaName);
      const responseData = await client.fetchAggregateData(
        this.tableName,
        {
          name: this.selectedColumn,
          column: "name",
        },
        {
          name: this.selectedRow,
          column: "name",
        },
        this.graphqlFilter
      );
      if (responseData[this.tableName + "_groupBy"]) {
        responseData[this.tableName + "_groupBy"].forEach((item: any) =>
          this.addItem(item)
        );
        this.noResults = !Boolean(this.columns.length);
      } else {
        this.noResults = true;
      }
      this.loading = false;
    },
    addItem(item: any) {
      const column: string = item[this.selectedColumn].name || "not specified";
      const row: string = item[this.selectedRow].name || "not specified";

      if (!this.aggregateData[row]) {
        this.aggregateData[row] = { [column]: item.count };
      } else {
        this.aggregateData[row][column] = item.count;
      }

      if (!this.columns.includes(column)) {
        this.columns.push(column);
      }
      if (!this.rows.includes(row)) {
        this.rows.push(row);
      }
    },
  },
  created() {
    if (this.allColumns.length > 0) {
      this.refColumns = getRefTypeColumns(this.allColumns as IColumn[]);
      if (this.refColumns?.length > 0) {
        this.selectedColumn = this.refColumns[0];
        this.selectedRow = this.refColumns[1] || this.refColumns[0];
      }
    }
    this.fetchData();
  },
});

function getRefTypeColumns(columns: IColumn[]): string[] {
  return columns
    .filter((column: IColumn) => isRefType(column))
    .map((column: IColumn) => column.name);
}

function isRefType(column: IColumn): boolean {
  return (
    column.columnType.startsWith("REF") ||
    column.columnType.startsWith("ONTOLOGY")
  );
}
</script>

<docs>
<template>
  <demo-item>
    <label>AggregateTable</label>
    <AggregateTable
      tableName="Pet"
      schemaName="pet store"
      :allColumns="allColumns"
      :minimumValue="1"
    />
    <label>AggregateTable with filters set</label>
    <AggregateTable
      tableName="Pet"
      schemaName="pet store"
      :allColumns="allColumns"
      :minimumValue="1"
      :graphqlFilter="graphqlFilter"
    />
  </demo-item>
</template>

<script>
export default {
  data() {
    return {
      allColumns: [
        {
          name: "name",
          columnType: "STRING",
        },
        {
          name: "category",
          columnType: "REF",
        },
        {
          name: "tags",
          columnType: "ONTOLOGY_ARRAY",
        },
        {
          name: "orders",
          columnType: "REFBACK",
        },
      ],
      graphqlFilter: { name: { like: ["pooky"] } },
    };
  },
};
</script>
</docs>
