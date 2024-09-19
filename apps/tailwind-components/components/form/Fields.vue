<script setup lang="ts">
import type { FormField } from "#build/components";
import type {
  columnId,
  columnValue,
  IColumn,
  ITableMetaData,
} from "../../../metadata-utils/src/types";

const props = defineProps<{
  metadata: ITableMetaData;
  data: Record<columnId, columnValue>[];
}>();

const status = reactive({
  pristine: true,
  touched: false,
});

interface IChapter {
  title: string | "_NO_CHAPTERS";
  columns: IColumn[];
}

const chapters = computed(() => {
  return props.metadata.columns.reduce((acc, column) => {
    if (column.columnType === "HEADING") {
      acc.push({
        title: column.id,
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

const dataMap = reactive(
  Object.fromEntries(props.metadata.columns.map((column) => [column.id, ""]))
);

const errorMap = reactive(
  Object.fromEntries(props.metadata.columns.map((column) => [column.id, []]))
);

const numberOffFieldsWithErrors = computed(() =>
  Object.values(errorMap).reduce((acc, errors) => acc + errors.length, 0)
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

const formFields = ref<InstanceType<typeof FormField>[]>([]);

function validate() {
  formFields.value.forEach((formField) => {
    formField.validate(dataMap[formField.id]);
  });
}

defineExpose({ validate });
</script>
<template>
  <div>
    <div>
      dataMap: {{ dataMap }}
      <hr class="my-2" />
      errorMap: {{ errorMap }}
    </div>
    <div class="first:pt-0 pt-10" v-for="chapter in chapters">
      <h2
        class="font-display md:text-heading-5xl text-heading-5xl text-title-contrast pb-8"
        v-if="chapter.title !== '_NO_CHAPTERS'"
      >
        {{ chapter.title }}
      </h2>
      <div class="pb-8" v-for="column in chapter.columns">
        <FormField
          :column="column"
          :data="dataMap[column.id]"
          :errors="errorMap[column.id]"
          @update:modelValue="dataMap[column.id] = $event"
          @error="errorMap[column.id] = $event"
          @blur="validate"
          ref="formFields"
        ></FormField>
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
