<script lang="ts" setup>
import { ref, computed, watch, onMounted } from "vue";
import type { Ref } from "vue";

import { InputSearch, InputListbox, DisplayRecord } from "#components";
import InputDropdownOption from "./dropdown/InputOption.vue";
import InputDropdownToggle from "./dropdown/Toggle.vue";
import InputDropdownContainer from "./dropdown/Container.vue";
import InputDropdownToolbar from "./dropdown/Toolbar.vue";

import logger from "../../utils/logger";
import fetchTableData from "../../composables/fetchTableData";

import type { IQueryMetaData } from "../../../molgenis-components/src/client/IQueryMetaData.ts";
import { fetchTableMetadata } from "#imports";
import type { ITableDataResponse } from "../../composables/fetchTableData";
import type { IInputProps } from "../../types/types";
import type {
  ITableMetaData,
  columnValueObject,
  recordValue,
  IInputValueLabel,
} from "../../../metadata-utils/src/types";

const props = withDefaults(
  defineProps<
    IInputProps & {
      refSchemaId: string;
      refTableId: string;
      refLabel: string;
      multiselect?: boolean;
      limit?: number;
      showMgColumns?: boolean;
    }
  >(),
  {
    placeholder: "Select an option",
    multiselect: false,
    limit: 10,
    showMgColumns: false,
  }
);

const emit = defineEmits([
  "update:modelValue",
  "error",
  "blur",
  "focus",
  "search",
]);

const isExpanded = ref<boolean>(false);
const toggleElemRef = ref<InstanceType<typeof InputDropdownToggle>>();
const displayText = ref<string>(props.placeholder);
const searchTerm = defineModel<string>("");
const sortMethod = ref<string>();

const modelValue = defineModel<columnValueObject[] | columnValueObject>();
const tableMetadata = ref<ITableMetaData>();

const count = ref<number>(0);
const offset = ref<number>(0);
const selectionMap: Ref<recordValue> = ref({});
const optionMap: Ref<recordValue> = ref({});
const initialCount = ref<number>(0);
const hasNoResults = ref<boolean>(true);

function updateDisplayText(text: string | undefined | null): string {
  if (typeof text === "undefined" || text === null || text === "") {
    return props.placeholder;
  }
  return text;
}

async function prepareModel() {
  tableMetadata.value = await fetchTableMetadata(
    props.refSchemaId,
    props.refTableId
  );

  const filters =
    Array.isArray(modelValue.value) && modelValue.value.length > 0
      ? (modelValue.value as []).map((value) => extractPrimaryKey(value))
      : extractPrimaryKey(modelValue.value);
  const data: ITableDataResponse = await fetchTableData(
    props.refSchemaId,
    props.refTableId,
    { filter: { equals: filters } }
  );
  if (data.rows) {
    hasNoResults.value = false;
    data.rows.forEach(
      (row) => (selectionMap.value[applyTemplate(props.refLabel, row)] = row)
    );
  }

  await loadOptions({ limit: props.limit });
  initialCount.value = count.value;
}

function applyTemplate(template: string, row: Record<string, any>): string {
  const ids = Object.keys(row);
  const vals = Object.values(row);
  const label = new Function(...ids, "return `" + template + "`;")(...vals);
  return label;
}

async function loadOptions(filter: IQueryMetaData) {
  hasNoResults.value = true;
  if (sortMethod.value) {
    filter.orderby = {};
    filter.orderby[sortMethod.value] = "ASC";
  }
  const data: ITableDataResponse = await fetchTableData(
    props.refSchemaId,
    props.refTableId,
    filter
  );

  optionMap.value = {};

  if (data.rows) {
    hasNoResults.value = false;
    data.rows.forEach(
      (row) => (optionMap.value[applyTemplate(props.refLabel, row)] = row)
    );
    count.value = data.count;
  } else {
    hasNoResults.value = true;
  }
  logger.debug("loaded options for " + props.id);
}

function extractPrimaryKey(value: any) {
  const result = {} as columnValueObject;
  tableMetadata.value?.columns
    .filter((column) => column.key === 1)
    .forEach((column) => {
      result[column.id] = value[column.id];
    });
  return result;
}

function select(label: string) {
  if (!props.multiselect) {
    selectionMap.value = {};
  }
  selectionMap.value[label] = optionMap.value[label];
  emit(
    "update:modelValue",
    props.multiselect
      ? Object.values(selectionMap.value).map((value) =>
          extractPrimaryKey(value)
        )
      : extractPrimaryKey(optionMap.value[label])
  );
}

