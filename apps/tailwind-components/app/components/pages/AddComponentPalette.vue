<script setup lang="ts">
import { ref } from "vue";
import DraggableComponent from "./DraggableComponent.vue";
const emit = defineEmits(["dragging"]);
const handleDragEvent = (event: DragEvent) => {
  emit("dragging", event);
};

const componentsOpen = ref<boolean>(true);
const blocksOpen = ref<boolean>(true);
</script>

<template>
  <div>
    <div class="px-5 pt-3 pb-3 flex items-center justify-between">
      <h2
        class="font-display text-heading-3xl text-search-filter-title font-bold uppercase"
      >
        Component palette
      </h2>
    </div>

    <hr class="border-t border-filter-divider mx-5" />
    <div
      class="p-5 flex items-center gap-1 cursor-pointer group"
      role="button"
      tabindex="0"
      :aria-expanded="componentsOpen"
      aria-controls="???"
      @click="componentsOpen = !componentsOpen"
    >
      <h3
        class="font-sans text-body-base font-bold text-search-filter-group-title group-hover:underline min-w-0 break-words"
      >
        Components
      </h3>
      <span
        class="flex items-center justify-center w-8 h-8 rounded-full text-search-filter-group-toggle group-hover:bg-search-filter-group-toggle transition-transform shrink-0"
        :class="{ 'rotate-180': componentsOpen }"
      >
        <BaseIcon name="caret-up" :width="26" />
      </span>
    </div>
    <div v-if="componentsOpen" class="px-5 pb-5">
      <ul>
        <li>
          <DraggableComponent
            icon="heading"
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
    </div>

    <hr class="border-t border-filter-divider mx-5" />
    <div
      class="p-5 flex items-center gap-1 cursor-pointer group"
      role="button"
      tabindex="0"
      :aria-expanded="blocksOpen"
      aria-controls="???"
      @click="blocksOpen = !blocksOpen"
    >
      <h3
        class="font-sans text-body-base font-bold text-search-filter-group-title group-hover:underline min-w-0 break-words"
      >
        Blocks
      </h3>
      <span
        class="flex items-center justify-center w-8 h-8 rounded-full text-search-filter-group-toggle group-hover:bg-search-filter-group-toggle transition-transform shrink-0"
        :class="{ 'rotate-180': blocksOpen }"
      >
        <BaseIcon name="caret-up" :width="26" />
      </span>
    </div>
    <div v-if="blocksOpen" class="px-5 pb-5">
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
  </div>
</template>
