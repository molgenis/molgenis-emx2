<script setup lang="ts">
let truncate = ref(true);
const cutoff = 250;

const props = withDefaults(
  defineProps<{
    resource: any;
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
    buildValueKey(props.resourceId) +
    "?keys=" +
    JSON.stringify(props.resourceId)
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
    <header :class="headerClasses" class="flex">
      <div :class="titleContainerClasses" class="grow">
        <h2 class="min-w-[160px] mr-4 md:inline-block block">
          <NuxtLink
            :to="`${resourceName}/${resourceIdPath}`"
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
        <NuxtLink :to="`${resourceName}/${resourceIdPath}`">
          <IconButton
            icon="arrow-right"
            class="text-blue-500 hidden xl:flex xl:justify-end"
          />
        </NuxtLink>
      </div>
    </header>

    <div v-if="!compact">
      <template v-if="resource.description">
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
      </template>

      <!-- TODO think about generic way to add additional context -->
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
      <template
        v-if="!resource.acronym && !resource.name && !resource.description"
      >
        <div>{{ resource }}</div>
        <hr class="m-3" />
        <div>{{ resourceId }}</div>
      </template>
    </div>
  </article>
</template>
