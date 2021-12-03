<template>
  <div>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <div v-else style="text-align: center">
      <form
        v-if="showHeaderIfNeeded"
        class="form-inline justify-content-between mb-2 bg-white"
      >
        <InputSearch v-if="table" v-model="searchTerms" />
        <Pagination class="ml-2" v-model="page" :limit="limit" :count="count" />
        <SelectionBox
          v-if="showSelect"
          :selection="selection"
          @update:selection="$emit('update:selection', $event)"
        />
      </form>
      <Spinner v-if="loading" />
      <div v-else>
        <TableMolgenis
          :selection="selection"
          @update:selection="$emit('update:selection', $event)"
          :metadata="tableMetadata"
          :columns="columnsVisible"
          :data="data"
          :showSelect="showSelect"
          @select="select"
          @deselect="deselect"
        >
          <template v-slot:header>
            <slot name="colheader" v-bind="$props" />
            <label>{{ count }} records found</label>
          </template>
          <template v-slot:colheader="slotProps">
            <slot
              name="colheader"
              v-bind="$props"
              :canEdit="canEdit"
              :reload="reload"
              :grapqlURL="graphqlURL"
            />
            <RowButtonAdd
              v-if="canEdit && !slotProps.col"
              :table="table"
              :graphqlURL="graphqlURL"
              @close="reload"
              class="d-inline p-0"
            />
          </template>
          <template v-slot:rowheader="slotProps">
            <slot
              name="rowheader"
              :row="slotProps.row"
              :metadata="tableMetadata"
              :rowkey="slotProps.rowkey"
            />
            <RowButtonEdit
              v-if="canEdit"
              :table="table"
              :graphqlURL="graphqlURL"
              :pkey="getPkey(slotProps.row)"
              @close="reload"
            />
            <RowButtonDelete
              v-if="canEdit"
              :table="table"
              :graphqlURL="graphqlURL"
              :pkey="getPkey(slotProps.row)"
              @close="reload"
            />
          </template>
        </TableMolgenis>
      </div>
    </div>
  </div>
</template>

<script>
import TableMixin from "../mixins/TableMixin";
import TableMolgenis from "./TableMolgenis";
import MessageError from "../forms/MessageError";
import InputSearch from "../forms/InputSearch";
import Pagination from "./Pagination.vue";
import Spinner from "../layout/Spinner.vue";
import SelectionBox from "./SelectionBox";
import RowButtonAdd from "./RowButtonAdd";
import RowButtonEdit from "./RowButtonEdit";
import RowButtonDelete from "./RowButtonDelete";

export default {
  components: {
    SelectionBox,
    TableMolgenis,
    MessageError,
    InputSearch,
    Pagination,
    Spinner,
    RowButtonEdit,
    RowButtonDelete,
    RowButtonAdd,
  },
  mixins: [TableMixin],
  props: {
    /** two-way binding of the selection */
    selection: { type: Array, default: () => [] },
    /** enables checkbox to select rows */
    showSelect: {
      type: Boolean,
      default: false,
    },
    showHeader: {
      type: Boolean,
      default: true,
    },
    showColumns: {
      type: Array,
    },
  },
  data: function () {
    return {
      page: 1,
      loading: true,
    };
  },
  computed: {
    showHeaderIfNeeded() {
      return this.showHeader || this.count > this.limit;
    },
    columnsVisible() {
      return this.tableMetadata.columns.filter(
        (c) =>
          (this.showColumns == null && !c.name.startsWith("mg_")) ||
          (this.showColumns != null && this.showColumns.includes(c.name))
      );
    },
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
  },
};
</script>

<docs>
Example:
```
<template>
  <div>
    <!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
    <TableSearch table="Variables" graphqlURL="/CohortsCentral/graphql" :limit="10">
      <template :selection.sync="selectedItems" v-slot:rowheader="props">my row action {{ props.row.name }}</template>
    </TableSearch>
    Selected: {{ selectedItems }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        selectedItems: null
      }
    }
  }
</script>

```
Example with select
```
<template>
  <div>
    <!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
    <TableSearch
        :selection.sync="selectedItems"
        table="Pet"
        :showSelect="true"
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
