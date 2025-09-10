<script setup lang="ts">
import type { ITableDataResponse } from "../../composables/fetchTableData";
import type { IQueryMetaData } from "../../../molgenis-components/src/client/IQueryMetaData.ts";
import type {
  ITableMetaData,
  columnValueObject,
  recordValue,
} from "../../../metadata-utils/src/types";

import { type IInputProps, type IValueLabel } from "../../types/types";
import logger from "../../utils/logger";
import { fetchTableMetadata } from "#imports";
import { ref, computed, watch, onMounted, useTemplateRef, nextTick } from "vue";
import fetchTableData from "../../composables/fetchTableData";
import { fetchGraphql } from "#imports";

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
    limit: 25,
  }
);

const modelValue = defineModel<columnValueObject[] | columnValueObject>();
const tableMetadata = ref<ITableMetaData>();

const emit = defineEmits(["focus", "blur", "error", "update:modelValue"]);

const isLoading = ref<boolean>(true);
const loadingError = ref<string>("");

const optionMap = ref<recordValue>({});
const selectionMap = ref<recordValue>({});

const initialCount = ref<number>(0);
const count = ref<number>(0);
const offset = ref<number>(0);
const searchTerms = ref<string>("");
const hasNoResults = ref<boolean>(true);
const maxTableRows = ref<number>(0);

const refInputContainer = useTemplateRef("refInputContainer");
const refInputs = ref<HTMLInputElement[]>();

const columnName = computed<string>(() => {
  return props.refLabel.replace(/[\{\}\$]/g, "");
});

const entitiesLeftToLoad = computed<number>(() => {
  return Math.min(count.value - offset.value - props.limit, props.limit);
});

const listOptions = computed<IValueLabel[]>(() => {
  return Object.keys(optionMap.value).map((label) => {
    return { value: label } as IValueLabel;
  });
});

const selection = computed<string | string[]>(() => {
  const values = props.isArray
    ? (Object.keys(selectionMap.value) as string[])
    : (Object.keys(selectionMap.value)[0] as string);
  return values;
});

onMounted(() => {
  prepareModel()
    .then(() => {
      getMaxTableRows();
      initialCount.value = props.limit;
    })
    .catch((err) => {
      loadingError.value = err;
      throw new Error(err);
    })
    .finally(() => (isLoading.value = false));
});

watch(
  () => refInputContainer.value,
  () => {
    setRefInputs();
  }
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
}

// the selectionMap can not be a computed property because it needs to initialized asynchronously therefore use a watcher instead of a computed property
// todo: move the options fetch to the outside of the component and pass it as a (synchronous) prop
watch(
  () => modelValue.value,
  () => {
    if (!props.isArray) {
      delete selectionMap.value[Object.keys(selectionMap.value)[0]];
      if (modelValue.value) {
        selectionMap.value[applyTemplate(props.refLabel, modelValue.value)] =
          modelValue.value;
      }
    } else if (props.isArray) {
      selectionMap.value = {};
      if (
        modelValue.value &&
        Array.isArray(modelValue.value) &&
        modelValue.value.length
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

  if (data.rows) {
    hasNoResults.value = false;
    data.rows.forEach((row) => {
      const newRow = {} as recordValue;
      newRow[applyTemplate(props.refLabel, row)] = row;
      Object.assign(optionMap.value, newRow);
    });
    count.value = data.count;
  } else {
    hasNoResults.value = true;
  }
  logger.debug("loaded options for " + props.id);
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
  if (!props.disabled) {
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
}

function clearSelection() {
  selectionMap.value = {};
  emit("update:modelValue", props.isArray ? [] : undefined);
}

async function loadMore() {
  offset.value += props.limit;

  if (offset.value < maxTableRows.value) {
    refInputs.value = [];

    await loadOptions({
      offset: offset.value,
      limit: props.limit,
      searchTerms: searchTerms.value,
    });

    await nextTick();
    setRefInputs();

    if (refInputs.value) {
      const itemToFocus = refInputs.value[offset.value - 1];
      itemToFocus.scrollIntoView({
        behavior: "smooth",
        block: "nearest",
        inline: "center",
      });
      itemToFocus.focus();
    }
  }
}

function setRefInputs() {
  refInputs.value = refInputContainer.value?.querySelectorAll(
    "input"
  ) as unknown as HTMLInputElement[];
}

async function getMaxTableRows() {
  const data = await fetchGraphql(
    props.refSchemaId,
    `query {${props.refTableId}_agg(search: "${
      searchTerms.value || ""
    }") { count }} `,
    {}
  );
  maxTableRows.value = data[`${props.refTableId}_agg`].count;
}
</script>

<template>
  <InputBusyIndicator v-if="isLoading" height="xl" />
  <Message :id="`${id}-ref-error`" :invalid="true" v-else-if="loadingError">
    <span>{{ loadingError }}</span>
  </Message>
  <InputGroupContainer v-else @focus="emit('focus')" @blur="emit('blur')">
    <ButtonFilterWellContainer
      ref="selectionContainer"
      :id="`${id}-ref-selections`"
      @clear="clearSelection"
    >
      <template v-if="selection">
        <Button
          v-for="label in isArray ? selection : [selection]"
          icon="cross"
          icon-position="right"
          type="filterWell"
          size="tiny"
          @click="deselect(label as string)"
        >
          {{ label }}
        </Button>
      </template>
    </ButtonFilterWellContainer>
    <div class="my-4" v-if="maxTableRows > props.limit">
      <label :for="`search-for-${id}`" class="sr-only">
        search in {{ columnName }}
      </label>
      <InputSearch
        :id="`search-for-${id}`"
        size="small"
        :modelValue="searchTerms"
        @update:modelValue="updateSearch"
        :placeholder="`Search in ${columnName}`"
      />
    </div>
    <template v-if="!hasNoResults">
      <fieldset ref="refInputContainer">
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
          class="[&_label]:text-body-sm"
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
          class="[&_label]:text-body-sm"
        />
      </fieldset>
      <ButtonText @click="loadMore" v-if="offset + limit < count">
        load {{ entitiesLeftToLoad }} more
      </ButtonText>
    </template>
    <ButtonText v-else>No results found</ButtonText>
  </InputGroupContainer>
</template>
