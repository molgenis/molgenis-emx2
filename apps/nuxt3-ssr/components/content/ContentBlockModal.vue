<script setup lang="ts">
import { INotificationType } from "~/interfaces/types";

const { title, subTitle, description, type } = withDefaults(
  defineProps<{
    title: string;
    description?: string;
    subTitle?: string;
    type?: INotificationType;
  }>(),
  {
    type: INotificationType.light,
  }
);

const bgClass = computed(() => {
  switch (type) {
    case INotificationType.light:
      return "bg-white";
    case INotificationType.dark:
      return "bg-black";
    case INotificationType.success:
      return "bg-green-500";
    case INotificationType.error:
      return "bg-red-500";
    case INotificationType.warning:
      return "bg-yellow-500";
    case INotificationType.info:
      return "bg-blue-500";
  }
});
</script>

<template>
  <section class="pb-18 pt-7 pr-16 pl-7 text-gray-900" :class="bgClass">
    <div v-if="subTitle">{{ subTitle }}</div>
    <h2 class="mb-5 uppercase text-heading-4xl font-display" v-if="title">
      {{ title }}
    </h2>
    <div class="mb-5 prose max-w-none" v-if="description">
      <ContentReadMore :value="description" />
    </div>
    <slot></slot>
  </section>
</template>
