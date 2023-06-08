<script setup>
  defineEmits(["checkbox-update"]);
</script>

<template>
  <div :key="option.name" v-for="option of sortedOptions">
    <tree-branch-component :option="option" @checkbox-update="handleSelection" />
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
    parentSelected: {
      type: Boolean,
      required: false,
    },
    options: {
      type: Array,
      required: true,
    },
  },
  data() {
    return {
      selectedOptions: [],
    };
  },
  methods: {
    handleSelection(update) {
      if(update.checked) {
        this.selectedOptions.push(update.option)
      }
      else {
        this.selectedOptions = this.selectedOptions.filter(selected => selected.label !== update.option.label)
      }
       this.$emit("checkbox-update", update);
    },
  },
  computed: {
    allOptionsSelected() {
      return this.options.length === this.selectedOptions.length;
    },
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