<template>
  <div class="flex mb-4">
    <div class="flex-1">
      <h3 class="text-heading-3xl">Project icons</h3>
      <div class="grid grid-cols-4 gap-4">
        <div
          v-for="icon in customIcons"
          class="flex flex-col justify-center items-center"
        >
          <label class="">{{ icon }}</label>
          <div class="p-4">
            <Icon :name="icon" :class="selectedAnimationClass" />
          </div>
        </div>
      </div>

      <hr class="my-4" />
      <h3 class="py-3 text-heading-3xl">Nuxt icons</h3>
      <p class="py-1">
        see:
        <a href="https://nuxt.com/modules/icon/" target="_blank"
          >nuxt.com/modules/icon</a
        >
      </p>
      <p class="pt-1 pb-3">
        and: <a href="https://icones.js.org/" target="_blank">icones.js.org</a>
      </p>
      <div class="grid grid-cols-4 gap-4">
        <div
          v-for="icon in nuxtIcons"
          class="flex flex-col justify-center items-center"
        >
          <label class="">{{ icon }}</label>
          <div class="p-4">
            <Icon :name="icon" :class="selectedAnimationClass" />
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
const modules: Record<string, any> = import.meta.glob("../assets/icons/*.svg", {
  import: "default",
  eager: true,
});

const names = Object.keys(modules).map((key: string) => {
  return key.split("/").reverse()[0].replace(".svg", "");
});

const preFixLocalIcon = (str: string) => `mg:${str}`;

const localIconNames = names.map(preFixLocalIcon);

const selectedAnimationClass = ref<string | null>(null);
const customIcons = [...localIconNames];
const nuxtIcons = [
  "uil:github",
  "line-md:arrow-left",
  "line-md:arrow-right",
  "line-md:arrow-up",
  "line-md:arrow-down",
  "line-md:chevron-left",
  "line-md:chevron-right",
  "line-md:chevron-up",
  "line-md:chevron-down",
  "ic:baseline-check",
  "line-md:person",
  "mg:image-diagram-2",
  "mg:arrow-right",
];
customIcons.push(...localIconNames);
</script>
