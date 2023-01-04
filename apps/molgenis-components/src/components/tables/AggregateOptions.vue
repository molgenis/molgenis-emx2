<template>
  <div class="btn-group">
    <span>
      <InputSelect id="aggregate-column-select" class="column-select" :modelValue="selectedColumnHeader"
        @update:modelValue="$emit('update:selectedColumnHeader', $event)" :options="refColumns" />
    </span>
    <span>
      <InputSelect id="aggregate-row-select" class="row-select" :modelValue="selectedRowHeader"
        @update:modelValue="$emit('update:selectedRowHeader', $event)" :options="refColumns" />
    </span>
  </div>
</template>

<style>
.column-select select {
  border-color: var(--primary);
  border-right: 0px;
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
  padding-right: 1rem;
  color: var(--primary);
}

.row-select select {
  border-color: var(--primary);
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
  color: var(--primary);
}
</style>
    
<script>
import InputSelect from "../forms/InputSelect.vue";
export default {
  name: "AggregateOptions",
  props: {
    components: { InputSelect },
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
      return (column.columnType.startsWith("REF") && column.required) || (column.columnType.startsWith("ONTOLOGY") && column.required)
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
    