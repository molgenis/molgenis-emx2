<script setup lang="ts">

const theme = useCookie("theme", {
  default: () => "",
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
const isFocusLayout = ref<boolean>(false);

function toggleLayout () {
  if(isFocusLayout.value) {
    isFocusLayout.value = false;
    setPageLayout('default');
    
  } else {
    isFocusLayout.value = true;
    setPageLayout('focus');
  }
}

</script>


<template>
  <nav class="fixed top-0 w-[100%] bg-white z-50 p-2 flex flex-row justify-start items-center gap-2 shadow-sm">
    <div class="grow flex gap-4">
      <NuxtLink class="hover:underline" to="/">
        <img format="svg" src="~/assets/img/molgenis-logo-blue-small.svg" alt="molgenis, open source software"
          class="w-28" />
        <span class="sr-only">Home</span>
      </NuxtLink>
      <Button type="outline" size="small" @click="toggleLayout">{{ isFocusLayout ? "show side": "focus"}}</Button>
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
            <label class="hover:cursor-pointer" for="umcg-theme"> UMCG </label>
          </div>
          <div class="px-1">
            <input class="hover:cursor-pointer mr-2" id="aumc-theme" type="radio" v-model="theme" value="aumc" />
            <label class="hover:cursor-pointer" for="aumc-theme"> AUMC </label>
          </div>
        </fieldset>
      </div>
    </div>
  </nav>
            
  <NuxtLayout @click="isExpanded = false">
    <NuxtPage  />
  </NuxtLayout>     

</template>
