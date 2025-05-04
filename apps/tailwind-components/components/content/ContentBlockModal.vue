<script setup lang="ts">
import { computed } from "vue";
import type { INotificationType } from "../../types/types";

const props = withDefaults(
  defineProps<{
    title: string;
    description?: string;
    subTitle?: string;
    type?: INotificationType;
  }>(),
  {
    type: "light",
  }
);

const bgClass = computed(() => {
  switch (props.type) {
    case "light":
      return "bg-white";
    case "dark":
      return "bg-black";
    case "success":
      return "bg-green-500";
    case "error":
      return "bg-red-500";
    case "warning":
      return "bg-yellow-500";
    case "info":
      return "bg-blue-500";
  }
});
</script>

<template>
  <section class="pb-[2.5rem] pt-[3.125rem] px-[2.5rem]" :class="bgClass">
    <div v-if="subTitle">{{ subTitle }}</div>
    <div class="flex justify-between">
      <div>
        <h2 v-if="title" class="mb-5 uppercase text-heading-4xl font-display">
          {{ title }}
        </h2>
      </div>
      <div>
        <slot name="title-button" />
      </div>
    </div>
    <div class="mb-5 prose max-w-none" v-if="description">
      <div v-html="description"></div>
    </div>
    <slot></slot>
  </section>
</template>
