<template>
  <div v-if="tableMetadata">
    <router-link to="/">< Back to {{ schema.name }}</router-link>
    <h1>{{ table }}</h1>
    <MessageError v-if="error">{{ error }}</MessageError>
    <div class="row flex-nowrap">
      <div class=" col col-mg-4 col-lg-4">
        <FilterSidebar v-if="table" :filters="tableMetadata.columns" />
      </div>
      <div v-if="loading" class="col">
        <Spinner />
      </div>
      <div v-else class="col">
        <div>
          <label>{{ count }} records found</label>
        </div>
        <FilterWells v-if="table" :filters="tableMetadata.columns" />
        <MolgenisTable
          :metadata="tableMetadata"
          :data="data"
          class="table-responsive"
          :key="JSON.stringify(tableMetadata.columns)"
        >
          <template v-slot:colheader>
            <RowButtonAdd :table="table" @close="reload" />
          </template>
          <template v-slot:rowheader="slotProps">
            <RowButtonEdit
              :table="table"
              :pkey="pkey(slotProps.row)"
              @close="reload"
            />
            <RowButtonDelete
              :table="table"
              :pkey="pkey(slotProps.row)"
              @close="reload"
            />
          </template>
        </MolgenisTable>
      </div>
    </div>
    <ShowMore title="debug">
      <br />DEBUG
      <br />
      graphqlFilter = {{ JSON.stringify(graphqlFilter) }}
      <br />
      columns = {{ JSON.stringify(columns) }}
      <br />
      table = {{ table }} }
      <br />
      graphql={{ JSON.stringify(graphql) }}
      <br />
      columnNames = {{ columnNames }}
      <br />
      rows = {{ data }}
      <br />
      <pre>tableMetadata={{ JSON.stringify(tableMetadata, null, "\t") }}</pre>
      <br />
      <pre>data={{ JSON.stringify(data, null, "\t") }}</pre>
    </ShowMore>
  </div>
</template>

<script>
import {
  MolgenisTable,
  FilterSidebar,
  FilterWells,
  IconBar,
  MessageError,
  RowButtonAdd,
  RowButtonDelete,
  RowButtonEdit,
  Spinner,
  TableMixin,
  ShowMore
} from "@mswertz/emx2-styleguide";

export default {
  extends: TableMixin,
  components: {
    Spinner,
    MessageError,
    MolgenisTable,
    FilterSidebar,
    FilterWells,
    RowButtonEdit,
    RowButtonAdd,
    RowButtonDelete,
    IconBar,
    ShowMore
  },
  computed: {
    //overrides from TableMixin
    graphqlFilter() {
      let filter = {};
      if (this.tableMetadata) {
        this.tableMetadata.columns.forEach(col => {
          let conditions = Array.isArray(col.conditions)
            ? col.conditions.filter(f => f !== "" && f != undefined)
            : [];
          if (conditions.length > 0) {
            if (col.columnType.startsWith("STRING")) {
              filter[col.name] = { like: col.conditions };
            } else if (col.columnType.startsWith("BOOL")) {
              filter[col.name] = { equals: col.conditions };
            } else if (col.columnType.startsWith("REF")) {
              filter[col.name] = { _byPrimaryKey: col.conditions };
            } else if (
              [
                "DECIMAL",
                "DECIMAL_ARRAY",
                "INT",
                "INT_ARRAY",
                "DATE",
                "DATE_ARRAY"
              ].includes(col.columnType)
            ) {
              filter[col.name] = {
                between: conditions.flat()
              };
            }
          }
        });
      }
      return filter;
    },
    columns() {
      if (this.tableMetadata && this.tableMetadata.columns) {
        return this.tableMetadata.columns.map(col => col.name);
      }
      return null;
    }
  },
  methods: {
    pkey(row) {
      let result = {};
      if (this.tableMetadata != null) {
        this.tableMetadata.columns.forEach(col => {
          if (col.key == 1) {
            result[col.name] = row[col.name];
          }
        });
      }
      return result;
    }
  }
};
</script>
