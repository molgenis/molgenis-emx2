<script setup lang="ts">
import type {
  columnId,
  columnValue,
  IColumn,
  IFormLegendSection,
  ITableMetaData,
} from "../../../metadata-utils/src/types";
import { isColumnVisible } from "../../../molgenis-components/src/components/forms/formUtils/formUtils";
import cohortTableMetadata from "./data/cohort-table-metadata";

definePageMeta({
  layout: "full-page",
});
const crumbs = computed(() => {
  let crumb: { [key: string]: string } = {};
  crumb["Catalogue example"] = `/catalogue-example`;
  crumb["Cohorts"] = `/catalogue-example/cohorts`;
  return crumb;
});
const current = computed(() => "Edit cohort: CONSTANCES");
const formValues = ref<Record<string, columnValue>>({});

const metadata = cohortTableMetadata as ITableMetaData;
const errorMap = reactive<Record<columnId, string>>({});
const visibleMap = reactive<Record<columnId, boolean>>({});

//initialize visibility for headers
metadata.columns
  .filter((column) => column.columnType === "HEADING")
  .forEach((column) => {
    if (metadata) {
      logger.debug(isColumnVisible(column, formValues.value, metadata));
    }
    visibleMap[column.id] =
      !column.visible ||
      (metadata && isColumnVisible(column, formValues.value, metadata))
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
  return metadata.columns.reduce((acc, column) => {
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
          errorCount: 0,
        });
      }
      acc[acc.length - 1].columns.push(column);
      if (errorMap[column.id]) acc[acc.length - 1].errorCount++;
    }
    return acc;
  }, [] as (IFormLegendSection & { columns: IColumn[] })[]);
});
</script>
<template>
  <Container>
    <PageHeader title="Edit cohort: CONSTANCES" align="left">
      <template #prefix>
        <BreadCrumbs :align="'left'" :crumbs="crumbs" :current="current" />
      </template>
      <template #title-prefix>
        <Button
          class="mr-4"
          type="filterWell"
          :iconOnly="true"
          icon="arrow-left"
          size="large"
          label="back"
        ></Button>
      </template>
      <template #title-suffix>
        <span class="ml-3 bg-gray-400 px-2 py-2 rounded text-white font-bold"
          >Draft</span
        >
      </template>
    </PageHeader>
    <section class="flex flex-row min-w-full">
      <FormLegend :sections="chapters" class="pr-20 mr-5" />

      <FormFields
        class="grow"
        id="row-edit-sample"
        schemaId="row-edit-sample"
        :metadata="metadata"
        :chapters="chapters"
        :visibleMap="visibleMap"
        :errorMap="errorMap"
        :activeChapterId="activeChapterId"
        v-model="formValues"
      />
    </section>
  </Container>
</template>
