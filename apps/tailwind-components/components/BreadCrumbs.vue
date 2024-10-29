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
  <div class="flex justify-between xl:hidden text-menu">
    <NuxtLink :to="Object.values(crumbs).slice(-1)[0]">
      <span class="sr-only">Go up one level</span>
      <BaseIcon name="arrow-left" />
    </NuxtLink>
    <!-- <a href="#">
      <span class="sr-only">Favorite</span>
      <BaseIcon name="star" />
    </a> -->
  </div>
  <nav aria-label="breadcrumb">
    <ol
      class="items-center hidden gap-3 tracking-widest xl:flex font-display text-heading-lg"
      :class="{ 'justify-center': align === 'center' }"
    >
      <li
        v-for="(url, label, index) in crumbs"
        :key="label"
        class="flex justify-center items-center gap-3"
      >
        <a
          href=""
          class="text-breadcrumb"
          v-if="current"
          aria-current="page"
          @click.prevent
        >
          {{ current }}
        </a>
        <NuxtLink v-else :to="url" class="text-breadcrumb hover:underline">
          {{ label }}
        </NuxtLink>
        <span
          v-if="index < Object.keys(crumbs).length - 1"
          class="text-breadcrumb-arrow"
        >
          <BaseIcon name="caret-right" :width="12" />
        </span>
      </li>
    </ol>
  </nav>
</template>
