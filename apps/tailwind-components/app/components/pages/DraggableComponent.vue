<script setup lang="ts">
import { ref } from "vue";
import type { IDraggingInfo } from "../../../types/cms";
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
const showPleaseDragMe = ref<boolean>(false)

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
    class="!justify-start w-full mb-1"
    draggable="true"
    @click="showPleaseDragMe = true"
    @mouseleave = "showPleaseDragMe = false"
    @dragstart="
      startDrag($event, { dragging: true, componentName, componentType })
    "
    @dragend="
      endDrag($event, { dragging: false, componentName, componentType })
    "
    type="primary"
    size="tiny"
    :icon="icon"
    icon-position="left"
  >
    {{ props.componentName }}
    <span class="" v-if="showPleaseDragMe"> - Please drag me </span>
  </Button>
</template>
