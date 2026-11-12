<script setup lang="ts">
defineProps<{
  title: string;
  description: string;
}>();
</script>

<template>
  <div class="value-showcase">
    <div class="icon">
      <slot name="icon"></slot>
    </div>
    <h2 class="title">{{ title }}</h2>
    <p class="description">{{ description }}</p>
  </div>
</template>

<style lang="css">
.value-showcase {
  display: grid;
  grid-template-areas: "icon" "title" "description";
  justify-content: center;
  align-items: center;
  text-align: center;
  gap: 0.3em;
}

.value-showcase .icon {
  grid-area: icon;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-right: 1em;
}

.value-showcase .icon svg {
  width: 28pt;
  height: 28pt;
}

.value-showcase .title {
  grid-area: title;
  font-weight: bold;
  font-size: 15pt;
  margin: 0;
}

.value-showcase .description {
  grid-area: description;
}

@media (min-width: 712px) {
  .value-showcase {
    grid-template-areas: "icon title" "icon description";
    align-items: center;
    text-align: left;
  }
}
</style>
