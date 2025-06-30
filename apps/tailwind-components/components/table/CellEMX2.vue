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
          v-else-if="metadata.columnType.endsWith('ARRAY')"
          :metadata="metadata"
          :data="data"
        />

        <ValueString
          v-else-if="metadata.columnType === 'STRING'"
          :metadata="metadata"
          :data="data"
        />

        <ValueText
          v-else-if="metadata.columnType === 'TEXT'"
          :metadata="metadata"
          :data="data"
        />

        <ValueDecimal
          v-else-if="metadata.columnType === 'DECIMAL'"
          :metadata="metadata"
          :data="data"
        />

        <ValueLong
          v-else-if="metadata.columnType === 'LONG'"
          :metadata="metadata"
          :data="typeof data === 'number' ? data : Number(data)"
        />

        <ValueInt
          v-else-if="metadata.columnType === 'INT'"
          :metadata="metadata"
          :data="typeof data === 'number' ? data : Number(data)"
        />

        <ValueRef
          v-else-if="metadata.columnType === 'REF'"
          :metadata="metadata as IRefColumn"
          :data="data"
          @refCellClicked="$emit('cellClicked', $event)"
        />

        <ValueObject
          v-else-if="metadata.columnType === 'ONTOLOGY'"
          :metadata="metadata"
          :data="data"
        />

        <ValueBool
          v-else-if="metadata.columnType === 'BOOL'"
          :metadata="metadata"
          :data="data"
        />

        <ValueEmail
          v-else-if="metadata.columnType === 'EMAIL'"
          :metadata="metadata"
          :data="data"
        />

        <ValueHyperlink
          v-else-if="metadata.columnType === 'HYPERLINK'"
          :metadata="metadata"
          :data="data"
        />

        <ValueRefBack
          v-else-if="metadata.columnType === 'REFBACK'"
          :metadata="metadata as IRefColumn"
          :data="data"
          @refBackCellClicked="$emit('cellClicked', $event)"
        />

        <ValueFile
          v-else-if="metadata.columnType === 'FILE'"
          :metadata="metadata"
          :data="data"
        />

        <template v-else> {{ metadata.columnType }} </template>
      </span>
      <slot />
    </div>
  </td>
</template>

<script setup lang="ts">
import type { IColumn, IRefColumn } from "../../../metadata-utils/src/types";
import type { RefPayload } from "../../types/types";
defineProps<{
  metadata: IColumn;
  data: any;
}>();

defineEmits<{
  (e: "cellClicked", payload: RefPayload): void;
}>();
</script>
