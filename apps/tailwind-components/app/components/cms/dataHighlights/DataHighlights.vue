<script setup lang="ts">
import { ref, computed } from "vue";
import DynamicComponentWidth from "../DynamicComponentWidth.vue";
import type { IDataHighlights } from "../../../../types/cms";
import type { ICmsComponentWidths } from "../../../../types/CmsComponents";

const props = defineProps<IDataHighlights>();

const value = ref<number>(0);
  
const query = computed<string|undefined>(() => {
  if (props.retrieveValueFromTable) {
    const table = props.highlightValueTable;
    let body = ""
    
    if (props.highlightValueColumn) {
      body += props.highlightValueColumn;
      body += "\n" 
    }
    
    body += props.highlightValueAggregationType?.name;
    
    return `query getAggregation($filter: ${table}Filter) {
      ${table}_agg(filter: $filter) {
        ${body}
      }
    }`
  };
})

</script>

<template>
  {{ query }}
  <DynamicComponentWidth
    :id="id"
    :width="(highlightWidth?.name as ICmsComponentWidths)"
  >
    <data
      :value="highlightValue"
      class="block py-2.5 px-5 text-title font-display items-center border border-theme rounded-base"
    >
      <span class="block text-heading-lg truncate">{{ highlightLabel }}</span>
      <span
        class="block text-heading-6xl text-center leading-none py-1 truncate"
      >
        {{ highlightValue }}
      </span>
    </data>
  </DynamicComponentWidth>
</template>
