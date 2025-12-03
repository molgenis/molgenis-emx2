<script setup lang="ts">
import type { ITableDataResponse } from "../../composables/fetchTableData";
import type { IQueryMetaData } from "../../../../metadata-utils/src/IQueryMetaData";
import type {
  ITableMetaData,
  columnValueObject,
  recordValue,
} from "../../../../metadata-utils/src/types";

import { type IInputProps, type IValueLabel } from "../../../types/types";
import logger from "../../utils/logger";
import { fetchTableMetadata } from "#imports";
import {
  ref,
  type Ref,
  computed,
  watch,
  onMounted,
  useTemplateRef,
  nextTick,
} from "vue";
import fetchTableData from "../../composables/fetchTableData";
import type { IColumn } from "../../../../metadata-utils/src/types";
import InputCheckboxGroup from "./CheckboxGroup.vue";
import InputRadioGroup from "./RadioGroup.vue";
import InputGroupContainer from "../input/InputGroupContainer.vue";
import ButtonText from "../button/Text.vue";
import Button from "../Button.vue";
import BaseIcon from "../BaseIcon.vue";
import TextNoResultsMessage from "../text/NoResultsMessage.vue";
import { useClickOutside } from "../../composables/useClickOutside";

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
    limit: 50, //can be rather larger now because will auto load more if needed
  }
);

const initLoading = ref(true);
const modelValue = defineModel<columnValueObject[] | columnValueObject>();
const tableMetadata = ref<ITableMetaData>();

const emit = defineEmits(["focus", "blur", "error", "update:modelValue"]);
const optionMap: Ref<recordValue> = ref({});
const selectionMap: Ref<recordValue> = ref({});
const initialCount = ref<number>(0);
const count = ref<number>(0);
const offset = ref<number>(0);
const showSearch = ref<boolean>(false);
const searchTerms: Ref<string> = ref("");
const hasNoResults = ref<boolean>(true);
const showSelect = ref(false);

const columnName = computed<string>(() => {
  return (tableMetadata.value?.label || tableMetadata.value?.id) as string;
});

//computed elements to translate to CheckboxGroup or
const listOptions = computed(() => {
  return Object.keys(optionMap.value).map((label) => {
    return { value: label } as IValueLabel;
  });
});
const selection = computed(() =>
  props.isArray
    ? (Object.keys(selectionMap.value) as string[])
    : (Object.keys(selectionMap.value)[0] as string)
);

async function prepareModel() {
  tableMetadata.value = await fetchTableMetadata(
    props.refSchemaId,
    props.refTableId
  );

  if (
    modelValue.value && Array.isArray(modelValue.value)
      ? modelValue.value.length > 0
      : modelValue.value
  ) {
    const data: ITableDataResponse = await fetchTableData(
      props.refSchemaId,
      props.refTableId,
      { filter: { equals: extractPrimaryKey(modelValue.value) } }
    );
    if (data.rows) {
      hasNoResults.value = false;
      data.rows.forEach(
        (row) => (selectionMap.value[applyTemplate(props.refLabel, row)] = row)
      );
    }
  }

  await loadOptions({ limit: props.limit });
  initialCount.value = count.value;
  initLoading.value = false;
}

watch(
  () => props.refSchemaId,
  () => prepareModel
);
watch(
  () => props.refTableId,
  () => prepareModel
);

