<template>
  <tree-level
    :items="itemsAsTree"
    :selectedItems="value"
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
  props: {
    /**
     * Array of items that form the tree
     */
    items: {
      type: Array,
      default: () => [],
    },
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
    handleSelectionChange() {
      console.log("handleSelectionChange");
    },
  },
};
</script>

<docs>
```
const treeItems = [
  { name: 'fruit', definition: 'Fruit' },
  { name: 'apple', definition: 'Apple', parent: {name: 'fruit'} },
  { name: 'peer', definition: 'Peer', parent: {name: 'fruit'} },
  { name: 'cars',definition: 'Cars' },
  { name: 'sports', definition: 'Sports', parent: {name: 'cars'} },
  { name: '4x4', definition: '4 Wheel drive', parent: {name: 'cars'} },
  { name: 'landrover', definition: 'Land Rover', parent: {name: '4x4'} },
  { name: 'ferari', definition: 'Ferari', parent: {name: 'sports'} },
  { name: 'toys', definition: 'Toys' },
  { name: 'wood', definition: 'Wood', parent: {name: 'toys'} },
  { name: 'outside', definition: 'Outside', parent: {name: 'toys'} },
]
<div class="row">
  <div class="col-5">
    <tree-component :items=treeItems></tree-component>
  </div>
</div>
```
</docs>
