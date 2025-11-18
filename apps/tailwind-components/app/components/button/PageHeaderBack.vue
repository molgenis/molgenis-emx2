<script lang="ts" setup>
import Button from "../Button.vue";

withDefaults(
  defineProps<{
    label?: string;
  }>(),
  {
    label: "back",
  }
);
</script>
<template>
  <Button
    type="filterWell"
    size="large"
    :label="label"
    :iconOnly="true"
    icon="arrow-left"
  />
</template>
