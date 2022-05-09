<template>
  <div>
    <h1>{{ tableName }} Explorer</h1>
  </div>
</template>

<script>
export default {
  name: "ExplorerTable",
  props: {
    tableName: {
      type: String,
      required: true,
    },
    metadata: {
      type: Object | Function,
    },
  },
};
</script>

<docs>
<template>
  <demo-item>
    <explorer-table tableName="Cohorts"></explorer-table>
  </demo-item>
</template>
</docs>
