<script lang="ts" setup>
import type { ButtonIconPosition, ButtonSize, ButtonType } from "~/types/types";

withDefaults(
  defineProps<{
    type?: ButtonType;
    size?: ButtonSize;
    label?: string;
    iconPosition?: ButtonIconPosition;
    disabled?: boolean;
    iconOnly?: boolean;
    tooltip?: string;
    data: string;
    mediaType: string;
    fileName: string;
  }>(),
  {
    type: "outline",
    size: "small",
    label: "Download",
    disabled: false,
    iconOnly: false,
  }
);

function startDownload(data: string, mediaType: string, fileName: string) {
  const blob = new Blob([data], { type: mediaType });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement("a");

  a.style = "display: none";
  a.href = url;
  a.download = fileName;
  a.click();

  window.URL.revokeObjectURL(url);
}
</script>
<template>
  <Button
    :type="type"
    :size="size"
    :label="label"
    icon="download"
    :icon-position="iconPosition"
    :disabled="disabled"
    :icon-only="iconOnly"
    :tooltip="tooltip"
    @click.prevent="startDownload(data, mediaType, fileName)"
  />
</template>
