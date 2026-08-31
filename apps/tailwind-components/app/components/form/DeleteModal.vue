<script setup lang="ts">
import { computed, ref } from "vue";
import type { ITableMetaData } from "../../../../metadata-utils/src";
import type {
  columnId,
  columnValue,
} from "../../../../metadata-utils/src/types";
import useForm from "../../composables/useForm";
import { errorToMessage } from "../../utils/errorToMessage";
import RecordKeyAccordion from "../display/RecordKeyAccordion.vue";
import { useSession } from "../../composables/useSession";
import { SessionExpiredError } from "../../utils/sessionExpiredError";
import Modal from "../Modal.vue";
import Button from "../Button.vue";
import BaseIcon from "../BaseIcon.vue";
import FormError from "./Error.vue";
import FormMessage from "./Message.vue";
import DraftLabel from "../label/DraftLabel.vue";
import { useTable } from "../../composables/useTable";

const props = withDefaults(
  defineProps<{
    schemaId: string;
    metadata: ITableMetaData;
    formValues: Record<columnId, columnValue>;
    showButton?: boolean;
  }>(),
  {
    showButton: true,
  }
);

const emit = defineEmits(["update:deleted", "update:cancelled"]);

const visible = defineModel("visible", {
  type: Boolean,
  default: false,
});

const deleteErrorMessage = ref<string>("");
const cascadeDeleteMsg = ref<string | null>(null);

const session = await useSession();
const formMessage = ref<string>("");
const showReAuthenticateButton = ref<boolean>(false);

const rowType = computed(() => props.metadata.id);
const isDraft = ref(false);

fetchCascadeDeleteMessage();

function onCancel() {
  visible.value = false;
  emit("update:cancelled");
}

async function onDeleteConfirm() {
  const { deleteRecord } = useForm(props.metadata, props.formValues);
  const resp = await deleteRecord().catch((err) => {
    console.error("Error deleting data", err);

    if (err instanceof SessionExpiredError) {
      deleteErrorMessage.value =
        "Your session has expired. Please re-authenticate to continue.";
      showReAuthenticateButton.value = true;
    } else {
      deleteErrorMessage.value = errorToMessage(err, "Error deleting record");
    }

    return null;
  });

  if (!resp) {
    return;
  }

  isDraft.value = false;
  visible.value = false;
  emit("update:deleted", resp);
}

function reAuthenticate() {
  session.reAuthenticate(
    deleteErrorMessage,
    showReAuthenticateButton,
    formMessage
  );
}

function fetchCascadeDeleteMessage() {
  const { cascadeDeleteConfirmationMsg } = useTable(
    props.schemaId,
    props.metadata.id
  );
  cascadeDeleteConfirmationMsg()
    .then((msg) => {
      cascadeDeleteMsg.value = msg;
    })
    .catch((err) => {
      console.error("Error fetching cascade delete message", err);
      cascadeDeleteMsg.value = null;
    });
}
</script>

<template>
  <template v-if="showButton">
    <slot>
      <Button
        class="m-10"
        type="primary"
        size="small"
        icon="plus"
        @click="visible = true"
        >Delete {{ rowType }}</Button
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
            Delete {{ rowType }}
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

    <ModalContentContainer>
      <Transition name="slide-up">
        <FormError
          v-show="deleteErrorMessage"
          :message="deleteErrorMessage"
          :show-prev-next-buttons="false"
          class="sticky mx-4 bottom-0 transition-all transition-discrete"
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

      <div class="w-[90%] mx-auto">
        <RecordKeyAccordion :metadata="metadata" :row-data="formValues" />

        <div class="text-title-contrast-pop py-8" v-if="cascadeDeleteMsg">
          {{ cascadeDeleteMsg }}
        </div>
      </div>
    </ModalContentContainer>

    <template #footer>
      <div class="flex justify-between items-center">
        <menu class="flex items-center justify-end h-modal-footer">
          <div class="flex gap-4">
            <Button type="secondary" @click="onCancel">Cancel</Button>
            <Button type="primary" @click="onDeleteConfirm">Delete</Button>
          </div>
        </menu>
      </div>
    </template>
  </Modal>
</template>
