<script setup>
import Button from "./Button.vue";
</script>

<template>
  <div class="flex justify-between">
    <Button
      type="primary"
      size="small"
      label="filters"
      icon="filter"
      iconPosition="left"
    ></Button>

    <Button
      type="secondary"
      size="small"
      label="view"
      icon="view-normal"
      iconPosition="left"
    ></Button>
  </div>
</template>
