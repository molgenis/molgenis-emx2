<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import type {
  columnId,
  columnValue,
  IColumn,
  IRow,
} from "../../../metadata-utils/src/types";
import {
  //todo: can we have required calculation done in useForm?
  isRequired,
} from "../../../molgenis-components/src/components/forms/formUtils/formUtils";
import logger from "../../utils/logger";
import { vIntersectionObserver } from "@vueuse/components";
import fetchRowPrimaryKey from "../../composables/fetchRowPrimaryKey";

const props = defineProps<{
  schemaId: string;
  tableId: string;
  columns: IColumn[];
  onUpdate: (column: IColumn, event: any) => void;
  onFocus: (column: IColumn) => void;
  onBlur: (column: IColumn) => void;
  constantValues?: IRow;
}>();

const emit = defineEmits(["error", "update:activeChapterId"]);

const modelValue = defineModel<IRow>("modelValue", {
  required: true,
});

const errors = defineModel<Record<columnId, string>>("errors", {
  required: true,
});

function updateActiveChapter(sectionId: string) {
  emit("update:activeChapterId", sectionId);
}

function onIntersectionObserver(entries: IntersectionObserverEntry[]) {
  const highest = entries.find((entry) => entry.isIntersecting);
  if (highest) {
    //based on currently visible column if that is the highest
    if (highest.target.id.includes("form-field")) {
      //find the section it is part of
      let currentSection = "_scroll_to_top";
      for (const col of props.columns) {
        if (col.columnType === "HEADING" || col.columnType === "SECTION") {
          currentSection = col.id;
        }
        if (highest.target.id === `${col.id}-form-field`) {
          break;
        }
      }
      updateActiveChapter(currentSection);
    } else {
      updateActiveChapter(highest.target.id);
    }
  }
}

const rowKey = ref<columnValue>();

async function updateRowKey() {
  rowKey.value = await fetchRowPrimaryKey(
    modelValue.value,
    props.tableId,
    props.schemaId
  );
}

function copyConstantValuesToModelValue() {
  if (props.constantValues) {
    modelValue.value = Object.assign({}, props.constantValues);
  }
}

watch(
  () => modelValue.value,
  async () => {
    if (modelValue.value) {
      await updateRowKey();
    }
  }
);

watch(
  () => props.constantValues,
  () => {
    copyConstantValuesToModelValue();
  }
);

onMounted(() => {
  updateRowKey();
  copyConstantValuesToModelValue();
});
</script>

<template>
  <div>
    <template v-for="(column, index) in columns">
      <div
        id="_scroll_to_top"
        v-if="
          index === 0 &&
          column.columnType !== 'HEADING' &&
          column.columnType !== 'SECTION'
        "
        class="-mt-10"
      >
        <span v-intersection-observer="onIntersectionObserver"></span>
      </div>
      <div
        v-if="
          column.columnType === 'HEADING' || column.columnType === 'SECTION'
        "
        :id="column.id"
        v-intersection-observer="onIntersectionObserver"
      >
        <h2
          class="first:pt-0 pt-10 font-display md:text-heading-5xl text-heading-5xl text-form-header pb-8"
          v-if="column.id !== '_scroll_to_top'"
        >
          {{ column.label }}
        </h2>
      </div>
      <FormField
        class="pb-8"
        v-else-if="!Object.keys(constantValues || {}).includes(column.id)"
        v-intersection-observer="onIntersectionObserver"
        v-model="modelValue[column.id]"
        :id="`${column.id}-form-field`"
        :type="column.columnType"
        :label="column.label"
        :description="column.description"
        :rowKey="rowKey"
        :required="isRequired(column.required ?? false)"
        :error-message="errors[column.id]"
        :ref-schema-id="column.refSchemaId || schemaId"
        :ref-table-id="column.refTableId"
        :ref-label="column.refLabel || column.refLabelDefault"
        :ref-back-id="column.refBackId"
        :invalid="errors[column.id]?.length > 0"
        @update:modelValue="onUpdate(column, $event ?? '')"
        @blur="onBlur(column)"
        @focus="onFocus(column)"
      />
    </template>
  </div>
</template>
