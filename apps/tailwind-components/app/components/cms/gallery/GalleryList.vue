<script setup lang="ts">
import type { IContainers } from "../../../../types/cms";

const props = defineProps<{
  pageType: "Configurable pages" | "Developer pages";
  containers: IContainers[];
  schema: string;
}>();

const pageTypeId: string = props.pageType.replaceAll(" ", "-").toLowerCase();

function setNuxtLink(page: string): string {
  if (props.pageType === "Developer pages") {
    return `/${props.schema}/pages/${page}/editor`;
  }
  return `/${props.schema}/pages/${page}/configure`;
}
</script>

<template>
  <div :id="pageTypeId">
    <h2
      :id="`${pageTypeId}-title`"
      class="font-display text-title uppercase text-heading-4xl mt-7.5 mb-2.5"
    >
      {{ pageType }}
    </h2>
    <nav :aria-labelledby="`${pageTypeId}-title`">
      <ul
        class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 flew-wrap justify-start items-center gap-7.5"
      >
        <li
          v-for="container in containers"
          class="relative group border rounded-base w-full h-48 p-7.5 hover:shadow-md transition-shadow flex justify-center items-center bg-form-legend"
        >
          <div
            class="absolute top-2.5 right-2.5 p-[5px] h-10 w-10 flex justify-center items-center border border-transparent rounded-full text-button-text hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
            v-tooltip.bottom="`Edit`"
          >
            <NuxtLink
              :to="setNuxtLink(container.name)"
              class="font-display tracking-widest uppercase text-heading-lg hover:underline cursor-pointer"
            >
              <BaseIcon name="Edit" :width="18" />
              <span class="sr-only">edit page</span>
            </NuxtLink>
          </div>
          <NuxtLink
            :to="`/${schema}/pages/${container.name}/`"
            class="text-button-text hover:underline"
          >
            {{ container.name }}
          </NuxtLink>
        </li>
      </ul>
    </nav>
  </div>
</template>
