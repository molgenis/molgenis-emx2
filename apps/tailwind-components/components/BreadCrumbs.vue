<script setup lang="ts">
withDefaults(
  defineProps<{
    crumbs?: Record<string, string>;
    current?: string | undefined;
    align?: "left" | "center";
  }>(),
  {
    crumbs: () => ({}),
    current: undefined,
    align: "center",
  }
);
</script>

<template>
  <nav aria-label="breadcrumb">
    <ol class="flex justify-between xl:hidden text-menu">
      <li>
        <NuxtLink :to="Object.values(crumbs).slice(-1)[0]">
          <span class="sr-only">Go back one page</span>
          <BaseIcon name="arrow-left" />
        </NuxtLink>
      </li>
    </ol>

    <ol
      class="items-center hidden gap-3 tracking-widest xl:flex font-display text-heading-lg"
      :class="{ 'justify-center': align === 'center' }"
    >
      <li
        v-for="(url, label, index) in crumbs"
        :key="label"
        class="flex justify-center items-center gap-3"
      >
        <NuxtLink :to="url" class="text-breadcrumb hover:underline">
          {{ label }}
        </NuxtLink>
        <span
          class="text-breadcrumb-arrow"
          v-if="index < Object.keys(crumbs).length - 1 || current"
        >
          <BaseIcon name="caret-right" :width="12" />
        </span>
      </li>
      <li v-if="current && !Object.keys(crumbs).includes(current)">
        <a
          href=""
          class="text-breadcrumb hover:underline"
          aria-current="page"
          @click.prevent
        >
          {{ current }}
        </a>
      </li>
    </ol>
  </nav>
</template>
