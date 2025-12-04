<script setup lang="ts">
import type { Crumb } from "../../types/types";
import BaseIcon from "./BaseIcon.vue";
withDefaults(
  defineProps<{
    crumbs?: Crumb[];
    align?: "left" | "center";
  }>(),
  {
    crumbs: () => [],
    align: "center",
  }
);
</script>

<template>
  <nav aria-label="breadcrumb">
    <ol class="flex justify-between xl:hidden text-menu">
      <li>
        <NuxtLink
          :to="crumbs.length > 1 ? crumbs[crumbs.length - 2]?.url ?? '/' : '/'"
        >
          <span class="sr-only">Go back one page</span>
          <BaseIcon name="arrow-left" />
        </NuxtLink>
      </li>
    </ol>

    <ol
      class="items-center hidden gap-3 tracking-widest xl:flex font-display text-heading-lg"
      :class="{ 'justify-center': align === 'center' }"
    >
      <template v-for="(crumb, index) in crumbs">
        <li v-if="crumbs.length === 1" aria-current="page">
          <span class="text-breadcrumb cursor-default">{{ crumb.label }}</span>
        </li>
        <li v-else class="flex justify-center items-center gap-3">
          <span v-if="crumb.url === ''" class="text-breadcrumb cursor-default">
            {{ crumb.label }}
          </span>
          <NuxtLink
            v-else
            :to="crumb.url"
            class="text-breadcrumb hover:underline"
          >
            {{ crumb.label }}
          </NuxtLink>
          <span
            class="text-breadcrumb-arrow"
            v-if="index < Object.keys(crumbs).length - 1"
          >
            <BaseIcon name="caret-right" :width="12" />
          </span>
        </li>
      </template>
    </ol>
  </nav>
</template>
