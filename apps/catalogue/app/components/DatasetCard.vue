<script setup lang="ts">
import type { IDatasets } from "../../interfaces/catalogue";
import { computed } from "vue";
import IconButton from "../../../tailwind-components/app/components/button/IconButton.vue";
import ContentReadMore from "../../../tailwind-components/app/components/ContentReadMore.vue";

const cutoff = 250;

const props = withDefaults(
  defineProps<{
    dataset?: IDatasets;
    data?: IDatasets; // from displayConfig.component
    schema?: string;
    compact?: boolean;
    catalogue?: string;
  }>(),
  {
    compact: false,
  }
);

// Support both data prop (from Emx2ListView) and dataset prop (backwards compat)
const item = computed(() => props.data || props.dataset);

const articleClasses = computed(() => {
  return props.compact ? "py-5 lg:px-12.5 p-5" : "lg:px-12.5 py-12.5 px-5";
});

const detailLink = computed(() => {
  const resourceId = item.value?.resource?.id;
  const name = item.value?.name;
  return `/${props.catalogue}/datasets/${resourceId}/${name}`;
});

const datasetTypes = computed(() => {
  if (!item.value?.datasetType?.length) return "";
  return item.value.datasetType.map((t) => t.name).join(", ");
});
</script>

<template>
  <article v-if="item" :class="articleClasses">
    <header class="flex items-start xl:items-center">
      <div class="grow">
        <h2 class="min-w-[160px] mr-4 md:inline-block block">
          <NuxtLink
            :to="detailLink"
            class="text-body-base font-extrabold text-link hover:underline hover:bg-link-hover"
          >
            {{ item.label || item.name }}
          </NuxtLink>
        </h2>
        <span
          v-if="item.label"
          class="mt-1.5 block md:inline mr-4 text-body-base text-gray-600"
        >
          {{ item.name }}
        </span>
        <span class="mt-1.5 block md:inline text-body-sm text-gray-500">
          {{ item.resource?.name || item.resource?.id }}
        </span>
      </div>
      <div class="flex">
        <NuxtLink :to="detailLink">
          <IconButton
            icon="arrow-right"
            class="text-link hidden xl:flex xl:justify-end"
          />
        </NuxtLink>
      </div>
    </header>

    <div v-if="!compact">
      <ContentReadMore :text="item.description" :cutoff="cutoff" />

      <dl class="hidden xl:flex gap-5 xl:gap-14 text-body-base mt-3">
        <div v-if="datasetTypes">
          <dt class="flex-auto block text-gray-600">Type</dt>
          <dd>{{ datasetTypes }}</dd>
        </div>
        <div v-if="item.unitOfObservation">
          <dt class="flex-auto block text-gray-600">Unit</dt>
          <dd>{{ item.unitOfObservation.name }}</dd>
        </div>
        <div v-if="item.numberOfRows">
          <dt class="flex-auto block text-gray-600">Rows</dt>
          <dd>
            {{ new Intl.NumberFormat("en-GB").format(item.numberOfRows) }}
          </dd>
        </div>
      </dl>
    </div>
  </article>
</template>
