<script lang="ts" setup>
import {
  ref,
  computed,
  watch,
  onMounted,
  useTemplateRef,
  shallowRef,
} from "vue";
import { useIntersectionObserver } from "@vueuse/core";
import type { Ref } from "vue";

import {
  InputSearch,
  InputListbox,
  DisplayRecord,
  InputRefSelectContainer,
  InputRefSelectInputOption,
  InputRefSelectToggle,
  InputRefSelectToolbar,
} from "#components";

import { fetchGraphql } from "#imports";
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
const optionsToDisplay = computed(() => {
  if (showSelectionMap.value) {
    return selectionMap.value;
  } else {
    return optionMap.value;
  }
});

const initialCount = ref<number>(0);
const counter = ref<number>(0);
const counterOffset = ref<number>(0);
const maxTableRows = ref<number>(0);
const hasNoResults = ref<boolean>(true);

const displayText = ref<string>(props.placeholder);
const searchTerm = defineModel<string>("");
const sortMethod = ref<string>();
const isExpanded = ref<boolean>(false);
const collapseAllOptions = ref<boolean>(false);
const showSelectionMap = ref<boolean>(false);

const toggleElemRef = ref<InstanceType<typeof InputRefSelectToggle>>();
const optionElemsRef = useTemplateRef<HTMLDivElement>("refOptionsContainer");
const loadMoreTarget = useTemplateRef<HTMLDivElement>("inputOptionsTarget");
const targetIsVisible = shallowRef<boolean>(false);
const showListboxShadow = ref<boolean>(true);

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

  await getMaxTableRows();

  if (data.rows) {
    hasNoResults.value = false;
    data.rows.forEach(
      (row) => (optionMap.value[applyTemplate(props.refLabel, row)] = row)
    );
  }

  await loadOptions({ limit: props.limit });
  initialCount.value = counter.value;
}

async function loadOptions(filter: IQueryMetaData) {
  if (sortMethod.value) {
    filter.orderby = {};
    filter.orderby[sortMethod.value] = "ASC";
  }

  const data: ITableDataResponse = await fetchTableData(
    props.refSchemaId,
    props.refTableId,
    filter
  );

  await getMaxTableRows();

  if (data.rows) {
    hasNoResults.value = false;
    data.rows.forEach(
      (row) => (optionMap.value[applyTemplate(props.refLabel, row)] = row)
    );
    counter.value = Object.keys(optionMap.value).length;
  } else {
    hasNoResults.value = true;
    counter.value = 0;
  }
}

