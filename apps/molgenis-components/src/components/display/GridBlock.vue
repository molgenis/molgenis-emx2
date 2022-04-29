<template>
  <div class="card block">
    <h3 v-if="heading">{{ heading }}</h3>
    <slot></slot>
  </div>
</template>

<style scoped>
.block {
  margin-bottom: 1rem;
  padding: 1rem;
}
</style>

<script>
export default {
  name: "GridBlock",
  props: {
    heading: {
      type: String,
      required: false,
    },
  },
};
</script>