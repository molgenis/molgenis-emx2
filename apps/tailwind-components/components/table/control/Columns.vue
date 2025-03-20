<template>
  <Button type="outline" icon="columns" size="small" @click="showModal = true">
    Columns
  </Button>
  <SideModal
    class="hidden"
    :slide-in-right="true"
    :fullScreen="false"
    button-alignment="left"
    :show="showModal"
    @close="showModal = false"
  >
    <ContentBlockModal title="Columns">
      <draggable
        tag="ul"
        :list="columnsInColumnsSelectModal"
        item-key="id"
        @change="handleColumnDragEvent"
      >
        <template #item="{ element }">
          <li class="mt-2.5 relative hover:bg-tab-hover hover:cursor-grab">
            <div class="flex justify-between">
              <div class="flex items-start">
                <div class="flex items-center">
                  <InputCheckbox
                    v-model="element.visible"
                    :id="element.id"
                    class="w-5 h-5 rounded-3px ml-[6px] mr-2.5 mt-0.5 accent-yellow-500 border border-checkbox"
                  />
                </div>
                <label
                  class="hover:cursor-pointer text-body-sm group"
                  :for="element.id"
                >
                  {{ element.label }}
                </label>
              </div>
              <BaseIcon name="equal" class="hover:cursor-grab" />
            </div>
          </li>
        </template>
      </draggable>
    </ContentBlockModal>
    <template #footer>
      <Button type="primary" size="small" label="Save" @click="handleSave" />
      <Button
        type="secondary"
        size="small"
        label="Cancel"
        class="ml-2.5"
        @click="handleCancel"
      />
    </template>
  </SideModal>
</template>

<script setup lang="ts">
import draggable from "vuedraggable";
import type { IColumnConfig } from "~/types/types";
import { sortColumns } from "~/utils/sortColumns";
import type { IColumn } from "../../../../metadata-utils/src/types";

const props = defineProps<{
  columns: IColumn[];
}>();

const emits = defineEmits(["update:columns"]);

const showModal = ref(false);
const columnsInColumnsSelectModal = ref<IColumnConfig[]>(
  indexColumns(sortColumns(props.columns.map(columnToColumnConfig)))
);

function indexColumns(columns: IColumnConfig[]) {
  return columns.map((column, index) => {
    column.position = index;
    return column;
  });
}

function handleColumnDragEvent() {
  columnsInColumnsSelectModal.value = indexColumns(
    columnsInColumnsSelectModal.value
  );
}

function handleCancel() {
  columnsInColumnsSelectModal.value = sortColumns(
    indexColumns(props.columns.map(columnToColumnConfig))
  );
  showModal.value = false;
}

function handleSave() {
  const updated = props.columns.map((column: IColumn) => {
    const columnConfig = columnsInColumnsSelectModal.value.find(
      (col) => col.id === column.id
    );
    if (columnConfig) {
      const newColumn = { ...column };
      newColumn.position = columnConfig.position;
      newColumn.visible = columnConfig.visible ? "true" : "false";
      return newColumn;
    } else {
      return column;
    }
  });

  emits("update:columns", updated);
  showModal.value = false;
}

function columnToColumnConfig(column: IColumn): IColumnConfig {
  return {
    id: column.id,
    label: column.label || column.id,
    position: column.position ?? 0,
    visible: column.visible === "false" ? false : true,
  };
}
</script>
