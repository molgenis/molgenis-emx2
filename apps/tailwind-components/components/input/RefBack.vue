<script lang="ts" setup>
import type { IInputProps } from "../../types/types";
import type {
  columnValue,
  columnValueObject,
} from "../../../metadata-utils/src/types";
import InputRefListItem from "./ref/ListItem.vue";
import fetchRowData from "../../composables/fetchRowData";
import fetchTableMetadata from "../../composables/fetchTableMetadata";
import fetchRowPrimaryKey from "../../composables/fetchRowPrimaryKey";
import DeleteModal from "../form/DeleteModal.vue";
import EditModal from "../form/EditModal.vue";
import AddModal from "../form/AddModal.vue";
import { ref, watch } from "vue";

const props = withDefaults(
  defineProps<
    IInputProps & {
      refSchemaId: string;
      refTableId: string;
      refLabel: string;
      canEdit?: boolean;
    }
  >(),
  {
    canEdit: true,
  }
);

const metadata = await fetchTableMetadata(props.refSchemaId, props.refTableId);

const modelValue = defineModel<columnValueObject[]>();

async function fetchRowDetails(rowIndex: number) {
  if (modelValue.value && modelValue.value[rowIndex]) {
    const rowKey = await fetchRowPrimaryKey(
      modelValue.value[rowIndex],
      props.refTableId,
      props.refSchemaId
    );
    const rowData = await fetchRowData(
      props.refSchemaId,
      props.refTableId,
      rowKey
    );
    modelValue.value[rowIndex] = rowData;
  }
}

const showDeleteModal = ref(false);
const showEditModal = ref(false);
const showAddModal = ref(false);
const crudRow = ref<Record<string, columnValue> | null>(null);

async function removeRefBackItem(rowIndex: number) {
  console.log("Removing row at index:", rowIndex);
  if (modelValue.value && modelValue.value[rowIndex]) {
    const rowKey = await fetchRowPrimaryKey(
      modelValue.value[rowIndex],
      props.refTableId,
      props.refSchemaId
    );
    const rowData = await fetchRowData(
      props.refSchemaId,
      props.refTableId,
      rowKey
    );
    showDeleteModal.value = true;
    crudRow.value = rowData;
  }
}

function afterRowDeleted(row: columnValueObject) {
  // todo:  remove the row from modelValue
  crudRow.value = null;
  showDeleteModal.value = false;
}

function afterRowAdded(row: columnValueObject) {
  // todo: add the new row to the modelValue array
  showAddModal.value = false;
}

watch(showDeleteModal, (newValue) => {
  if (newValue === false) {
    crudRow.value = null;
  }
});

async function editRefBackItem(rowIndex: number) {
  if (modelValue.value && modelValue.value[rowIndex]) {
    const rowKey = await fetchRowPrimaryKey(
      modelValue.value[rowIndex],
      props.refTableId,
      props.refSchemaId
    );
    const rowData = await fetchRowData(
      props.refSchemaId,
      props.refTableId,
      rowKey
    );
    showEditModal.value = true;
    crudRow.value = rowData;
  }
}

function afterRowEdited(row: columnValueObject) {
  console.log("Row edited:", row);
  showEditModal.value = false;
  crudRow.value = null;
}
</script>
<template>
  <Button
    v-if="canEdit"
    class="my-[10px]"
    icon="plus"
    type="text"
    size="small"
    @click="showAddModal = true"
    >Add{{ props.refTableId }}</Button
  >
  <ul class="border divide-y divide-gray-200">
    <InputRefListItem
      v-for="(ref, index) in modelValue"
      :refData="ref"
      :refLabel="props.refLabel"
      :refMetadata="metadata"
      :refSchemaId="props.refSchemaId"
      :canEdit="props.canEdit"
      @expand="fetchRowDetails(index)"
      @remove="removeRefBackItem(index)"
      @edit="editRefBackItem(index)"
    />
  </ul>

  <AddModal
    v-if="showAddModal"
    :schemaId="props.refSchemaId"
    :metadata="metadata"
    @update:added="afterRowAdded"
    v-model:visible="showAddModal"
  />

  <DeleteModal
    v-if="crudRow && showDeleteModal"
    :schemaId="props.refSchemaId"
    :metadata="metadata"
    :formValues="crudRow"
    @update:deleted="afterRowDeleted"
    v-model:visible="showDeleteModal"
  />

  <EditModal
    v-if="crudRow && showEditModal"
    :schemaId="props.refSchemaId"
    :metadata="metadata"
    :formValues="crudRow"
    @update:updated="afterRowEdited"
    v-model:visible="showEditModal"
  />
</template>
