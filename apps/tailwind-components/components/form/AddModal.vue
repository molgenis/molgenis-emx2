<template>
  <template v-if="showButton">
    <slot :setVisible="setVisible">
      <Button
        class="m-10"
        type="primary"
        size="small"
        icon="plus"
        @click="visible = true"
        >Add {{ rowType }}</Button
      >
    </slot>
  </template>
  <Modal v-model:visible="visible" max-width="max-w-9/10">
    <template #header>
      <header class="pt-[36px] px-8 overflow-y-auto border-b border-divider">
        <div class="mb-5 relative flex items-center">
          <h2
            class="uppercase text-heading-4xl font-display text-title-contrast"
          >
            Add {{ rowType }}
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
          v-if="visible && sections"
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
          v-if="visible"
          :schemaId="schemaId"
          :metadata="metadata"
          :sections="sections"
          :constantValues="constantValues"
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
    <Transition name="slide-up">
      <FormError
        v-show="saveErrorMessage"
        :message="saveErrorMessage"
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
            <Button type="outline" @click="onSaveDraft">Save draft</Button>
            <Button type="primary" @click="onSave">Save</Button>
          </div>
        </menu>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { ITableMetaData } from "../../../metadata-utils/src";
import type {
  columnId,
  columnValue,
  IRow,
} from "../../../metadata-utils/src/types";
import useSections from "../../composables/useSections";
import useForm from "../../composables/useForm";
import { errorToMessage } from "../../utils/errorToMessage";

const props = withDefaults(
  defineProps<{
    metadata: ITableMetaData;
    schemaId: string;
    constantValues?: IRow;
    showButton?: boolean;
  }>(),
  {
    showButton: true,
  }
);

const emit = defineEmits(["update:added", "update:cancelled"]);

const visible = defineModel("visible", {
  type: Boolean,
  default: false,
});

const saveErrorMessage = ref<string>("");

function setVisible() {
  visible.value = true;
}

const rowType = computed(() => props.metadata.id);
const isDraft = ref(false);

function onCancel() {
  visible.value = false;
  emit("update:cancelled");
}

async function onSaveDraft() {
  const resp = await insertInto(props.schemaId, props.metadata.id).catch(
    (err) => {
      console.error("Error saving data", err);
      saveErrorMessage.value = errorToMessage(err, "Error saving draft");
      return null;
    }
  );

  if (!resp) {
    return;
  }

  isDraft.value = true;
  emit("update:added", resp);
}

async function onSave() {
  const resp = await insertInto(props.schemaId, props.metadata.id).catch(
    (err) => {
      console.error("Error saving data", err);
      saveErrorMessage.value = errorToMessage(err, "Error saving data");

      return null;
    }
  );

  if (!resp) {
    return;
  }

  isDraft.value = false;
  visible.value = false;
  emit("update:added", resp);
}

const formValues = ref<Record<string, columnValue>>({});
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

function resetState() {
  formValues.value = {};
  errorMap.value = {};
  saveErrorMessage.value = "";
  isDraft.value = false;
}

watch(visible, (newValue, oldValue) => {
  if (newValue && !oldValue) {
    resetState();
  }
});

const {
  requiredMessage,
  errorMessage,
  gotoPreviousRequiredField,
  gotoNextRequiredField,
  gotoNextError,
  gotoPreviousError,
  insertInto,
} = useForm(props.metadata, formValues, errorMap, (fieldId) => {
  scrollToElementInside("fields-container", fieldId);
});
</script>
