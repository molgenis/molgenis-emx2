<template>
  <div>
    <MessageError v-if="error">{{ error }}</MessageError>
    <div v-else style="text-align: center">
      <InputSearch v-if="table" v-model="searchTerms" />
      <Pagination v-model="page" :limit="limit" :count="count" />
      <Spinner v-if="loading" />
      <MolgenisTable
        v-else
        v-model="selectedItems"
        :metadata="tableMetadata"
        :data="data"
        @select="select"
        @deselect="deselect"
      >
        <template v-slot:colheader>
          <slot name="colheader" />
        </template>
        <template v-slot:rowheader="slotProps">
          <slot
            name="rowheader"
            :row="slotProps.row"
            :metadata="metadata"
            :rowkey="slotProps.rowkey"
          />
        </template>
      </MolgenisTable>
    </div>
    <ShowMore title="debug info">
      <pre>
graphql = {{ graphql }}

filter = {{ filter }}

selectedItems =
{{ selectedItems }}

data =
{{ data }}
          
schema =
{{ schema }}
      </pre>
    </ShowMore>
  </div>
</template>

<script>
import TableMixin from "../mixins/TableMixin";
import MolgenisTable from "./MolgenisTable";
import MessageError from "./MessageError";
import InputSearch from "./InputSearch";
import Pagination from "./Pagination.vue";
import Spinner from "./Spinner.vue";
import ShowMore from "./ShowMore";

export default {
  mixins: [TableMixin],
  props: {
    defaultValue: Array,
    selectColumn: String,
    filter: { type: Object, defaultValue: {} }
  },
  components: {
    ShowMore,
    MolgenisTable,
    MessageError,
    InputSearch,
    Pagination,
    Spinner
  },
  data: function() {
    return {
      selectedItems: [],
      page: 1,
      loading: true
    };
  },
  methods: {
    select(value) {
      this.$emit("select", value);
    },
    deselect(value) {
      this.$emit("deselect", value);
    }
  },
  watch: {
    selectedItems() {
      this.$emit("input", this.selectedItems);
    },
    page() {
      this.loading = true;
      this.offset = this.limit * (this.page - 1);
      this.reload();
    }
  }
};
</script>

<docs>
    Example:
    ```
    <!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
    <TableSearch table="Code" graphqlURL="/TestCohortCatalogue/graphql">
        <template v-model="selectedItems" v-slot:rowheader="props">my row action {{props.row.name}}</template>
    </TableSearch>
    Selected: {{selectedItems}}

    ```
    Example with select and default value
    ```
    <template>
        <div>
            <!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
            <TableSearch
                    v-model="selectedItems"
                    table="Pet"
                    :defaultValue="['pooky']"
                    graphqlURL="/pet store/graphql"
            >
                <template v-slot:rowheader="props">my row action {{props.row.name}}</template>
            </TableSearch>
            Selected: {{selectedItems}}
        </div>
    </template>

    <script>
        export default {
            data: function () {
                return {
                    selectedItems: []
                };
            }
        };
    </script>
    ```
    Example with filter:
    ```
    <!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
    <TableSearch table="Variable" :filter="{collection:{equals:{name:'LifeCycle'}}}"
                 graphqlURL="/TestCohortCatalogue/graphql">
        <template v-model="selectedItems" v-slot:rowheader="props">my row action {{props.row.name}}</template>
    </TableSearch>
    Selected: {{selectedItems}}

    ```
</docs>
