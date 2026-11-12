<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type {
  ITableMetaData,
  TruncateStatus,
} from "../../../../../metadata-utils/src/types";
import Button from "../../Button.vue";
import Modal from "../../Modal.vue";
import ModalContentContainer from "../../ModalContentContainer.vue";
import BaseIcon from "../../BaseIcon.vue";
import { useTable } from "../../../composables/useTable";

const props = defineProps<{
  metadata: ITableMetaData;
}>();

const emit = defineEmits<{
  (e: "update:truncated", truncated: boolean): void;
}>();

const isModalOpen = ref(false);
const truncateStatus = ref<TruncateStatus>("REQUEST_CONFIRMATION");
const truncateResultMessage = ref<string | null>(null);
const showResultDetails = ref(false);
const modalSize = computed(() =>
  truncateResultMessage.value ? "medium" : "small"
);

async function handleTruncate() {
  const table = useTable(props.metadata.schemaId, props.metadata.id);
  const truncateResult = await table.truncate(truncateStatus);
  if (truncateResult.status === "FAILED") {
    truncateResultMessage.value =
      truncateResult.description || "An unknown error occurred.";
  }
}

function showConfirmationModal() {
  isModalOpen.value = true;
}

watch(isModalOpen, (newStatus) => {
  if (!newStatus) {
    resetModal();
  }
});

function resetModal() {
  if (truncateStatus.value === "COMPLETED") {
    emit("update:truncated", true);
  }
  isModalOpen.value = false;
  truncateStatus.value = "REQUEST_CONFIRMATION";
  truncateResultMessage.value = null;
  showResultDetails.value = false;
}
</script>

<template>
  <slot :showConfirmationModal="showConfirmationModal">
    <Button type="outline" size="medium" @click="isModalOpen = true">
      Truncate
    </Button>
  </slot>

  <Modal
    v-model:visible="isModalOpen"
    :title="`Truncate ${metadata.label}`"
    :size="modalSize"
  >
    <ModalContentContainer>
      <p
        class="text-sm text-title-contrast"
        v-if="truncateStatus === 'REQUEST_CONFIRMATION'"
      >
        Are you sure you want to truncate the table
        <strong>{{ metadata.label }}</strong> ? This removes all data from the
        table. This action cannot be undone.
      </p>
      <div
        class="text-sm flex flex-row gap-1"
        v-else-if="truncateStatus === 'RUNNING'"
      >
        <span class="text-sm text-title-contrast">
          Truncating the table <strong>{{ metadata.label }}</strong>
        </span>
        <BaseIcon class="ml-2 animate-spin" name="ProgressActivity" />
      </div>
      <p
        class="text-sm text-title-contrast"
        v-else-if="truncateStatus === 'COMPLETED'"
      >
        Successfully truncated the table <strong>{{ metadata.label }}</strong>
        >.
      </p>
      <div class="text-sm" v-else-if="truncateStatus === 'FAILED'">
        <div class="flex flex-row gap-2 items-center">
          <p class="text-sm text-title-contrast">
            Failed to truncate the table <strong>{{ metadata.label }}</strong
            >.
          </p>
          <Button
            v-if="truncateResultMessage"
            type="text"
            size="tiny"
            @click="showResultDetails = !showResultDetails"
          >
            {{ showResultDetails ? "Hide" : "Show" }} Details
          </Button>
        </div>

        <p
          v-if="truncateResultMessage && showResultDetails"
          class="mt-2 italic text-sm text-title-contrast"
        >
          {{ truncateResultMessage }}
        </p>
      </div>
    </ModalContentContainer>

    <template #footer>
      <menu class="flex items-center justify-end gap-4 h-modal-footer">
        <Button
          type="outline"
          size="medium"
          @click="resetModal"
          :disabled="truncateStatus === 'RUNNING'"
          v-if="
            truncateStatus === 'REQUEST_CONFIRMATION' ||
            truncateStatus === 'RUNNING'
          "
        >
          Cancel
        </Button>
        <Button
          type="primary"
          size="medium"
          @click="handleTruncate"
          v-if="
            truncateStatus === 'REQUEST_CONFIRMATION' ||
            truncateStatus === 'RUNNING'
          "
          :disabled="truncateStatus === 'RUNNING'"
        >
          Truncate
        </Button>
        <Button
          v-if="truncateStatus === 'COMPLETED' || truncateStatus === 'FAILED'"
          type="primary"
          size="medium"
          @click="resetModal"
        >
          Close
        </Button>
      </menu>
    </template>
  </Modal>
</template>
