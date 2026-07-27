<script setup lang="ts">
import { computed, ref, useId } from "vue";
import type { IRow } from "../../../../metadata-utils/src/types";
import fetchRecordOfOwnClass from "../../composables/fetchRecordOfOwnClass";
import { useSession } from "../../composables/useSession";
import Button from "../Button.vue";
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

const { isAdmin, session } = await useSession(props.schemaId);
const { metadata: tableMetadata, row } = await fetchRecordOfOwnClass(
  props.schemaId,
  props.tableId,
  props.rowId
);

const canModifyRecord = computed(() => {
  return (
    session.value?.roles?.[props.schemaId]?.includes("Editor") || isAdmin.value
  );
});

function afterEditClosed() {
  showEditModal.value = false;
  emit("changed");
}
</script>

<template>
  <div class="flex pb-7.5 gap-2.5 justify-end">
    <Button
      v-if="canModifyRecord"
      type="outline"
      icon="edit"
      @click="showEditModal = true"
      >Edit
    </Button>
    <Button
      v-if="canModifyRecord"
      type="outline"
      icon="trash"
      @click="showDeleteModal = true"
      >Delete
    </Button>
  </div>

  <DeleteModal
    v-if="tableMetadata && row && showDeleteModal"
    :showButton="false"
    :schemaId="schemaId"
    :metadata="tableMetadata"
    :formValues="row"
    v-model:visible="showDeleteModal"
    @update:deleted="emit('deleted')"
    @update:cancelled="showDeleteModal = false"
  />

  <EditModal
    v-if="tableMetadata && row && showEditModal"
    :key="editModalKey"
    :showButton="false"
    :schemaId="schemaId"
    :metadata="tableMetadata"
    :formValues="row"
    :isInsert="false"
    v-model:visible="showEditModal"
    @update:cancelled="afterEditClosed"
    @update:added="afterEditClosed"
    @update:edited="afterEditClosed"
  />
</template>
