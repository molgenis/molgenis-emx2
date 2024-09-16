<script setup lang="ts">
import draggable from "vuedraggable";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { SideModal } from "#build/components";

const props = defineProps<{
  columns: IColumn[];
}>();

const showModal = ref(false);

interface IColumnConfig {
  id: string;
  label: string;
  position: number;
  visible: boolean;
}

const columnsInColumnsSelectModal = ref<IColumnConfig[]>([]);

watch(
  () => props.columns,
  (value) => {
    columnsInColumnsSelectModal.value = value.map(columToColumnConfig);
  }
);

function handleColumnDragEvent(event: {
  moved: {
    element: IColumnConfig;
    newIndex: number;
    oldIndex: number;
  };
}) {
  columnsInColumnsSelectModal.value.forEach((column) => {
    if (column.id === event.moved.element.id) {
      column.position = event.moved.newIndex;
    } else if (
      column.position >= event.moved.newIndex &&
      column.position < event.moved.oldIndex
    ) {
      column.position = (column.position ?? 0) + 1;
    } else if (
      column.position <= event.moved.newIndex &&
      column.position > event.moved.oldIndex
    ) {
      column.position = (column.position ?? 0) - 1;
    }
  });
}

function handleCancel() {
  columnsInColumnsSelectModal.value = props.columns.map(columToColumnConfig);
  showModal.value = false;
}

function columToColumnConfig(column: IColumn): IColumnConfig {
  return {
    id: column.id,
    label: column.label || column.id,
    position: column.position ?? 0,
    visible: column.visible == "true" ? true : false,
  };
}
</script>

<template>
  <Button type="outline" icon="columns" size="small" @click="showModal = true"
    >Columns</Button
  >
  <SideModal
    ref="columnsSelectModal"
    :slide-in-right="true"
    :fullScreen="false"
    button-alignment="left"
    :show="showModal"
  >
    <ContentBlockModal title="Columns">
      <draggable
        class=""
        :list="
          columnsInColumnsSelectModal.sort(
            (a, b) => (a.position ?? 0) - (b.position ?? 0)
          )
        "
        item-key="id"
        @change="handleColumnDragEvent"
      >
        <template #item="{ element }">
          <div class="list-group-item" style="cursor: grab">
            {{ element.label }}
          </div>
        </template>
      </draggable>
    </ContentBlockModal>
    <template #footer>
      <Button
        type="primary"
        size="small"
        label="Save"
        @click="showModal = false"
      />
      <Button
        type="secondary"
        size="small"
        label="Cancel"
        @click="handleCancel"
      />
    </template>
  </SideModal>
</template>
