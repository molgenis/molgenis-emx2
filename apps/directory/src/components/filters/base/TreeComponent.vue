<template>
  <div :key="option.name" v-for="option of sortedOptions">
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
  computed: {
    sortedOptions() {
      if (this.options) {
        const copy = JSON.parse(JSON.stringify(this.options));

        return copy.sort(function (a, b) {
          if (a.code < b.code) {
            return -1;
          }
          if (a.code > b.code) {
            return 1;
          }
          return 0;
        });
      } else return [];
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