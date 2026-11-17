<script setup lang="ts">
import { computed } from "vue";
import { sanitizeHtmlText } from "../../../utils/cms/sanitizeHtmlText.ts";
import type { IParagraphs } from "../../../../types/cms";

const props = withDefaults(defineProps<IParagraphs&{ asText?: boolean }>(), {
  asText: false
});

const text = computed<string | undefined>(() => {
  if (props.text) {
    return sanitizeHtmlText(props.text);
  }
});
</script>

<template>
  <p
    :id="id"
    class="mb-2.5 text-title-contrast [&_a]:underline [&_a]:decoration-solid"
    :class="{
      'text-center': paragraphIsCentered,
      'text-left': !paragraphIsCentered,
    }"
    v-text="asText ? text : undefined"
    v-html="!asText ? text : undefined"
  />
</template>
