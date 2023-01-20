<template>
  <div>
    <nav class="navbar navbar-expand-lg navbar-light bg-light">
      <a class="navbar-brand" href="#">Aggregate</a>
      <AggregateOptions
        class="mt-3"
        :columns="columns"
        @setAggregateColumns="aggregateColumns = $event"
        v-model:selectedColumnHeader="selectedColumnHeader"
        v-model:selectedRowHeader="selectedRowHeader"
      />
    </nav>

    <AggregateTable
      v-if="aggregateColumns?.length > 0"
      :tableName="tableName"
      :schemaName="schemaName"
      :minimumValue="1"
      :columnHeaderProperties="aggregateColumns"
      :rowHeaderProperties="aggregateColumns"
      :selectedColumnHeaderProperty="selectedColumnHeader"
      columnHeaderNameProperty="name"
      :selectedRowHeaderProperty="selectedRowHeader"
      rowHeaderNameProperty="name"
    />
  </div>
</template>

<script lang="ts">
import AggregateOptions from "./AggregateOptions.vue";
import AggregateTable from "./AggregateTable.vue";
import { defineComponent } from "vue";

export default defineComponent({
  name: "AutoAggregateTable",
  components: { AggregateOptions, AggregateTable },
  props: {
    schemaName: {
      type: String,
      required: true,
    },
    tableName: {
      type: String,
      required: true,
    },
    minimumValue: {
      type: Number,
      default: 1,
    },
    columns: {
      type: Array,
      required: true,
    },
  },
  data: function () {
    return {
      loading: true,
      aggregateColumns: [] as string[],
      selectedColumnHeader: "",
      selectedRowHeader: "",
    };
  },
});
</script>

<docs>
<template>
  <demo-item>
    <AutoAggregateTable
        :tableName="tableName"
        :schemaName="schemaName"
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
        schemaName: 'pet store',
        columns: [{name: 'category', columnType: 'REF', required: true}, {name: 'tags', columnType: 'REF', required: true} ]
      };
    },
  };
</script>
</docs>
