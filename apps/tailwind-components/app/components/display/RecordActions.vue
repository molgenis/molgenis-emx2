<script setup lang="ts">
import { computed, ref, shallowRef, useId } from "vue";
import type { IRow } from "../../../../metadata-utils/src/types";
import type { IRecordOfOwnClass } from "../../composables/fetchRecordOfOwnClass";
import fetchRecordOfOwnClass from "../../composables/fetchRecordOfOwnClass";
import { useSession } from "../../composables/useSession";
import Button from "../Button.vue";
import Message from "../Message.vue";
import DeleteModal from "../form/DeleteModal.vue";
import EditModal from "../form/EditModal.vue";

const props = defineProps<{
  schemaId: string;
  tableId: string;
  rowId: IRow;
}>();

const emit = defineEmits<{
  (event: "changed"): void;
  (event: "deleted"): void;
}>();

const showEditModal = ref(false);
const showDeleteModal = ref(false);
const editModalKey = `edit-modal-${useId()}`;
const loadErrorId = `record-actions-error-${useId()}`;

const { isAdmin, session } = await useSession(props.schemaId);

const canModifyRecord = computed(() => {
  return (
    session.value?.roles?.[props.schemaId]?.includes("Editor") || isAdmin.value
  );
});

const record = shallowRef<IRecordOfOwnClass | null>(null);
const loadFailed = ref(false);

if (canModifyRecord.value) {
  try {
    record.value = await fetchRecordOfOwnClass(
      props.schemaId,
      props.tableId,
      props.rowId
    );
  } catch (error) {
    console.error(
      `Could not load record ${props.tableId} of schema ${props.schemaId} for editing`,
      error
    );
    loadFailed.value = true;
  }
}

function afterEditClosed() {
  showEditModal.value = false;
  emit("changed");
}
</script>

<template>
  <div class="flex pb-7.5 gap-2.5 justify-end">
    <Message v-if="canModifyRecord && loadFailed" :id="loadErrorId" invalid>
      Cannot load this record for editing
    </Message>
    <template v-if="canModifyRecord && record">
      <Button type="outline" icon="edit" @click="showEditModal = true"
        >Edit
      </Button>
      <Button type="outline" icon="trash" @click="showDeleteModal = true"
        >Delete
      </Button>
    </template>
  </div>

  <DeleteModal
    v-if="record && showDeleteModal"
    :showButton="false"
    :schemaId="schemaId"
    :metadata="record.metadata"
    :formValues="record.row"
    v-model:visible="showDeleteModal"
    @update:deleted="emit('deleted')"
    @update:cancelled="showDeleteModal = false"
  />

  <EditModal
    v-if="record && showEditModal"
    :key="editModalKey"
    :showButton="false"
    :schemaId="schemaId"
    :metadata="record.metadata"
    :formValues="record.row"
    :isInsert="false"
    v-model:visible="showEditModal"
    @update:cancelled="afterEditClosed"
    @update:added="afterEditClosed"
    @update:edited="afterEditClosed"
  />
</template>
