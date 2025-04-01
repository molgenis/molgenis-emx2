<script setup lang="ts">
interface Props {
  title: string;
  description?: string;
  callToAction?: string;
  count?: number;
  link: string;
  openLinkInNewTab?: boolean;
  image: string;
}

withDefaults(defineProps<Props>(), {
  callToAction: "",
});
</script>
<template>
  <div class="flex flex-col items-center max-w-sm">
    <div
      class="flex flex-row md:flex-col self-start md:self-center items-center max-w-sm"
    >
      <span class="mb-2 mt-2.5 text-icon">
        <BaseIcon :name="image" :width="55" />
      </span>
      <div class="relative">
        <NuxtLink :to="link" :target="openLinkInNewTab ? '_blank' : undefined">
          <h1
            class="font-display md:text-heading-5xl text-heading-5xl text-title-contrast-pop px-3"
          >
            {{ title }}
          </h1>
        </NuxtLink>
      </div>
      <slot name="title-suffix">
        <span
          class="bg-blue-50 text-title-contrast-pop flex justify-center rounded-full px-3 py-1 font-bold text-heading-sm"
          v-if="typeof count != 'undefined'"
        >
          {{ count }}
        </span>
      </slot>
      <span
        class="md:hidden absolute right-0 mr-3 hover:text-blue-800 text-blue-500"
      >
        <NuxtLink :to="link" :target="openLinkInNewTab ? '_blank' : undefined">
          <IconButton icon="arrow-right" />
        </NuxtLink>
      </span>
    </div>

    <p
      class="mt-1 mb-4 md:mb-0 text-left md:text-center lg:mb-5 text-body-lg text-sub-title-contrast h-full md:h-24"
      v-if="description"
    >
      {{ description }}
    </p>

    <NuxtLink
      class="md:block hidden mt-auto"
      :to="link"
      :target="openLinkInNewTab ? '_blank' : undefined"
    >
      <Button :label="callToAction || title" type="primary" size="medium" />
    </NuxtLink>
  </div>
</template>
