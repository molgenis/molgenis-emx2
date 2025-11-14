<script lang="ts" setup>
import type { ButtonIconPosition, ButtonSize, ButtonType } from "~/types/types";
import Button from "../Button.vue";
import {navigateTo} from "#app";

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
      class="mr-4"
      :type="type"
      size="large"
      :label="label"
      :iconOnly="true"
      :icon="icon"
      :disabled="disabled"
  />
</template>
