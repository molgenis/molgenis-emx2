<template>
  <div>
    <RowEdit
      v-if="columnsSplitByHeadings"
      :id="id"
      :value="value"
      :pkey="pkey"
      :tableName="tableName"
      :tableMetaData="tableMetaData"
      :graphqlURL="graphqlURL"
      :visibleColumns="columnsSplitByHeadings[page - 1]"
      :clone="clone"
      @input="$emit('input', $event)"
    />
  </div>
</template>

<script>
import RowEdit from "./RowEdit.vue";

export default {
  name: "EditModalWizard",
  components: { RowEdit },
  data() {
    return { columnsSplitByHeadings: null };
  },
  props: {
    value: {
      type: Object,
      required: true,
    },
    id: {
      type: String,
      required: true,
    },
    page: {
      type: Number,
      default: 1,
    },
    tableName: {
      type: String,
      required: true,
    },
    tableMetaData: {
      type: Object,
      required: true,
    },
    pkey: { type: Object },
    clone: {
      type: Boolean,
      required: false,
    },
    visibleColumns: {
      type: Array,
      required: false,
    },
    graphqlURL: {
      default: "graphql",
      type: String,
    },
  },
  mounted() {
    this.columnsSplitByHeadings = splitColumnsByHeadings(
      this.tableMetaData.columns
    );
    this.$emit("setPageCount", this.columnsSplitByHeadings.length);
  },
};

function splitColumnsByHeadings(columns) {
  return columns.reduce((accum, column) => {
    if (column.columnType === "HEADING") {
      accum.push([{ name: column.name }]);
    } else {
      if (accum.length === 0) {
        accum.push([]);
      }
      accum[accum.length - 1].push({ name: column.name });
    }
    return accum;
  }, []);
}
</script>
