<template>
  <nav
    class="text-title bg-navigation-sticky fixed top-0 w-[100%] z-50 p-2 flex flex-row justify-start items-center gap-2 shadow-sm"
  >
    <div class="grow flex gap-4">
      <button
        id="toggleLayoutBtn"
        @click="setMenuIsOpen"
        class="ml-4 mr-2 text-title"
        :tooltip="menuIsOpen ? 'hide menu' : 'show menu'"
      >
        <BaseIcon :name="menuIsOpen ? 'MenuOpen' : 'Menu'" :width="24" />
        <span class="sr-only">
          {{ menuIsOpen ? "hide menu" : "show menu" }}
        </span>
      </button>
      <NuxtLink class="hover:underline" to="/">
        <img
          format="svg"
          src="./assets/img/molgenis-logo-blue-small.svg"
          alt="molgenis, open source software"
          class="w-28"
        />
        <span class="sr-only">Home</span>
      </NuxtLink>
    </div>

    <div class=" ">
      <fieldset class="flex pr-2">
        <div>
          <legend class="text-current">Theme:</legend>
        </div>

        <div class="px-1">
          <input
            class="hover:cursor-pointer ml-1"
            id="default-theme"
            type="radio"
            v-model="theme"
            value=""
            name="theme"
          />
          <label class="hover:cursor-pointer" for="default-theme">
            Light
          </label>
        </div>
        <div class="px-1">
          <input
            class="hover:cursor-pointer ml-1"
            id="dark-theme"
            type="radio"
            v-model="theme"
            value="dark"
            name="theme"
          />
          <label class="hover:cursor-pointer" for="dark-theme"> Dark </label>
        </div>
        <div class="px-1">
          <input
            class="hover:cursor-pointer ml-1"
            id="molgenis-theme"
            type="radio"
            v-model="theme"
            value="molgenis"
            name="theme"
          />
          <label class="hover:cursor-pointer" for="molgenis-theme">
            Molgenis
          </label>
        </div>
        <div class="px-1">
          <input
            class="hover:cursor-pointer ml-1"
            id="umcg-theme"
            type="radio"
            v-model="theme"
            value="umcg"
            name="theme"
          />
          <label class="hover:cursor-pointer" for="umcg-theme"> UMCG </label>
        </div>
        <div class="px-1">
          <input
            class="hover:cursor-pointer ml-1"
            id="aumc-theme"
            type="radio"
            v-model="theme"
            value="aumc"
            name="theme"
          />
          <label class="hover:cursor-pointer" for="aumc-theme"> AUMC </label>
        </div>
      </fieldset>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { useCookie, useHead } from "#app";
import { ref } from "vue";

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

const emit = defineEmits(["menuIsOpen"]);
const menuIsOpen = ref<boolean>(true);

function setMenuIsOpen() {
  menuIsOpen.value = !menuIsOpen.value;
  emit("menuIsOpen", menuIsOpen.value);
}
</script>
