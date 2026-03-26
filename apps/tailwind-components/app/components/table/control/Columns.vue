<template>
  <Button type="outline" icon="columns" @click="showModal = true">
    Columns
  </Button>
  <Modal
    v-model:visible="showModal"
    type="right"
    title="Columns"
    subtitle="add table name hereu"
    :background-accessible="false"
  >
      <div class="px-8 py-5">
        <div class="flex flex-row mb-5">
          <InputSelect
            id="column-sorting-select"
            label="Sort by"
            v-model="selectedSortMethod"
            :options="sortMethods"
            @change="handleSortMethodChanged"
          />
          <Button
            type="text"
            size="small"
            label="Reset to default"
            icon="RestartAlt"
            class="ml-4 whitespace-nowrap"
            :onclick="resetToDefault"
          />

        </div>
      <div class="flex flex-row" v-for="option in columnsInColumnsSelectModal" :key="option.id">

        <InputLabel
          :for="`column-checkbox-group-${option.label}`"
          class="group flex justify-start items-center relative text-title-contrast"
        >
          <input
            type="checkbox"
            :id="`column-checkbox-group-${option.label}`"
            :value="option.visible"
            v-model="option.visible"
            class="ml-4 mt-2 sr-only"
          />
          <InputCheckboxIcon
            :checked="option.visible"
          />
          <span class="block">
            {{ option.label }}
          </span>

        </InputLabel>
          <BaseIcon name="drag-horizontal" class="ml-auto hover:cursor-grab" style="opacity: 0.2;"/>
      </div>

    </div>
    <template #footer>
      <div class="flex gap-2 justify-start py-2">
        <Button type="primary" size="small" label="Save" @click="handleSave" />
        <Button
          type="secondary"
          size="small"
          label="Cancel"
          @click="handleCancel"
        />
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";
import { sortColumns } from "../../../utils/sortColumns";
import BaseIcon from "../../BaseIcon.vue";
import Modal from "../../Modal.vue";
import InputSelect from "../../input/Select.vue";
import InputCheckbox from "../../input/Checkbox.vue";
import Button from "../../Button.vue";

const SORTING_METHODS = ["Default", "Ascending", "Descending", "Custom"];

const props = defineProps<{
  columns: IColumn[];
}>();

const emits = defineEmits(["update:columns"]);

const showModal = ref(false);
const columnsInColumnsSelectModal = ref<IColumnConfig[]>([]);
const sortMethods = ref<string[]>(SORTING_METHODS);
const selectedSortMethod = ref<string>("Default");

watch(() => props.columns, initializeColumns, { immediate: true });

function initializeColumns(newColumns: IColumn[]) {
  columnsInColumnsSelectModal.value = indexColumns(
    sortColumns(newColumns.map(columnToColumnConfig))
  );
}

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
  selectedSortMethod.value = "Custom";
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

function resetToDefault() {
  columnsInColumnsSelectModal.value = indexColumns(
    sortColumns(props.columns.map(columnToColumnConfig))
  );
  selectedSortMethod.value = "Default";
}

function handleSortMethodChanged(event: Event) {
  const target = event.target as HTMLSelectElement;
  selectedSortMethod.value = target.value;
  switch (target.value) {
    case "Ascending":
      columnsInColumnsSelectModal.value = indexColumns(
        columnsInColumnsSelectModal.value.sort((a, b) =>
          a.label.localeCompare(b.label)
        )
      );
      break;
    case "Descending":
      columnsInColumnsSelectModal.value = indexColumns(
        columnsInColumnsSelectModal.value
          .sort((a, b) => a.label.localeCompare(b.label))
          .reverse()
      );
      break;
    case "Default":
      resetToDefault();
      break;
  }
}

interface IColumnConfig {
  id: string;
  label: string;
  position: number;
  visible: boolean;
}
</script>
