<template>
  <template v-if="showButton">
    <slot :setVisible="setVisible">
      <Button
        class="m-10"
        type="primary"
        size="small"
        icon="plus"
        @click="visible = true"
        >{{ insertOrUpdateLabel }} {{ rowType }}</Button
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
            {{ insertOrUpdateLabel }} {{ rowType }}
            {{ editFormValues["mg_draft"] ? "(status=draft)" : "" }}
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
    <Transition name="slide-up">
      <FormError
        v-show="errorMessage"
        :message="errorMessage"
        class="sticky mx-4 h-[62px] bottom-0 transition-all transition-discrete"
        @error-prev="gotoPreviousError"
        @error-next="gotoNextError"
      />
    </Transition>
    <Transition name="slide-up">
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
    </Transition>
    <Transition name="slide-up">
      <FormMessage
        v-show="formMessage"
        :message="formMessage"
        class="sticky mx-4 h-[62px] bottom-0 transition-all transition-discrete"
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
            <Button type="outline" @click="onSave(true)"
              >{{ insertOrUpdateLabel }} as draft</Button
            >
            <Button type="primary" @click="onSave(false)"
              >{{ insertOrUpdateLabel }} {{ rowType }}</Button
            >
          </div>
        </menu>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { computed, ref, toRaw, watch } from "vue";
import type { ITableMetaData } from "../../../metadata-utils/src";
import type {
  columnId,
  columnValue,
  IRow,
} from "../../../metadata-utils/src/types";
import useForm from "../../composables/useForm";
import { errorToMessage } from "../../utils/errorToMessage";
import FormFields from "./Fields.vue";
import { SessionExpiredError } from "../../utils/sessionExpiredError";
import { useSession } from "../../composables/useSession";
import PreviousSectionNav from "./PreviousSectionNav.vue";
import NextSectionNav from "./NextSectionNav.vue";
import fetchRowPrimaryKey from "../../composables/fetchRowPrimaryKey";

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
const rowKey = ref<Record<string, columnValue>>();
const isInsert = ref(true);
const editFormValues = ref<Record<string, columnValue>>({});
watch(
  () => props.formValues,
  () => {
    if (props.formValues) {
      editFormValues.value = structuredClone(toRaw(props.formValues));
      updateRowKey();
      isInsert.value = false;
    }
  }
);

const session = await useSession();
const saveErrorMessage = ref<string>("");
const formMessage = ref<string>("");
const showReAuthenticateButton = ref<boolean>(false);

function setVisible() {
  visible.value = true;
}

const rowType = computed(() => props.metadata.id);
const isDraft = ref(false);

function onCancel() {
  visible.value = false;
  emit("update:cancelled");
}

const insertOrUpdateLabel = computed(() => {
  return isInsert.value ? "Insert" : "Save";
});

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
    if (draft) {
      editFormValues.value["mg_draft"] = true;
    } else {
      editFormValues.value["mg_draft"] = false;
    }
    const resp = isInsert.value ? await insertInto() : await updateInto();
    if (!resp) {
      return;
    }
    formMessage.value =
      (isInsert.value ? "inserted 1 " : "saved 1 ") +
      rowType.value +
      (draft ? " as draft" : "");
    emit(isInsert.value ? "update:added" : "update:updated", resp);
    if (isInsert.value) {
      await updateRowKey();
    }
    isInsert.value = false;
  } catch (err) {
    handleError(err, "Error saving data");
  }
}

watch(visible, (newValue, oldValue) => {
  if (newValue && !oldValue) {
    //
  }
});

watch(editFormValues.value, () => {
  formMessage.value = "";
});

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
} = useForm(props.metadata, editFormValues, "fields-container");

function reAuthenticate() {
  session.reAuthenticate(
    saveErrorMessage,
    showReAuthenticateButton,
    formMessage
  );
}
</script>
