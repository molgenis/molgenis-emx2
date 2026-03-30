<script setup lang="ts">
import { computed } from "vue";
import { NuxtLink } from "#components";
import type { IColumn } from "../../../../metadata-utils/src/types";
import ValueEMX2 from "../value/EMX2.vue";
import {
  getDetailColumns,
  getDescriptionColumn,
  getLogoColumn,
} from "../../utils/displayUtils";

const props = defineProps<{
  title: string;
  data: Record<string, any>;
  columns?: IColumn[];
  href?: string;
}>();

const descriptionColumn = computed(() =>
  getDescriptionColumn(props.columns ?? [], props.data)
);

const detailColumns = computed(() =>
  getDetailColumns(props.columns ?? [], props.data)
);

const logoColumn = computed(() =>
  getLogoColumn(props.columns ?? [], props.data)
);
</script>

<template>
  <div class="border border-gray-200 dark:border-gray-700 rounded-lg p-4">
    <div class="flex gap-4">
      <img
        v-if="logoColumn && data[logoColumn.id]?.url"
        :src="data[logoColumn.id].url"
        :alt="title"
        class="w-12 h-12 object-contain rounded"
      />
      <div class="flex-1 min-w-0">
        <h3 class="text-record-heading font-semibold">
          <NuxtLink v-if="href" :to="href" class="text-link hover:underline">
            {{ title }}
          </NuxtLink>
          <span v-else>{{ title }}</span>
        </h3>
        <p
          v-if="descriptionColumn"
          class="text-record-value text-sm mt-1 line-clamp-2"
        >
          {{ data[descriptionColumn.id] }}
        </p>
      </div>
    </div>
    <dl v-if="detailColumns.length" class="grid gap-1 mt-2">
      <template v-for="col in detailColumns" :key="col.id">
        <div class="flex gap-2">
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
