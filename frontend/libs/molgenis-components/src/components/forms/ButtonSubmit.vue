<template>
  <button type="submit" class="btn btn-primary" :disabled="disabled">
    <slot />
    <i v-if="icon" :class="'fa fa-fw fa-' + icon" class="mr-2 ml-0"></i>
  </button>
</template>

<script>
export default {
  name: "ButtonAction",
  props: {
    href: String,
    icon: String,
    disabled: {
      type: Boolean,
      default: false,
    },
  },
};
</script>
