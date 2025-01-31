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
    
    <div class=" ">
        <fieldset class="flex pr-2">
          <div>
            <legend class="text-current">Theme:</legend>
          </div>
          
          <div class="px-1">
            <input class="hover:cursor-pointer ml-1" id="default-theme" type="radio" v-model="theme" value="" />
            <label class="hover:cursor-pointer" for="default-theme">
              Light
            </label>
          </div>
          <div class="px-1">
            <input class="hover:cursor-pointer ml-1" id="dark-theme" type="radio" v-model="theme" value="dark" />
            <label class="hover:cursor-pointer" for="dark-theme">
              Dark
            </label>
          </div>
          <div class="px-1">
            <input class="hover:cursor-pointer ml-1" id="molgenis-theme" type="radio" v-model="theme" value="molgenis" />
            <label class="hover:cursor-pointer" for="molgenis-theme"> Molgenis </label>
          </div>
          <div class="px-1">
            <input class="hover:cursor-pointer ml-1" id="umcg-theme" type="radio" v-model="theme" value="umcg" />
            <label class="hover:cursor-pointer" for="umcg-theme"> UMCG </label>
          </div>
          <div class="px-1">
            <input class="hover:cursor-pointer ml-1" id="aumc-theme" type="radio" v-model="theme" value="aumc" />
            <label class="hover:cursor-pointer" for="aumc-theme"> AUMC </label>
          </div>
        </fieldset>
     
    </div>
  </nav>
            
  <NuxtLayout>
    <NuxtPage  />
  </NuxtLayout>     

</template>
