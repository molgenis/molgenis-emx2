<script setup lang="ts">
import {
  isColumnValueObject,
  type ITableMetaData,
  type columnValueObject,
  type recordValue,
} from "../../../../metadata-utils/src/types";
import type { ITableDataResponse } from "../../composables/fetchTableData";

import { useDebounceFn } from "@vueuse/core";
import { computed, onMounted, ref, watch } from "vue";
import { type IInputProps, type IValueLabel } from "../../../types/types";
import fetchRowPrimaryKey from "../../composables/fetchRowPrimaryKey";
import fetchTableData from "../../composables/fetchTableData";
import fetchTableMetadata from "../../composables/fetchTableMetadata";
import logger from "../../utils/logger";
import BaseIcon from "../BaseIcon.vue";
import Button from "../Button.vue";
import InputGroupContainer from "../input/InputGroupContainer.vue";
import TextNoResultsMessage from "../text/NoResultsMessage.vue";
import InputCheckboxGroup from "./CheckboxGroup.vue";
import InputListbox from "./Listbox.vue";
import InputRadioGroup from "./RadioGroup.vue";

const PAGE_SIZE = 20;

const props = withDefaults(
  defineProps<
    IInputProps & {
      refSchemaId: string;
      refTableId: string;
      refLabel: string;
      //todo, replace isArray with type="select"|"radio"|"checkbox"|"multiselect" and also enable this in emx2 metadata model
      isArray?: boolean;
      limit?: number;
    }
  >(),
  {
    isArray: true,
    limit: 20,
  }
);

const isInitLoading = ref(true);
const modelValue = defineModel<
  columnValueObject[] | columnValueObject | null
>();
const tableMetadata = ref<ITableMetaData>();

const emit = defineEmits(["focus", "blur", "update:modelValue"]);
const optionMap = ref<recordValue>({});
const selectionMap = ref<recordValue>({});
const initialCount = ref<number>(0);
const count = ref<number>(0);
const searchTerms = ref<string>("");
let loadedRows = 0;
let latestRequest = 0;
let isLoadingMore = false;

const columnName = computed<string>(() => {
  return (tableMetadata.value?.label || tableMetadata.value?.id) as string;
});

const optionLabels = computed((): string[] => Object.keys(optionMap.value));

const listOptions = computed((): IValueLabel[] =>
  optionLabels.value.map((label) => ({ value: label }))
);

const selectedLabels = computed((): string[] =>
  Object.keys(selectionMap.value)
);

const displayAsSelect = computed(() => initialCount.value > props.limit);

watch(
  () => props.refSchemaId,
  () => init
);
watch(
  () => props.refTableId,
  () => init
);
watch(
  () => modelValue.value,
  () => init
);

onMounted(() => {
  init();
});

async function init() {
  tableMetadata.value = await fetchTableMetadata(
    props.refSchemaId,
    props.refTableId
  );
  const isModalValueArray = Array.isArray(modelValue.value);
  if (
    modelValue.value &&
    (isModalValueArray ? modelValue.value.length : modelValue.value)
  ) {
    const keys = isModalValueArray
      ? await Promise.all(
          (modelValue.value as []).map((row) => extractPrimaryKey(row))
        )
      : await extractPrimaryKey(modelValue.value as columnValueObject);
    const data: ITableDataResponse = await fetchTableData(
      props.refSchemaId,
      props.refTableId,
      { filter: { equals: keys }, expandLevel: 1 }
    );
    if (data.rows) {
      data.rows.forEach(
        (row) => (selectionMap.value[applyTemplate(props.refLabel, row)] = row)
      );
    }
  }

  await loadOptions(false);
  if (count.value <= props.limit && loadedRows < count.value) {
    await loadOptions(true, count.value - loadedRows);
  }
  initialCount.value = count.value;
  isInitLoading.value = false;
}

function applyTemplate(template: string, row: Record<string, any>): string {
  const ids = Object.keys(row);
  const vals = Object.values(row);
  const label = new Function(...ids, "return `" + template + "`;")(...vals);
  return label;
}

async function loadOptions(append: boolean, pageSize = PAGE_SIZE) {
  const request = ++latestRequest;
  const data: ITableDataResponse = await fetchTableData(
    props.refSchemaId,
    props.refTableId,
    {
      limit: pageSize,
      offset: append ? loadedRows : 0,
      searchTerms: searchTerms.value,
      expandLevel: 1,
    }
  );
  if (request !== latestRequest) {
    return;
  }
  const rows = data.rows ?? [];
  if (!append) {
    optionMap.value = {};
    loadedRows = 0;
  }
  rows.forEach(
    (row) => (optionMap.value[applyTemplate(props.refLabel, row)] = row)
  );
  loadedRows += rows.length;
  count.value = data.count;
  logger.debug("loaded options for " + props.id);
}

