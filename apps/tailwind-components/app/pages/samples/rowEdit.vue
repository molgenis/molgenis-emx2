<script setup lang="ts">
import { useHead } from "#app";
import { definePageMeta } from "#imports";
import { ref } from "vue";
import { useRoute } from "vue-router";
import type {
  columnValue,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";
import useForm from "../../composables/useForm";
import cohortTableMetadata from "./data/cohort-table-metadata";
import BreadCrumbs from "../../components/BreadCrumbs.vue";
import Button from "../../components/Button.vue";
import Container from "../../components/Container.vue";
import PageHeader from "../../components/PageHeader.vue";
import FormLegend from "../../components/form/Legend.vue";
import FormFields from "../../components/form/Fields.vue";
import FormError from "../../components/form/Error.vue";
import FormRequiredInfoSection from "../../components/form/RequiredInfoSection.vue";
import DraftLabel from "../../components/label/DraftLabel.vue";
import type { Crumb } from "../../../types/types";

definePageMeta({
  layout: "full-page",
});

useHead({
  htmlAttrs: {
    "data-theme": (useRoute().query.theme as string) || "light",
  },
});

const crumbs: Crumb[] = [
  { label: "Catalogue example", url: "/catalogue-example" },
  { label: "Cohorts", url: "/catalogue-example/cohorts" },
  { label: "Edit cohort: CONSTANCES", url: "" },
];
const formValues = ref<Record<string, columnValue>>({});
const metadata = cohortTableMetadata as ITableMetaData;
const PAGE_OFF_SET = 200;

function scrollTo(elementId: string) {
  const element = document.getElementById(elementId);
  if (element) {
    window.scroll(0, element.offsetTop - PAGE_OFF_SET);
  }
}

const form = useForm(metadata, formValues);

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
    <PageHeader title="Edit cohort: CONSTANCES" align="left" backPath="/">
      <template #prefix>
        <BreadCrumbs :align="'left'" :crumbs="crumbs" />
      </template>
      <template #title-suffix>
        <DraftLabel />
      </template>
    </PageHeader>
    <section class="grid grid-cols-4 gap-3">
      <div class="col-span-1 bg-form-legend">
        <FormLegend
          v-if="form.sections.value"
          class="pr-20 mr-5 sticky top-0"
          :sections="form.sections.value"
          @goToSection="scrollTo($event)"
        />
      </div>

      <div id="row-edit-field-container" class="col-span-3 border">
        <div class="bg-form h-[116px] sticky top-0 z-10">
          <menu
            class="flex items-center justify-between pt-[20px] pb-[20px] px-[30px]"
          >
            <FormRequiredInfoSection
              :message="form.requiredMessage.value"
              @required-prev="form.gotoPreviousRequiredField"
              @required-next="form.gotoNextRequiredField"
            />
            <div class="flex gap-4">
              <Button type="secondary" @click="onCancel">Cancel</Button>
              <Button type="outline" @click="onSaveDraft">Save draft</Button>
              <Button type="primary" @click="onSave">Save</Button>
            </div>
          </menu>
          <FormError
            v-show="form.errorMessage.value"
            :message="form.errorMessage.value"
            class="sticky h-[62px] bottom-0 transition-all transition-discrete"
            @error-prev="form.gotoPreviousError"
            @error-next="form.gotoNextError"
          />
        </div>

        <FormFields class="px-32 bg-form" :form="form" />
      </div>
    </section>
  </Container>
</template>
