<script setup lang="ts">
import { computed } from "vue";
import { sanitizeHtmlText } from "../../../utils/cms/sanitizeHtmlText.ts";
import type { IParagraphs } from "../../../../types/cms";

const props = defineProps<IParagraphs & { isEditable?: boolean }>();

const text = computed<string | undefined>(() => {
  if (props.text) {
    return sanitizeHtmlText(props.text);
  }
});
const emit = defineEmits(["updateText"]);
</script>

<template>
  <p
    :id="id"
    class="mb-2.5 text-title-contrast [&_a]:underline [&_a]:decoration-solid"
    :class="{
      'text-center': paragraphIsCentered,
      'text-left': !paragraphIsCentered,
    }"
    :contenteditable="props.isEditable"
    @input="props.isEditable && emit('updateText', $el.innerHTML)"
    v-html="text"
  />
</template>
