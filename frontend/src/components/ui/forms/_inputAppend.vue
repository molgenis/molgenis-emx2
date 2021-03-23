<template>
  <div class="input-group-append">
    <slot />
    <AppendClear
      v-if="showClear"
      class="input-append"
      v-bind="$props"
      @clear="$emit('clear')"
    />
    <IconAction v-if="showMinus" class="invisible" icon="minus" />
    <IconAction v-if="showPlus" icon="plus" @click="$emit('add')" />
  </div>
</template>

<script>
import AppendClear from './_appendClear.vue'
import IconAction from './IconAction.vue'

export default {
  components: {AppendClear, IconAction},
  props: {
    clear: {
      type: Boolean,
      default: true,
    },
    error: String,
    readonly: {
      type: Boolean,
      default: false,
    },
    showClear: {
      type: Boolean,
      default: true,
    },
    showMinus: {
      type: Boolean,
      default: false,
    },
    showPlus: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['add', 'clear'],
}
</script>

<style>
.input-group-append .input-append-show {
  display: none;
}

.input-group-append:hover .input-append-show {
  display: block;
}
</style>

