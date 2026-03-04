<script setup lang="ts">
import MarkdownIt from "markdown-it";
import { computed } from "vue";

const props = defineProps<{ source: string }>();

const md = new MarkdownIt();
const html = computed(() => md.render(props.source));
</script>

<template>
  <div class="prose prose-sm max-w-none" v-html="html" />
</template>
