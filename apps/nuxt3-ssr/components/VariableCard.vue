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
    <header class="flex md:flex-row gap-3 items-start md:items-center">
      <div class="md:basis-1/5 p-2">
        <h2 class="break-all">
          <NuxtLink
            :to="`/${schema}/ssr-catalogue/${catalogue}/variables/${resourcePathId}`"
            class="text-body-base font-extrabold text-blue-500 hover:underline hover:bg-blue-50"
          >
            {{ variable?.name }}
          </NuxtLink>
        </h2>
      </div>
      <div class="hidden md:flex md:basis-3/5">
        <p class="text-body-base">
          {{ variable?.label }}
        </p>
      </div>
      <div class="hidden basis-1/5 xl:flex xl:justify-end">
        <NuxtLink
          :to="`/${schema}/ssr-catalogue/${catalogue}/variables/${resourcePathId}`"
        >
          <BaseIcon
            icon="arrow-right"
            class="text-blue-500"
          />
        </NuxtLink>
      </div>
    </header>
  </article>
</template>
