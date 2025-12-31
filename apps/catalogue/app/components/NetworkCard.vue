<script setup lang="ts">
const route = useRoute();
import type { INetwork } from "../../interfaces/types";
import { computed } from "vue";
import { useRoute } from "#app/composables/router";
import IconButton from "../../../tailwind-components/app/components/button/IconButton.vue";
import ContentReadMore from "../../../tailwind-components/app/components/ContentReadMore.vue";
import BaseIcon from "../../../tailwind-components/app/components/BaseIcon.vue";

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

const catalogue = route.params.catalogue || "all";

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

interface Link {
  url: string;
  title: string;
}

const links: Link[] = [];
</script>

<template>
  <article :class="articleClasses">
    <div class="grid grid-cols-12 gap-6">
      <div :class="[compact ? 'col-span-1' : 'col-span-3']">
        <div
          class="items-center flex justify-center"
          :class="[compact ? 'w-50px h-50px' : 'h-full w-full']"
        >
          <NuxtLink :to="`/${catalogue}/networks/${network.id}`">
            <img :src="network?.logo?.url" />
          </NuxtLink>
        </div>
      </div>
      <div :class="[compact ? 'col-span-11 flex ' : 'col-span-9']">
        <header :class="headerClasses" class="flex grow">
          <div :class="titleContainerClasses" class="">
            <h2 class="min-w-[160px] mr-4 md:inline-block block">
              <NuxtLink
                :to="`/${catalogue}/networks/${network.id}`"
                class="text-body-base font-extrabold text-link hover:underline hover:bg-link-hover"
              >
                {{ network?.acronym || network?.name }}
              </NuxtLink>
            </h2>
          </div>
          <div class="flex justify-end items-center grow">
            {{ network.name }}
            <!--
        <IconButton
          icon="star"
          :class="iconStarClasses"
          class="text-link xl:justify-end"
        />
        -->
            <NuxtLink
              v-if="!compact"
              :to="`/${catalogue}/networks/${network.id}`"
            >
              <IconButton
                icon="arrow-right"
                class="text-link hidden xl:flex xl:justify-end"
              />
            </NuxtLink>

            <a
              v-else
              v-for="(link, index) in links"
              v-bind:key="index"
              :href="link.url"
              class="text-link hover:underline hover:bg-link-hover mr-7.5 hidden sm:inline-block"
            >
              <BaseIcon name="caret-right" class="inline w-5 h-5 -ml-1.5" />{{
                link.title
              }}
            </a>
          </div>
        </header>

        <div v-if="!compact">
          <ContentReadMore :text="network.description"></ContentReadMore>
        </div>

        <a
          v-if="!compact"
          v-for="(link, index) in links"
          v-bind:key="index"
          :href="link.url"
          class="text-link hover:underline hover:bg-link-hover mr-7.5 hidden sm:inline-block"
        >
          <BaseIcon name="caret-right" class="inline w-5 h-5 -ml-1.5" />{{
            link.title
          }}
        </a>
      </div>
    </div>
  </article>
</template>
