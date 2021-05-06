<template>
  <div>
    <search-box
        v-model="search"
        :placeholder="placeholder"
    />
    <ul
        class="fa-ul pt-3"
        @input="selectionChanged"
    >
      <tree-node
          v-for="node in tree()"
          :key="node.id"
          v-bind="node"
      />
    </ul>
    <b-link
        class="toggle-select card-link"
        @click.prevent="toggleSelect"
    >
      {{ toggleSelectText }}
    </b-link>
  </div>
</template>

<script>
import TreeNode from './TreeNode'
import SearchBox from './SearchBox'
import { BLink } from 'bootstrap-vue'

export default {
  components: {
    TreeNode,
    SearchBox,
    BLink
  },
  props: {
    options: {
      type: Function,
      required: true
    },
    value: {
      type: Array,
      default: () => []
    },
    label: {
      type: String,
      required: true
    }
  },
  data () {
    return {
      search: '',
      nodes: []
    }
  },
  computed: {
    placeholder () {
      return `Search ${this.label.toLowerCase()}...`
    },
    toggleSelectText () {
      return this.value.filter(this.visible).length
          ? 'Deselect all'
          : 'Select all'
    },
    treeNodes () {
      return this.nodes
    }
  },
  async mounted () {
    this.nodes = await this.options()
  },
  methods: {
    matches (node) {
      return (
          !this.search.trim().length ||
          node.label.toLowerCase().includes(this.search.toLowerCase())
      )
    },
    matchesUp (id) {
      const node = this.findNode(id)
      return this.matches(node) || (node.parent && this.matchesUp(node.parent))
    },
    matchesDown (node) {
      return (
          this.matches(node) || this.childNodes(node.id).some(this.matchesDown)
      )
    },
    findNode (id) {
      return this.treeNodes.find(it => it.id === id)
    },
    visible (id) {
      const node = this.findNode(id)
      return this.matchesUp(id) || this.matchesDown(node)
    },
    childNodes (id) {
      return this.treeNodes && this.treeNodes.filter(node => node.parent === id)
    },
    tree (parent = null) {
      const children = this.childNodes(parent).map(child => ({
        ...child,
        children: this.tree(child.id),
        selected: this.value.includes(child.id),
        visible: this.visible(child.id)
      }))
      children.sort((a, b) => a.order - b.order)
      return children
    },
    toggleSelect () {
      const visibleValues = this.value.filter(it => this.visible(it))
      if (visibleValues.length) {
        this.$emit(
            'input',
            this.value.filter(it => !this.visible(it))
        )
      } else {
        const leaves = this.treeNodes
            .map(node => node.id)
            .filter(it => this.visible(it))
            .filter(id => !this.tree(id).length)
        this.$emit('input', [...this.value, ...leaves])
      }
    },
    selectionChanged (event) {
      const selection = [...this.value]
      const id = event.target.name
      const index = selection.indexOf(id)
      if (index === -1) {
        selection.push(id)
      } else {
        selection.splice(index, 1)
      }
      this.$emit('input', selection)
    }
  }
}
</script>
<style>
ul.fa-ul {
  margin-left: 0;
}
.card-link {
  font-style: italic;
  font-size: small;
}
</style>
