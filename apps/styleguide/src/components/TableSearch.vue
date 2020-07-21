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
        :metadata="metadata"
        :data="data"
        @select="select"
        @deselect="deselect"
      >
        <template v-slot:colheader>
          <slot name="colheader" />
        </template>
        <template v-slot:rowheader="slotProps">
          <slot name="rowheader" :row="slotProps.row" :metadata="metadata" />
        </template>
      </MolgenisTable>
    </div>
    {{ data }}
  </div>
</template>

<script>
import TableMixin from "../mixins/TableMixin";
import MolgenisTable from "./MolgenisTable";
import MessageError from "./MessageError";
import InputSearch from "./InputSearch";
import Pagination from "./Pagination.vue";
import Spinner from "./Spinner.vue";

export default {
  extends: TableMixin,
  props: {
    defaultValue: Array,
    selectColumn: String
  },
  components: {
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
    <TableSearch schema="pet%20store" table="Pet">
        <template v-model="selectedItems" v-slot:rowheader="props">my row action {{props.row.name}}</template>
    </TableSearch>

    ```
    Example with select and default value
    ```
    <template>
        <div>
            <TableSearch
                    v-model="selectedItems"
                    schema="pet%20store"
                    table="Pet"
                    :defaultValue="['pooky']"
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
</docs>
