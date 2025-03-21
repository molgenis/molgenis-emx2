<template>
  <slot>
    <Button
      class="m-10"
      type="primary"
      size="small"
      icon="plus"
      @click="visible = true"
      >Update {{ rowType }}</Button
    >
  </slot>
  <Modal v-model:visible="visible" max-width="max-w-9/10">
    <template #header>
      <header class="pt-[36px] px-8 overflow-y-auto border-b border-divider">
        <div class="mb-5 relative flex items-center">
          <h2
            class="uppercase text-heading-4xl font-display text-title-contrast"
          >
            Update {{ rowType }}
          </h2>

          <span
            v-show="isDraft"
            class="ml-3 bg-gray-400 px-2 py-1 rounded text-white font-bold -mt-1"
            >Draft</span
          >
        </div>

        <button
          @click="visible = false"
          aria-label="Close modal"
          class="absolute top-7 right-8 p-1"
        >
          <BaseIcon class="text-gray-400" name="cross" />
        </button>
      </header>
    </template>

    <section class="grid grid-cols-4 gap-1">
      <div class="col-span-1 bg-form-legend">
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
          :schemaId="schemaId"
          :metadata="metadata"
          :sections="sections"
          v-model:errors="errorMap"
          v-model="editFormValues"
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
        <FormRequiredInfoSection
          :message="requiredMessage"
          @required-next="gotoNextRequiredField"
          @required-prev="gotoPreviousRequiredField"
        />
        <menu class="flex items-center justify-end h-[116px]">
          <div class="flex gap-4">
            <Button type="secondary" @click="onCancel">Cancel</Button>
            <Button type="outline" @click="onUpdateDraft">Save draft</Button>
            <Button type="primary" @click="onUpdate">Save</Button>
          </div>
        </menu>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import type { ITableMetaData } from "../../../metadata-utils/src";
import type { columnId, columnValue } from "../../../metadata-utils/src/types";

const props = withDefaults(
  defineProps<{
    schemaId: string;
    metadata: ITableMetaData;
    formValues: Record<columnId, columnValue>;
  }>(),
  {}
);

const editFormValues = ref<Record<columnId, columnValue>>(props.formValues);

watchEffect(() => {
  editFormValues.value = props.formValues;
});

const visible = ref(false);

const rowType = computed(() => props.metadata.id);
const isDraft = ref(false);

function onCancel() {
  visible.value = false;
}

function onUpdateDraft() {
  console.log("do update as Draft");
  isDraft.value = true;
}

function onUpdate() {
  console.log("do update row");
  isDraft.value = false;
  visible.value = false;
}

const errorMap = ref<Record<columnId, string>>({});

const activeChapterId = ref<string>("_scroll_to_top");
const sections = useSections(props.metadata, activeChapterId, errorMap);

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
} = useForm(props.metadata, editFormValues, errorMap, (fieldId) => {
  scrollToElementInside("fields-container", `${fieldId}-form-field`);
});
</script>
