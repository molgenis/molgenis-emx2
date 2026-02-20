<template>
  <template v-if="showButton">
    <slot>
      <Button
        class="m-10"
        type="primary"
        size="small"
        icon="plus"
        @click="visible = true"
      >
        {{ isInsert ? "Add" : "Edit" }} {{ rowType }}
      </Button>
    </slot>
  </template>

  <Modal v-model:visible="visible" max-width="max-w-9/10" @closed="onCancel">
    <template #header>
      <header
        class="pt-[36px] px-8 overflow-y-auto border-b border-divider flex-none"
      >
        <div class="mb-5 relative flex items-center">
          <h2
            class="uppercase text-heading-4xl font-display text-title-contrast"
          >
            {{ isInsert ? "Add" : "Edit" }} {{ rowType }}
          </h2>

          <DraftLabel v-if="isDraft" />
        </div>

        <button
          @click="onCancel"
          aria-label="Close modal"
          class="absolute top-7 right-8 p-1"
        >
          <BaseIcon class="text-gray-400" name="cross" />
        </button>
      </header>
    </template>

    <div
      v-if="visible"
      class="min-h-0 flex-1"
      :class="{
        'grid grid-cols-4 gap-1': form?.showLegend.value,
        'overflow-y-auto': !form?.showLegend.value,
      }"
    >
      <div
        v-if="form?.showLegend.value"
        class="col-span-1 bg-form-legend overflow-y-auto min-h-0"
      >
        <FormLegend
          class="sticky top-0"
          :sections="form.sections.value"
          @goToSection="gotoSection"
        />
      </div>

      <Form
        class="col-span-3"
        v-if="form !== undefined"
        :form="form"
        :constantValues="constantValues"
      />
    </div>

    <TransitionSlideUp>
      <FormError
        v-show="errorMessage"
        :message="errorMessage"
        class="sticky mx-4 h-[62px] bottom-0 transition-all transition-discrete"
        @error-prev="form?.gotoPreviousError"
        @error-next="form?.gotoNextError"
      />
    </TransitionSlideUp>
    <TransitionSlideUp>
      <FormError
        v-show="saveErrorMessage"
        :message="saveErrorMessage"
        :show-prev-next-buttons="!showReAuthenticateButton"
        class="sticky mx-4 h-[62px] bottom-0 transition-all transition-discrete"
      >
        <Button
          v-if="showReAuthenticateButton"
          type="outline"
          size="small"
          @click="reAuthenticate"
          >Re-authenticate</Button
        >
      </FormError>
    </TransitionSlideUp>
    <TransitionSlideUp :auto-hide="true" v-model:visible="showFormMessage">
      <FormMessage
        v-show="formMessage"
        :message="formMessage"
        class="sticky mx-4 h-[62px] bottom-0 transition-all transition-discrete"
      />
    </TransitionSlideUp>

    <template #footer>
      <div class="flex justify-between items-center flex-none">
        <FormRequiredInfoSection
          :message="requiredMessage"
          @required-next="form?.gotoNextRequiredField"
          @required-prev="form?.gotoPreviousRequiredField"
        />
        <menu class="flex items-center justify-end h-[116px]">
          <div class="flex gap-4">
            <Button type="secondary" :disabled="saving" @click="onCancel">
              Cancel
            </Button>
            <Button type="outline" :disabled="saving" @click="onSave(true)">
              Save as draft
              <BaseIcon
                v-if="savingDraft"
                class="inline animate-spin"
                name="ProgressActivity"
              />
            </Button>
            <Button type="primary" :disabled="saving" @click="onSave(false)">
              Save
              <BaseIcon
                v-if="saving"
                class="inline animate-spin"
                name="ProgressActivity"
              />
            </Button>
          </div>
        </menu>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { computed, ref, toRaw, watch, nextTick, watchEffect } from "vue";
