<script setup lang="ts">
import { useRoute } from "#app";
import { useDatasetStore } from "#imports";
import { computed } from "vue";
import IconButton from "../../../tailwind-components/app/components/button/IconButton.vue";
import ContentReadMore from "../../../tailwind-components/app/components/ContentReadMore.vue";
import type { IResources } from "../../interfaces/catalogue";
import dateUtils from "../utils/dateUtils";
import CardButton from "./store/CartButton.vue";

const datasetStore = useDatasetStore();

const CUTOFF = 250;

const route = useRoute();

const props = withDefaults(
  defineProps<{
    resource: IResources;
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
</script>

<template>
  <article :class="articleClasses">
    <header :class="headerClasses" class="flex">
      <div :class="titleContainerClasses" class="grow">
        <h2 class="min-w-[160px] mr-4 md:inline-block block">
          <NuxtLink
            :to="`/${catalogue}/${route.params.resourceType}/${resource.id}`"
            class="text-body-base font-extrabold text-link hover:underline hover:bg-link-hover"
            data-track-category="CTA"
            data-track-action="navigate"
            data-track-name="resource-detail"
          >
            {{ resource.acronym || resource.name }}
          </NuxtLink>
        </h2>

        <span :class="subtitleClasses" class="mr-4 text-body-base">
          {{ resource.acronym ? resource.name : "" }}
        </span>
      </div>
      <div class="flex">
        <CardButton
          v-if="datasetStore.isEnabled"
          :resource="resource"
          :compact="props.compact"
        />
        <NuxtLink :to="`/${catalogue}/resources/${resource.id}`">
          <IconButton
            icon="arrow-right"
            class="text-link hidden xl:flex xl:justify-end"
            data-track-category="CTA"
            data-track-action="navigate"
            data-track-name="resource-detail"
          />
        </NuxtLink>
      </div>
    </header>

    <div v-if="!compact">
      <ContentReadMore :text="resource.description" :cutoff="CUTOFF" />

      <dl class="hidden xl:flex gap-5 xl:gap-14 text-body-base">
        <div>
          <dt class="flex-auto block text-gray-600">Type</dt>
          <dd>{{ resource.type?.map((type) => type.name).join(",") }}</dd>
        </div>
        <div>
          <dt class="flex-auto block text-gray-600">Design</dt>
          <dd>{{ resource.design?.name }}</dd>
        </div>
        <div>
          <dt class="flex-auto block text-gray-600">Participants</dt>
          <dd>{{ resource.numberOfParticipants }}</dd>
        </div>
        <div>
          <dt class="flex-auto block text-gray-600">Duration</dt>
          <dd>
            {{ startEndYear(resource.startYear, resource.endYear) }}
          </dd>
        </div>
      </dl>
    </div>
  </article>
</template>
