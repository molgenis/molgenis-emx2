<template>
  <div>
    <RowEdit
      v-if="columnsSplitByHeadings"
      :id="id"
      :modelValue="modelValue"
      :pkey="pkey"
      :tableName="tableName"
      :tableMetaData="tableMetaData"
      :schemaMetaData="schemaMetaData"
      :visibleColumns="columnsSplitByHeadings[page - 1]"
      :clone="clone"
      :locale="locale"
      @update:modelValue="$emit('update:modelValue', $event)"
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
    modelValue: {
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
    schemaMetaData: {
      type: Object,
      required: false,
    },
    locale: {
      type: String,
      default: () => "en",
    },
  },
  mounted() {
    this.columnsSplitByHeadings = splitColumnNamesByHeadings(
      filterVisibleColumns(this.tableMetaData.columns, this.visibleColumns)
    );
    this.$emit("setPageCount", this.columnsSplitByHeadings.length);
  },
  emits: ["setPageCount", "update:modelValue"],
};

function filterVisibleColumns(columns, visibleColumns) {
  if (!visibleColumns) {
    return columns;
  } else {
    return columns.filter((column) => visibleColumns.includes(column.name));
  }
}

function splitColumnNamesByHeadings(columns) {
  return columns.reduce((accum, column) => {
    if (column.columnType === "HEADING") {
      accum.push([column.name]);
    } else {
      if (accum.length === 0) {
        accum.push([]);
      }
      accum[accum.length - 1].push(column.name);
    }
    return accum;
  }, []);
}
</script>
