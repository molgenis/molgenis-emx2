<template>
  <button class="btn p-0 m-0 btn-outline-danger border-0" @click="onClick">
    <Tooltip :value="tooltip" class="p-1 mt-n1 mb-n1">
      <span v-if="label" class="mr-2 d-inline-block">{{ label }}</span>
      <i :class="'fas fa-fw fa-' + icon"></i>
    </Tooltip>
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
