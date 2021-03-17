<template>
    <div>
        <MessageError v-if="graphqlError">
            {{ graphqlError }}
        </MessageError>
        <div v-else style="text-align: center">
            <form
                v-if="showHeaderIfNeeded"
                class="form-inline justify-content-between mb-2"
            >
                <InputSearch v-if="table" v-model="searchTerms" />
                <Pagination
                    v-model="page" class="ml-2"
                    :count="count"
                    :limit="limit"
                />
                <SelectionBox
                    v-if="showSelect"
                    :selection="selection"
                    @update:selection="$emit('update:selection', $event)"
                />
            </form>
            <Spinner v-if="loading" />
            <div v-else>
                <TableMolgenis
                    :columns="columnsVisible"
                    :data="data"
                    :metadata="tableMetadata"
                    :selection="selection"
                    :show-select="showSelect"
                    @deselect="deselect"
                    @select="select"
                    @update:selection="$emit('update:selection', $event)"
                >
                    <template #colheader>
                        <slot
                            v-bind="$props"
                            :canEdit="canEdit"
                            :grapqlURL="graphqlURL"
                            name="colheader"
                            :reload="reload"
                        />
                    </template>
                    <template #rowheader="slotProps">
                        <slot
                            :metadata="tableMetadata"
                            name="rowheader"
                            :row="slotProps.row"
                            :rowkey="slotProps.rowkey"
                        />
                    </template>
                </TableMolgenis>
            </div>
        </div>
    </div>
</template>

<script>
import TableMixin from "../mixins/TableMixin.vue";
import TableMolgenis from "./TableMolgenis.vue";
import MessageError from "../forms/MessageError.vue";
import InputSearch from "../forms/InputSearch.vue";
import Pagination from "./Pagination.vue";
import Spinner from "../layout/Spinner.vue";
import SelectionBox from "./SelectionBox.vue";

export default {
  components: {
    SelectionBox,
    TableMolgenis,
    MessageError,
    InputSearch,
    Pagination,
    Spinner,
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
        (c) => this.showColumns == null || this.showColumns.includes(c.name)
      );
    },
  },
  watch: {
    page() {
      this.loading = true;
      this.offset = this.limit * (this.page - 1);
      this.reload();
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
};
</script>
