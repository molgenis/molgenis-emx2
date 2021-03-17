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
import AppendClear from "./_appendClear.vue";
import IconAction from "./IconAction.vue";

export default {
  components: { IconAction, AppendClear },
  props: {
    readonly: {
      type: Boolean,
      default: false,
    },
    error: String,
    showClear: {
      type: Boolean,
      default: true,
    },
    clear: {
      type: Boolean,
      default: true,
    },
    showPlus: {
      type: Boolean,
      default: false,
    },
    showMinus: {
      type: Boolean,
      default: false,
    },
  },
};
</script>


<style>
.input-group-append .input-append-show {
  display: none;
}

.input-group-append:hover .input-append-show {
  display: block;
}
</style>

