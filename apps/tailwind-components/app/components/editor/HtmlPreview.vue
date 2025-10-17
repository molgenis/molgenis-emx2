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
      disabled__full_screen: !content.enableFullScreen,
    }"
  >
    <div ref="preview" />
  </div>
</template>

<style lang="scss">
.emx2__page_preview {
  &.enabled {
    &.enabled__full_screen {
      @apply w-full;
    }

    &.disabled__full_screen {
      @apply max-w-lg mx-auto lg:px-7.5;
    }

    h1 {
      @apply text-heading-6xl;
    }

    h2 {
      @apply text-heading-5xl;
    }

    h3 {
      @apply text-heading-4xl;
    }

    h3 {
      @apply text-heading-3xl;
    }

    h4 {
      @apply text-heading-2xl;
    }

    h5 {
      @apply text-heading-xl;
    }

    h6 {
      @apply text-heading-lg;
    }

    p {
      @apply text-body-base mb-2.5;

      a {
        @apply underline;
      }
    }

    ul,
    ol {
      @apply ml-10;

      li {
        @apply mb-2.5;
      }
    }

    ul {
      @apply list-disc;
    }

    ol {
      @apply list-decimal;
    }

    &.enabled__button_styles {
      button {
        @apply h-14 px-7.5 text-heading-xl gap-4 tracking-widest uppercase rounded-input font-display bg-button-outline text-button-outline border border-button-outline hover:bg-button-outline-hover hover:text-button-outline-hover hover:border-button-outline-hover;

        &[type="submit"] {
          @apply tracking-widest uppercase rounded-input font-display bg-button-primary text-button-primary border-button-primary hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover;
        }
      }
    }
  }
}
</style>
