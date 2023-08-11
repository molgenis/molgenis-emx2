<template>
  <div>
    <Spinner v-if="loading" class="m-3" />
    <div v-else-if="!refColumnNames.length" class="alert alert-warning">
      Not enough input to create an aggregate table
    </div>

    <div class="border d-inline-block p-2 bg-white">
      <div class="aggregate-options">
        <table>
          <tr>
            <td>
              <label
                class="mx-2 col-form-label form-group mb-0 mr-3"
                for="aggregate-column-select"
              >
                Column:
              </label>
            </td>
            <td>
              <InputSelect
                class="mb-0"
                id="aggregate-column-select"
                v-model="selectedColumn"
                @update:modelValue="fetchData"
                :options="refColumnNames"
                required
              />
            </td>
          </tr>
          <tr>
            <td>
              <label
                class="mx-2 col-form-label form-group mb-0"
                for="aggregate-row-select"
              >
                Row:
              </label>
            </td>
            <td>
              <InputSelect
                class="mb-2"
                id="aggregate-row-select"
                v-model="selectedRow"
                @update:modelValue="fetchData"
                :options="refColumnNames"
                required
              />
            </td>
          </tr>
        </table>
      </div>

      <div v-if="errorMessage" class="alert alert-danger">
        {{ errorMessage }}
      </div>
      <div v-else-if="noResults" class="alert alert-warning">
        No results found
      </div>

      <TableStickyHeaders
        v-else
        :columns="columns"
        :rows="rows"
        :data="aggregateData"
        class="mb-n3"
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
import { IColumn } from "../../Interfaces/IColumn";
import Client from "../../client/client";
import InputSelect from "../forms/InputSelect.vue";
import { convertToCamelCase } from "../utils";
import IAggregateData from "./IAggregateData";
import TableStickyHeaders from "./TableStickyHeaders.vue";

export default defineComponent({
  name: "AggregateTable",
  components: { TableStickyHeaders, InputSelect },
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
      refColumnNames: [] as string[],
      loading: true,
      rows: [] as string[],
      columns: [] as string[],
      aggregateData: {} as IAggregateData,
      noResults: false,
      errorMessage: undefined,
    };
  },
  methods: {
    async fetchData() {
      this.loading = true;
      this.errorMessage = undefined;
      this.rows = [];
      this.columns = [];
      this.aggregateData = {};
      const client = Client.newClient(this.schemaName);
      const responseData = await client
        .fetchAggregateData(
          this.tableName,
          getRefColumnColumn(this.selectedColumn, this.allColumns as IColumn[]),
          getRefColumnColumn(this.selectedRow, this.allColumns as IColumn[]),
          this.graphqlFilter
        )
        .catch((error) => {
          this.errorMessage = error;
        });
      if (responseData && responseData[this.tableName + "_groupBy"]) {
        responseData[this.tableName + "_groupBy"].forEach((item: any) =>
          this.addItem(item)
        );
        this.noResults = !this.columns.length;
      } else {
        this.noResults = true;
      }
      this.loading = false;
    },
    addItem(item: any) {
      const column: string =
        item[convertToCamelCase(this.selectedColumn)].name || "not specified";
      const row: string =
        item[convertToCamelCase(this.selectedRow)].name || "not specified";

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
      this.refColumnNames = getRefTypeColumnNames(this.allColumns as IColumn[]);
      if (this.refColumnNames?.length > 0) {
        this.selectedColumn = this.refColumnNames[0];
        this.selectedRow = this.refColumnNames[1] || this.refColumnNames[0];
      }
    }
    this.fetchData();
  },
});

function getRefTypeColumnNames(columns: IColumn[]): string[] {
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

function getRefColumnColumn(
  columnName: string,
  columns: IColumn[]
): { name: string; column: string } {
  const currentColumn = columns.find((column) => column.name === columnName);
  //TODO make less hacky
  return {
    name: currentColumn?.id || "",
    column:
      currentColumn?.refLabelDefault?.replace("${", "").replace("}", "") || "",
  };
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
          id: "name",
          columnType: "STRING",
          key: 1,
        },
        {
          name: "category bla",
          id: "categoryBla",
          columnType: "REF",
          refTable: "Category",
          refLabelDefault: "${name}"
        },
        {
          name: "tags",
          id: "tags",
          columnType: "ONTOLOGY_ARRAY",
          refLabelDefault: "${name}"
        },
        {
          name: "orders",
          id: "orders",
          columnType: "REFBACK",
          refBack: "pet",
          refTable: "Order",
          refLabelDefault: "${orderId}"
        },
      ],
      graphqlFilter: { name: { like: ["pooky"] } },
    };
  },
};
</script>
</docs>
