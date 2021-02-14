<template>
  <div>
    <MessageError v-if="error">{{ error }}</MessageError>
    <div v-else style="text-align: center">
      <form class="form-inline justify-content-between mb-2">
        <InputSearch v-if="table" v-model="searchTerms" />
        <Pagination class="ml-2" v-model="page" :limit="limit" :count="count" />
        <SelectionBox v-if="showSelect" v-model="selectedItems" />
      </form>
      <Spinner v-if="loading" />
      <div v-else>
        <TableMolgenis
          v-model="selectedItems"
          :metadata="tableMetadata"
          :data="data"
          :showSelect="showSelect"
          @select="select"
          @deselect="deselect"
        >
          <template v-slot:colheader>
            <slot
              name="colheader"
              v-bind="$props"
              :canEdit="canEdit"
              :reload="reload"
              :grapqlURL="graphqlURL"
            />
          </template>
          <template v-slot:rowheader="slotProps">
            <slot
              name="rowheader"
              :row="slotProps.row"
              :metadata="tableMetadata"
              :rowkey="slotProps.rowkey"
            />
          </template>
        </TableMolgenis>
      </div>
    </div>
    <ShowMore title="debug info">
      <pre>
        canEdit =          {{ canEdit }}

        session =       {{ session }}

graphql = {{ graphql }}

filter = {{ filter }}

selectedItems = {{ selectedItems }}

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
import SelectionBox from "./SelectionBox";

export default {
  components: {
    SelectionBox,
    ShowMore,
    TableMolgenis,
    MessageError,
    InputSearch,
    Pagination,
    Spinner,
  },
  mixins: [TableMixin],
  props: {
    /** v-model value, represents selected item, if showSelect=true*/
    value: { type: Array, default: () => [] },
    /** enables checkbox to select rows */
    showSelect: {
      type: Boolean,
      default: false,
    },
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
    page() {
      this.loading = true;
      this.offset = this.limit * (this.page - 1);
      this.reload();
    },
    selectedItems() {
      this.$emit("input", this.selectedItems);
    },
    value() {
      this.selectedItems = this.value;
    },
  },
  created() {
    this.selectedItems = this.value;
  },
};
</script>

<docs>
Example:
```
<!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
<TableSearch table="Variables" graphqlURL="/CohortsCentral/graphql" :limit="10">
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
<template>
  <div>
    Example with filter:
    <!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
    <TableSearch table="Variables" :filter="{collection:{name:{equals:['LifeCycle','ARS']}}}"
                 graphqlURL="/CohortsCentral/graphql" :showSelect="true">
      <template v-model="selectedItems" v-slot:rowheader="props">my row action {{ props.row.name }}</template>
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
</docs>
