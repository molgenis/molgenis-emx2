<script lang="ts" setup>
import { computed, ref, unref, watch, type MaybeRef } from "vue";
import type {
  IRow,
  columnValue,
  columnValueObject,
} from "../../../../metadata-utils/src/types";
import type { IInputProps } from "../../../types/types";
import fetchRowData from "../../composables/fetchRowData";
import fetchRowPrimaryKey from "../../composables/fetchRowPrimaryKey";
import fetchTableData from "../../composables/fetchTableData";
import fetchTableMetadata from "../../composables/fetchTableMetadata";
import Button from "../Button.vue";
import DeleteModal from "../form/DeleteModal.vue";
import EditModal from "../form/EditModal.vue";
import Message from "../Message.vue";
import InputRefListItem from "./ref/ListItem.vue";

const props = withDefaults(
  defineProps<
    IInputProps & {
      refSchemaId: string;
      refTableId: string;
      refLabel: string;
      canEdit?: boolean;
      refBackColumn: string;
      refBackPrimaryKey?: MaybeRef<columnValue>;
    }
  >(),
  {
    canEdit: true,
  }
);

const modelValue = defineModel<columnValueObject[] | null>();

const metadata = await fetchTableMetadata(props.refSchemaId, props.refTableId);

const hasPrimaryKey = computed(() => {
  const primaryKey = unref(props.refBackPrimaryKey);
  return primaryKey && typeof primaryKey === "object"
    ? Boolean(Object.values(primaryKey).length)
    : false;
});

const addModalConstantValues = computed(() => {
  const result: IRow = {};
  result[props.refBackColumn as string] = unref(props.refBackPrimaryKey);
  return result;
});

watch(() => props.refTableId, reloadItems);

async function reloadItems() {
  const resp = await fetchTableData(props.refSchemaId, props.refTableId, {
    filter: {
      [props.refBackColumn]: {
        equals: unref(props.refBackPrimaryKey),
      },
    },
  });
  modelValue.value = resp.rows;
}

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

function afterRowDeleted() {
  crudRow.value = null;
  showDeleteModal.value = false;
  reloadItems();
}

function afterRowAdded() {
  showAddModal.value = false;
  reloadItems();
}

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

function afterRowEdited() {
  showEditModal.value = false;
  crudRow.value = null;
  reloadItems();
}

watch(showDeleteModal, (newValue) => {
  if (newValue === false) {
    crudRow.value = null;
  }
});
</script>

<template>
  <div v-if="!hasPrimaryKey" class="my-3">
    <Message class="p-6" id="`${id}-key-msg`">
      This {{ props.refTableId }} can only be filled in after you have saved (or
      saved draft).
    </Message>
  </div>
  <template v-else>
    <Button
      v-if="canEdit"
      class="my-[10px]"
      icon="plus"
      type="text"
      size="small"
      @click="showAddModal = true"
    >
      Add {{ metadata.label }}
    </Button>
    <ul v-if="modelValue" class="border divide-y">
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

    <EditModal
      v-if="showAddModal"
      :schemaId="props.refSchemaId"
      :metadata="metadata"
      :constantValues="addModalConstantValues"
      :isInsert="true"
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
      :isInsert="false"
      @update:updated="afterRowEdited"
      v-model:visible="showEditModal"
    />
  </template>
</template>
