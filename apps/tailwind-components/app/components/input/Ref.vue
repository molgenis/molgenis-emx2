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
import fetchTableMetadata from "../../composables/fetchTableMetadata";
import { ref, type Ref, computed, watch, onMounted, nextTick } from "vue";
import fetchTableData from "../../composables/fetchTableData";
import InputCheckboxGroup from "./CheckboxGroup.vue";
import InputRadioGroup from "./RadioGroup.vue";
import InputGroupContainer from "../input/InputGroupContainer.vue";
import Button from "../Button.vue";
import BaseIcon from "../BaseIcon.vue";
import TextNoResultsMessage from "../text/NoResultsMessage.vue";
import { useClickOutside } from "../../composables/useClickOutside";
import fetchRowPrimaryKey from "../../composables/fetchRowPrimaryKey";
import { useTemplateRef } from "vue";

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

const initLoading = ref(true);
const modelValue = defineModel<
  columnValueObject[] | columnValueObject | null
>();
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

async function init() {
  tableMetadata.value = await fetchTableMetadata(
    props.refSchemaId,
    props.refTableId
  );

  if (
    modelValue.value &&
    (Array.isArray(modelValue.value)
      ? modelValue.value.length > 0
      : modelValue.value)
  ) {
    const keys = Array.isArray(modelValue.value)
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
  () => init
);
watch(
  () => props.refTableId,
  () => init
);

// the selectionMap can not be a computed property because it needs to initialized asynchronously therefore use a watcher instead of a computed property
watch(
  () => modelValue.value,
  () => init
);

function applyTemplate(template: string, row: Record<string, any>): string {
  const ids = Object.keys(row);
  const vals = Object.values(row);
  const label = new Function(...ids, "return `" + template + "`;")(...vals);
  return label;
}

async function loadOptions(filter: IQueryMetaData) {
  hasNoResults.value = true;
  filter.expandLevel = 1;
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
  if (searchTerms.value) {
    updateSearch("");
  }
}

const wrapperRef = useTemplateRef("wrapperRef");
// Close dropdown when clicking outside
useClickOutside(wrapperRef, () => {
  showSelect.value = false;
});

const sentinel = useTemplateRef("sentinel");
let loadMoreObserver: IntersectionObserver | null = null;
onMounted(() => {
  loadMoreObserver = new IntersectionObserver(
    async (entries) => {
      const entry = entries[0];
      if (entry?.isIntersecting) {
        loadMore();
      }
    },
    {
      root: wrapperRef.value, // the container
      threshold: 0.25,
      rootMargin: "100px", //more smooth
    }
  );
});

function toggleSelect() {
  if (showSelect.value) {
    showSelect.value = false;
    loadMoreObserver?.disconnect();
  } else {
    showSelect.value = true;

    if (sentinel.value) {
      loadMoreObserver?.observe(sentinel.value!);
    }
  }
}

function updateSearch(newSearchTerms: string) {
  optionMap.value = {};
  offset.value = 0;
  searchTerms.value = newSearchTerms;
  loadOptions({ limit: props.limit, searchTerms: searchTerms.value });
}

async function select(label: string) {
  if (!props.isArray) {
    selectionMap.value = {};
  }
  const optionValue = optionMap.value[label];
  if (
    optionValue !== undefined &&
    optionValue !== null &&
    typeof optionValue === "object" &&
    !Array.isArray(optionValue)
  ) {
    selectionMap.value[label] = await extractPrimaryKey(optionValue);
  } else {
    throw new Error("Invalid option value for label: " + label);
  }

  if (searchTerms.value) {
    toggleSearch();
  }
  // close select dropdown for single select once an option is selected
  if (!props.isArray && showSelect.value === true) {
    toggleSelect();
  }
  emitValue();
}

function emitValue() {
  emit(
    "update:modelValue",
    props.isArray
      ? Object.values(selectionMap.value)
      : Object.values(selectionMap.value)[0]
  );
}

async function extractPrimaryKey(row: recordValue) {
  return await fetchRowPrimaryKey(row, props.refTableId, props.refSchemaId);
}

function deselect(label: string) {
  delete selectionMap.value[label];
  if (searchTerms.value) {
    toggleSearch();
  }
  if (!props.isArray && showSelect.value === true) {
    toggleSelect();
  }
  emitValue();
}

function clearSelection() {
  selectionMap.value = {};
  emit("update:modelValue", props.isArray ? [] : undefined);
  updateSearch(""); //reset
}

function loadMore() {
  offset.value += 25;
  loadOptions({
    offset: offset.value,
    limit: props.limit,
    searchTerms: searchTerms.value,
  });
}

const displayAsSelect = computed(() => initialCount.value > props.limit);

onMounted(() => {
  init();
});
</script>

<template>
  <div
    ref="lazyLoadTrigger"
    v-show="initLoading"
    class="h-20 flex justify-start items-center"
  >
    <BaseIcon name="progress-activity" class="animate-spin text-input" />
  </div>
  <div
    v-show="!initLoading && initialCount"
    :class="{
      'flex items-center border outline-none rounded-input cursor-pointer':
        displayAsSelect,
      'bg-input ': displayAsSelect && !disabled,
      'border-disabled': displayAsSelect && disabled,
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
    @click.stop="displayAsSelect && !showSelect ? toggleSelect : null"
  >
    <InputGroupContainer
      :id="`${id}-ref`"
      class="border-transparent w-full relative"
      @focus="emit('focus')"
      @blur="emit('blur')"
    >
      <div
        v-show="displayAsSelect"
        class="flex items-center justify-between gap-2 px-2 h-input"
        @click.stop.self="toggleSelect"
      >
        <div class="flex flex-wrap items-center gap-2">
          <template v-if="isArray ? selection.length : selection" role="group">
            <Button
              v-for="label in isArray ? selection : [selection]"
              icon="cross"
              iconPosition="right"
              type="filterWell"
              size="tiny"
              class="h-[36px]"
              :class="{
                'text-disabled cursor-not-allowed': disabled,
                'text-valid bg-valid': valid,
                'text-invalid bg-invalid': invalid,
              }"
              @click="deselect(label as string)"
            >
              {{ label }}
            </Button>
          </template>
          <div v-if="!disabled">
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
              @click.stop.self="toggleSelect"
            />
          </div>
        </div>
        <div class="flex items-center gap-2">
          <BaseIcon
            v-show="showSelect"
            name="caret-up"
            :class="{
              'text-valid': valid,
              'text-invalid': invalid,
              'text-disabled cursor-not-allowed': disabled,
              'text-input': !disabled,
            }"
            @click.stop.self="toggleSelect"
          />
          <BaseIcon
            v-show="!showSelect"
            name="caret-down"
            :class="{
              'text-valid': valid,
              'text-invalid': invalid,
              'text-disabled cursor-not-allowed': disabled,
              'text-input': !disabled,
            }"
            @click.stop.self="toggleSelect"
          />
        </div>
      </div>
      <div
        ref="wrapperRef"
        :class="{
          'max-h-[50vh] top-4 rounded-theme bg-input overflow-y-auto w-full pt-2 pb-6 pl-4 ':
            displayAsSelect,
        }"
        v-show="(showSelect && !disabled) || !displayAsSelect"
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
      </div>
    </InputGroupContainer>
  </div>
  <div
    v-show="initialCount === 0"
    class="py-4 flex justify-start items-center text-input-description"
  >
    <TextNoResultsMessage label="No options available" />
  </div>
  <Button
    v-if="isArray ? selection.length : selection && !displayAsSelect"
    @click="clearSelection"
    type="text"
    size="tiny"
    iconPosition="right"
    class="mr-2 underline cursor-pointer"
    :class="{ 'pl-2': displayAsSelect }"
  >
    Clear
  </Button>
</template>
