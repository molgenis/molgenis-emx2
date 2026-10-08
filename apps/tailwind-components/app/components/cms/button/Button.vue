<script setup lang="ts">
import { computed } from "vue";
import Button from "../../Button.vue";
import type { ButtonType, ButtonSize } from "../../../../types/types.ts";
import type { IButtons } from "../../../../types/cms.ts";

const props = withDefaults(defineProps<IButtons>(), {
  buttonIsCentered: false,
  type: "primary",
  size: "medium",
  isEditable: false,
});
</script>

<template>
  <div
    class="flex"
    :class="{
      'justify-center': buttonIsCentered,
      'justify-start': !buttonIsCentered,
    }"
  >
    <Button
      class="inline-block"
      :id="id"
      :label="buttonLabel || buttonLink"
      :type="buttonType as ButtonType"
      :size="buttonSize as ButtonSize"
      :href="buttonLink"
    />
  </div>
</template>
