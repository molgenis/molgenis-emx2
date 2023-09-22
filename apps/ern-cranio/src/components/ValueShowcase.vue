<template>
  <div class="value-showcase">
    <div class="icon">
      <slot></slot>
    </div>
    <h2 class="title">{{ title }}</h2>
    <p class="description">{{ description }}</p>
  </div>
</template>

<script setup>
const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    required: true,
  },
});
</script>

<style lang="scss">
.value-showcase {
  display: grid;
  grid-template-areas:
    "icon title"
    "icon description";

  .icon {
    grid-area: icon;
    display: flex;
    justify-content: center;
    align-items: center;
    margin-right: 1em;

    svg {
      $size: 24pt;
      width: $size;
      height: $size;
    }
  }

  .title {
    grid-area: title;
    font-weight: bold;
    font-size: 14pt;
    margin: 0;
  }

  .description {
    grid-area: description;
  }
}
</style>
