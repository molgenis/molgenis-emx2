<script setup lang="ts">
import type {
  columnId,
  columnValue,
  ITableMetaData,
} from "../../../metadata-utils/src/types";
import cohortTableMetadata from "./data/cohort-table-metadata";

definePageMeta({
  layout: "full-page",
});

useHead({
  htmlAttrs: {
    "data-theme": (useRoute().query.theme as string) || "light",
  },
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

const PAGE_OFF_SET = 200;

function scrollTo(elementId: string) {
  const element = document.getElementById(elementId);
  if (element) {
    window.scroll(0, element.offsetTop - PAGE_OFF_SET);
  }
}

const {
  requiredMessage,
  errorMessage,
  gotoPreviousRequiredField,
  gotoNextRequiredField,
  gotoNextError,
  gotoPreviousError,
} = useForm(metadata, formValues, errorMap, scrollTo);

function onSave() {
  alert("Do Save");
}

function onSaveDraft() {
  alert("Do draft save");
}

function onCancel() {
  alert("Do cancel");
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
      <div class="col-span-1 bg-form-legend">
        <FormLegend
          v-if="sections"
          class="pr-20 mr-5 sticky top-0"
          :sections="sections"
          @goToSection="scrollTo($event)"
        />
      </div>

      <div id="row-edit-field-container" class="col-span-3 border">
        <div class="bg-form h-[116px] sticky top-0 z-10">
          <menu
            class="flex items-center justify-between pt-[20px] pb-[20px] px-[30px]"
          >
            <FormRequired
              :message="requiredMessage"
              @required-prev="gotoPreviousRequiredField"
              @required-next="gotoNextRequiredField"
            />
            <div class="flex gap-4">
              <Button type="secondary" @click="onCancel">Cancel</Button>
              <Button type="outline" @click="onSaveDraft">Save draft</Button>
              <Button type="primary" @click="onSave">Save</Button>
            </div>
          </menu>
          <FormError
            v-show="errorMessage"
            :message="errorMessage"
            class="sticky h-[62px] bottom-0 transition-all transition-discrete"
            @error-prev="gotoPreviousError"
            @error-next="gotoNextError"
          />
        </div>

        <FormFields
          class="px-32 bg-form"
          schemaId="catalogue-demo"
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
