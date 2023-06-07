<script setup>
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
</script>

<template>
  <li class="my-[5px]">
    <span class="absolute ml-7">
      <BaseIcon
        name="collapsible-list-item"
        :width="18"
        class="text-gray-400" />
    </span>

    <dt class="pt-1 pl-14 flex items-start">
      <div class="flex items-center gap-1">
        <div class="inline-block">
          <a v-if="url" :href="url" class="hover:underline text-blue-500">
            {{ title }}
          </a>
          <span v-else>{{ title }}</span>
        </div>
        <span v-if="count" class="text-gray-400 inline-block ml-1">
          - {{ count }}
        </span>
        <div v-if="tooltip" class="inline-block">
          <CustomTooltip label="Lees meer" :content="tooltip" />
        </div>
      </div>
    </dt>

    <slot></slot>
  </li>
</template>
