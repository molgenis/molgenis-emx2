<script lang="ts" setup>
import Button from "../Button.vue";
import type {ButtonType} from "../../../types/types";

withDefaults(
    defineProps<{
      label: string;
      type?: ButtonType;
      icon: string;
      disabled?: boolean;
    }>(),
    {
      type: "filterWell",
    }
);
</script>
<template>
  <Button
      :type="type"
      size="large"
      :label="label"
      :iconOnly="true"
      :icon="icon"
      :disabled="disabled"
  />
</template>
