<script lang="ts" setup>
import { ref, computed, watch, onMounted } from "vue";
import type { Ref } from "vue";

import {
  InputSearch,
  InputListbox,
  DisplayRecord,
  InputDropdownContainer,
  InputDropdownInputOption,
  InputDropdownToggle,
  InputDropdownToolbar,
} from "#components";

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

const modelValue = defineModel<columnValueObject[] | columnValueObject>();
const tableMetadata = ref<ITableMetaData>();
const optionMap: Ref<recordValue> = ref({});
const selectionMap: Ref<recordValue> = ref({});

const initialCount = ref<number>(0);
const counter = ref<number>(0);
const counterOffset = ref<number>(0);
const hasNoResults = ref<boolean>(true);

const displayText = ref<string>(props.placeholder);
const searchTerm = defineModel<string>("");
const sortMethod = ref<string>();
const isExpanded = ref<boolean>(false);

const toggleElemRef = ref<InstanceType<typeof InputDropdownToggle>>();

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
  initialCount.value = counter.value;
}

const orderByColumnNames = computed<IInputValueLabel[]>(() => {
  return (
    tableMetadata.value?.columns
      .filter((column) => {
        return (
          (column.columnType !== "HEADING" && !column.id.startsWith("mg_")) ||
          props.showMgColumns
        );
      })
      .map((column) => {
        return { value: column.id, label: column.label };
      }) || []
  );
});

function updateDisplayText(text: string | undefined | null): string {
  if (typeof text === "undefined" || text === null || text === "") {
    return props.placeholder;
  }
  return text;
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
    counter.value = data.count;
  } else {
    hasNoResults.value = true;
  }
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
  counterOffset.value = 0;
  searchTerm.value = newSearchTerms;
  loadOptions({ limit: props.limit, searchTerms: searchTerm.value });
}

function clearSelection() {
  selectionMap.value = {};
  emit("update:modelValue", props.multiselect ? [] : undefined);
  updateSearch(""); //reset
}

function loadMore() {
  counterOffset.value += props.limit;
  loadOptions({
    offset: counterOffset.value,
    limit: props.limit,
    searchTerms: searchTerm.value,
  });
}

onMounted(() => prepareModel());

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

watch([props.placeholder], () => {
  displayText.value = updateDisplayText("");
});

watch(
  () => searchTerm.value,
  () => {
    updateSearch(searchTerm.value);
  }
);

watch(
  () => sortMethod.value,
  () => {
    loadOptions({ limit: props.limit, searchTerms: searchTerm.value });
  }
);
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
        'max-h-82.5': isExpanded,
      }"
    >
      <InputDropdownToolbar class="grid grid-cols-[2fr_1fr]">
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
        <div>
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
            :options="orderByColumnNames"
            @update:model-value="(value: IInputValueLabel) => (sortMethod = value?.value as string)"
            :enable-search="false"
            placeholder="Sort by"
          />
        </div>
      </InputDropdownToolbar>
      <fieldset :id="`${id}-ref-dropdown-options`">
        <label class="sr-only">
          {{
            props.multiselect
              ? "select one or more options"
              : "select an option"
          }}
        </label>
        <template v-if="!hasNoResults">
          <div
            v-for="(option, label) in optionMap"
            class="border-b last:border-none"
          >
            <InputDropdownInputOption
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
                :input-row-data="(option as recordValue)"
                :showMgColumns="showMgColumns"
              />
            </InputDropdownInputOption>
          </div>
        </template>
        <div
          :id="`${id}-ref-dropdown-options-status`"
          class="flex justify-center items-center"
          :class="{
            'h-[56px]': hasNoResults,
          }"
          role="status"
          aria-atomic="true"
        >
          <TextNoResultsMessage v-if="hasNoResults" />
        </div>
      </fieldset>
    </InputDropdownContainer>
  </InputGroupContainer>
</template>
