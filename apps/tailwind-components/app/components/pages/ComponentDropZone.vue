<script setup lang="ts">
import { useMouse } from "@vueuse/core";
import { useElementBounding } from "@vueuse/core";
import { ref, useTemplateRef } from "vue";
import { useWindowScroll } from "@vueuse/core";
import { useRafFn } from "@vueuse/core";
import { addBlock, addComponent } from "../../utils/cms";
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
    draggingInfo: {
      dragging: boolean;
      componentName: string;
      componentType: string;
    };
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

useRafFn(() => {
  canPlace.value =
    props.draggingInfo.dragging &&
    props.draggingInfo.componentType === props.componentType;
  distance.value = Math.max(
    Math.min(
      ((maxDistance -
        Math.min(
          Math.abs(mouse.y.value - scroll.y.value - (y.value + maxSize * 0.5)),
          maxDistance
        )) /
        maxDistance) *
        1.5,
      1
    ) * maxSize,
    0
  );
});

async function addComponentToBlock() {
  if (props.draggingInfo.componentType === "Component") {
    await addComponent(
      props.schema,
      props.draggingInfo.componentName +
        "-" +
        Math.floor(Math.random() * 100000000),
      props.parent,
      props.order,
      props.draggingInfo.componentName
    );
  } else {
    await addBlock(
      props.schema,
      props.draggingInfo.componentName +
        "-" +
        Math.floor(Math.random() * 100000000),
      props.parent,
      props.order,
      props.draggingInfo.componentName
    );
  }
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
          @drop="addComponentToBlock"
          @dragenter="hover = true"
          @dragleave="hover = false"
          ref="dropzone"
          :class="{
            border: distance > 0,
            'border-dashed': !hover,
            'bg-dashboard-dropzone-hover': hover,
          }"
          class="border-button-primary rounded-lg overflow-hidden flex items-center justify-center bg-dashboard-dropzone text-center shadow-dashboard-palette"
          :style="{ height: distance + 'px' }"
        >
          <p class="text-title-contrast pointer-events-none">
            Add new {{ props.draggingInfo.componentName }}
            <span class="hidden" :class="{ '!inline': hover }">here</span>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>
