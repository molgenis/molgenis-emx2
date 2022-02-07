<template>
  <NuxtLink :to="to" class="btn col m-2" no-prefetch>
    <span class="badge badge-light">{{ count }}</span>
    <h3>{{ label }}</h3>
    <p class="text-left">
      <slot></slot>
    </p>
  </NuxtLink>
</template>

<script>
export default {
  name: "CountCard",
  props: {
    label: {
      type: String,
    },
    count: {
      type: Number,
      default: () => 0,
    },
    to: {
      type: String,
    },
  },
};
</script>