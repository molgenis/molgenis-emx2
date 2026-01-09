<script setup lang="ts">
import type {
  columnId,
  columnValue,
  IColumn,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";
import useForm from "../../composables/useForm";
import FormFields from "./Fields.vue";
import FormLegend from "./Legend.vue";
import NextSectionNav from "./NextSectionNav.vue";
import PreviousSectionNav from "./PreviousSectionNav.vue";
import fetchRowPrimaryKey from "../../composables/fetchRowPrimaryKey";
import { ref } from "vue";

const props = defineProps<{
  metadata: ITableMetaData;
  constantValues?: Record<columnId, columnValue>;
}>();

const formValues = defineModel("formValues", {
  type: Object as () => Record<columnId, columnValue>,
  required: true,
});

const {
  requiredMessage,
  errorMessage,
  gotoPreviousRequiredField,
  gotoNextRequiredField,
  gotoNextError,
  gotoPreviousError,
  gotoSection,
  previousSection,
  nextSection,
  insertInto: insert,
  updateInto,
  visibleColumnErrors,
  onUpdateColumn,
  onBlurColumn,
  onViewColumn,
  onLeaveViewColumn,
  validateAllColumns,
  validateKeyColumns,
  sections,
  visibleColumns,
  visibleColumnIds,
} = useForm(props.metadata, formValues, "fields-container");

defineExpose({
  isValid,
  isDraftValid,
  gotoPreviousRequiredField,
  gotoNextRequiredField,
  gotoNextError,
  gotoPreviousError,
  insertInto,
  updateInto,
  errorMessage,
  requiredMessage,
});

const rowKey = ref<Record<string, columnValue>>();
async function fetchRowKey() {
  rowKey.value = await fetchRowPrimaryKey(
    formValues.value,
    props.metadata.id,
    props.metadata.schemaId
  );
}
await fetchRowKey();

function onLeaveView(column: IColumn) {
  visibleColumnIds.value.delete(column.id);
}

function isValid() {
  validateAllColumns();
  return Object.keys(visibleColumnErrors.value).length < 1;
}

function isDraftValid() {
  validateKeyColumns();
  return Object.keys(visibleColumnErrors.value).length < 1;
}

// wrapper to update rowKey after insert because we keep the from open
function insertInto() {
  const insertPromise = insert();
  insertPromise.then(async () => fetchRowKey());
  return insertPromise;
}
</script>
<template>
  <div class="grid grid-cols-4 gap-1 min-h-0">
    <div class="col-span-1 bg-form-legend overflow-y-auto h-full min-h-0">
      <FormLegend
        v-if="sections"
        class="sticky top-0"
        :sections="sections"
        @goToSection="gotoSection"
      />
    </div>

    <div id="fields-container" class="col-span-3 p-12.5 overflow-y-auto">
      <FormFields
        ref="formFields"
        :rowKey="rowKey"
        :columns="visibleColumns"
        :constantValues="constantValues"
        :visibleColumnErrors="visibleColumnErrors"
        v-model="formValues"
        @update="onUpdateColumn"
        @blur="onBlurColumn"
        @view="onViewColumn"
        @leaving-view="onLeaveView"
      />
    </div>
  </div>
</template>
