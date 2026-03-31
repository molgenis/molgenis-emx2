<script setup lang="ts">
import { computed } from "vue";
import { NuxtLink } from "#components";
import type { IColumn } from "../../../../metadata-utils/src/types";
import ValueEMX2 from "../value/EMX2.vue";
import {
  getDetailColumns,
  getDescriptionColumn,
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
</script>

<template>
  <li class="border lg:even:border-l-0 p-11 relative -mb-[1px]">
    <div class="flex items-start flex-col h-full">
      <span class="block">
        <NuxtLink
          v-if="href"
          :to="href"
          class="font-bold text-link hover:underline"
        >
          {{ title }}
        </NuxtLink>
        <span v-else class="font-bold">{{ title }}</span>
      </span>
      <p v-if="descriptionColumn" class="mt-1 line-clamp-2">
        {{ data[descriptionColumn.id] }}
      </p>
      <dl v-if="detailColumns.length" class="mt-3 grid gap-1">
        <div v-for="col in detailColumns" :key="col.id" class="flex gap-2">
          <dt class="text-sm font-bold capitalize">
            {{ col.label || col.id }}
          </dt>
          <dd class="text-sm">
            <ValueEMX2 :metadata="col" :data="data[col.id]" />
          </dd>
        </div>
      </dl>
    </div>
  </li>
</template>
