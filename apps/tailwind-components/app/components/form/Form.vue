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

const props = defineProps<{
  metadata: ITableMetaData;
  constantValues?: Record<columnId, columnValue>;
  rowKey?: Record<string, columnValue>;
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
  insertInto,
  updateInto,
  errorMap,
  onUpdateColumn,
  onBlurColumn,
  onViewColumn,
  onLeaveViewColumn,
  validateAllColumns,
  sections,
  visibleColumns,
  visibleColumnIds,
} = useForm(props.metadata, formValues, "fields-container");

defineExpose({
  isValid,
  gotoPreviousRequiredField,
  gotoNextRequiredField,
  gotoNextError,
  gotoPreviousError,
  insertInto,
  updateInto,
  errorMessage,
  requiredMessage,
});

function onLeaveView(column: IColumn) {
  visibleColumnIds.value.delete(column.id);
}

function isValid() {
  validateAllColumns();
  return Object.keys(errorMap.value).length < 1;
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

    <div id="fields-container" class="col-span-3 px-4 py-50px overflow-y-auto">
      <PreviousSectionNav
        v-if="previousSection"
        @click="gotoSection(previousSection.id)"
      >
        {{ previousSection.label }}
      </PreviousSectionNav>
      <FormFields
        ref="formFields"
        :row-key="rowKey"
        :columns="visibleColumns"
        :constantValues="constantValues"
        :errorMap="errorMap"
        v-model="formValues"
        @update="onUpdateColumn"
        @blur="onBlurColumn"
        @view="onViewColumn"
        @leaving-view="onLeaveView"
      />
      <NextSectionNav v-if="nextSection" @click="gotoSection(nextSection.id)">
        {{ nextSection.label }}
      </NextSectionNav>
    </div>
  </div>
</template>
