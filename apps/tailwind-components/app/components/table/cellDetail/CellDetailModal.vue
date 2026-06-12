<template>
  <Modal
    v-if="showModal"
    type="right"
    :visible="showModal"
    @update:visible="(event) => emit('update:showModal', event)"
    :title="subtitle"
    @closed="emit('update:showModal', false)"
  >
    <template
      v-if="isArrayLikeDetail(payload.metadata) && Array.isArray(payload.data)"
    >
      <ul>
        <li v-for="(item, index) in payload.data" :key="index">
          <TableCellDetailRef
            v-if="isRefLikeDetail(payload.metadata)"
            :metadata="toRefColumn(payload.metadata)"
            :columnValue="toRefColumnValue(item)"
            :schema="cellSchemaId || schemaId"
            :showDataOwner="false"
            @onRefClick="handleDetailRefClick"
          />
          <div
            v-else
            class="px-8 first:pt-[50px] last:pb-[50px]"
            style="overflow-wrap: break-word"
          >
            {{ item }}
          </div>
        </li>
      </ul>
    </template>

    <TableCellDetailRef
      v-else-if="
        isRefLikeDetail(payload.metadata) && !Array.isArray(payload.data)
      "
      :metadata="toRefColumn(payload.metadata)"
      :columnValue="toRefColumnValue(payload.data)"
      :schema="cellSchemaId || schemaId"
      :showDataOwner="false"
      @onRefClick="handleDetailRefClick"
    />

    <template v-else>
      <div
        class="px-8 first:pt-[50px] last:pb-[50px]"
        style="overflow-wrap: break-word"
      >
        {{ payload.data }}
      </div>
    </template>
  </Modal>
</template>

<script lang="ts" setup>
import { computed, nextTick } from "vue";
import type { cellPayload } from "../../../../types/types";
import { isArrayLikeDetail, isRefLikeDetail } from "../../../utils/refUtils";
import { toRefColumn, toRefColumnValue } from "../../../utils/typeUtils";

const props = defineProps<{
  payload: cellPayload;
  schemaId: string;
  showModal: boolean;
}>();

const emit = defineEmits<{
  (event: "update:showModal", value: boolean): void;
  (event: "update:cellDetailValue", value: cellPayload): void;
}>();

const subtitle = computed(() => props.payload.metadata.label ?? "");

const cellSchemaId = computed(
  () => props.payload.metadata.refSchemaId ?? props.schemaId ?? ""
);

async function handleDetailRefClick(event: cellPayload) {
  emit("update:showModal", false);
  await nextTick();
  emit("update:cellDetailValue", event);
  emit("update:showModal", true);
}
</script>
