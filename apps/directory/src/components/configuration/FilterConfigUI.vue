<template>
  <div>
    <div class="mb-3">
      <h3>Filters</h3>
      <div class="d-flex justify-content-between flex-wrap">
        <small class="d-inline-block">Rearrange the filters by dragging and dropping. Unchecking a filter
          means it will be shown under <i>More filters</i> by default.</small>
        <button @click="emitAdd" class="btn btn-info mt-2">
          Add new filter
          <span class="fa fa-plus fa-lg ml-1" aria-hidden="true"></span>
        </button>
      </div>
    </div>
    <draggable
      :list="appConfig.filterFacets"
      class="list-group"
      ghost-class="ghost"
      @start="dragStart()"
      @end="sync()">
      <div
        @click="activeFilter = element.name"
        class="list-group-item d-flex"
        :class="{ editing: index === filterIndex }"
        v-for="(element, index) in appConfig.filterFacets"
        :key="element.name">
        {{ index + 1 }}. {{ element.label || element.name }}
        <small class="ml-auto" v-if="element.builtIn">Rearranging this has no effect in the application.</small>
        <label v-if="!element.builtIn" class="ml-auto"><input
            type="checkbox"
            v-model="appConfig.filterFacets[index].showFacet"
            @change="sync"/></label>
        <span
          v-if="!element.builtIn"
          @click="editFilter(index)"
          class="fa fa-pencil-square-o edit-button fa-lg"
          aria-hidden="true"></span>
      </div>
    </draggable>
  </div>
</template>

<script>
import draggable from 'vuedraggable'

export default {
  name: 'simple',
  components: {
    draggable
  },
  props: {
    config: {
      type: Object,
      required: () => true
    }
  },
  data () {
    return {
      appConfig: {},
      activeFilter: '',
      dragging: false,
      filterIndex: -1
    }
  },
  watch: {
    config () {
      this.setData()
    }
  },
  methods: {
    dragStart () {
      this.dragging = true
      this.editFilter(-1)
    },
    setData () {
      this.appConfig = Object.assign({}, this.config)
    },
    sync () {
      this.draggable = false
      this.$emit('update', this.appConfig)
    },
    editFilter (index) {
      /** reset when toggled */
      if (this.filterIndex === index) {
        index = -1
      }

      this.filterIndex = index
      this.$emit('edit', index)
    },
    emitAdd () {
      this.$emit('add')
    }
  },
  mounted () {
    this.setData()
  }
}
</script>
<style scoped>
.list-group-item:hover {
  cursor: grab;
}

.ghost {
  opacity: 0.5;
  background: #c8ebfb;
}

.edit-button {
  position: relative;
  top: 0.3rem;
  margin-left: 1rem;
}

.edit-button:hover {
  cursor: pointer;
  color: black;
}

.editing {
  box-shadow: inset 0.5px 0.5px 0 black, inset -1.5px -1.5px 0 black;
}
</style>
