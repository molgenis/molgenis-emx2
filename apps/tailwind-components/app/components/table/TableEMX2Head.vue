<template>
  <thead>
    <tr>
      <TableHeadCell v-if="showDraftColumn" class="w-24 lg:w-28">
        <TableHeaderAction
          :column="{ id: 'mg_draft', label: 'Draft' }"
          :schemaId="schemaId"
          :tableId="tableId"
          :settings="settings"
          @sort-requested="$emit('sort-requested')"
        />
      </TableHeadCell>
      <TableHeadCell
        v-for="column in columns"
        :style="{
          width: columnWidths[column.id] + 'px',
          userSelect: isResizing ? 'none' : 'auto',
        }"
        class="relative group"
      >
        <div
          class="absolute right-0 top-0 h-full w-4 cursor-col-resize group"
          @mousedown.stop="
            $emit('start-resize', { event: $event, id: column.id })
          "
        >
          <div
            class="absolute right-0 top-0 h-full w-[2px] bg-transparent hover:bg-button-primary"
          />
        </div>
        <TableHeaderAction
          :column="column"
          :schemaId="schemaId"
          :tableId="tableId"
          :settings="settings"
          @sort-requested="$emit('sort-requested', column.id)"
        />
      </TableHeadCell>
    </tr>
  </thead>
</template>

<script setup lang="ts">
import TableHeadCell from "./TableHeadCell.vue";
import TableHeaderAction from "./TableHeaderAction.vue";

defineProps<{
  schemaId: string;
  tableId: string;
  settings: {
    orderby: {
      column: string;
      direction: "ASC" | "DESC";
    };
  };
  columns: any;
  columnWidths: Record<string, number>;
  isResizing: boolean;
  showDraftColumn?: boolean;
}>();
</script>
