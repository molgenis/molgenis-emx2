<script setup lang="ts">

const theme = useCookie("theme", {
  default: () => "",
});

const invert = useCookie("invert", {
  default: () => false,
});

useHead({
  title: "Tailwind components",
  meta: [
    {
      name: "Tailwind components",
      content: "Components",
    },
  ],
  htmlAttrs: {
    "data-theme": theme,
  },
});

const isExpanded = ref<boolean>(false);

const modules = import.meta.glob("./**/*.story.vue", {
  import: "default",
  eager: true,
});

const stories = Object.keys(modules)
  .map((module: string) => {
    const name: string = module.split("/").reverse()[0];
    const path: string = module.replace("./pages/", "/");
    const dir: string = path
      .split("/")
      .filter((path: string) => path !== "" && path !== name)[0];
    const nameCleaned: string = name.replace(".story.vue", "");
    return {
      name: dir
        ? `${dir.charAt(0).toUpperCase() + dir.slice(1)}${nameCleaned}`
        : nameCleaned,
      dir: dir,
      path: path.replace(".vue", ""),
    };
  })
  .sort((current: Record<string, any>, next: Record<string, any>) => {
    return current.name.localeCompare(next.name);
  });
</script>


<template>
  <nav class="fixed top-0 w-[100%] bg-white z-50 p-2 flex flex-row justify-start items-center gap-2 shadow-sm">
    <div class="grow">
      <NuxtLink class="hover:underline" to="/">
        <img format="svg" src="~/assets/img/molgenis-logo-blue-small.svg" alt="molgenis, open source software"
          class="w-28" />
        <span class="sr-only">Home</span>
      </NuxtLink>
    </div>
    <div class="w-[150px] relative">
      <button id="theme-selector-toggle" aria-controls="theme-selector" :aria-expanded="isExpanded"
        @click="isExpanded = !isExpanded">
        <span>Theme</span>
        <CaretDown class="w-5 inline-block" />
      </button>
      <div class="absolute mt-2 z-50 bg-white py-2 px-4 rounded shadow-sm border" v-show="isExpanded">
        <fieldset class="mb-2">
          <legend class="text-current text-body-sm">Select a theme</legend>
          <div class="px-1">
            <input class="hover:cursor-pointer mr-2" id="default-theme" type="radio" v-model="theme" value="" />
            <label class="hover:cursor-pointer" for="default-theme">
              Default
            </label>
          </div>
          <div class="px-1">
            <input class="hover:cursor-pointer mr-2" id="umcg-theme" type="radio" v-model="theme" value="umcg" />
            <label class="hover:cursor-pointer" for="umcg-theme"> Umcg </label>
          </div>
        </fieldset>
        <fieldset>
          <legend class="text-current text-body-sm">Set inversion</legend>
          <div class="px-1">
            <input id="invert-off" class="hover:cursor-pointer mr-2" type="radio" :value="false" name="invert"
              v-model="invert" />
            <label class="hover:cursor-pointer" for="invert-off">Off</label>
          </div>
          <div class="px-1">
            <input id="invert-on" class="hover:cursor-pointer mr-2" type="radio" :value="true" name="invert"
              v-model="invert" />
            <label class="hover:cursor-pointer" for="invert-on">On</label>
          </div>
        </fieldset>
      </div>
    </div>
  </nav>
  <div
    class="overflow-x-clip min-h-screen bg-base-gradient relative after:bg-app-wrapper after:w-full after:h-[166px] after:top-0 after:absolute after:opacity-20 after:z-20 xl:after:hidden pt-15"
    @click="isExpanded = false">
    <div class="absolute top-0 left-0 z-10 w-screen h-screen overflow-hidden opacity-background-gradient">
      <BackgroundGradient class="z-10" />
    </div>
    <div class="z-30 relative min-h-screen flex flex-col">
      <main class="mb-auto">
        <div id="header-place-holder"></div>
        <div class="xl:flex">
          <aside class="xl:min-w-95 xl:w-95 hidden xl:block pl-6">
            <h2 class="text-2xl font-bold my-5">Components</h2>
            <ul class="list-none">
              <li class="py-2" v-for="story in stories">
                <NuxtLink class="hover:underline" :to="story.path">{{
                  story.name
                }}</NuxtLink>
              </li>
            </ul>

            <div class="pr-6 my-6">
              <hr>
            </div>
            <h2 class="text-2xl font-bold my-5">Other</h2>
            <NuxtLink class="hover:underline" to="/DataFetch.other">Data fetching</NuxtLink>
          </aside>
          <div class="xl:pl-7.5 grow p-6">
            <slot name="main">
              <NuxtPage :invertTheme="invert" />
            </slot>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>