<template>
  <button
    type="button"
    class="btn border-0"
    :class="light ? 'btn-outline-light' : 'btn-outline-primary'"
    @click.prevent="onClick">
    <slot />
    <i v-if="icon" :class="'fa fa-fw fa-' + icon"></i>
  </button>
</template>

<script>
import ButtonAction from "./ButtonAction.vue";

export default {
  name: "ButtonAlt",
  extends: ButtonAction,
  props: {
    light: { type: Boolean, default: false },
    icon: {
      type: String,
      required: false,
      default: () => null,
    },
  },
};
</script>

<docs>
<template>
  <demo-item>

    <div>
      <ButtonAlt v-on:click="alert('cancel clicked')">Cancel</ButtonAlt>
    </div>

    <div class="mt-3">
      <label for="alt-icon-btn">with icon</label>
      <div>
        <ButtonAlt id="alt-icon-btn" icon="times" v-on:click="alert('cancel clicked')">Cancel</ButtonAlt>
      </div>
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
