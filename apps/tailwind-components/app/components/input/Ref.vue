<script setup lang="ts">
import type {
  ITableMetaData,
  columnValueObject,
  recordValue,
} from "../../../../metadata-utils/src/types";
import type { ITableDataResponse } from "../../composables/fetchTableData";

import { useDebounceFn } from "@vueuse/core";
import { computed, onMounted, ref, watch } from "vue";
import { type IInputProps, type IValueLabel } from "../../../types/types";
import { getPrimaryKey } from "../../utils/getPrimaryKey";
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
const options = ref<IValueLabel[]>([]);
const selection = ref<IValueLabel[]>([]);
const initialCount = ref<number>(0);
const count = ref<number>(0);
const searchTerms = ref<string>("");
let loadedRows = 0;
let latestRequest = 0;
let isLoadingMore = false;

const columnName = computed<string>(() => {
  return (tableMetadata.value?.label || tableMetadata.value?.id) as string;
});

const selectedKeys = computed((): string[] =>
  selection.value.map((entry) => entry.value)
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
    selection.value = await Promise.all((data.rows ?? []).map(toEntry));
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

async function toEntry(row: recordValue): Promise<IValueLabel> {
  return {
    value: JSON.stringify(await extractPrimaryKey(row)),
    label: applyTemplate(props.refLabel, row),
  };
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
  const rows = data.rows ?? [];
  const entries = await Promise.all(rows.map(toEntry));
  if (request < latestRequest) {
    return;
  }
  if (!append) {
    options.value = [];
    loadedRows = 0;
  }
  options.value.push(...entries);
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

function setSelection(keys: string[]) {
  const known = [...selection.value, ...options.value];
  const next = keys.map((key) => known.find((entry) => entry.value === key)!);
  selection.value = next;
  const rows = next.map((entry) => JSON.parse(entry.value));
  emit("update:modelValue", props.isArray ? rows : rows[0] ?? null);
  onBlur();
}

async function extractPrimaryKey(row: recordValue) {
  return await getPrimaryKey(row, props.refTableId, props.refSchemaId);
}

function select(key: string) {
  setSelection(props.isArray ? [...selectedKeys.value, key] : [key]);
}

function deselect(key: string) {
  setSelection(selectedKeys.value.filter((selected) => selected !== key));
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
    :options="options"
    :placeholder="placeholder"
    :invalid="invalid"
    :valid="valid"
    :disabled="disabled"
    :describedBy="describedBy"
    :modelValue="selection"
    @update:modelValue="
      setSelection(($event ?? []).map((entry) => entry.value))
    "
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
    :options="options"
    :placeholder="placeholder"
    :invalid="invalid"
    :valid="valid"
    :disabled="disabled"
    :describedBy="describedBy"
    :modelValue="selection[0] ?? null"
    @update:modelValue="setSelection($event == null ? [] : [$event.value])"
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
            :options="options"
            :modelValue="selectedKeys"
            @select="select"
            @deselect="deselect"
            :invalid="invalid"
            :valid="valid"
            :disabled="disabled"
          />
          <InputRadioGroup
            v-else
            :id="id"
            :options="options"
            :modelValue="selectedKeys[0]"
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
      v-if="selectedKeys.length"
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
