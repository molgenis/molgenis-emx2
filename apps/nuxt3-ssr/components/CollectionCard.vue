<script setup lang="ts">
import dateUtils from "~/utils/dateUtils";
import type { ICollection } from "~/interfaces/types";
const cutoff = 250;

const props = withDefaults(
  defineProps<{
    collection: ICollection;
    schema: string;
    compact?: boolean;
    catalogue?: string;
  }>(),
  {
    compact: false,
  }
);

const startEndYear = dateUtils.startEndYear;

const articleClasses = computed(() => {
  return props.compact ? "py-5 lg:px-12.5 p-5" : "lg:px-12.5 py-12.5 px-5";
});

const subtitleClasses = computed(() => {
  return props.compact ? "hidden md:block" : "mt-1.5 block md:inline";
});

const titleContainerClasses = computed(() => {
  return props.compact ? "flex items-center" : "";
});

const headerClasses = computed(() => {
  return props.compact ? "" : "items-start xl:items-center";
});

const iconStarClasses = computed(() => {
  return props.compact ? "" : "items-baseline xl:items-center mt-0.5 xl:mt-0";
});
</script>

<template>
  <article :class="articleClasses">
    <header :class="headerClasses" class="flex">
      <div :class="titleContainerClasses" class="grow">
        <h2 class="min-w-[160px] mr-4 md:inline-block block">
          <NuxtLink
            :to="`/${schema}/ssr-catalogue/${catalogue}/collections/${collection.id}`"
            class="text-body-base font-extrabold text-blue-500 hover:underline hover:bg-blue-50"
          >
            {{ collection?.acronym || collection?.name }}
          </NuxtLink>
        </h2>

        <span :class="subtitleClasses" class="mr-4 text-body-base">
          {{ collection?.acronym ? collection?.name : "" }}
        </span>
      </div>
      <div class="flex">
        <!--
        <IconButton
          icon="star"
          :class="iconStarClasses"
          class="text-blue-500 xl:justify-end"
        />
        -->
        <NuxtLink
          :to="`/${schema}/ssr-catalogue/${catalogue}/collections/${collection.id}`"
        >
          <IconButton
            icon="arrow-right"
            class="text-blue-500 hidden xl:flex xl:justify-end"
          />
        </NuxtLink>
      </div>
    </header>

    <div v-if="!compact">
      <ContentReadMore :text="collection.description" :cutoff="cutoff" />

      <dl class="hidden xl:flex gap-5 xl:gap-14 text-body-base">
        <div>
          <dt class="flex-auto block text-gray-600">Type</dt>
          <dd>{{ collection?.type?.map((type) => type.name).join(",") }}</dd>
        </div>
        <div>
          <dt class="flex-auto block text-gray-600">Design</dt>
          <dd>{{ collection?.design?.name }}</dd>
        </div>
        <div>
          <dt class="flex-auto block text-gray-600">Participants</dt>
          <dd>{{ collection?.numberOfParticipants }}</dd>
        </div>
        <div>
          <dt class="flex-auto block text-gray-600">Duration</dt>
          <dd>
            {{ startEndYear(collection?.startYear, collection?.endYear) }}
          </dd>
        </div>
      </dl>
    </div>
  </article>
</template>