function deselect(label: string) {
  delete selectionMap.value[label];
  emit(
    "update:modelValue",
    props.multiselect
      ? Object.values(selectionMap.value).map((value) =>
          extractPrimaryKey(value)
        )
      : undefined
  );
}

function updateSearch(newSearchTerms: string | undefined) {
  optionMap.value = {};
  offset.value = 0;
  searchTerm.value = newSearchTerms;
  loadOptions({ limit: props.limit, searchTerms: searchTerm.value });
}

function clearSelection() {
  selectionMap.value = {};
  emit("update:modelValue", props.multiselect ? [] : undefined);
  updateSearch(""); //reset
}

function loadMore() {
  offset.value += props.limit;
  loadOptions({
    offset: offset.value,
    limit: props.limit,
    searchTerms: searchTerm.value,
  });
}

watch(
  () => props.refSchemaId,
  () => prepareModel
);
watch(
  () => props.refTableId,
  () => prepareModel
);

watch(
  () => modelValue.value,
  () => {
    if (!props.multiselect) {
      delete selectionMap.value[Object.keys(selectionMap.value)[0]];
      if (modelValue.value) {
        selectionMap.value[applyTemplate(props.refLabel, modelValue.value)] =
          modelValue.value;
      }
    } else {
      selectionMap.value = {};
      if (
        modelValue.value &&
        Array.isArray(modelValue.value) &&
        modelValue.value.length > 0
      ) {
        modelValue.value.forEach((value) => {
          selectionMap.value[applyTemplate(props.refLabel, value)] = value;
        });
      }
    }
  }
);

function getColumns() {
  return (
    tableMetadata.value?.columns.filter(
      (column) =>
        (column.columnType != "HEADING" && !column.id.startsWith("mg")) ||
        props.showMgColumns
    ) || []
  );
}

watch([props.placeholder], () => {
  displayText.value = updateDisplayText("");
});

watch(
  () => searchTerm.value,
  () => updateSearch(searchTerm.value)
);
watch(
  () => sortMethod.value,
  () => loadOptions({ limit: props.limit, searchTerms: searchTerm.value })
);

// onMounted(() => prepareModel());
prepareModel();
</script>

<template>
  <InputGroupContainer
    :id="`${id}-ref-dropdown`"
    class="w-full relative"
    @focus="emit('focus')"
    @blur="emit('blur')"
  >
    <InputDropdownToggle
      :id="id"
      :elemIdControlledByToggle="`${id}-ref-dropdown-content`"
      ref="toggleElemRef"
      @click="isExpanded = !isExpanded"
    >
      <template #ref-dropdown-label>
        <span class="w-full">
          {{ displayText }}
        </span>
      </template>
    </InputDropdownToggle>
    <InputDropdownContainer
      :id="`${id}-ref-dropdown-content`"
      :aria-expanded="isExpanded"
      :tabindex="isExpanded ? 1 : 0"
      class="border rounded"
      :class="{
        hidden: disabled || !isExpanded,
      }"
    >
      <!-- for background -->
      <InputDropdownToolbar class="">
        <div class="w-full">
          <label :for="`${id}-ref-dropdown-search`" class="sr-only">
            search for values
          </label>
          <InputSearch
            :id="`${id}ref-dropdown-search`"
            v-model="searchTerm"
            placeholder="Search"
          />
        </div>
        <div class="w-[175px]">
          <label
            :id="`${id}-ref-dropdown-sort-input-label`"
            :for="`${id}-ref-dropdown-sort-input`"
            class="sr-only"
          >
            sort data by
          </label>
          <InputListbox
            :id="`${id}-ref-dropdown-sorting`"
            :labelId="`${id}-ref-dropdown-sort-input-label`"
            :options="
              getColumns().map((column) => {
                return { value: column.id, label: column.label };
              })
            "
            @update:model-value="(value: string) => (sortMethod = value?.value)"
            :enable-search="false"
            placeholder="Sort data by"
          />
        </div>
      </InputDropdownToolbar>
      <fieldset :id="`${id}-ref-dropdown-options`">
        <label></label>
        <!-- need to implement :checked on input option component -->

        <div v-for="(option, label) in optionMap">
          <InputDropdownOption
            :id="(label as string)"
            :label="label"
            :option="(option as recordValue)"
            :multiselect="multiselect"
            :checked="Object.hasOwn(selectionMap, label)"
            @select="select"
            @deselect="deselect"
          >
            <DisplayRecord
              :table-metadata="tableMetadata"
              :row="(option as recordValue)"
              :showMgColumns="showMgColumns"
            />
          </InputDropdownOption>
        </div>
      </fieldset>
    </InputDropdownContainer>
  </InputGroupContainer>
</template>
