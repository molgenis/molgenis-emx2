<template>
  <button type="submit" class="btn btn-primary">
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
  },
};
</script>


