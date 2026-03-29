<script setup lang="ts">
import { NuxtLink } from "#components";
import type { IColumn } from "../../../../metadata-utils/src/types";
import ValueEMX2 from "../value/EMX2.vue";

defineProps<{
  title: string;
  data: Record<string, any>;
  columns?: IColumn[];
  href?: string;
}>();
</script>

<template>
  <div class="border border-gray-200 dark:border-gray-700 rounded-lg p-4">
    <h3 class="text-record-heading font-semibold mb-2">
      <NuxtLink v-if="href" :to="href" class="text-link hover:underline">
        {{ title }}
      </NuxtLink>
      <span v-else>{{ title }}</span>
    </h3>
    <dl v-if="columns?.length" class="grid gap-1">
      <template v-for="col in columns" :key="col.id">
        <div v-if="data[col.id] != null" class="flex gap-2">
          <dt class="text-record-label text-sm min-w-[80px]">
            {{ col.label || col.id }}
          </dt>
          <dd class="text-record-value text-sm">
            <ValueEMX2 :metadata="col" :data="data[col.id]" />
          </dd>
        </div>
      </template>
    </dl>
  </div>
</template>
