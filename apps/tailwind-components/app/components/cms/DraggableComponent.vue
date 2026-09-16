<script setup lang="ts">
import { ref } from "vue";
import type { IDraggingInfo } from "../../../types/CmsComponents";
import Button from "../Button.vue";
const props = withDefaults(
  defineProps<{
    componentLabel?: string;
    componentName: string;
    componentType?: string;
    icon?: string;
  }>(),
  {
    componentType: "Component",
    icon: "plus",
  }
);

const emit = defineEmits(["dragging"]);
const showPleaseDragMe = ref<boolean>(false);

const startDrag = (event: DragEvent, componentInfo: IDraggingInfo) => {
  emit("dragging", componentInfo);
  showPleaseDragMe.value = false;
};
const endDrag = (event: DragEvent, componentInfo: IDraggingInfo) => {
  emit("dragging", componentInfo);
};
</script>

<template>
  <button
    class="grid grid-cols-[18px_1fr_18px] items-center w-full mb-1 h-button-small px-5 text-heading-sm gap-3 cursor-grab bg-button-secondary text-button-secondary border-button-secondary hover:bg-button-secondary-hover hover:text-button-secondary-hover hover:border-button-secondary-hover"
    draggable="true"
    @click="showPleaseDragMe = true"
    @mouseleave="showPleaseDragMe = false"
    @dragstart="
      startDrag($event, {
        dragging: true,
        action: 'create',
        componentName,
        componentType,
      })
    "
    @dragend="
      endDrag($event, {
        dragging: false,
        action: 'create',
        componentName,
        componentType,
      })
    "
  >
    <BaseIcon v-if="icon" :name="icon" :width="16" :height="16" />
    <span class="block w-auto text-left">
      {{ props.componentLabel || props.componentName }}
      <span class="" v-if="showPleaseDragMe"> - Please drag me </span>
    </span>
    <BaseIcon name="drag" :width="16" :height="16" />
  </button>
</template>
