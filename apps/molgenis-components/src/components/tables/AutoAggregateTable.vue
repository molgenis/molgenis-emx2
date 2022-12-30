<template>
  <div>
    {{ selectedColumnHeader }}
    {{ selectedRowHeader }}
    <AggregateOptions :columns="columns" @setAggregateColumns="aggregateColumns = $event"
      v-model:selectedColumnHeader="selectedColumnHeader" v-model:selectedRowHeader="selectedRowHeader" />

    <AggregateTable v-if="aggregateColumns?.length > 0" :table="table" :graphQlEndpoint="graphQlEndpoint"
      :minimumValue="1" :columnHeaderProperties="aggregateColumns" :rowHeaderProperties="aggregateColumns"
      :selectedColumnHeaderProperty="selectedColumnHeader" columnHeaderNameProperty="name"
      :selectedRowHeaderProperty="selectedRowHeader" rowHeaderNameProperty="name" />
  </div>
</template>
  
<script>
import AggregateOptions from "./AggregateOptions.vue";
import AggregateTable from "./AggregateTable.vue";

export default {
  name: "AutoAggregateTable",
  components: { AggregateOptions, AggregateTable },
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
    columns: {
      type: Object,
      required: true,
    }
  },
  data: function () {
    return {
      loading: true,
      aggregateColumns: [],
      selectedColumnHeader: "",
      selectedRowHeader: ""
    };
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
  