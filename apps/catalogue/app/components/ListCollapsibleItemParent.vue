<script setup>
import { ref } from "vue";
import BaseIcon from "../../../tailwind-components/app/components/BaseIcon.vue";
import CustomTooltip from "./CustomTooltip.vue";

defineProps({
  title: {
    type: String,
  },
  count: {
    type: Number,
  },
  tooltip: {
    type: String, // yield component?? would be nice
  },
  url: {
    type: String,
  },
});

let collapsed = ref(true);
const toggleCollapse = () => {
  collapsed.value = !collapsed.value;
};
</script>

<template>
  <li class="pb-2.5 -ml-1.5">
    <div class="flex gap-1 items-start">
      <span
        @click="toggleCollapse()"
        class="text-link mr-1 mt-0.5 rounded-full hover:bg-link-hover hover:cursor-pointer p-0.5"
        :class="{ 'rotate-180': collapsed }"
      >
        <BaseIcon name="caret-up" :width="20" />
      </span>

      <div>
        <a v-if="url" :href="url" class="hover:underline text-link">
          {{ title }}
        </a>
        <span v-else>
          {{ title }}
        </span>
        <div class="whitespace-nowrap inline-flex items-center">
          <span v-if="count" class="text-gray-400 inline-block ml-1"
            >- {{ count }}</span
          >
          <div v-if="tooltip" class="inline-block ml-1">
            <CustomTooltip label="Lees meer" :content="tooltip" />
          </div>
        </div>
      </div>
    </div>

    <ul class="break-inside-avoid" :class="{ hidden: collapsed }">
      <slot></slot>
    </ul>
  </li>
</template>
