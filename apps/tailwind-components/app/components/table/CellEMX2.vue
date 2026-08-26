<template>
  <td class="p-2 border-b min-h-8">
    <slot name="row-actions"></slot>
    <div class="flex overflow-hidden items-center gap-2">
      <div class="truncate min-w-0" ref="cellRef">
        <slot>
          <ValueEMX2
            v-if="metadata && data !== undefined && data !== null"
            :metadata="metadata"
            :data="data"
            :collapse="false"
            @valueClick="$emit('cellClicked', $event)"
          />
          <template v-else>
            <span class="min-h-4 inline-block"></span>
          </template>
        </slot>
      </div>
      <Button
        v-if="isEllipsisActive"
        type="text"
        size="tiny"
        @click="handleShowMore"
      >
        More
      </Button>
    </div>
  </td>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from "vue";
import type {
  columnValue,
  IColumn,
} from "../../../../metadata-utils/src/types";
import type { cellPayload } from "../../../types/types";
import Button from "../Button.vue";
import ValueEMX2 from "../value/EMX2.vue";

const props = defineProps<{
  metadata?: IColumn;
  data?: columnValue;
}>();

const cellRef = ref<HTMLElement | null>(null);
const isEllipsisActive = ref(false);
let resizeObserver: ResizeObserver;

const emit = defineEmits<{
  (e: "cellClicked", payload: cellPayload): void;
}>();

onMounted(async () => {
  await nextTick();
  setIsEllipsisActive();
  if (cellRef.value) {
    resizeObserver = new ResizeObserver(setIsEllipsisActive);
    resizeObserver.observe(cellRef.value);
  }
});

onUnmounted(() => {
  resizeObserver?.disconnect();
});

function setIsEllipsisActive() {
  isEllipsisActive.value = cellRef.value
    ? cellRef.value.offsetWidth < cellRef.value.scrollWidth
    : false;
}

function handleShowMore() {
  if (props.metadata) {
    emit("cellClicked", {
      data: props.data,
      metadata: props.metadata,
    });
  }
}
</script>
