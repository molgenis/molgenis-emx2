<template>
  <div>
    <div v-for="idx in columns.keys()" :key="idx">
      <TreeListFilter v-model="columns[idx]" />
    </div>
  </div>
</template>

<script>
import TreeListFilter from "./TreeListFilter";

export default {
  components: { TreeListFilter },
  props: {
    columns: Array,
  },
};
</script>
