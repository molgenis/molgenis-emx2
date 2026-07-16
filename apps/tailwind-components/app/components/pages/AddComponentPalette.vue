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

const { width } = useWindowSize();
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
      class="flex flex-col gap-2 p-2 bg-white border rounded-base shadow-dashboard-palette transition-shadow duration-200"
      :class="{ 'shadow-dashboard-palette-hover': isDragging }"
    >
      <div
        class="cursor-grab select-none border-b pb-1"
        :class="{ 'cursor-grabbing': isDragging }"
        ref="handle"
      >
        <h2>Component palette</h2>
      </div>
      <h3
        class="px-1 h-6 text-body-sm rounded text-center inline-block bg-dashboard-dropzone text-title-contrast"
      >
        Components
      </h3>
      <ul>
        <li>
          <DraggableComponent
            icon="title"
            componentName="Heading"
            @dragging="handleDragEvent"
          />
        </li>
        <li>
          <DraggableComponent
            icon="paragraph"
            componentName="Paragraph"
            @dragging="handleDragEvent"
          />
        </li>
        <li>
          <DraggableComponent
            icon="image"
            componentName="Image"
            @dragging="handleDragEvent"
          />
        </li>
      </ul>
      <h3
        class="px-1 h-6 text-body-sm rounded text-center inline-block bg-dashboard-dropzone text-title-contrast"
      >
        Blocks
      </h3>
      <ul>
        <li>
          <DraggableComponent
            icon="header"
            componentName="Header"
            componentType="Block"
            @dragging="handleDragEvent"
          />
        </li>
        <li>
          <DraggableComponent
            icon="view-normal"
            componentName="Section"
            componentType="Block"
            @dragging="handleDragEvent"
          />
        </li>
      </ul>
    </div>
  </UseDraggable>
</template>
