<script setup lang="ts">
import { computed } from "vue";
import { sanitizeHtmlText } from "../../../utils/cms/sanitizeHtmlText";

const props = defineProps<{ text: string }>();

const text = computed<string>(() => {
  return sanitizeHtmlText(props.text);
});
</script>

<template>
  <li class="text-title-contrast mb-1 last:mb-0" v-html="text" />
</template>
