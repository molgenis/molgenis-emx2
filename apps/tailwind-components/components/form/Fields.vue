<script setup lang="ts">
import type {
  columnId,
  columnValue,
  IColumn,
  ITableMetaData,
} from "../../../metadata-utils/src/types";
import {
  getColumnValidationError,
  isColumnVisible,
  isMissingValue,
} from "~/utils/formUtils";
import type { IFormLegendSection } from "metadata-utils/dist/src/types";

//todo: don't forget about reflinks

const props = defineProps<{
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

//initialize visiblity for headers
props.metadata.columns
  .filter((column) => column.columnType === "HEADING")
  .forEach((column) => {
    console.log(
      isColumnVisible(column, dataMap, props.schemaId, props.metadata)
    );
    visibleMap[column.id] =
      !column.visible ||
      isColumnVisible(column, dataMap, props.schemaId, props.metadata)
        ? true
        : false;
    console.log(
      "check heading " +
        column.id +
        "=" +
        visibleMap[column.id] +
        " expression " +
        column.visible
    );
  });

//todo, chapters should be rendered in this component so they can account for visibility

const activeChapterId: Ref<string | null> = ref(null);
const chapters = computed(() => {
  return props.metadata.columns.reduce((acc, column) => {
    if (column.columnType === "HEADING") {
      acc.push({
        title: column.label,
        id: column.id,
        columns: [],
        isActive: column.id === activeChapterId.value,
      });
    } else {
      if (acc.length === 0) {
        acc.push({
          title: "_NO_CHAPTERS",
          id: "_NO_CHAPTERS",
          columns: [],
        });
      }
      acc[acc.length - 1].columns.push(column);
    }
    return acc;
  }, [] as IFormLegendSection[]);
});

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

//todo specify exact validation behavior
//made som optimization to not validate all fields all the time in this phase
//when we focus we check validation of that column while keeping the validation of previous one
//when we blur we keep previous error
/** this is called on every touch of a column. Before submit of a form we need to validate everything but that can be done by the container of this */
function validateColumn(column: IColumn) {
  console.log("validate " + column.id);
  delete errorMap[column.id];

  //validate required
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
    errorMap[column.id] = props.metadata.columns
      .filter((c) => c.validation?.includes(column.id))
      .map((c) => {
        const result = getColumnValidationError(
          c.validation as string,
          dataMap,
          props.schemaId,
          props.metadata
        );
        return result;
      })
      .join("");
  }

  //todo: need also to updated visible and computed if related
}

function onUpdate(column: IColumn, $event: columnValue) {
  dataMap[column.id] = $event;
  if (errorMap[column.id]) {
    validateColumn(column);
  }
  props.metadata.columns
    .filter((c) => c.visible?.includes(column.id))
    .forEach((c) => {
      visibleMap[c.id] = isColumnVisible(
        c,
        dataMap,
        props.schemaId,
        props.metadata
      )
        ? true
        : false;
      console.log("updating visibility for " + c.id + "=" + visibleMap[c.id]);
    });
  emit("update:modelValue", dataMap);
}

function onFocus(column: IColumn) {
  console.log("focus " + column.id);
  //will validate previous column, because checkbox, radio don't have 'blur'
  if (previousColumn.value) {
    validateColumn(previousColumn.value);
  }
  previousColumn.value = column;
}

function checkVisibleExpression(column: IColumn) {
  //while not stable lets keep these logs, is there a log framework we can use to switch this of in prod?
  //when input becomes into view port
  if (
    !column.visible ||
    isColumnVisible(column, dataMap, props.schemaId, props.metadata)
  ) {
    visibleMap[column.id] = true;
  } else {
    visibleMap[column.id] = false;
  }
  console.log(
    "checking visibility of " + column.id + "=" + visibleMap[column.id]
  );
}

function updateActiveChapter(chapterId: string) {
  console.log("active header = " + chapterId);
  activeChapterId.value = chapterId;
}
</script>
<template>
  <div id="mock-form-container" class="basis-2/3 flex flex-row border">
    <div class="basis-1/3">
      <FormLegend
        :sections="
          chapters.filter((chapter) => chapter.title !== '_NO_CHAPTERS')
        "
      />
    </div>
    <div class="basis-2/3 h-screen overflow-y-scroll">
      <div
        v-for="chapter in chapters"
        :id="chapter.id"
        v-when-in-view="() => updateActiveChapter(chapter.id)"
      >
        <h2
          class="first:pt-0 pt-10 font-display md:text-heading-5xl text-heading-5xl text-form-header pb-8 scroll-mt-20"
          v-if="chapter.title !== '_NO_CHAPTERS' && visibleMap[chapter.id]"
        >
          {{ chapter.title }}
        </h2>
        <!-- todo filter invisible -->
        <template
          v-for="column in chapter.columns.filter(
            (c) => !c.id.startsWith('mg_')
          )"
        >
          <div
            style="height: 500px"
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
            :required="isRequired(column.required)"
            :error-message="errorMap[column.id]"
            :ref-schema-id="column.refSchemaId || schemaId"
            :ref-table-id="column.refTableId"
            :ref-label="column.refLabel || column.refLabelDefault"
            :state="errorMap[column.id] ? 'invalid' : 'default'"
            @update:modelValue="onUpdate(column, $event)"
            @blur="validateColumn(column)"
            @focus="onFocus(column)"
          />
        </template>
      </div>
      <div class="bg-red-500 p-3 font-bold">
        {{ numberOffFieldsWithErrors }} fields require your attention before you
        can save this {{ recordLabel }} ( temporary section for dev)
      </div>
      <div class="bg-gray-200 p-3">
        {{ numberOfRequiredFields - numberOfRequiredFieldsWithData }} /
        {{ numberOfRequiredFields }} required fields left ( temporary section
        for dev)
      </div>
    </div>
  </div>
</template>
