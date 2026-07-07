<script setup lang="ts">
import type { DSVRowAny } from "d3";
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
const { componentName, componentType, icon } = props;
const emit = defineEmits(["dragging"]);

const startDrag = (event: DragEvent, componentInfo: DSVRowAny) => {
  emit("dragging", { dragging: true, ...componentInfo });
  dragging.value = true;
  fromX.value = event.pageX;
  fromY.value = event.pageY;
  console.log("startDrag", event.pageX, event.pageY, event);
};
const endDrag = (event: DragEvent, componentInfo: any) => {
  console.log("endDrag", event.pageX, event.pageY, event);
  emit("dragging", { dragging: false, ...componentInfo });
  dragging.value = false;
};
</script>

<template>
  <Button
    class="!justify-start"
    draggable="true"
    @dragstart="startDrag($event, { componentName, componentType })"
    @dragend="endDrag($event, { componentName, componentType })"
    type="secondary"
    size="tiny"
    :icon="icon"
    icon-position="left"
  >
    {{ props.componentName }}
  </Button>
</template>
