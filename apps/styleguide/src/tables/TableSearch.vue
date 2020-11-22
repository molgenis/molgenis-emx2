<template>
  <div>
    <MessageError v-if="error">{{ error }}</MessageError>
    <div v-else style="text-align: center">
      <InputSearch v-if="table" v-model="searchTerms" />
      <Pagination v-model="page" :limit="limit" :count="count" />
      <Spinner v-if="loading" />
      <TableMolgenis
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
      </TableMolgenis>
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
import TableMolgenis from "./TableMolgenis";
import MessageError from "../forms/MessageError";
import InputSearch from "../forms/InputSearch";
import Pagination from "./Pagination.vue";
import Spinner from "../layout/Spinner.vue";
import ShowMore from "../layout/ShowMore";

export default {
  mixins: [TableMixin],
  props: {
    defaultValue: Array,
    selectColumn: String,
    filter: { type: Object, defaultValue: {} },
  },
  components: {
    ShowMore,
    TableMolgenis,
    MessageError,
    InputSearch,
    Pagination,
    Spinner,
  },
  data: function () {
    return {
      selectedItems: [],
      page: 1,
      loading: true,
    };
  },
  methods: {
    select(value) {
      this.$emit("select", value);
    },
    deselect(value) {
      this.$emit("deselect", value);
    },
  },
  watch: {
    selectedItems() {
      this.$emit("input", this.selectedItems);
    },
    page() {
      this.loading = true;
      this.offset = this.limit * (this.page - 1);
      this.reload();
    },
  },
};
</script>

<docs>
Example:
```
<!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
<TableSearch table="AbstractVariable" graphqlURL="/TestCohortCatalogue/graphql">
  <template v-model="selectedItems" v-slot:rowheader="props">my row action {{ props.row.name }}</template>
</TableSearch>
Selected: {{ selectedItems }}

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
      <template v-slot:rowheader="props">my row action {{ props.row.name }}</template>
    </TableSearch>
    Selected: {{ selectedItems }}
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
<TableSearch table="Code" :filter="{collection:{equals:{name:'LifeCycle'}}}"
             graphqlURL="/TestCohortCatalogue/graphql">
  <template v-model="selectedItems" v-slot:rowheader="props">my row action {{ props.row.name }}</template>
</TableSearch>
Selected: {{ selectedItems }}

```
</docs>
