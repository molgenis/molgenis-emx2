<template>
  <button
    type="button"
    :class="light ? 'btn btn-outline-light' : 'btn btn-outline-primary'"
    @click="onClick"
  >
    <i v-if="icon" :class="'fa-' + icon" class="fa fa-fw mr-2 ml-0"></i>
    <slot />
  </button>
</template>

<script>
/** Cancel button */
import ButtonAction from "./ButtonAction.vue";

export default {
  name: "ButtonOutline",
  extends: ButtonAction,
  props: {
    light: { type: Boolean, default: false },
  },
};
</script>

<docs>
<template>
  <demo-item>
    <div>
      <ButtonOutline v-on:click="alert('cancel clicked')">Cancel</ButtonOutline>
    </div>
  </demo-item>
</template>
<script>
  export default {
    methods: {
      alert(text) {
        alert(text);
      },
    },
  };
</script>
</docs>
