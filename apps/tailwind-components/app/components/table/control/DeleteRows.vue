<script setup lang="ts">
import { ref } from "vue";
import type {
  columnId,
  columnValue,
  ITableMetaData,
} from "../../../../../metadata-utils/src/types";
import { useSession } from "../../../composables/useSession";
import { useTable } from "../../../composables/useTable";
import FormError from "../../form/Error.vue";
import Button from "../../Button.vue";
import BaseIcon from "../../BaseIcon.vue";
import Modal from "../../Modal.vue";

const session = await useSession();
const table = useTable();

const props = defineProps<{
  schemaId: string;
  metadata: ITableMetaData;
  keys: Set<Record<columnId, columnValue>>;
}>();

const emit = defineEmits<{
  (e: "update:deleted", deleted: boolean): void;
  (e: "update:cancelled", cancelled: boolean): void;
}>();

const visible = defineModel("visible", {
  type: Boolean,
  default: true,
});

const deleteErrorMessage = ref<string>("");
const message = ref<string>("");
const showReAuthenticateButton = ref<boolean>(false);

function reAuthenticate() {
  session.reAuthenticate(deleteErrorMessage, showReAuthenticateButton, message);
}

function onDeleteConfirm() {
  table.deleteRecords(props.schemaId, props.metadata.id, props.keys);
  emit("update:deleted", true);
  visible.value = false;
}
</script>
<template>
  <Modal v-model:visible="visible" max-width="max-w-9/10">
    <template #header>
      <header class="pt-[36px] px-8 overflow-y-auto border-b border-divider">
        <div class="mb-5 relative flex items-center">
          <h2
            class="uppercase text-heading-4xl font-display text-title-contrast"
          >
            Delete {{ keys.size }} {{ keys.size === 1 ? "row" : "rows" }}
          </h2>
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

    <section class="grid grid-cols-4 gap-1"></section>
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

    <div class="w-[90%] m-auto py-4">
      <p class="text-body-regular text-text-contrast">
        Are you sure you want to delete the selected {{ keys.size }}
        {{ keys.size === 1 ? "row" : "rows" }}? This action cannot be undone.
      </p>
    </div>

    <template #footer>
      <div class="flex justify-between items-center">
        <menu class="flex items-center justify-end h-[116px]">
          <div class="flex gap-4">
            <Button type="secondary" @click="visible = false">Cancel</Button>
            <Button type="primary" @click="onDeleteConfirm">Delete</Button>
          </div>
        </menu>
      </div>
    </template>
  </Modal>
</template>
