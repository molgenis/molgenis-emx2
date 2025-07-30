<template>
  <slot :setVisible="setVisible">
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
            Edit {{ rowType }}
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
          :constant-values="constantValues"
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
        :hasPreviousError="hasPreviousError"
        :hasNextError="hasNextError"
        @error-prev="gotoPreviousError"
        @error-next="gotoNextError"
      />
    </Transition>
    <Transition name="slide-up">
      <FormError
        v-show="updateErrorMessage"
        :message="updateErrorMessage"
        class="sticky mx-4 h-[62px] bottom-0 ransition-all transition-discrete"
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
import { computed, ref, toRaw, watchEffect } from "vue";
import useForm from "../../composables/useForm";
import useSections from "../../composables/useSections";
import type { ITableMetaData } from "../../../metadata-utils/src";
import type { columnId, columnValue } from "../../../metadata-utils/src/types";
import { errorToMessage } from "../../utils/errorToMessage";

const props = defineProps<{
  schemaId: string;
  metadata: ITableMetaData;
  constantValues?: Record<columnId, columnValue>;
  formValues: Record<columnId, columnValue>;
}>();

const emit = defineEmits(["update:updated", "update:cancelled"]);

const editFormValues = ref<Record<columnId, columnValue>>(
  structuredClone(toRaw(props.formValues))
);

const visible = defineModel("visible", {
  type: Boolean,
  default: false,
});

const updateErrorMessage = ref<string>("");

function setVisible() {
  visible.value = true;
}

const rowType = computed(() => props.metadata.id);

const isDraft = computed(() => {
  return editFormValues.value.mg_draft === true;
});

function onCancel() {
  visible.value = false;
  emit("update:cancelled");
}

async function onUpdateDraft() {
  editFormValues.value.mg_draft = true;
  const resp = await updateInto(props.schemaId, props.metadata.id).catch(
    (err) => {
      console.error("Error saving data", err);
      updateErrorMessage.value = errorToMessage(err, "Error updating draft");
      return null;
    }
  );

  if (!resp) {
    return;
  }

  emit("update:updated", resp);
}

async function onUpdate() {
  editFormValues.value.mg_draft = false;
  const resp = await updateInto(props.schemaId, props.metadata.id).catch(
    (err) => {
      console.error("Error saving data", err);
      updateErrorMessage.value = errorToMessage(err, "Error updating record");
      return null;
    }
  );

  if (!resp) {
    return;
  }

  visible.value = false;
  emit("update:updated", resp);
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
  hasNextError,
  hasPreviousError,
  updateInto,
} = useForm(props.metadata, editFormValues, errorMap, (fieldId: string) => {
  scrollToElementInside("fields-container", fieldId);
});
</script>
