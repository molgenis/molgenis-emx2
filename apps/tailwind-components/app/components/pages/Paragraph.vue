<script setup lang="ts">
import { computed } from "vue";
import EditButton from "./EditButton.vue";
import { renderParagraphUrls } from "../../utils/cms";
import type { IParagraphs } from "../../../types/cms";

const props = withDefaults(
  defineProps<IParagraphs & { isEditable?: boolean }>(),
  {
    paragraphIsCentered: false,
    isEditable: false,
  }
);

const renderedText = computed<string | undefined>(() => {
  if (props.text) {
    return renderParagraphUrls(props.text);
  }
});

const emit = defineEmits<{
  (e: "edit"): void;
}>();
</script>

<template>
  <p
    :id="id"
    class="text-title-contrast"
    :class="{
      'text-center': paragraphIsCentered,
      'text-left': !paragraphIsCentered,
    }"
  >
    <EditButton
      v-if="isEditable"
      @click="emit('edit')"
      :class="{
        'text-center': paragraphIsCentered,
        'text-left': !paragraphIsCentered,
      }"
    >
      <span class="sr-only">edit paragraph: </span>
      <span
        class="group-hover:underline group-focus:underline"
        v-html="renderedText"
      />
    </EditButton>
    <span v-else v-html="renderedText" />
  </p>
</template>
