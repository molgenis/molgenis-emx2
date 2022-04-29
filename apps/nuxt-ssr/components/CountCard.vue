<template>
  <NuxtLink :to="to" class="card card-body card-height" no-prefetch>
    <span class="badge badge-light float-right">{{ count }}</span>
    <h3>{{ label }}</h3>
    <p class="text-left">
      <slot></slot>
    </p>
  </NuxtLink>
</template>

<style scoped>
.btn .badge {
  position: absolute;
  top: 5px;
  right: 5px;
}

.card-height {
  min-height: 200px;
}
</style>

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
      type: String|Object,
    },
  },
};
</script>