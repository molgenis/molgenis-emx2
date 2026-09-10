<script setup lang="ts">
import { ref } from "vue";
import type { IDraggingInfo } from "../../../types/CmsComponents";
import Button from "../Button.vue";
const props = withDefaults(
  defineProps<{
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
  <Button
    class="!justify-start w-full mb-1 cursor-grab relative"
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
    type="secondary"
    size="tiny"
    :icon="icon"
    icon-position="left"
  >
    <div class="flex items-center">
      <span>{{ props.componentName }}</span>
      <span class="" v-if="showPleaseDragMe"> - Please drag me </span>
      <BaseIcon name="drag" :width="16" :height="16" class="absolute right-1" />
    </div>
  </Button>
</template>
