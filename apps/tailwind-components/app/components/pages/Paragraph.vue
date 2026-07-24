<script setup lang="ts">
import { computed } from "vue";
import { renderTextUrls } from "../../utils/cms";
import type { IParagraphs } from "../../../types/cms";

const props = withDefaults(
  defineProps<IParagraphs & { isEditable?: boolean }>(),
  {
    paragraphIsCentered: false,
    isEditable: false,
  }
);
const showMenu = defineModel("showMenu");

const renderedText = computed<string | undefined>(() => {
  if (props.text) {
    return renderTextUrls(props.text);
  }
});
</script>

<template>
  <p
    :id="id"
    class="text-title-contrast"
    :class="{
      'text-center': paragraphIsCentered,
      'text-left': !paragraphIsCentered,
      underline: showMenu,
    }"
  >
    {{ renderedText }}
  </p>
</template>