function updateSearch(newSearchTerms: string | undefined) {
  const terms = newSearchTerms ?? "";
  if (terms !== searchTerms.value) {
    searchTerms.value = terms;
    loadOptions(false);
  }
}

async function loadMore() {
  if (isLoadingMore || loadedRows >= count.value) {
    return;
  }
  isLoadingMore = true;
  try {
    await loadOptions(true);
  } finally {
    isLoadingMore = false;
  }
}

async function setSelection(labels: string[]) {
  const next: recordValue = {};
  for (const label of labels) {
    next[label] = Object.hasOwn(selectionMap.value, label)
      ? selectionMap.value[label]
      : await primaryKeyOfOption(label);
  }
  selectionMap.value = next;
  emit(
    "update:modelValue",
    props.isArray ? Object.values(next) : Object.values(next)[0] ?? null
  );
  onBlur();
}

async function primaryKeyOfOption(label: string) {
  const row = optionMap.value[label];
  if (!isColumnValueObject(row)) {
    throw new Error("Invalid option value for label: " + label);
  }
  return await extractPrimaryKey(row);
}

async function extractPrimaryKey(row: recordValue) {
  return await fetchRowPrimaryKey(row, props.refTableId, props.refSchemaId);
}

function select(label: string) {
  setSelection(props.isArray ? [...selectedLabels.value, label] : [label]);
}

function deselect(label: string) {
  setSelection(selectedLabels.value.filter((selected) => selected !== label));
}

function clearSelection() {
  setSelection([]);
}

const onBlur = useDebounceFn(() => {
  emit("blur");
}, 100);
</script>

<template>
  <div v-show="isInitLoading" class="h-20 flex justify-start items-center">
    <BaseIcon name="progress-activity" class="animate-spin text-input" />
  </div>
  <InputListbox
    v-if="displayAsSelect && isArray"
    multiple
    enableSearch
    showClearButton
    :id="id"
    :options="optionLabels"
    :placeholder="placeholder"
    :invalid="invalid"
    :valid="valid"
    :disabled="disabled"
    :describedBy="describedBy"
    :modelValue="selectedLabels"
    @update:modelValue="setSelection($event ?? [])"
    @search="updateSearch($event)"
    @load-more="loadMore"
    @focus="emit('focus')"
    @blur="onBlur"
  />
  <InputListbox
    v-else-if="displayAsSelect"
    enableSearch
    showClearButton
    :id="id"
    :options="optionLabels"
    :placeholder="placeholder"
    :invalid="invalid"
    :valid="valid"
    :disabled="disabled"
    :describedBy="describedBy"
    :modelValue="selectedLabels[0] ?? null"
    @update:modelValue="setSelection($event == null ? [] : [$event])"
    @search="updateSearch($event)"
    @load-more="loadMore"
    @focus="emit('focus')"
    @blur="onBlur"
  />
  <template v-else-if="initialCount">
    <div
      :class="{
        'border-valid text-valid': valid && !disabled,
        'border-invalid text-invalid': invalid && !disabled,
        'text-disabled cursor-not-allowed': disabled,
        'bg-disabled border-valid text-valid cursor-not-allowed':
          valid && disabled,
        'bg-disabled border-invalid text-invalid cursor-not-allowed':
          invalid && disabled,
        'text-input hover:border-input-hover focus-within:border-input-focused':
          !disabled && !invalid && !valid,
      }"
    >
      <InputGroupContainer
        :id="`${id}-ref`"
        class="border-transparent w-full relative"
        @focus="emit('focus')"
        @blur="onBlur"
      >
        <fieldset>
          <legend class="sr-only">select {{ columnName }} options</legend>
          <InputCheckboxGroup
            v-if="isArray"
            :id="id"
            :options="listOptions"
            :modelValue="selectedLabels"
            @select="select"
            @deselect="deselect"
            :invalid="invalid"
            :valid="valid"
            :disabled="disabled"
          />
          <InputRadioGroup
            v-else
            :id="id"
            :options="listOptions"
            :modelValue="selectedLabels[0]"
            @select="select"
            @deselect="deselect"
            :invalid="invalid"
            :valid="valid"
            :disabled="disabled"
          />
        </fieldset>
      </InputGroupContainer>
    </div>
    <Button
      v-if="selectedLabels.length"
      @click="clearSelection"
      type="text"
      size="tiny"
      iconPosition="right"
      class="mr-2 underline cursor-pointer"
    >
      Clear
    </Button>
  </template>
  <div
    v-else-if="!isInitLoading"
    class="py-4 flex justify-start items-center text-input-description"
  >
    <TextNoResultsMessage label="No options available" />
  </div>
</template>
