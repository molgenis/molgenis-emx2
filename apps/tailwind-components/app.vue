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
</script>
<template>
  <div
    class="overflow-x-clip min-h-screen bg-base-gradient relative after:bg-app-wrapper after:w-full after:h-[166px] after:top-0 after:absolute after:opacity-20 after:z-20 xl:after:hidden"
  >
    <div
      class="absolute top-0 left-0 z-10 w-screen h-screen overflow-hidden opacity-background-gradient"
    >
      <BackgroundGradient class="z-10" />
    </div>
    <div class="z-30 relative min-h-screen flex flex-col">
      <main class="mb-auto">
        <Container>
          <div id="header-place-holder"></div>
          <div class="xl:flex">
            <aside class="xl:min-w-95 xl:w-95 hidden xl:block">
              <h2 class="text-2xl font-bold my-5">Components</h2>
              <ul class="list-none">
                <li class="py-2">
                  <NuxtLink class="hover:underline" to="/">Home</NuxtLink>
                </li>
                <li class="py-2">
                  <NuxtLink class="hover:underline" to="/Banner.story">
                    Banner
                  </NuxtLink>
                </li>
                <li class="py-2">
                  <NuxtLink class="hover:underline" to="/Comp1.story">
                    Comp 1
                  </NuxtLink>
                </li>
                <li class="py-2">
                  <NuxtLink class="hover:underline" to="/CustomTooltip.story">
                    Custom tooltip
                  </NuxtLink>
                </li>
                <li class="py-2">
                  <NuxtLink class="hover:underline" to="/DisplayList.story">
                    Display list
                  </NuxtLink>
                </li>
                <li class="py-2">
                  <NuxtLink class="hover:underline" to="/input/Checkbox.story">
                    Input checkbox
                  </NuxtLink>
                </li>
                <li class="py-2">
                  <NuxtLink class="hover:underline" to="/input/List.story">
                    Input list
                  </NuxtLink>
                </li>
                <li class="py-2">
                  <NuxtLink class="hover:underline" to="/input/Select.story">
                    Input select
                  </NuxtLink>
                </li>
                <li class="py-2">
                  <NuxtLink class="hover:underline" to="/input/String.story">
                    Input string
                  </NuxtLink>
                </li>
                <li class="py-2">
                  <NuxtLink class="hover:underline" to="/input/TextArea.story">
                    Input text area
                  </NuxtLink>
                </li>
                <li class="py-2">
                  <NuxtLink class="hover:underline" to="/input/Tree.story">
                    Input tree
                  </NuxtLink>
                </li>
                <li class="py-2">
                  <NuxtLink class="hover:underline" to="/Modal.story">
                    Modal
                  </NuxtLink>
                </li>
                <li class="py-2">
                  <NuxtLink class="hover:underline" to="/FilterSearch.story">
                    Filer search
                  </NuxtLink>
                </li>
                <li class="py-2">
                  <NuxtLink class="hover:underline" to="/Icons.story">
                    Icons
                  </NuxtLink>
                </li>
                <li class="py-2">
                  <NuxtLink class="hover:underline" to="">
                    Viz chart legend
                  </NuxtLink>
                </li>
              </ul>
              <div class="pr-6 mt-6">
                <hr />
              </div>
              <fieldset class="mt-3">
                <legend>Theme:</legend>
                <div>
                  <input
                    class="hover:cursor-pointer m-2"
                    id="default-theme"
                    type="radio"
                    v-model="theme"
                    value=""
                  />
                  <label class="hover:cursor-pointer" for="default-theme">
                    Default
                  </label>
                </div>
                <div>
                  <input
                    class="hover:cursor-pointer m-2"
                    id="umcg-theme"
                    type="radio"
                    v-model="theme"
                    value="umcg"
                  />
                  <label class="hover:cursor-pointer" for="umcg-theme">
                    Umcg
                  </label>
                </div>
              </fieldset>
            </aside>
            <div class="xl:pl-7.5 xl:max-w-[54rem] 2xl:grow 2xl:max-w-none">
              <slot name="main">
                <NuxtPage />
              </slot>
            </div>
          </div>
        </Container>
      </main>
    </div>
  </div>
</template>
