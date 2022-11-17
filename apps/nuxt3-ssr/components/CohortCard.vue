<script setup>
import { computed } from "vue";

const props = defineProps({
  cohort: {
    type: Object,
    required: true,
  },
  schema: {
    type: String,
    required: true,
  },
  compact: {
    type: Boolean,
    default: 0,
  },
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
            :to="`/${schema}/ssr-catalogue/${cohort.pid}`"
            class="
              text-body-base
              font-extrabold
              text-blue-500
              hover:underline hover:bg-blue-50
            "
          >
            {{ cohort.name }}
          </NuxtLink>
        </h2>

        <span :class="subtitleClasses" class="text-body-base">
          {{ cohort?.institution?.acronym }}
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
        <NuxtLink :to="`/${schema}/ssr-catalogue/${cohort.pid}`">
          <IconButton
            icon="arrow-right"
            class="text-blue-500 hidden xl:flex xl:justify-end"
          />
        </NuxtLink>
      </div>
    </header>

    <div v-if="!compact">
      <p class="text-body-base my-5 xl:block hidden">
        {{ cohort?.description }}
      </p>

      <p class="text-body-base mt-5 block xl:hidden">
        The European Human Exposome Network (EHEN) is the world’s largest
        network of projects studying the impact of environmental exposures
        across a lifetime - the exposome - on human health. Collectively, the
        EHEN projects are working in 24 countries acros...
      </p>

      <a
        class="text-blue-500 hover:underline hover:bg-blue-50 mb-5 xl:hidden"
        href="#"
      >
        Read more
      </a>

      <dl class="hidden xl:flex gap-5 xl:gap-14 text-body-base">
        <div>
          <dt class="flex-auto block text-gray-600">Keywords</dt>
          <dd>{{ cohort?.keywords }}</dd>
        </div>
        <div>
          <dt class="flex-auto block text-gray-600">Type</dt>
          <dd>{{ cohort?.type?.map((type) => type.name).join(",") }}</dd>
        </div>
        <div>
          <dt class="flex-auto block text-gray-600">Design</dt>
          <dd>{{ cohort?.design?.name }}</dd>
        </div>
      </dl>
    </div>
  </article>
</template>
