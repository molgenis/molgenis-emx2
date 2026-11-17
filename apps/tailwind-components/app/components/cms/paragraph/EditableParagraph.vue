<script setup lang="ts">
import { ref, onMounted, watch  } from "vue";
import Paragraph from "./Paragraph.vue";
import ComponentActions from "../ComponentActions.vue";
import {
  actionsToolbarShowDelay,
  actionsToolbarHideDelay,
} from "../../../utils/cms/constants.ts";
import type { IParagraphs } from "../../../../types/cms";

const props = withDefaults(
  defineProps<IParagraphs & { isEditable?: boolean }>(),
  {
    paragraphIsCentered: false,
    isEditable: false,
  }
);
const emit = defineEmits(["edit", "delete", "move", "updateText"]);
const showMenu = ref<boolean>(false);
const displayTextOnly = ref<boolean>(false);
const localText = ref<string>(props.text ?? "");
const draftText = ref<string>(props.text ?? "");

watch(
  () => props.text,
  (newServerText) => {
    if (!displayTextOnly.value && newServerText !== undefined) {
      localText.value = newServerText;
    }
  }
);

function updateText($event: Event) {
  const newText = ($event.target as HTMLElement)?.innerText ?? '';
  draftText.value = newText;
  emit('updateText', newText);
}
</script>

<template>
  <VMenu
    v-if="isEditable"
    v-model:shown="showMenu"
    showGroup="component-menu"
    :triggers="['hover', 'focus']"
    :popperTriggers="['hover', 'focus']"
    :delay="{ show: actionsToolbarShowDelay, hide: actionsToolbarHideDelay }"
    :placement="paragraphIsCentered ? 'bottom' : 'bottom-start'"
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
    <Paragraph
      ref="editorRef"
      v-bind="props"
      :text="localText"
      :contenteditable="props.isEditable&&'plaintext-only'"
      @input="props.isEditable && updateText($event)"
      @focus="displayTextOnly = true"
      @blur="displayTextOnly = false;localText = draftText"
      :asText="displayTextOnly"
    />
  </VMenu>
  <Paragraph v-else v-bind="props" />
</template>
