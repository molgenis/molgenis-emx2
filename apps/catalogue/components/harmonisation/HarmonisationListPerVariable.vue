<script setup lang="ts">
import type { IMapping } from "~/interfaces/types";

defineProps<{
  mappings: IMapping[] | undefined;
}>();
</script>
<template>
  <div class="grid grid-cols-3 gap-4">
    <div>
      <ul>
        <li
          v-for="mapping in mappings
            ?.filter((m) => m.match && m.match.name != 'unmapped')
            .sort((a, b) =>
              a.source.id.toLowerCase().localeCompare(b.source.id.toLowerCase())
            )"
          class="pb-2"
        >
          <div class="flex items-center gap-2">
            <HarmonisationStatusIcon
              :status="mapping.match.name"
              size="large"
            />
            <span>{{ mapping.source.id }}</span>
          </div>
        </li>
      </ul>
    </div>
  </div>
  <HarmonisationLegendDatailed class="mt-12" />
</template>
