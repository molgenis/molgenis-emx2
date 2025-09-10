<template>
  <div class="flex mb-4">
    <div class="flex-1">
      <div class="grid grid-cols-4 gap-4">
        <div
          v-for="icon in icons"
          class="flex flex-col justify-center items-center text-title"
        >
          <label class="">{{ icon }}</label>
          <div class="p-4">
            <BaseIcon :name="icon" :class="selectedAnimationClass" />
          </div>
        </div>
      </div>
    </div>
    <div class="h-12 ml-4 mt-2">
      <fieldset class="border border-gray-900 mb-2 p-1">
        <legend class="m-2 px-2">Added classes</legend>

        <label class="ml-1 hover:cursor-pointer" for="animation-select">
          Animation
        </label>
        <div class="mb-2">
          <select
            id="animation-select"
            v-model="selectedAnimationClass"
            class="h-full rounded-md border-1 bg-transparent py-0 pl-2 pr-7 text-gray-500 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm"
          >
            <option value="null">None</option>
            <option value="animate-spin">animate-spin</option>
            <option value="animate-ping">animate-ping</option>
            <option value="animate-pulse">animate-pulse</option>
            <option value="animate-bounce ">animate-bounce</option>
          </select>
        </div>
      </fieldset>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";

const modules: Record<string, any> = import.meta.glob(
  "../components/global/icons/*.vue",
  {
    import: "default",
    eager: true,
  }
);

const icons = Object.keys(modules).map((key: string) => {
  return key.split("/").reverse()[0].replace(".vue", "");
});

const selectedAnimationClass = ref<string | null>(null);
</script>
