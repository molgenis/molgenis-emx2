<template>
  <template v-if="showButton">
    <slot :setVisible="setVisible">
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
      <header class="pt-[36px] px-8 overflow-y-auto border-b border-divider">
        <div class="mb-5 relative flex items-center">
          <h2
            class="uppercase text-heading-4xl font-display text-title-contrast"
          >
            {{ isInsert ? "Add" : "Edit" }} {{ rowType }}
          </h2>

          <DraftLabel v-if="isDraft" />
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

    <div class="grid grid-cols-4 gap-1 min-h-0">
      <div class="col-span-1 bg-form-legend overflow-y-auto h-full min-h-0">
        <FormLegend
          v-if="visible && sections"
          class="sticky top-0"
          :sections="sections"
          @goToSection="gotoSection"
        />
      </div>

      <div
        id="fields-container"
        class="col-span-3 px-4 py-50px overflow-y-auto"
      >
        <PreviousSectionNav
          v-if="previousSection"
          @click="gotoSection(previousSection.id)"
        >
          {{ previousSection.label }}
        </PreviousSectionNav>
        <FormFields
          v-if="visible"
          ref="formFields"
          :row-key="rowKey"
          :columns="visibleColumns"
          :constantValues="constantValues"
          :errorMap="errorMap"
          v-model="editFormValues"
          @update="onUpdateColumn"
          @blur="onBlurColumn"
          @view="onViewColumn"
        />
        <NextSectionNav v-if="nextSection" @click="gotoSection(nextSection.id)">
          {{ nextSection.label }}
        </NextSectionNav>
      </div>
    </div>
    <TransitionSlideUp>
      <FormError
        v-show="errorMessage"
        :message="errorMessage"
        class="sticky mx-4 h-[62px] bottom-0 transition-all transition-discrete"
        @error-prev="gotoPreviousError"
        @error-next="gotoNextError"
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
      <div class="flex justify-between items-center">
        <FormRequiredInfoSection
          :message="requiredMessage"
          @required-next="gotoNextRequiredField"
          @required-prev="gotoPreviousRequiredField"
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
import { computed, ref, toRaw, watch } from "vue";
import { getInitialFormValues } from "../../utils/typeUtils";
import type { ITableMetaData } from "../../../../metadata-utils/src";
import type {
  columnId,
  columnValue,
  IRow,
} from "../../../../metadata-utils/src/types";
import fetchRowPrimaryKey from "../../composables/fetchRowPrimaryKey";
import useForm from "../../composables/useForm";
import { useSession } from "../../composables/useSession";
import { errorToMessage } from "../../utils/errorToMessage";
import { SessionExpiredError } from "../../utils/sessionExpiredError";
import BaseIcon from "../BaseIcon.vue";
import Button from "../Button.vue";
import Modal from "../Modal.vue";
import FormError from "./Error.vue";
import FormFields from "./Fields.vue";
import FormLegend from "./Legend.vue";
import FormMessage from "./Message.vue";
import NextSectionNav from "./NextSectionNav.vue";
import PreviousSectionNav from "./PreviousSectionNav.vue";
import FormRequiredInfoSection from "./RequiredInfoSection.vue";
import DraftLabel from "../label/DraftLabel.vue";
import TransitionSlideUp from "../transition/SlideUp.vue";

const props = withDefaults(
  defineProps<{
    schemaId: string;
    metadata: ITableMetaData;
    constantValues?: IRow;
    showButton?: boolean;
    formValues?: Record<columnId, columnValue>;
  }>(),
  {
    showButton: true,
  }
);

const emit = defineEmits([
  "update:added",
  "update:updated",
  "update:cancelled",
]);
const visible = defineModel("visible", {
  type: Boolean,
  default: false,
});

const saving = ref(false);
const savingDraft = computed(
  () => saving.value && editFormValues.value["mg_draft"] === true
);
const rowKey = ref<Record<string, columnValue>>();
const isInsert = ref(props.formValues ? false : true);
const editFormValues = ref<Record<string, columnValue>>(
  structuredClone(toRaw(props.formValues)) ||
    getInitialFormValues(props.metadata)
);

if (props.formValues) {
  await updateRowKey();
}

watch(visible, (newValue, oldValue) => {
  if (newValue && !oldValue) {
    reset();
  }
});

watch(editFormValues.value, () => {
  formMessage.value = "";
});

const session = await useSession();
const saveErrorMessage = ref<string>("");
const formMessage = ref<string>("");
const showReAuthenticateButton = ref<boolean>(false);

function setVisible() {
  visible.value = true;
}

const rowType = computed(() => props.metadata.id);
const isDraft = computed(
  () => editFormValues.value["mg_draft"] === true || false
);

function onCancel() {
  visible.value = false;
  saveErrorMessage.value = "";
  formMessage.value = "";
  editFormValues.value =
    structuredClone(toRaw(props.formValues)) ||
    getInitialFormValues(props.metadata);
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

async function updateRowKey() {
  rowKey.value = await fetchRowPrimaryKey(
    editFormValues.value,
    props.metadata.id,
    props.metadata.schemaId as string
  );
}

async function onSave(draft: boolean) {
  saveErrorMessage.value = "";
  formMessage.value = "";
  if (!draft) {
    validateAllColumns();
    if (Object.keys(errorMap.value).length > 0) {
      return;
    }
  }
  try {
    editFormValues.value["mg_draft"] = draft;
    saving.value = true;
    const resp = await (isInsert.value ? insertInto() : updateInto()).catch(
      () => (saving.value = false)
    );
    saving.value = false;
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
    if (isInsert.value) {
      await updateRowKey();
    }
    isInsert.value = false;
  } catch (err) {
    handleError(err, "Error saving data");
  }
}

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
  validateAllColumns,
  sections,
  visibleColumns,
  reset,
} = useForm(props.metadata, editFormValues, "fields-container");

function reAuthenticate() {
  session.reAuthenticate(
    saveErrorMessage,
    showReAuthenticateButton,
    formMessage
  );
}

const showFormMessage = ref(false);
</script>
