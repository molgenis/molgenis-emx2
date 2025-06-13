<script setup lang="ts">
import { computed } from "vue";
import type { IColumn } from "../../../metadata-utils/src/types";

const props = defineProps<{
  metadata: IColumn;
  data: string[] | number[] | Record<string, any>;
}>();

const elementType = computed(() => props.metadata.columnType.split("_")[0]);
</script>

<template>
  <template v-for="(listElement, index) in data">
    <ValueString
      v-if="elementType === 'STRING'"
      :metadata="metadata"
      :data="listElement as string"
    />
    <ValueString
      v-if="elementType === 'TEXT'"
      :metadata="metadata"
      :data="listElement as string"
    />
    <ValueDecimal
      v-else-if="elementType === 'DECIMAL'"
      :metadata="metadata"
      :data="listElement as number"
    />
    <ValueLong
      v-else-if="elementType === 'LONG'"
      :metadata="metadata"
      :data="listElement as number"
    />
    <ValueLong
      v-else-if="elementType === 'INT'"
      :metadata="metadata"
      :data="listElement as number"
    />
    <ValueBool
      v-else-if="elementType === 'BOOL'"
      :metadata="metadata"
      :data="listElement as boolean"
    />
    <ValueEmail
      v-else-if="elementType === 'EMAIL'"
      :metadata="metadata"
      :data="listElement as string"
    />
    <ValueHyperlink
      v-else-if="elementType === 'HYPERLINK'"
      :metadata="metadata"
      :data="listElement as string"
    />
    <ValueObject
      v-else-if="elementType === 'REF'"
      :metadata="metadata"
      :data="listElement"
    />
    <ValueObject
      v-else-if="elementType === 'ONTOLOGY'"
      :metadata="metadata"
      :data="listElement"
    />

    <span v-else>{{ elementType }}</span>
    <span v-if="Number(data.length) - 1 !== Number(index)">,&nbsp;</span>
  </template>
</template>
