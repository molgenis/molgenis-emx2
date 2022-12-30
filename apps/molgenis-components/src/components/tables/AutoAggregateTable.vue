<template>
  <div>
    <AggregateTable v-if="!loading" :table="table" :graphQlEndpoint="graphQlEndpoint" :minimumValue="1"
      :columnHeaderProperties="refColumns" :rowHeaderProperties="refColumns"
      :selectedColumnHeaderProperty="refColumns[0]" columnHeaderNameProperty="name"
      :selectedRowHeaderProperty="refColumns[1] || refColumns[0]" rowHeaderNameProperty="name" />
  </div>
</template>
  
<script>
import AggregateTable from "./AggregateTable.vue";

export default {
  name: "AutoAggregateTable",
  components: { AggregateTable },
  props: {
    graphQlEndpoint: {
      type: String,
      default: "graphql",
    },
    table: {
      type: String,
      required: true,
    },
    minimumValue: {
      type: Number,
      default: 1,
    },
    columns: Object
  },
  data: function () {
    return {
      loading: true,
      refColumns: []
    };
  },
  methods: {
    isRefType(column) {
      return column.columnType.startsWith("REF") || column.columnType.startsWith("ONTOLOGY")
    },
    refTypeColumns(columns) {
      return columns.filter(column => this.isRefType(column)).map(column => column.name)
    },
  },
  created() {
    if (this.columns?.length > 0) {
      this.refColumns = this.refTypeColumns(this.columns)
      console.log(this.refColumns?.length)
      if (this.refColumns?.length > 0)
        this.loading = false;
    }
  },

};
</script>
  
<docs>
<template>
  <demo-item>
    <AutoAggregateTable
        :table="tableName"
        :graphQlEndpoint="endpoint"
        :minimumValue="1"
    />
  </demo-item>
</template>

<script>
  export default {
    data() {
      return {
        tableName: 'Pet',
        endpoint: '/pet store/graphql',
      };
    },
  };
</script>
</docs>
  