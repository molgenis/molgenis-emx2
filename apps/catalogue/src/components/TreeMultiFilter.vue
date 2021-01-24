<template>
  <div>
    <div v-for="idx in columns.keys()">
      <TreeListFilter v-model="columns[idx]" />
    </div>
  </div>
</template>

<script>
import { IconAction } from "@mswertz/emx2-styleguide";
import TreeListFilter from "./TreeListFilter";

export default {
  components: { IconAction, TreeListFilter },
  props: {
    columns: Array,
  },
};
</script>
