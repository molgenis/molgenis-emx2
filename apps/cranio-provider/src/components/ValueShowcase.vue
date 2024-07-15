<template>
  <div class="value-showcase">
    <div class="icon">
      <slot name="icon"></slot>
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
  grid-template-areas: "icon" "title" "description";
  justify-content: center;
  align-items: center;
  text-align: center;
  gap: 0.3em;

  @media (min-width: 712px) {
    grid-template-areas: "icon title" "icon description";
    align-items: center;
    text-align: left;
  }

  .icon {
    grid-area: icon;
    display: flex;
    justify-content: center;
    align-items: center;
    margin-right: 1em;

    svg {
      $size: 28pt;
      width: $size;
      height: $size;
    }
  }

  .title {
    grid-area: title;
    font-weight: bold;
    font-size: 15pt;
    margin: 0;
  }

  .description {
    grid-area: description;
  }
}
</style>