const namesForOrderByInput = computed<IInputValueLabel[]>(() => {
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

function applyTemplate(template: string, row: Record<string, any>): string {
  const ids = Object.keys(row);
  const vals = Object.values(row);
  const label = new Function(...ids, "return `" + template + "`;")(...vals);
  return label;
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
  showSelectionMap.value = false;
  selectionMap.value = {};
  emit("update:modelValue", props.multiselect ? [] : undefined);
}

function loadMore() {
  counterOffset.value += props.limit;

  if (counterOffset.value < maxTableRows.value) {
    loadOptions({
      offset: counterOffset.value,
      limit: props.limit,
      searchTerms: searchTerm.value,
    });
  }
}

const { stop } = useIntersectionObserver(
  loadMoreTarget,
  ([entry], observerElement) => {
    targetIsVisible.value = entry?.isIntersecting || false;
  }
);

function updateDisplayText() {
  const selectionMapKeys = Object.keys(selectionMap.value);
  if (selectionMapKeys.length) {
    displayText.value = selectionMapKeys.join(", ");
  } else {
    displayText.value = props.placeholder;
  }
}

async function getMaxTableRows() {
  const data = await fetchGraphql(
    props.refSchemaId,
    `query {${props.refTableId}_agg(search: "${
      searchTerm.value || ""
    }") { count }} `,
    {}
  );
  maxTableRows.value = data[`${props.refTableId}_agg`].count;
}

function applyModelValueToSelection() {
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

onMounted(async () => {
  await prepareModel();
  applyModelValueToSelection();
  updateDisplayText();
});

watch(
  () => props.refSchemaId,
  () => prepareModel
);
watch(
  () => props.refTableId,
  () => prepareModel
);

watch(
  () => isExpanded.value,
  () => {
    if (optionElemsRef.value) {
      optionElemsRef.value.scrollTop = 0;
    }

    if (!isExpanded.value) {
      updateSearch("");
    }
  }
);

watch(
  () => modelValue.value,
  () => applyModelValueToSelection
);

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

watch(
  () => selectionMap.value,
  () => {
    updateDisplayText();
  }
);

watch(
  () => targetIsVisible.value,
  () => {
    if (targetIsVisible.value) {
      if (counterOffset.value < maxTableRows.value) {
        loadMore();
      }

      if (counter.value === maxTableRows.value) {
        showListboxShadow.value = false;
      }
    } else {
      showListboxShadow.value = true;
    }
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
    <InputRefSelectToggle
      :id="id"
      :elemIdControlledByToggle="`${id}-ref-dropdown-content`"
      ref="toggleElemRef"
      @click="isExpanded = !isExpanded"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
    >
      <template #ref-dropdown-label>
        <span>
          {{ displayText }}
        </span>
      </template>
    </InputRefSelectToggle>
    <InputRefSelectContainer
      ref="refDropdownContainer"
      :id="`${id}-ref-dropdown-content`"
      :aria-expanded="isExpanded"
      :tabindex="isExpanded ? 1 : 0"
      class="border rounded relative"
      :class="{
        hidden: disabled || !isExpanded,
      }"
    >
      <InputRefSelectToolbar class="flex flex-col gap-2">
        <div class="w-full grid grid-cols-[2fr_1fr] gap-4">
          <div>
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
              :options="namesForOrderByInput"
              @update:model-value="(value: IInputValueLabel) => (sortMethod = value?.value as string)"
              :enable-search="false"
              placeholder="Sort by"
            />
          </div>
        </div>
        <div
          class="grid grid-cols-1 gap-2 md:grid-cols-[135px_165px] [&>button>span]:text-body-sm"
        >
          <ButtonText
            :id="`${id}-ref-dropdown-btn-clear`"
            icon="Trash"
            @click="clearSelection"
          >
            Clear selection
          </ButtonText>
          <ButtonText
            :id="`${id}-ref-dropdown-btn-show`"
            :icon="showSelectionMap ? 'filter-alt-off' : 'filter-alt'"
            @click="showSelectionMap = !showSelectionMap"
            :disabled="!Object.keys(selectionMap).length"
            :class="{
              'cursor-not-allowed': hasNoResults,
            }"
          >
            Show
            {{
              showSelectionMap
                ? "all"
                : multiselect
                ? "selected values"
                : "selected value"
            }}
          </ButtonText>
        </div>
      </InputRefSelectToolbar>
      <div
        ref="refOptionsContainer"
        class="overflow-y-scroll"
        :class="{
          'max-h-[275px]': isExpanded,
        }"
      >
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
              v-for="(option, label) in optionsToDisplay"
              class="border-b last:border-none"
            >
              <InputRefSelectInputOption
                :id="(label as string)"
                :group-id="id"
                :label="label"
                :option="(option as recordValue)"
                :multiselect="multiselect"
                :checked="Object.hasOwn(selectionMap, label)"
                @select="select"
                @deselect="deselect"
                :collapse-hidden-content="!isExpanded ? true : null"
              >
                <DisplayRecord
                  :table-metadata="tableMetadata"
                  :input-row-data="(option as recordValue)"
                  :showMgColumns="showMgColumns"
                />
              </InputRefSelectInputOption>
            </div>
            <div ref="inputOptionsTarget" />
          </template>
          <div
            v-if="hasNoResults"
            :id="`${id}-ref-dropdown-options-status`"
            class="flex justify-center items-center h-56"
            role="status"
            aria-atomic="true"
          >
            <TextNoResultsMessage v-if="hasNoResults" />
          </div>
        </fieldset>
      </div>
      <div
        v-if="showListboxShadow"
        class="absolute w-full bottom-0 shadow-[0_0_3px_2px_rgba(0,0,0,0.08)]"
      />
    </InputRefSelectContainer>
  </InputGroupContainer>
</template>
