<script setup lang="ts">
import type { DSVRowAny } from "d3";
import Button from "../Button.vue";
const props = withDefaults(
  defineProps<{
    componentName: string;
    componentType?: string;
  }>(),
  {
    componentType: "Component",
  }
);
const { componentName, componentType } = props;
const emit = defineEmits(["dragging"]);

const startDrag = (event: DragEvent, componentInfo: DSVRowAny) => {
  emit("dragging", { dragging: true, ...componentInfo });
};
const endDrag = (event: DragEvent, componentInfo: any) => {
  emit("dragging", { dragging: false, ...componentInfo });
};
</script>

<template>
  <Button
    draggable="true"
    @dragstart="startDrag($event, { componentName, componentType })"
    @dragend="endDrag($event, { componentName, componentType })"
    type="primary"
    size="tiny"
    icon="plus"
    icon-position="left"
  >
    {{ props.componentName }}
  </Button>
</template>
