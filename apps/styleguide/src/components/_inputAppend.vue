<template>
  <div class="input-group-append">
    <slot />
    <AppendPlus v-if="showPlus" v-bind="$props" @add="$emit('add')" />
    <AppendClear v-if="showClear" v-bind="$props" @clear="$emit('clear')" />
  </div>
</template>

<script>
import AppendPlus from './_appendPlus'
import AppendClear from './_appendClear'

export default {
  components: { AppendPlus, AppendClear },
  props: {
    readonly: {
      type: Boolean,
      default: false
    },
    error: String,
    showClear: {
      type: Boolean,
      default: true
    },
    clear: {
      type: Boolean,
      default: true
    },
    showPlus: {
      type: Boolean,
      default: false
    }
  }
}
</script>
