<script setup lang="ts">
import type { IVariable } from "~/interfaces/types";
import { getKey } from "~/utils/variableUtils";

const props = withDefaults(
  defineProps<{
    variable: IVariable;
    schema: string;
    catalogue: string;
  }>(),
  { catalogue: "all" }
);

const variableKey = computed(() => getKey(props.variable));

const resourcePathId = resourceIdPath(variableKey.value);
</script>

<template>
  <article class="py-5 lg:px-12.5 p-5">
    <header class="flex">
      <div class="grow flex items-center">
        <h2 class="min-w-[160px] mr-4 md:inline-block block">
          <NuxtLink
            :to="`/${schema}/ssr-catalogue/${catalogue}/variables/${resourcePathId}`"
            class="text-body-base font-extrabold text-blue-500 hover:underline hover:bg-blue-50"
          >
            {{ variable?.name }}
          </NuxtLink>
        </h2>

        <span class="mr-4 text-body-base hidden md:block">
          {{ variable?.label }}
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
          :to="`/${schema}/ssr-catalogue/${catalogue}/variables/${resourcePathId}`"
        >
          <IconButton
            icon="arrow-right"
            class="text-blue-500 hidden xl:flex xl:justify-end"
          />
        </NuxtLink>
      </div>
    </header>
  </article>
</template>
