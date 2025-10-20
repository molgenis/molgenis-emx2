<script lang="ts" setup>
import { useTemplateRef, watch } from "vue";
import { type DeveloperPage, generateHtmlPreview } from "../../utils/Pages";

const props = defineProps<{
  content: DeveloperPage;
}>();

const previewElem = useTemplateRef<HTMLDivElement>("preview");

function renderPreview() {
  generateHtmlPreview(props.content, previewElem.value as HTMLDivElement);
}

watch(
  () => previewElem.value,
  () => {
    renderPreview();
  }
);

watch(
  () => props.content,
  () => renderPreview(),
  { deep: true }
);
</script>

<template>
  <div
    class="emx2__page_preview"
    :class="{
      enabled: content.enableBaseStyles,
      enabled__button_styles: content.enableButtonStyles,
      enabled__full_screen: content.enableFullScreen,
      'disabled__full_screen p-7.5': !content.enableFullScreen,
    }"
  >
    <div ref="preview" />
  </div>
</template>

<style>
.emx2__page_preview.enabled.enabled__full_screen {
  @apply w-full;
}

.emx2__page_preview.enabled.disabled__full_screen {
  @apply max-w-lg mx-auto lg:px-7.5;
}

.emx2__page_preview.enabled h1 {
  @apply text-heading-6xl;
}

.emx2__page_preview.enabled h2 {
  @apply text-heading-5xl;
}

.emx2__page_preview.enabled h3 {
  @apply text-heading-4xl;
}

.emx2__page_preview.enabled h3 {
  @apply text-heading-3xl;
}

.emx2__page_preview.enabled h4 {
  @apply text-heading-2xl;
}

.emx2__page_preview.enabled h5 {
  @apply text-heading-xl;
}

.emx2__page_preview.enabled h6 {
  @apply text-heading-lg;
}

.emx2__page_preview.enabled p {
  @apply text-body-base mb-2.5;
}

.emx2__page_preview.enabled p a {
  @apply underline;
}

.emx2__page_preview.enabled ul,
.emx2__page_preview.enabled ol {
  @apply ml-10;
}

.emx2__page_preview.enabled ul li,
.emx2__page_preview.enabled ol li {
  @apply mb-2.5;
}

.emx2__page_preview.enabled ul {
  @apply list-disc;
}

.emx2__page_preview.enabled ol {
  @apply list-decimal;
}

.emx2__page_preview.enabled.enabled__button_styles button {
  @apply h-14 px-7.5 text-heading-xl gap-4 tracking-widest uppercase rounded-input font-display bg-button-outline text-button-outline border border-button-outline hover:bg-button-outline-hover hover:text-button-outline-hover hover:border-button-outline-hover;
}

.emx2__page_preview.enabled.enabled__button_styles button[type="submit"] {
  @apply tracking-widest uppercase rounded-input font-display bg-button-primary text-button-primary border-button-primary hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover;
}
</style>
