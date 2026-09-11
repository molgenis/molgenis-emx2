<script setup lang="ts">
import { computed, ref } from "vue";
import sanitizeHtml from "sanitize-html";

import ComponentActions from "./ComponentActions.vue";
import type { IParagraphs } from "../../../types/cms";

const props = withDefaults(
  defineProps<IParagraphs & { isEditable?: boolean }>(),
  {
    paragraphIsCentered: false,
    isEditable: false,
  }
);
const emit = defineEmits(["edit", "delete", "move"]);
const showMenu = ref<boolean>(false);

const text = computed<string | undefined>(() => {
  if (props.text) {
    return sanitizeHtml(props.text, {
      allowedTags: ["a", "code", "em", "i", "span", "strong"],
    });
  }
});
</script>

<template>
  <VMenu
    v-if="isEditable"
    v-model:shown="showMenu"
    showGroup="component-menu"
    :triggers="['hover', 'focus']"
    :popperTriggers="['hover', 'focus']"
    :delay="{ show: 100, hide: 200 }"
    :placement="paragraphIsCentered ? 'bottom-auto' : 'bottom-start'"
    noAutoFocus
  >
    <template #popper>
      <ComponentActions
        name="Paragraph"
        :id="`${id}-toolbar`"
        :aria-controls="id"
        @edit="$emit('edit')"
        @delete="$emit('delete')"
        @move="$emit('move', $event)"
      />
    </template>
    <p
      :id="id"
      class="mb-2.5 text-title-contrast [&_a]:underline [&_a]:decoration-solid"
      :class="{
        'text-center': paragraphIsCentered,
        'text-left': !paragraphIsCentered,
        underline: showMenu,
      }"
      v-html="text"
    />
  </VMenu>
  <p
    v-else
    :id="id"
    class="mb-2.5 text-title-contrast [&_a]:underline [&_a]:decoration-solid"
    :class="{
      'text-center': paragraphIsCentered,
      'text-left': !paragraphIsCentered,
      underline: showMenu,
    }"
    v-html="text"
  />
</template>
