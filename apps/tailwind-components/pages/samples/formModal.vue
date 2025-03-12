<script setup lang="ts">
import type { ITableMetaData } from "../../../metadata-utils/src";
import type { columnValue, columnId } from "../../../metadata-utils/src/types";
import cohortTableMetadata from "./data/cohort-table-metadata";
import Modal from "@/components/Modal.vue";

definePageMeta({
  layout: "full-page",
});

const modal = ref<InstanceType<typeof Modal>>();
function show() {
  modal.value?.show();
}

function hide() {
  modal.value?.close();
}

const formValues = ref<Record<string, columnValue>>({});
const metadata = cohortTableMetadata as ITableMetaData;
const errorMap = ref<Record<columnId, string>>({});

const activeChapterId = ref<string>("_scroll_to_top");
const sections = useSections(metadata, activeChapterId, errorMap);

function onSave() {
  alert("Do Save");
}

function onSaveDraft() {
  alert("Do draft save");
}

function scrollToElementInside(containerId: string, elementId: string) {
  const container = document.getElementById(containerId);
  const element = document.getElementById(elementId);
  if (container && element) {
    container.scrollTop = element.offsetTop - container.offsetTop;
    element.scrollIntoView();
  }
}

const {
  requiredMessage,
  errorMessage,
  gotoPreviousRequiredField,
  gotoNextRequiredField,
  gotoNextError,
  gotoPreviousError,
} = useForm(metadata, formValues, errorMap, (fieldId) => {
  scrollToElementInside("fields-container", `${fieldId}-form-field`);
});
</script>
<template>
  <Container>
    <div class="  "><Button class="m-10" @click="show"> Show</Button></div>

    <Modal ref="modal" max-width="max-w-9/10">
      <template #header>
        <header class="pt-[36px] px-8 overflow-y-auto border-b border-divider">
          <div class="mb-5 relative flex items-center">
            <h2
              class="uppercase text-heading-4xl font-display text-title-contrast"
            >
              Edit cohort: CONSTANCES
            </h2>

            <span
              class="ml-3 bg-gray-400 px-2 py-1 rounded text-white font-bold -mt-1"
              >Draft</span
            >
          </div>

          <button
            @click="hide"
            aria-label="Close modal"
            class="absolute top-7 right-8 p-1"
          >
            <BaseIcon class="text-gray-400" name="cross" />
          </button>
        </header>
      </template>

      <section class="grid grid-cols-4 gap-1">
        <div class="col-span-1">
          <FormLegend
            v-if="sections"
            class="sticky top-0"
            :sections="sections"
            @goToSection="scrollToElementInside('fields-container', $event)"
          />
        </div>

        <div
          id="fields-container"
          class="col-span-3 px-4 py-50px overflow-y-auto"
        >
          <FormFields
            schemaId="row-edit-sample"
            :metadata="metadata"
            :sections="sections"
            v-model:errors="errorMap"
            v-model="formValues"
            @update:active-chapter-id="activeChapterId = $event"
          />
        </div>
      </section>
      <Transition name="slide-up">
        <FormError
          v-show="errorMessage"
          :message="errorMessage"
          class="sticky mx-4 h-[62px] bottom-0 ransition-all transition-discrete"
          @error-prev="gotoPreviousError"
          @error-next="gotoNextError"
        />
      </Transition>

      <template #footer>
        <div class="flex justify-between items-center">
          <FormRequired
            :message="requiredMessage"
            @required-next="gotoNextRequiredField"
            @required-prev="gotoPreviousRequiredField"
          />
          <menu class="flex items-center justify-end h-[116px]">
            <div class="flex gap-4">
              <Button type="secondary" @click="hide">Cancel</Button>
              <Button type="outline" @click="onSaveDraft">Save draft</Button>
              <Button type="primary" @click="onSave">Save</Button>
            </div>
          </menu>
        </div>
      </template>
    </Modal>
  </Container>
</template>

<style scoped>
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.25s ease-out;
}

.slide-up-enter-from {
  opacity: 0;
  transform: translateY(62px);
}

.slide-up-leave-to {
  opacity: 0;
  transform: translateY(62px);
}
</style>
