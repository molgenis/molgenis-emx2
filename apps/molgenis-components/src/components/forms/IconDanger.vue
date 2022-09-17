<template>
  <button
      class="btn p-1 mt-n1 mb-n1 btn-outline-danger border-0"
      @click="onClick"
  >
    <span v-if="label" class="mr-2">{{ label }}</span>
    <i :class="'fas fa-fw fa-' + icon" @mouseenter="showTooltip = true" @mouseleave="showTooltip = false"></i>
    <Tooltip v-if="showTooltip" :value="tooltip">
  </button>
</template>

<script>
/** Button that is shown as a icon */
import IconAction from "./IconAction.vue";

export default {
  extends: IconAction,
};
</script>

<docs>
<template>
  <IconDanger icon="times" @click="alert('clicked')" tooltip="dangerous tooltip"/>
</template>
<script>
  export default {
    methods: {
      alert(text) {
        alert(text)
      }
    }
  }
</script>
</docs>
