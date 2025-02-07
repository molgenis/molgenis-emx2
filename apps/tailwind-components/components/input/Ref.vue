<script setup lang="ts">
import type { ITableDataResponse } from "~/composables/fetchTableData";
import type { IQueryMetaData } from "../../../molgenis-components/src/client/IQueryMetaData.ts";
import type {
  ITableMetaData,
  columnValueObject,
} from "../../../metadata-utils/src/types";

import { type IInputProps, type IValueLabel } from "~/types/types";
import logger from "@/utils/logger";

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
    limit: 10,
  }
);

const modelValue = defineModel<columnValueObject[] | columnValueObject>();
const tableMetadata = ref<ITableMetaData>();

const emit = defineEmits(["focus", "blur", "error", "update:modelValue"]);
const optionMap: Ref<Record<string, columnValueObject>> = ref({});
const selectionMap: Ref<Record<string, columnValueObject>> = ref({});
const initialCount = ref<number>(0);
const count = ref<number>(0);
const offset = ref<number>(0);
const showSearch = ref<boolean>(false);
const searchTerms: Ref<string> = ref("");
const hasNoResults = ref<boolean>(true);
const columnName = computed<string>(() => {
  return props.refLabel.replace(/[\{\}\$]/g, "");
});

const entitiesLeftToLoad = computed<number>(() => {
  return Math.min(count.value - offset.value - props.limit, props.limit);
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

onMounted(async () => {
  tableMetadata.value = await fetchTableMetadata(
    props.refSchemaId,
    props.refTableId
  );
  //first we need to retrieve all selected items so are sure we have all for the template
  if (
    modelValue.value && Array.isArray(modelValue.value)
      ? modelValue.value.length > 0
      : modelValue.value
  ) {
    const data: ITableDataResponse = await fetchTableData(
      props.refSchemaId,
      props.refTableId,
      { filter: { equals: modelValue.value } }
    );
    if (data.rows) {
      hasNoResults.value = false;
      data.rows?.forEach(
        (row) => (selectionMap.value[applyTemplate(props.refLabel, row)] = row)
      );
    }
  }

  //then we load the options for the first time
  await loadOptions({ limit: props.limit });
  initialCount.value = count.value;
});

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
  if (!showSearch.value) updateSearch("");
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
    .filter((column) => column.key === 1)
    .forEach((column) => {
      result[column.id] = value[column.id];
    });
  return result;
}

function deselect(label: string) {
  delete selectionMap.value[label];
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
  offset.value += props.limit;
  loadOptions({
    offset: offset.value,
    limit: props.limit,
    searchTerms: searchTerms.value,
  });
}
</script>

<template>
  <div
    class="flex flex-wrap gap-2 mb-2"
    v-if="isArray ? selection.length : selection"
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
  </div>
  <div class="flex flex-wrap gap-2 mb-2">
    <ButtonText @click="toggleSearch" :aria-controls="`search-for-${id}`">
      Search
    </ButtonText>
    <ButtonText @click="clearSelection"> Clear all </ButtonText>
  </div>
  <template v-if="showSearch && initialCount > limit">
    <InputLabel :for="`search-for-${id}`" class="sr-only">
      search in {{ columnName }}
    </InputLabel>
    <InputSearch
      :id="`search-for-${id}`"
      :modelValue="searchTerms"
      @update:modelValue="updateSearch"
      class="mb-2"
      :placeholder="`Search in ${columnName}`"
      :aria-hidden="!showSearch"
    />
  </template>
  <template v-if="!hasNoResults">
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
      @focus="$emit('focus')"
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
      @focus="$emit('focus')"
    />
    <ButtonText @click="loadMore" v-if="offset + limit < count">
      load {{ entitiesLeftToLoad }} more
    </ButtonText>
  </template>
  <ButtonText v-else :inverted="inverted">No results found</ButtonText>
</template>
