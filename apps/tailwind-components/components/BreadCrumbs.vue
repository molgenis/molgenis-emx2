<script setup lang="ts">
withDefaults(
  defineProps<{
    crumbs?: Record<string, string>;
    align?: "left" | "center";
  }>(),
  {
    crumbs: () => ({}),
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
      <template v-for="(url, label, index) in crumbs" :key="label">
        <li v-if="index === Object.keys(crumbs).length - 1" aria-current="page">
          <span class="text-breadcrumb cursor-default">{{ label }}</span>
        </li>
        <li v-else class="flex justify-center items-center gap-3">
          <span v-if="url === ''" class="text-breadcrumb cursor-default">
            {{ label }}
          </span>
          <NuxtLink v-else :to="url" class="text-breadcrumb hover:underline">
            {{ label }}
          </NuxtLink>
          <span
            class="text-breadcrumb-arrow"
            v-if="index < Object.keys(crumbs).length - 1 || current"
          >
            <BaseIcon name="caret-right" :width="12" />
          </span>
        </li>
      </template>
    </ol>
  </nav>
</template>
