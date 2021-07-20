<template>
  <tree-level
    :items="itemsAsTree"
    :selectedItemIds="selectedItemIds"
    @change-keyword-check="handleSelectionChange"
    :handleChange="handleSelectionChange"
  >
  </tree-level>
</template>

<script>
import TreeLevel from "./_TreeLevel.vue";

export default {
  name: "TreeComponent",
  components: { TreeLevel },
  data() {
    return {
      selectedItemIds: this.value,
    };
  },
  props: {
    /**
     * Array of items that form the tree
     */
    items: {
      type: Array,
      default: () => [],
    },
    /**
     * Array of selected item id's
     */
    value: {
      type: Array,
      default: () => [],
    },
  },
  computed: {
    itemsAsTree() {
      // normalize array, fill out empty parents
      const normalized = this.items.map((item) =>
        !item.parent ? { ...item, parent: { name: null } } : item
      );
      // recursive list to tree function
      const nest = (items, name = null) => {
        return items
          .filter((item) => item.parent.name === name)
          .map((item) => ({ ...item, children: nest(items, item.name) }));
      };

      // create tree from list
      return nest(normalized);
    },
  },
  methods: {
    handleSelectionChange(itemId) {
      if (this.selectedItemIds.includes(itemId)) {
        this.selectedItemIds.splice(this.selectedItemIds.indexOf(itemId), 1);
      } else {
        this.selectedItemIds.push(itemId);
      }
      this.$emit("change", this.selectedItemIds);
    },
  },
};
</script>

<docs>
```
<template>
  <div>
    <p class="mt-1">
      <span style="font-weight: bold">Selected items:{{ selected.join(', ') }}</span>
      <span v-if="!selected.length" style="font-style: italic;">None</span>
    </p>
    <div class="row">
      <div class="col-5">
        <TreeComponent :items="treeItems" v-model="selected"></TreeComponent>
      </div>
    </div>
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        selected: [],
        treeItems: [
          {name: 'fruit'},
          {name: 'apple', parent: {name: 'fruit'}},
          {name: 'peer', parent: {name: 'fruit'}},
          {name: 'cars'},
          {name: 'sports', parent: {name: 'cars'}},
          {name: '4 x 4', parent: {name: 'cars'}},
          {name: 'Land Rover', parent: {name: '4 x 4'}},
          {name: 'ferari', parent: {name: 'sports'}},
          {name: 'toys'},
          {name: 'wood', parent: {name: 'toys'}},
          {name: 'Outside toys of children', parent: {name: 'toys'}},
        ]
      }
    }
  }
</script>
```
</docs>
