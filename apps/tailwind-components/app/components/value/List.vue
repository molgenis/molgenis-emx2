<script setup lang="ts">
import { computed } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import ValueString from "./String.vue";
import ValueDecimal from "./Decimal.vue";
import ValueLong from "./Long.vue";
import ValueBool from "./Bool.vue";
import ValueEmail from "./Email.vue";
import ValueHyperlink from "./Hyperlink.vue";
import ValueObject from "./Object.vue";
import ValueDate from "./Date.vue";
import ValueDateTime from "./DateTime.vue";

const props = withDefaults(
  defineProps<{
    metadata: IColumn;
    data: string[] | number[] | Record<string, any>;
    hideListSeparator?: boolean;
  }>(),
  {
    hideListSeparator: false,
  }
);

const elementType = computed(() => props.metadata.columnType.split("_")[0]);
</script>

<template>
  <template v-for="(listElement, index) in data">
    <ValueString
      v-if="
        elementType === 'STRING' ||
        elementType === 'AUTO_ID' ||
        elementType === 'PERIOD'
      "
      :metadata="metadata"
      :data="listElement as string"
    />
    <ValueString
      v-else-if="elementType === 'TEXT'"
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
    <ValueInt
      v-else-if="elementType === 'INT' || elementType === 'NON_NEGATIVE_INT'"
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
      v-else-if="
        elementType === 'REF' ||
        elementType === 'MULTISELECT' ||
        elementType === 'CHECKBOX'
      "
      :metadata="metadata"
      :data="listElement"
    />
    <ValueObject
      v-else-if="elementType === 'ONTOLOGY'"
      :metadata="metadata"
      :data="listElement"
    />
    <ValueDate
      v-else-if="elementType === 'DATE'"
      :metadata="metadata"
      :data="listElement"
    />
    <ValueDateTime
      v-else-if="elementType === 'DATETIME'"
      :metadata="metadata"
      :data="listElement"
    />
    <span v-else>{{ elementType }}</span>
    <span
      v-if="Number(data.length) - 1 !== Number(index) && !hideListSeparator"
    >
      ,&nbsp;
    </span>
  </template>
</template>
