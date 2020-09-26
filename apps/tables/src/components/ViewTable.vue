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
        <FilterWells v-if="table" :filters="tableMetadata.columns" />
        <MolgenisTable
          :metadata="tableMetadata"
          :data="data"
          class="table-responsive"
          :key="JSON.stringify(tableMetadata.columns)"
        >
          <template v-slot:header
            ><label>{{ count }} records found</label></template
          >
          <template v-slot:colheader>
            <RowButtonAdd v-if="canEdit" :table="table" @close="reload" />
          </template>
          <template v-slot:rowheader="slotProps">
            <RowButtonEdit
              v-if="canEdit"
              :table="table"
              :pkey="pkey(slotProps.row)"
              @close="reload"
            />
            <RowButtonDelete
              v-if="canEdit"
              :table="table"
              :pkey="pkey(slotProps.row)"
              @close="reload"
            />
          </template>
        </MolgenisTable>
      </div>
    </div>
    <ShowMore title="debug">
      <pre>
      graphqlFilter = {{ JSON.stringify(graphqlFilter) }}

      session = {{ session }}

      columns = {{ JSON.stringify(columns) }}

      table = {{ table }} }

      graphql={{ JSON.stringify(graphql) }}

      columnNames = {{ columnNames }}

      rows = {{ data }}

      tableMetadata={{ JSON.stringify(tableMetadata, null, "\t") }}

    data={{ JSON.stringify(data, null, "\t") }}
    </pre
      >
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
              filter[col.name] = { equals: col.conditions };
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
    },
    canEdit() {
      return (
        this.session.email == "admin" ||
        (this.session.roles &&
          (this.session.roles.includes("Editor") ||
            this.session.roles.includes("Manager")))
      );
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
