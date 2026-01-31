<template>
  <Button
    :type="buttonType"
    :size="size"
    :icon="iconComputed"
    @click="showModal = true"
  >
    {{ labelComputed }}
  </Button>
  <SideModal
    class="hidden"
    :slide-in-right="true"
    :fullScreen="false"
    button-alignment="left"
    :show="showModal"
    :include-footer="false"
    @close="showModal = false"
  >
    <ContentBlockModal :title="labelComputed">
      <template v-slot:title-button>
        <Button
          type="text"
          size="small"
          label="Reset to default"
          icon="RestartAlt"
          class="leading-9"
          :onclick="resetToDefault"
        />
      </template>

      <InputSelect
        id="column-sorting-select"
        label="Sort by"
        v-model="selectedSortMethod"
        :options="sortMethods"
        @change="handleSortMethodChanged"
      />

      <div class="mb-4">
        <div class="font-semibold mb-2">
          data (
          <button
            type="button"
            class="text-link hover:underline text-body-sm"
            @click="showAll(true)"
          >
            all
          </button>
          <button
            type="button"
            class="text-link hover:underline text-body-sm ml-2"
            @click="hideAll(true)"
          >
            none
          </button>
          )
        </div>
        <ul>
          <li
            v-for="element in dataColumns"
            :key="element.id"
            class="mt-2.5 relative hover:bg-tab-hover hover:cursor-grab"
          >
            <div class="flex justify-between">
              <div class="flex items-start">
                <div class="flex items-center">
                  <InputCheckbox
                    v-model="element[checkAttribute]"
                    :id="element.id"
                    class="w-5 h-5 rounded-3px ml-[6px] mr-2.5 mt-0.5 border border-checkbox accent-primary"
                  />
                </div>
                <label
                  class="text-body-base hover:cursor-pointer group"
                  :for="element.id"
                >
                  {{ element.label }}
                </label>
              </div>
              <BaseIcon name="equal" class="hover:cursor-grab" />
            </div>
          </li>
        </ul>
      </div>

      <div v-if="metadataColumns.length">
        <div class="font-semibold mb-2">
          metadata (
          <button
            type="button"
            class="text-link hover:underline text-body-sm"
            @click="showAll(false)"
          >
            all
          </button>
          <button
            type="button"
            class="text-link hover:underline text-body-sm ml-2"
            @click="hideAll(false)"
          >
            none
          </button>
          )
        </div>
        <ul>
          <li
            v-for="element in metadataColumns"
            :key="element.id"
            class="mt-2.5 relative hover:bg-tab-hover hover:cursor-grab"
          >
            <div class="flex justify-between">
              <div class="flex items-start">
                <div class="flex items-center">
                  <InputCheckbox
                    v-model="element[checkAttribute]"
                    :id="element.id"
                    class="w-5 h-5 rounded-3px ml-[6px] mr-2.5 mt-0.5 border border-checkbox accent-primary"
                  />
                </div>
                <label
                  class="text-body-base hover:cursor-pointer text-body-sm group"
                  :for="element.id"
                >
                  {{ element.label }}
                </label>
              </div>
              <BaseIcon name="equal" class="hover:cursor-grab" />
            </div>
          </li>
        </ul>
      </div>
    </ContentBlockModal>
  </SideModal>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from "vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";
import { sortColumns } from "../../../utils/sortColumns";
import BaseIcon from "../../BaseIcon.vue";
import SideModal from "../../SideModal.vue";
import ContentBlockModal from "../../content/ContentBlockModal.vue";
import InputSelect from "../../input/Select.vue";
import InputCheckbox from "../../input/Checkbox.vue";
import Button from "../../Button.vue";

const SORTING_METHODS = ["Default", "Ascending", "Descending", "Custom"];

const props = withDefaults(
  defineProps<{
    columns: IColumn[];
    mode?: "columns" | "filters";
    size?: "tiny" | "small" | "medium" | "large";
    label?: string;
    icon?: string;
    buttonType?: "outline" | "text" | "primary" | "secondary";
  }>(),
  {
    mode: "columns",
    size: "medium",
    buttonType: "outline",
  }
);

const emits = defineEmits(["update:columns"]);

const showModal = ref(false);
const columnsInColumnsSelectModal = ref<IColumnConfig[]>([]);
const sortMethods = ref<string[]>(SORTING_METHODS);
const selectedSortMethod = ref<string>("Default");

const iconComputed = computed(() =>
  props.icon ?? (props.mode === "filters" ? "filter" : "columns")
);

const labelComputed = computed(() =>
  props.label ?? (props.mode === "filters" ? "Filters" : "Columns")
);

const checkAttribute = computed(() =>
  props.mode === "filters" ? "showFilter" : "visible"
);

const dataColumns = computed(() =>
  columnsInColumnsSelectModal.value.filter((col) => !col.id.startsWith("mg_"))
);

const metadataColumns = computed(() =>
  columnsInColumnsSelectModal.value.filter((col) => col.id.startsWith("mg_"))
);

const isInitializing = ref(false);

watch(() => props.columns, initializeColumns, { immediate: true });

function initializeColumns(newColumns: IColumn[]) {
  isInitializing.value = true;
  columnsInColumnsSelectModal.value = indexColumns(
    sortColumns(newColumns.map(columnToColumnConfig))
  );
  nextTick(() => {
    isInitializing.value = false;
  });
}

function emitUpdate() {
  if (isInitializing.value) return;
  const attr = checkAttribute.value;
  const updated = props.columns.map((column: IColumn) => {
    const columnConfig = columnsInColumnsSelectModal.value.find(
      (col) => col.id === column.id
    );
    if (columnConfig) {
      const newColumn = { ...column };
      newColumn.position = columnConfig.position;
      if (attr === "visible") {
        newColumn.visible = columnConfig.visible ? "true" : "false";
      } else {
        newColumn.showFilter = columnConfig.showFilter;
      }
      return newColumn;
    }
    return column;
  });
  emits("update:columns", updated);
}

watch(columnsInColumnsSelectModal, emitUpdate, { deep: true });

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

function columnToColumnConfig(column: IColumn): IColumnConfig {
  const attr = checkAttribute.value;
  let attrValue: boolean;

  if (attr === "visible") {
    attrValue = column.visible === "false" ? false : true;
  } else {
    attrValue = column.showFilter !== false;
  }

  return {
    id: column.id,
    label: column.label || column.id,
    position: column.position ?? 0,
    [attr]: attrValue,
  } as IColumnConfig;
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
  visible?: boolean;
  showFilter?: boolean;
}

function showAll(isData: boolean) {
  const attr = checkAttribute.value;
  const updated = columnsInColumnsSelectModal.value.map((col) => {
    if (
      (isData && !col.id.startsWith("mg_")) ||
      (!isData && col.id.startsWith("mg_"))
    ) {
      return { ...col, [attr]: true };
    }
    return col;
  });
  columnsInColumnsSelectModal.value = updated;
}

function hideAll(isData: boolean) {
  const attr = checkAttribute.value;
  const updated = columnsInColumnsSelectModal.value.map((col) => {
    if (
      (isData && !col.id.startsWith("mg_")) ||
      (!isData && col.id.startsWith("mg_"))
    ) {
      return { ...col, [attr]: false };
    }
    return col;
  });
  columnsInColumnsSelectModal.value = updated;
}
</script>
