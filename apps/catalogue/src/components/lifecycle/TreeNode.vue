<template>
  <li
      v-show="visible"
      :class="isFolder ? 'ml-4' : 'ml-0'"
  >
    <span v-if="!isFolder">
      <b-checkbox
          :name="id"
          :checked="selected"
      >{{ label }}</b-checkbox>
    </span>
    <span v-else>
      <span @click="toggleOpen">
        <font-awesome-icon
            class="mt-1"
            :icon="icon"
            :list-item="true"
        />{{
          label
        }}
      </span>
      <ul
          v-if="isFolder"
          v-show="isOpen"
          class="fa-ul"
      >
        <tree-node
            v-for="child in children"
            :key="child.id"
            class="item"
            v-bind="child"
        />
      </ul>
    </span>
  </li>
</template>

<script>

export default {
  name: 'TreeNode',
  props: {
    id: {
      type: String,
      required: true
    },
    children: {
      type: Array,
      required: true
    },
    label: {
      type: String,
      required: true
    },
    selected: {
      type: Boolean,
      default: false
    },
    visible: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      isOpen: false
    }
  },
  computed: {
    isFolder () {
      return this.children && this.children.length
    },
    icon () {
      return this.isOpen ? 'folder-open' : 'folder'
    }
  },
  methods: {
    toggleOpen () {
      this.isOpen = !this.isOpen
    }
  }
}
</script>
