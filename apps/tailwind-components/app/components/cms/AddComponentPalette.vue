<script setup lang="ts">
import { ref } from "vue";
import DraggableComponent from "./DraggableComponent.vue";
import Accordion from "../Accordion.vue";
const emit = defineEmits(["dragging"]);
const handleDragEvent = (event: DragEvent) => {
  emit("dragging", event);
};
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
    <div class="mx-5 py-2.5 border-b">
      <Accordion
        id="components-section"
        label="Components"
        :inList="true"
        class="[&_div:first-child]:p-0 [&_div:last-child]:p-0"
      >
        <ul class="py-2.5">
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
          <li>
            <DraggableComponent
              icon="ListBulleted"
              componentName="UnorderedLists"
              componentLabel="Unordered list"
              @dragging="handleDragEvent"
            />
          </li>
          <li>
            <DraggableComponent
              icon="ListNumbered"
              componentName="OrderedLists"
              componentLabel="Ordered list"
              @dragging="handleDragEvent"
            />
          </li>
          <li>
            <DraggableComponent
              icon="AddLink"
              componentName="NavigationCards"
              componentLabel="Navigation card"
              @dragging="handleDragEvent"
            />
          </li>
          <li>
            <DraggableComponent
              icon="Colorize"
              componentName="CustomComponents"
              componentLabel="Custom component"
              @dragging="handleDragEvent"
            />
          </li>
        </ul>
      </Accordion>
    </div>
    <div class="mx-5 py-2.5 border-b">
      <Accordion
        id="visualisation-section"
        label="Visualisation"
        :inList="true"
        class="[&_div:first-child]:p-0 [&_div:last-child]:p-0"
      >
        <ul class="py-2.5">
          <li>
            <DraggableComponent
              icon="AddChart"
              componentName="ColumnCharts"
              componentLabel="Column chart"
              @dragging="handleDragEvent"
            />
          </li>
        </ul>
      </Accordion>
    </div>
    <div class="mx-5 py-2.5 border-b">
      <Accordion
        id="blocks-section"
        label="Layouts"
        :inList="true"
        class="[&_div:first-child]:p-0 [&_div:last-child]:p-0"
      >
        <ul class="py-2.5">
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
          <li>
            <DraggableComponent
              icon="columns"
              componentName="Section - 2 Columns"
              componentType="Block"
              @dragging="handleDragEvent"
            />
          </li>
          <li>
            <DraggableComponent
              icon="columns"
              componentName="Section - 3 Columns"
              componentType="Block"
              @dragging="handleDragEvent"
            />
          </li>
        </ul>
      </Accordion>
    </div>
  </div>
</template>
