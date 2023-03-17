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
  components: {RowEdit},
  data() {
    return {columnsSplitByHeadings: null};
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
    pkey: {type: Object},
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
    this.columnsSplitByHeadings = splitColumnsByHeadings(
        this.tableMetaData.columns,
        this.visibleColumns
    );
    this.$emit("setPageCount", this.columnsSplitByHeadings.length);
  },
  emits: ["setPageCount", "update:modelValue"],
};

function splitColumnsByHeadings(columns, visibleColumns) {
  return columns.reduce((accum, column) => {
    if (column.columnType === "HEADING") {
      accum.push([{name: column.name}]);
    } else {
      if (accum.length === 0) {
        accum.push([]);
      }
      if (
          !visibleColumns ||
          visibleColumns.find(
              (visibleColumn) => column.name === visibleColumn.name
          )
      ) {
        accum[accum.length - 1].push({name: column.name});
      }
    }
    return accum;
  }, []);
}
</script>
