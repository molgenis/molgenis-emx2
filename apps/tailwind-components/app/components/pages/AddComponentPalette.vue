<script setup lang="ts">
import { UseDraggable } from "@vueuse/components";
import { useTemplateRef } from "vue";
import { useWindowSize } from "@vueuse/core";
import DraggableComponent from "./DraggableComponent.vue";
const emit = defineEmits(["dragging"]);
const props = withDefaults(
  defineProps<{
    content: any;
    metadata: any;
  }>(),
  {}
);

const { width, height } = useWindowSize();
const handle = useTemplateRef("handle");

const handleDragEvent = (event: DragEvent) => {
  emit("dragging", event);
};
</script>

<template>
  <UseDraggable
    v-slot="{ isDragging }"
    class="fixed z-50"
    :initial-value="{ x: width - 200, y: 200 }"
    :handle="handle"
  >
    <div
      class="flex flex-col gap-2 p-2 bg-white border rounded shadow-lg transition-shadow duration-200"
      :class="{ 'shadow-xl': isDragging }"
    >
      <div
        class="cursor-grab select-none border-b pb-1"
        :class="{ 'cursor-grabbing': isDragging }"
        ref="handle"
      >
        <strong>Component palette</strong>
      </div>
      <DraggableComponent componentName="Header" @dragging="handleDragEvent" />
      <DraggableComponent componentName="Image" @dragging="handleDragEvent" />
      <DraggableComponent
        componentName="Paragraph"
        @dragging="handleDragEvent"
      />
      <DraggableComponent
        componentName="Section"
        componentType="Block"
        @dragging="handleDragEvent"
      />
    </div>
  </UseDraggable>
</template>
