<script setup>
import ContentBlock from "./ContentBlock.vue";
import IconButton from "./IconButton.vue";
import BaseIcon from "./BaseIcon.vue";

defineProps({
  title: {
    type: String,
  },
  description: {
    type: String,
  },
});
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <ul class="list-none pt-12.5 -mx-12.5">
      <li class="border-t border-gray-200">
        <article class="grid grid-cols-12 gap-6 p-12.5">
          <div class="col-span-3">
            <div class="items-center flex h-full w-full justify-center">
              <a href="#">
                <img src="https://via.placeholder.com/137x150" />
              </a>
            </div>
          </div>
          <div class="col-span-9">
            <header class="flex items-center h-full sm:h-min">
              <div class="grow">
                <h2
                  class="min-w-[160px] inline-block mr-4 text-heading-base sm:text-heading-3xl font-extrabold text-blue-500"
                >
                  <a href="#" class="hover:underline hover:bg-blue-50">
                    Placeholder network</a
                  >
                </h2>
              </div>
              <div class="hidden sm:block">
                <div class="flex">
                  <IconButton icon="star" class="text-blue-500" />
                  <IconButton icon="arrow-right" class="text-blue-500" />
                </div>
              </div>
            </header>

            <p class="text-body-base my-5 hidden sm:block">
              The European Human Exposome Network (EHEN) is the world’s largest
              network of projects studying the impact of environmental exposures
              across a lifetime - the exposome - on human health. Collectively,
              the EHEN projects are working in 24 countries across Europe and
              the USA. There are over 100 data sets being used including
              cohorts, cross-sectional studies, biobanks, randomised controlled
              trials (RCTs), case control studies and health or occupational
              registries.
            </p>

            <a
              href="#"
              class="text-blue-500 hover:underline hover:bg-blue-50 mr-7.5 hidden sm:inline-block"
              ><BaseIcon
                name="caret-right"
                class="inline w-5 h-5 -ml-1.5"
              />Network</a
            >
            <a
              href="#"
              class="text-blue-500 hover:underline hover:bg-blue-50 mr-7.5 hidden sm:inline-block"
              ><BaseIcon
                name="caret-right"
                class="inline w-5 h-5 -ml-1.5"
              />Cohorts</a
            >
            <a
              href="#"
              class="text-blue-500 hover:underline hover:bg-blue-50 mr-7.5 hidden sm:inline-block"
              ><BaseIcon
                name="caret-right"
                class="inline w-5 h-5 -ml-1.5"
              />Variables</a
            >
          </div>
        </article>
      </li>
      <li class="border-t border-gray-200">
        <article class="grid grid-cols-12 gap-6 p-12.5">
          <div class="col-span-3">
            <div class="items-center flex h-full w-full justify-center">
              <a href="#">
                <img src="https://via.placeholder.com/137x150" />
              </a>
            </div>
          </div>
          <div class="col-span-9">
            <header class="flex items-center h-full sm:h-min">
              <div class="grow">
                <h2
                  class="min-w-[160px] inline-block mr-4 text-heading-base sm:text-heading-3xl font-extrabold text-blue-500"
                >
                  <a href="#" class="hover:underline hover:bg-blue-50">
                    Placeholder network</a
                  >
                </h2>
              </div>
              <div class="hidden sm:block">
                <div class="flex">
                  <IconButton icon="star" class="text-blue-500" />
                  <IconButton icon="arrow-right" class="text-blue-500" />
                </div>
              </div>
            </header>

            <p class="text-body-base my-5 hidden sm:block">
              The University Medical Center Groningen (UMCG) is one of the
              largest hospitals in the Netherlands and the largest employer in
              the Northern Netherlands. The more than 12,000 employees work
              together on care, research, training and education with the common
              goal: building the future of health.
            </p>

            <a
              href="#"
              class="text-blue-500 hover:underline hover:bg-blue-50 mr-7.5 hidden sm:inline-block"
              ><BaseIcon
                name="caret-right"
                class="inline w-5 h-5 -ml-1.5"
              />Network</a
            >
            <a
              href="#"
              class="text-blue-500 hover:underline hover:bg-blue-50 mr-7.5 hidden sm:inline-block"
              ><BaseIcon
                name="caret-right"
                class="inline w-5 h-5 -ml-1.5"
              />Cohorts</a
            >
            <a
              href="#"
              class="text-blue-500 hover:underline hover:bg-blue-50 mr-7.5 hidden sm:inline-block"
              ><BaseIcon
                name="caret-right"
                class="inline w-5 h-5 -ml-1.5"
              />Variables</a
            >
          </div>
        </article>
      </li>
    </ul>
  </ContentBlock>
</template>
