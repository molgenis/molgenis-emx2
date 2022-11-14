<script setup >
import IconButton from "./IconButton.vue";
defineProps({
  cohort: {
    type: Object,
    required: true,
  },
});
</script>

<template>
  <article class="p-12.5">
    <header class="flex items-center">
      <NuxtLink :to="`/catalogue/ssr-catalogue/${cohort.pid}`">
        <h2 class="text-heading-base font-extrabold text-blue-500 min-w-[160px] inline-block mr-4">
          {{ cohort.name }}
        </h2>
      </NuxtLink>

      <span class="text-heading-base grow"> {{ cohort?.institution?.acronym }}</span>

      <div class="flex">
        <!-- <IconButton icon="star" class="text-blue-500" /> -->
        <NuxtLink :to="`/${cohort.pid}`">
          <IconButton icon="arrow-right" class="text-blue-500" />
        </NuxtLink>
      </div>
    </header>

    <p class="text-body-base my-5">
      {{ cohort?.description }}
    </p>

    <dl class="flex flex-col md:flex-row gap-5 md:gap-14 text-body-base">
      <div>
        <dt class="flex-auto block text-gray-600">Keywords</dt>
        <dd>{{ cohort?.keywords }}</dd>
      </div>
      <div>
        <dt class="flex-auto block text-gray-600">Type</dt>
        <dd>{{ cohort?.type?.name }}</dd>
      </div>
      <div>
        <dt class="flex-auto block text-gray-600">Design</dt>
        <dd>{{ cohort?.design?.name }}</dd>
      </div>
    </dl>
  </article>
</template>