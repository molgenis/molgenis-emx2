<template>
  <div v-if="visible" class="filter-header mb-2 pt-2 border-top bg-white">
    <div
      class="d-flex flex-row justify-content-between"
      @click="
        expanded
          ? $emit('update:expanded', false)
          : $emit('update:expanded', true)
      "
    >
      <h6 class="mb-0 font-weight-bold p-2">
        {{ title }}
        <span v-if="count > 0" class="badge badge-secondary">{{ count }}</span>
      </h6>
      <IconAction
        class="ml-auto"
        :icon="expanded ? 'angle-up' : 'angle-down'"
      />
    </div>
    <slot name="header" />
    <div v-if="expanded" class="p-2">
      <!-- @slot Use this slot to place the filter box content -->
      <slot />
    </div>
  </div>
</template>

<script>
import IconAction from '../forms/IconAction.vue'

export default {
  components: {
    IconAction,
  },
  props: {
    title: String,
    visible: Boolean,
    count: Number,
    expanded: {type: Boolean, default: false},
  },
  data() {
    return {hover: false}
  },
}
</script>

<style scoped>
.filter-header h6 {
  vertical-align: top;
}

.filter-header:hover {
  cursor: move;
}
</style>
