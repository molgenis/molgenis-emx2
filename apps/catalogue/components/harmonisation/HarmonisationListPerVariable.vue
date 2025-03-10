<script setup lang="ts">
import type { IMapping } from "~/interfaces/types";

defineProps<{
  mappings: IMapping[] | undefined;
}>();
</script>
<template>
  <ul class="flex flex-col flex-wrap min-h-32 max-h-96 overflow-auto">
    <li
      v-for="mapping in mappings
        ?.filter((m) => m.match && m.match.name != 'unmapped')
        .sort((a, b) =>
          a.source.id.toLowerCase().localeCompare(b.source.id.toLowerCase())
        )"
      class="pb-2"
    >
      <div class="flex items-center gap-2">
        <HarmonisationStatusIcon :status="mapping.match.name" size="large" />
        <span>{{ mapping.source.id }}</span>
      </div>
    </li>
  </ul>

  <HarmonisationLegendDetailed class="mt-[50px]" />
</template>
