<script setup lang="ts">
import type { HarmonisationStatus, IMapping } from "~/interfaces/types";

const props = defineProps<{
  mappings: IMapping[] | undefined;
}>();
</script>
<template>
  <div class="grid grid-cols-3 gap-4">
    <div>
      <ul>
        <li
          v-for="mapping in mappings?.filter(
            (m) => m.match && m.match.name != 'na'
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
