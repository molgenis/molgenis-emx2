<template>
  <td
    class="h-4 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5 overflow-ellipsis whitespace-nowrap overflow-hidden"
  >
    <div class="flex justify-between">
      <span
        class="py-2.5 text-body-base flex items-center shrink overflow-ellipsis whitespace-nowrap overflow-hidden flex-0"
      >
        <template v-if="data == null || data === undefined"></template>
        <ValueList
          v-else-if="metaData.columnType.endsWith('ARRAY')"
          :metaData="metaData"
          :data="data"
        />

        <ValueString
          v-else-if="metaData.columnType === 'STRING'"
          :metaData="metaData"
          :data="data"
        />

        <ValueText
          v-else-if="metaData.columnType === 'TEXT'"
          :metaData="metaData"
          :data="data"
        />

        <ValueDecimal
          v-else-if="metaData.columnType === 'DECIMAL'"
          :metaData="metaData"
          :data="data"
        />

        <ValueLong
          v-else-if="metaData.columnType === 'LONG'"
          :metaData="metaData"
          :data="typeof data === 'number' ? data : Number(data)"
        />

        <ValueInt
          v-else-if="metaData.columnType === 'INT'"
          :metaData="metaData"
          :data="typeof data === 'number' ? data : Number(data)"
        />

        <ValueRef
          v-else-if="metaData.columnType === 'REF'"
          :metaData="metaData as IRefColumn"
          :data="data"
          @refCellClicked="$emit('cellClicked', $event)"
        />

        <ValueObject
          v-else-if="metaData.columnType === 'ONTOLOGY'"
          :metaData="metaData"
          :data="data"
        />

        <ValueBool
          v-else-if="metaData.columnType === 'BOOL'"
          :metaData="metaData"
          :data="data"
        />

        <ValueEmail
          v-else-if="metaData.columnType === 'EMAIL'"
          :metaData="metaData"
          :data="data"
        />

        <ValueHyperlink
          v-else-if="metaData.columnType === 'HYPERLINK'"
          :metaData="metaData"
          :data="data"
        />

        <ValueRefBack
          v-else-if="metaData.columnType === 'REFBACK'"
          :metaData="metaData as IRefColumn"
          :data="data"
          @refBackCellClicked="$emit('cellClicked', $event)"
        />

        <ValueFile
          v-else-if="metaData.columnType === 'FILE'"
          :metaData="metaData"
          :data="data"
        />

        <template v-else> {{ metaData.columnType }} </template>
      </span>
      <slot />
    </div>
  </td>
</template>

<script setup lang="ts">
import type { IColumn, IRefColumn } from "../../../metadata-utils/src/types";
import type { RefPayload } from "../../types/types";
defineProps<{
  metaData: IColumn;
  data: any;
}>();

defineEmits<{
  (e: "cellClicked", payload: RefPayload): void;
}>();
</script>
