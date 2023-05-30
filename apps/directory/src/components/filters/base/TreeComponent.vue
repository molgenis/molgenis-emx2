<template>
  <div :key="option.name" v-for="option of options">
    <tree-branch-component :option="option" />
  </div>
</template>

<script>
import TreeBranchComponent from "./TreeBranchComponent.vue";
export default {
  name: "TreeComponent",
  components: {
    TreeBranchComponent,
  },
  props: {
    options: {
      type: Array,
      required: true,
    },
  },
};
</script>
<style scoped>
ul {
  margin-right: 1rem;
  list-style-type: none;
}
</style>