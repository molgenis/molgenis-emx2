<template>
  <button type="button" class="btn btn-danger" @click="onClick">
    <slot />
  </button>
</template>

<script>
import ButtonAction from "./ButtonAction.vue";

export default {
  extends: ButtonAction,
};
</script>

<docs>

<template>
  <ButtonDanger v-on:click="alert('danger clicked');">Dangerous actions</ButtonDanger>
</template>
<script>
  export default {
    methods: {
      alert(text) {
        alert(text);
      }
    }
  }
</script>

</docs>
