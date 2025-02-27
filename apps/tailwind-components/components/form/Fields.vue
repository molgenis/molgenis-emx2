<script setup lang="ts">
import type {
  columnId,
  columnValue,
  IColumn,
  IFormLegendSection,
  ITableMetaData,
  recordValue,
} from "../../../metadata-utils/src/types";
import {
  isColumnVisible,
  getColumnError,
  isRequired,
} from "../../../molgenis-components/src/components/forms/formUtils/formUtils";
import logger from "@/utils/logger";
import consola from "consola";

const props = withDefaults(
  defineProps<{
    id: string;
    schemaId: string;
    metadata: ITableMetaData;
    chapters: (IFormLegendSection & { columns: IColumn[] })[];
    visibleMap: Record<columnId, boolean>;
    errorMap: Record<columnId, string>;
    activeChapterId?: string | null;
  }>(),
  {
    activeChapterId: null,
  }
);

const modelValue = defineModel<recordValue>("modelValue", {
  required: true,
});

const emit = defineEmits(["error", "update:activeChapterId"]);

function validateColumn(column: IColumn) {
  logger.debug("validate " + column.id);

  const error = getColumnError(column, modelValue.value, props.metadata);

  consola.info("error", error);

  if (error) {
    emit("error", error);
  } else {
    const errorValue = props.metadata.columns
      .filter((c) => c.validation?.includes(column.id))
      .map((c) => {
        const result = getColumnError(c, modelValue.value, props.metadata);
        return result;
      })
      .join("");
    emit("error", errorValue);
  }
}

const previousColumn = ref<IColumn>();

function onUpdate(column: IColumn, $event: columnValue) {
  if (props.errorMap[column.id]) {
    validateColumn(column);
  }
  props.metadata.columns
    .filter((c) => c.visible?.includes(column.id))
    .forEach((c) => {
      props.visibleMap[c.id] = isColumnVisible(
        c,
        modelValue.value,
        props.metadata
      )
        ? true
        : false;
      logger.debug(
        "updating visibility for " + c.id + "=" + props.visibleMap[c.id]
      );
    });
  previousColumn.value = column;
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
  emit("update:activeChapterId", chapterId);
}

function checkVisibleExpression(column: IColumn) {
  if (
    !column.visible ||
    isColumnVisible(column, modelValue.value, props.metadata!)
  ) {
    props.visibleMap[column.id] = true;
  } else {
    props.visibleMap[column.id] = false;
  }
  logger.debug(
    "checking visibility of " + column.id + "=" + props.visibleMap[column.id]
  );
}
</script>
<template>
  <div class="h-screen overflow-y-scroll" :id="id + '-fields-container'">
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
        v-for="column in chapter.columns.filter((c) => !c.id.startsWith('mg_'))"
      >
        <div
          style="height: 100px"
          v-on-first-view="() => checkVisibleExpression(column)"
          v-if="visibleMap[column.id] === undefined"
        ></div>
        <FormField
          class="pb-8"
          v-else-if="visibleMap[column.id] === true"
          v-model="modelValue[column.id]"
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
          @update:modelValue="onUpdate(column, $event ?? '')"
          @blur="onBlur(column)"
          @focus="onFocus(column)"
        />
      </template>
    </div>
  </div>
</template>
