<script setup lang="ts">
import { computed } from "vue";
import type { IColumn } from "../../../metadata-utils/src/types";

const props = defineProps<{
  metaData: IColumn;
  data: string[] | number[] | Record<string, any>;
}>();

const elementType = computed(() => props.metaData.columnType.split("_")[0]);
</script>

<template>
  <template v-for="(listElement, index) in data">
    <ValueString
      v-if="elementType === 'STRING'"
      :metaData="metaData"
      :data="listElement as string"
    />
    <ValueString
      v-if="elementType === 'TEXT'"
      :metaData="metaData"
      :data="listElement as string"
    />
    <ValueDecimal
      v-else-if="elementType === 'DECIMAL'"
      :metaData="metaData"
      :data="listElement as number"
    />
    <ValueLong
      v-else-if="elementType === 'LONG'"
      :metaData="metaData"
      :data="listElement as number"
    />
    <ValueLong
      v-else-if="elementType === 'INT'"
      :metaData="metaData"
      :data="listElement as number"
    />
    <ValueBool
      v-else-if="elementType === 'BOOL'"
      :metaData="metaData"
      :data="listElement as boolean"
    />
    <ValueEmail
      v-else-if="elementType === 'EMAIL'"
      :metaData="metaData"
      :data="listElement as string"
    />
    <ValueHyperlink
      v-else-if="elementType === 'HYPERLINK'"
      :metaData="metaData"
      :data="listElement as string"
    />
    <ValueObject
      v-else-if="elementType === 'REF'"
      :metaData="metaData"
      :data="listElement"
    />
    <ValueObject
      v-else-if="elementType === 'ONTOLOGY'"
      :metaData="metaData"
      :data="listElement"
    />

    <span v-else>{{ elementType }}</span>
    <span v-if="Number(data.length) - 1 !== Number(index)">,&nbsp;</span>
  </template>
</template>
