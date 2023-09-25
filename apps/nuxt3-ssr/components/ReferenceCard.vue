<script setup>
defineProps({
  imageUrl: {
    type: String,
  },
  title: {
    type: String,
  },
  description: {
    type: String,
  },
  url: {
    type: String,
  },
  links: {
    type: Array,
  },
});
</script>

<template>
  <li class="border-t border-gray-200">
    <article class="grid grid-cols-12 gap-6 p-12.5">
      <div class="col-span-3">
        <div class="items-center flex h-full w-full justify-center">
          <a :href="url">
            <img :src="imageUrl" />
          </a>
        </div>
      </div>
      <div class="col-span-9">
        <header class="flex items-center h-full sm:h-min">
          <div class="grow">
            <h2
              class="min-w-[160px] inline-block mr-4 text-heading-base sm:text-heading-3xl font-extrabold text-blue-500"
            >
              <a :href="url" class="hover:underline hover:bg-blue-50">
                {{ title }}
              </a>
            </h2>
          </div>
          <div class="hidden sm:block">
            <div class="flex">
              <!-- <IconButton icon="star" class="text-blue-500" /> -->
              <a :href="url">
                <IconButton icon="arrow-right" class="text-blue-500" />
              </a>
            </div>
          </div>
        </header>

        <p class="text-body-base my-5 hidden sm:block">
          <ContentReadMore :value="description" />
        </p>

        <a
          v-for="(link, index) in links"
          v-bind:key="index"
          :href="link.url"
          class="text-blue-500 hover:underline hover:bg-blue-50 mr-7.5 hidden sm:inline-block"
        >
          <BaseIcon name="caret-right" class="inline w-5 h-5 -ml-1.5" />{{
            link.title
          }}
        </a>
      </div>
    </article>
  </li>
</template>
