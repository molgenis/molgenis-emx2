<script setup lang="ts">
import { computed } from "vue";
import { computedAsync } from "@vueuse/core";
import { useLazyFetch } from "#app";
import DynamicComponentWidth from "../DynamicComponentWidth.vue";
import { buildAggregationQuery } from "../../../utils/cms/buildAggregationQuery.ts";
import type { IDataHighlights } from "../../../../types/cms";
import type { ICmsComponentWidths } from "../../../../types/CmsComponents";

const props = withDefaults(defineProps<IDataHighlights>(), {
  highlightValue: 0,
});

const url = computed<string>(() => {
  return props.highlightValueSchema
    ? `/${props.highlightValueSchema}/graphql`
    : "/graphql";
});

const internalHighlightValue = computedAsync(async () => {
  if (props.retrieveValueFromTable) {
    const aggMethod = props.highlightValueAggregationType?.name as string;

    const queryInfo = buildAggregationQuery({
      table: props.highlightValueTable as string,
      column: props.highlightValueColumn,
      method: aggMethod,
    });

    let filter = {};
    try {
      if (props.highlightValueFilter) {
        console.log(props.highlightValueFilter);
        filter = JSON.parse(props.highlightValueFilter);
      }

      const { data } = useLazyFetch(url.value, {
        method: "POST",
        body: {
          query: queryInfo.query,
          variables: { filter: filter },
        },
      });

      if (data.value) {
        const result = data.value as Record<string, any>;
        const tableData = result.data[queryInfo.gqlTableName];
        if (queryInfo.gqlBodyIsNested) {
          return tableData[aggMethod][props.highlightValueColumn as string];
        } else {
          return tableData[aggMethod];
        }
      }
    } catch (error) {
      return "N/A";
    }
  } else {
    return props.highlightValue;
  }
});
</script>

<template>
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
        {{ internalHighlightValue }}
      </span>
    </data>
  </DynamicComponentWidth>
</template>
