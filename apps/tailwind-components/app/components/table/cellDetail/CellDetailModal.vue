<template>
  <Modal
    v-if="showModal"
    type="right"
    :visible="showModal"
    @update:visible="(event) => emit('update:showModal', event)"
    :title="subtitle"
    @closed="showModal = false"
  >
    <TableCellDetailRef
      v-if="column && isRefDetailModal"
      :metadata="toRefColumn(column)"
      :columnValue="toRefColumnValue(value)"
      :schema="cellSchemaId ?? schemaId"
      :showDataOwner="false"
      @onRefClick="handleDetailRefClick"
    />
    <template v-else-if="value && column && isArrayLikeDetail(column)">
      <ul>
        <li v-for="(item, index) in value" :key="index">
          <TableCellDetailRef
            v-if="column && isRefLikeDetail(column)"
            :metadata="toRefColumn(column)"
            :columnValue="toRefColumnValue(item as columnValue)"
            :schema="cellSchemaId ?? schemaId"
            :showDataOwner="false"
            @onRefClick="handleDetailRefClick"
          />
          <span v-else>{{ item }}</span>
        </li>
      </ul>
    </template>
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
import type {
  cellPayload,
  ColumnPayload,
  ListPayload,
  RefPayload,
} from "../../../../types/types";
import type {
  columnValue,
  IColumn,
} from "../../../../../metadata-utils/src/types";
import { computed, nextTick } from "vue";
import { isArrayLikeDetail, isRefLikeDetail } from "../../../utils/refUtils";
import { toRefColumn, toRefColumnValue } from "../../../utils/typeUtils";

const props = defineProps<{
  payload?: cellPayload;
  column?: IColumn;
  schemaId?: string;
  showModal: boolean;
}>();

const emit = defineEmits<{
  (event: "update:showModal", value: boolean): void;
  (
    event: "update:payload",
    value: RefPayload | ColumnPayload | ListPayload
  ): void;
}>();

const subtitle = computed(() => props.column?.label ?? "");
const value = computed(() => props.payload?.data as columnValue);
const cellSchemaId = computed(
  () => props.column?.refSchemaId ?? props.schemaId ?? ""
);

async function handleDetailRefClick(
  event: RefPayload | ColumnPayload | ListPayload
) {
  emit("update:showModal", false);
  await nextTick();
  emit("update:payload", event);
  emit("update:showModal", true);
}

const isRefDetailModal = computed(() => {
  return (
    props.column &&
    isRefLikeDetail(props.column) &&
    !isArrayLikeDetail(props.column)
  );
});
</script>
