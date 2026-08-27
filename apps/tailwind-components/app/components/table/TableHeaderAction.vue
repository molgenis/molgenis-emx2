<script setup lang="ts">
import ArrowUp from "../global/icons/ArrowUp.vue";
import ArrowDown from "../global/icons/ArrowDown.vue";
import { useId } from "vue";

defineProps<{
  column: {
    id: string;
    label: string;
  };
  schemaId: string;
  tableId: string;
  isResizing?: boolean;
  settings: {
    orderby: {
      column: string;
      direction: "ASC" | "DESC";
    };
  };
}>();

const mgAriaSortMappings: Record<string, string> = {
  ASC: "ascending",
  DESC: "descending",
};

const id = useId();

const emit = defineEmits<{
  (e: "sort-requested", columnId: string): void;
}>();
</script>
<template>
  <div class="flex justify-start items-center gap-1">
    <button
      :id="`table-emx2-${id}-${schemaId}-${tableId}-${column.label}-sort-btn`"
      type="button"
      @click.prevent
      class="overflow-ellipsis whitespace-nowrap max-w-56 overflow-hidden inline-block text-left text-table-column-header font-normal align-middle"
      :ariaSort="
        settings.orderby.column === column.id
          ? mgAriaSortMappings[settings.orderby.direction]
          : 'none'
      "
    >
      <span
        @click="emit('sort-requested', column.id)"
        class="hover:cursor-pointer"
        >{{ column.label }}</span
      >
    </button>
    <ArrowUp
      v-if="
        column.id === settings.orderby.column &&
        settings.orderby.direction === 'ASC'
      "
      aria-hidden="true"
      class="h-4 w-4 text-table-column-header font-normal"
    />
    <ArrowDown
      v-if="
        column.id === settings.orderby.column &&
        settings.orderby.direction === 'DESC'
      "
      aria-hidden="true"
      class="h-4 w-4 text-table-column-header font-normal"
    />
  </div>
</template>
