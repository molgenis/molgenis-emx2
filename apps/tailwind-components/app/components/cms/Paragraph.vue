<script setup lang="ts">
import { computed, ref } from "vue";
import { renderTextUrls } from "../../utils/cms";
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

const renderedText = computed<string | undefined>(() => {
  if (props.text) {
    return renderTextUrls(props.text);
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
      class="text-title-contrast"
      :class="{
        'text-center': paragraphIsCentered,
        'text-left': !paragraphIsCentered,
        underline: showMenu,
      }"
      v-html="renderedText"
    />
  </VMenu>

  <p
    v-else
    :id="id"
    class="text-title-contrast"
    :class="{
      'text-center': paragraphIsCentered,
      'text-left': !paragraphIsCentered,
      underline: showMenu,
    }"
    v-html="renderedText"
  />
</template>
