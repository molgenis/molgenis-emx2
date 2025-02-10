<script setup lang="ts">
import type {
  columnId,
  columnValue,
  IColumn,
  ITableMetaData,
} from "../../../metadata-utils/src/types";
import {
  isColumnVisible,
  getColumnError,
  isRequired,
} from "../../../molgenis-components/src/components/forms/formUtils/formUtils";
import type { IFormLegendSection } from "../../../metadata-utils/src/types";
import { scrollToElementInside } from "~/utils/scrollTools";
import logger from "@/utils/logger";

//todo: don't forget about default values for reflinks

const props = defineProps<{
  id: string;
  schemaId: string;
  metadata: ITableMetaData;
  data: Record<columnId, columnValue>[];
}>();

const emit = defineEmits(["error", "update:modelValue"]);

const dataMap = reactive<Record<columnId, columnValue>>(
  Object.fromEntries(
    props.metadata.columns
      .filter((column) => column.columnType !== "HEADING")
      .map((column) => [column.id, ""])
  )
);
const visibleMap = reactive<Record<columnId, boolean>>({});
const errorMap = reactive<Record<columnId, string>>({});
const previousColumn = ref<IColumn>();

//initialize visibility for headers
props.metadata.columns
  .filter((column) => column.columnType === "HEADING")
  .forEach((column) => {
    logger.debug(isColumnVisible(column, dataMap, props.metadata));
    visibleMap[column.id] =
      !column.visible || isColumnVisible(column, dataMap, props.metadata)
        ? true
        : false;
    logger.debug(
      "check heading " +
        column.id +
        "=" +
        visibleMap[column.id] +
        " expression " +
        column.visible
    );
  });

const activeChapterId: Ref<string | null> = ref(null);
const chapters = computed(() => {
  return props.metadata.columns.reduce((acc, column) => {
    if (column.columnType === "HEADING") {
      acc.push({
        label: column.label,
        id: column.id,
        columns: [],
        isActive: column.id === activeChapterId.value,
        errorCount: 0,
      });
    } else {
      if (acc.length === 0) {
        acc.push({
          label: "_top",
          id: "_scroll_to_top",
          columns: [],
          isActive: "_scroll_to_top" === activeChapterId.value,
        });
      }
      acc[acc.length - 1].columns.push(column);
      if (errorMap[column.id]) {
        const lastChapter = acc[acc.length - 1];
        if (lastChapter && lastChapter.errorCount) {
          lastChapter.errorCount++;
        }
      }
    }
    return acc;
  }, [] as (IFormLegendSection & { columns: IColumn[] })[]);
});

const numberOfFieldsWithErrors = computed(
  () => Object.values(errorMap).filter((value) => value).length
);

const numberOfRequiredFields = computed(
  () => props.metadata.columns.filter((column) => column.required).length
);

const numberOfRequiredFieldsWithData = computed(
  () =>
    props.metadata.columns.filter(
      (column) => column.required && dataMap[column.id]
    ).length
);

const recordLabel = computed(() => props.metadata.label);

function validateColumn(column: IColumn) {
  logger.debug("validate " + column.id);
  delete errorMap[column.id];
  const error = getColumnError(column, dataMap, props.metadata);
  if (error) errorMap[column.id] = error;
  else {
    errorMap[column.id] = props.metadata.columns
      .filter((c) => c.validation?.includes(column.id))
      .map((c) => {
        const result = getColumnError(
          c.validation as string,
          dataMap,
          props.metadata
        );
        return result;
      })
      .join("");
  }
}

function checkVisibleExpression(column: IColumn) {
  if (!column.visible || isColumnVisible(column, dataMap, props.metadata)) {
    visibleMap[column.id] = true;
  } else {
    visibleMap[column.id] = false;
  }
  logger.debug(
    "checking visibility of " + column.id + "=" + visibleMap[column.id]
  );
}

