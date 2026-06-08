<template>
  <Modal
    v-if="showModal && column && value !== undefined"
    type="right"
    :visible="showModal"
    @update:visible="(event) => emit('update:showModal', event)"
    :title="subtitle"
    @closed="showModal = false"
  >
    <template v-if="isArrayLikeDetail(column) && Array.isArray(value)">
      <ul>
        <li v-for="(item, index) in value" :key="index">
          <TableCellDetailRef
            v-if="column"
            :metadata="toRefColumn(column)"
            :columnValue="toRefColumnValue(item)"
            :schema="cellSchemaId ?? schemaId"
            :showDataOwner="false"
            @onRefClick="handleDetailRefClick"
          />
          <span v-else> {{ item }}</span>
        </li>
      </ul>
    </template>
    <TableCellDetailRef
      v-else-if="isRefLikeDetail(column) && !Array.isArray(value)"
      :metadata="toRefColumn(column)"
      :columnValue="toRefColumnValue(value)"
      :schema="cellSchemaId ?? schemaId"
      :showDataOwner="false"
      @onRefClick="handleDetailRefClick"
    />
    <template v-else>
      <div
        class="px-8 first:pt-[50px] last:pb-[50px]"
        style="overflow-wrap: break-word"
      >
        {{ value }}
      </div>
    </template>
  </Modal>
</template>

<script lang="ts" setup>
import { computed, nextTick } from "vue";
import type {
  columnValue,
  IColumn,
} from "../../../../../metadata-utils/src/types";
import type {
  ColumnPayload,
  ListPayload,
  RefPayload,
} from "../../../../types/types";
import { isArrayLikeDetail, isRefLikeDetail } from "../../../utils/refUtils";
import { toRefColumn, toRefColumnValue } from "../../../utils/typeUtils";

const props = defineProps<{
  value?: columnValue | columnValue[];
  column?: IColumn;
  schemaId?: string;
  showModal: boolean;
}>();

const emit = defineEmits<{
  (event: "update:showModal", value: boolean): void;
  (
    event: "update:cellDetailValue",
    value: { data: columnValue | columnValue[]; column: IColumn | undefined }
  ): void;
}>();

const subtitle = computed(() => props.column?.label ?? "");

const cellSchemaId = computed(
  () => props.column?.refSchemaId ?? props.schemaId ?? ""
);

async function handleDetailRefClick(
  event: RefPayload | ColumnPayload | ListPayload
) {
  emit("update:showModal", false);
  await nextTick();
  emit("update:cellDetailValue", {
    data: event.data,
    column: props.column,
  });
  emit("update:showModal", true);
}
</script>