import type { ITableMetaData } from "../../../../metadata-utils/src";
import type {
  columnId,
  columnValue,
  IRow,
} from "../../../../metadata-utils/src/types";
import { useSession } from "../../composables/useSession";
import { errorToMessage } from "../../utils/errorToMessage";
import { SessionExpiredError } from "../../utils/sessionExpiredError";
import { getInitialFormValues } from "../../utils/typeUtils";
import BaseIcon from "../BaseIcon.vue";
import Button from "../Button.vue";
import DraftLabel from "../label/DraftLabel.vue";
import Modal from "../Modal.vue";
import Form from "./Form.vue";
import FormRequiredInfoSection from "./RequiredInfoSection.vue";
import FormError from "./Error.vue";
import FormMessage from "./Message.vue";
import TransitionSlideUp from "../transition/SlideUp.vue";
import FormLegend from "./Legend.vue";
import useForm, { type UseForm } from "../../composables/useForm";

const props = withDefaults(
  defineProps<{
    schemaId: string;
    metadata: ITableMetaData;
    isInsert: boolean;
    constantValues?: IRow;
    showButton?: boolean;
    formValues?: Record<columnId, columnValue>;
  }>(),
  {
    showButton: true,
  }
);

const saving = ref(false);
const isInsert = ref(props.isInsert);
const formValues = ref<Record<string, columnValue>>(initFormValues());

const emit = defineEmits([
  "update:added",
  "update:updated",
  "update:cancelled",
]);

const visible = defineModel<boolean>("visible");

// lazy init formContext (form) when modal is opened
let form: UseForm | undefined;
watchEffect(() => {
  if (visible.value) {
    form = useForm(props.metadata, formValues);
  } else {
    form = undefined;
  }
});

const savingDraft = computed(
  () => saving.value && formValues.value["mg_draft"] === true
);

watch(formValues.value, () => {
  formMessage.value = "";
});

const session = await useSession();
const saveErrorMessage = ref<string>("");
const formMessage = ref<string>("");
const showReAuthenticateButton = ref<boolean>(false);

const rowType = computed(() => props.metadata.id);
const isDraft = computed(() => formValues.value["mg_draft"] === true || false);

function initFormValues() {
  const values =
    structuredClone(toRaw(props.formValues)) ||
    getInitialFormValues(props.metadata);
  return Object.assign(values, props.constantValues || {});
}

const requiredMessage = computed(() => {
  if (!visible.value) {
    return "";
  }
  return form?.requiredMessage.value || "";
});

const errorMessage = computed(() => {
  if (!visible.value) {
    return "";
  }
  return form?.errorMessage.value || "";
});

function gotoSection(sectionId: string) {
  form?.gotoSection(sectionId);
}

function onCancel() {
  visible.value = false;
  emit("update:cancelled");
}

function handleError(err: unknown, defaultMessage: string) {
  console.error("Error saving data", err);
  if (err instanceof SessionExpiredError) {
    saveErrorMessage.value =
      "Your session has expired. Please re-authenticate to continue.";
    showReAuthenticateButton.value = true;
  } else {
    saveErrorMessage.value = errorToMessage(err, defaultMessage);
  }
}

async function onSave(draft: boolean) {
  saveErrorMessage.value = "";
  formMessage.value = "";
  saving.value = true;
  await nextTick();

  const isReadyForSubmit = draft ? form?.isDraftValid() : form?.isValid();

  if (isReadyForSubmit) {
    try {
      formValues.value["mg_draft"] = draft;

      let resp: IRow | null = null;
      if (isInsert.value) {
        resp = await form?.insertInto();
      } else {
        resp = await form?.updateInto();
      }

      if (!resp) {
        throw new Error(
          `No response from server on ${isInsert.value ? "insert" : "update"}`
        );
      }
      formMessage.value = `${isInsert.value ? "inserted" : "saved"} ${
        rowType.value
      } ${draft ? "as draft" : ""}`;
      showFormMessage.value = true;
      emit(isInsert.value ? "update:added" : "update:updated", resp);
      isInsert.value = false;
    } catch (err) {
      handleError(err, "Error saving data");
    } finally {
      saving.value = false;
    }
  } else {
    saving.value = false;
  }
}

watch(formValues.value, () => {
  formMessage.value = "";
});

function reAuthenticate() {
  session.reAuthenticate(
    saveErrorMessage,
    showReAuthenticateButton,
    formMessage
  );
}

const showFormMessage = ref(false);
</script>
