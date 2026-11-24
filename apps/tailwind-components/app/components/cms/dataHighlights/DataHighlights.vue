<script setup lang="ts">
import DynamicComponentWidth from "../DynamicComponentWidth.vue";
import type { IDataHighlights } from "../../../../types/cms";
import type { ICmsComponentWidths } from "../../../../types/CmsComponents";

defineProps<IDataHighlights>();
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
      <span class="block text-heading-lg">{{ highlightLabel }}</span>
      <span class="block text-heading-6xl text-center leading-none">{{
        highlightValue
      }}</span>
    </data>
  </DynamicComponentWidth>
</template>
