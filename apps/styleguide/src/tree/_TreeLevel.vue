<template>
  <div class="list-group">
    <tree-item
      v-for="(item, index) in items"
      :key="index"
      :item="item"
      :selectedItemIds="selectedItemIds"
      :handleChange="handleChange"
    />
  </div>
</template>

<script>
import TreeItem from "./_TreeItem.vue";
export default {
  name: "TreeLevel",
  components: { TreeItem },
  props: {
    items: Array,
    selectedItemIds: Array,
    handleChange: Function,
  },
};
</script>
