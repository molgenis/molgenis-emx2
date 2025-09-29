<script lang="ts" setup>
import { ref, useId, watch } from "vue";
import { useForm } from "#imports";
import type { IRow, ColumnType } from "../../../metadata-utils/src/types";

const id = useId();

const model = ref<IRow>({});
const emits = defineEmits(["update:modelValue"]);

const props = defineProps<{
  modelValue?: IRow;
}>();

const metadata = ref({
  id: "page-dependencies",
  label: "External Dependencies",
  tableType: "FORM",
  columns: [
    {
      columnType: "HYPERLINK" as ColumnType,
      id: "url",
      label: "",
    },
    {
      columnType: "BOOL" as ColumnType,
      id: "defer",
      label: "Defer?",
      trueLabel: "Yes",
      falseLabel: "No",
      showClearButton: false,
      align: "horizontal",
    },
  ],
});

const { errorMap, onUpdateColumn, onBlurColumn } = useForm(metadata, model);

watch(
  () => model.value,
  () => {
    emits("update:modelValue", model.value);
  }
);
</script>

<template>
  <div class="flex justify-between items-start gap-5">
    <FormFields
      :id="`page-editor-js-${id}-dependencies`"
      class="w-full flex justify-start items-start gap-5 [&>div]:w-full [&_div:nth-child(2)]:w-auto [&_div:nth-child(2)_svg]:mt-0"
      v-model="model"
      :columns="metadata.columns"
      :error-map="errorMap"
      @update="onUpdateColumn"
      @blur="onBlurColumn"
    />
    <div>
      <Button
        type="inline"
        class="hover:bg-button-secondary-hover focus:bg-button-secondary-hover"
        :icon-only="true"
        icon="Trash"
        label="Delete"
      />
    </div>
  </div>
</template>
