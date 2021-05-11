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

const selected = []
<p class="mt-1">
  <span style="font-weight: bold">Selected items:</span> {{selected.join(', ')}}
  <span v-if="!selected.length" style="font-style: italic;">None<span>
</p>
<div class="row">
  <div class="col-5">
    <tree-component :items=treeItems v-model="selected"></tree-component>
  </div>
</div>

```
</docs>
