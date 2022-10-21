<template>
  <div>
    <RowEdit
      :id="id"
      :value="value"
      :pkey="pkey"
      :tableName="tableName"
      :tableMetaData="tableMetaData"
      :graphqlURL="graphqlURL"
      :visibleColumns="columnsSplitByHeadings[page]"
      :clone="clone"
      @input="$emit('input', $event)"
    />
  </div>
</template>

<script>
export default {
  name: "EditModalWizard",
  components: {},
  data() {
    return {
      page: 0,
      columnsSplitByHeadings: splitColumnsByHeadings(
        this.tableMetaData.columns
      ),
    };
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

<docs>
  <template>
  <DemoItem label="Edit Modal">
    <EditModalWizard
      v-if="showRowEdit"
      id="row-edit"
      v-model="rowData"
      :tableName="tableName"
      :tableMetaData="tableMetaData"
      :graphqlURL="graphqlURL"
    />
  </DemoItem>
</template>
  <script>
export default {
  data: function () {
    return {
      showRowEdit: true,
      tableName: "Pet",
      tableMetaData: {
        columns: [],
      },
      rowData: {},
      graphqlURL: "/pet store/graphql",
    };
  },
  watch: {
    async tableName(newValue, oldValue) {
      if (newValue !== oldValue) {
        this.rowData = {};
        await this.reload();
      }
    },
  },
  methods: {
    async reload() {
      this.showRowEdit = false;
      const client = this.$Client.newClient(this.graphqlURL);
      this.tableMetaData = (await client.fetchMetaData()).tables.find(
        (table) => table.id === this.tableName
      );
      this.showRowEdit = true;
    },
  },
  async mounted() {
    this.reload();
  },
};
</script>
</docs>