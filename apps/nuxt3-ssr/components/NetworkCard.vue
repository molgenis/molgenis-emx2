<script setup lang="ts">
import { computed } from "vue";

const mobileShowMoreText = ref(false);
const mobileShowMoreTextLength = 250;

const props = withDefaults(
  defineProps<{
    network: INetwork;
    schema: string;
    compact?: boolean;
  }>(),
  {
    compact: false,
  }
);

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

const isShowingMobileMoreText = computed(() => {
  return (
    mobileShowMoreText.value ||
    (props.network?.description &&
      props.network?.description?.length < mobileShowMoreTextLength)
  );
});
</script>

<template>
  <article :class="articleClasses">
    <div class="grid grid-cols-12 gap-6">
      <div class="col-span-3">
        <div class="items-center flex h-full w-full justify-center">
          <NuxtLink :to="`/${schema}/ssr-catalogue/${network.id}`">
            <img :src="network?.logo?.url" />
          </NuxtLink>
        </div>
      </div>
      <div class="col-span-9">
        <header :class="headerClasses" class="flex">
          <div :class="titleContainerClasses" class="grow">
            <h2 class="min-w-[160px] mr-4 md:inline-block block">
              <NuxtLink
                :to="`/${schema}/ssr-catalogue/${network.id}`"
                class="text-body-base font-extrabold text-blue-500 hover:underline hover:bg-blue-50"
              >
                {{ network?.acronym || network?.name }}
              </NuxtLink>
            </h2>

            <span :class="subtitleClasses" class="mr-4 text-body-base">
              {{ network?.acronym ? network?.name : "" }}
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
            <NuxtLink :to="`/${schema}/ssr-catalogue/${network.id}`">
              <IconButton
                icon="arrow-right"
                class="text-blue-500 hidden xl:flex xl:justify-end"
              />
            </NuxtLink>
          </div>
        </header>

        <div v-if="!compact">
          <Truncate :value="network.description" />
        </div>
      </div>
    </div>
  </article>
</template>
