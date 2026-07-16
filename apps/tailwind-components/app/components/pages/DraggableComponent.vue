<script setup lang="ts">
import type { IDraggingInfo } from "../../../types/cms";
import Button from "../Button.vue";
import { ref } from "vue";

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

const dragging = ref<boolean>(false);
const emit = defineEmits(["dragging"]);

const startDrag = (event: DragEvent, componentInfo: IDraggingInfo) => {
  emit("dragging", componentInfo);
  dragging.value = true;
};
const endDrag = (event: DragEvent, componentInfo: IDraggingInfo) => {
  emit("dragging", componentInfo);
  dragging.value = false;
};
</script>

<template>
  <Button
    class="!justify-start"
    draggable="true"
    @dragstart="
      startDrag($event, { dragging: true, componentName, componentType })
    "
    @dragend="
      endDrag($event, { dragging: false, componentName, componentType })
    "
    type="secondary"
    size="tiny"
    :icon="icon"
    icon-position="left"
  >
    {{ props.componentName }}
  </Button>
</template>
