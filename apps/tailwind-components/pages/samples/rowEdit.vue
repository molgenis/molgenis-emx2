<script setup lang="ts">
import { a } from "vitest/dist/chunks/suite.BJU7kdY9.js";
import type {
  columnId,
  columnValue,
  ITableMetaData,
} from "../../../metadata-utils/src/types";
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
const errorMap = ref<Record<columnId, string>>({});

const activeChapterId = ref<string>("_scroll_to_top");
const sections = useSections(metadata, activeChapterId, errorMap);

function scrollTo(elementId: string) {
  const element = document.getElementById(elementId);
  if (element) {
    element.scrollIntoView();
  }
}
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
    <section class="grid grid-cols-4 gap-3">
      <div class="col-span-1">
        <FormLegend
          v-if="sections"
          class="pr-20 mr-5 sticky top-0"
          :sections="sections"
          @goToSection="scrollTo($event)"
        />
      </div>

      <div id="row-edit-field-container" class="col-span-3 border p-10">
        <FormFields
          class="px-32 pt-16"
          schemaId="row-edit-sample"
          :metadata="metadata"
          :sections="sections"
          v-model:errors="errorMap"
          v-model="formValues"
          @update:active-chapter-id="activeChapterId = $event"
        />
      </div>
    </section>
  </Container>
</template>
