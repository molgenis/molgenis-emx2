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
      class="flex flex-col gap-2 p-2 bg-white border rounded shadow-dashboard-palette transition-shadow duration-200"
      :class="{ 'shadow-dashboard-palette-hover': isDragging }"
    >
      <div
        class="cursor-grab select-none border-b pb-1"
        :class="{ 'cursor-grabbing': isDragging }"
        ref="handle"
      >
        <strong>Component palette</strong>
      </div>
      <div
        class="px-1 h-6 text-body-sm rounded text-center inline-block bg-dashboard-dropzone text-title-contrast"
      >
        Components
      </div>
      <DraggableComponent
        icon="title"
        componentName="Heading"
        @dragging="handleDragEvent"
      />
      <DraggableComponent
        icon="paragraph"
        componentName="Paragraph"
        @dragging="handleDragEvent"
      />
      <DraggableComponent
        icon="image"
        componentName="Image"
        @dragging="handleDragEvent"
      />
      <div
        class="px-1 h-6 text-body-sm rounded text-center inline-block bg-dashboard-dropzone text-title-contrast"
      >
        Blocks
      </div>
      <DraggableComponent
        icon="header"
        componentName="Header"
        componentType="Block"
        @dragging="handleDragEvent"
      />
      <DraggableComponent
        icon="view-normal"
        componentName="Section"
        componentType="Block"
        @dragging="handleDragEvent"
      />
    </div>
  </UseDraggable>
</template>
