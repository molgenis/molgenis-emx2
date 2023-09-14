<script setup lang="ts">
import type { IResource } from "~/interfaces/types";
let truncate = ref(true);
const cutoff = 250;

const props = withDefaults(
  defineProps<{
    resource: IResource;
    schema: string;
    resourceName: string;
    compact?: boolean;
    resourceId: Record<string, string>;
  }>(),
  {
    compact: false,
  }
);

const resourceIdPath = computed(() => {
  return (
    Object.values(props.resourceId)[0] +
    "?keys=" +
    new URLSearchParams(props.resourceId).toString()
  );
});

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
    <div class="grid grid-cols-12 gap-6">
      <div :class="{ 'col-span-3': !compact }">
        <div
          v-if="resource?.logo"
          class="items-center flex h-full w-full justify-center"
        >
          <NuxtLink
            :to="`/${schema}/ssr-catalogue/${resourceName}/${resource.id}`"
          >
            <img :src="resource?.logo?.url" />
          </NuxtLink>
        </div>
      </div>
      <div class="col-span-12" :class="{ 'col-span-9': !compact }">
        <header :class="headerClasses" class="flex">
          <div :class="titleContainerClasses" class="grow">
            <h2 class="min-w-[160px] mr-4 md:inline-block block">
              <NuxtLink
                :to="`/${schema}/ssr-catalogue/${resourceName}/${resourceIdPath}`"
                class="text-body-base font-extrabold text-blue-500 hover:underline hover:bg-blue-50"
              >
                {{ resource?.acronym || resource?.name }}
              </NuxtLink>
            </h2>

            <span :class="subtitleClasses" class="mr-4 text-body-base">
              {{ resource?.acronym ? resource?.name : "" }}
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
            <NuxtLink :to="`/${schema}/ssr-catalogue/resources/${resource.id}`">
              <IconButton
                icon="arrow-right"
                class="text-blue-500 hidden xl:flex xl:justify-end"
              />
            </NuxtLink>
          </div>
        </header>
      </div>

      <div v-if="!compact">
        <p class="text-body-base my-5 xl:block hidden">
          {{ resource?.description }}
        </p>

        <p
          v-if="resource?.description"
          class="text-body-base mt-5 block xl:hidden"
        >
          {{
            truncate
              ? `${resource?.description?.substring(0, cutoff)}...`
              : resource?.description
          }}
        </p>

        <button
          v-if="truncate && resource?.description?.length > cutoff"
          class="text-blue-500 hover:underline hover:bg-blue-50 mt-5 xl:hidden"
          @click="truncate = false"
        >
          Read more
        </button>

        <!-- <dl class="hidden xl:flex gap-5 xl:gap-14 text-body-base">
        <div>
          <dt class="flex-auto block text-gray-600">Type</dt>
          <dd>{{ resource?.type?.map((type) => type.name).join(",") }}</dd>
        </div>
        <div>
          <dt class="flex-auto block text-gray-600">Design</dt>
          <dd>{{ resource?.design?.name }}</dd>
        </div>
        <div>
          <dt class="flex-auto block text-gray-600">Participants</dt>
          <dd>{{ resource?.numberOfParticipants }}</dd>
        </div>
        <div>
          <dt class="flex-auto block text-gray-600">Duration</dt>
          <dd>
            {{ startEndYear(resource?.startYear, resource?.endYear) }}
          </dd>
        </div>
      </dl> -->
      </div>
    </div>
  </article>
</template>
