<script lang="ts" setup>
import type { IInputProps } from "~/types/types";
import type { columnValueObject } from "../../../metadata-utils/src/types";
import InputRefListItem from "./ref/ListItem.vue";
import fetchRowData from "../../composables/fetchRowData";
import fetchTableMetadata from "../../composables/fetchTableMetadata";
import fetchRowPrimaryKey from "../../composables/fetchRowPrimaryKey";

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
    const rowKey = await fetchRowPrimaryKey(modelValue.value[rowIndex], props.refTableId, props.refSchemaId);
    const rowData = await fetchRowData(
      props.refSchemaId,
      props.refTableId,
      rowKey
    );
    modelValue.value[rowIndex] = rowData;
  }
}
</script>
<template>
  <Button v-if="canEdit" icon="plus" type="text" size="small"
    >Add{{ props.refTableId }}</Button
  >
  <ul class="border divide-y divide-gray-200">
    <InputRefListItem
      v-for="(ref, index) in modelValue"
      :refData="ref"
      :refLabel="props.refLabel"
      :refMetadata="metadata"
      :canEdit="props.canEdit"
      @expand="fetchRowDetails(index)"
    />
  </ul>
</template>
