<script setup lang="ts">
import type { C } from "vitest/dist/chunks/environment.d8YfPkTm.js";
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

const props = defineProps<{
  schemaId: string;
  sections: IFormLegendSection[];
  metadata: ITableMetaData;
}>();

const visibleMap = reactive<Record<columnId, boolean>>({});

const modelValue = defineModel<recordValue>("modelValue", {
  required: true,
});

const errors = defineModel<Record<columnId, string>>("errors", {
  required: true,
});

const emit = defineEmits(["error", "update:activeChapterId"]);

function validateColumn(column: IColumn) {
  logger.debug("validate " + column.id);

  const error = getColumnError(column, modelValue.value, props.metadata);

  consola.info("error", error);

  if (error) {
    errors.value[column.id] = error;
  } else {
    errors.value[column.id] = props.metadata.columns
      .filter((c) => c.validation?.includes(column.id))
      .map((c) => {
        const result = getColumnError(c, modelValue.value, props.metadata);
        return result;
      })
      .join("");
  }
}

const previousColumn = ref<IColumn>();

function onUpdate(column: IColumn, $event: columnValue) {
  if (errors.value[column.id]) {
    validateColumn(column);
  }
  props.metadata.columns
    .filter((c) => c.visible?.includes(column.id))
    .forEach((c) => {
      visibleMap[c.id] = isColumnVisible(c, modelValue.value, props.metadata)
        ? true
        : false;
      logger.debug("updating visibility for " + c.id + "=" + visibleMap[c.id]);
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

function updateActiveChapter(sectionId: string) {
  emit("update:activeChapterId", sectionId);
}

function checkVisibleExpression(column: IColumn) {
  if (
    !column.visible ||
    isColumnVisible(column, modelValue.value, props.metadata!)
  ) {
    visibleMap[column.id] = true;
  } else {
    visibleMap[column.id] = false;
  }
  logger.debug(
    "checking visibility of " + column.id + "=" + visibleMap[column.id]
  );
}

//initialize visibility for headers
props.metadata.columns
  .filter((column) => column.columnType === "HEADING")
  .forEach((column) => {
    if (props.metadata) {
      logger.debug(isColumnVisible(column, modelValue.value, props.metadata));
    }
    visibleMap[column.id] =
      !column.visible ||
      (props.metadata &&
        isColumnVisible(column, modelValue.value, props.metadata))
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
</script>
<template>
  <template v-for="column in metadata.columns">
    <div
      v-if="column.columnType === 'HEADING'"
      :id="column.id"
      v-when-overlaps-with-top-of-container="
        () => updateActiveChapter(column.id)
      "
    >
      <h2
        class="first:pt-0 pt-10 font-display md:text-heading-5xl text-heading-5xl text-form-header pb-8 scroll-mt-20"
        v-if="column.id !== '_scroll_to_top' && visibleMap[column.id]"
      >
        {{ column.label }}
      </h2>
    </div>

    <div
      style="height: 100px"
      v-on-first-view="() => checkVisibleExpression(column)"
      v-else-if="
        !column.id.startsWith('mg_') && visibleMap[column.id] === undefined
      "
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
      :error-message="errors[column.id]"
      :ref-schema-id="column.refSchemaId || schemaId"
      :ref-table-id="column.refTableId"
      :ref-label="column.refLabel || column.refLabelDefault"
      :invalid="errors[column.id]?.length > 0"
      @update:modelValue="onUpdate(column, $event ?? '')"
      @blur="onBlur(column)"
      @focus="onFocus(column)"
    />
  </template>
</template>
