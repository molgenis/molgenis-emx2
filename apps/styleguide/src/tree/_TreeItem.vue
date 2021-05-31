<template>
  <div
    class="mg-tree-item list-group-item list-group-item-action text-lowercase"
    @click.stop="handleClick"
  >
    <div :class="{ 'form-check': !hasChildren }">
      <input
        v-if="!hasChildren"
        :id="'check-input-' + item.name"
        class="form-check-input"
        type="checkbox"
        :checked="isSelected"
        @change="handleChange(item.name)"
      />
      <i v-if="hasChildren && isCollapsed" class="mr-2 fa fa-caret-up"></i>
      <i v-if="hasChildren && !isCollapsed" class="mr-2 fa fa-caret-down"></i>
      <label
        class="form-check-label"
        :for="'check-input-' + item.name"
        :class="{ 'mb-1': hasChildren && !isCollapsed }"
      >
        {{ item.name }}
      </label>
      <tree-level
        v-if="hasChildren && !isCollapsed"
        :items="item.children"
        :handleChange="handleChange"
        :selectedItemIds="selectedItemIds"
      ></tree-level>
    </div>
  </div>
</template>

<script>
export default {
  name: "TreeItem",
  components: {
    TreeLevel: () => import("./_TreeLevel.vue"),
  },
  props: {
    item: Object,
    handleChange: Function,
    selectedItemIds: Array,
  },
  data() {
    return {
      isCollapsed: true,
    };
  },
  computed: {
    hasChildren() {
      return this.item.children.length;
    },
    isSelected() {
      return this.selectedItemIds.includes(this.item.name);
    },
  },
  methods: {
    handleClick() {
      if (this.hasChildren) {
        this.isCollapsed = !this.isCollapsed;
      }
    },
  },
};
</script>

<style scoped>
.list-group-item:hover,
.form-check-label:hover,
form-check-input:hover {
  cursor: pointer;
}

.mg-tree-item {
  overflow: hidden;
}

.mg-tree-item label {
  font-size: smaller;
}
</style>
