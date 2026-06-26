<template>
  <thead>
    <tr>
      <TableHeadCell class="sticky left-0 bg-table z-20 w-12">
        <!-- <div class="flex justify-center items-center">
                <Checkbox @change="toggleAllRows" />
              </div> -->
      </TableHeadCell>
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
      <!-- Empty, sticky header cell aligning with the floating row-action column. -->
      <th
        v-if="hasRowActions"
        aria-hidden="true"
        class="sticky right-0 z-10 w-40 border-b border-gray-200 bg-table"
      />
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
  hasRowActions?: boolean;
}>();
</script>
