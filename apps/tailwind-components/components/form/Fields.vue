<script setup lang="ts">
import type {
  columnId,
  columnValue,
  IColumn,
  ITableMetaData,
} from "../../../metadata-utils/src/types";
import {
  executeExpression,
  getColumnValidationError,
  isMissingValue,
} from "~/utils/formUtils";

//todo: don't forget about reflinks

const props = defineProps<{
  schemaId: string;
  metadata: ITableMetaData;
  data: Record<columnId, columnValue>[];
}>();

const emit = defineEmits(["error", "update:modelValue"]);

interface IChapter {
  title: string | "_NO_CHAPTERS";
  columns: IColumn[];
}

const chapters = computed(() => {
  return props.metadata.columns.reduce((acc, column) => {
    if (column.columnType === "HEADING") {
      acc.push({
        title: column.label,
        id: column.id,
        columns: [],
      });
    } else {
      if (acc.length === 0) {
        acc.push({
          title: "_NO_CHAPTERS",
          columns: [],
        });
      }
      acc[acc.length - 1].columns.push(column);
    }
    return acc;
  }, [] as IChapter[]);
});

const dataMap = reactive<Record<columnId, columnValue>>(
  Object.fromEntries(
    props.metadata.columns
      .filter((column) => column.columnType !== "HEADING")
      .map((column) => [column.id, ""])
  )
);

const errorMap = reactive<Record<columnId, string>>({});
const visibleMap = reactive<Record<columnId, boolean>>({});

const numberOffFieldsWithErrors = computed(
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

const previousFocus = ref<IColumn>();

//todo specify exact validation behavior
//made som optimization to not validate all fields all the time in this phase
//when we focus we check validation of that column while keeping the validation of previous one
//when we blur we keep previous error
/** this is called on every touch of a column. Before submit of a form we need to validate everything but that can be done by the container of this */
function validateColumn(column: IColumn, onBlur: boolean) {
  console.log(onBlur ? "blur " + column.id : "focus " + column.id);

  //on blur we check if required was filled in
  if (isRequired(column.required) && isMissingValue(dataMap[column.id])) {
    errorMap[column.id] = column.label + " is required";
  }

  //validate type specific regexp
  else if (column.columnType == "HYPERLINK") {
    errorMap[column.id] =
      column.label + " is not a valid " + column.columnType.toLowerCase();
    return;
  }

  //validate all expressions that include current column id
  //todo only for visible columns!
  //todo make regexp, might lead to false hits if column id is subset of another column id
  else {
    if (isMissingValue(dataMap[column.id])) {
      delete errorMap[column.id];
    } else {
      errorMap[column.id] = props.metadata.columns
        .filter((c) => c.validation?.includes(column.id))
        .map((c) => {
          const result = getColumnValidationError(
            c.validation as string,
            dataMap,
            props.schemaId,
            props.metadata
          );
          console.log("hallo " + result);
          return result;
        })
        .join("");
    }
  }

  //todo: need also to updated visible and computed if related
}

function onUpdate(column: IColumn, $event: columnValue) {
  dataMap[column.id] = $event;
  validateColumn(column, false);
  emit("update:modelValue", dataMap);
}
</script>
<template>
  <div>
    <div class="first:pt-0 pt-10" v-for="chapter in chapters">
      <h2
        class="font-display md:text-heading-5xl text-heading-5xl text-form-header pb-8 scroll-mt-20"
        :id="`${chapter.id}-chapter-title`"
        v-if="chapter.title !== '_NO_CHAPTERS'"
      >
        {{ chapter.title }}
      </h2>
      <!-- todo filter invisible -->
      <div
        class="pb-8"
        v-for="column in chapter.columns.filter((c) => !c.id.startsWith('mg_'))"
      >
        <Input
          v-model="dataMap[column.id]"
          :id="`${column.id}-form-field`"
          :schemaId="schemaId"
          :type="column.columnType"
          :label="column.label"
          :description="column.description"
          :required="column.required === true || column.required == 'true'"
          :error-message="errorMap[column.id]"
          :ref-schema-id="column.refSchemaId || schemaId"
          :ref-table-id="column.refTableId"
          :ref-label="column.refLabel || column.refLabelDefault"
          :state="errorMap[column.id] ? 'invalid' : null"
          @update:modelValue="onUpdate(column, $event)"
          @blur="validateColumn(column, true)"
          @focus="validateColumn(column, false)"
        />
      </div>
    </div>
    <div class="bg-red-500 p-3 font-bold">
      {{ numberOffFieldsWithErrors }} fields require your attention before you
      can save this {{ recordLabel }} ( temporary section for dev)
    </div>
    <div class="bg-gray-200 p-3">
      {{ numberOfRequiredFieldsWithData }} /
      {{ numberOfRequiredFields }} required fields left ( temporary section for
      dev)
    </div>
  </div>
</template>