// the selectionMap can not be a computed property because it needs to initialized asynchronously therefore use a watcher instead of a computed property
// todo: move the options fetch to the outside of the component and pass it as a (synchronous) prop
watch(
  () => modelValue.value,
  () => {
    if (props.isArray === false) {
      const key = Object.keys(selectionMap.value)[0];
      if (key !== undefined) {
        delete selectionMap.value[key];
      }
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

function applyTemplate(template: string, row: Record<string, any>): string {
  const ids = Object.keys(row);
  const vals = Object.values(row);
  const label = new Function(...ids, "return `" + template + "`;")(...vals);
  return label;
}

async function loadOptions(filter: IQueryMetaData) {
  hasNoResults.value = true;
  const data: ITableDataResponse = await fetchTableData(
    props.refSchemaId,
    props.refTableId,
    filter
  );

  if (!filter.offset) {
    optionMap.value = {}; //empty
  }
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

function toggleSearch() {
  showSearch.value = !showSearch.value;
  if (searchTerms.value) updateSearch("");
}

function updateSearch(newSearchTerms: string) {
  optionMap.value = {};
  offset.value = 0;
  searchTerms.value = newSearchTerms;
  loadOptions({ limit: props.limit, searchTerms: searchTerms.value });
}

function select(label: string) {
  if (!props.isArray) {
    selectionMap.value = {};
  }
  selectionMap.value[label] = optionMap.value[label];
  if (searchTerms.value) toggleSearch();
  emit(
    "update:modelValue",
    props.isArray
      ? Object.values(selectionMap.value).map((value) =>
          extractPrimaryKey(value)
        )
      : extractPrimaryKey(optionMap.value[label])
  );
}

function extractPrimaryKey(value: any) {
  const result = {} as columnValueObject;
  tableMetadata.value?.columns
    .filter((column: IColumn) => column.key === 1)
    .forEach((column: IColumn) => {
      result[column.id] = value[column.id];
    });
  return result;
}

function deselect(label: string) {
  delete selectionMap.value[label];
  if (searchTerms.value) toggleSearch();
  emit(
    "update:modelValue",
    props.isArray
      ? Object.values(selectionMap.value).map((value) =>
          extractPrimaryKey(value)
        )
      : undefined
  );
}

function clearSelection() {
  selectionMap.value = {};
  emit("update:modelValue", props.isArray ? [] : undefined);
  updateSearch(""); //reset
}

function loadMore() {
  offset.value += 25; //small number is more smooth
  loadOptions({
    offset: offset.value,
    limit: props.limit,
    searchTerms: searchTerms.value,
  });
}

const displayAsSelect = computed(() => initialCount.value > props.limit);

prepareModel();

// Close dropdown when clicking outside
const wrapperRef = ref<HTMLElement | null>(null);
useClickOutside(wrapperRef, () => {
  showSelect.value = false;
});

//observer to know when to load more values
const sentinel = ref();
onMounted(() => {
  const observer = new IntersectionObserver(
    async (entries) => {
      const entry = entries[0];
      if (entry?.isIntersecting) {
        loadMore();
      }
    },
    {
      root: wrapperRef.value, // the container
      threshold: 0.1,
      rootMargin: "100px", //more smooth
    }
  );

  observer.observe(sentinel.value);
});
</script>

<template>
  <div v-show="initLoading" class="h-20 flex justify-start items-center">
    <BaseIcon name="progress-activity" class="animate-spin text-input" />
  </div>
  <div
    v-show="!initLoading && initialCount"
    :class="{
      'flex items-center border outline-none rounded-input cursor-pointer':
        displayAsSelect,
    }"
    @click.stop="displayAsSelect ? (showSelect = true) : null"
  >
    <InputGroupContainer
      :id="`${id}-ref`"
      class="border-transparent w-full relative"
      @focus="emit('focus')"
      @blur="emit('blur')"
    >
      <div
        v-show="displayAsSelect"
        class="flex items-center justify-between gap-2 m-2"
        @click.stop="showSelect = !showSelect"
      >
        <div class="flex flex-wrap items-center gap-2">
          <template v-if="modelValue" role="group">
            <Button
              @click="clearSelection"
              v-if="isArray && selection.length > 1"
              type="filterWell"
              size="tiny"
              icon="cross"
              iconPosition="right"
              class="mr-2"
              >clear all</Button
            >
            <Button
              v-for="label in isArray ? selection : [selection]"
              icon="cross"
              iconPosition="right"
              type="filterWell"
              size="tiny"
              @click="deselect(label as string)"
            >
              {{ label }}
            </Button>
          </template>
          <div>
            <label :for="`search-for-${id}`" class="sr-only">
              search in ontology
            </label>
            <input
              :id="`search-for-${id}`"
              type="text"
              v-model="searchTerms"
              @input="updateSearch(searchTerms)"
              class="flex-1 min-w-[100px] bg-transparent focus:outline-none"
              placeholder="Search in terms"
              autocomplete="off"
              @click.stop="showSelect = true"
            />
          </div>
        </div>
        <div>
          <BaseIcon
            v-show="showSelect"
            name="caret-up"
            @click.stop="showSelect = false"
          />
          <BaseIcon
            v-show="!showSelect"
            name="caret-down"
            class="justify-end"
          />
        </div>
      </div>
      <div
        ref="wrapperRef"
        :class="{
          'absolute z-20 max-h-[50vh] border rounded-input bg-white overflow-y-auto w-full pl-4':
            displayAsSelect,
        }"
        v-show="showSelect || !displayAsSelect"
      >
        <fieldset>
          <legend class="sr-only">select {{ columnName }} options</legend>
          <InputCheckboxGroup
            v-if="isArray"
            :id="id"
            :options="listOptions"
            :modelValue="(selection as string[])"
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
            :modelValue="(selection as string)"
            @select="select"
            @deselect="deselect"
            :invalid="invalid"
            :valid="valid"
            :disabled="disabled"
          />
        </fieldset>
        <div ref="sentinel" class="h-1"></div>
        <ButtonText
          v-if="
            initialCount <= limit &&
            (isArray ? selection.length > 0 : selection)
          "
          @click="clearSelection"
          >Clear</ButtonText
        >
      </div>
    </InputGroupContainer>
  </div>
  <div
    v-show="initialCount"
    class="py-4 flex justify-start items-center text-input-description"
  >
    <TextNoResultsMessage label="No options available" />
  </div>
</template>