function onUpdate(column: IColumn, $event: columnValue) {
  dataMap[column.id] = $event;
  if (errorMap[column.id]) {
    validateColumn(column);
  }
  props.metadata.columns
    .filter((c) => c.visible?.includes(column.id))
    .forEach((c) => {
      visibleMap[c.id] = isColumnVisible(c, dataMap, props.metadata)
        ? true
        : false;
      logger.debug("updating visibility for " + c.id + "=" + visibleMap[c.id]);
    });
  previousColumn.value = column;
  emit("update:modelValue", dataMap);
}

function onFocus(column: IColumn) {
  logger.debug("focus " + column.id + " previous " + previousColumn.value);
  //will validate previous column, because checkbox, radio don't have 'blur'
  if (previousColumn.value) {
    validateColumn(previousColumn.value);
  }
  previousColumn.value = column;
}

function onBlur(column: IColumn) {
  previousColumn.value = column;
  validateColumn(column);
}

function updateActiveChapter(chapterId: string) {
  activeChapterId.value = chapterId;
}

function goToSection(headerId: string) {
  //requires all elements before id to have check visibility so we know their sizes
  //todo: loading animation this might take while
  //todo: next to visibility we also need to wait until all have retrieved options or ensure refs have fixed size
  for (let i = 0; i < props.metadata.columns.length; i++) {
    const column = props.metadata.columns[i];
    if (visibleMap[column.id] === undefined) checkVisibleExpression(column);
    if (column.id === props.id) break;
  }
  scrollToElementInside(props.id + "-fields-container", headerId);
}
</script>
<template>
  <div class="flex flex-row border">
    <div v-if="chapters.length > 1" class="basis-1/3">
      <FormLegend :sections="chapters" @go-to-section="goToSection" />
    </div>
    <div
      class="h-screen overflow-y-scroll"
      :class="{ 'basis-2/3': chapters.length > 1 }"
      :id="id + '-fields-container'"
    >
      <div
        v-for="chapter in chapters"
        :id="chapter.id"
        v-when-overlaps-with-top-of-container="
          () => updateActiveChapter(chapter.id)
        "
      >
        <h2
          class="first:pt-0 pt-10 font-display md:text-heading-5xl text-heading-5xl text-form-header pb-8 scroll-mt-20"
          v-if="chapter.label !== '_scroll_to_top' && visibleMap[chapter.id]"
        >
          {{ chapter.label }}
        </h2>
        <!-- todo filter invisible -->
        <template
          v-for="column in chapter.columns.filter(
            (c) => !c.id.startsWith('mg_')
          )"
        >
          <div
            style="height: 100px"
            v-on-first-view="() => checkVisibleExpression(column)"
            v-if="visibleMap[column.id] === undefined"
          ></div>
          <FormField
            class="pb-8"
            v-else-if="visibleMap[column.id] === true"
            v-model="dataMap[column.id]"
            :id="`${column.id}-form-field`"
            :type="column.columnType"
            :label="column.label"
            :description="column.description"
            :required="isRequired(column.required ?? false)"
            :error-message="errorMap[column.id]"
            :ref-schema-id="column.refSchemaId || schemaId"
            :ref-table-id="column.refTableId"
            :ref-label="column.refLabel || column.refLabelDefault"
            :invalid="errorMap[column.id]?.length > 0"
            @update:modelValue="onUpdate(column, $event)"
            @blur="onBlur(column)"
            @focus="onFocus(column)"
          />
        </template>
      </div>
      <div class="bg-red-500 p-3 font-bold">
        {{ numberOfFieldsWithErrors }} fields require your attention before you
        can save this {{ recordLabel }} ( temporary section for dev)
      </div>
      <div class="bg-gray-200 p-3">
        {{ numberOfRequiredFields - numberOfRequiredFieldsWithData }} /
        {{ numberOfRequiredFields }} required fields left ( temporary section
        for dev)
      </div>
      <div
        id="spacer-so-we-can-scroll-each-chapter-to-top-if-requested"
        class="h-screen"
      />
    </div>
  </div>
</template>
