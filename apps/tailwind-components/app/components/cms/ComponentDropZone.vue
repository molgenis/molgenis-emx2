<script setup lang="ts">
import { useMouse } from "@vueuse/core";
import { useElementBounding } from "@vueuse/core";
import { ref, useTemplateRef } from "vue";
import { useWindowScroll } from "@vueuse/core";
import { useRafFn as useAnimationFrame } from "@vueuse/core";
import { hideAllPoppers } from "floating-vue";
import {
  addBlock,
  addComponent,
  moveBlockTo,
  moveComponentTo,
} from "../../utils/cms";
import type { IDraggingInfo } from "../../../types/CmsComponents";
const scroll = useWindowScroll();
const dropzone = useTemplateRef("dropzone");

const mouse = useMouse();
const { y } = useElementBounding(dropzone);

const props = withDefaults(
  defineProps<{
    componentType: string;
    schema: string;
    order?: number;
    parent: string;
    draggingInfo: IDraggingInfo;
  }>(),
  {
    order: 0,
  }
);

// emit update page
const emit = defineEmits(["updatePage"]);
const maxDistance = 75;
const maxSize = 30;
const distance = ref<number>(Infinity);
const canPlace = ref<boolean>(false);
const hover = ref<boolean>(false);

useAnimationFrame(() => {
  canPlace.value =
    props.draggingInfo.dragging &&
    props.draggingInfo.componentType === props.componentType;

  // In pixels from center of element
  const distanceFromMouse = Math.abs(
    mouse.y.value - scroll.y.value - (y.value + maxSize * 0.5)
  );
  // [0 to 1] factor based on maxDistance
  const normalizedDistanceFromMouse =
    (maxDistance - Math.min(distanceFromMouse, maxDistance)) / maxDistance;
  // distance with small fudging factor stop animation when close to the element
  distance.value = Math.max(
    Math.min(normalizedDistanceFromMouse * 1.5, 1) * maxSize,
    0
  );
});
async function handleDrop() {
  if (props.draggingInfo.action === "create") {
    await addComponentToBlock();
  } else {
    await moveComponentToBlock();
  }
}
async function addComponentToBlock() {
  if (props.draggingInfo.componentType === "Component") {
    await addComponent(
      props.schema,
      props.draggingInfo.componentName + "-" + crypto.randomUUID(),
      props.parent,
      props.order,
      props.draggingInfo.componentName
    );
  } else {
    await addBlock(
      props.schema,
      props.draggingInfo.componentName + "-" + crypto.randomUUID(),
      props.parent,
      props.order,
      props.draggingInfo.componentName
    );
  }
  hideAllPoppers();
  emit("updatePage");
}
async function moveComponentToBlock() {
  if (props.draggingInfo.componentType === "Component") {
    await moveComponentTo(
      props.schema,
      props.draggingInfo.moveOrderId || "",
      props.draggingInfo.parentId || "",
      props.parent,
      props.order
    );
  } else {
    await moveBlockTo(
      props.schema,
      props.draggingInfo.moveOrderId || "",
      props.parent,
      props.order
    );
  }
  hideAllPoppers();
  emit("updatePage");
}
</script>

<template>
  <div v-if="canPlace" class="relative">
    <div class="absolute left-0 right-0 top-0 bottom-0 z-40">
      <div
        class="border-t border-button-primary border-dashed opacity-30"
      ></div>
      <div class="-translate-y-1/2">
        <div
          @dragover.prevent
          @drop="handleDrop"
          @dragenter="hover = true"
          @dragleave="hover = false"
          ref="dropzone"
          :class="{
            border: distance > 0,
            'border-dashed': !hover,
            'bg-dashboard-dropzone-hover': hover,
          }"
          class="border-button-primary rounded-base overflow-hidden flex items-center justify-center bg-dashboard-dropzone text-center shadow-xl"
          :style="{ height: distance + 'px' }"
        >
          <p class="text-title-contrast pointer-events-none">
            {{ props.draggingInfo.action === "create" ? "Add new" : "Move" }}
            {{ props.draggingInfo.componentName }}
            <span class="hidden" :class="{ '!inline': hover }">here</span>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>
