<template>
  <nav class="navbar navbar-expand-lg navbar-light bg-light">
    <a class="navbar-brand" href="#">Aggregate</a>
    <ul class="navbar-nav mr-auto">
      <li>
        <InputSelect class="m-0 mr-2" id="column-select" :modelValue="selectedColumnHeader"
          @update:modelValue="$emit('update:selectedColumnHeader', $event)" :options="refColumns" />
      </li>
      <li>
        <InputSelect class="m-0" id="row-select" :modelValue="selectedRowHeader"
          @update:modelValue="$emit('update:selectedRowHeader', $event)" :options="refColumns" />
      </li>
    </ul>
  </nav>
</template>
    
<script>

export default {
  name: "AggregateOptions",
  props: {
    columns: {
      type: Object,
      required: true,
    },
    selectedColumnHeader: {
      type: String,
      required: true,
    },
    selectedRowHeader: {
      type: String,
      required: true,
    }
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
      if (this.refColumns?.length > 0) {
        this.$emit("setAggregateColumns", this.refColumns);
        this.$emit('update:selectedColumnHeader', this.refColumns[0])
        this.$emit('update:selectedRowHeader', this.refColumns[1] || this.refColumns[0])
        this.loading = false;
      }
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
        :columns="columns"
    />
  </demo-item>
</template>

<script>
  export default {
    data() {
      return {
        tableName: 'Pet',
        endpoint: '/pet store/graphql',
        columns: [{name: 'category',columnType: 'REF'}, {name: 'tags',columnType: 'REF'} ]
      };
    },
  };
</script>
</docs>
    