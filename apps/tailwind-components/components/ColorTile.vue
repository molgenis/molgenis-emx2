<script setup lang="ts">
const props = defineProps<{
  color: string;
  type: "background" | "text" | "border" | "radius";
}>();

const backgroundClass = computed(() => {
  return props.color.startsWith("bg-") ? props.color : `bg-${props.color}`;
});

const textClass = computed(() => {
  return props.color.startsWith("text-") ? props.color : `text-${props.color}`;
});

const borderColorClass = computed(() => {
  return props.color.startsWith("border-")
    ? props.color
    : `border-${props.color}`;
});

const radiusClass = computed(() => {
  return props.color.startsWith("rounded-")
    ? props.color
    : `rounded-${props.color}`;
});
</script>
<template>
  <div class="w-full md:w-36 mb-4 md:mr-4">
    <div
      v-if="type === 'background'"
      class="mb-2 flex-none w-full md:w-36 h-16 md:h-36 flex items-center justify-center"
      :class="backgroundClass"
    ></div>
    <div
      v-else-if="type === 'text'"
      class="mb-2 flex-none w-full md:w-36 h-16 md:h-36 flex items-center justify-center border border-gray-300"
      :class="textClass"
    >
      <span class="text-3xl" :class="textClass">Aa</span>
    </div>
    <div
      v-else-if="type === 'border'"
      class="mb-2 flex-none w-full md:w-36 h-16 md:h-36 flex items-center justify-center border-2"
      :class="borderColorClass"
    ></div>
    <div
      v-else-if="type === 'radius'"
      class="mb-2 flex-none w-full md:w-36 h-16 md:h-36 flex items-center justify-center border-2 border-gray-300"
      :class="radiusClass"
    ></div>
    <div class="relative">
      <div
        class="pointer-events-none absolute z-50 py-1 px-2 text-sm bg-gray-800 dark:bg-midnight text-white rounded transition duration-200 overflow-hidden"
      >
        <br /><!---->
      </div>
      <div
        class="inline-block text-sm text-gray-800 dark:text-gray-400 font-mono hover:text-teal-600 dark-hover:text-teal-400 cursor-pointer break-words"
      >
        {{ color }}
      </div>
    </div>
  </div>
</template>
